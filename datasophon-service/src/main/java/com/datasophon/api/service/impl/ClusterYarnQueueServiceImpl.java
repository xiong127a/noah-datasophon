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
import com.datasophon.api.converter.ClusterYarnQueueConverter;
import com.datasophon.api.master.ActorUtils;
import com.datasophon.api.master.handler.service.ServiceConfigureHandler;
import com.datasophon.api.service.ClusterServiceRoleInstanceService;
import com.datasophon.api.service.ClusterYarnQueueService;
import com.datasophon.common.Constants;
import com.datasophon.common.command.ExecuteCmdCommand;
import com.datasophon.common.dto.ClusterServiceRoleInstanceDTO;
import com.datasophon.common.dto.ClusterYarnQueueDTO;
import com.datasophon.common.enums.Status;
import com.datasophon.common.exception.BusinessException;
import com.datasophon.common.model.Generators;
import com.datasophon.common.model.PageResult;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.model.ServiceRoleInfo;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.dao.entity.ClusterYarnQueueEntity;
import com.datasophon.dao.mapper.ClusterYarnQueueMapper;
import com.mybatisflex.core.paginate.Page;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.apache.pekko.actor.ActorSelection;
import org.apache.pekko.pattern.Patterns;
import org.apache.pekko.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 集群Yarn队列服务实现
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
@Service("clusterYarnQueueService")
@Transactional
public class ClusterYarnQueueServiceImpl extends ServiceImpl<ClusterYarnQueueMapper, ClusterYarnQueueEntity>
        implements ClusterYarnQueueService {

    private static final Logger logger = LoggerFactory.getLogger(ClusterYarnQueueServiceImpl.class);

    @Autowired
    private ClusterServiceRoleInstanceService roleInstanceService;

    @Autowired
    private ClusterYarnQueueMapper clusterYarnQueueMapper;

    @Autowired
    private ClusterYarnQueueConverter clusterYarnQueueConverter;

    @Override
    public PageResult<ClusterYarnQueueDTO> listByPage(Long clusterId, Integer page, Integer pageSize) {
        Page<ClusterYarnQueueEntity> pageParam = Page.of(page, pageSize);
        Page<ClusterYarnQueueEntity> pageResult = clusterYarnQueueMapper.selectPageByClusterId(pageParam, clusterId);

        List<ClusterYarnQueueDTO> dtoList = clusterYarnQueueConverter.entityListToDtoList(pageResult.getRecords());
        return PageResult.of(dtoList, pageResult.getTotalRow(), page, pageSize);
    }

    @Override
    public ClusterYarnQueueDTO saveQueue(ClusterYarnQueueDTO clusterYarnQueueDTO) throws BusinessException {
        if (clusterYarnQueueMapper.existsByQueueName(clusterYarnQueueDTO.queueName())) {
            throw new BusinessException(Status.QUEUE_NAME_ALREADY_EXISTS.getCode(),
                    Status.QUEUE_NAME_ALREADY_EXISTS.getMsg());
        }

        ClusterYarnQueueEntity entity = clusterYarnQueueConverter.dtoToEntity(clusterYarnQueueDTO);
        entity.setCreateTime(LocalDateTime.now());
        this.save(entity);

        return clusterYarnQueueConverter.entityToDto(entity);
    }

    @Override
    public void refreshQueues(Long clusterId) throws BusinessException {
        List<ClusterYarnQueueEntity> list = clusterYarnQueueMapper.selectByClusterId(clusterId);
        // 查询resourcemanager节点
        List<ClusterServiceRoleInstanceDTO> roleList = roleInstanceService
                .getServiceRoleInstanceListByClusterIdAndRoleName(clusterId, "ResourceManager");

        // 构建configfilemap
        HashMap<Generators, List<ServiceConfig>> configFileMap = new HashMap<>();
        Generators generators = new Generators();
        generators.setFilename("fair-scheduler.xml");
        generators.setOutputDirectory("etc/hadoop");
        generators.setConfigFormat("custom");
        generators.setTemplateName("fair-scheduler.ftl");

        ArrayList<ServiceConfig> serviceConfigs = new ArrayList<>();
        ServiceConfig config = new ServiceConfig();
        ArrayList<JSONObject> queueList = new ArrayList<>();
        for (ClusterYarnQueueEntity clusterYarnQueueEntity : list) {
            JSONObject queue = new JSONObject();
            Integer minMem = clusterYarnQueueEntity.getMinMem() * 1024;
            Integer maxMem = clusterYarnQueueEntity.getMaxMem() * 1024;
            clusterYarnQueueEntity.setMinResources(minMem + "mb," + clusterYarnQueueEntity.getMinCore() + "vcores");
            clusterYarnQueueEntity.setMaxResources(maxMem + "mb," + clusterYarnQueueEntity.getMaxCore() + "vcores");
            BeanUtil.copyProperties(clusterYarnQueueEntity, queue, false);
            queueList.add(queue);
        }
        config.setName("queueList");
        config.setValue(queueList);
        config.setConfigType("map");
        config.setRequired(true);
        serviceConfigs.add(config);

        configFileMap.put(generators, serviceConfigs);
        String hostname = "";
        for (ClusterServiceRoleInstanceDTO roleInstanceDto : roleList) {
            // 调用指令刷新yarn队列配置
            ServiceRoleInfo serviceRoleInfo = new ServiceRoleInfo();
            serviceRoleInfo.setName("ResourceManager");
            serviceRoleInfo.setParentName("YARN");
            serviceRoleInfo.setConfigFileMap(configFileMap);
            serviceRoleInfo.setDecompressPackageName("hadoop-3.3.3");
            serviceRoleInfo.setHostname(roleInstanceDto.hostname());
            ServiceConfigureHandler configureHandler = new ServiceConfigureHandler();
            ExecResult execResult = configureHandler.handlerRequest(serviceRoleInfo);
            if (!execResult.getExecResult()) {
                throw new BusinessException(Status.FAILED_REFRESH_THE_QUEUE_TO_YARN.getCode(),
                        Status.FAILED_REFRESH_THE_QUEUE_TO_YARN.getMsg());
            }
            if (StringUtils.isBlank(hostname)) {
                hostname = roleInstanceDto.hostname();
            }
        }
        ExecuteCmdCommand command = new ExecuteCmdCommand();
        ArrayList<String> commands = new ArrayList<>();
        commands.add(Constants.INSTALL_PATH + "/hadoop-3.3.3/bin/yarn");
        commands.add("rmadmin");
        commands.add("-refreshQueues");
        command.setCommands(commands);
        
        // 使用HTTP方式提交任务到Worker
        ExecResult execResult = WorkerTaskHelper.submitAndWait(hostname, command, 180);
        if (execResult.getExecResult()) {
            logger.info("yarn dfsadmin -refreshQueues success at {}", hostname);
        } else {
            logger.info(execResult.getExecOut());
            throw new BusinessException(Status.FAILED_REFRESH_THE_QUEUE_TO_YARN.getCode(),
                    Status.FAILED_REFRESH_THE_QUEUE_TO_YARN.getMsg());
        }
    }

    @Override
    public ClusterYarnQueueDTO getQueueByName(Long clusterId, String queueName) {
        ClusterYarnQueueEntity entity = clusterYarnQueueMapper.selectByClusterIdAndQueueName(clusterId, queueName);
        return Objects.nonNull(entity) ? clusterYarnQueueConverter.entityToDto(entity) : null;
    }

    @Override
    public ClusterYarnQueueDTO getByIdAsDto(Long id) {
        ClusterYarnQueueEntity entity = getById(id);
        return Objects.nonNull(entity) ? clusterYarnQueueConverter.entityToDto(entity) : null;
    }

    @Override
    public List<ClusterYarnQueueDTO> getQueuesByClusterId(Long clusterId) {
        // SQL逻辑已在DAO层，直接调用Mapper方法
        List<ClusterYarnQueueEntity> entities = getMapper().selectByClusterId(clusterId);
        return clusterYarnQueueConverter.entityListToDtoList(entities);
    }

    @Override
    public ClusterYarnQueueDTO updateQueue(ClusterYarnQueueDTO dto) throws BusinessException {
        ClusterYarnQueueEntity entity = clusterYarnQueueConverter.dtoToEntity(dto);
        updateById(entity);
        return clusterYarnQueueConverter.entityToDto(entity);
    }

    @Override
    public boolean deleteQueue(Long id) {
        return removeById(id);
    }
}
