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

import com.datasophon.api.enums.Status;
import com.datasophon.api.kubernetes.handler.KubernetesServiceStopHandler;
import com.datasophon.api.load.GlobalVariables;
import com.datasophon.api.master.ActorUtils;
import com.datasophon.api.service.ClusterAlertHistoryService;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.api.service.ClusterServiceCommandService;
import com.datasophon.api.service.ClusterServiceInstanceRoleGroupService;
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
import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.ClusterInfoEntity;
import com.datasophon.dao.entity.ClusterServiceInstanceRoleGroup;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;
import com.datasophon.dao.entity.FrameServiceEntity;
import com.datasophon.dao.entity.FrameServiceRoleEntity;
import com.datasophon.dao.enums.NeedRestart;
import com.datasophon.dao.enums.RoleType;
import com.datasophon.dao.enums.ServiceRoleState;
import com.datasophon.dao.mapper.ClusterServiceRoleInstanceMapper;
import com.datasophon.kubernetes.actor.KubernetesLogActor;
import com.datasophon.kubernetes.util.CommonUtil;
import com.mybatisflex.core.query.QueryChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSelection;
import org.apache.pekko.pattern.Patterns;
import org.apache.pekko.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
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
import java.util.stream.Collectors;

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
    private ClusterServiceInstanceRoleGroupService roleGroupService;

    @Autowired
    private RoleGroupEntityService roleGroupEntityService;

    @Autowired
    private ClusterServiceRoleInstanceMapper roleInstanceMapper;

    @Autowired
    private ClusterAlertHistoryService alertHistoryService;

    @Autowired
    private ClusterServiceRoleInstanceWebuisService webuisService;

    @Autowired
    public ClusterServiceRoleInstanceServiceImpl(
            @org.springframework.context.annotation.Lazy ClusterInfoService clusterInfoService,
            @org.springframework.context.annotation.Lazy FrameServiceRoleService frameServiceRoleService,
            FrameServiceService frameService,
            @org.springframework.context.annotation.Lazy ClusterServiceRoleInstanceService roleInstanceService,
            ClusterServiceCommandService commandService,
            ClusterServiceInstanceRoleGroupService roleGroupService,
            ClusterServiceRoleInstanceMapper roleInstanceMapper,
            @org.springframework.context.annotation.Lazy ClusterAlertHistoryService alertHistoryService,
            ClusterServiceRoleInstanceWebuisService webuisService) {
        this.clusterInfoService = clusterInfoService;
        this.frameServiceRoleService = frameServiceRoleService;
        this.frameService = frameService;
        this.roleInstanceService = roleInstanceService;
        this.commandService = commandService;
        this.roleGroupService = roleGroupService;
        this.roleInstanceMapper = roleInstanceMapper;
        this.alertHistoryService = alertHistoryService;
        this.webuisService = webuisService;
    }

    @Override
    public List<ClusterServiceRoleInstanceEntity> listStoppedServiceRoleListByHostnameAndClusterId(String hostname,
            Integer clusterId) {
        return QueryChain.of(ClusterServiceRoleInstanceEntity.class)
                .where(ClusterServiceRoleInstanceEntity::getClusterId).eq(clusterId)
                .and(ClusterServiceRoleInstanceEntity::getHostname).eq(hostname)
                .and(ClusterServiceRoleInstanceEntity::getServiceRoleState).eq(ServiceRoleState.STOP)
                .list();
    }

    @Override
    public List<ClusterServiceRoleInstanceEntity> getServiceRoleListByHostnameAndClusterId(String hostname,
            Integer clusterId) {
        return QueryChain.of(ClusterServiceRoleInstanceEntity.class)
                .where(ClusterServiceRoleInstanceEntity::getClusterId).eq(clusterId)
                .and(ClusterServiceRoleInstanceEntity::getHostname).eq(hostname)
                .list();
    }

    @Override
    public List<ClusterServiceRoleInstanceEntity> getServiceRoleInstanceListByServiceIdAndRoleState(Integer serviceId,
            ServiceRoleState stop) {
        return QueryChain.of(ClusterServiceRoleInstanceEntity.class)
                .where(ClusterServiceRoleInstanceEntity::getServiceId).eq(serviceId)
                .and(ClusterServiceRoleInstanceEntity::getServiceRoleState).eq(stop)
                .list();
    }

    @Override
    public ClusterServiceRoleInstanceEntity getOneServiceRole(String name, String hostname, Integer id) {
        QueryChain<ClusterServiceRoleInstanceEntity> query = QueryChain.of(ClusterServiceRoleInstanceEntity.class)
                .where(ClusterServiceRoleInstanceEntity::getServiceRoleName).eq(name)
                .and(ClusterServiceRoleInstanceEntity::getClusterId).eq(id);

        if (StringUtils.isNotBlank(hostname)) {
            query.and(ClusterServiceRoleInstanceEntity::getHostname).eq(hostname);
        }

        List<ClusterServiceRoleInstanceEntity> list = query.list();
        if (Objects.nonNull(list) && !list.isEmpty()) {
            return list.getFirst();
        }
        return null;
    }

    @Override
    public Result listAll(Integer serviceInstanceId, String hostname, Integer serviceRoleState, String serviceRoleName,
            Integer roleGroupId, Integer page, Integer pageSize) {
        int offset = (page - 1) * pageSize;

        QueryChain<ClusterServiceRoleInstanceEntity> query = QueryChain.of(ClusterServiceRoleInstanceEntity.class)
                .where(ClusterServiceRoleInstanceEntity::getServiceId).eq(serviceInstanceId);

        if (Objects.nonNull(serviceRoleState)) {
            query.and(ClusterServiceRoleInstanceEntity::getServiceRoleState).eq(serviceRoleState);
        }

        if (StringUtils.isNotBlank(serviceRoleName)) {
            query.and(ClusterServiceRoleInstanceEntity::getServiceRoleName).eq(serviceRoleName);
        }

        if (Objects.nonNull(roleGroupId)) {
            query.and(ClusterServiceRoleInstanceEntity::getRoleGroupId).eq(roleGroupId);
        }

        if (StringUtils.isNotBlank(hostname)) {
            query.and(ClusterServiceRoleInstanceEntity::getHostname).like("%" + hostname + "%");
        }

        long count = query.count();
        List<ClusterServiceRoleInstanceEntity> cluServiceRoleInstList = query
                .limit(offset, pageSize)
                .list();

        if (CollectionUtils.isEmpty(cluServiceRoleInstList)) {
            return Result.successEmptyCount();
        }

        for (ClusterServiceRoleInstanceEntity roleInstanceEntity : cluServiceRoleInstList) {
                            ClusterServiceInstanceRoleGroup roleGroup = roleGroupEntityService
                    .getById(roleInstanceEntity.getRoleGroupId());
            if (Objects.nonNull(roleGroup)) {
                roleInstanceEntity.setRoleGroupName(roleGroup.getRoleGroupName());
            }
            roleInstanceEntity.setServiceRoleStateCode(roleInstanceEntity.getServiceRoleState().getValue());
        }

        return Result.success(cluServiceRoleInstList,count);
    }

    @Override
    public Result getLog(Integer serviceRoleInstanceId) throws Exception {
        ClusterServiceRoleInstanceEntity roleInstance = this.getById(serviceRoleInstanceId);
        ClusterInfoEntity clusterInfo = clusterInfoService.getById(roleInstance.getClusterId());
        FrameServiceRoleEntity serviceRole = frameServiceRoleService.getServiceRoleByFrameCodeAndServiceRoleName(
                clusterInfo.getClusterFrame(), roleInstance.getServiceRoleName());
        Map<String, String> globalVariables = GlobalVariables.get(roleInstance.getClusterId());
        if (serviceRole.getServiceRoleType() == RoleType.CLIENT) {
            return Result.success("client does not have any log");
        }
        FrameServiceEntity frameServiceEntity = frameService.getById(serviceRole.getServiceId());
        String logFile = serviceRole.getLogFile();
        if (StringUtils.isNotBlank(logFile)) {
            logFile = PlaceholderUtils.replacePlaceholders(logFile, globalVariables, Constants.REGEX_VARIABLE);
            logger.info("logFile is {}", logFile);
        }
        logger.info("start to get {} log from {}", serviceRole.getServiceRoleName(), roleInstance.getHostname());
        String kubeConfig = clusterInfoService.getKubeConfigByClusterId(roleInstance.getClusterId());
        Future<Object> logFuture;
        Timeout timeout = new Timeout(Duration.create(60, TimeUnit.SECONDS));
        if (Constants.KUBERNETES_MODE.equals(clusterInfo.getDepType())) {
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
            return Result.success(logResult.getExecOut());
        }
        return Result.success();
    }

    @Override
    public List<ClusterServiceRoleInstanceEntity> getServiceRoleInstanceListByServiceId(int id) {
        return QueryChain.of(ClusterServiceRoleInstanceEntity.class)
                .where(ClusterServiceRoleInstanceEntity::getServiceId).eq(id)
                .list();
    }

    @Override
    public List<ClusterServiceRoleInstanceEntity> getServiceRoleInstanceListByClusterId(int clusterId) {
        return QueryChain.of(ClusterServiceRoleInstanceEntity.class)
                .where(ClusterServiceRoleInstanceEntity::getClusterId).eq(clusterId)
                .list();
    }

    @Override
    public Result deleteServiceRole(List<String> idList) {
        Collection<ClusterServiceRoleInstanceEntity> list = this.listByIds(idList);
        Map<String, Long> roleNameRemoveCount = list.stream()
                .filter(instance -> instance.getServiceRoleState() != ServiceRoleState.RUNNING) // 过滤条件
                .collect(Collectors.groupingBy(
                        ClusterServiceRoleInstanceEntity::getServiceRoleName, // 以服务角色名称为键
                        Collectors.counting() // 统计数量
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
        if (Constants.KUBERNETES_MODE.equals(clusterInfo.getDepType())) {
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
        return flag ? Result.error(Status.EXIT_RUNNING_INSTANCES.getMsg()) : Result.success();
    }

    @Override
    public List<ClusterServiceRoleInstanceEntity> getServiceRoleInstanceListByClusterIdAndRoleName(Integer clusterId,
            String roleName) {
        return QueryChain.of(ClusterServiceRoleInstanceEntity.class)
                .where(ClusterServiceRoleInstanceEntity::getClusterId).eq(clusterId)
                .and(ClusterServiceRoleInstanceEntity::getServiceRoleName).eq(roleName)
                .list();
    }

    @Override
    public List<ClusterServiceRoleInstanceEntity> getRunningServiceRoleInstanceListByServiceId(
            Integer serviceInstanceId) {
        return QueryChain.of(ClusterServiceRoleInstanceEntity.class)
                .where(ClusterServiceRoleInstanceEntity::getServiceId).eq(serviceInstanceId)
                .and(ClusterServiceRoleInstanceEntity::getServiceRoleState).eq(ServiceRoleState.RUNNING)
                .list();
    }

    @Override
    public Result restartObsoleteService(Integer roleGroupId) {
        ClusterServiceInstanceRoleGroup roleGroup = roleGroupEntityService.getById(roleGroupId);
        List<ClusterServiceRoleInstanceEntity> list = QueryChain.of(ClusterServiceRoleInstanceEntity.class)
                .where(ClusterServiceRoleInstanceEntity::getRoleGroupId).eq(roleGroupId)
                .and(ClusterServiceRoleInstanceEntity::getNeedRestart).eq(NeedRestart.YES)
                .list();

        if (Objects.nonNull(list) && !list.isEmpty()) {
            List<String> ids = list.stream().map(e -> e.getId() + "").collect(Collectors.toList());
            commandService.generateServiceRoleCommand(roleGroup.getClusterId(), CommandType.RESTART_SERVICE,
                    roleGroup.getServiceInstanceId(), ids, null);
        } else {
            return Result.error(Status.ROLE_GROUP_HAS_NO_OUTDATED_SERVICE.getMsg());
        }
        return Result.success();
    }

    @Override
    public Result decommissionNode(String serviceRoleInstanceIds, String serviceName) throws Exception {
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
        // 查询已退役节点
        List<ClusterServiceRoleInstanceEntity> list = QueryChain.of(ClusterServiceRoleInstanceEntity.class)
                .where(ClusterServiceRoleInstanceEntity::getServiceRoleState).eq(ServiceRoleState.DECOMMISSIONING)
                .and(ClusterServiceRoleInstanceEntity::getId).in((Object) serviceRoleInstanceIds.split(","))
                .list();

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
        return Result.success();
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
    public List<ClusterServiceRoleInstanceEntity> getObsoleteService(Integer serviceInstanceId) {
        return QueryChain.of(ClusterServiceRoleInstanceEntity.class)
                .where(ClusterServiceRoleInstanceEntity::getServiceId).eq(serviceInstanceId)
                .and(ClusterServiceRoleInstanceEntity::getNeedRestart).eq(NeedRestart.YES)
                .list();
    }

    @Override
    public List<ClusterServiceRoleInstanceEntity> getStoppedRoleInstanceOnHost(Integer clusterId, String hostname,
            ServiceRoleState state) {
        return QueryChain.of(ClusterServiceRoleInstanceEntity.class)
                .where(ClusterServiceRoleInstanceEntity::getClusterId).eq(clusterId)
                .and(ClusterServiceRoleInstanceEntity::getHostname).eq(hostname)
                .and(ClusterServiceRoleInstanceEntity::getServiceRoleState).eq(state)
                .list();
    }

    @Override
    public void reomveRoleInstance(Integer serviceInstanceId) {
        this.remove(QueryChain.of(ClusterServiceRoleInstanceEntity.class)
                .where(ClusterServiceRoleInstanceEntity::getServiceId).eq(serviceInstanceId)
                .and(ClusterServiceRoleInstanceEntity::getServiceRoleState).eq(ServiceRoleState.STOP));
    }

    @Override
    public ClusterServiceRoleInstanceEntity getKAdminRoleIns(Integer clusterId) {
        return QueryChain.of(ClusterServiceRoleInstanceEntity.class)
                .where(ClusterServiceRoleInstanceEntity::getClusterId).eq(clusterId)
                .and(ClusterServiceRoleInstanceEntity::getServiceRoleName).eq("KAdmin")
                .one();
    }

    @Override
    public List<ClusterServiceRoleInstanceEntity> listServiceRoleByName(String serviceRoleName) {
        return QueryChain.of(ClusterServiceRoleInstanceEntity.class)
                .where(ClusterServiceRoleInstanceEntity::getServiceRoleName).eq(serviceRoleName)
                .list();
    }

    @Override
    public ClusterServiceRoleInstanceEntity listServiceRoleByNameAndClusterId(Integer clusterId,
            String serviceRoleName) {
        List<ClusterServiceRoleInstanceEntity> list = QueryChain.of(ClusterServiceRoleInstanceEntity.class)
                .where(ClusterServiceRoleInstanceEntity::getClusterId).eq(clusterId)
                .and(ClusterServiceRoleInstanceEntity::getServiceRoleName).eq(serviceRoleName)
                .list();

        if (Objects.nonNull(list) && !list.isEmpty()) {
            return list.getFirst();
        }
        return null;
    }

    @Override
    public ClusterServiceRoleInstanceEntity getServiceRoleInsByHostAndName(String hostName, String serviceRoleName) {
        return QueryChain.of(ClusterServiceRoleInstanceEntity.class)
                .where(ClusterServiceRoleInstanceEntity::getHostname).eq(hostName)
                .and(ClusterServiceRoleInstanceEntity::getServiceRoleName).eq(serviceRoleName)
                .one();
    }

    @Override
    public List<ClusterServiceRoleInstanceEntity> listRoleIns(String hostname, String serviceName) {
        return QueryChain.of(ClusterServiceRoleInstanceEntity.class)
                .where(ClusterServiceRoleInstanceEntity::getHostname).eq(hostname)
                .and(ClusterServiceRoleInstanceEntity::getServiceName).eq(serviceName)
                .list();
    }

    @Override
    public List<ClusterServiceRoleInstanceEntity> getServiceRoleInstanceListByServiceInstanceIdAndRoleName(
            Integer clusterId, Integer serviceInstanceId, String roleName) {
        QueryChain<ClusterServiceRoleInstanceEntity> query = QueryChain.of(ClusterServiceRoleInstanceEntity.class)
                .where(ClusterServiceRoleInstanceEntity::getClusterId).eq(clusterId)
                .and(ClusterServiceRoleInstanceEntity::getServiceRoleName).eq(roleName);

        if (serviceInstanceId != null) {
            query.and(ClusterServiceRoleInstanceEntity::getServiceId).eq(serviceInstanceId);
        }

        return query.list();
    }
}
