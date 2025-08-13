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
import com.datasophon.dao.entity.ClusterHostEntity;
import org.apache.pekko.actor.ActorRef;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.datasophon.common.enums.Status;
import com.datasophon.api.master.ActorUtils;
import com.datasophon.api.master.PrometheusActor;
import com.datasophon.api.master.RackActor;
import com.datasophon.api.service.ClusterRackService;
import com.datasophon.api.service.RoleInstanceQueryService;
import com.datasophon.api.service.host.ClusterHostService;

import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.command.ExecuteCmdCommand;
import com.datasophon.common.command.GenerateHostPrometheusConfig;
import com.datasophon.common.command.GenerateRackPropCommand;
import com.datasophon.common.model.HostInfo;
import com.datasophon.common.dto.ClusterRackDTO;
import com.datasophon.common.dto.ClusterServiceRoleInstanceDTO;
import com.datasophon.common.model.PageResult;
import com.datasophon.common.exception.BusinessException;
import com.mybatisflex.core.paginate.Page;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;
import com.datasophon.dao.mapper.ClusterHostMapper;
import com.datasophon.dao.mapper.ClusterServiceRoleInstanceMapper;
import com.datasophon.common.enums.HostState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import scala.concurrent.duration.FiniteDuration;

import java.util.ArrayList;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 集群主机服务实现
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-01
 */
@Service("clusterHostService")
@Transactional
public class ClusterHostServiceImpl extends ServiceImpl<ClusterHostMapper, ClusterHostEntity>
        implements
        ClusterHostService {

    private static final Logger logger = LoggerFactory.getLogger(ClusterHostServiceImpl.class);

    @Autowired
    private RoleInstanceQueryService roleInstanceQueryService;
    @Autowired
    private ClusterRackService clusterRackService;
    @Autowired
    private ClusterServiceRoleInstanceMapper clusterServiceRoleInstanceMapper;
    @Autowired
    private ClusterHostMapper clusterHostMapper;

    @Override
    public ClusterHostEntity getClusterHostByHostname(String hostname) {
        return clusterHostMapper.selectByHostname(hostname);
    }

    @Override
    public ClusterHostEntity getClusterHostByIp(String ip) {
        return clusterHostMapper.selectByIp(ip);
    }

    @Override
    public PageResult<ClusterHostEntity> listByPage(Long clusterId, String hostname, String ip,
                                                    String cpuArchitecture, Integer hostState,
                                                    String orderField, String orderType, Integer page, Integer pageSize) {
        Page<ClusterHostEntity> pageParam = Page.of(page, pageSize);
        Page<ClusterHostEntity> pageResult = clusterHostMapper
                .selectPageByClusterIdAndFilters(pageParam, clusterId, hostname, ip, cpuArchitecture, hostState,
                        orderType);

        return PageResult.of(pageResult.getRecords(), pageResult.getTotalRow(), page, pageSize);
    }

    @Override
    public List<ClusterHostEntity> getHostListByClusterId(Long clusterId) {
        return clusterHostMapper.selectByClusterId(clusterId);
    }

    @Override
    public List<ClusterHostEntity> getAllManagedHostsByClusterId(Long clusterId) {
        return clusterHostMapper.selectManagedHostsByClusterIdOrderByHostname(clusterId);
    }

    @Override
    public List<ClusterServiceRoleInstanceDTO> getRoleListByHostname(Long clusterId, String hostname) {
        // 直接返回查询结果，DTO应该已经包含正确的状态码
        return roleInstanceQueryService.getServiceRoleListByHostnameAndClusterId(hostname, clusterId);
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
    public void deleteHosts(String hostIds) throws BusinessException {
        // 批量移除
        String[] ids = hostIds.split(Constants.COMMA);
        for (String hostId : ids) {
            ClusterHostEntity host = this.getById(hostId);

            Long clusterId = host.getClusterId();
            List<ClusterServiceRoleInstanceEntity> list = clusterServiceRoleInstanceMapper
                    .selectRunningNonClientRolesByClusterIdAndHostname(clusterId, host.getHostname());

            List<String> roles = list.stream().map(ClusterServiceRoleInstanceEntity::getServiceRoleName)
                    .toList();
            if (!list.isEmpty()) {
                throw new BusinessException(Status.HOST_EXIT_ONE_RUNNING_ROLE.getCode(),
                        host.getHostname() + Status.HOST_EXIT_ONE_RUNNING_ROLE.getMsg() + roles);
            }

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

            Map<String, HostInfo> map = Convert.toMap(String.class, HostInfo.class,
                    CacheUtils.get(clusterId + Constants.HOST_MAP));
            if (Objects.nonNull(map)) {
                map.remove(host.getHostname());
            }
        }
    }

    @Override
    public List<ClusterRackDTO> getRack(Long clusterId) {
        return clusterRackService.queryClusterRack(clusterId);
    }

    @Override
    public void removeHostByClusterId(Long clusterId) {
        clusterHostMapper.deleteByClusterId(clusterId);
    }

    @Override
    public void saveHost(ClusterHostEntity clusterHostEntity) {
        this.save(clusterHostEntity);
    }

    @Override
    public void updateBatchNodeLabel(List<String> hostIds, String nodeLabel) {
        List<ClusterHostEntity> list = clusterHostMapper.selectByIds(hostIds);
        for (ClusterHostEntity clusterHostEntity : list) {
            clusterHostEntity.setNodeLabel(nodeLabel);
        }
        this.updateBatch(list);
    }

    @Override
    public List<ClusterHostEntity> getHostListByIds(List<String> ids) {

        // 查询ID匹配的主机
        List<ClusterHostEntity> hostsByIds = clusterHostMapper.selectByIds(ids);
        List<ClusterHostEntity> result = new ArrayList<>(hostsByIds);

        // 查询主机名匹配的主机
        List<ClusterHostEntity> hostsByNames = clusterHostMapper.selectByHostnames(ids);

        // 合并去重
        for (ClusterHostEntity host : hostsByNames) {
            boolean exists = false;
            for (ClusterHostEntity existingHost : hostsByIds) {
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
    public void assignRack(Long clusterId, String rack, String hostIds) throws BusinessException {
        List<String> ids = List.of(hostIds.split(","));
        List<ClusterHostEntity> list = clusterHostMapper.selectByIds(ids);
        for (ClusterHostEntity clusterHostEntity : list) {
            clusterHostEntity.setRack(rack);
        }
        this.updateBatch(list);

        GenerateRackPropCommand command = new GenerateRackPropCommand();
        command.setClusterId(clusterId);
        ActorRef rackActor = ActorUtils.getLocalActor(RackActor.class, "rackActor");
        rackActor.tell(command, ActorRef.noSender());
    }

    @Override
    public List<ClusterHostEntity> getClusterHostByRack(Long clusterId, String rack) {
        return clusterHostMapper.selectByClusterIdAndRack(clusterId, rack);
    }







    @Override
    public void updateBatchHostStatus(List<ClusterHostEntity> hosts) {
        if (hosts == null || hosts.isEmpty()) {
            return;
        }

        try {
            // 使用继承的updateBatch方法批量更新
            this.updateBatch(hosts);
            logger.debug("Successfully updated {} hosts status", hosts.size());
        } catch (Exception e) {
            logger.error("Failed to batch update hosts status", e);
            // 如果批量更新失败，尝试逐个更新
            for (ClusterHostEntity host : hosts) {
                try {
                    this.updateById(host);
                } catch (Exception ex) {
                    logger.warn("Failed to update host {} status", host.getId(), ex);
                }
            }
        }
    }

    @Override
    public List<ClusterHostEntity> getHostsByIpList(Long clusterId, List<String> ipList) {
        return getMapper().selectByClusterIdAndIpList(clusterId, ipList);
    }
}
