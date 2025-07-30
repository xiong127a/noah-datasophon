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
import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.ClusterInfoEntity;
import com.datasophon.dao.entity.ClusterServiceCommandEntity;
import com.datasophon.dao.entity.ClusterServiceCommandHostCommandEntity;
import com.datasophon.dao.enums.CommandState;
import com.datasophon.dao.mapper.ClusterServiceCommandHostCommandMapper;
import com.datasophon.kubernetes.util.KubernetesMinaUtils;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;
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

@Service("clusterServiceCommandHostCommandService")
public class ClusterServiceCommandHostCommandServiceImpl
        extends
        ServiceImpl<ClusterServiceCommandHostCommandMapper, ClusterServiceCommandHostCommandEntity>
        implements
        ClusterServiceCommandHostCommandService {

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
    public Result getHostCommandList(String hostname, String commandHostId, Integer page, Integer pageSize) {
        // 创建分页对象
        Page<ClusterServiceCommandHostCommandEntity> pagingRequest = new Page<>(page, pageSize);

        // 构建查询
        QueryChain<ClusterServiceCommandHostCommandEntity> query = QueryChain
                .of(ClusterServiceCommandHostCommandEntity.class)
                .where(ClusterServiceCommandHostCommandEntity::getCommandHostId).eq(commandHostId)
                .orderBy(ClusterServiceCommandHostCommandEntity::getCreateTime).desc();

        // 执行分页查询
        Page<ClusterServiceCommandHostCommandEntity> pageResult = query.page(pagingRequest);
        List<ClusterServiceCommandHostCommandEntity> list = pageResult.getRecords();
        long total = pageResult.getTotalRow();

        // 处理结果
        for (ClusterServiceCommandHostCommandEntity hostCommandEntity : list) {
            hostCommandEntity.setCommandStateCode(hostCommandEntity.getCommandState().getValue());

            // 确保已完成/失败命令有正确的进度显示
            updateCommandProgress(hostCommandEntity);
        }
        return Result.success(list,total);
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
                    updateById(hostCommandEntity);
                }
            }
        } catch (Exception e) {
            logger.error("更新命令进度时出错", e);
        }
    }

    @Override
    public List<ClusterServiceCommandHostCommandEntity> getHostCommandListByCommandId(String commandId) {
        return QueryChain.of(ClusterServiceCommandHostCommandEntity.class)
                .where(ClusterServiceCommandHostCommandEntity::getCommandId).eq(commandId)
                .list();
    }

    @Override
    public ClusterServiceCommandHostCommandEntity getByHostCommandId(String hostCommandId) {
        return QueryChain.of(ClusterServiceCommandHostCommandEntity.class)
                .where(ClusterServiceCommandHostCommandEntity::getHostCommandId).eq(hostCommandId)
                .one();
    }

    @Override
    public void updateByHostCommandId(ClusterServiceCommandHostCommandEntity hostCommand) {
        QueryChain<ClusterServiceCommandHostCommandEntity> updateCondition = QueryChain
                .of(ClusterServiceCommandHostCommandEntity.class)
                .where(ClusterServiceCommandHostCommandEntity::getHostCommandId).eq(hostCommand.getHostCommandId());
        this.update(hostCommand, updateCondition);
    }

    @Override
    public Long getHostCommandSizeByHostnameAndCommandHostId(String hostname, String commandHostId) {
        return QueryChain.of(ClusterServiceCommandHostCommandEntity.class)
                .where(ClusterServiceCommandHostCommandEntity::getCommandHostId).eq(commandHostId)
                .count();
    }

    @Override
    public Integer getHostCommandTotalProgressByHostnameAndCommandHostId(String hostname, String commandHostId) {
        return hostCommandMapper.getHostCommandTotalProgressByHostnameAndCommandHostId(hostname, commandHostId);
    }

    @Override
    public Result getHostCommandLog(Integer clusterId, String hostCommandId) throws Exception {
        ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);
        ClusterServiceCommandHostCommandEntity hostCommand = QueryChain.of(ClusterServiceCommandHostCommandEntity.class)
                .where(ClusterServiceCommandHostCommandEntity::getHostCommandId).eq(hostCommandId)
                .one();

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
            return Result.success(logResult.getExecOut());
        }
        return Result.success();
    }

    @Override
    public List<ClusterServiceCommandHostCommandEntity> findFailedHostCommand(String hostname, String commandHostId) {
        return QueryChain.of(ClusterServiceCommandHostCommandEntity.class)
                .where(ClusterServiceCommandHostCommandEntity::getCommandHostId).eq(commandHostId)
                .and(ClusterServiceCommandHostCommandEntity::getCommandState).eq(CommandState.FAILED)
                .list();
    }

    @Override
    public List<ClusterServiceCommandHostCommandEntity> findCanceledHostCommand(String hostname, String commandHostId) {
        return QueryChain.of(ClusterServiceCommandHostCommandEntity.class)
                .where(ClusterServiceCommandHostCommandEntity::getCommandHostId).eq(commandHostId)
                .and(ClusterServiceCommandHostCommandEntity::getCommandState).eq(CommandState.CANCEL)
                .list();
    }
}
