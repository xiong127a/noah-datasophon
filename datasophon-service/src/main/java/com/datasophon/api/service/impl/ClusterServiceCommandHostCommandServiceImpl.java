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

import com.datasophon.api.service.ClusterServiceCommandHostCommandService;
import com.datasophon.api.service.ClusterServiceCommandService;
import com.datasophon.api.converter.ClusterServiceCommandHostCommandConverter;
import com.datasophon.common.dto.ClusterServiceCommandHostCommandDTO;
import com.datasophon.dao.entity.ClusterServiceCommandHostCommandEntity;
import com.datasophon.common.enums.CommandState;
import com.datasophon.dao.mapper.ClusterServiceCommandHostCommandMapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.datasophon.common.model.PageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * 集群服务操作指令主机指令表实现
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-05
 */
@Service("clusterServiceCommandHostCommandService")
public class ClusterServiceCommandHostCommandServiceImpl extends ServiceImpl<ClusterServiceCommandHostCommandMapper, ClusterServiceCommandHostCommandEntity> implements ClusterServiceCommandHostCommandService {

    private static final Logger logger = LoggerFactory.getLogger(ClusterServiceCommandHostCommandServiceImpl.class);

    @Autowired
    private ClusterServiceCommandHostCommandConverter converter;

    @Autowired
    private ClusterServiceCommandService commandService;

    @Override
    public PageResult<ClusterServiceCommandHostCommandDTO> getHostCommandList(String hostname, String commandHostId,
            Integer page, Integer pageSize) {
        // 使用MyBatis-Flex分页查询 
        PageResult<ClusterServiceCommandHostCommandEntity> entityPageResult = 
                getMapper().selectPageByCommandHostId(commandHostId, page, pageSize);
        
        // 处理结果，确保状态和进度一致
        List<ClusterServiceCommandHostCommandEntity> entityRecords = entityPageResult.getRecords();
        entityRecords.forEach(this::updateCommandProgress);
        
        // 🔧 批量查询serviceName并设置到实体中
        populateServiceNames(entityRecords);
        
        // Entity列表转DTO列表 - JDK21特性
        var dtoList = entityRecords.stream()
                .map(converter::entityToDto)
                .toList();
        
        // 返回DTO分页结果
        return PageResult.of(dtoList, entityPageResult.getTotal(), page, pageSize);
    }
    
    /**
     * 批量查询并填充serviceName字段
     * 避免N+1查询问题
     */
    private void populateServiceNames(List<ClusterServiceCommandHostCommandEntity> hostCommands) {
        if (hostCommands == null || hostCommands.isEmpty()) {
            return;
        }
        
        try {
            // 提取所有的commandId（去重）
            var commandIds = hostCommands.stream()
                    .map(ClusterServiceCommandHostCommandEntity::getCommandId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            
            if (commandIds.isEmpty()) {
                return;
            }
            
            // 批量查询服务命令获取serviceName映射
            var serviceCommands = commandService.listByIds(commandIds);
            var serviceNameMap = serviceCommands.stream()
                    .collect(java.util.stream.Collectors.toMap(
                            cmd -> cmd.getId(),
                            cmd -> cmd.getServiceName() != null ? cmd.getServiceName() : ""
                    ));
            
            // 设置serviceName到主机命令实体
            hostCommands.forEach(hostCommand -> {
                Long commandId = hostCommand.getCommandId();
                if (commandId != null) {
                    String serviceName = serviceNameMap.get(commandId);
                    hostCommand.setServiceName(serviceName);
                }
            });
            
        } catch (Exception e) {
            logger.warn("批量查询serviceName时出错，将使用空值", e);
        }
    }

    /**
     * 更新命令的进度值，确保状态和进度一致
     * 
     * @param hostCommandEntity 主机命令实体
     */
    private void updateCommandProgress(ClusterServiceCommandHostCommandEntity hostCommandEntity) {
        try {
            // 确保状态和进度一致，完成或失败的命令应该显示100%
            boolean shouldUpdate = false;
            CommandState state = hostCommandEntity.getCommandState();
            if (CommandState.SUCCESS.equals(state) || 
                CommandState.FAILED.equals(state) || 
                CommandState.CANCEL.equals(state)) {
                shouldUpdate = hostCommandEntity.getCommandProgress() == null || 
                             hostCommandEntity.getCommandProgress() != 100;
            }
            
            if (shouldUpdate) {
                hostCommandEntity.setCommandProgress(100);
                updateById(hostCommandEntity);
            }
        } catch (Exception e) {
            logger.error("更新命令进度时出错", e);
        }
    }

    @Override
    public List<ClusterServiceCommandHostCommandDTO> getHostCommandListByCommandId(Long commandId) {
        // 使用Mapper查询并转换为DTO
        List<ClusterServiceCommandHostCommandEntity> entities = getMapper().selectByCommandId(commandId);
        return entities.stream()
                .map(converter::entityToDto)
                .toList();
    }

    @Override
    public ClusterServiceCommandHostCommandDTO getByHostCommandId(Long hostCommandId) {
        // 使用Mapper查询
        ClusterServiceCommandHostCommandEntity entity = getMapper().selectByHostCommandId(hostCommandId);
        return entity != null ? converter.entityToDto(entity) : null;
    }

    @Override
    public Long getHostCommandSizeByHostnameAndCommandHostId(String hostname, Long commandHostId) {
        // 调用Mapper方法统计数量
        return getMapper().countByCommandHostId(commandHostId);
    }

    @Override
    public Integer getHostCommandTotalProgressByHostnameAndCommandHostId(String hostname, Long commandHostId) {
        // 调用Mapper方法获取总进度
        return getMapper().getHostCommandTotalProgressByHostnameAndCommandHostId(hostname, commandHostId);
    }

    // 已删除，功能迁移到WebSocket Controller

    @Override
    public List<ClusterServiceCommandHostCommandDTO> findFailedHostCommand(String hostname, Long commandHostId) {
        // 调用Mapper方法查询失败的主机命令
        List<ClusterServiceCommandHostCommandEntity> entities = 
                getMapper().selectByCommandHostIdAndState(commandHostId, CommandState.FAILED);
        return entities.stream()
                .map(converter::entityToDto)
                .toList();
    }

    @Override
    public List<ClusterServiceCommandHostCommandDTO> findCanceledHostCommand(String hostname, Long commandHostId) {
        // 调用Mapper方法查询取消的主机命令
        List<ClusterServiceCommandHostCommandEntity> entities = 
                getMapper().selectByCommandHostIdAndState(commandHostId, CommandState.CANCEL);
        return entities.stream()
                .map(converter::entityToDto)
                .toList() ;
    }

    // DTO方法实现
    @Override
    public ClusterServiceCommandHostCommandDTO getByIdAsDto(String id) {
        ClusterServiceCommandHostCommandEntity entity = getById(id);
        return entity != null ? converter.entityToDto(entity) : null;
    }

    @Override
    public ClusterServiceCommandHostCommandDTO saveHostCommand(ClusterServiceCommandHostCommandDTO dto) {
        ClusterServiceCommandHostCommandEntity entity = converter.dtoToEntity(dto);
        save(entity);
        return converter.entityToDto(entity);
    }

    @Override
    public void updateHostCommand(ClusterServiceCommandHostCommandDTO dto) {
        ClusterServiceCommandHostCommandEntity entity = converter.dtoToEntity(dto);
        updateById(entity);
    }

    @Override
    public void updateByHostCommandId(ClusterServiceCommandHostCommandDTO hostCommandDTO) {
        // DTO转Entity
        var hostCommandEntity = converter.dtoToEntity(hostCommandDTO);
        
        // 调用Mapper方法更新
        getMapper().updateByHostCommandId(hostCommandEntity);
    }
}
