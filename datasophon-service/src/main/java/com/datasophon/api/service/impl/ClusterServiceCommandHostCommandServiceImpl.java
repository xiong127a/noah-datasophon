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

import com.datasophon.api.master.ActorUtils;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.api.service.ClusterServiceCommandHostCommandService;
import com.datasophon.api.service.ClusterServiceCommandService;
import com.datasophon.common.Constants;
import com.datasophon.common.command.GetLogCommand;
import com.datasophon.common.dto.ClusterServiceCommandDTO;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.utils.PropertyUtils;
import com.datasophon.api.converter.ClusterServiceCommandHostCommandConverter;
import com.datasophon.common.dto.ClusterServiceCommandHostCommandDTO;
import com.datasophon.dao.entity.ClusterInfoEntity;
import com.datasophon.dao.entity.ClusterServiceCommandHostCommandEntity;
import com.datasophon.common.enums.CommandState;
import com.datasophon.dao.mapper.ClusterServiceCommandHostCommandMapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.datasophon.kubernetes.util.KubernetesMinaUtils;
import com.datasophon.common.model.PageResult;
import org.apache.pekko.actor.ActorSelection;
import org.apache.pekko.pattern.Patterns;
import org.apache.pekko.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

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
    private static final int DEFAULT_LOG_TIMEOUT_SECONDS = 30;
    private static final String AKKA_TCP_PREFIX = "akka.tcp://datasophon@";
    private static final String AKKA_USER_WORKER_PATH = "/user/worker/commandLogActor";
    private static final int MAXIMUM_LOG_LENGTH = 100000;

    @Autowired
    private ClusterServiceCommandHostCommandConverter converter;

    @Autowired
    private ClusterInfoService clusterInfoService;

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
        
        // Entity列表转DTO列表 - JDK21特性
        var dtoList = entityRecords.stream()
                .map(converter::entityToDto)
                .toList();
        
        // 返回DTO分页结果
        return PageResult.of(dtoList, entityPageResult.getTotal(), page, pageSize);
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
    public List<ClusterServiceCommandHostCommandDTO> getHostCommandListByCommandId(String commandId) {
        // 使用Mapper查询并转换为DTO
        List<ClusterServiceCommandHostCommandEntity> entities = getMapper().selectByCommandId(commandId);
        return entities.stream()
                .map(converter::entityToDto)
                .toList();
    }

    @Override
    public ClusterServiceCommandHostCommandDTO getByHostCommandId(String hostCommandId) {
        // 使用Mapper查询
        ClusterServiceCommandHostCommandEntity entity = getMapper().selectByHostCommandId(hostCommandId);
        return entity != null ? converter.entityToDto(entity) : null;
    }

    @Override
    public Long getHostCommandSizeByHostnameAndCommandHostId(String hostname, String commandHostId) {
        // 调用Mapper方法统计数量
        return getMapper().countByCommandHostId(commandHostId);
    }

    @Override
    public Integer getHostCommandTotalProgressByHostnameAndCommandHostId(String hostname, String commandHostId) {
        // 调用Mapper方法获取总进度
        return getMapper().getHostCommandTotalProgressByHostnameAndCommandHostId(hostname, commandHostId);
    }

    @Override
    public String getHostCommandLog(Integer clusterId, String hostCommandId) throws Exception {
        ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);
        ClusterServiceCommandHostCommandEntity hostCommand = getMapper().selectByHostCommandId(hostCommandId);

        if (hostCommand == null) {
            return "";
        }

        ClusterServiceCommandDTO commandDto = commandService.getCommandById(hostCommand.getCommandId());

        ExecResult logResult = new ExecResult();
        String serviceName = commandDto.serviceName();
        String serviceRoleName = hostCommand.getServiceRoleName();
        String logFile = String.format("%s/%s/%s.log", "logs", serviceName, serviceRoleName);

        Timeout timeout = new Timeout(Duration.create(DEFAULT_LOG_TIMEOUT_SECONDS, TimeUnit.SECONDS));
        if (clusterInfo.getDepType() != null && clusterInfo.getDepType().isKubernetes()) {
            String baseDir = System.getProperty("user.dir");
            String logStr = KubernetesMinaUtils
                    .readLastRows(
                            baseDir + Constants.SLASH + logFile,
                            Charset.defaultCharset(), PropertyUtils.getInt("rows"));
            logResult.setExecResult(true);
            logResult.setExecOut(logStr);
        } else {
            GetLogCommand command = new GetLogCommand();
            command.setLogFile(logFile);
            command.setDecompressPackageName("datasophon-worker");
            logger.info("Start to get {} install log from host {}", serviceRoleName, hostCommand.getHostname());
            ActorSelection configActor = ActorUtils.actorSystem
                    .actorSelection(
                            AKKA_TCP_PREFIX + hostCommand.getHostname() + ":2552/user/worker/logActor");
            Future<Object> logFuture = Patterns.ask(configActor, command, timeout);
            logResult = (ExecResult) Await.result(logFuture, timeout.duration());
        }
        if (Objects.nonNull(logResult) && logResult.getExecResult()) {
            return logResult.getExecOut();
        }
        return "";
    }

    @Override
    public List<ClusterServiceCommandHostCommandDTO> findFailedHostCommand(String hostname, String commandHostId) {
        // 调用Mapper方法查询失败的主机命令
        List<ClusterServiceCommandHostCommandEntity> entities = 
                getMapper().selectByCommandHostIdAndState(commandHostId, CommandState.FAILED);
        return entities.stream()
                .map(converter::entityToDto)
                .toList();
    }

    @Override
    public List<ClusterServiceCommandHostCommandDTO> findCanceledHostCommand(String hostname, String commandHostId) {
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
