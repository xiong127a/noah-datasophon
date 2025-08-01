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
import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.utils.PropertyUtils;
import com.datasophon.dao.entity.ClusterInfoEntity;
import com.datasophon.dao.entity.ClusterServiceCommandEntity;
import com.datasophon.dao.entity.ClusterServiceCommandHostCommandEntity;
import com.datasophon.dao.enums.CommandState;
import com.datasophon.dao.mapper.ClusterServiceCommandHostCommandMapper;
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
 * @date 2024-12-19
 */
@Service("clusterServiceCommandHostCommandService")
public class ClusterServiceCommandHostCommandServiceImpl implements ClusterServiceCommandHostCommandService {

    private static final Logger logger = LoggerFactory.getLogger(ClusterServiceCommandHostCommandServiceImpl.class);
    private static final int DEFAULT_LOG_TIMEOUT_SECONDS = 30;
    private static final String AKKA_TCP_PREFIX = "akka.tcp://datasophon@";
    private static final String AKKA_USER_WORKER_PATH = "/user/worker/commandLogActor";
    private static final int MAXIMUM_LOG_LENGTH = 100000;

    @Autowired
    private ClusterServiceCommandHostCommandMapper hostCommandMapper;

    @Autowired
    private ClusterInfoService clusterInfoService;

    @Autowired
    private ClusterServiceCommandService commandService;

    @Override
    public PageResult<ClusterServiceCommandHostCommandEntity> getHostCommandList(String hostname, String commandHostId,
            Integer page, Integer pageSize) {
        // 使用mapper的分页查询方法
        PageResult<ClusterServiceCommandHostCommandEntity> pageResult = hostCommandMapper
                .selectPageByCommandHostId(commandHostId, page, pageSize);
        List<ClusterServiceCommandHostCommandEntity> list = pageResult.getRecords();

        // 处理结果
        for (ClusterServiceCommandHostCommandEntity hostCommandEntity : list) {
            hostCommandEntity.setCommandStateCode(hostCommandEntity.getCommandState().getValue());

            // 确保已完成/失败命令有正确的进度显示
            updateCommandProgress(hostCommandEntity);
        }
        return pageResult;
    }

    /**
     * 更新命令的进度值，确保状态和进度一致
     * 
     * @param hostCommandEntity 主机命令实体
     */
    private void updateCommandProgress(ClusterServiceCommandHostCommandEntity hostCommandEntity) {
        try {
            // 确保状态和进度一致，完成或失败的命令应该显示100%
            if (CommandState.SUCCESS.equals(hostCommandEntity.getCommandState()) ||
                    CommandState.FAILED.equals(hostCommandEntity.getCommandState()) ||
                    CommandState.CANCEL.equals(hostCommandEntity.getCommandState())) {

                if (hostCommandEntity.getCommandProgress() == null || hostCommandEntity.getCommandProgress() != 100) {
                    hostCommandEntity.setCommandProgress(100);
                    hostCommandMapper.updateById(hostCommandEntity);
                }
            }
        } catch (Exception e) {
            logger.error("更新命令进度时出错", e);
        }
    }

    @Override
    public List<ClusterServiceCommandHostCommandEntity> getHostCommandListByCommandId(String commandId) {
        return hostCommandMapper.selectByCommandId(commandId);
    }

    @Override
    public ClusterServiceCommandHostCommandEntity getByHostCommandId(String hostCommandId) {
        return hostCommandMapper.selectByHostCommandId(hostCommandId);
    }

    @Override
    public void updateByHostCommandId(ClusterServiceCommandHostCommandEntity hostCommand) {
        hostCommandMapper.updateByHostCommandId(hostCommand);
    }

    @Override
    public Long getHostCommandSizeByHostnameAndCommandHostId(String hostname, String commandHostId) {
        return hostCommandMapper.countByCommandHostId(commandHostId);
    }

    @Override
    public Integer getHostCommandTotalProgressByHostnameAndCommandHostId(String hostname, String commandHostId) {
        return hostCommandMapper.getHostCommandTotalProgressByHostnameAndCommandHostId(hostname, commandHostId);
    }

    @Override
    public String getHostCommandLog(Integer clusterId, String hostCommandId) throws Exception {
        ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);
        ClusterServiceCommandHostCommandEntity hostCommand = hostCommandMapper.selectByHostCommandId(hostCommandId);

        if (hostCommand == null) {
            return "";
        }

        ClusterServiceCommandEntity commandEntity = commandService.getCommandById(hostCommand.getCommandId());

        ExecResult logResult = new ExecResult();
        String serviceName = commandEntity.getServiceName();
        String serviceRoleName = hostCommand.getServiceRoleName();
        String logFile = String.format("%s/%s/%s.log", "logs", serviceName, serviceRoleName);

        Timeout timeout = new Timeout(Duration.create(DEFAULT_LOG_TIMEOUT_SECONDS, TimeUnit.SECONDS));
        if (Constants.KUBERNETES_MODE.equals(clusterInfo.getDepType())) {
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
    public List<ClusterServiceCommandHostCommandEntity> findFailedHostCommand(String hostname, String commandHostId) {
        return hostCommandMapper.selectByCommandHostIdAndState(commandHostId, CommandState.FAILED);
    }

    @Override
    public List<ClusterServiceCommandHostCommandEntity> findCanceledHostCommand(String hostname, String commandHostId) {
        return hostCommandMapper.selectByCommandHostIdAndState(commandHostId, CommandState.CANCEL);
    }

    // 标准CRUD方法实现
    @Override
    public ClusterServiceCommandHostCommandEntity getById(String id) {
        return hostCommandMapper.selectById(id);
    }

    @Override
    public ClusterServiceCommandHostCommandEntity save(ClusterServiceCommandHostCommandEntity entity) {
        hostCommandMapper.insert(entity);
        return entity;
    }

    @Override
    public ClusterServiceCommandHostCommandEntity updateById(ClusterServiceCommandHostCommandEntity entity) {
        hostCommandMapper.updateById(entity);
        return entity;
    }

    @Override
    public boolean removeByIds(List<String> ids) {
        return hostCommandMapper.deleteByIds(ids) > 0;
    }

    @Override
    public List<ClusterServiceCommandHostCommandEntity> getAllHostCommands() {
        return hostCommandMapper.selectAll();
    }
}
