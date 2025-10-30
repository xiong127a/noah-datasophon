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

import com.datasophon.api.converter.ClusterServiceRoleGroupConfigConverter;
import com.datasophon.api.load.GlobalVariables;
import com.datasophon.api.master.CancelCommandMap;
import com.datasophon.api.service.*;
import com.datasophon.api.utils.ConfigGroupUtils;
import com.datasophon.api.utils.ProcessUtils;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.command.ExecuteServiceRoleCommand;
import com.datasophon.common.dto.ClusterServiceRoleGroupConfigDTO;
import com.datasophon.common.dto.ClusterServiceRoleInstanceDTO;
import com.datasophon.common.enums.*;
import com.datasophon.common.model.Generators;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.model.ServiceRoleInfo;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.dao.entity.ClusterServiceRoleGroupConfigEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Master服务执行服务实现
 * 替代MasterServiceActor，处理服务角色的执行命令（安装、启动、停止、重启等）
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-10-30
 */
@Service
public class MasterServiceExecutionServiceImpl implements MasterServiceExecutionService {

    private static final Logger logger = LoggerFactory.getLogger(MasterServiceExecutionServiceImpl.class);

    @Autowired
    private ClusterServiceRoleGroupConfigService roleGroupConfigService;

    @Autowired
    private ClusterServiceRoleInstanceService roleInstanceService;

    @Autowired
    private ServiceInstallationService serviceInstallationService;

    @Autowired
    private CommandExecutionService commandExecutionService;

    @Autowired
    private ServiceStateManagementService serviceStateManagementService;

    @Autowired
    private ClusterServiceRoleGroupConfigConverter configConverter;

    @Override
    @Async("taskExecutor")
    public void executeServiceRoleCommand(ExecuteServiceRoleCommand command) {
        try {
            logger.info("MasterServiceExecutionService 接收到命令: commandType={}, serviceName={}", 
                    command.getCommandType(), 
                    command.getServiceName());

            List<ServiceRoleInfo> serviceRoleInfoList = command.getMasterRoles();
            if (serviceRoleInfoList == null || serviceRoleInfoList.isEmpty()) {
                logger.warn("服务角色列表为空");
                return;
            }

            // 排序
            Collections.sort(serviceRoleInfoList);

            int successNum = 0;
            for (ServiceRoleInfo serviceRoleInfo : serviceRoleInfoList) {
                logger.info("{} 服务角色数量为 {}", serviceRoleInfo.getName(), serviceRoleInfoList.size());

                // 检查是否已取消
                if (CancelCommandMap.exists(serviceRoleInfo.getHostCommandId())) {
                    logger.info("命令已取消: {}", serviceRoleInfo.getHostCommandId());
                    continue;
                }

                try {
                    // 准备服务角色配置
                    prepareServiceRoleConfig(serviceRoleInfo, command);

                    // 执行命令
                    ExecResult execResult = executeCommand(serviceRoleInfo, command);

                    // 处理执行结果
                    if (execResult != null && execResult.getExecResult()) {
                        successNum++;
                        handleSuccess(serviceRoleInfo, command, successNum, serviceRoleInfoList.size());
                    } else {
                        handleFailure(serviceRoleInfo, command);
                    }
                } catch (Exception e) {
                    logger.error("执行服务角色命令失败: {} on {}", 
                            serviceRoleInfo.getName(), 
                            serviceRoleInfo.getHostname(), e);
                    handleFailure(serviceRoleInfo, command);
                }
            }

            logger.info("命令执行完成，成功数量: {}/{}", successNum, serviceRoleInfoList.size());

        } catch (Exception e) {
            logger.error("处理ExecuteServiceRoleCommand消息时出错", e);
        }
    }

    /**
     * 准备服务角色配置
     */
    private void prepareServiceRoleConfig(ServiceRoleInfo serviceRoleInfo, ExecuteServiceRoleCommand command) {
        Long serviceInstanceId = serviceRoleInfo.getServiceInstanceId();
        ClusterServiceRoleInstanceDTO serviceRoleInstance = roleInstanceService.getOneServiceRole(
                serviceRoleInfo.getName(),
                serviceRoleInfo.getHostname(),
                serviceRoleInfo.getClusterId());

        HashMap<Generators, List<ServiceConfig>> configFileMap = new HashMap<>();
        boolean enableRangerPlugin = isEnableRangerPlugin(
                serviceRoleInfo.getClusterId(), serviceRoleInfo.getParentName());
        boolean needReConfig = false;

        // 根据命令类型准备配置
        if (command.getCommandType() == CommandType.INSTALL_SERVICE) {
            // 安装时获取角色组配置
            Long roleGroupId = (Long) CacheUtils.get("UseRoleGroup_" + serviceInstanceId);
            if (roleGroupId != null) {
                ClusterServiceRoleGroupConfigDTO configDto = roleGroupConfigService.getConfigByRoleGroupId(roleGroupId);
                if (configDto != null) {
                    ClusterServiceRoleGroupConfigEntity config = configConverter.dtoToEntity(configDto);
                    ConfigGroupUtils.generateConfigFileMap(configFileMap, config, serviceRoleInfo.getClusterId());
                }
            }
        } else if (serviceRoleInstance != null && 
                   Objects.equals(NeedRestart.YES.getValue(), serviceRoleInstance.needRestart())) {
            // 需要重启时重新生成配置
            ClusterServiceRoleGroupConfigDTO configDto = roleGroupConfigService.getConfigByRoleGroupId(
                    serviceRoleInstance.roleGroupId());
            if (configDto != null) {
                ClusterServiceRoleGroupConfigEntity config = configConverter.dtoToEntity(configDto);
                ConfigGroupUtils.generateConfigFileMap(configFileMap, config, serviceRoleInfo.getClusterId());
                needReConfig = true;
            }
        }

        logger.debug("enableRangerPlugin={}, needReConfig={}", enableRangerPlugin, needReConfig);
        serviceRoleInfo.setConfigFileMap(configFileMap);
        serviceRoleInfo.setEnableRangerPlugin(enableRangerPlugin);
        serviceRoleInfo.setNeedReConfig(needReConfig);
    }

    /**
     * 执行命令
     */
    private ExecResult executeCommand(ServiceRoleInfo serviceRoleInfo, ExecuteServiceRoleCommand command) {
        CommandType commandType = command.getCommandType();
        boolean needReConfig = serviceRoleInfo.isNeedReConfig();

        return switch (commandType) {
            case INSTALL_SERVICE -> {
                logger.info("开始安装 {} 在主机 {}", serviceRoleInfo.getName(), serviceRoleInfo.getHostname());
                yield serviceInstallationService.startInstallService(serviceRoleInfo);
            }
            case START_SERVICE -> {
                logger.info("开始启动 {} 在主机 {}", serviceRoleInfo.getName(), serviceRoleInfo.getHostname());
                yield serviceInstallationService.startService(serviceRoleInfo, needReConfig);
            }
            case STOP_SERVICE -> {
                logger.info("开始停止 {} 在主机 {}", serviceRoleInfo.getName(), serviceRoleInfo.getHostname());
                yield serviceInstallationService.stopService(serviceRoleInfo);
            }
            case RESTART_SERVICE -> {
                logger.info("开始重启 {} 在主机 {}", serviceRoleInfo.getName(), serviceRoleInfo.getHostname());
                yield serviceInstallationService.restartService(serviceRoleInfo, needReConfig);
            }
            default -> {
                logger.warn("未知的命令类型: {}", commandType);
                yield null;
            }
        };
    }

    /**
     * 处理成功情况
     */
    private void handleSuccess(ServiceRoleInfo serviceRoleInfo, ExecuteServiceRoleCommand command, 
                                int successNum, int totalNum) {
        CommandType commandType = command.getCommandType();
        
        // 记录成功日志
        logger.info("{} {} 成功在 {}", 
                serviceRoleInfo.getName(), 
                getCommandTypeDesc(commandType), 
                serviceRoleInfo.getHostname());

        // 更新服务角色状态
        if (commandType == CommandType.START_SERVICE || commandType == CommandType.RESTART_SERVICE) {
            serviceStateManagementService.updateServiceRoleState(
                    commandType,
                    serviceRoleInfo.getName(),
                    serviceRoleInfo.getHostname(),
                    command.getClusterId(),
                    ServiceRoleState.RUNNING);
        } else if (commandType == CommandType.STOP_SERVICE) {
            serviceStateManagementService.updateServiceRoleState(
                    commandType,
                    serviceRoleInfo.getName(),
                    serviceRoleInfo.getHostname(),
                    command.getClusterId(),
                    ServiceRoleState.STOP);
        } else if (commandType == CommandType.INSTALL_SERVICE) {
            serviceInstallationService.saveServiceInstallInfo(serviceRoleInfo);
        }

        // 当所有MASTER角色成功时，通知命令执行器
        if (ServiceRoleType.MASTER.equals(serviceRoleInfo.getRoleType()) && successNum == totalNum) {
            logger.info("所有MASTER角色 {} 成功", getCommandTypeDesc(commandType));
            commandExecutionService.tellCommandActorResult(
                    serviceRoleInfo.getParentName(),
                    command,
                    ServiceExecuteState.SUCCESS);
        }
    }

    /**
     * 处理失败情况
     */
    private void handleFailure(ServiceRoleInfo serviceRoleInfo, ExecuteServiceRoleCommand command) {
        logger.error("{} {} 失败在 {}", 
                serviceRoleInfo.getName(), 
                getCommandTypeDesc(command.getCommandType()), 
                serviceRoleInfo.getHostname());

        // 只有MASTER角色失败时才通知命令执行器
        if (ServiceRoleType.MASTER.equals(serviceRoleInfo.getRoleType())) {
            commandExecutionService.tellCommandActorResult(
                    serviceRoleInfo.getParentName(),
                    command,
                    ServiceExecuteState.ERROR);
        }
    }

    /**
     * 获取命令类型描述
     */
    private String getCommandTypeDesc(CommandType commandType) {
        return switch (commandType) {
            case INSTALL_SERVICE -> "安装";
            case START_SERVICE -> "启动";
            case STOP_SERVICE -> "停止";
            case RESTART_SERVICE -> "重启";
            default -> commandType.toString();
        };
    }

    /**
     * 检查是否启用Ranger插件
     */
    private boolean isEnableRangerPlugin(Long clusterId, String serviceName) {
        Map<String, String> variables = GlobalVariables.get(clusterId);
        if (variables.containsKey("enableRangerPlugin")) {
            return "true".equals(variables.get("enableRangerPlugin"));
        }
        return false;
    }
}

