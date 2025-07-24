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

package com.datasophon.api.service.host.impl;

import cn.hutool.core.convert.Convert;
import org.apache.pekko.actor.ActorRef;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson2.JSONObject;
import com.mybatisflex.core.query.QueryChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.datasophon.api.enums.Status;
import com.datasophon.api.master.ActorUtils;
import com.datasophon.api.master.PrometheusActor;
import com.datasophon.api.master.RackActor;
import com.datasophon.api.service.ClusterRackService;
import com.datasophon.api.service.ClusterServiceRoleInstanceService;
import com.datasophon.api.service.host.ClusterHostService;
import com.datasophon.api.service.host.dto.QueryHostListPageDTO;
import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.command.ExecuteCmdCommand;
import com.datasophon.common.command.GenerateHostPrometheusConfig;
import com.datasophon.common.command.GenerateRackPropCommand;
import com.datasophon.common.model.HostInfo;
import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.ClusterHostDO;
import com.datasophon.dao.entity.ClusterRack;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;
import com.datasophon.dao.enums.RoleType;
import com.datasophon.dao.enums.ServiceRoleState;
import com.datasophon.dao.mapper.ClusterHostMapper;
import com.datasophon.domain.host.enums.HostState;
import com.datasophon.domain.host.enums.MANAGED;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import scala.concurrent.duration.FiniteDuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service("clusterHostService")
@Transactional
public class ClusterHostServiceImpl extends ServiceImpl<ClusterHostMapper, ClusterHostDO>
        implements
        ClusterHostService {

    private static final Logger logger = LoggerFactory.getLogger(ClusterHostServiceImpl.class);

    // 移除字段注入
    private final ClusterServiceRoleInstanceService roleInstanceService;
    private final ClusterRackService clusterRackService;

    // 添加构造函数注入
    @Autowired
    public ClusterHostServiceImpl(ClusterServiceRoleInstanceService roleInstanceService, ClusterRackService clusterRackService) {
        this.roleInstanceService = roleInstanceService;
        this.clusterRackService = clusterRackService;
    }

    @Override
    public ClusterHostDO getClusterHostByHostname(String hostname) {
        return QueryChain.of(ClusterHostDO.class)
                .where(ClusterHostDO::getHostname).eq(hostname)
                .one();
    }

    @Override
    public ClusterHostDO getClusterHostByIp(String ip) {
        return QueryChain.of(ClusterHostDO.class)
                .where(ClusterHostDO::getIp).eq(ip)
                .one();
    }

    @Override
    public Result<List<QueryHostListPageDTO>> listByPage(Integer clusterId, String hostname, String ip, String cpuArchitecture, Integer hostState,
            String orderField, String orderType, Integer page, Integer pageSize) {
        List<QueryHostListPageDTO> hostListPageDTOS = new ArrayList<>();
        int offset = (page - 1) * pageSize;

        QueryChain<ClusterHostDO> queryChain = QueryChain.of(ClusterHostDO.class)
                .where(ClusterHostDO::getClusterId).eq(clusterId)
                .and(ClusterHostDO::getManaged).eq(MANAGED.YES);

        if (StringUtils.isNotBlank(cpuArchitecture)) {
            queryChain.and(ClusterHostDO::getCpuArchitecture).eq(cpuArchitecture);
        }

        if (hostState != null) {
            queryChain.and(ClusterHostDO::getHostState).eq(hostState);
        }

        if (StringUtils.isNotBlank(ip)) {
            queryChain.and(ClusterHostDO::getIp).like("%" + ip + "%");
        }

        if (StringUtils.isNotBlank(hostname)) {
            queryChain.and(ClusterHostDO::getHostname).like("%" + hostname + "%");
        }

        if ("asc".equals(orderType)) {
            queryChain.orderBy(ClusterHostDO::getHostname).asc();
        } else {
            queryChain.orderBy(ClusterHostDO::getHostname).desc();
        }

        List<ClusterHostDO> list = queryChain.limit(offset, pageSize).list();

        // 回显rack的名称 而不是ID
        Map<String, String> rackMap = clusterRackService.queryClusterRack(clusterId).stream()
                .collect(Collectors.toMap(obj -> obj.getId() + "", ClusterRack::getRack));

        Map<String, HostInfo> hostInfoMap = Convert.toMap(String.class, HostInfo.class, CacheUtils.get(clusterId + Constants.HOST_MAP));

        for (ClusterHostDO clusterHostDO : list) {
            QueryHostListPageDTO queryHostListPageDTO = new QueryHostListPageDTO();
            BeanUtils.copyProperties(clusterHostDO, queryHostListPageDTO);
            // 查询主机上服务角色数
            long serviceRoleNum = QueryChain.of(ClusterServiceRoleInstanceEntity.class)
                    .where(ClusterServiceRoleInstanceEntity::getHostname).eq(clusterHostDO.getHostname())
                    .count();
            queryHostListPageDTO.setServiceRoleNum(serviceRoleNum);
            queryHostListPageDTO.setHostState(clusterHostDO.getHostState().getValue());
            queryHostListPageDTO.setRack(rackMap.getOrDefault(queryHostListPageDTO.getRack(), "/default-rack"));

            // 从缓存中获取hosts文件内容和操作系统类型
            if (hostInfoMap != null) {
                HostInfo hostInfo = hostInfoMap.get(clusterHostDO.getHostname());
                if (hostInfo != null) {
                    // 设置hosts文件内容
                    if (hostInfo.getOsInfo() != null && hostInfo.getOsInfo().getDnsInfo() != null) {
                        queryHostListPageDTO.setHostsFile(hostInfo.getOsInfo().getDnsInfo().getHostsFileContent());
                    } else {
                        queryHostListPageDTO.setHostsFile(null);
                    }

                    // 设置操作系统类型
                    if (hostInfo.getOsInfo() != null) {
                        queryHostListPageDTO.setOsType(hostInfo.getOsInfo().getDistribution());
                    }
                }
            }

            hostListPageDTOS.add(queryHostListPageDTO);
        }

        QueryChain<ClusterHostDO> countQuery = QueryChain.of(ClusterHostDO.class)
                .where(ClusterHostDO::getClusterId).eq(clusterId)
                .and(ClusterHostDO::getManaged).eq(MANAGED.YES);

        if (StringUtils.isNotBlank(cpuArchitecture)) {
            countQuery.and(ClusterHostDO::getCpuArchitecture).eq(cpuArchitecture);
        }

        if (hostState != null) {
            countQuery.and(ClusterHostDO::getHostState).eq(hostState);
        }

        if (StringUtils.isNotBlank(hostname)) {
            countQuery.and(ClusterHostDO::getHostname).like("%" + hostname + "%");
        }

        long count = countQuery.count();

        return Result.success(hostListPageDTOS,count);
    }

    @Override
    public List<ClusterHostDO> getHostListByClusterId(Integer clusterId) {
        return QueryChain.of(ClusterHostDO.class)
                .where(ClusterHostDO::getClusterId).eq(clusterId)
                .and(ClusterHostDO::getManaged).eq(MANAGED.YES)
                .list();
    }

    @Override
    public Result<List<ClusterServiceRoleInstanceEntity>> getRoleListByHostname(Integer clusterId, String hostname) {
        List<ClusterServiceRoleInstanceEntity> list = roleInstanceService
                .getServiceRoleListByHostnameAndClusterId(hostname, clusterId);
        for (ClusterServiceRoleInstanceEntity roleInstanceEntity : list) {
            roleInstanceEntity.setServiceRoleStateCode(roleInstanceEntity.getServiceRoleState().getValue());
        }
        return Result.success(list);
    }

    /**
     * 批量删除主机。
     * 删除主机，首先停止主机上的服务
     * 其次删除主机 worker，同时移除 Prometheus hosts
     * 然后删除主机运行的实例
     *
     */
    @Override
    @Transactional
    public Result<String> deleteHosts(String hostIds) {
        // 批量移除
        String[] ids = hostIds.split(Constants.COMMA);
        for (String hostId : ids) {
            ClusterHostDO host = this.getById(hostId);

            Integer clusterId = host.getClusterId();
            List<ClusterServiceRoleInstanceEntity> list = QueryChain.of(ClusterServiceRoleInstanceEntity.class)
                    .where(ClusterServiceRoleInstanceEntity::getClusterId).eq(clusterId)
                    .and(ClusterServiceRoleInstanceEntity::getHostname).eq(host.getHostname())
                    .and(ClusterServiceRoleInstanceEntity::getServiceRoleState).eq(ServiceRoleState.RUNNING)
                    .and(ClusterServiceRoleInstanceEntity::getRoleType).ne(RoleType.CLIENT)
                    .list();

            List<String> roles = list.stream().map(ClusterServiceRoleInstanceEntity::getServiceRoleName)
                    .toList();
            if (!list.isEmpty()) {
                return Result.error(host.getHostname() + Status.HOST_EXIT_ONE_RUNNING_ROLE.getMsg() + roles);
            }

            // ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);
            // String clusterCode = clusterInfo.getClusterCode();
            String distributeAgentKey = clusterId + Constants.UNDERLINE + Constants.START_DISTRIBUTE_AGENT;
            if (CacheUtils.constainsKey(distributeAgentKey + Constants.UNDERLINE + host.getHostname())) {
                CacheUtils.removeKey(distributeAgentKey + Constants.UNDERLINE + host.getHostname());
            }

            this.removeById(hostId);

            if (host.getHostState() != HostState.OFFLINE) {
                // stop the worker on this host
                ActorRef execCmdActor = ActorUtils.getRemoteActor(host.getHostname(), "executeCmdActor");
                ExecuteCmdCommand command = new ExecuteCmdCommand();
                ArrayList<String> commands = new ArrayList<>();
                commands.add("service");
                commands.add("datasophon-worker");
                commands.add("stop");

                command.setCommands(commands);
                execCmdActor.tell(command, ActorRef.noSender());
            }
            // remove host from prometheus
            ActorRef prometheusActor = ActorUtils.getLocalActor(PrometheusActor.class,
                    ActorUtils.getActorRefName(PrometheusActor.class));

            // Prometheus 移除 hosts 信息
            GenerateHostPrometheusConfig prometheusConfigCommand = new GenerateHostPrometheusConfig();
            prometheusConfigCommand.setClusterId(clusterId);

            ActorUtils.actorSystem.scheduler().scheduleOnce(
                    FiniteDuration.apply(3L, TimeUnit.SECONDS),
                    prometheusActor,
                    prometheusConfigCommand,
                    ActorUtils.actorSystem.dispatcher(),
                    ActorRef.noSender());

            Map<String, HostInfo> map = Convert.toMap(String.class, HostInfo.class, CacheUtils.get(clusterId + Constants.HOST_MAP));
            if (Objects.nonNull(map)) {
                map.remove(host.getHostname());
            }
        }
        return Result.success();
    }

    @Override
    public Result<ArrayList<JSONObject>> getRack(Integer clusterId) {
        ArrayList<JSONObject> list = new ArrayList<>();
        JSONObject rack = new JSONObject();
        rack.put("rack", "/default-rack");
        list.add(rack);
        return Result.success(list);
    }

    @Override
    public void removeHostByClusterId(Integer clusterId) {
        this.remove(QueryChain.of(ClusterHostDO.class)
                .where(ClusterHostDO::getClusterId).eq(clusterId));
    }

    @Override
    public void updateBatchNodeLabel(List<String> hostIds, String nodeLabel) {
        List<ClusterHostDO> list = QueryChain.of(ClusterHostDO.class)
                .where(ClusterHostDO::getId).in(hostIds)
                .list();
        for (ClusterHostDO clusterHostDO : list) {
            clusterHostDO.setNodeLabel(nodeLabel);
        }
        this.updateBatch(list);
    }

    @Override
    public List<ClusterHostDO> getHostListByIds(List<String> ids) {

        // 查询ID匹配的主机
        List<ClusterHostDO> hostsByIds = QueryChain.of(ClusterHostDO.class)
                .where(ClusterHostDO::getId).in(ids)
                .list();
        List<ClusterHostDO> result = new ArrayList<>(hostsByIds);

        // 查询主机名匹配的主机
        List<ClusterHostDO> hostsByNames = QueryChain.of(ClusterHostDO.class)
                .where(ClusterHostDO::getHostname).in(ids)
                .list();

        // 合并去重
        for (ClusterHostDO host : hostsByNames) {
            boolean exists = false;
            for (ClusterHostDO existingHost : hostsByIds) {
                if (existingHost.getId().equals(host.getId())) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                result.add(host);
            }
        }

        return result;
    }

    @Override
    public Result<String> assignRack(Integer clusterId, String rack, String hostIds) {
        List<String> ids = Arrays.asList(hostIds.split(","));
        List<ClusterHostDO> list = QueryChain.of(ClusterHostDO.class)
                .where(ClusterHostDO::getId).in(ids)
                .list();
        for (ClusterHostDO clusterHostDO : list) {
            clusterHostDO.setRack(rack);
        }
        this.updateBatch(list);

        GenerateRackPropCommand command = new GenerateRackPropCommand();
        command.setClusterId(clusterId);
        ActorRef rackActor = ActorUtils.getLocalActor(RackActor.class, "rackActor");
        rackActor.tell(command, ActorRef.noSender());
        return Result.success();
    }

    @Override
    public List<ClusterHostDO> getClusterHostByRack(Integer clusterId, String rack) {
        return QueryChain.of(ClusterHostDO.class)
                .where(ClusterHostDO::getClusterId).eq(clusterId)
                .and(ClusterHostDO::getRack).eq(rack)
                .list();
    }

    public Result<String> saveKubernetesHost(List<HostInfo> hostInfoList, Integer clusterId) {
        for (HostInfo hostInfo : hostInfoList) {
            ClusterHostDO hostEntity = this.getClusterHostByHostname(hostInfo.getHostname());
            if (ObjectUtil.isNull(hostEntity)) {
                ClusterHostDO clusterHostDO = new ClusterHostDO();
                clusterHostDO.setClusterId(clusterId);
                clusterHostDO.setCreateTime(hostInfo.getCreateTime());
                // 使用正确的主机名，而不是IP
                clusterHostDO.setHostname(hostInfo.getHostname());
                clusterHostDO.setIp(hostInfo.getIp());
                clusterHostDO.setRack("/default-rack");
                clusterHostDO.setHostState(HostState.RUNNING);
                clusterHostDO.setManaged(MANAGED.YES);

                // 从K8S API获取的完整信息
                String arch = hostInfo.getCpuArchitecture();
                if (StringUtils.isBlank(arch)) {
                    arch = "x86_64";
                    logger.warn("Host {} architecture is empty, using default: {}", hostInfo.getHostname(), arch);
                } else {
                    logger.info("Host {} architecture from Kubernetes API: {}", hostInfo.getHostname(), arch);
                }
                clusterHostDO.setCpuArchitecture(arch);

                // 设置节点标签
                clusterHostDO.setNodeLabel("default");

                // 注意：在K8S模式下，硬件信息（coreNum, totalMem, totalDisk等）
                // 应该从K8S API获取的原始ClusterHostDO中获取
                // 但是由于HostInfo对象不包含这些信息，我们需要在调用此方法时
                // 确保从K8S API获取的完整信息能够正确传递
                // 建议修改调用方式，直接传递ClusterHostDO列表而不是HostInfo列表

                this.save(clusterHostDO);
                logger.info("Successfully saved Kubernetes host {} with info: hostname={}, ip={}, arch={}",
                        hostInfo.getHostname(), hostInfo.getHostname(), hostInfo.getIp(), arch);
            }
        }
        return Result.success();
    }

    /**
     * 直接保存K8S主机信息（使用从K8S API获取的完整ClusterHostDO信息）
     */
    public Result<String> saveKubernetesHostDirect(List<ClusterHostDO> kubernetesHosts, Integer clusterId) {
        for (ClusterHostDO kubernetesHost : kubernetesHosts) {
            ClusterHostDO hostEntity = this.getClusterHostByHostname(kubernetesHost.getHostname());
            if (ObjectUtil.isNull(hostEntity)) {
                ClusterHostDO clusterHostDO = new ClusterHostDO();
                clusterHostDO.setClusterId(clusterId);
                clusterHostDO.setCreateTime(kubernetesHost.getCreateTime());
                // 使用正确的主机名，而不是IP
                clusterHostDO.setHostname(kubernetesHost.getHostname());
                clusterHostDO.setIp(kubernetesHost.getIp());
                clusterHostDO.setRack("/default-rack");
                clusterHostDO.setHostState(HostState.RUNNING);
                clusterHostDO.setManaged(MANAGED.YES);

                // 从K8S API获取的完整硬件信息
                clusterHostDO.setCpuArchitecture(kubernetesHost.getCpuArchitecture());
                clusterHostDO.setCoreNum(kubernetesHost.getCoreNum());
                clusterHostDO.setTotalMem(kubernetesHost.getTotalMem());
                clusterHostDO.setTotalDisk(kubernetesHost.getTotalDisk());
                clusterHostDO.setUsedMem(kubernetesHost.getUsedMem());
                clusterHostDO.setUsedDisk(kubernetesHost.getUsedDisk());

                // 设置节点标签
                clusterHostDO.setNodeLabel("default");

                this.save(clusterHostDO);
                logger.info(
                        "Successfully saved Kubernetes host {} with complete info: hostname={}, ip={}, arch={}, cores={}, mem={}GB, disk={}GB",
                        kubernetesHost.getHostname(), kubernetesHost.getHostname(), kubernetesHost.getIp(),
                        kubernetesHost.getCpuArchitecture(), kubernetesHost.getCoreNum(),
                        kubernetesHost.getTotalMem(), kubernetesHost.getTotalDisk());
            }
        }
        return Result.success();
    }

    /**
     * 获取K8S模式下的完整硬件信息
     */
    public Result<List<ClusterHostDO>> getK8sHostsWithHardwareInfo(Integer clusterId) {
        try {
            // 从缓存中获取K8S完整硬件信息
            Object cachedData = CacheUtils.get(clusterId + "_K8S_HOSTS_FOR_SAVE");
            if (cachedData != null) {
                @SuppressWarnings("unchecked")
                List<ClusterHostDO> kubernetesHosts = (List<ClusterHostDO>) cachedData;
                logger.info("获取到K8S完整硬件信息，共{}台主机", kubernetesHosts.size());
                return Result.success(kubernetesHosts);
            } else {
                logger.warn("未找到K8S完整硬件信息缓存");
                return Result.success(new ArrayList<>());
            }
        } catch (Exception e) {
            logger.error("获取K8S完整硬件信息失败", e);
            return Result.error("获取K8S完整硬件信息失败: " + e.getMessage());
        }
    }
}
