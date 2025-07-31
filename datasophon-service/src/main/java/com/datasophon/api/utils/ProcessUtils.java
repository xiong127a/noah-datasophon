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

package com.datasophon.api.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.datasophon.api.kubernetes.handler.KubernetesDeploymentYamlHandler;
import com.datasophon.api.kubernetes.handler.KubernetesHostTagHandler;
import com.datasophon.api.kubernetes.handler.KubernetesServiceConfigureHandler;
import com.datasophon.api.kubernetes.handler.KubernetesServiceInstallHandler;
import com.datasophon.api.kubernetes.handler.KubernetesServiceRoleStopHandler;
import com.datasophon.api.kubernetes.handler.KubernetesServiceScaleHandler;
import com.datasophon.api.kubernetes.handler.KubernetesServiceStartHandler;
import com.datasophon.api.load.GlobalVariables;
import com.datasophon.api.load.ServiceConfigMap;
import com.datasophon.api.master.ActorUtils;
import com.datasophon.api.master.CancelCommandMap;
import com.datasophon.api.master.MasterServiceActor;
import com.datasophon.api.master.ServiceCommandActor;
import com.datasophon.api.master.ServiceExecuteResultActor;
import com.datasophon.api.master.handler.service.ServiceConfigureHandler;
import com.datasophon.api.master.handler.service.ServiceHandler;
import com.datasophon.api.master.handler.service.ServiceInstallHandler;
import com.datasophon.api.master.handler.service.ServiceStartHandler;
import com.datasophon.api.master.handler.service.ServiceStopHandler;
import com.datasophon.api.service.*;
import com.datasophon.api.service.host.ClusterHostService;
import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.command.ExecuteCmdCommand;
import com.datasophon.common.command.ExecuteServiceRoleCommand;
import com.datasophon.common.command.FileOperateCommand;
import com.datasophon.common.enums.CommandType;
import com.datasophon.common.enums.ServiceExecuteState;
import com.datasophon.common.enums.ServiceRoleType;
import com.datasophon.common.model.DAGGraph;
import com.datasophon.common.model.ExternalLink;
import com.datasophon.common.model.Generators;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.model.ServiceExecuteResultMessage;
import com.datasophon.common.model.ServiceNode;
import com.datasophon.common.model.ServiceRoleInfo;
import com.datasophon.common.model.StartWorkerMessage;
import com.datasophon.common.model.UpdateCommandHostMessage;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.utils.HostUtils;
import com.datasophon.common.utils.PlaceholderUtils;
import com.datasophon.common.utils.PropertyUtils;
import com.datasophon.dao.entity.*;
import com.datasophon.dao.enums.AlertLevel;
import com.datasophon.dao.enums.CommandState;
import com.datasophon.dao.enums.NeedRestart;
import com.datasophon.dao.enums.RoleType;
import com.datasophon.dao.enums.ServiceRoleState;
import com.datasophon.dao.enums.ServiceState;
import com.datasophon.dao.enums.HostState;
import com.datasophon.dao.enums.MANAGED;
import com.mybatisflex.core.query.QueryChain;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSelection;
import org.apache.pekko.actor.Props;
import org.apache.pekko.pattern.Patterns;
import org.apache.pekko.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.datasophon.api.utils.CacheOperateUtils.putRemoteVariableCache;

public class ProcessUtils {

    private static final Logger logger = LoggerFactory.getLogger(ProcessUtils.class);

    public static void saveServiceInstallInfo(ServiceRoleInfo serviceRoleInfo) {
        ClusterServiceInstanceService serviceInstanceService = SpringUtil
                .getBean(ClusterServiceInstanceService.class);
        ClusterServiceInstanceConfigService serviceInstanceConfigService = SpringUtil
                .getBean(ClusterServiceInstanceConfigService.class);
        ClusterServiceRoleInstanceService serviceRoleInstanceService = SpringUtil
                .getBean(ClusterServiceRoleInstanceService.class);
        ClusterInfoService clusterInfoService = SpringUtil.getBean(ClusterInfoService.class);
        ClusterServiceRoleInstanceWebuisService webuisService = SpringUtil
                .getBean(ClusterServiceRoleInstanceWebuisService.class);
        ClusterServiceInstanceRoleGroupService roleGroupService = SpringUtil
                .getBean(ClusterServiceInstanceRoleGroupService.class);

        ClusterInfoEntity clusterInfo = clusterInfoService.getById(serviceRoleInfo.getClusterId());

        ClusterServiceInstanceEntity clusterServiceInstance = serviceInstanceService
                .getServiceInstanceByClusterIdAndServiceName(serviceRoleInfo.getClusterId(),
                        serviceRoleInfo.getParentName());
        if (Objects.isNull(clusterServiceInstance)) {
            clusterServiceInstance = new ClusterServiceInstanceEntity();
            clusterServiceInstance.setClusterId(serviceRoleInfo.getClusterId());
            clusterServiceInstance.setServiceName(serviceRoleInfo.getParentName());
            clusterServiceInstance.setServiceState(ServiceState.RUNNING);
            clusterServiceInstance.setCreateTime(new Date());
            clusterServiceInstance.setUpdateTime(new Date());
            serviceInstanceService.save(clusterServiceInstance);
            // save config
            List<ServiceConfig> list = ServiceConfigMap.get(clusterInfo.getClusterCode() + Constants.UNDERLINE
                    + serviceRoleInfo.getParentName() + Constants.CONFIG);
            String config = JSON.toJSONString(list);
            ClusterServiceInstanceConfigEntity clusterServiceInstanceConfig = new ClusterServiceInstanceConfigEntity();
            clusterServiceInstanceConfig.setClusterId(serviceRoleInfo.getClusterId());
            clusterServiceInstanceConfig.setServiceId(clusterServiceInstance.getId());
            clusterServiceInstanceConfig.setConfigJson(config);
            clusterServiceInstanceConfig.setConfigJsonMd5(SecureUtil.md5(config));
            clusterServiceInstanceConfig.setConfigVersion(1);
            clusterServiceInstanceConfig.setCreateTime(new Date());
            clusterServiceInstanceConfig.setUpdateTime(new Date());
            serviceInstanceConfigService.save(clusterServiceInstanceConfig);
        } else {
            clusterServiceInstance.setServiceState(ServiceState.RUNNING);
            clusterServiceInstance.setServiceStateCode(ServiceState.RUNNING.getValue());
            serviceInstanceService.updateById(clusterServiceInstance);
        }
        Integer roleGroupId = (Integer) CacheUtils.get("UseRoleGroup_" + clusterServiceInstance.getId());
        ClusterServiceInstanceRoleGroup roleGroup = roleGroupService.getById(roleGroupId);

        // save role instance
        ClusterServiceRoleInstanceEntity roleInstanceEntity = serviceRoleInstanceService
                .getOneServiceRole(serviceRoleInfo.getName(), serviceRoleInfo.getHostname(), clusterInfo.getId());
        if (Objects.isNull(roleInstanceEntity)) {
            ClusterServiceRoleInstanceEntity roleInstance = new ClusterServiceRoleInstanceEntity();
            roleInstance.setServiceId(clusterServiceInstance.getId());
            roleInstance.setRoleType(CommonUtils.convertRoleType(serviceRoleInfo.getRoleType().getName()));
            roleInstance.setCreateTime(new Date());
            roleInstance.setHostname(serviceRoleInfo.getHostname());
            roleInstance.setClusterId(serviceRoleInfo.getClusterId());
            roleInstance.setServiceRoleName(serviceRoleInfo.getName());
            roleInstance.setServiceRoleState(ServiceRoleState.RUNNING);
            roleInstance.setUpdateTime(new Date());
            roleInstance.setServiceName(serviceRoleInfo.getParentName());
            roleInstance.setRoleGroupId(roleGroup.getId());
            roleInstance.setNeedRestart(NeedRestart.NO);
            serviceRoleInstanceService.save(roleInstance);
            if (Constants.ZKSERVER.equalsIgnoreCase(roleInstance.getServiceRoleName())) {
                ClusterZkService clusterZkService = SpringUtil.getBean(ClusterZkService.class);
                ClusterZk clusterZk = new ClusterZk();
                clusterZk.setMyid((Integer) CacheUtils.get("zkserver_" + serviceRoleInfo.getHostname()));
                clusterZk.setClusterId(serviceRoleInfo.getClusterId());
                clusterZk.setZkServer(roleInstance.getHostname());
                clusterZkService.save(clusterZk);
            }

            if (Objects.nonNull(serviceRoleInfo.getExternalLink())) {
                ExternalLink externalLink = serviceRoleInfo.getExternalLink();
                Map<String, String> globalVariables = GlobalVariables.get(clusterInfo.getId());
                globalVariables.put("${hostname}", serviceRoleInfo.getHostname());
                String url = PlaceholderUtils.replacePlaceholders(externalLink.getUrl(), globalVariables,
                        Constants.REGEX_VARIABLE);
                Integer port = extractPortFromUrl(url);

                ClusterServiceRoleInstanceWebuis webui = webuisService.getRoleInstanceWebUi(roleInstance.getId());
                List<ClusterServiceRoleInstanceWebuis> clusterServiceRoleInstanceWebuis = webuisService
                        .listWebUisByServiceInstanceId(clusterServiceInstance.getId());
                if (Objects.nonNull(webui)) {
                    logger.info("web ui already exists");
                } else {
                    boolean foundPortMapping = false;

                    // 遍历配置映射查找端口映射
                    for (Map.Entry<Generators, List<ServiceConfig>> entry : serviceRoleInfo.getConfigFileMap()
                            .entrySet()) {
                        if (CollUtil.isEmpty(entry.getValue())) {
                            continue;
                        }

                        for (ServiceConfig serviceConfig : entry.getValue()) {
                            if (!serviceConfig.getName().endsWith("node_port_mappings")) {
                                continue;
                            }
                            List<Map<String, String>> portMappings = (List<Map<String, String>>) serviceConfig
                                    .getValue();

                            for (Map<String, String> portMapping : portMappings) {
                                String mappedPorts = portMapping.get(port);
                                if (mappedPorts == null) {
                                    continue;
                                }

                                for (String mappedPort : mappedPorts.split(",")) {
                                    for (ClusterServiceRoleInstanceWebuis clusterServiceRoleInstanceWebui : clusterServiceRoleInstanceWebuis) {
                                        if (clusterServiceRoleInstanceWebui.getWebUrl().contains(mappedPort)) {
                                            logger.info("web ui already exists");
                                            return;
                                        }
                                    }
                                    ClusterServiceRoleInstanceWebuis webuis = new ClusterServiceRoleInstanceWebuis();

                                    // 替换URL端口
                                    webuis.setWebUrl(replacePortInUrl(url, mappedPort));

                                    webuis.setServiceInstanceId(clusterServiceInstance.getId());
                                    webuis.setServiceRoleInstanceId(roleInstance.getId());
                                    webuis.setName(String.format("%s(%s)",
                                            externalLink.getName(),
                                            serviceRoleInfo.getHostname()));

                                    webuisService.save(webuis);
                                }
                            }
                            foundPortMapping = true;
                        }
                    }

                    // 如果没有找到端口映射，保存原始URL
                    if (!foundPortMapping) {
                        ClusterServiceRoleInstanceWebuis webuis = new ClusterServiceRoleInstanceWebuis();
                        webuis.setWebUrl(url);
                        webuis.setServiceInstanceId(clusterServiceInstance.getId());
                        webuis.setServiceRoleInstanceId(roleInstance.getId());
                        webuis.setName(String.format("%s(%s)",
                                externalLink.getName(),
                                serviceRoleInfo.getHostname()));
                        webuisService.save(webuis);
                    }

                    globalVariables.remove("${hostname}");
                }

            }
        }

    }

    public static void saveHostInstallInfo(StartWorkerMessage message, String clusterCode,
            ClusterHostService clusterHostService) {
        ClusterInfoService clusterInfoService = SpringUtil.getBean(ClusterInfoService.class);
        ClusterHostDO clusterHostDO = new ClusterHostDO();
        BeanUtil.copyProperties(message, clusterHostDO);

        ClusterInfoEntity cluster = clusterInfoService.getClusterByClusterCode(clusterCode);

        clusterHostDO.setClusterId(cluster.getId());
        clusterHostDO.setCheckTime(new Date());
        clusterHostDO.setRack("/default-rack");
        clusterHostDO.setNodeLabel("default");
        clusterHostDO.setCreateTime(new Date());
        clusterHostDO.setIp(HostUtils.getIpByHost(message.getHostname()));
        clusterHostDO.setHostState(HostState.RUNNING);
        clusterHostDO.setManaged(MANAGED.YES);
        clusterHostService.save(clusterHostDO);
    }

    public static void updateCommandStateToFailed(List<String> commandIds) {
        for (String commandId : commandIds) {
            logger.info("command id is {}", commandId);
            // cancel worker and sub node
            ClusterServiceCommandHostCommandService service = SpringUtil
                    .getBean(ClusterServiceCommandHostCommandService.class);
            ActorRef commandActor = ActorUtils.getLocalActor(ServiceCommandActor.class, "commandActor");
            List<ClusterServiceCommandHostCommandEntity> hostCommandList = service
                    .getHostCommandListByCommandId(commandId);
            for (ClusterServiceCommandHostCommandEntity hostCommandEntity : hostCommandList) {
                if (hostCommandEntity.getCommandState() == CommandState.RUNNING) {
                    logger.info("{} host command  set to cancel", hostCommandEntity.getCommandName());
                    CancelCommandMap.put(hostCommandEntity.getHostCommandId(), hostCommandEntity.getCommandName());

                    hostCommandEntity.setCommandState(CommandState.CANCEL);
                    hostCommandEntity.setCommandProgress(100);
                    service.updateByHostCommandId(hostCommandEntity);
                    UpdateCommandHostMessage message = new UpdateCommandHostMessage();
                    message.setCommandId(commandId);
                    message.setCommandHostId(hostCommandEntity.getCommandHostId());
                    message.setHostname(hostCommandEntity.getHostname());
                    if (hostCommandEntity.getServiceRoleType() == RoleType.MASTER) {
                        message.setServiceRoleType(ServiceRoleType.MASTER);
                    } else {
                        message.setServiceRoleType(ServiceRoleType.WORKER);
                    }
                    ActorUtils.actorSystem.scheduler().scheduleOnce(
                            FiniteDuration.apply(3L, TimeUnit.SECONDS),
                            commandActor,
                            message,
                            ActorUtils.actorSystem.dispatcher(),
                            ActorRef.noSender());
                }
            }
        }
    }

    public static void tellCommandActorResult(String serviceName, ExecuteServiceRoleCommand executeServiceRoleCommand,
            ServiceExecuteState state) {
        ActorRef serviceExecuteResultActor = ActorUtils.getLocalActor(ServiceExecuteResultActor.class,
                ActorUtils.getActorRefName(ServiceExecuteResultActor.class));

        ServiceExecuteResultMessage serviceExecuteResultMessage = new ServiceExecuteResultMessage();
        serviceExecuteResultMessage.setServiceExecuteState(state);
        serviceExecuteResultMessage.setDag(executeServiceRoleCommand.getDag());
        serviceExecuteResultMessage.setServiceName(serviceName);
        serviceExecuteResultMessage.setClusterCode(executeServiceRoleCommand.getClusterCode());
        serviceExecuteResultMessage.setServiceRoleType(executeServiceRoleCommand.getServiceRoleType());
        serviceExecuteResultMessage.setCommandType(executeServiceRoleCommand.getCommandType());
        serviceExecuteResultMessage.setDag(executeServiceRoleCommand.getDag());
        serviceExecuteResultMessage.setClusterId(executeServiceRoleCommand.getClusterId());
        serviceExecuteResultMessage.setActiveTaskList(executeServiceRoleCommand.getActiveTaskList());
        serviceExecuteResultMessage.setErrorTaskList(executeServiceRoleCommand.getErrorTaskList());
        serviceExecuteResultMessage.setReadyToSubmitTaskList(executeServiceRoleCommand.getReadyToSubmitTaskList());
        serviceExecuteResultMessage.setCompleteTaskList(executeServiceRoleCommand.getCompleteTaskList());

        serviceExecuteResultActor.tell(serviceExecuteResultMessage, ActorRef.noSender());
    }

    public static void handleCommandResult(String hostCommandId, Boolean execResult,
                                           String execOut) {
        ClusterServiceCommandHostCommandService service = SpringUtil
                .getBean(ClusterServiceCommandHostCommandService.class);

        ClusterServiceCommandHostCommandEntity hostCommand = service.getByHostCommandId(hostCommandId);
        hostCommand.setCommandProgress(100);
        if (execResult) {
            hostCommand.setCommandState(CommandState.SUCCESS);
            hostCommand.setResultMsg("success");
            logger.info("{} in {} success", hostCommand.getCommandName(), hostCommand.getHostname());
        } else {
            hostCommand.setCommandState(CommandState.FAILED);
            hostCommand.setResultMsg(execOut);
            logger.info("{} in {} failed", hostCommand.getCommandName(), hostCommand.getHostname());
        }
        service.updateByHostCommandId(hostCommand);
        // 更新command host进度
        // 更新command进度
        UpdateCommandHostMessage message = new UpdateCommandHostMessage();
        message.setExecResult(execResult);
        message.setCommandId(hostCommand.getCommandId());
        message.setCommandHostId(hostCommand.getCommandHostId());
        message.setHostname(hostCommand.getHostname());
        if (hostCommand.getServiceRoleType() == RoleType.MASTER) {
            message.setServiceRoleType(ServiceRoleType.MASTER);
        } else {
            message.setServiceRoleType(ServiceRoleType.WORKER);
        }

        ActorRef commandActor = ActorUtils.getLocalActor(ServiceCommandActor.class, "commandActor");
        ActorUtils.actorSystem.scheduler().scheduleOnce(FiniteDuration.apply(
                1L, TimeUnit.SECONDS),
                commandActor, message,
                ActorUtils.actorSystem.dispatcher(),
                ActorRef.noSender());

    }

    public static void buildExecuteServiceRoleCommand(
            Integer clusterId,
            CommandType commandType,
            String clusterCode,
            DAGGraph<String, ServiceNode, String> dag,
            Map<String, ServiceExecuteState> activeTaskList,
            Map<String, String> errorTaskList,
            Map<String, String> readyToSubmitTaskList,
            Map<String, String> completeTaskList,
            String node,
            List<ServiceRoleInfo> masterRoles,
            ServiceRoleInfo workerRole,
            ActorRef serviceActor,
            ServiceRoleType serviceRoleType) {
        ExecuteServiceRoleCommand executeServiceRoleCommand = new ExecuteServiceRoleCommand(clusterId, node,
                masterRoles);
        executeServiceRoleCommand.setServiceRoleType(serviceRoleType);
        executeServiceRoleCommand.setCommandType(commandType);
        executeServiceRoleCommand.setDag(dag);
        executeServiceRoleCommand.setClusterCode(clusterCode);
        executeServiceRoleCommand.setClusterId(clusterId);
        executeServiceRoleCommand.setActiveTaskList(activeTaskList);
        executeServiceRoleCommand.setErrorTaskList(errorTaskList);
        executeServiceRoleCommand.setReadyToSubmitTaskList(readyToSubmitTaskList);
        executeServiceRoleCommand.setCompleteTaskList(completeTaskList);
        executeServiceRoleCommand.setWorkerRole(workerRole);
        serviceActor.tell(executeServiceRoleCommand, ActorRef.noSender());
    }

    public static ClusterServiceCommandEntity generateCommandEntity(Integer clusterId, CommandType commandType,
            String serviceName) {
        ClusterServiceCommandEntity commandEntity = new ClusterServiceCommandEntity();
        String commandId = IdUtil.simpleUUID();
        commandEntity.setCommandId(commandId);
        commandEntity.setClusterId(clusterId);
        commandEntity.setCommandName(commandType.getCommandName(PropertyUtils.getString(Constants.LOCALE_LANGUAGE))
                + Constants.SPACE + serviceName);
        commandEntity.setCommandProgress(0L);
        commandEntity.setCommandState(CommandState.RUNNING);
        commandEntity.setCommandType(commandType.getValue());
        commandEntity.setCreateTime(new Date());
        commandEntity.setCreateBy("admin");
        commandEntity.setServiceName(serviceName);
        return commandEntity;
    }

    public static ClusterServiceCommandHostEntity generateCommandHostEntity(String commandId, String hostname) {
        ClusterServiceCommandHostEntity commandHost = new ClusterServiceCommandHostEntity();
        String commandHostId = IdUtil.simpleUUID();
        commandHost.setCommandHostId(commandHostId);
        commandHost.setCommandId(commandId);
        commandHost.setHostname(hostname);
        commandHost.setCommandState(CommandState.RUNNING);
        commandHost.setCommandProgress(0L);
        commandHost.setCreateTime(new Date());

        return commandHost;
    }

    public static ClusterServiceCommandHostCommandEntity generateCommandHostCommandEntity(CommandType commandType,
            String commandId,
            String serviceRoleName,
            RoleType serviceRoleType,
            ClusterServiceCommandHostEntity commandHost) {
        ClusterServiceCommandHostCommandEntity hostCommand = new ClusterServiceCommandHostCommandEntity();
        String hostCommandId = IdUtil.simpleUUID();
        hostCommand.setHostCommandId(hostCommandId);
        hostCommand.setServiceRoleName(serviceRoleName);
        hostCommand.setCommandHostId(commandHost.getCommandHostId());
        hostCommand.setCommandState(CommandState.RUNNING);
        hostCommand.setCommandProgress(0);
        hostCommand.setHostname(commandHost.getHostname());
        hostCommand.setCommandName(commandType.getCommandName(PropertyUtils.getString(Constants.LOCALE_LANGUAGE))
                + Constants.SPACE + serviceRoleName);
        hostCommand.setCommandId(commandId);
        hostCommand.setCommandType(commandType.getValue());
        hostCommand.setServiceRoleType(serviceRoleType);
        hostCommand.setCreateTime(new Date());
        return hostCommand;
    }

    public static void updateServiceRoleState(CommandType commandType, String serviceRoleName, String hostname,
            Integer clusterId, ServiceRoleState serviceRoleState) {
        ClusterServiceRoleInstanceService serviceRoleInstanceService = SpringUtil
                .getBean(ClusterServiceRoleInstanceService.class);
        ClusterServiceRoleInstanceEntity serviceRole = serviceRoleInstanceService.getOneServiceRole(serviceRoleName,
                hostname, clusterId);
        serviceRole.setServiceRoleState(serviceRoleState);
        serviceRole.setServiceRoleStateCode(serviceRoleState.getValue());
        if (commandType != CommandType.STOP_SERVICE) {
            serviceRole.setNeedRestart(NeedRestart.NO);
        }
        serviceRoleInstanceService.updateById(serviceRole);
    }

    /**
     * 保存到变量表和全局变量缓存
     */
    public static void generateClusterVariable(Map<String, String> globalVariables, Integer clusterId,
            String variableName, String value) {
        ClusterVariableService variableService = SpringUtil
                .getBean(ClusterVariableService.class);
        ClusterVariable clusterVariable = variableService.getVariableByVariableName(variableName, clusterId);
        if (Objects.nonNull(clusterVariable)) {
            logger.info("update variable {} value {} to {}", variableName, clusterVariable.getVariableValue(), value);
            clusterVariable.setVariableValue(value);
            variableService.updateById(clusterVariable);
        } else {
            ClusterVariable newClusterVariable = new ClusterVariable();
            newClusterVariable.setClusterId(clusterId);
            newClusterVariable.setVariableName(variableName);
            newClusterVariable.setVariableValue(value);
            variableService.save(newClusterVariable);
        }
        globalVariables.put(variableName, value);
        putRemoteVariableCache(variableName, value, clusterId);
    }

    public static void hdfsEcMethond(Integer serviceInstanceId,
                                     TreeSet<String> list, String type, String roleName) throws Exception {

        List<ClusterServiceRoleInstanceEntity> namenodes = QueryChain.of(ClusterServiceRoleInstanceEntity.class)
                .where(ClusterServiceRoleInstanceEntity::getServiceId).eq(serviceInstanceId)
                .and(ClusterServiceRoleInstanceEntity::getServiceRoleName).eq(roleName)
                .list();

        // 更新namenode节点的whitelist白名单
        for (ClusterServiceRoleInstanceEntity namenode : namenodes) {
            ActorSelection actorSelection = ActorUtils.actorSystem.actorSelection(
                    "akka.tcp://datasophon@" + namenode.getHostname() + ":2552/user/worker/fileOperateActor");
            ActorSelection execCmdActor = ActorUtils.actorSystem.actorSelection(
                    "akka.tcp://datasophon@" + namenode.getHostname() + ":2552/user/worker/executeCmdActor");
            Timeout timeout = new Timeout(Duration.create(180, TimeUnit.SECONDS));
            FileOperateCommand fileOperateCommand = new FileOperateCommand();
            fileOperateCommand.setLines(list);
            fileOperateCommand.setPath(Constants.INSTALL_PATH + "/hadoop-3.3.3/etc/hadoop/" + type);
            Future<Object> future = Patterns.ask(actorSelection, fileOperateCommand, timeout);
            ExecResult fileOperateResult = (ExecResult) Await.result(future, timeout.duration());
            if (Objects.nonNull(fileOperateResult) && fileOperateResult.getExecResult()) {
                logger.info("write {} success in namenode {}", type, namenode.getHostname());
                // 刷新白名单
                ExecuteCmdCommand command = new ExecuteCmdCommand();
                ArrayList<String> commands = new ArrayList<>();
                commands.add(Constants.INSTALL_PATH + "/hadoop-3.3.3/bin/hdfs");
                commands.add("dfsadmin");
                commands.add("-refreshNodes");
                command.setCommands(commands);
                Future<Object> execFuture = Patterns.ask(execCmdActor, command, timeout);
                ExecResult execResult = (ExecResult) Await.result(execFuture, timeout.duration());
                if (execResult.getExecResult()) {
                    logger.info("hdfs dfsadmin -refreshNodes success at {}", namenode.getHostname());
                }
            }
        }
    }

    /**
     * 为各集群的每个角色创建各自的 MasterServiceActor
     */
    public static void createServiceActor(ClusterInfoEntity clusterInfo) {
        FrameServiceService frameServiceService = SpringUtil.getBean(FrameServiceService.class);

        List<FrameServiceEntity> frameServiceList = frameServiceService
                .getAllFrameServiceByFrameCode(clusterInfo.getClusterFrame());
        for (FrameServiceEntity frameServiceEntity : frameServiceList) {
            // create service actor
            logger.debug("create {} actor",
                    clusterInfo.getClusterCode() + "-serviceActor-" + frameServiceEntity.getServiceName());
            ActorUtils.actorSystem.actorOf(Props.create(MasterServiceActor.class)
                    .withDispatcher("my-forkjoin-dispatcher"),
                    clusterInfo.getClusterCode() + "-serviceActor-" + frameServiceEntity.getServiceName());
        }
    }

    public static String getExceptionMessage(Exception ex) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream pout = new PrintStream(out);
        ex.printStackTrace(pout);
        String ret = out.toString();
        pout.close();
        try {
            out.close();
        } catch (Exception ignored) {
        }
        return ret;
    }

    public static ExecResult restartService(ServiceRoleInfo serviceRoleInfo, boolean needReConfig) throws Exception {
        String depMode = getDepMode(serviceRoleInfo.getClusterId());
        if (Constants.PVM_MODE.equals(depMode)) {
            ServiceHandler serviceStartHandler = new ServiceStartHandler();
            ServiceHandler serviceStopHandler = new ServiceStopHandler();
            if (needReConfig) {
                ServiceConfigureHandler serviceConfigureHandler = new ServiceConfigureHandler();
                serviceStopHandler.setNext(serviceConfigureHandler);
                serviceConfigureHandler.setNext(serviceStartHandler);
            } else {
                serviceStopHandler.setNext(serviceStartHandler);
            }
            return serviceStopHandler.handlerRequest(serviceRoleInfo);
        } else {
            KubernetesHostTagHandler kubernetesHostTagHandler = new KubernetesHostTagHandler();
            KubernetesServiceRoleStopHandler kubernetesServiceRoleStopHandler = new KubernetesServiceRoleStopHandler();
            KubernetesServiceScaleHandler kubernetesServiceScaleHandler = new KubernetesServiceScaleHandler();
            kubernetesServiceRoleStopHandler.setNext(kubernetesServiceScaleHandler);
            if (needReConfig) {
                KubernetesServiceConfigureHandler kubernetesServiceConfigureHandler = new KubernetesServiceConfigureHandler();
                KubernetesDeploymentYamlHandler kubernetesDeploymentYamlHandler = new KubernetesDeploymentYamlHandler();
                kubernetesServiceConfigureHandler.setNext(kubernetesDeploymentYamlHandler);
                kubernetesDeploymentYamlHandler.setNext(kubernetesHostTagHandler);
            }
            kubernetesHostTagHandler.setNext(kubernetesServiceRoleStopHandler);
            kubernetesServiceRoleStopHandler.setNext(kubernetesServiceScaleHandler);
            return kubernetesHostTagHandler.handlerRequest(serviceRoleInfo);
        }
    }

    public static ExecResult startService(ServiceRoleInfo serviceRoleInfo, boolean needReConfig) throws Exception {
        String depMode = getDepMode(serviceRoleInfo.getClusterId());
        ExecResult execResult;
        if (Constants.PVM_MODE.equals(depMode)) {
            if (needReConfig) {
                ServiceConfigureHandler serviceHandler = new ServiceConfigureHandler();
                ServiceHandler serviceStartHandler = new ServiceStartHandler();
                serviceHandler.setNext(serviceStartHandler);
                execResult = serviceHandler.handlerRequest(serviceRoleInfo);
            } else {
                ServiceHandler serviceStartHandler = new ServiceStartHandler();
                execResult = serviceStartHandler.handlerRequest(serviceRoleInfo);
            }
        } else {
            if (needReConfig) {
                KubernetesServiceConfigureHandler kubernetesServiceConfigureHandler = new KubernetesServiceConfigureHandler();
                KubernetesDeploymentYamlHandler kubernetesDeploymentYamlHandler = new KubernetesDeploymentYamlHandler();
                KubernetesHostTagHandler kubernetesHostTagHandler = new KubernetesHostTagHandler();
                KubernetesServiceScaleHandler kubernetesServiceScaleHandler = new KubernetesServiceScaleHandler();
                kubernetesServiceConfigureHandler.setNext(kubernetesDeploymentYamlHandler);
                kubernetesDeploymentYamlHandler.setNext(kubernetesHostTagHandler);
                kubernetesHostTagHandler.setNext(kubernetesServiceScaleHandler);
                execResult = kubernetesServiceConfigureHandler.handlerRequest(serviceRoleInfo);
            } else {
                KubernetesHostTagHandler kubernetesHostTagHandler = new KubernetesHostTagHandler();
                KubernetesServiceStartHandler kubernetesServiceStartHandler = new KubernetesServiceStartHandler();
                KubernetesServiceScaleHandler kubernetesServiceScaleHandler = new KubernetesServiceScaleHandler();
                kubernetesHostTagHandler.setNext(kubernetesServiceStartHandler);
                kubernetesServiceStartHandler.setNext(kubernetesServiceScaleHandler);
                execResult = kubernetesHostTagHandler.handlerRequest(serviceRoleInfo);
            }
        }
        return execResult;
    }

    public static ExecResult stopService(ServiceRoleInfo serviceRoleInfo) throws Exception {
        String depMode = getDepMode(serviceRoleInfo.getClusterId());
        ExecResult execResult;
        if (Constants.PVM_MODE.equals(depMode)) {
            ServiceHandler serviceStopHandler = new ServiceStopHandler();
            execResult = serviceStopHandler.handlerRequest(serviceRoleInfo);
        } else {
            KubernetesHostTagHandler kubernetesHostTagHandler = new KubernetesHostTagHandler();
            KubernetesServiceRoleStopHandler kubernetesServiceRoleStopHandler = new KubernetesServiceRoleStopHandler();
            KubernetesServiceScaleHandler kubernetesServiceScaleHandler = new KubernetesServiceScaleHandler();
            kubernetesHostTagHandler.setNext(kubernetesServiceRoleStopHandler);
            kubernetesServiceRoleStopHandler.setNext(kubernetesServiceScaleHandler);
            execResult = kubernetesHostTagHandler.handlerRequest(serviceRoleInfo);
        }
        return execResult;
    }

    public static ExecResult startInstallService(ServiceRoleInfo serviceRoleInfo) throws Exception {
        String depMode = getDepMode(serviceRoleInfo.getClusterId());
        ExecResult execResult;
        if (Constants.PVM_MODE.equals(depMode)) {
            ServiceHandler serviceInstallHandler = new ServiceInstallHandler();
            ServiceHandler serviceConfigureHandler = new ServiceConfigureHandler();
            ServiceHandler serviceStartHandler = new ServiceStartHandler();
            serviceInstallHandler.setNext(serviceConfigureHandler);
            serviceConfigureHandler.setNext(serviceStartHandler);
            execResult = serviceInstallHandler.handlerRequest(serviceRoleInfo);
        } else {
            // serviceRoleInfo.setCommandType(CommandType.INSTALL_SERVICE);
            // 在Kubernetes环境中使用责任链模式实现服务安装流程
            // 责任链模式允许请求依次经过多个处理器，每个处理器负责一个特定的安装步骤
            // 这种设计提高了代码的模块化程度，便于维护和扩展

            // 1. 创建服务安装处理器 - 负责服务组件的初始安装准备工作
            KubernetesServiceInstallHandler kubernetesServiceInstallHandler = new KubernetesServiceInstallHandler();
            // 2. 创建服务配置处理器 - 负责生成和应用服务的配置文件
            KubernetesServiceConfigureHandler kubernetesServiceConfigureHandler = new KubernetesServiceConfigureHandler();
            // 3. 创建Kubernetes部署YAML处理器 - 负责生成Kubernetes部署所需的YAML文件

            KubernetesDeploymentYamlHandler kubernetesDeploymentYamlHandler = new KubernetesDeploymentYamlHandler();
            // 4. 创建主机标签处理器 - 为Kubernetes节点添加相应的标签，确保Pod被调度到正确的节点
            KubernetesHostTagHandler kubernetesHostTagHandler = new KubernetesHostTagHandler();
            // 5. 创建服务启动处理器 - 负责启动已配置的服务
            KubernetesServiceStartHandler kubernetesServiceStartHandler = new KubernetesServiceStartHandler();

            // 构建责任链，确定处理器的执行顺序
            kubernetesServiceInstallHandler.setNext(kubernetesServiceConfigureHandler); // 安装完成后进行配置
            kubernetesServiceConfigureHandler.setNext(kubernetesDeploymentYamlHandler); // 配置完成后生成YAML
            kubernetesDeploymentYamlHandler.setNext(kubernetesHostTagHandler); // YAML生成后设置主机标签
            kubernetesHostTagHandler.setNext(kubernetesServiceStartHandler); // 标签设置后启动服务

            // 从责任链的第一个处理器开始执行请求，服务角色信息会依次通过所有处理器
            execResult = kubernetesServiceInstallHandler.handlerRequest(serviceRoleInfo);
        }
        return execResult;
    }

    public static ExecResult configServiceRoleInstance(ClusterInfoEntity clusterInfo,
            Map<Generators, List<ServiceConfig>> configFileMap,
            ClusterServiceRoleInstanceEntity roleInstanceEntity) {
        ServiceRoleInfo serviceRoleInfo = new ServiceRoleInfo();
        serviceRoleInfo.setName(roleInstanceEntity.getServiceRoleName());
        serviceRoleInfo.setParentName(roleInstanceEntity.getServiceName());
        serviceRoleInfo.setConfigFileMap(configFileMap);
        serviceRoleInfo
                .setDecompressPackageName(PackageUtils.getServiceDcPackageName(clusterInfo.getClusterFrame(), "YARN"));
        serviceRoleInfo.setHostname(roleInstanceEntity.getHostname());
        ServiceConfigureHandler configureHandler = new ServiceConfigureHandler();
        return configureHandler.handlerRequest(serviceRoleInfo);
    }

    /**
     * &#064;Description: 生成configFileMap
     */
    public static void generateConfigFileMap(Map<Generators, List<ServiceConfig>> configFileMap,
            ClusterServiceRoleGroupConfig config, Integer clusterId) {
        ConfigGroupUtils.generateConfigFileMap(configFileMap, config, clusterId);
    }

    public static List<ServiceConfig> getServiceConfig(ClusterServiceRoleGroupConfig config) {
        return JSONArray.parseArray(config.getConfigJson(), ServiceConfig.class);
    }

    public static ServiceConfig createServiceConfig(String configName, Object configValue, String type) {
        ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.setName(configName);
        serviceConfig.setLabel(configName);
        serviceConfig.setValue(configValue);
        serviceConfig.setRequired(true);
        serviceConfig.setHidden(false);
        serviceConfig.setType(type);
        return serviceConfig;
    }

    public static ClusterInfoEntity getClusterInfo(Integer clusterId) {
        ClusterInfoService clusterInfoService = SpringUtil.getBean(ClusterInfoService.class);
        return clusterInfoService.getById(clusterId);
    }

    public static Map<Integer, String> getAllClusterIdAndType() {
        ClusterInfoService clusterInfoService = SpringUtil.getBean(ClusterInfoService.class);
        return clusterInfoService.list().stream()
                .collect(Collectors.toMap(ClusterInfoEntity::getId, ClusterInfoEntity::getDepType));
    }

    /**
     * 并集：左边集合与右边集合合并
     */
    public static void addAll(List<ServiceConfig> left, List<ServiceConfig> right) {
        if (left == null) {
            return;
        }
        if (right == null) {
            return;
        }
        // 使用LinkedList方便插入和删除
        List<ServiceConfig> res = new LinkedList<>(right);
        Set<String> set = new HashSet<>();
        //
        for (ServiceConfig item : left) {
            set.add(item.getName());
        }
        // 迭代器遍历listA
        for (ServiceConfig item : res) {
            // 如果set中包含id则remove
            if (!set.contains(item.getName())) {
                left.add(item);
            }
        }
    }

    public static void syncUserGroupToHosts(List<ClusterHostDO> hostList, String groupName, String operate) {
        for (ClusterHostDO hostEntity : hostList) {
            ActorRef execCmdActor = ActorUtils.getRemoteActor(hostEntity.getHostname(), "unixGroupActor");
            ExecuteCmdCommand command = new ExecuteCmdCommand();
            ArrayList<String> commands = new ArrayList<>();
            commands.add(operate);
            commands.add(groupName);
            command.setCommands(commands);
            execCmdActor.tell(command, ActorRef.noSender());
        }
    }

    public static Map<String, ServiceConfig> translateToMap(List<ServiceConfig> list) {
        return list.stream()
                .collect(Collectors.toMap(ServiceConfig::getName, serviceConfig -> serviceConfig, (v1, v2) -> v1));
    }
    public static void recoverAlert(ClusterServiceRoleInstanceEntity roleInstanceEntity) {
        ClusterServiceRoleInstanceService roleInstanceService = SpringUtil
                .getBean(ClusterServiceRoleInstanceService.class);
        ClusterAlertHistoryService alertHistoryService = SpringUtil
                .getBean(ClusterAlertHistoryService.class);

        ClusterAlertHistory clusterAlertHistory = QueryChain.of(ClusterAlertHistory.class)
                .where(ClusterAlertHistory::getAlertTargetName).eq(roleInstanceEntity.getServiceRoleName() + " Survive")
                .and(ClusterAlertHistory::getClusterId).eq(roleInstanceEntity.getClusterId())
                .and(ClusterAlertHistory::getHostname).eq(roleInstanceEntity.getHostname())
                .and(ClusterAlertHistory::getIsEnabled).eq(1)
                .one();

        if (Objects.nonNull(clusterAlertHistory)) {
            clusterAlertHistory.setIsEnabled(2);
            alertHistoryService.updateById(clusterAlertHistory);
        }
        // update service role instance state
        if (roleInstanceEntity.getServiceRoleState() != ServiceRoleState.RUNNING) {
            roleInstanceEntity.setServiceRoleState(ServiceRoleState.RUNNING);
            roleInstanceService.updateById(roleInstanceEntity);
        }
    }

    public static void saveAlert(ClusterServiceRoleInstanceEntity roleInstanceEntity, String alertTargetName,
            AlertLevel alertLevel, String alertAdvice) {
        ClusterServiceRoleInstanceService roleInstanceService = SpringUtil
                .getBean(ClusterServiceRoleInstanceService.class);
        ClusterAlertHistoryService alertHistoryService = SpringUtil
                .getBean(ClusterAlertHistoryService.class);
        ClusterServiceInstanceService serviceInstanceService = SpringUtil
                .getBean(ClusterServiceInstanceService.class);

        ClusterAlertHistory clusterAlertHistory = QueryChain.of(ClusterAlertHistory.class)
                .where(ClusterAlertHistory::getAlertTargetName).eq(alertTargetName)
                .and(ClusterAlertHistory::getClusterId).eq(roleInstanceEntity.getClusterId())
                .and(ClusterAlertHistory::getHostname).eq(roleInstanceEntity.getHostname())
                .and(ClusterAlertHistory::getIsEnabled).eq(1)
                .one();

        ClusterServiceInstanceEntity serviceInstanceEntity = serviceInstanceService
                .getById(roleInstanceEntity.getServiceId());
        if (Objects.isNull(clusterAlertHistory)) {
            clusterAlertHistory = ClusterAlertHistory.builder()
                    .clusterId(roleInstanceEntity.getClusterId())
                    .alertGroupName(roleInstanceEntity.getServiceName().toLowerCase())
                    .alertTargetName(alertTargetName)
                    .createTime(new Date())
                    .updateTime(new Date())
                    .alertLevel(alertLevel)
                    .alertInfo("")
                    .alertAdvice(alertAdvice)
                    .hostname(roleInstanceEntity.getHostname())
                    .serviceRoleInstanceId(roleInstanceEntity.getId())
                    .serviceInstanceId(roleInstanceEntity.getServiceId())
                    .isEnabled(1)
                    .serviceInstanceId(roleInstanceEntity.getServiceId())
                    .build();

            alertHistoryService.save(clusterAlertHistory);
        }
        // update service role instance state
        serviceInstanceEntity.setServiceState(ServiceState.EXISTS_EXCEPTION);
        roleInstanceEntity.setServiceRoleState(ServiceRoleState.STOP);
        if (alertLevel == AlertLevel.WARN) {
            serviceInstanceEntity.setServiceState(ServiceState.EXISTS_ALARM);
            roleInstanceEntity.setServiceRoleState(ServiceRoleState.EXISTS_ALARM);
        }
        serviceInstanceService.updateById(serviceInstanceEntity);
        roleInstanceService.updateById(roleInstanceEntity);
    }

    public static String getDepMode(Integer clusterId) {
        ClusterInfoService clusterInfoService = SpringUtil.getBean(ClusterInfoService.class);
        return clusterInfoService.getById(clusterId).getDepType();
    }

    public static Boolean enableKerberos(Integer clusterId, String serviceParentName) {
        Map<String, String> globalVariables = GlobalVariables.get(clusterId);
        return Boolean.parseBoolean(globalVariables.get("${enable" + serviceParentName + "Kerberos}"));
    }

    public static boolean enableRangerPlugin(Integer clusterId, String serviceParentName) {
        Map<String, String> globalVariables = GlobalVariables.get(clusterId);
        return Boolean.parseBoolean(globalVariables.get("${enable" + serviceParentName + "Plugin}"));
    }

    /**
     * 获取指定服务角色的主机名
     * 如果有多个实例，返回第一个运行中的实例
     */
    public static String getServiceRoleHostname(Integer clusterId, String serviceName, String servicRoleName) {
        // 查询指定服务角色的实例
        List<ClusterServiceRoleInstanceEntity> serviceRoles = QueryChain.of(ClusterServiceRoleInstanceEntity.class)
                .where(ClusterServiceRoleInstanceEntity::getClusterId).eq(clusterId)
                .and(ClusterServiceRoleInstanceEntity::getServiceName).eq(serviceName)
                .and(ClusterServiceRoleInstanceEntity::getServiceRoleName).eq(servicRoleName)
                .and(ClusterServiceRoleInstanceEntity::getServiceRoleState).eq(ServiceRoleState.RUNNING)
                .list();

        if (serviceRoles != null && !serviceRoles.isEmpty()) {
            // 返回第一个运行中的实例
            return serviceRoles.getFirst().getHostname();
        }

        // 如果没有运行中的实例，尝试获取任意状态的实例
        ClusterServiceRoleInstanceEntity anyRole = QueryChain.of(ClusterServiceRoleInstanceEntity.class)
                .where(ClusterServiceRoleInstanceEntity::getClusterId).eq(clusterId)
                .and(ClusterServiceRoleInstanceEntity::getServiceName).eq(serviceName)
                .and(ClusterServiceRoleInstanceEntity::getServiceRoleName).eq(servicRoleName)
                .one();

        return anyRole != null ? anyRole.getHostname() : null;
    }

    public static Integer extractPortFromUrl(String url) {
        try {
            java.net.URI uri = java.net.URI.create(url);
            return uri.getPort() == -1 ? null : uri.getPort();
        } catch (Exception e) {
            logger.error("Failed to extract port from URL: {}", url, e);
            return null;
        }
    }

    public static String replacePortInUrl(String url, String newPort) {
        try {
            java.net.URI uri = java.net.URI.create(url);
            return url.replace(
                    ":" + uri.getPort(),
                    ":" + newPort);
        } catch (Exception e) {
            logger.error("Failed to replace port in URL: {}", url, e);
            return url; // 返回原始URL如果替换失败
        }
    }
}
