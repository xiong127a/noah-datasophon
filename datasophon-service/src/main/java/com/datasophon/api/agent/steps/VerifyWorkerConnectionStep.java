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
import com.datasophon.api.master.ActorUtils;
import com.datasophon.api.service.host.ClusterHostService;
import com.datasophon.common.command.CollectSystemInfoCommand;
import com.datasophon.common.command.PingCommand;
import com.datasophon.common.command.SystemInfoResult;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.dao.entity.ClusterHostEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pekko.actor.ActorSelection;
import org.apache.pekko.pattern.Patterns;
import org.apache.pekko.util.Timeout;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 验证Worker连接并收集硬件信息步骤
 * 
 * @author DataSophon Team
 * @date 2025-01-31
 */
@Slf4j
@RequiredArgsConstructor
public class VerifyWorkerConnectionStep implements AgentDistributionStep {
    
    private final ClusterHostService clusterHostService;
    
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
        
        logWriter.logInfo(clusterId, hostIp, "verify", "开始验证Worker连接", null);
        log.info("开始验证Worker连接: {}", hostname);
        
        try {
            // 1. 等待Worker服务完全启动（Akka系统初始化需要时间）
            logWriter.logInfo(clusterId, hostIp, "verify", "等待Worker服务完全启动...", null);
            Thread.sleep(5000); // 等待5秒
            
            // 2. 尝试连接Worker并发送Ping命令
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
            
            // 3. 收集硬件信息
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
     * Ping Worker节点
     */
    private boolean pingWorker(String hostname) {
        String actorPath = "pekko://datasophon@" + hostname + ":2552/user/worker/pingActor";
        try {
            log.info("尝试连接Worker节点: {}, 完整路径: {}", hostname, actorPath);
            
            ActorSelection pingActor = ActorUtils.actorSystem.actorSelection(actorPath);
            
            PingCommand pingCommand = new PingCommand();
            pingCommand.setMessage("agent_distribution_verify");
            
            log.info("发送Ping命令到: {}", actorPath);
            Timeout timeout = new Timeout(Duration.create(10, TimeUnit.SECONDS));
            Future<Object> future = Patterns.ask(pingActor, pingCommand, timeout);
            
            ExecResult result = (ExecResult) Await.result(future, timeout.duration());
            log.info("收到Worker节点{}的Ping响应: {}", hostname, result.getExecOut());
            return result.getExecResult();
            
        } catch (Exception e) {
            log.error("Ping Worker节点{}失败 (路径: {}): {}", hostname, actorPath, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 收集Worker系统信息
     */
    private SystemInfoResult collectSystemInfo(String hostname, Long clusterId) {
        try {
            // 向Worker的SystemInfoActor发送收集命令
            ActorSelection systemInfoActor = ActorUtils.actorSystem.actorSelection(
                    "pekko://datasophon@" + hostname + ":2552/user/worker/systemInfoActor");
            
            CollectSystemInfoCommand collectCommand = new CollectSystemInfoCommand();
            collectCommand.setClusterId(clusterId);
            collectCommand.setHostname(hostname);
            
            Timeout timeout = new Timeout(Duration.create(30, TimeUnit.SECONDS));
            Future<Object> future = Patterns.ask(systemInfoActor, collectCommand, timeout);
            
            SystemInfoResult result = (SystemInfoResult) Await.result(future, timeout.duration());
            return result;
            
        } catch (Exception e) {
            log.error("收集Worker{}系统信息失败: {}", hostname, e.getMessage(), e);
            return null;
        }
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
            // 从 "free -h" 输出中提取总内存，格式: "Mem:  15Gi  ..."
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
            // 从 "df -h" 输出中提取根分区总磁盘，格式: "/dev/sda1  100G  ..."
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

