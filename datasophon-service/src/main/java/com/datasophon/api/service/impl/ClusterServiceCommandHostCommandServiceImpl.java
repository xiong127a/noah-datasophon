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

import org.apache.pekko.actor.ActorSelection;
import org.apache.pekko.pattern.Patterns;
import org.apache.pekko.util.Timeout;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datasophon.api.master.ActorUtils;
import com.datasophon.api.service.*;
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

    @Autowired
    ClusterServiceCommandHostCommandMapper hostCommandMapper;

    @Autowired
    FrameServiceRoleService frameServiceRoleService;

    @Autowired
    FrameServiceService frameService;

    @Autowired
    ClusterInfoService clusterInfoService;

    @Autowired
    ClusterServiceCommandService commandService;

    @Override
    public Result getHostCommandList(String hostname, String commandHostId, Integer page, Integer pageSize) {
        int offset = (page - 1) * pageSize;
        List<ClusterServiceCommandHostCommandEntity> list = this
                .list(new QueryWrapper<ClusterServiceCommandHostCommandEntity>()
                        .eq(Constants.COMMAND_HOST_ID, commandHostId)
                        .orderByDesc(Constants.CREATE_TIME)
                        .last("limit " + offset + "," + pageSize));
        long total = this.count(new QueryWrapper<ClusterServiceCommandHostCommandEntity>()
                .eq(Constants.COMMAND_HOST_ID, commandHostId));
        for (ClusterServiceCommandHostCommandEntity hostCommandEntity : list) {
            hostCommandEntity.setCommandStateCode(hostCommandEntity.getCommandState().getValue());

            // 确保已完成/失败命令有正确的进度显示
            updateCommandProgress(hostCommandEntity);
        }
        return Result.success(list).put(Constants.TOTAL, total);
    }

    /**
     * 更新命令的进度值，确保状态和进度一致
     * 
     * @param hostCommandEntity 主机命令实体
     */
    private void updateCommandProgress(ClusterServiceCommandHostCommandEntity hostCommandEntity) {
        try {
            // 对所有最终状态的命令（成功、失败、取消），进度都应设为100%
            // 前端通过状态判断颜色（成功-绿色，失败-红色，取消-黄色）
            if ((CommandState.SUCCESS.equals(hostCommandEntity.getCommandState()) ||
                    CommandState.FAILED.equals(hostCommandEntity.getCommandState()) ||
                    CommandState.CANCEL.equals(hostCommandEntity.getCommandState())) &&
                    (hostCommandEntity.getCommandProgress() == null || hostCommandEntity.getCommandProgress() < 100)) {

                hostCommandEntity.setCommandProgress(100);

                // 更新数据库进度值
                this.updateById(hostCommandEntity);
                logger.info("命令 {} 状态为 {}，进度设为100%以保持一致性",
                        hostCommandEntity.getHostCommandId(),
                        hostCommandEntity.getCommandState().getDesc());
            }

            // 确保运行中的命令至少显示一些进度
            else if (CommandState.RUNNING.equals(hostCommandEntity.getCommandState())
                    && (hostCommandEntity.getCommandProgress() == null
                            || hostCommandEntity.getCommandProgress() == 0)) {
                hostCommandEntity.setCommandProgress(10);
                // 更新数据库进度值
                this.updateById(hostCommandEntity);
            }

            // 确保命令进度不为空
            if (hostCommandEntity.getCommandProgress() == null) {
                hostCommandEntity.setCommandProgress(0);
                // 更新数据库进度值
                this.updateById(hostCommandEntity);
            }
        } catch (Exception e) {
            logger.error("更新命令进度时出错: " + e.getMessage(), e);
            // 出错时确保至少有默认进度
            if (hostCommandEntity.getCommandProgress() == null) {
                hostCommandEntity.setCommandProgress(0);
            }
        }
    }

    @Override
    public List<ClusterServiceCommandHostCommandEntity> getHostCommandListByCommandId(String commandId) {
        return this.lambdaQuery().eq(ClusterServiceCommandHostCommandEntity::getCommandId, commandId).list();
    }

    @Override
    public ClusterServiceCommandHostCommandEntity getByHostCommandId(String hostCommandId) {
        return this.getOne(new QueryWrapper<ClusterServiceCommandHostCommandEntity>().eq(Constants.HOST_COMMAND_ID,
                hostCommandId));
    }

    @Override
    public void updateByHostCommandId(ClusterServiceCommandHostCommandEntity hostCommand) {
        this.update(hostCommand, new QueryWrapper<ClusterServiceCommandHostCommandEntity>()
                .eq(Constants.HOST_COMMAND_ID, hostCommand.getHostCommandId()));
    }

    @Override
    public Long getHostCommandSizeByHostnameAndCommandHostId(String hostname, String commandHostId) {
        long size = this.count(new QueryWrapper<ClusterServiceCommandHostCommandEntity>()
                .eq(Constants.HOSTNAME, hostname).eq(Constants.COMMAND_HOST_ID, commandHostId));
        return size;
    }

    @Override
    public Integer getHostCommandTotalProgressByHostnameAndCommandHostId(String hostname, String commandHostId) {
        return hostCommandMapper.getHostCommandTotalProgressByHostnameAndCommandHostId(hostname, commandHostId);
    }

    @Override
    public Result getHostCommandLog(Integer clusterId, String hostCommandId) throws Exception {
        ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);
        ClusterServiceCommandHostCommandEntity hostCommand = this
                .getOne(new QueryWrapper<ClusterServiceCommandHostCommandEntity>().eq(Constants.HOST_COMMAND_ID,
                        hostCommandId));
        ClusterServiceCommandEntity commandEntity = commandService.getCommandById(hostCommand.getCommandId());

        ExecResult logResult = new ExecResult();
        String serviceName = commandEntity.getServiceName();
        String serviceRoleName = hostCommand.getServiceRoleName();
        String logFile = String.format("%s/%s/%s.log", "logs", serviceName, serviceRoleName);

        Timeout timeout = new Timeout(Duration.create(60, TimeUnit.SECONDS));
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
                            "akka.tcp://datasophon@" + hostCommand.getHostname() + ":2552/user/worker/logActor");
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
        return this.list(new QueryWrapper<ClusterServiceCommandHostCommandEntity>()
                .eq(Constants.HOSTNAME, hostname)
                .eq(Constants.COMMAND_HOST_ID, commandHostId)
                .eq(Constants.COMMAND_STATE, CommandState.FAILED));
    }

    @Override
    public List<ClusterServiceCommandHostCommandEntity> findCanceledHostCommand(String hostname, String commandHostId) {
        return this.list(new QueryWrapper<ClusterServiceCommandHostCommandEntity>()
                .eq(Constants.HOSTNAME, hostname)
                .eq(Constants.COMMAND_HOST_ID, commandHostId)
                .eq(Constants.COMMAND_STATE, CommandState.CANCEL));
    }
}
