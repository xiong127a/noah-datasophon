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

import com.datasophon.common.enums.Status;
import com.datasophon.api.kubernetes.handler.KubernetesServiceStopHandler;
import com.datasophon.api.load.GlobalVariables;
import com.datasophon.api.master.ActorUtils;
import com.datasophon.api.service.ClusterAlertHistoryService;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.api.service.ClusterServiceCommandService;
import com.datasophon.api.service.ClusterServiceRoleInstanceService;
import com.datasophon.api.service.ClusterServiceRoleInstanceWebuisService;
import com.datasophon.api.service.FrameServiceRoleService;
import com.datasophon.api.service.FrameServiceService;
import com.datasophon.api.service.RoleGroupEntityService;
import com.datasophon.api.utils.ClusterInfoUtils;
import com.datasophon.api.utils.ProcessUtils;
import com.datasophon.common.Constants;
import com.datasophon.common.command.GetLogCommand;
import com.datasophon.common.command.KubernetesGetLogCommand;
import com.datasophon.common.enums.CommandType;
import com.datasophon.common.model.ServiceRoleInfo;
import com.datasophon.common.utils.CollectionUtils;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.utils.PlaceholderUtils;
// Result类已移除 - Service层严格禁止返回Result，只返回DTO/Entity或抛出异常
import com.datasophon.common.model.PageResult;
import com.datasophon.dao.entity.ClusterInfoEntity;
import com.datasophon.dao.entity.ClusterServiceInstanceRoleGroup;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;
import com.datasophon.dao.entity.FrameServiceEntity;
import com.datasophon.common.dto.FrameServiceRoleDTO;
import com.datasophon.common.enums.NeedRestart;
import com.datasophon.common.enums.ServiceRoleState;
import com.datasophon.dao.mapper.ClusterServiceRoleInstanceMapper;
import com.datasophon.api.converter.ClusterServiceRoleInstanceConverter;
import com.datasophon.common.dto.ClusterServiceRoleInstanceDTO;
import com.datasophon.kubernetes.actor.KubernetesLogActor;
import com.datasophon.kubernetes.util.CommonUtil;
// QueryChain已迁移到DAO层，不再在Service层使用
import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.apache.pekko.actor.ActorRef;
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

@Service("clusterServiceRoleInstanceService")
public class ClusterServiceRoleInstanceServiceImpl
        extends
        ServiceImpl<ClusterServiceRoleInstanceMapper, ClusterServiceRoleInstanceEntity>
        implements
        ClusterServiceRoleInstanceService {

    private static final Logger logger = LoggerFactory.getLogger(ClusterServiceRoleInstanceServiceImpl.class);

    @Autowired
    private ClusterInfoService clusterInfoService;

    @org.springframework.context.annotation.Lazy
    @Autowired
    private FrameServiceRoleService frameServiceRoleService;

    @Autowired
    private FrameServiceService frameService;

    @org.springframework.context.annotation.Lazy
    @Autowired
    private ClusterServiceRoleInstanceService roleInstanceService;

    @Autowired
    private ClusterServiceCommandService commandService;

    @Autowired
    private RoleGroupEntityService roleGroupEntityService;

    @Autowired
    private ClusterServiceRoleInstanceMapper roleInstanceMapper;

    @Autowired
    private ClusterAlertHistoryService alertHistoryService;

    @Autowired
    private ClusterServiceRoleInstanceWebuisService webuisService;

    @Autowired
    private ClusterServiceRoleInstanceConverter clusterServiceRoleInstanceConverter;

    @Autowired
    public ClusterServiceRoleInstanceServiceImpl(
            @org.springframework.context.annotation.Lazy ClusterInfoService clusterInfoService,
            @org.springframework.context.annotation.Lazy FrameServiceRoleService frameServiceRoleService,
            FrameServiceService frameService,
            @org.springframework.context.annotation.Lazy ClusterServiceRoleInstanceService roleInstanceService,
            ClusterServiceCommandService commandService,
            ClusterServiceRoleInstanceMapper roleInstanceMapper,
            @org.springframework.context.annotation.Lazy ClusterAlertHistoryService alertHistoryService,
            ClusterServiceRoleInstanceWebuisService webuisService) {
        this.clusterInfoService = clusterInfoService;
        this.frameServiceRoleService = frameServiceRoleService;
        this.frameService = frameService;
        this.roleInstanceService = roleInstanceService;
        this.commandService = commandService;
        this.roleInstanceMapper = roleInstanceMapper;
        this.alertHistoryService = alertHistoryService;
        this.webuisService = webuisService;
    }

    @Override
    public List<ClusterServiceRoleInstanceDTO> listStoppedServiceRoleListByHostnameAndClusterId(String hostname,
            Integer clusterId) {
        List<ClusterServiceRoleInstanceEntity> entities = getMapper()
                .selectStoppedServiceRolesByClusterIdAndHostname(clusterId, hostname);
        return clusterServiceRoleInstanceConverter.entityListToDtoList(entities);
    }

    @Override
    public List<ClusterServiceRoleInstanceDTO> getServiceRoleListByHostnameAndClusterId(String hostname,
            Integer clusterId) {
        List<ClusterServiceRoleInstanceEntity> entities = getMapper().selectByClusterIdAndHostname(clusterId, hostname);
        return clusterServiceRoleInstanceConverter.entityListToDtoList(entities);
    }

    @Override
    public List<ClusterServiceRoleInstanceDTO> getServiceRoleInstanceListByServiceIdAndRoleState(Integer serviceId,
            ServiceRoleState stop) {
        List<ClusterServiceRoleInstanceEntity> entities = getMapper().selectByServiceIdAndRoleState(serviceId, stop);
        return clusterServiceRoleInstanceConverter.entityListToDtoList(entities);
    }

    @Override
    public ClusterServiceRoleInstanceDTO getOneServiceRole(String name, String hostname, Integer id) {
        ClusterServiceRoleInstanceEntity entity = getMapper().selectByServiceRoleNameAndClusterIdAndHostname(name, id,
                hostname);

        if (entity != null) {
            return clusterServiceRoleInstanceConverter.entityToDto(entity);
        }
        return null;
    }

    @Override
    public PageResult<ClusterServiceRoleInstanceDTO> listAll(Integer serviceInstanceId, String hostname,
            Integer serviceRoleState, String serviceRoleName,
            Integer roleGroupId, Integer page, Integer pageSize) {
        int offset = (page - 1) * pageSize;

        // DAO层：使用Mapper统计数量
        long count = getMapper().countByConditions(serviceInstanceId, hostname, serviceRoleState,
                serviceRoleName, roleGroupId);

        if (count == 0) {
            return PageResult.empty(page, pageSize);
        }

        // DAO层：使用Mapper分页查询
        List<ClusterServiceRoleInstanceEntity> cluServiceRoleInstList = getMapper()
                .selectByConditionsWithPage(serviceInstanceId, hostname, serviceRoleState,
                        serviceRoleName, roleGroupId, offset, pageSize);

        if (CollectionUtils.isEmpty(cluServiceRoleInstList)) {
            return PageResult.empty(page, pageSize);
        }

        // Service层：业务逻辑处理 - 使用JDK21现代特性
        List<ClusterServiceRoleInstanceEntity> processedList = cluServiceRoleInstList.stream()
                .peek(roleInstanceEntity -> {
                    // 设置角色组名称
                    ClusterServiceInstanceRoleGroup roleGroup = roleGroupEntityService
                            .getById(roleInstanceEntity.getRoleGroupId());
                    if (Objects.nonNull(roleGroup)) {
                        roleInstanceEntity.setRoleGroupName(roleGroup.getRoleGroupName());
                    }
                    // 设置状态码
                    roleInstanceEntity.setServiceRoleStateCode(roleInstanceEntity.getServiceRoleState().getValue());
                })
                .toList();

        // Service层：Entity → DTO转换
        List<ClusterServiceRoleInstanceDTO> dtoList = clusterServiceRoleInstanceConverter
                .entityListToDtoList(processedList);
        return PageResult.of(dtoList, count, page, pageSize);
    }

    @Override
    public String getLog(Integer serviceRoleInstanceId) throws Exception {
        ClusterServiceRoleInstanceEntity roleInstance = this.getById(serviceRoleInstanceId);
        ClusterInfoEntity clusterInfo = clusterInfoService.getById(roleInstance.getClusterId());
        FrameServiceRoleDTO serviceRole = frameServiceRoleService.getServiceRoleByFrameCodeAndServiceRoleName(
                clusterInfo.getClusterFrame(), roleInstance.getServiceRoleName());
        Map<String, String> globalVariables = GlobalVariables.get(roleInstance.getClusterId());
        if (serviceRole.serviceRoleType() == 3) { // CLIENT = 3
            return "client does not have any log";
        }
        FrameServiceEntity frameServiceEntity = frameService.getById(serviceRole.serviceId());
        String logFile = serviceRole.logFile();
        if (StringUtils.isNotBlank(logFile)) {
            logFile = PlaceholderUtils.replacePlaceholders(logFile, globalVariables, Constants.REGEX_VARIABLE);
            logger.info("logFile is {}", logFile);
        }
        logger.info("start to get {} log from {}", serviceRole.serviceRoleName(), roleInstance.getHostname());
        String kubeConfig = clusterInfoService.getKubeConfigByClusterId(roleInstance.getClusterId());
        Future<Object> logFuture;
        Timeout timeout = new Timeout(Duration.create(60, TimeUnit.SECONDS));
        if (clusterInfo.getDepType() != null && clusterInfo.getDepType().isKubernetes()) {
            KubernetesGetLogCommand kubernetesGetLogCommand = new KubernetesGetLogCommand();
            kubernetesGetLogCommand.setLogFile(logFile);
            String namespace = ClusterInfoUtils.getKubernetesNamespace(clusterInfo.getId());
            kubernetesGetLogCommand.setNamespace(namespace);
            kubernetesGetLogCommand.setDecompressPackageName(frameServiceEntity.getDecompressPackageName());
            kubernetesGetLogCommand.setHostname(roleInstance.getHostname());
            kubernetesGetLogCommand.setKubeConfig(kubeConfig);
            kubernetesGetLogCommand.setServiceRoleFullName(CommonUtil
                    .generateServiceRoleFullName(roleInstance.getServiceName(), roleInstance.getServiceRoleName()));
            ActorRef kubernetesLog = ActorUtils.getLocalActor(KubernetesLogActor.class,
                    ActorUtils.getActorRefName(KubernetesLogActor.class));
            logFuture = Patterns.ask(kubernetesLog, kubernetesGetLogCommand, timeout);
        } else {
            GetLogCommand command = new GetLogCommand();
            command.setLogFile(logFile);
            command.setDecompressPackageName(frameServiceEntity.getDecompressPackageName());
            ActorSelection configActor = ActorUtils.actorSystem
                    .actorSelection(
                            "akka.tcp://datasophon@" + roleInstance.getHostname() + ":2552/user/worker/logActor");
            logFuture = Patterns.ask(configActor, command, timeout);
        }
        ExecResult logResult = (ExecResult) Await.result(logFuture, timeout.duration());
        if (Objects.nonNull(logResult) && logResult.getExecResult()) {
            return logResult.getExecOut();
        }
        return "No log available";
    }

    @Override
    public List<ClusterServiceRoleInstanceDTO> getServiceRoleInstanceListByServiceId(int id) {
        // SQL逻辑已迁移到DAO层
        List<ClusterServiceRoleInstanceEntity> entities = getMapper().selectByServiceId(id);
        return clusterServiceRoleInstanceConverter.entityListToDtoList(entities);
    }

    @Override
    public List<ClusterServiceRoleInstanceDTO> getServiceRoleInstanceListByClusterId(int clusterId) {
        // SQL逻辑已迁移到DAO层
        List<ClusterServiceRoleInstanceEntity> entities = getMapper().selectByClusterId(clusterId);
        return clusterServiceRoleInstanceConverter.entityListToDtoList(entities);
    }

    @Override
    public void deleteServiceRole(List<String> idList) {
        Collection<ClusterServiceRoleInstanceEntity> list = this.listByIds(idList);
        Map<String, Long> roleNameRemoveCount = list.stream()
                .filter(instance -> instance.getServiceRoleState() != ServiceRoleState.RUNNING) // 过滤条件
                .collect(java.util.stream.Collectors.groupingBy(
                        ClusterServiceRoleInstanceEntity::getServiceRoleName, // 以服务角色名称为键
                        java.util.stream.Collectors.counting() // 统计数量
                ));
        // is there a running instance
        boolean flag = false;
        Integer clusterId = null;
        String ServiceName = null;
        ArrayList<Integer> needRemoveList = new ArrayList<>();
        for (ClusterServiceRoleInstanceEntity instance : list) {
            if (clusterId == null) {
                clusterId = instance.getClusterId();
            }
            if (ServiceName == null) {
                ServiceName = instance.getServiceName();
            }
            if (instance.getServiceRoleState() == ServiceRoleState.RUNNING) {
                flag = true;
            } else {
                clusterId = instance.getClusterId();
                needRemoveList.add(instance.getId());
            }
        }
        ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);
        if (clusterInfo.getDepType() != null && clusterInfo.getDepType().isKubernetes()) {
            List<String> matchingRoleNames = new ArrayList<>();
            for (Map.Entry<String, Long> entry : roleNameRemoveCount.entrySet()) {
                String roleName = entry.getKey();
                Long count = entry.getValue();
                if (roleInstanceService.listServiceRoleByName(roleName).size() == (count)) {
                    matchingRoleNames.add(roleName); // 添加到列表
                }
            }
            for (String serviceRoleName : matchingRoleNames) {
                KubernetesServiceStopHandler kubernetesServiceStopHandler = new KubernetesServiceStopHandler();
                ServiceRoleInfo serviceRoleInfo = new ServiceRoleInfo();
                serviceRoleInfo.setClusterId(clusterId);
                serviceRoleInfo.setParentName(ServiceName);
                serviceRoleInfo.setName(serviceRoleName);
                try {
                    kubernetesServiceStopHandler.handlerRequest(serviceRoleInfo);
                    logger.info("remove {} deployment success", serviceRoleName);
                } catch (Exception e) {
                    logger.error("remove {} deployment failed", serviceRoleName);
                }
            }
        }
        if (!needRemoveList.isEmpty()) {
            alertHistoryService.removeAlertByRoleInstanceIds(needRemoveList);
            this.removeByIds(needRemoveList);
            // delete if there is a webui
            webuisService.removeByRoleInsIds(needRemoveList);

        }
        if (flag) {
            throw new RuntimeException(Status.EXIT_RUNNING_INSTANCES.getMsg());
        }
        // 删除成功，无需返回值
    }

    @Override
    public List<ClusterServiceRoleInstanceDTO> getServiceRoleInstanceListByClusterIdAndRoleName(Integer clusterId,
            String roleName) {
        // SQL逻辑已迁移到DAO层
        List<ClusterServiceRoleInstanceEntity> entities = getMapper().selectByClusterIdAndRoleName(clusterId, roleName);
        return clusterServiceRoleInstanceConverter.entityListToDtoList(entities);
    }

    @Override
    public List<ClusterServiceRoleInstanceDTO> getRunningServiceRoleInstanceListByServiceId(
            Integer serviceInstanceId) {
        // SQL逻辑已迁移到DAO层
        List<ClusterServiceRoleInstanceEntity> entities = getMapper().selectByServiceIdAndState(serviceInstanceId,
                ServiceRoleState.RUNNING);
        return clusterServiceRoleInstanceConverter.entityListToDtoList(entities);
    }

    @Override
    public void restartObsoleteService(Integer roleGroupId) {
        ClusterServiceInstanceRoleGroup roleGroup = roleGroupEntityService.getById(roleGroupId);
        // SQL逻辑已迁移到DAO层
        List<ClusterServiceRoleInstanceEntity> list = getMapper()
                .selectByServiceIdAndNeedRestart(roleGroup.getServiceInstanceId(), NeedRestart.YES);

        if (Objects.nonNull(list) && !list.isEmpty()) {
            List<String> ids = list.stream().map(e -> e.getId() + "").toList();
            commandService.generateServiceRoleCommand(roleGroup.getClusterId(), CommandType.RESTART_SERVICE,
                    roleGroup.getServiceInstanceId(), ids, null);
        } else {
            throw new RuntimeException(Status.ROLE_GROUP_HAS_NO_OUTDATED_SERVICE.getMsg());
        }
    }

    @Override
    public String decommissionNode(String serviceRoleInstanceIds, String serviceName) {
        TreeSet<String> hosts = new TreeSet<>();
        Integer serviceInstanceId = null;
        String serviceRoleName = "";
        for (String str : serviceRoleInstanceIds.split(",")) {
            int serviceRoleInstanceId = Integer.parseInt(str);
            ClusterServiceRoleInstanceEntity roleInstanceEntity = this.getById(serviceRoleInstanceId);
            if ("DataNode".equals(roleInstanceEntity.getServiceRoleName())
                    || "NodeManager".equals(roleInstanceEntity.getServiceRoleName())) {
                hosts.add(roleInstanceEntity.getHostname());
                serviceInstanceId = roleInstanceEntity.getServiceId();
                serviceRoleName = roleInstanceEntity.getServiceRoleName();
                roleInstanceEntity.setServiceRoleState(ServiceRoleState.DECOMMISSIONING);
                this.updateById(roleInstanceEntity);
            }
        }
        // 查询已退役节点，SQL逻辑已迁移到DAO层
        List<ClusterServiceRoleInstanceEntity> list = getMapper().selectByStateAndIds(ServiceRoleState.DECOMMISSIONING,
                serviceRoleInstanceIds);

        // 添加已退役节点到黑名单
        for (ClusterServiceRoleInstanceEntity roleInstanceEntity : list) {
            hosts.add(roleInstanceEntity.getHostname());
        }
        String type = "blacklist";
        String roleName = "NameNode";
        if ("nodemanager".equalsIgnoreCase(serviceRoleName)) {
            type = "nmexclude";
            roleName = "ResourceManager";
        }
        if (!hosts.isEmpty()) {
            ProcessUtils.hdfsEcMethond(serviceInstanceId, hosts, type, roleName);
        }
        return "Decommission completed successfully";
    }

    @Override
    public void updateToNeedRestart(Integer roleGroupId, String serviceRoleName) {
        roleInstanceMapper.updateToNeedRestartByServiceRoleName(roleGroupId, serviceRoleName);
    }

    @Override
    public void updateToNeedRestart(Integer roleGroupId) {
        roleInstanceMapper.updateToNeedRestart(roleGroupId);
    }

    @Override
    public void updateToNeedRestartByHost(String hostName) {
        roleInstanceMapper.updateToNeedRestartByHost(hostName);
    }

    @Override
    public List<ClusterServiceRoleInstanceDTO> getObsoleteService(Integer serviceInstanceId) {
        List<ClusterServiceRoleInstanceEntity> entities = getMapper().selectByServiceIdAndNeedRestart(serviceInstanceId,
                NeedRestart.YES);
        return clusterServiceRoleInstanceConverter.entityListToDtoList(entities);
    }

    @Override
    public List<ClusterServiceRoleInstanceDTO> getStoppedRoleInstanceOnHost(Integer clusterId, String hostname,
            ServiceRoleState state) {
        List<ClusterServiceRoleInstanceEntity> entities = getMapper().selectByClusterIdAndHostnameAndState(clusterId,
                hostname, state);
        return clusterServiceRoleInstanceConverter.entityListToDtoList(entities);
    }

    @Override
    public void reomveRoleInstance(Integer serviceInstanceId) {
        // SQL逻辑已迁移到DAO层
        getMapper().deleteByServiceIdAndState(serviceInstanceId, ServiceRoleState.STOP);
    }

    @Override
    public ClusterServiceRoleInstanceDTO getKAdminRoleIns(Integer clusterId) {
        ClusterServiceRoleInstanceEntity entity = getMapper().selectByClusterIdAndServiceRoleName(clusterId, "KAdmin");
        return clusterServiceRoleInstanceConverter.entityToDto(entity);
    }

    @Override
    public List<ClusterServiceRoleInstanceDTO> listServiceRoleByName(String serviceRoleName) {
        List<ClusterServiceRoleInstanceEntity> entities = getMapper().selectByServiceRoleName(serviceRoleName);
        return clusterServiceRoleInstanceConverter.entityListToDtoList(entities);
    }

    @Override
    public ClusterServiceRoleInstanceDTO listServiceRoleByNameAndClusterId(Integer clusterId,
            String serviceRoleName) {
        ClusterServiceRoleInstanceEntity entity = getMapper().selectByClusterIdAndServiceRoleName(clusterId,
                serviceRoleName);
        return clusterServiceRoleInstanceConverter.entityToDto(entity);
    }

    @Override
    public ClusterServiceRoleInstanceDTO getServiceRoleInsByHostAndName(String hostName, String serviceRoleName) {
        ClusterServiceRoleInstanceEntity entity = getMapper().selectByHostnameAndServiceRoleName(hostName,
                serviceRoleName);
        return clusterServiceRoleInstanceConverter.entityToDto(entity);
    }

    @Override
    public List<ClusterServiceRoleInstanceDTO> listRoleIns(String hostname, String serviceName) {
        List<ClusterServiceRoleInstanceEntity> entities = getMapper().selectByHostnameAndServiceName(hostname,
                serviceName);
        return clusterServiceRoleInstanceConverter.entityListToDtoList(entities);
    }

    @Override
    public List<ClusterServiceRoleInstanceDTO> getServiceRoleInstanceListByServiceInstanceIdAndRoleName(
            Integer clusterId, Integer serviceInstanceId, String roleName) {
        List<ClusterServiceRoleInstanceEntity> entities = getMapper()
                .selectByClusterIdAndServiceIdAndRoleName(clusterId, serviceInstanceId, roleName);
        return clusterServiceRoleInstanceConverter.entityListToDtoList(entities);
    }

    @Override
    public void updateServiceRoleInstanceState(Integer serviceRoleInstanceId, ServiceRoleState serviceRoleState) {
        ClusterServiceRoleInstanceEntity entity = getById(serviceRoleInstanceId);
        if (entity != null) {
            entity.setServiceRoleState(serviceRoleState);
            updateById(entity);
        }
    }

    @Override
    public List<ClusterServiceRoleInstanceDTO> getServiceRolesByNames(List<String> serviceNames) {
        List<ClusterServiceRoleInstanceEntity> entities = getMapper().getServiceRolesByNames(serviceNames);
        return clusterServiceRoleInstanceConverter.entityListToDtoList(entities);
    }
}
