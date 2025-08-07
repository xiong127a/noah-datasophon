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

package com.datasophon.api.controller.v1.kubernetes;

import com.datasophon.api.annotation.ApiVersion;
import com.datasophon.api.annotation.ClusterId;
import com.datasophon.api.dto.Result;
import com.datasophon.api.service.kubernetes.KubernetesHostService;
import com.datasophon.dao.entity.ClusterHostDO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Kubernetes 主机管理控制器
 * 专门处理K8S模式下的主机发现、列表获取和导入功能
 */
@Slf4j
@ApiVersion(path = "k8s/hosts")
public class KubernetesHostController {

    @Autowired
    private KubernetesHostService kubernetesHostService;

    /**
     * 发现K8S集群中的主机
     * 直接从K8S API获取节点信息，包含完整的硬件详情
     */
    @PostMapping("/discover")
    public Result<List<ClusterHostDO>> discoverHosts(
            @RequestBody Map<String, Object> request,
            @ClusterId Integer clusterId) {
        
        String kubeConfigContent = (String) request.get("kubeConfigContent");
        String namespace = (String) request.get("namespace");
        
        log.info("开始发现K8S集群主机，clusterId: {}, namespace: {}", clusterId, namespace);
        
        try {
            List<ClusterHostDO> discoveredHosts = kubernetesHostService.discoverHosts(
                kubeConfigContent, namespace, clusterId);
            
            log.info("成功发现{}台K8S主机", discoveredHosts.size());
            return Result.success(discoveredHosts);
            
        } catch (Exception e) {
            log.error("发现K8S主机失败", e);
            return Result.error("发现K8S主机失败: " + e.getMessage());
        }
    }

    /**
     * 获取已发现的K8S主机列表
     * 用于Step2界面显示和主机校验
     */
    @GetMapping("/list")
    public Result<List<ClusterHostDO>> listHosts(@ClusterId Integer clusterId) {
        log.info("获取K8S主机列表，clusterId: {}", clusterId);
        
        try {
            List<ClusterHostDO> hosts = kubernetesHostService.getDiscoveredHosts(clusterId);
            log.info("获取到{}台K8S主机", hosts.size());
            return Result.success(hosts);
            
        } catch (Exception e) {
            log.error("获取K8S主机列表失败", e);
            return Result.error("获取K8S主机列表失败: " + e.getMessage());
        }
    }

    /**
     * 导入选定的K8S主机到集群
     * 用户在Step2选择主机后调用此接口
     */
    @PostMapping("/import")
    public Result<Void> importHosts(
            @RequestBody List<ClusterHostDO> selectedHosts,
            @ClusterId Integer clusterId) {
        
        log.info("导入{}台K8S主机到集群{}", selectedHosts.size(), clusterId);
        
        try {
            kubernetesHostService.importHosts(selectedHosts, clusterId);
            log.info("成功导入{}台K8S主机", selectedHosts.size());
            return Result.success();
            
        } catch (Exception e) {
            log.error("导入K8S主机失败", e);
            return Result.error("导入K8S主机失败: " + e.getMessage());
        }
    }

    /**
     * 刷新K8S主机硬件信息
     * 重新获取最新的硬件状态
     */
    @PostMapping("/refresh")
    public Result<List<ClusterHostDO>> refreshHosts(
            @RequestBody Map<String, Object> request,
            @ClusterId Integer clusterId) {
        
        String kubeConfigContent = (String) request.get("kubeConfigContent");
        String namespace = (String) request.get("namespace");
        
        log.info("刷新K8S主机信息，clusterId: {}", clusterId);
        
        try {
            List<ClusterHostDO> refreshedHosts = kubernetesHostService.refreshHosts(
                kubeConfigContent, namespace, clusterId);
            
            log.info("成功刷新{}台K8S主机信息", refreshedHosts.size());
            return Result.success(refreshedHosts);
            
        } catch (Exception e) {
            log.error("刷新K8S主机信息失败", e);
            return Result.error("刷新K8S主机信息失败: " + e.getMessage());
        }
    }

    /**
     * 检查K8S连接状态
     */
    @PostMapping("/check-connection")
    public Result<Map<String, Object>> checkConnection(
            @RequestBody Map<String, Object> request) {
        
        String kubeConfigContent = (String) request.get("kubeConfigContent");
        
        try {
            Map<String, Object> connectionInfo = kubernetesHostService.checkConnection(kubeConfigContent);
            return Result.success(connectionInfo);
            
        } catch (Exception e) {
            log.error("检查K8S连接失败", e);
            return Result.error("检查K8S连接失败: " + e.getMessage());
        }
    }
}