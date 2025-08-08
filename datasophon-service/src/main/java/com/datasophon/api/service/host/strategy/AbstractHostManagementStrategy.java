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

package com.datasophon.api.service.host.strategy;

import com.datasophon.api.service.host.strategy.model.*;
import com.datasophon.dao.entity.ClusterHostDO;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 主机管理策略抽象基类
 * 使用模板方法模式定义主机管理的通用流程
 */
@Slf4j
public abstract class AbstractHostManagementStrategy implements HostManagementStrategy {

    /**
     * 模板方法：发现主机
     * 定义发现主机的通用流程，具体步骤由子类实现
     */
    @Override
    public final HostDiscoveryResult discoverHosts(HostDiscoveryRequest request) {
        long startTime = System.currentTimeMillis();
        
        try {
            log.info("开始发现主机，策略类型: {}, 集群ID: {}", getStrategyType(), request.getClusterId());
            
            // 1. 验证请求参数
            validateDiscoveryRequest(request);
            
            // 2. 准备连接
            prepareConnection(request.getConnectionParams());
            
            // 3. 执行发现（由子类实现）
            List<ClusterHostDO> discoveredHosts = doDiscoverHosts(request);
            
            // 4. 后处理发现的主机
            List<ClusterHostDO> processedHosts = postProcessDiscoveredHosts(discoveredHosts, request);
            
            // 5. 构建结果
            long discoveryTime = System.currentTimeMillis() - startTime;
            
            return HostDiscoveryResult.builder()
                    .hosts(processedHosts)
                    .totalCount(processedHosts.size())
                    .success(true)
                    .discoveryTime(discoveryTime)
                    .metadata(buildDiscoveryMetadata(processedHosts, request))
                    .build();
                    
        } catch (Exception e) {
            log.error("发现主机失败，策略类型: {}, 集群ID: {}", getStrategyType(), request.getClusterId(), e);
            
            long discoveryTime = System.currentTimeMillis() - startTime;
            
            return HostDiscoveryResult.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .discoveryTime(discoveryTime)
                    .build();
        }
    }

    /**
     * 模板方法：获取主机列表
     */
    @Override
    public final HostListResult getHostList(HostListRequest request) {
        try {
            log.debug("获取主机列表，策略类型: {}, 集群ID: {}", getStrategyType(), request.getClusterId());
            
            // 1. 验证请求参数
            validateListRequest(request);
            
            // 2. 执行查询（由子类实现）
            HostListResult result = doGetHostList(request);
            
            // 3. 添加统计信息
            result.setStatistics(buildListStatistics(result.getHosts()));
            
            return result;
            
        } catch (Exception e) {
            log.error("获取主机列表失败，策略类型: {}, 集群ID: {}", getStrategyType(), request.getClusterId(), e);
            
            return HostListResult.builder()
                    .hosts(List.of())
                    .total(0L)
                    .page(request.getPage())
                    .pageSize(request.getPageSize())
                    .hasMore(false)
                    .build();
        }
    }

    /**
     * 模板方法：导入主机
     */
    @Override
    public final void importHosts(HostImportRequest request) {
        try {
            log.info("开始导入主机，策略类型: {}, 集群ID: {}, 主机数量: {}", 
                    getStrategyType(), request.getClusterId(), request.getSelectedHosts().size());
            
            // 1. 验证导入请求
            validateImportRequest(request);
            
            // 2. 预处理主机数据
            List<ClusterHostDO> processedHosts = preProcessHostsForImport(request.getSelectedHosts(), request);
            
            // 3. 执行导入（由子类实现）
            doImportHosts(processedHosts, request);
            
            // 4. 后处理
            postProcessImportedHosts(processedHosts, request);
            
            log.info("成功导入{}台主机", processedHosts.size());
            
        } catch (Exception e) {
            log.error("导入主机失败，策略类型: {}, 集群ID: {}", getStrategyType(), request.getClusterId(), e);
            throw new RuntimeException("导入主机失败: " + e.getMessage(), e);
        }
    }

    // ==================== 抽象方法，由子类实现 ====================

    /**
     * 执行主机发现的具体逻辑
     */
    protected abstract List<ClusterHostDO> doDiscoverHosts(HostDiscoveryRequest request);

    /**
     * 执行获取主机列表的具体逻辑
     */
    protected abstract HostListResult doGetHostList(HostListRequest request);

    /**
     * 执行导入主机的具体逻辑
     */
    protected abstract void doImportHosts(List<ClusterHostDO> hosts, HostImportRequest request);

    // ==================== 钩子方法，子类可选择性重写 ====================

    /**
     * 验证发现请求参数
     */
    protected void validateDiscoveryRequest(HostDiscoveryRequest request) {
        if (request.getClusterId() == null) {
            throw new IllegalArgumentException("集群ID不能为空");
        }
        if (request.getConnectionParams() == null || request.getConnectionParams().isEmpty()) {
            throw new IllegalArgumentException("连接参数不能为空");
        }
    }

    /**
     * 验证列表请求参数
     */
    protected void validateListRequest(HostListRequest request) {
        if (request.getClusterId() == null) {
            throw new IllegalArgumentException("集群ID不能为空");
        }
        if (request.getPage() <= 0) {
            request.setPage(1);
        }
        if (request.getPageSize() <= 0 || request.getPageSize() > 100) {
            request.setPageSize(20);
        }
    }

    /**
     * 验证导入请求参数
     */
    protected void validateImportRequest(HostImportRequest request) {
        if (request.getClusterId() == null) {
            throw new IllegalArgumentException("集群ID不能为空");
        }
        if (request.getSelectedHosts() == null || request.getSelectedHosts().isEmpty()) {
            throw new IllegalArgumentException("选择的主机列表不能为空");
        }
    }

    /**
     * 准备连接
     */
    protected void prepareConnection(Map<String, Object> connectionParams) {
        // 默认实现为空，子类可根据需要重写
    }

    /**
     * 后处理发现的主机
     */
    protected List<ClusterHostDO> postProcessDiscoveredHosts(List<ClusterHostDO> hosts, HostDiscoveryRequest request) {
        // 默认实现：设置集群ID
        hosts.forEach(host -> {
            if (host.getClusterId() == null) {
                host.setClusterId(request.getClusterId());
            }
        });
        return hosts;
    }

    /**
     * 预处理要导入的主机
     */
    protected List<ClusterHostDO> preProcessHostsForImport(List<ClusterHostDO> hosts, HostImportRequest request) {
        // 默认实现：确保集群ID正确
        hosts.forEach(host -> host.setClusterId(request.getClusterId()));
        return hosts;
    }

    /**
     * 后处理已导入的主机
     */
    protected void postProcessImportedHosts(List<ClusterHostDO> hosts, HostImportRequest request) {
        // 默认实现为空，子类可根据需要重写
    }

    /**
     * 构建发现元数据
     */
    protected Map<String, Object> buildDiscoveryMetadata(List<ClusterHostDO> hosts, HostDiscoveryRequest request) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("strategyType", getStrategyType().getCode());
        metadata.put("discoveredCount", hosts.size());
        return metadata;
    }

    /**
     * 构建列表统计信息
     */
    protected Map<String, Object> buildListStatistics(List<ClusterHostDO> hosts) {
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalHosts", hosts.size());
        
        // 使用新的ManagementStatus字段进行统计，配置中状态不计入受管
        long managedCount = hosts.stream()
                .filter(h -> h.getManagementStatus() != null && h.getManagementStatus().isManaged())
                .count();
        long configuringCount = hosts.stream()
                .filter(h -> h.getManagementStatus() != null && h.getManagementStatus().isConfiguring())
                .count();
        long unmanagedCount = hosts.size() - managedCount - configuringCount;
        
        statistics.put("managedHosts", managedCount);
        statistics.put("unmanagedHosts", unmanagedCount); 
        statistics.put("configuringHosts", configuringCount);
        return statistics;
    }
}