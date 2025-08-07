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

package com.datasophon.api.master;

import cn.hutool.extra.spring.SpringUtil;
import com.datasophon.api.service.ClusterServiceRoleGroupConfigService;
import com.datasophon.api.service.ClusterServiceRoleInstanceService;
import com.datasophon.api.service.ServiceInstallationService;
import com.datasophon.api.service.ServiceStateManagementService;
import com.datasophon.api.service.CommandExecutionService;
import com.datasophon.api.utils.RollingRestartUtils;
import com.datasophon.api.utils.ConfigGroupUtils;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.command.ExecuteServiceRoleCommand;
import com.datasophon.common.enums.CommandType;
import com.datasophon.common.model.Generators;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.model.ServiceRoleInfo;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.dao.entity.ClusterServiceRoleGroupConfig;
import com.datasophon.common.dto.ClusterServiceRoleInstanceDTO;
import com.datasophon.common.dto.ClusterServiceRoleGroupConfigDTO;
import com.datasophon.api.converter.ClusterServiceRoleGroupConfigConverter;
import com.datasophon.common.enums.NeedRestart;
import com.datasophon.common.enums.ServiceRoleState;
import org.apache.pekko.actor.AbstractActor;
import org.apache.pekko.japi.pf.ReceiveBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Worker服务处理Actor
 * 负责处理各种服务操作命令，包括安装、启动、停止、重启等操作
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-05
 */
public class WorkerServiceActor extends AbstractActor {

    private static final Logger logger = LoggerFactory.getLogger(WorkerServiceActor.class);

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .match(ExecuteServiceRoleCommand.class, executeServiceRoleCommand -> {
                    ClusterServiceRoleGroupConfigService roleGroupConfigService = SpringUtil
                            .getBean(ClusterServiceRoleGroupConfigService.class);
                    ClusterServiceRoleInstanceService roleInstanceService = SpringUtil
                            .getBean(ClusterServiceRoleInstanceService.class);
                    ClusterServiceRoleGroupConfigConverter roleGroupConfigConverter = SpringUtil
                            .getBean(ClusterServiceRoleGroupConfigConverter.class);

                    ServiceRoleInfo serviceRoleInfo = executeServiceRoleCommand.getWorkerRole();
                    ExecResult execResult = new ExecResult();
                    Integer serviceInstanceId = serviceRoleInfo.getServiceInstanceId();
                    ClusterServiceRoleInstanceDTO serviceRoleInstance = roleInstanceService.getOneServiceRole(
                            serviceRoleInfo.getName(),
                            serviceRoleInfo.getHostname(),
                            serviceRoleInfo.getClusterId());
                    Map<Generators, List<ServiceConfig>> configFileMap = new HashMap<>();
                    boolean needReConfig = false;
                    if (executeServiceRoleCommand.getCommandType() == CommandType.INSTALL_SERVICE) {
                        Integer roleGroupId = (Integer) CacheUtils.get("UseRoleGroup_" + serviceInstanceId);
                        ClusterServiceRoleGroupConfigDTO configDto = roleGroupConfigService
                                .getConfigByRoleGroupId(roleGroupId);
                        ClusterServiceRoleGroupConfig config = roleGroupConfigConverter.dtoToEntity(configDto);
                        ConfigGroupUtils.generateConfigFileMap(configFileMap, config, serviceRoleInfo.getClusterId());
                    } else if (Objects.equals(NeedRestart.YES.getValue(), serviceRoleInstance.needRestart())) {
                        ClusterServiceRoleGroupConfigDTO configDto = roleGroupConfigService
                                .getConfigByRoleGroupId(serviceRoleInstance.roleGroupId());
                        ClusterServiceRoleGroupConfig config = roleGroupConfigConverter.dtoToEntity(configDto);
                        ConfigGroupUtils.generateConfigFileMap(configFileMap, config, serviceRoleInfo.getClusterId());
                        needReConfig = true;
                    }
                    serviceRoleInfo.setConfigFileMap(configFileMap);
                    serviceRoleInfo.setEnableRangerPlugin(false);
                    ServiceInstallationService serviceInstallationService = SpringUtil.getBean(ServiceInstallationService.class);
                    ServiceStateManagementService serviceStateManagementService = SpringUtil.getBean(ServiceStateManagementService.class);
                    CommandExecutionService commandExecutionService = SpringUtil.getBean(CommandExecutionService.class);
                    
                    switch (executeServiceRoleCommand.getCommandType()) {
                        case INSTALL_SERVICE:
                            try {
                                logger.info("start to install {} int host {}", serviceRoleInfo.getName(),
                                        serviceRoleInfo.getHostname());
                                execResult = serviceInstallationService.startInstallService(serviceRoleInfo);
                                if (Objects.nonNull(execResult) && execResult.getExecResult()) {
                                    // install success
                                    serviceInstallationService.saveServiceInstallInfo(serviceRoleInfo);
                                    logger.info("{} install success in {}", serviceRoleInfo.getName(),
                                            serviceRoleInfo.getHostname());
                                }
                            } catch (Exception e) {
                                logger.info("{} install failed in {}", serviceRoleInfo.getName(),
                                        serviceRoleInfo.getHostname());
                                logger.error("Error installing service: " + e.getMessage(), e);
                            }
                            break;
                        case START_SERVICE:
                            try {
                                logger.info("start  {} in host {}", serviceRoleInfo.getName(),
                                        serviceRoleInfo.getHostname());
                                execResult = serviceInstallationService.startService(serviceRoleInfo, needReConfig);
                                if (Objects.nonNull(execResult) && execResult.getExecResult()) {
                                    // 更新角色实例状态为正在运行
                                    serviceStateManagementService.updateServiceRoleState(CommandType.START_SERVICE,
                                            serviceRoleInfo.getName(),
                                            serviceRoleInfo.getHostname(),
                                            executeServiceRoleCommand.getClusterId(),
                                            ServiceRoleState.RUNNING);
                                }
                            } catch (Exception e) {
                                logger.error("Error starting service: " + e.getMessage(), e);
                            }
                            break;
                        case STOP_SERVICE:
                            try {
                                logger.info("stop {} in host {}", serviceRoleInfo.getName(),
                                        serviceRoleInfo.getHostname());
                                execResult = serviceInstallationService.stopService(serviceRoleInfo);
                                if (Objects.nonNull(execResult) && execResult.getExecResult()) {// 执行成功
                                    // 更新角色实例状态为停止
                                    serviceStateManagementService.updateServiceRoleState(CommandType.STOP_SERVICE,
                                            serviceRoleInfo.getName(),
                                            serviceRoleInfo.getHostname(),
                                            executeServiceRoleCommand.getClusterId(),
                                            ServiceRoleState.STOP);
                                }
                            } catch (Exception e) {
                                logger.error("Error starting service: " + e.getMessage(), e);
                            }
                            break;
                        case RESTART_SERVICE:
                            try {
                                logger.info("restart {} in host {}", serviceRoleInfo.getName(),
                                        serviceRoleInfo.getHostname());
                                execResult = serviceInstallationService.restartService(serviceRoleInfo, needReConfig);
                                if (Objects.nonNull(execResult) && execResult.getExecResult()) {
                                    // 更新角色实例状态为正在运行
                                    serviceStateManagementService.updateServiceRoleState(CommandType.RESTART_SERVICE,
                                            serviceRoleInfo.getName(),
                                            serviceRoleInfo.getHostname(), executeServiceRoleCommand.getClusterId(),
                                            ServiceRoleState.RUNNING);
                                }
                                RollingRestartUtils.updateStatus(serviceRoleInfo.getHostname() + serviceInstanceId,
                                        execResult.getExecResult());
                            } catch (Exception e) {
                                logger.error("Error starting service: " + e.getMessage(), e);
                            }
                            break;
                        default:
                            break;
                    }
                    commandExecutionService.handleCommandResult(serviceRoleInfo.getHostCommandId(), execResult.getExecResult(),
                            execResult.getExecOut());
                })
                .matchAny(this::unhandled)
                .build();
    }
}
