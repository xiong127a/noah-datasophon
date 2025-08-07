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
import com.datasophon.api.converter.K8sToClusterHostConverter;
import com.datasophon.api.dto.Result;
import com.datasophon.api.service.host.UnifiedHostManagementService;
import com.datasophon.api.service.host.strategy.model.HostDiscoveryResult;
import com.datasophon.common.dto.HostDiscoveryResultDTO;
import com.datasophon.common.dto.HostInfoDTO;
import com.datasophon.common.dto.Step1ConfigurationDto;
import com.datasophon.common.dto.FilterOptionsDTO;
import com.datasophon.common.enums.ClusterType;
import com.datasophon.dao.entity.ClusterHostDO;
import com.datasophon.common.enums.MANAGED;
import com.datasophon.common.enums.HostState;
import com.datasophon.api.service.host.ClusterHostService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 统一主机管理控制器
 * 专注于集群配置阶段的主机发现功能
 * 自动根据集群类型选择合适的策略（PVM/Kubernetes）
 * 
 * @author DataSophon Team
 */
@Slf4j
@RestController
@ApiVersion(path = "host")
public class UnifiedHostController {

    @Autowired
    private UnifiedHostManagementService hostManagementService;

    @Autowired
    private K8sToClusterHostConverter converter;
    
    @Autowired
    private ClusterHostService clusterHostService;

    /**
     * Step1配置完成后的主机发现接口
     * 根据Step1的配置信息发现主机列表
     * <p>
     * 支持两种模式：
     * 1. PVM模式：解析IP范围，返回主机列表
     * 2. K8S模式：调用K8S API获取节点列表
     */
    @PostMapping("discover-from-step1")
    public Result<HostDiscoveryResultDTO> discoverHostsFromStep1Configuration(
            @RequestBody Step1ConfigurationDto step1Config,
            @ClusterId Integer clusterId) {
        
        log.info("开始从Step1配置发现主机，集群ID: {}, 集群类型: {}", clusterId, step1Config.getClusterType());
        
        try {
            // 设置集群ID到DTO中
            step1Config.setClusterId(clusterId);
            
            // 验证输入参数
            validateStep1Configuration(step1Config);
            
            // 根据集群类型构造连接参数
            Map<String, Object> connectionParams = buildConnectionParams(step1Config);
            
            // 调用主机发现服务
            HostDiscoveryResult result = hostManagementService.discoverHosts(
                clusterId, connectionParams, step1Config.getForceRefresh());
            
            if (result.getSuccess()) {
                log.info("主机发现成功，集群ID: {}, 发现主机数: {}", clusterId, result.getTotalCount());
                
                // 转换为前端需要的DTO格式
                HostDiscoveryResultDTO responseDto = convertToHostDiscoveryResultDTO(result, clusterId, step1Config.getClusterType());
                
                return Result.success(responseDto);
            } else {
                log.error("主机发现失败，集群ID: {}, 错误: {}", clusterId, result.getErrorMessage());
                return Result.error(result.getErrorMessage());
            }
            
        } catch (IllegalArgumentException e) {
            log.error("Step1配置参数错误，集群ID: {}", clusterId, e);
            return Result.error("配置参数错误: " + e.getMessage());
        } catch (Exception e) {
            log.error("主机发现异常，集群ID: {}", clusterId, e);
            return Result.error("主机发现失败: " + e.getMessage());
        }
    }

    /**
     * 验证Step1配置参数
     */
    private void validateStep1Configuration(Step1ConfigurationDto config) {
        if (config.getClusterType() == null) {
            throw new IllegalArgumentException("集群类型不能为空");
        }
        
        ClusterType clusterType = config.getClusterType();
        
        if (clusterType.isPvm()) {
            // 验证PVM配置
            if (config.getHosts() == null || config.getHosts().trim().isEmpty()) {
                throw new IllegalArgumentException("PVM集群必须提供主机IP列表");
            }
            if (config.getSshUser() == null || config.getSshUser().trim().isEmpty()) {
                throw new IllegalArgumentException("PVM集群必须提供SSH用户名");
            }
            if (config.getSshPort() == null || config.getSshPort().trim().isEmpty()) {
                throw new IllegalArgumentException("PVM集群必须提供SSH端口");
            }
            if (config.getSshPassword() == null || config.getSshPassword().trim().isEmpty()) {
                throw new IllegalArgumentException("PVM集群必须提供SSH密码");
            }
        } else if (clusterType.isKubernetes()) {
            // 验证K8S配置
            if (config.getKubeConfigContent() == null || config.getKubeConfigContent().trim().isEmpty()) {
                throw new IllegalArgumentException("Kubernetes集群必须提供kubeconfig文件内容");
            }
            if (config.getNamespace() == null || config.getNamespace().trim().isEmpty()) {
                throw new IllegalArgumentException("Kubernetes集群必须提供命名空间");
            }
        } else {
            throw new IllegalArgumentException("不支持的集群类型: " + clusterType);
        }
    }

    /**
     * 根据Step1配置构建连接参数
     */
    private Map<String, Object> buildConnectionParams(Step1ConfigurationDto config) {
        Map<String, Object> params = new HashMap<>();
        
        ClusterType clusterType = config.getClusterType();
        
        if (clusterType.isPvm()) {
            // PVM集群参数
            params.put("hosts", config.getHosts());
            params.put("sshUser", config.getSshUser());
            params.put("sshPort", config.getSshPort());
            params.put("sshPassword", config.getSshPassword());
            
            log.debug("构建PVM连接参数: 主机数={}, SSH用户={}, SSH端口={}", 
                parseHostCount(config.getHosts()), config.getSshUser(), config.getSshPort());
                
        } else if (clusterType.isKubernetes()) {
            // K8S集群参数
            params.put("kubeConfigContent", config.getKubeConfigContent());
            params.put("namespace", config.getNamespace());
            
            // 可选参数
            if (config.getIsCreatingNewNamespace() != null) {
                params.put("isCreatingNewNamespace", config.getIsCreatingNewNamespace());
            }
            if (config.getCustomNamespace() != null) {
                params.put("customNamespace", config.getCustomNamespace());
            }
            if (config.getClusterVersion() != null) {
                params.put("clusterVersion", config.getClusterVersion());
            }
            
            log.debug("构建K8S连接参数: 命名空间={}, 是否新建命名空间={}", 
                config.getNamespace(), config.getIsCreatingNewNamespace());
        }
        
        return params;
    }

    /**
     * 解析主机数量（用于日志）
     * 简化版本，实际解析在策略层进行
     */
    private int parseHostCount(String hosts) {
        if (hosts == null || hosts.trim().isEmpty()) {
            return 0;
        }
        
        // 简单统计行数作为估算值
        String[] lines = hosts.split("\n");
        int count = 0;
        for (String line : lines) {
            line = line.trim();
            if (!line.isEmpty()) {
                if (line.contains("[") && line.contains("]")) {
                    // 范围格式：10.3.144.[19-23]
                    try {
                        String range = line.substring(line.indexOf('[') + 1, line.indexOf(']'));
                        String[] parts = range.split("-");
                        if (parts.length == 2) {
                            int start = Integer.parseInt(parts[0]);
                            int end = Integer.parseInt(parts[1]);
                            count += (end - start + 1);
                        } else {
                            count += 1;
                        }
        } catch (Exception e) {
                        count += 1; // 解析失败时按1个计算
                    }
                } else if (line.contains(",")) {
                    // 逗号分隔格式
                    count += line.split(",").length;
                } else {
                    // 单个主机
                    count += 1;
                }
            }
        }
        return count;
    }

    /**
     * 将内部结果转换为前端需要的DTO格式 - 使用Java 21 Pattern Matching
     */
    private HostDiscoveryResultDTO convertToHostDiscoveryResultDTO(
            HostDiscoveryResult result, Integer clusterId, ClusterType clusterType) {
        
        // 转换ClusterHostDO到HostInfoDTO
        List<HostInfoDTO> hostDtos = result.getHosts().stream()
            .map(host -> converter.convertClusterHostToDTO(host))
            .toList(); // Java 16+ 新特性，替代collect(Collectors.toList())
        
        // 构造元数据 - 使用Map.of简化
        var metadata = Map.<String, Object>of(
            "discoveredCount", result.getTotalCount(),
            "strategyType", clusterType.getCode()
        );
        
        // 计算筛选选项 - 后端统一提供
        var filterOptions = calculateFilterOptions(hostDtos);
        
        return result.getSuccess() 
            ? HostDiscoveryResultDTO.success(hostDtos, result.getTotalCount(), metadata, result.getDiscoveryTime(), filterOptions)
            : HostDiscoveryResultDTO.error(result.getErrorMessage());
    }
    
    /**
     * 计算筛选选项 - 从主机数据中提取所有可能的状态和角色
     */
    private FilterOptionsDTO calculateFilterOptions(List<HostInfoDTO> hosts) {
        var statuses = hosts.stream()
            .map(host -> host.getStatus() != null ? host.getStatus() : "Ready")
            .distinct()
            .sorted()
            .toList();
            
        var roles = hosts.stream()
            .map(host -> host.getRoles() != null ? host.getRoles() : "<none>")
            .flatMap(roleStr -> {
                // 处理多角色情况，如 "control-plane,worker"
                if (roleStr.contains(",")) {
                    return java.util.Arrays.stream(roleStr.split(","))
                        .map(String::trim)
                        .filter(role -> !role.isEmpty());
                } else {
                    return java.util.stream.Stream.of(roleStr);
                }
            })
            .distinct()
            .sorted()
            .toList();
            
        return new FilterOptionsDTO(statuses, roles);
    }

    /**
     * 校验集群所有主机状态
     * 检查所有主机是否都满足进入下一步的条件：
     * 1. Kubernetes集群：所有主机都是未受管且状态为Ready
     * 2. PVM集群：根据具体业务逻辑校验
     */
    @GetMapping("validate-hosts-for-next-step")
    public Result<Map<String, Object>> validateHostsForNextStep(@ClusterId Integer clusterId) {
        log.info("开始校验集群主机状态，集群ID: {}", clusterId);
        
        try {
            // 获取集群所有主机
            List<ClusterHostDO> allHosts = clusterHostService.getHostListByClusterId(clusterId);
            
            if (allHosts == null || allHosts.isEmpty()) {
                log.warn("集群没有发现任何主机，集群ID: {}", clusterId);
                return Result.success(Map.of(
                    "valid", false,
                    "message", "集群中没有发现任何主机，请先完成主机发现",
                    "totalHosts", 0,
                    "unmanagedHosts", 0,
                    "readyHosts", 0
                ));
            }

            // 统计主机状态
            long totalHosts = allHosts.size();
            long unmanagedHosts = allHosts.stream()
                .filter(host -> MANAGED.NO.equals(host.getManaged()))
                .count();
            long readyHosts = allHosts.stream()
                .filter(host -> HostState.RUNNING.equals(host.getHostState()))
                .count();
            
            log.info("主机状态统计 - 总数: {}, 未受管: {}, Ready状态: {}", totalHosts, unmanagedHosts, readyHosts);

            // 校验条件：所有主机都必须是未受管且状态为Ready
            boolean allUnmanaged = unmanagedHosts == totalHosts;
            boolean allReady = readyHosts == totalHosts;
            boolean valid = allUnmanaged && allReady;

            String message;
            if (valid) {
                message = String.format("校验通过：所有 %d 台主机都是未受管状态且Ready", totalHosts);
            } else if (!allUnmanaged) {
                long managedCount = totalHosts - unmanagedHosts;
                message = String.format("校验失败：存在 %d 台已受管主机，请确保所有主机都是未受管状态", managedCount);
            } else {
                long notReadyCount = totalHosts - readyHosts;
                message = String.format("校验失败：存在 %d 台非Ready状态主机，请确保所有主机状态都是Ready", notReadyCount);
            }

            Map<String, Object> result = Map.of(
                "valid", valid,
                "message", message,
                "totalHosts", totalHosts,
                "unmanagedHosts", unmanagedHosts,
                "readyHosts", readyHosts,
                "managedHosts", totalHosts - unmanagedHosts,
                "notReadyHosts", totalHosts - readyHosts
            );

            log.info("主机校验完成，集群ID: {}, 结果: {}", clusterId, valid ? "通过" : "失败");
            return Result.success(result);

        } catch (Exception e) {
            log.error("校验集群主机状态失败，集群ID: {}", clusterId, e);
            return Result.error("校验主机状态失败: " + e.getMessage());
        }
    }
}