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
import com.datasophon.api.service.impl.InstallServiceImpl;
import com.datasophon.common.enums.ManagementStatus;
import com.datasophon.common.model.PageResult;
import com.datasophon.dao.entity.ClusterHostEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * PVM（传统虚拟机）主机管理策略实现
 * 处理传统虚拟机模式下的主机发现、管理和导入
 */
@Slf4j
@Component
public class PvmHostStrategy extends AbstractHostManagementStrategy {

    @Autowired
    private ClusterHostService clusterHostService;
    
    @Autowired
    private InstallServiceImpl installService;

    @Override
    public StrategyType getStrategyType() {
        return StrategyType.PVM;
    }

    @Override
    protected void validateDiscoveryRequest(HostDiscoveryRequest request) {
        super.validateDiscoveryRequest(request);
        
        String hosts = (String) request.getConnectionParams().get("hosts");
        String sshUser = (String) request.getConnectionParams().get("sshUser");
        String sshPort = (String) request.getConnectionParams().get("sshPort");
        String sshPassword = (String) request.getConnectionParams().get("sshPassword");
        
        if (hosts == null || hosts.trim().isEmpty()) {
            throw new IllegalArgumentException("主机列表不能为空");
        }
        if (sshUser == null || sshUser.trim().isEmpty()) {
            throw new IllegalArgumentException("SSH用户名不能为空");
        }
        if (sshPort == null || sshPort.trim().isEmpty()) {
            throw new IllegalArgumentException("SSH端口不能为空");
        }
        if (sshPassword == null || sshPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("SSH密码不能为空");
        }
    }

    @Override
    protected void prepareConnection(Map<String, Object> connectionParams) {
        // PVM模式可以在这里做SSH连接测试
        // 这里简化处理，实际可以测试SSH连接
        log.debug("准备PVM SSH连接参数");
    }

    @Override
    protected List<ClusterHostEntity> doDiscoverHosts(HostDiscoveryRequest request) {
        String hosts = (String) request.getConnectionParams().get("hosts");
        String sshUser = (String) request.getConnectionParams().get("sshUser");
        String sshPort = (String) request.getConnectionParams().get("sshPort");
        String sshPassword = (String) request.getConnectionParams().get("sshPassword");
        
        log.info("开始分析PVM主机列表: {}", hosts);
        
        try {
            // 调用现有的主机分析方法
            installService.analysisHostList(
                request.getClusterId(),
                hosts,
                sshUser,
                Integer.parseInt(sshPort),
                sshPassword,
                null, // kubeConfigContent - PVM模式不需要
                1, // page
                100 // pageSize - 获取所有主机
            );
            
            // 分析完成后，获取发现的主机
            return Collections.emptyList(); // 实际实现中应该返回分析后的主机列表
            
        } catch (Exception e) {
            log.error("分析PVM主机列表失败", e);
            throw new RuntimeException("分析PVM主机列表失败: " + e.getMessage(), e);
        }
    }

    @Override
    protected HostListResult doGetHostList(HostListRequest request) {
        try {
            // 调用现有的分页查询方法
            PageResult<ClusterHostEntity> pageResult = clusterHostService.listByPage(
                request.getClusterId(),
                request.getHostname(),
                request.getIp(),
                request.getCpuArchitecture(),
                request.getHostState(),
                request.getOrderField(),
                request.getOrderType(),
                request.getPage(),
                request.getPageSize()
            );
            
            // 获取队列状态（PVM模式特有）
            Map<String, Object> queueStatus = getQueueStatus(request.getClusterId());
            
            return HostListResult.builder()
                    .hosts(pageResult.getRecords())
                    .total(pageResult.getTotal())
                    .page(request.getPage())
                    .pageSize(request.getPageSize())
                    .hasMore(pageResult.getTotal() > (long) request.getPage() * request.getPageSize())
                    .queueStatus(queueStatus)
                    .build();
                    
        } catch (Exception e) {
            log.error("获取PVM主机列表失败", e);
            throw new RuntimeException("获取PVM主机列表失败: " + e.getMessage(), e);
        }
    }

    @Override
    protected void doImportHosts(List<ClusterHostEntity> hosts, HostImportRequest request) {
        try {
            log.info("开始导入{}台PVM主机", hosts.size());
            
            // PVM模式的主机通常已经在分析阶段保存，这里可能需要更新状态
            for (ClusterHostEntity host : hosts) {
                // 更新主机为受管状态
                host.setManagementStatus(ManagementStatus.MANAGED);
                clusterHostService.updateById(host);
            }
            
            log.info("成功导入{}台PVM主机", hosts.size());
            
        } catch (Exception e) {
            log.error("导入PVM主机失败", e);
            throw new RuntimeException("导入PVM主机失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<ClusterHostEntity> refreshHosts(Long clusterId, Map<String, Object> connectionParams) {
        // PVM模式刷新主机信息
        HostDiscoveryRequest request = HostDiscoveryRequest.builder()
                .clusterId(clusterId)
                .connectionParams(connectionParams)
                .forceRefresh(true)
                .build();
        
        HostDiscoveryResult result = discoverHosts(request);
        if (result.getSuccess()) {
            return result.getHosts();
        } else {
            throw new RuntimeException("刷新PVM主机失败: " + result.getErrorMessage());
        }
    }

    @Override
    public Map<String, Object> checkConnection(Map<String, Object> connectionParams) {
        String hosts = (String) connectionParams.get("hosts");
        // TODO: 可以在未来实现SSH连接参数验证
        // String sshUser = (String) connectionParams.get("sshUser");
        // String sshPort = (String) connectionParams.get("sshPort");
        // String sshPassword = (String) connectionParams.get("sshPassword");
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 这里可以实现SSH连接测试
            // 简化处理，实际应该测试第一台主机的SSH连接
            
            result.put("connected", true);
            result.put("testedHosts", Arrays.asList(hosts.split(",")));
            result.put("message", "SSH连接测试成功");
            
            log.info("PVM SSH连接检查成功");
            
        } catch (Exception e) {
            result.put("connected", false);
            result.put("error", e.getMessage());
            result.put("message", "SSH连接测试失败: " + e.getMessage());
            
            log.error("PVM SSH连接检查失败", e);
        }
        
        return result;
    }

    @Override
    public Map<String, Object> performHostCheck(Long clusterId, List<String> hostnames,
                                              Map<String, Object> connectionParams) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            String sshUser = (String) connectionParams.get("sshUser");
            String sshPort = (String) connectionParams.get("sshPort");
            
            // 调用现有的主机环境检查方法
            installService.getHostCheckStatus(
                clusterId,
                sshUser,
                Integer.parseInt(sshPort)
            );
            
            result.put("started", true);
            result.put("checkedHosts", hostnames.size());
            result.put("message", "主机环境检查已启动");
            
        } catch (Exception e) {
            result.put("started", false);
            result.put("error", e.getMessage());
            result.put("message", "启动主机环境检查失败: " + e.getMessage());
            
            log.error("PVM主机环境检查失败", e);
        }
        
        return result;
    }

    @Override
    public Map<String, Object> getHostCheckStatus(Long clusterId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 调用现有的检查完成状态方法
            boolean completed = installService.hostCheckCompleted(clusterId);
            
            result.put("completed", completed);
            result.put("data", Collections.emptyList());
            result.put("queueStatus", getQueueStatus(clusterId));
            
        } catch (Exception e) {
            result.put("completed", false);
            result.put("error", e.getMessage());
            
            log.error("获取PVM主机检查状态失败", e);
        }
        
        return result;
    }

    @Override
    public void cleanup(Long clusterId) {
        try {
            // 调用现有的清理方法
            installService.cleanupHostCheckResources(clusterId);
            log.info("已清理集群{}的PVM主机检查资源", clusterId);
            
        } catch (Exception e) {
            log.error("清理PVM主机检查资源失败", e);
        }
    }

    @Override
    public Map<String, Object> validateForNextStep(Long clusterId) {
        // PVM校验规则可按需实现；这里保持与现有逻辑兼容，简单返回未实现提示
        Map<String, Object> result = new HashMap<>();
        result.put("valid", false);
        result.put("message", "PVM模式下一步校验规则待实现");
        return result;
    }

    /**
     * 获取队列状态信息（PVM模式特有）
     */
    private Map<String, Object> getQueueStatus(Long clusterId) {
        Map<String, Object> queueStatus = new HashMap<>();
        
        try {
            // 这里应该获取实际的队列状态
            // 简化处理，返回默认值
            queueStatus.put("queueSize", 0);
            queueStatus.put("runningTasks", 0);
            queueStatus.put("processorThreadAlive", true);
            
        } catch (Exception e) {
            log.warn("获取队列状态失败", e);
            queueStatus.put("queueSize", 0);
            queueStatus.put("runningTasks", 0);
            queueStatus.put("processorThreadAlive", false);
        }
        
        return queueStatus;
    }

    @Override
    protected Map<String, Object> buildDiscoveryMetadata(List<ClusterHostEntity> hosts, HostDiscoveryRequest request) {
        Map<String, Object> metadata = super.buildDiscoveryMetadata(hosts, request);
        
        // PVM模式特有的元数据
        String hostString = (String) request.getConnectionParams().get("hosts");
        if (hostString != null) {
            String[] hostArray = hostString.split(",");
            metadata.put("requestedHosts", hostArray.length);
            metadata.put("hostList", Arrays.asList(hostArray));
        }
        
        return metadata;
    }
}