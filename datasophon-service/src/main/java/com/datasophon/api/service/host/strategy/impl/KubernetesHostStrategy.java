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

package com.datasophon.api.service.host.strategy.impl;

import com.datasophon.api.service.host.ClusterHostService;
import com.datasophon.api.service.host.strategy.AbstractHostManagementStrategy;
import com.datasophon.api.service.host.strategy.model.*;


import com.datasophon.api.converter.K8sToClusterHostConverter;
import com.datasophon.dao.entity.ClusterHostEntity;
import com.datasophon.common.enums.HostState;
import com.datasophon.common.enums.ManagementStatus;
import com.datasophon.kubernetes.model.K8sNodeInfo;
import com.datasophon.kubernetes.util.KubeUtil;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Kubernetes主机管理策略实现
 * 专门处理K8S模式下的主机发现、管理和导入
 */
@Slf4j
@Component
public class KubernetesHostStrategy extends AbstractHostManagementStrategy {

    @Autowired
    private ClusterHostService clusterHostService;

    @Autowired
    private K8sToClusterHostConverter k8sToClusterHostConverter;



    // K8S主机临时存储，替代缓存
    private final Map<Long, List<ClusterHostEntity>> k8sHostsStorage = new HashMap<>();

    // K8S节点信息临时存储
    private final Map<Long, List<K8sNodeInfo>> k8sNodeInfoStorage = new HashMap<>();

    @Override
    public StrategyType getStrategyType() {
        return StrategyType.KUBERNETES;
    }

    @Override
    protected void validateDiscoveryRequest(HostDiscoveryRequest request) {
        super.validateDiscoveryRequest(request);
        
        String kubeConfigContent = (String) request.getConnectionParams().get("kubeConfigContent");
        if (kubeConfigContent == null || kubeConfigContent.trim().isEmpty()) {
            throw new IllegalArgumentException("K8S配置内容不能为空");
        }
    }

    @Override
    protected void prepareConnection(Map<String, Object> connectionParams) {
        String kubeConfigContent = (String) connectionParams.get("kubeConfigContent");
        
        try (KubernetesClient client = KubeUtil.getKubeClientByConfig(kubeConfigContent)) {
            // 测试连接
            KubeUtil.testConnect(client);
            log.info("K8S连接测试成功");
        } catch (Exception e) {
            throw new RuntimeException("K8S连接测试失败: " + e.getMessage(), e);
        }
    }

    @Override
    protected List<ClusterHostEntity> doDiscoverHosts(HostDiscoveryRequest request) {
        String kubeConfigContent = (String) request.getConnectionParams().get("kubeConfigContent");
        String namespace = (String) request.getConnectionParams().get("namespace");
        
        log.info("开始从K8S集群发现主机，namespace: {}", namespace);
        
        try {
            // 从K8S API获取节点信息
            List<K8sNodeInfo> k8sNodes = KubeUtil.getHostListByConfig(kubeConfigContent);
            
            // 过滤并存储原始K8S节点信息
            List<K8sNodeInfo> validK8sNodes = k8sNodes.stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            k8sNodeInfoStorage.put(request.getClusterId(), validK8sNodes);
            
            // 转换为ClusterHostDO对象（用于传统流程兼容）
            List<ClusterHostEntity> hosts = validK8sNodes.stream()
                    .map(k8sNode -> k8sToClusterHostConverter.convertToClusterHost(k8sNode, request.getClusterId()))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            
            // 🔧 修复：合并数据库中已有主机的状态信息
            hosts = mergeWithExistingHostStatus(hosts, request.getClusterId());
            
            // 存储到临时存储中
            k8sHostsStorage.put(request.getClusterId(), hosts);
            
            log.info("成功从K8S集群发现{}台主机", hosts.size());
            return hosts;
            
        } catch (Exception e) {
            log.error("从K8S集群发现主机失败", e);
            throw new RuntimeException("从K8S集群发现主机失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 合并数据库中已有主机的状态信息
     * 🔧 查询所有主机，根据IP进行状态更新，以数据库状态为最高优先级
     */
    private List<ClusterHostEntity> mergeWithExistingHostStatus(List<ClusterHostEntity> discoveredHosts, Long clusterId) {
        if (discoveredHosts == null || discoveredHosts.isEmpty()) {
            return discoveredHosts;
        }
        
        try {
            // 🔧 查询数据库中的所有主机信息（不限集群ID）
            List<ClusterHostEntity> allExistingHosts = clusterHostService.list();
            
            if (allExistingHosts == null || allExistingHosts.isEmpty()) {
                log.info("数据库中没有任何主机记录，直接返回发现的主机列表");
                return discoveredHosts;
            }
            
            // 创建IP到已有主机的映射（全局范围）
            Map<String, ClusterHostEntity> existingHostMap = allExistingHosts.stream()
                    .collect(Collectors.toMap(
                        ClusterHostEntity::getIp, 
                        host -> host, 
                        (existing, replacement) -> {
                            // 如果IP重复，优先选择受管状态的主机
                            if (existing.getManagementStatus() == ManagementStatus.MANAGED) {
                                return existing;
                            }
                            return replacement.getManagementStatus() == ManagementStatus.MANAGED ? replacement : existing;
                        }));
            
            // 合并状态信息
            List<ClusterHostEntity> mergedHosts = discoveredHosts.stream()
                    .map(discoveredHost -> {
                        ClusterHostEntity existingHost = existingHostMap.get(discoveredHost.getIp());
                        if (existingHost != null) {
                            // 🔧 发现的主机在全局数据库中已存在，优先使用数据库中的受管状态
                            discoveredHost.setId(existingHost.getId());
                            discoveredHost.setManagementStatus(existingHost.getManagementStatus());
                            discoveredHost.setCreateTime(existingHost.getCreateTime());
                            
                            log.debug("合并全局主机状态 - IP: {}, 数据库状态: {} (来源集群: {}) → 保留", 
                                    discoveredHost.getIp(), 
                                    existingHost.getManagementStatus() != null ? existingHost.getManagementStatus().getDesc() : "null",
                                    existingHost.getClusterId());
                        } else {
                            // 🔧 全新发现的主机，保持转换器设置的初始状态（UNMANAGED）
                            log.debug("全新发现主机 - IP: {}, 初始状态: {}", 
                                    discoveredHost.getIp(), 
                                    discoveredHost.getManagementStatus() != null ? discoveredHost.getManagementStatus().getDesc() : "null");
                        }
                        return discoveredHost;
                    })
                    .toList(); // Java 21 简化写法
            
            log.info("全局主机状态合并完成 - 发现主机: {}台, 全局已有主机: {}台, 当前集群: {}", 
                    discoveredHosts.size(), allExistingHosts.size(), clusterId);
            return mergedHosts;
            
        } catch (Exception e) {
            log.error("合并全局主机状态失败，返回原始发现列表", e);
            return discoveredHosts;
        }
    }

    /**
     * 获取K8S节点信息（用于前端展示）
     */
    public List<K8sNodeInfo> getK8sNodeInfoList(Long clusterId) {
        return k8sNodeInfoStorage.get(clusterId);
    }

    @Override
    protected HostListResult doGetHostList(HostListRequest request) {
        Long clusterId = request.getClusterId();
        
        // 优先从临时存储获取
        List<ClusterHostEntity> hosts = k8sHostsStorage.get(clusterId);
        
        if (hosts == null || hosts.isEmpty()) {
            // 如果临时存储没有，尝试从数据库获取已导入的主机
            hosts = clusterHostService.getAllManagedHostsByClusterId(clusterId);
        }
        
        if (hosts == null) {
            hosts = new ArrayList<>();
        }
        
        // 应用筛选条件
        List<ClusterHostEntity> filteredHosts = applyFilters(hosts, request);
        
        // K8S模式不支持分页，返回所有结果
        return HostListResult.builder()
                .hosts(filteredHosts)
                .total((long) filteredHosts.size())
                .page(1)
                .pageSize(filteredHosts.size())
                .hasMore(false)
                .build();
    }

    @Override
    protected void doImportHosts(List<ClusterHostEntity> hosts, HostImportRequest request) {
        try {
            log.info("开始导入{}台K8S主机到数据库", hosts.size());
            
            // 1. 检查IP重复
            List<String> ipList = hosts.stream().map(ClusterHostEntity::getIp).toList();
            List<ClusterHostEntity> existingHosts = clusterHostService.getHostsByIpList(request.getClusterId(), ipList);
            
            if (!existingHosts.isEmpty()) {
                List<String> duplicateIps = existingHosts.stream().map(ClusterHostEntity::getIp).toList();
                log.warn("发现重复IP，将跳过这些主机：{}", duplicateIps);
                
                // 过滤掉重复IP的主机
                hosts = hosts.stream()
                        .filter(host -> !duplicateIps.contains(host.getIp()))
                        .toList();
                
                if (hosts.isEmpty()) {
                    log.warn("所有主机IP都已存在，跳过导入");
                    return;
                }
                log.info("过滤重复IP后，实际导入{}台主机", hosts.size());
            }
            
            // 2. 批量保存主机
            clusterHostService.saveBatch(hosts);
            
            // 3. 清理临时存储
            k8sHostsStorage.remove(request.getClusterId());
            
            log.info("成功导入{}台K8S主机", hosts.size());
            
        } catch (Exception e) {
            log.error("导入K8S主机失败", e);
            throw new RuntimeException("导入K8S主机失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<ClusterHostEntity> refreshHosts(Long clusterId, Map<String, Object> connectionParams) {
        HostDiscoveryRequest request = HostDiscoveryRequest.builder()
                .clusterId(clusterId)
                .connectionParams(connectionParams)
                .forceRefresh(true)
                .build();
        
        HostDiscoveryResult result = discoverHosts(request);
        if (result.getSuccess()) {
            return result.getHosts();
        } else {
            throw new RuntimeException("刷新K8S主机失败: " + result.getErrorMessage());
        }
    }

    @Override
    public Map<String, Object> checkConnection(Map<String, Object> connectionParams) {
        String kubeConfigContent = (String) connectionParams.get("kubeConfigContent");
        Map<String, Object> result = new HashMap<>();
        
        try (KubernetesClient client = KubeUtil.getKubeClientByConfig(kubeConfigContent)) {
            // 测试连接并获取集群信息
            KubeUtil.testConnect(client);
            
            String masterUrl = client.getMasterUrl().toString();
            String version = client.getKubernetesVersion().getGitVersion();
            
            result.put("connected", true);
            result.put("masterUrl", masterUrl);
            result.put("version", version);
            result.put("message", "K8S连接成功");
            
            log.info("K8S连接检查成功，集群地址: {}, 版本: {}", masterUrl, version);
            
        } catch (Exception e) {
            result.put("connected", false);
            result.put("error", e.getMessage());
            result.put("message", "K8S连接失败: " + e.getMessage());
            
            log.error("K8S连接检查失败", e);
        }
        
        return result;
    }

    @Override
    public Map<String, Object> performHostCheck(Long clusterId, List<String> hostnames, 
                                              Map<String, Object> connectionParams) {
        // K8S模式下的主机检查逻辑
        Map<String, Object> result = new HashMap<>();
        
        // K8S节点通常不需要传统的SSH检查，主要检查节点状态
        List<ClusterHostEntity> hosts = k8sHostsStorage.get(clusterId);
        if (hosts != null) {
            long readyHosts = hosts.stream()
                    .filter(host -> hostnames.contains(host.getHostname()))
                    .filter(host -> HostState.RUNNING.equals(host.getHostState()))
                    .count();
            
            result.put("checkedHosts", hostnames.size());
            result.put("readyHosts", readyHosts);
            result.put("completed", true);
            result.put("message", String.format("检查完成，%d/%d 台主机就绪", readyHosts, hostnames.size()));
        } else {
            result.put("completed", false);
            result.put("message", "未找到主机信息，请先发现主机");
        }
        
        return result;
    }

    @Override
    public Map<String, Object> getHostCheckStatus(Long clusterId) {
        Map<String, Object> result = new HashMap<>();
        
        List<ClusterHostEntity> hosts = k8sHostsStorage.get(clusterId);
        if (hosts != null && !hosts.isEmpty()) {
            long readyHosts = hosts.stream()
                    .filter(host -> HostState.RUNNING.equals(host.getHostState()))
                    .count();
            
            result.put("totalHosts", hosts.size());
            result.put("readyHosts", readyHosts);
            result.put("completed", true);
            result.put("progress", (double) readyHosts / hosts.size());
        } else {
            result.put("completed", false);
            result.put("progress", 0.0);
        }
        
        return result;
    }

    @Override
    public void cleanup(Long clusterId) {
        // 清理临时存储
        k8sHostsStorage.remove(clusterId);
        k8sNodeInfoStorage.remove(clusterId);
        log.info("已清理集群{}的K8S主机临时数据", clusterId);
    }

    @Override
    public Map<String, Object> validateForNextStep(Long clusterId) {
        Map<String, Object> result = new HashMap<>();
        List<ClusterHostEntity> hosts = k8sHostsStorage.get(clusterId);
        if (hosts == null || hosts.isEmpty()) {
            // 回退到数据库读取
            hosts = clusterHostService.getAllManagedHostsByClusterId(clusterId);
        }

        if (hosts == null || hosts.isEmpty()) {
            result.put("valid", false);
            result.put("message", "集群中没有发现任何主机，请先完成主机发现");
            result.put("totalHosts", 0);
            result.put("unmanagedHosts", 0);
            result.put("readyHosts", 0);
            return result;
        }

        long total = hosts.size();
        // 统计可配置的主机（未受管和配置中状态）
        long unmanaged = hosts.stream().filter(h -> h.getManagementStatus().canConfigure()).count();
        long ready = hosts.stream().filter(h -> HostState.RUNNING.equals(h.getHostState())).count();

        boolean allUnmanaged = unmanaged == total;
        boolean allReady = ready == total;
        boolean valid = allUnmanaged && allReady;

        result.put("valid", valid);
        result.put("totalHosts", total);
        result.put("unmanagedHosts", unmanaged);
        result.put("readyHosts", ready);
        result.put("managedHosts", total - unmanaged);
        result.put("notReadyHosts", total - ready);
        
        // 添加详细的调试信息
        log.info("主机校验详情 - 集群ID: {}, 总主机数: {}, 未受管数: {}, Ready数: {}, 校验结果: {}",
                clusterId, total, unmanaged, ready, valid);
        
        // 打印每台主机的状态（仅在校验失败时）
        if (!valid) {
            hosts.forEach(host -> {
                log.info("主机状态详情 - IP: {}, 管理状态: {}, 主机状态: {}", 
                        host.getIp(), host.getManagementStatus(), host.getHostState());
            });
        }
        
        if (valid) {
            result.put("message", String.format("校验通过：所有 %d 台主机都是未受管状态且Ready", total));
        } else {
            if (!allUnmanaged) {
                long managed = total - unmanaged;
                result.put("message", String.format("校验失败：存在 %d 台已受管主机，请确保所有主机都是未受管状态", managed));
            } else {
                long notReady = total - ready;
                result.put("message", String.format("校验失败：存在 %d 台非Ready状态主机，请确保所有主机状态都是Ready", notReady));
            }
        }
        return result;
    }
    /**
     * 应用筛选条件
     */
    private List<ClusterHostEntity> applyFilters(List<ClusterHostEntity> hosts, HostListRequest request) {
        return hosts.stream()
                .filter(host -> {
                    // 主机名筛选
                    if (request.getHostname() != null && !request.getHostname().trim().isEmpty()) {
                        return host.getHostname().contains(request.getHostname());
                    }
                    return true;
                })
                .filter(host -> {
                    // IP筛选
                    if (request.getIp() != null && !request.getIp().trim().isEmpty()) {
                        return host.getIp().contains(request.getIp());
                    }
                    return true;
                })
                .filter(host -> {
                    // CPU架构筛选
                    if (request.getCpuArchitecture() != null && !request.getCpuArchitecture().trim().isEmpty()) {
                        return request.getCpuArchitecture().equals(host.getCpuArchitecture());
                    }
                    return true;
                })
                .filter(host -> {
                    // 主机状态筛选
                    if (request.getHostState() != null) {
                        return request.getHostState().equals(host.getHostState().getValue());
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }
}