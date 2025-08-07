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

package com.datasophon.api.service.host;

import com.datasophon.api.service.host.strategy.HostManagementStrategy;
import com.datasophon.api.service.host.strategy.HostManagementStrategyFactory;
import com.datasophon.api.service.host.strategy.model.*;
import com.datasophon.dao.entity.ClusterHostDO;
import com.datasophon.dao.entity.ClusterInfoEntity;
import com.datasophon.dao.mapper.ClusterInfoMapper;
import com.datasophon.common.model.ClusterInfoDO;
import com.datasophon.api.converter.ClusterInfoConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 统一主机管理服务
 * 作为主机管理系统的门面，协调不同的策略实现
 * 根据集群类型自动选择合适的策略（PVM或K8S）
 */
@Slf4j
@Service
public class UnifiedHostManagementService {

    @Autowired
    private HostManagementStrategyFactory strategyFactory;

    @Autowired
    private ClusterInfoMapper clusterInfoMapper;
    
    @Autowired
    private ClusterInfoConverter clusterInfoConverter;

    /**
     * 发现主机
     * 根据集群类型自动选择策略发现主机
     *
     * @param clusterId 集群ID
     * @param connectionParams 连接参数
     * @param forceRefresh 是否强制刷新
     * @return 发现结果
     */
    public HostDiscoveryResult discoverHosts(Integer clusterId, Map<String, Object> connectionParams, 
                                           Boolean forceRefresh) {
        try {
            // 获取集群信息
            ClusterInfoDO cluster = getClusterInfo(clusterId);
            
            // 选择策略
            HostManagementStrategy strategy = strategyFactory.getStrategyWithContext(clusterId, cluster.getDepType());
            
            // 构建请求
            HostDiscoveryRequest request = HostDiscoveryRequest.builder()
                    .clusterId(clusterId)
                    .connectionParams(connectionParams)
                    .forceRefresh(forceRefresh != null ? forceRefresh : false)
                    .build();
            
            // 执行发现
            HostDiscoveryResult result = strategy.discoverHosts(request);
            
            log.info("集群{}主机发现完成，策略: {}, 发现主机数: {}", 
                    clusterId, strategy.getStrategyType().getCode(), 
                    result.getSuccess() ? result.getTotalCount() : 0);
            
            return result;
            
        } catch (Exception e) {
            log.error("集群{}主机发现失败", clusterId, e);
            
            return HostDiscoveryResult.builder()
                    .success(false)
                    .errorMessage("主机发现失败: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 获取主机列表
     * 支持分页和筛选，根据集群类型自动选择策略
     *
     * @param clusterId 集群ID
     * @param page 页码
     * @param pageSize 页大小
     * @param hostname 主机名筛选
     * @param ip IP筛选
     * @param cpuArchitecture CPU架构筛选
     * @param hostState 主机状态筛选
     * @param orderField 排序字段
     * @param orderType 排序类型
     * @return 主机列表结果
     */
    public HostListResult getHostList(Integer clusterId, Integer page, Integer pageSize,
                                    String hostname, String ip, String cpuArchitecture, 
                                    Integer hostState, String orderField, String orderType) {
        try {
            // 获取集群信息
            ClusterInfoDO cluster = getClusterInfo(clusterId);
            
            // 选择策略
            HostManagementStrategy strategy = strategyFactory.getStrategyWithContext(clusterId, cluster.getDepType());
            
            // 构建请求
            HostListRequest request = HostListRequest.builder()
                    .clusterId(clusterId)
                    .page(page != null ? page : 1)
                    .pageSize(pageSize != null ? pageSize : 20)
                    .hostname(hostname)
                    .ip(ip)
                    .cpuArchitecture(cpuArchitecture)
                    .hostState(hostState)
                    .orderField(orderField)
                    .orderType(orderType)
                    .build();
            
            // 执行查询
            HostListResult result = strategy.getHostList(request);
            
            log.debug("集群{}主机列表查询完成，策略: {}, 返回主机数: {}", 
                    clusterId, strategy.getStrategyType().getCode(), result.getHosts().size());
            
            return result;
            
        } catch (Exception e) {
            log.error("集群{}主机列表查询失败", clusterId, e);
            
            return HostListResult.builder()
                    .hosts(List.of())
                    .total(0L)
                    .page(page != null ? page : 1)
                    .pageSize(pageSize != null ? pageSize : 20)
                    .hasMore(false)
                    .build();
        }
    }

    /**
     * 导入主机
     * 将用户选择的主机导入到集群中
     *
     * @param clusterId 集群ID
     * @param selectedHosts 选择的主机列表
     * @param connectionParams 连接参数
     * @param importOptions 导入选项
     */
    public void importHosts(Integer clusterId, List<ClusterHostDO> selectedHosts,
                           Map<String, Object> connectionParams, Map<String, Object> importOptions) {
        try {
            // 获取集群信息
            ClusterInfoDO cluster = getClusterInfo(clusterId);
            
            // 选择策略
            HostManagementStrategy strategy = strategyFactory.getStrategyWithContext(clusterId, cluster.getDepType());
            
            // 构建请求
            HostImportRequest request = HostImportRequest.builder()
                    .clusterId(clusterId)
                    .selectedHosts(selectedHosts)
                    .connectionParams(connectionParams)
                    .importOptions(importOptions)
                    .build();
            
            // 执行导入
            strategy.importHosts(request);
            
            log.info("集群{}主机导入完成，策略: {}, 导入主机数: {}", 
                    clusterId, strategy.getStrategyType().getCode(), selectedHosts.size());
            
        } catch (Exception e) {
            log.error("集群{}主机导入失败", clusterId, e);
            throw new RuntimeException("主机导入失败: " + e.getMessage(), e);
        }
    }

    /**
     * 刷新主机信息
     * 重新获取主机的最新状态
     *
     * @param clusterId 集群ID
     * @param connectionParams 连接参数
     * @return 刷新后的主机列表
     */
    public List<ClusterHostDO> refreshHosts(Integer clusterId, Map<String, Object> connectionParams) {
        try {
            // 获取集群信息
            ClusterInfoDO cluster = getClusterInfo(clusterId);
            
            // 选择策略
            HostManagementStrategy strategy = strategyFactory.getStrategyWithContext(clusterId, cluster.getDepType());
            
            // 执行刷新
            List<ClusterHostDO> refreshedHosts = strategy.refreshHosts(clusterId, connectionParams);
            
            log.info("集群{}主机信息刷新完成，策略: {}, 刷新主机数: {}", 
                    clusterId, strategy.getStrategyType().getCode(), refreshedHosts.size());
            
            return refreshedHosts;
            
        } catch (Exception e) {
            log.error("集群{}主机信息刷新失败", clusterId, e);
            throw new RuntimeException("主机信息刷新失败: " + e.getMessage(), e);
        }
    }

    /**
     * 检查连接状态
     * 验证是否能正常连接到目标环境
     *
     * @param clusterId 集群ID
     * @param connectionParams 连接参数
     * @return 连接状态信息
     */
    public Map<String, Object> checkConnection(Integer clusterId, Map<String, Object> connectionParams) {
        try {
            // 获取集群信息
            ClusterInfoDO cluster = getClusterInfo(clusterId);
            
            // 选择策略
            HostManagementStrategy strategy = strategyFactory.getStrategyWithContext(clusterId, cluster.getDepType());
            
            // 执行连接检查
            Map<String, Object> result = strategy.checkConnection(connectionParams);
            
            log.info("集群{}连接检查完成，策略: {}, 连接状态: {}", 
                    clusterId, strategy.getStrategyType().getCode(), result.get("connected"));
            
            return result;
            
        } catch (Exception e) {
            log.error("集群{}连接检查失败", clusterId, e);
            throw new RuntimeException("连接检查失败: " + e.getMessage(), e);
        }
    }

    /**
     * 执行主机环境检查
     * 对主机进行环境校验
     *
     * @param clusterId 集群ID
     * @param hostnames 主机名列表
     * @param connectionParams 连接参数
     * @return 检查结果
     */
    public Map<String, Object> performHostCheck(Integer clusterId, List<String> hostnames,
                                              Map<String, Object> connectionParams) {
        try {
            // 获取集群信息
            ClusterInfoDO cluster = getClusterInfo(clusterId);
            
            // 选择策略
            HostManagementStrategy strategy = strategyFactory.getStrategyWithContext(clusterId, cluster.getDepType());
            
            // 执行主机检查
            Map<String, Object> result = strategy.performHostCheck(clusterId, hostnames, connectionParams);
            
            log.info("集群{}主机环境检查完成，策略: {}, 检查主机数: {}", 
                    clusterId, strategy.getStrategyType().getCode(), hostnames.size());
            
            return result;
            
        } catch (Exception e) {
            log.error("集群{}主机环境检查失败", clusterId, e);
            throw new RuntimeException("主机环境检查失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取主机检查状态
     * 查询主机环境检查的进度和结果
     *
     * @param clusterId 集群ID
     * @return 检查状态
     */
    public Map<String, Object> getHostCheckStatus(Integer clusterId) {
        try {
            // 获取集群信息
            ClusterInfoDO cluster = getClusterInfo(clusterId);
            
            // 选择策略
            HostManagementStrategy strategy = strategyFactory.getStrategyWithContext(clusterId, cluster.getDepType());
            
            // 获取检查状态
            Map<String, Object> result = strategy.getHostCheckStatus(clusterId);
            
            log.debug("集群{}主机检查状态查询完成，策略: {}", 
                    clusterId, strategy.getStrategyType().getCode());
            
            return result;
            
        } catch (Exception e) {
            log.error("集群{}主机检查状态查询失败", clusterId, e);
            throw new RuntimeException("主机检查状态查询失败: " + e.getMessage(), e);
        }
    }

    /**
     * 清理资源
     * 清理指定集群的主机管理相关资源
     *
     * @param clusterId 集群ID
     */
    public void cleanup(Integer clusterId) {
        try {
            // 获取集群信息
            ClusterInfoDO cluster = getClusterInfo(clusterId);
            
            // 选择策略
            HostManagementStrategy strategy = strategyFactory.getStrategyWithContext(clusterId, cluster.getDepType());
            
            // 执行清理
            strategy.cleanup(clusterId);
            
            log.info("集群{}资源清理完成，策略: {}", 
                    clusterId, strategy.getStrategyType().getCode());
            
        } catch (Exception e) {
            log.error("集群{}资源清理失败", clusterId, e);
            // 清理失败不抛异常，只记录日志
        }
    }

    /**
     * 根据部署类型直接获取策略
     * 用于不需要集群信息的场景
     *
     * @param depType 部署类型
     * @return 策略实例
     */
    public HostManagementStrategy getStrategyByDepType(String depType) {
        return strategyFactory.getStrategyByDepType(depType);
    }

    /**
     * 获取所有支持的策略类型
     * 用于前端显示可选的部署模式
     *
     * @return 策略类型映射
     */
    public Map<HostManagementStrategy.StrategyType, HostManagementStrategy> getAllStrategies() {
        return strategyFactory.getAllStrategies();
    }

    /**
     * 获取集群信息
     *
     * @param clusterId 集群ID
     * @return 集群信息
     */
    private ClusterInfoDO getClusterInfo(Integer clusterId) {
        if (clusterId == null) {
            throw new IllegalArgumentException("集群ID不能为空");
        }
        
        ClusterInfoEntity entity = clusterInfoMapper.selectOneById(clusterId);
        if (entity == null) {
            throw new IllegalArgumentException("集群不存在: " + clusterId);
        }
        
        // 使用MapStruct转换器
        return clusterInfoConverter.entityToDo(entity);
    }
}