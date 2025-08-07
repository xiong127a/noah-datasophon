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

package com.datasophon.api.controller.v1.host;

import com.datasophon.api.annotation.ApiVersion;
import com.datasophon.api.annotation.ClusterId;
import com.datasophon.api.dto.Result;
import com.datasophon.api.service.host.UnifiedHostManagementService;
import com.datasophon.api.service.host.strategy.model.HostDiscoveryResult;
import com.datasophon.api.service.host.strategy.model.HostListResult;
import com.datasophon.dao.entity.ClusterHostDO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 统一主机管理控制器
 * 使用策略模式自动选择PVM或K8S模式，提供统一的API接口
 */
@Slf4j
@ApiVersion(path = "host")
public class UnifiedHostController {

    @Autowired
    private UnifiedHostManagementService hostManagementService;

    /**
     * 发现主机
     * 根据集群类型自动选择策略发现主机
     */
    @PostMapping("/discover")
    public Result<HostDiscoveryResult> discoverHosts(
            @RequestBody Map<String, Object> request,
            @ClusterId Integer clusterId) {
        
        log.info("开始发现主机，集群ID: {}", clusterId);
        
        try {
            Boolean forceRefresh = (Boolean) request.get("forceRefresh");
            
            // 移除非连接参数
            Map<String, Object> connectionParams = Map.copyOf(request);
            connectionParams.remove("forceRefresh");
            
            HostDiscoveryResult result = hostManagementService.discoverHosts(
                clusterId, connectionParams, forceRefresh);
            
            if (result.getSuccess()) {
                log.info("主机发现成功，集群ID: {}, 发现主机数: {}", clusterId, result.getTotalCount());
                return Result.success(result);
            } else {
                log.error("主机发现失败，集群ID: {}, 错误: {}", clusterId, result.getErrorMessage());
                return Result.error(result.getErrorMessage());
            }
            
        } catch (Exception e) {
            log.error("主机发现异常，集群ID: {}", clusterId, e);
            return Result.error("主机发现失败: " + e.getMessage());
        }
    }

    /**
     * 获取主机列表
     * 支持分页和筛选，自动适配不同的集群模式
     */
    @GetMapping("/list")
    public Result<HostListResult> getHostList(
            @ClusterId Integer clusterId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) String hostname,
            @RequestParam(required = false) String ip,
            @RequestParam(required = false) String cpuArchitecture,
            @RequestParam(required = false) Integer hostState,
            @RequestParam(required = false) String orderField,
            @RequestParam(required = false) String orderType) {
        
        log.debug("获取主机列表，集群ID: {}, 页码: {}, 页大小: {}", clusterId, page, pageSize);
        
        try {
            HostListResult result = hostManagementService.getHostList(
                clusterId, page, pageSize, hostname, ip, cpuArchitecture, 
                hostState, orderField, orderType);
            
            log.debug("主机列表获取成功，集群ID: {}, 返回主机数: {}", clusterId, result.getHosts().size());
            return Result.success(result);
            
        } catch (Exception e) {
            log.error("获取主机列表异常，集群ID: {}", clusterId, e);
            return Result.error("获取主机列表失败: " + e.getMessage());
        }
    }

    /**
     * 导入主机
     * 将用户选择的主机导入到集群中
     */
    @PostMapping("/import")
    public Result<Void> importHosts(
            @RequestBody Map<String, Object> request,
            @ClusterId Integer clusterId) {
        
        log.info("开始导入主机，集群ID: {}", clusterId);
        
        try {
            @SuppressWarnings("unchecked")
            List<ClusterHostDO> selectedHosts = (List<ClusterHostDO>) request.get("selectedHosts");
            @SuppressWarnings("unchecked")
            Map<String, Object> connectionParams = (Map<String, Object>) request.get("connectionParams");
            @SuppressWarnings("unchecked")
            Map<String, Object> importOptions = (Map<String, Object>) request.get("importOptions");
            
            if (selectedHosts == null || selectedHosts.isEmpty()) {
                return Result.error("选择的主机列表不能为空");
            }
            
            hostManagementService.importHosts(clusterId, selectedHosts, connectionParams, importOptions);
            
            log.info("主机导入成功，集群ID: {}, 导入主机数: {}", clusterId, selectedHosts.size());
            return Result.success();
            
        } catch (Exception e) {
            log.error("导入主机异常，集群ID: {}", clusterId, e);
            return Result.error("导入主机失败: " + e.getMessage());
        }
    }

    /**
     * 刷新主机信息
     * 重新获取主机的最新状态
     */
    @PostMapping("/refresh")
    public Result<List<ClusterHostDO>> refreshHosts(
            @RequestBody Map<String, Object> connectionParams,
            @ClusterId Integer clusterId) {
        
        log.info("开始刷新主机信息，集群ID: {}", clusterId);
        
        try {
            List<ClusterHostDO> refreshedHosts = hostManagementService.refreshHosts(clusterId, connectionParams);
            
            log.info("主机信息刷新成功，集群ID: {}, 刷新主机数: {}", clusterId, refreshedHosts.size());
            return Result.success(refreshedHosts);
            
        } catch (Exception e) {
            log.error("刷新主机信息异常，集群ID: {}", clusterId, e);
            return Result.error("刷新主机信息失败: " + e.getMessage());
        }
    }

    /**
     * 检查连接状态
     * 验证是否能正常连接到目标环境
     */
    @PostMapping("/check-connection")
    public Result<Map<String, Object>> checkConnection(
            @RequestBody Map<String, Object> connectionParams,
            @ClusterId Integer clusterId) {
        
        log.info("开始检查连接状态，集群ID: {}", clusterId);
        
        try {
            Map<String, Object> result = hostManagementService.checkConnection(clusterId, connectionParams);
            
            Boolean connected = (Boolean) result.get("connected");
            log.info("连接状态检查完成，集群ID: {}, 连接状态: {}", clusterId, connected);
            
            return Result.success(result);
            
        } catch (Exception e) {
            log.error("检查连接状态异常，集群ID: {}", clusterId, e);
            return Result.error("检查连接状态失败: " + e.getMessage());
        }
    }

    /**
     * 执行主机环境检查
     * 对主机进行环境校验
     */
    @PostMapping("/check")
    public Result<Map<String, Object>> performHostCheck(
            @RequestBody Map<String, Object> request,
            @ClusterId Integer clusterId) {
        
        log.info("开始执行主机环境检查，集群ID: {}", clusterId);
        
        try {
            @SuppressWarnings("unchecked")
            List<String> hostnames = (List<String>) request.get("hostnames");
            @SuppressWarnings("unchecked")
            Map<String, Object> connectionParams = (Map<String, Object>) request.get("connectionParams");
            
            if (hostnames == null || hostnames.isEmpty()) {
                return Result.error("主机名列表不能为空");
            }
            
            Map<String, Object> result = hostManagementService.performHostCheck(
                clusterId, hostnames, connectionParams);
            
            log.info("主机环境检查完成，集群ID: {}, 检查主机数: {}", clusterId, hostnames.size());
            return Result.success(result);
            
        } catch (Exception e) {
            log.error("执行主机环境检查异常，集群ID: {}", clusterId, e);
            return Result.error("执行主机环境检查失败: " + e.getMessage());
        }
    }

    /**
     * 获取主机检查状态
     * 查询主机环境检查的进度和结果
     */
    @GetMapping("/check-status")
    public Result<Map<String, Object>> getHostCheckStatus(@ClusterId Integer clusterId) {
        
        log.debug("查询主机检查状态，集群ID: {}", clusterId);
        
        try {
            Map<String, Object> result = hostManagementService.getHostCheckStatus(clusterId);
            
            log.debug("主机检查状态查询完成，集群ID: {}", clusterId);
            return Result.success(result);
            
        } catch (Exception e) {
            log.error("查询主机检查状态异常，集群ID: {}", clusterId, e);
            return Result.error("查询主机检查状态失败: " + e.getMessage());
        }
    }

    /**
     * 清理资源
     * 清理指定集群的主机管理相关资源
     */
    @PostMapping("/cleanup")
    public Result<Void> cleanup(@ClusterId Integer clusterId) {
        
        log.info("开始清理主机管理资源，集群ID: {}", clusterId);
        
        try {
            hostManagementService.cleanup(clusterId);
            
            log.info("主机管理资源清理完成，集群ID: {}", clusterId);
            return Result.success();
            
        } catch (Exception e) {
            log.error("清理主机管理资源异常，集群ID: {}", clusterId, e);
            return Result.error("清理主机管理资源失败: " + e.getMessage());
        }
    }

    /**
     * 获取支持的策略类型
     * 用于前端显示可选的部署模式
     */
    @GetMapping("/strategies")
    public Result<Map<String, String>> getSupportedStrategies() {
        
        try {
            Map<String, String> strategies = hostManagementService.getAllStrategies()
                    .entrySet()
                    .stream()
                    .collect(java.util.stream.Collectors.toMap(
                            entry -> entry.getKey().getCode(),
                            entry -> entry.getKey().getDescription()
                    ));
            
            return Result.success(strategies);
            
        } catch (Exception e) {
            log.error("获取支持的策略类型异常", e);
            return Result.error("获取支持的策略类型失败: " + e.getMessage());
        }
    }
}