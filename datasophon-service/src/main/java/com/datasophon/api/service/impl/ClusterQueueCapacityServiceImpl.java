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
import com.datasophon.api.converter.ClusterQueueCapacityConverter;
import com.datasophon.api.converter.ClusterServiceRoleInstanceConverter;
import com.datasophon.common.dto.ClusterQueueCapacityDTO;
import com.datasophon.common.enums.Status;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.api.service.ClusterQueueCapacityService;
import com.datasophon.api.service.ClusterServiceRoleInstanceService;
import com.datasophon.api.utils.HadoopUtils;

import com.datasophon.common.model.Generators;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.dao.entity.ClusterInfoEntity;
import com.datasophon.dao.entity.ClusterQueueCapacity;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;
import com.datasophon.dao.mapper.ClusterQueueCapacityMapper;
import com.datasophon.dao.model.ClusterQueueCapacityList;
import com.datasophon.dao.model.Links;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * 集群队列容量服务实现类
 * 提供集群队列容量的业务逻辑处理
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
@Service("clusterQueueCapacityService")
public class ClusterQueueCapacityServiceImpl extends ServiceImpl<ClusterQueueCapacityMapper, ClusterQueueCapacity>
        implements ClusterQueueCapacityService {

    private static final Logger logger = LoggerFactory.getLogger(ClusterQueueCapacityServiceImpl.class);

    @Autowired
    private ClusterQueueCapacityConverter clusterQueueCapacityConverter;

    @Autowired
    private ClusterServiceRoleInstanceService roleInstanceService;
    
    @Autowired
    private ClusterInfoService clusterInfoService;
    
    @Autowired
    private ClusterServiceRoleInstanceConverter roleInstanceConverter;

    @Override
    public boolean refreshToYarn(Integer clusterId) throws Exception {
        List<ClusterQueueCapacity> list = getMapper().selectByClusterId(clusterId);
        ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);
        // Service层：获取DTO列表后使用Converter转换为Entity列表
        List<ClusterServiceRoleInstanceEntity> roleList = roleInstanceConverter
                .dtoListToEntityList(roleInstanceService
                        .getServiceRoleInstanceListByClusterIdAndRoleName(clusterId, "ResourceManager"))
;

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
        this.save(queueCapacity);
    }

    @Override
    public ClusterQueueCapacityList listCapacityQueue(Integer clusterId) {
        List<ClusterQueueCapacity> list = getMapper().selectByClusterId(clusterId);

        ClusterQueueCapacityList clusterQueueCapacityList = new ClusterQueueCapacityList();
        clusterQueueCapacityList.setRootId("root");
        clusterQueueCapacityList.setNodes(list);

        List<Links> linksList = new ArrayList<>();
        for (ClusterQueueCapacity clusterQueueCapacity : list) {
            Links links = new Links();
            links.setFrom(clusterQueueCapacity.getParent());
            links.setTo(clusterQueueCapacity.getQueueName());
            linksList.add(links);
        }
        clusterQueueCapacityList.setLinks(linksList);
        return clusterQueueCapacityList;
    }

    // 新增DTO方法实现
    @Override
    public ClusterQueueCapacityDTO getByIdAsDto(Integer id) {
        ClusterQueueCapacity entity = this.getById(id);
        return clusterQueueCapacityConverter.entityToDto(entity);
    }

    @Override
    public ClusterQueueCapacityDTO saveQueueCapacity(ClusterQueueCapacityDTO dto) {
        ClusterQueueCapacity entity = clusterQueueCapacityConverter.dtoToEntity(dto);
        this.save(entity);
        return clusterQueueCapacityConverter.entityToDto(entity);
    }

    @Override
    public void updateQueueCapacity(ClusterQueueCapacityDTO dto) {
        ClusterQueueCapacity entity = clusterQueueCapacityConverter.dtoToEntity(dto);
        this.updateById(entity);
    }

}
