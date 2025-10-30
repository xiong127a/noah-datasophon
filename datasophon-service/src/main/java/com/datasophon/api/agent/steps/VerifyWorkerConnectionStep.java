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

package com.datasophon.api.agent.steps;

import com.datasophon.api.agent.AgentDistributionContext;
import com.datasophon.api.agent.AgentDistributionStep;
import com.datasophon.api.agent.util.AgentLogWriter;
import com.datasophon.api.client.WorkerHttpClient;
import com.datasophon.api.service.host.ClusterHostService;
import com.datasophon.common.command.SystemInfoResult;
import com.datasophon.dao.entity.ClusterHostEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 验证Worker连接并收集硬件信息步骤（HTTP方式）
 * 
 * @author DataSophon Team
 * @date 2025-01-31
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VerifyWorkerConnectionStep implements AgentDistributionStep {
    
    private final ClusterHostService clusterHostService;
    private final WorkerHttpClient workerHttpClient;
    
    @Override
    public String getStepName() {
        return "验证Worker连接";
    }
    
    @Override
    public void execute(AgentDistributionContext context) throws Exception {
        AgentLogWriter logWriter = context.getLogWriter();
        Long clusterId = context.getClusterId();
        String hostIp = context.getHostIp();
        String hostname = context.getHostname();
        
        logWriter.logInfo(clusterId, hostIp, "verify", "开始验证Worker连接（HTTP方式）", null);
        log.info("开始验证Worker连接（HTTP）: hostname={}, ip={}", hostname, hostIp);
        
        try {
            // 1. 等待Worker服务完全启动（Spring Boot初始化需要时间）
            logWriter.logInfo(clusterId, hostIp, "verify", "等待Worker服务完全启动...", null);
            Thread.sleep(5000); // 等待5秒
            
            // 2. 尝试Ping Worker（HTTP方式）
            logWriter.logInfo(clusterId, hostIp, "verify", "尝试连接Worker节点...", null);
            boolean pingSuccess = pingWorker(hostname);
            
            if (!pingSuccess) {
                // 如果Ping失败，再等待5秒重试一次
                logWriter.logWarning(clusterId, hostIp, "verify", 
                        "首次连接失败，5秒后重试...", null);
                Thread.sleep(5000);
                pingSuccess = pingWorker(hostname);
            }
            
            if (!pingSuccess) {
                throw new Exception("无法连接到Worker节点，请检查Worker服务是否正常启动");
            }
            
            Map<String, Object> pingInfo = new HashMap<>();
            pingInfo.put("status", "connected");
            logWriter.logSuccess(clusterId, hostIp, "verify", 
                    "Worker连接成功", pingInfo);
            
            // 3. 收集硬件信息（HTTP方式）
            logWriter.logInfo(clusterId, hostIp, "verify", "开始收集硬件信息...", null);
            SystemInfoResult systemInfo = collectSystemInfo(hostname, clusterId);
            
            if (systemInfo == null || !systemInfo.getExecResult()) {
                throw new Exception("收集硬件信息失败");
            }
            
            // 4. 构建硬件信息数据
            Map<String, Object> hardwareInfo = new HashMap<>();
            hardwareInfo.put("hostname", systemInfo.getHostname());
            hardwareInfo.put("ipAddress", systemInfo.getIpAddress());
            hardwareInfo.put("cpuArchitecture", systemInfo.getCpuArchitecture());
            hardwareInfo.put("cpuCores", systemInfo.getCpuCores());
            hardwareInfo.put("osVersion", systemInfo.getOsVersion());
            hardwareInfo.put("memoryInfo", systemInfo.getMemoryInfo());
            hardwareInfo.put("diskInfo", systemInfo.getDiskInfo());
            hardwareInfo.put("systemLoad", systemInfo.getSystemLoad());
            
            logWriter.logSuccess(clusterId, hostIp, "verify", 
                    String.format("硬件信息收集成功: CPU=%s核, 架构=%s, 系统=%s", 
                            systemInfo.getCpuCores(), 
                            systemInfo.getCpuArchitecture(),
                            systemInfo.getOsVersion()),
                    hardwareInfo);
            
            // 5. 更新主机硬件信息到数据库
            updateHostHardwareInfo(clusterId, hostname, systemInfo);
            
            logWriter.logSuccess(clusterId, hostIp, "verify", 
                    "Worker验证完成，Agent分发成功", hardwareInfo);
            log.info("Worker验证成功: {}, 硬件信息已保存", hostname);
            
        } catch (Exception e) {
            Map<String, Object> errorInfo = new HashMap<>();
            errorInfo.put("error", e.getMessage());
            logWriter.logError(clusterId, hostIp, "verify",
                    "Worker验证失败: " + e.getMessage(), errorInfo);
            throw new Exception("Worker验证失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * Ping Worker节点（HTTP方式）
     */
    private boolean pingWorker(String hostname) {
        try {
            log.info("尝试连接Worker节点（HTTP）: http://{}:2552/api/ping", hostname);
            
            boolean success = workerHttpClient.ping(hostname);
            
            if (success) {
                log.info("收到Worker节点{}的Ping响应（HTTP）", hostname);
            }
            
            return success;
            
        } catch (Exception e) {
            log.error("Ping Worker节点{}失败（HTTP）: {}", hostname, e.getMessage());
            return false;
        }
    }
    
    /**
     * 收集Worker系统信息（HTTP方式）
     */
    private SystemInfoResult collectSystemInfo(String hostname, Long clusterId) {
        try {
            log.info("通过HTTP收集Worker系统信息: http://{}:2552/api/info", hostname);
            
            SystemInfoResult systemInfo = workerHttpClient.getSystemInfo(hostname);
            
            if (systemInfo != null && systemInfo.getExecResult()) {
                systemInfo.setClusterId(clusterId);
                log.info("成功收集Worker{}的系统信息（HTTP）", hostname);
                return systemInfo;
            }
            
        } catch (Exception e) {
            log.error("收集Worker{}系统信息失败（HTTP）: {}", hostname, e.getMessage());
        }
        
        return null;
    }
    
    /**
     * 更新主机硬件信息到数据库
     */
    private void updateHostHardwareInfo(Long clusterId, String hostname, SystemInfoResult systemInfo) {
        try {
            ClusterHostEntity host = clusterHostService.getClusterHostByHostname(hostname);
            if (host != null) {
                host.setCpuArchitecture(systemInfo.getCpuArchitecture());
                host.setCoreNum(systemInfo.getCpuCores());
                host.setTotalMem(extractTotalMemory(systemInfo.getMemoryInfo()));
                host.setTotalDisk(extractTotalDisk(systemInfo.getDiskInfo()));
                host.setIp(systemInfo.getIpAddress());
                
                clusterHostService.updateById(host);
                log.info("更新主机{}硬件信息成功", hostname);
            }
        } catch (Exception e) {
            log.warn("更新主机{}硬件信息失败: {}", hostname, e.getMessage());
        }
    }
    
    /**
     * 从内存信息中提取总内存（简化版）
     */
    private Integer extractTotalMemory(String memoryInfo) {
        try {
            if (memoryInfo != null && memoryInfo.contains("Mem:")) {
                String[] lines = memoryInfo.split("\n");
                for (String line : lines) {
                    if (line.trim().startsWith("Mem:")) {
                        String[] parts = line.trim().split("\\s+");
                        if (parts.length > 1) {
                            String totalMem = parts[1].replaceAll("[^0-9.]", "");
                            return (int) Double.parseDouble(totalMem);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("解析内存信息失败: {}", e.getMessage());
        }
        return null;
    }
    
    /**
     * 从磁盘信息中提取总磁盘（简化版）
     */
    private Integer extractTotalDisk(String diskInfo) {
        try {
            if (diskInfo != null) {
                String[] lines = diskInfo.split("\n");
                for (String line : lines) {
                    if (line.contains("/") && !line.startsWith("Filesystem")) {
                        String[] parts = line.trim().split("\\s+");
                        if (parts.length > 1) {
                            String totalDisk = parts[1].replaceAll("[^0-9.]", "");
                            return (int) Double.parseDouble(totalDisk);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("解析磁盘信息失败: {}", e.getMessage());
        }
        return null;
    }
}
