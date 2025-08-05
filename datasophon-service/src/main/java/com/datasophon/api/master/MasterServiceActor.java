/*
 *
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
 *
 */

package com.datasophon.api.master;

import org.apache.pekko.actor.AbstractActor;

import cn.hutool.extra.spring.SpringUtil;
import com.datasophon.api.load.GlobalVariables;
import com.datasophon.api.service.ClusterServiceRoleGroupConfigService;
import com.datasophon.api.service.ClusterServiceRoleInstanceService;
import com.datasophon.api.utils.ProcessUtils;
import com.datasophon.api.service.ServiceInstallationService;
import com.datasophon.api.service.CommandExecutionService;
import com.datasophon.api.service.ServiceStateManagementService;
import com.datasophon.api.utils.ConfigGroupUtils;
import com.datasophon.api.converter.ClusterServiceRoleGroupConfigConverter;
import com.datasophon.common.dto.ClusterServiceRoleGroupConfigDTO;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.command.ExecuteServiceRoleCommand;
import com.datasophon.common.enums.CommandType;
import com.datasophon.common.enums.ServiceExecuteState;
import com.datasophon.common.enums.ServiceRoleType;
import com.datasophon.common.model.Generators;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.model.ServiceRoleInfo;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.dao.entity.ClusterServiceRoleGroupConfig;
import com.datasophon.common.dto.ClusterServiceRoleInstanceDTO;
import com.datasophon.dao.enums.NeedRestart;
import com.datasophon.dao.enums.ServiceRoleState;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MasterServiceActor extends AbstractActor {

    private static final Logger logger = LoggerFactory.getLogger(MasterServiceActor.class);

    @Override
    public void postStop() {
        logger.info("{} service actor stopped ", getSelf().path().toString());
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(ExecuteServiceRoleCommand.class, this::handleExecuteServiceRoleCommand)
                .matchAny(this::unhandled)
                .build();
    }

    private void handleExecuteServiceRoleCommand(ExecuteServiceRoleCommand executeServiceRoleCommand) {
        try {
            ClusterServiceRoleGroupConfigService roleGroupConfigService = SpringUtil
                    .getBean(ClusterServiceRoleGroupConfigService.class);
            ClusterServiceRoleInstanceService roleInstanceService = SpringUtil
                    .getBean(ClusterServiceRoleInstanceService.class);

            List<ServiceRoleInfo> serviceRoleInfoList = executeServiceRoleCommand.getMasterRoles();
            Collections.sort(serviceRoleInfoList); // 排序

            int successNum = 0;
            for (ServiceRoleInfo serviceRoleInfo : serviceRoleInfoList) {
                logger.info(
                        "{} service role size is {}",
                        serviceRoleInfo.getName(),
                        serviceRoleInfoList.size());
                if (CancelCommandMap.exists(serviceRoleInfo.getHostCommandId())) {
                    continue;
                }
                new ExecResult();
                ExecResult execResult;
                Integer serviceInstanceId = serviceRoleInfo.getServiceInstanceId();
                ClusterServiceRoleInstanceDTO serviceRoleInstance = roleInstanceService.getOneServiceRole(
                        serviceRoleInfo.getName(),
                        serviceRoleInfo.getHostname(),
                        serviceRoleInfo.getClusterId());
                HashMap<Generators, List<ServiceConfig>> configFileMap = new HashMap<>();
                boolean enableRangerPlugin = isEnableRangerPlugin(
                        serviceRoleInfo.getClusterId(), serviceRoleInfo.getParentName());
                boolean needReConfig = false;
                if (executeServiceRoleCommand.getCommandType() == CommandType.INSTALL_SERVICE) {
                    Integer roleGroupId = (Integer) CacheUtils.get("UseRoleGroup_" + serviceInstanceId);
                    ClusterServiceRoleGroupConfigDTO configDto = roleGroupConfigService.getConfigByRoleGroupId(roleGroupId);
                    // 使用MapStruct Converter进行转换 - 符合架构规范
                    ClusterServiceRoleGroupConfigConverter converter = SpringUtil.getBean(ClusterServiceRoleGroupConfigConverter.class);
                    ClusterServiceRoleGroupConfig config = converter.dtoToEntity(configDto);
                    // TODO 获取角色组配置
                    ConfigGroupUtils.generateConfigFileMap(configFileMap, config, serviceRoleInfo.getClusterId());
                } else if (Objects.equals(NeedRestart.YES.getValue(), serviceRoleInstance.needRestart())) {
                    ClusterServiceRoleGroupConfigDTO configDto = roleGroupConfigService.getConfigByRoleGroupId(
                            serviceRoleInstance.roleGroupId());
                    // 使用MapStruct Converter进行转换 - 符合架构规范
                    ClusterServiceRoleGroupConfigConverter converter = SpringUtil.getBean(ClusterServiceRoleGroupConfigConverter.class);
                    ClusterServiceRoleGroupConfig config = converter.dtoToEntity(configDto);
                    ConfigGroupUtils.generateConfigFileMap(configFileMap, config, serviceRoleInfo.getClusterId());
                    needReConfig = true;
                }
                logger.info("enable ranger plugin is {}, needReConfig is {}", enableRangerPlugin, needReConfig);
                serviceRoleInfo.setConfigFileMap(configFileMap);
                serviceRoleInfo.setEnableRangerPlugin(enableRangerPlugin);

                switch (executeServiceRoleCommand.getCommandType()) {
                    case INSTALL_SERVICE:
                        try {
                            logger.info(
                                    "start to install {} in host {}",
                                    serviceRoleInfo.getName(),
                                    serviceRoleInfo.getHostname());

                            ServiceInstallationService serviceInstallationService = SpringUtil.getBean(ServiceInstallationService.class);
                            execResult = serviceInstallationService.startInstallService(serviceRoleInfo);
                            if (Objects.nonNull(execResult) && execResult.getExecResult()) {
                                serviceInstallationService.saveServiceInstallInfo(serviceRoleInfo);
                                successNum += 1;
                                if (ServiceRoleType.MASTER.equals(serviceRoleInfo.getRoleType())
                                        && successNum == serviceRoleInfoList.size()) {
                                    logger.info("all master role has installed");
                                    CommandExecutionService commandExecutionService = SpringUtil.getBean(CommandExecutionService.class);
                                    commandExecutionService.tellCommandActorResult(
                                            serviceRoleInfo.getParentName(),
                                            executeServiceRoleCommand,
                                            ServiceExecuteState.SUCCESS);
                                }
                                logger.info(
                                        "{} install success in {}",
                                        serviceRoleInfo.getName(),
                                        serviceRoleInfo.getHostname());
                            } else {
                                if (ServiceRoleType.MASTER.equals(serviceRoleInfo.getRoleType())) {
                                    logger.info(
                                            "{} install failed in {}",
                                            serviceRoleInfo.getName(),
                                            serviceRoleInfo.getHostname());
                                    CommandExecutionService commandExecutionService = SpringUtil.getBean(CommandExecutionService.class);
                                    commandExecutionService.tellCommandActorResult(
                                            serviceRoleInfo.getParentName(),
                                            executeServiceRoleCommand,
                                            ServiceExecuteState.ERROR);
                                }
                            }

                        } catch (Exception e) {
                            logger.info(
                                    "{} install failed in {}",
                                    serviceRoleInfo.getName(),
                                    serviceRoleInfo.getHostname());
                            CommandExecutionService commandExecutionService = SpringUtil.getBean(CommandExecutionService.class);
                            commandExecutionService.tellCommandActorResult(
                                    serviceRoleInfo.getParentName(),
                                    executeServiceRoleCommand,
                                    ServiceExecuteState.ERROR);
                            logger.error(ProcessUtils.getExceptionMessage(e));
                        }
                        break;
                    case START_SERVICE:
                        try {
                            logger.info(
                                    "start  {} in host {}",
                                    serviceRoleInfo.getName(),
                                    serviceRoleInfo.getHostname());
                            ServiceInstallationService serviceInstallationService = SpringUtil.getBean(ServiceInstallationService.class);
                            execResult = serviceInstallationService.startService(serviceRoleInfo, needReConfig);
                            if (Objects.nonNull(execResult) && execResult.getExecResult()) {
                                successNum += 1;
                                if (ServiceRoleType.MASTER.equals(serviceRoleInfo.getRoleType())
                                        && successNum == serviceRoleInfoList.size()) {
                                    logger.info(
                                            "{} start success", serviceRoleInfo.getParentName());
                                    CommandExecutionService commandExecutionService = SpringUtil.getBean(CommandExecutionService.class);
                                    commandExecutionService.tellCommandActorResult(
                                            serviceRoleInfo.getParentName(),
                                            executeServiceRoleCommand,
                                            ServiceExecuteState.SUCCESS);
                                }
                                // update service role state is running
                                ServiceStateManagementService serviceStateManagementService = SpringUtil.getBean(ServiceStateManagementService.class);
                                serviceStateManagementService.updateServiceRoleState(
                                        CommandType.START_SERVICE,
                                        serviceRoleInfo.getName(),
                                        serviceRoleInfo.getHostname(),
                                        executeServiceRoleCommand.getClusterId(),
                                        ServiceRoleState.RUNNING);
                            } else {
                                if (ServiceRoleType.MASTER.equals(serviceRoleInfo.getRoleType())) {
                                    logger.info("{} start failed", serviceRoleInfo.getParentName());
                                    CommandExecutionService commandExecutionService = SpringUtil.getBean(CommandExecutionService.class);
                                    commandExecutionService.tellCommandActorResult(
                                            serviceRoleInfo.getParentName(),
                                            executeServiceRoleCommand,
                                            ServiceExecuteState.ERROR);
                                }
                            }
                        } catch (Exception e) {
                            CommandExecutionService commandExecutionService = SpringUtil.getBean(CommandExecutionService.class);
                            commandExecutionService.tellCommandActorResult(
                                    serviceRoleInfo.getParentName(),
                                    executeServiceRoleCommand,
                                    ServiceExecuteState.ERROR);
                            logger.error(ProcessUtils.getExceptionMessage(e));
                        }
                        break;
                    case STOP_SERVICE:
                        try {
                            logger.info(
                                    "stop {} in host {}",
                                    serviceRoleInfo.getName(),
                                    serviceRoleInfo.getHostname());
                            ServiceInstallationService serviceInstallationService = SpringUtil.getBean(ServiceInstallationService.class);
                            execResult = serviceInstallationService.stopService(serviceRoleInfo);
                            if (Objects.nonNull(execResult) && execResult.getExecResult()) { // 执行成功
                                successNum += 1;
                                if (ServiceRoleType.MASTER.equals(serviceRoleInfo.getRoleType())
                                        && successNum == serviceRoleInfoList.size()) {
                                    logger.info("{} stop success", serviceRoleInfo.getParentName());
                                    CommandExecutionService commandExecutionService = SpringUtil.getBean(CommandExecutionService.class);
                                    commandExecutionService.tellCommandActorResult(
                                            serviceRoleInfo.getParentName(),
                                            executeServiceRoleCommand,
                                            ServiceExecuteState.SUCCESS);
                                }
                                // update service role state is stopped
                                ServiceStateManagementService serviceStateManagementService = SpringUtil.getBean(ServiceStateManagementService.class);
                                serviceStateManagementService.updateServiceRoleState(
                                        CommandType.STOP_SERVICE,
                                        serviceRoleInfo.getName(),
                                        serviceRoleInfo.getHostname(),
                                        executeServiceRoleCommand.getClusterId(),
                                        ServiceRoleState.STOP);
                            } else {

                                if (ServiceRoleType.MASTER.equals(serviceRoleInfo.getRoleType())) {
                                    logger.info("{} stop failed", serviceRoleInfo.getParentName());
                                    CommandExecutionService commandExecutionService = SpringUtil.getBean(CommandExecutionService.class);
                                    commandExecutionService.tellCommandActorResult(
                                            serviceRoleInfo.getParentName(),
                                            executeServiceRoleCommand,
                                            ServiceExecuteState.ERROR);
                                }
                            }
                        } catch (Exception e) {
                            CommandExecutionService commandExecutionService = SpringUtil.getBean(CommandExecutionService.class);
                            commandExecutionService.tellCommandActorResult(
                                    serviceRoleInfo.getParentName(),
                                    executeServiceRoleCommand,
                                    ServiceExecuteState.ERROR);
                            logger.error(ProcessUtils.getExceptionMessage(e));
                        }
                        break;
                    case RESTART_SERVICE:
                        try {
                            logger.info(
                                    "restart {} in host {}",
                                    serviceRoleInfo.getName(),
                                    serviceRoleInfo.getHostname());
                            ServiceInstallationService serviceInstallationService = SpringUtil.getBean(ServiceInstallationService.class);
                            execResult = serviceInstallationService.restartService(serviceRoleInfo, needReConfig);
                            if (Objects.nonNull(execResult) && execResult.getExecResult()) {
                                successNum += 1;
                                if (ServiceRoleType.MASTER.equals(serviceRoleInfo.getRoleType())
                                        && successNum == serviceRoleInfoList.size()) {
                                    logger.info(
                                            "{} restart success", serviceRoleInfo.getParentName());
                                    CommandExecutionService commandExecutionService = SpringUtil.getBean(CommandExecutionService.class);
                                    commandExecutionService.tellCommandActorResult(
                                            serviceRoleInfo.getParentName(),
                                            executeServiceRoleCommand,
                                            ServiceExecuteState.SUCCESS);
                                }
                                // update service role state is running
                                ServiceStateManagementService serviceStateManagementService = SpringUtil.getBean(ServiceStateManagementService.class);
                                serviceStateManagementService.updateServiceRoleState(
                                        CommandType.RESTART_SERVICE,
                                        serviceRoleInfo.getName(),
                                        serviceRoleInfo.getHostname(),
                                        executeServiceRoleCommand.getClusterId(),
                                        ServiceRoleState.RUNNING);
                            } else {
                                if (ServiceRoleType.MASTER.equals(serviceRoleInfo.getRoleType())) {
                                    logger.info(
                                            "{} restart failed", serviceRoleInfo.getParentName());
                                    CommandExecutionService commandExecutionService = SpringUtil.getBean(CommandExecutionService.class);
                                    commandExecutionService.tellCommandActorResult(
                                            serviceRoleInfo.getParentName(),
                                            executeServiceRoleCommand,
                                            ServiceExecuteState.ERROR);
                                }
                            }
                        } catch (Exception e) {
                            CommandExecutionService commandExecutionService = SpringUtil.getBean(CommandExecutionService.class);
                            commandExecutionService.tellCommandActorResult(
                                    serviceRoleInfo.getParentName(),
                                    executeServiceRoleCommand,
                                    ServiceExecuteState.ERROR);
                            logger.error(ProcessUtils.getExceptionMessage(e));
                        }
                        break;
                    default:
                        break;
                }
            }
        } catch (Exception e) {
            logger.error("处理ExecuteServiceRoleCommand消息时出错", e);
        }
    }

    private boolean isEnableRangerPlugin(Integer clusterId, String serviceName) {
        Map<String, String> variables = GlobalVariables.get(clusterId);
        if (variables.containsKey("enableRangerPlugin")) {
            return "true".equals(variables.get("enableRangerPlugin"));
        }
        return false;
    }


}
