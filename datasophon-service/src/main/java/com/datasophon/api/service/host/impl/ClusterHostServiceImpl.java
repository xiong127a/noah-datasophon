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
import com.datasophon.api.service.RackConfigurationService;
import com.datasophon.dao.entity.ClusterHostEntity;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.datasophon.common.enums.ManagementStatus;
import com.datasophon.common.enums.Status;
import com.datasophon.api.service.PrometheusIntegrationService;
import com.datasophon.api.service.ClusterRackService;
import com.datasophon.api.service.RoleInstanceQueryService;
import com.datasophon.api.service.host.ClusterHostService;

import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
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
import com.mybatisflex.core.query.QueryChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
    private PrometheusIntegrationService prometheusIntegrationService;
    
    @Autowired
    private RackConfigurationService rackConfigurationService;
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
    public List<ClusterHostEntity> getHostListByClusterIdAndManaged(Long clusterId) {
        return clusterHostMapper.selectByClusterIdAndManaged(clusterId);
    }

    @Override
    public List<ClusterHostEntity> getHostListByClusterId(Long clusterId) {
        return clusterHostMapper.selectByClusterId(clusterId);
    }

    @Override
    public List<ClusterHostEntity> getAllManagedHostsByClusterId(Long clusterId) {
        logger.info("🔍 [Service调试] 查询受管主机 - 集群ID: {}", clusterId);
        
        List<ClusterHostEntity> result = clusterHostMapper.selectManagedHostsByClusterIdOrderByHostname(clusterId);
        
        logger.info("🔍 [Service调试] DAO查询结果 - 集群ID: {}, 返回数量: {}", clusterId, result.size());
        
        return result;
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
            if (CacheUtils.containsKey(distributeAgentKey + Constants.UNDERLINE + host.getHostname())) {
                CacheUtils.removeKey(distributeAgentKey + Constants.UNDERLINE + host.getHostname());
            }

            this.removeById(hostId);

            if (host.getHostState() != HostState.OFFLINE) {
                // TODO: 通过HTTP REST API停止Worker
                logger.info("需要停止Worker: hostname={}", host.getHostname());
            }
            
            // Prometheus 移除 hosts 信息 - 延迟3秒执行
            prometheusIntegrationService.generateHostPrometheusConfigDelayed(clusterId, 3);

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
        // 直接调用重写后的save方法，避免重复检查
        this.save(clusterHostEntity);
    }

    /**
     * 重写父类save方法，添加重复检查
     */
    @Override
    public boolean save(ClusterHostEntity entity) {
        // 添加重复主机检查逻辑
        if (entity.getClusterId() != null) {
            checkForDuplicateHost(entity);
        }
        return super.save(entity);
    }

    /**
     * 重写父类saveBatch方法，添加重复检查
     * 使用synchronized防止并发插入重复数据
     */
    @Override
    public synchronized boolean saveBatch(Collection<ClusterHostEntity> entityList) {
        // 过滤重复主机，区分处理：已受管=报错，其他=跳过
        List<ClusterHostEntity> filteredList = entityList.stream()
                .filter(entity -> {
                    try {
                        if (entity.getClusterId() != null) {
                            checkForDuplicateHost(entity);
                        }
                        return true; // 没有重复，保留
                    } catch (BusinessException e) {
                        // 根据异常消息区分处理方式
                        if (e.getMessage().contains("受管状态，无法重复添加")) {
                            // 已受管状态：直接抛出异常，中断整个批量操作
                            logger.error("批量保存中断：{}", e.getMessage());
                            throw e;
                        } else {
                            // 未受管或配置中状态：跳过该主机，继续处理其他主机
                            logger.info("跳过重复主机 {}[{}]: {}", 
                                       entity.getHostname(), entity.getIp(), e.getMessage());
                            return false; // 跳过，过滤掉
                        }
                    }
                })
                .toList();
        
        if (filteredList.isEmpty()) {
            logger.info("所有主机都已存在，跳过批量保存");
            return true;
        }
        
        logger.info("批量保存主机：原{}台，过滤后{}台", entityList.size(), filteredList.size());
        return super.saveBatch(filteredList);
    }

    /**
     * 检查重复主机
     * 如果主机IP和hostname已存在：
     * - 未受管状态：跳过添加（抛出特殊异常标识跳过）
     * - 已受管状态：抛出异常
     * - 配置中状态：抛出异常
     * 
     * @param newHost 待添加的主机信息
     * @throws BusinessException 当主机已存在时
     */
    private void checkForDuplicateHost(ClusterHostEntity newHost) {
        // 检查IP重复 - 使用DAO层方法
        if (newHost.getIp() != null) {
            ClusterHostEntity existingHostByIp = clusterHostMapper
                    .selectByClusterIdAndIp(newHost.getClusterId(), newHost.getIp());
            
            if (existingHostByIp != null) {
                handleDuplicateHost(existingHostByIp, "IP", newHost.getIp());
            }
        }

        // 检查主机名重复 - 使用DAO层方法
        if (newHost.getHostname() != null) {
            ClusterHostEntity existingHostByName = clusterHostMapper
                    .selectByClusterIdAndHostname(newHost.getClusterId(), newHost.getHostname());
            
            if (existingHostByName != null) {
                handleDuplicateHost(existingHostByName, "主机名", newHost.getHostname());
            }
        }
    }

    /**
     * 处理重复主机的逻辑
     * 根据用户需求：
     * - 如果主机已受管：报错，无法重复添加
     * - 如果主机未受管或配置中：跳过添加
     * 
     * @param existingHost 已存在的主机
     * @param type 重复类型（IP或主机名）
     * @param value 重复的值
     * @throws BusinessException 只有已受管状态才抛出异常
     */
    private void handleDuplicateHost(ClusterHostEntity existingHost, String type, String value) {
        var managementStatus = existingHost.getManagementStatus();
        
        if (managementStatus == ManagementStatus.MANAGED) {
            // 已受管状态：抛出错误异常
            logger.error("主机{}[{}]已存在且为受管状态，无法重复添加", type, value);
            throw new BusinessException("主机" + type + "[" + value + "]已存在且为受管状态，无法重复添加");
        } else if (managementStatus == ManagementStatus.UNMANAGED || 
                   managementStatus == ManagementStatus.CONFIGURING) {
            // 未受管或配置中状态：记录日志，抛出跳过异常
            String statusText = managementStatus == ManagementStatus.UNMANAGED ? "未受管" : "配置中";
            logger.info("主机{}[{}]已存在且为{}状态，跳过添加", type, value, statusText);
            throw new BusinessException("主机" + type + "[" + value + "]已存在且为" + statusText + "状态，跳过添加");
        }
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

        // 生成机架配置 - Actor功能已迁移到同步执行
        try {
            generateRackConfiguration(clusterId);
        } catch (Exception e) {
            logger.error("生成机架配置失败, clusterId: {}", clusterId, e);
        }
    }

    /**
     * 生成机架配置（使用RackConfigurationService）
     */
    private void generateRackConfiguration(Long clusterId) {
        logger.info("开始生成集群 {} 的机架配置", clusterId);
        GenerateRackPropCommand command = new GenerateRackPropCommand();
        command.setClusterId(clusterId);
        rackConfigurationService.generateRackProperties(command);
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

    @Override
    public int getHostCountByClusterId(Long clusterId) {
        try {
            return (int) QueryChain.of(ClusterHostEntity.class)
                .where(ClusterHostEntity::getClusterId).eq(clusterId)
                .count();
        } catch (Exception e) {
            logger.error("获取集群{}主机总数失败", clusterId, e);
            return 0;
        }
    }

    @Override
    public int getRunningHostCountByClusterId(Long clusterId) {
        try {
            return (int) QueryChain.of(ClusterHostEntity.class)
                .where(ClusterHostEntity::getClusterId).eq(clusterId)
                .and(ClusterHostEntity::getHostState).eq(HostState.RUNNING.getValue())
                .count();
        } catch (Exception e) {
            logger.error("获取集群{}运行中主机数量失败", clusterId, e);
            return 0;
        }
    }
}
