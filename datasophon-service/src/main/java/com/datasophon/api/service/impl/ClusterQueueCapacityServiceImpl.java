/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.datasophon.api.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson2.JSONObject;
import com.datasophon.common.enums.Status;
import com.datasophon.api.service.ClusterQueueCapacityService;
import com.datasophon.api.service.ClusterServiceRoleInstanceService;
import com.datasophon.api.utils.HadoopUtils;
import com.datasophon.api.utils.ProcessUtils;
import com.datasophon.common.model.Generators;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.dao.entity.ClusterInfoEntity;
import com.datasophon.dao.entity.ClusterQueueCapacity;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;
import com.datasophon.dao.mapper.ClusterQueueCapacityMapper;
import com.datasophon.dao.model.ClusterQueueCapacityList;
import com.datasophon.dao.model.Links;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * 集群队列容量服务实现
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2024-12-19
 */
@Service("clusterQueueCapacityService")
public class ClusterQueueCapacityServiceImpl implements ClusterQueueCapacityService {

    private static final Logger logger = LoggerFactory.getLogger(ClusterQueueCapacityServiceImpl.class);

    @Autowired
    private ClusterQueueCapacityMapper clusterQueueCapacityMapper;

    @Autowired
    private ClusterServiceRoleInstanceService roleInstanceService;

    @Override
    public boolean refreshToYarn(Integer clusterId) throws Exception {
        List<ClusterQueueCapacity> list = clusterQueueCapacityMapper.selectByClusterId(clusterId);
        ClusterInfoEntity clusterInfo = ProcessUtils.getClusterInfo(clusterId);
        List<ClusterServiceRoleInstanceEntity> roleList = roleInstanceService
                .getServiceRoleInstanceListByClusterIdAndRoleName(clusterId, "ResourceManager");

        // build configfilemap
        HashMap<Generators, List<ServiceConfig>> configFileMap = new HashMap<>();
        Generators generators = new Generators();
        generators.setFilename("capacity-scheduler.xml");
        generators.setOutputDirectory("etc/hadoop");
        generators.setConfigFormat("custom");
        generators.setTemplateName("capacity-scheduler.ftl");

        ArrayList<ServiceConfig> serviceConfigs = new ArrayList<>();
        ServiceConfig config = new ServiceConfig();
        ArrayList<JSONObject> queueList = new ArrayList<>();

        for (ClusterQueueCapacity clusterYarnQueue : list) {
            JSONObject queue = new JSONObject();
            BeanUtil.copyProperties(clusterYarnQueue, queue, false);
            queueList.add(queue);
        }

        config.setName("queueList");
        config.setValue(queueList);
        config.setConfigType("map");
        config.setRequired(true);

        serviceConfigs.add(config);

        configFileMap.put(generators, serviceConfigs);
        String hostname = "";
        for (ClusterServiceRoleInstanceEntity roleInstanceEntity : roleList) {
            ExecResult execResult = HadoopUtils.configQueueProp(clusterInfo, configFileMap, roleInstanceEntity);
            if (!execResult.getExecResult()) {
                throw new RuntimeException("config capacity-scheduler.xml failed");
            }
            if (StringUtils.isBlank(hostname)) {
                hostname = roleInstanceEntity.getHostname();
            }
        }
        ExecResult execResult = HadoopUtils.refreshQueuePropToYarn(clusterInfo, hostname);
        if (execResult.getExecResult()) {
            logger.info("yarn dfsadmin -refreshQueues success at {}", hostname);
            return true;
        } else {
            logger.info(execResult.getExecOut());
            throw new RuntimeException(Status.FAILED_REFRESH_THE_QUEUE_TO_YARN.getMsg());
        }
    }

    @Override
    public void createDefaultQueue(Integer clusterId) {
        ClusterQueueCapacity queueCapacity = new ClusterQueueCapacity();
        queueCapacity.setCapacity("100");
        queueCapacity.setClusterId(clusterId);
        queueCapacity.setQueueName("default");
        queueCapacity.setNodeLabel("default");
        queueCapacity.setAclUsers("*");
        queueCapacity.setParent("root");
        clusterQueueCapacityMapper.insert(queueCapacity);
    }

    @Override
    public ClusterQueueCapacityList listCapacityQueue(Integer clusterId) {
        List<ClusterQueueCapacity> list = clusterQueueCapacityMapper.selectByClusterId(clusterId);

        ClusterQueueCapacityList clusterQueueCapacityList = new ClusterQueueCapacityList();
        clusterQueueCapacityList.setRootId("root");
        clusterQueueCapacityList.setNodes(list);

        ArrayList<Links> linksList = new ArrayList<>();
        for (ClusterQueueCapacity clusterQueueCapacity : list) {
            Links links = new Links();
            links.setFrom(clusterQueueCapacity.getParent());
            links.setTo(clusterQueueCapacity.getQueueName());
            linksList.add(links);
        }
        clusterQueueCapacityList.setLinks(linksList);
        return clusterQueueCapacityList;
    }

    // 标准CRUD方法实现
    @Override
    public ClusterQueueCapacity getById(Integer id) {
        return clusterQueueCapacityMapper.selectById(id);
    }

    @Override
    public ClusterQueueCapacity save(ClusterQueueCapacity entity) {
        clusterQueueCapacityMapper.insert(entity);
        return entity;
    }

    @Override
    public ClusterQueueCapacity updateById(ClusterQueueCapacity entity) {
        clusterQueueCapacityMapper.updateById(entity);
        return entity;
    }

    @Override
    public boolean removeByIds(List<Integer> ids) {
        return clusterQueueCapacityMapper.deleteByIds(ids) > 0;
    }

    @Override
    public List<ClusterQueueCapacity> getAllQueueCapacities() {
        return clusterQueueCapacityMapper.selectAll();
    }

}
