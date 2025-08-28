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

package com.datasophon.worker.actor;

import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.utils.ShellUtils;
import org.apache.pekko.actor.AbstractActor;
import org.apache.pekko.japi.pf.ReceiveBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;

/**
 * 系统信息收集Actor
 * 
 * 职责：
 * 1. 响应Master的系统信息收集请求
 * 2. 收集本机的系统信息（IP、CPU架构、内存、磁盘等）
 * 3. 返回格式化的系统信息给Master
 */
public class SystemInfoActor extends AbstractActor {

    private static final Logger logger = LoggerFactory.getLogger(SystemInfoActor.class);

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .match(CollectSystemInfoCommand.class, this::handleCollectSystemInfo)
                .matchAny(this::unhandled)
                .build();
    }

    /**
     * 处理系统信息收集请求
     */
    private void handleCollectSystemInfo(CollectSystemInfoCommand command) {
        logger.info("收到Master的系统信息收集请求，集群ID: {}", command.getClusterId());
        
        try {
            SystemInfoResult systemInfo = collectSystemInfo();
            systemInfo.setClusterId(command.getClusterId());
            systemInfo.setHostname(command.getHostname());
            
            logger.info("系统信息收集完成: {}", systemInfo);
            
            // 返回系统信息给Master
            getSender().tell(systemInfo, getSelf());
            
        } catch (Exception e) {
            logger.error("收集系统信息时发生异常", e);
            
            ExecResult errorResult = new ExecResult();
            errorResult.setExecResult(false);
            errorResult.setExecOut("收集系统信息失败: " + e.getMessage());
            
            getSender().tell(errorResult, getSelf());
        }
    }

    /**
     * 收集系统信息
     */
    private SystemInfoResult collectSystemInfo() throws Exception {
        SystemInfoResult result = new SystemInfoResult();
        
        // 获取主机名
        String hostname = InetAddress.getLocalHost().getHostName();
        result.setHostname(hostname);
        
        // 获取IP地址
        String ipAddress = InetAddress.getLocalHost().getHostAddress();
        result.setIpAddress(ipAddress);
        
        // 获取CPU架构
        String cpuArchitecture = ShellUtils.getCpuArchitecture();
        result.setCpuArchitecture(cpuArchitecture);
        
        // 获取操作系统信息
        String osInfo = System.getProperty("os.name") + " " + System.getProperty("os.version");
        result.setOsVersion(osInfo);
        
        // 获取内存信息
        String memoryInfo = getMemoryInfo();
        result.setMemoryInfo(memoryInfo);
        
        // 获取磁盘信息
        String diskInfo = getDiskInfo();
        result.setDiskInfo(diskInfo);
        
        // 获取CPU核心数
        int cpuCores = Runtime.getRuntime().availableProcessors();
        result.setCpuCores(cpuCores);
        
        // 获取系统负载（Linux系统）
        String systemLoad = getSystemLoad();
        result.setSystemLoad(systemLoad);
        
        return result;
    }

    /**
     * 获取内存信息
     */
    private String getMemoryInfo() {
        try {
            ExecResult result = ShellUtils.exceShell("free -h");
            if (result.getExecResult()) {
                return result.getExecOut();
            }
        } catch (Exception e) {
            logger.warn("获取内存信息失败", e);
        }
        
        // 使用Java API作为备选
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory() / 1024 / 1024; // MB
        long totalMemory = runtime.totalMemory() / 1024 / 1024; // MB
        long freeMemory = runtime.freeMemory() / 1024 / 1024; // MB
        
        return String.format("Max: %dMB, Total: %dMB, Free: %dMB", maxMemory, totalMemory, freeMemory);
    }

    /**
     * 获取磁盘信息
     */
    private String getDiskInfo() {
        try {
            ExecResult result = ShellUtils.exceShell("df -h");
            if (result.getExecResult()) {
                return result.getExecOut();
            }
        } catch (Exception e) {
            logger.warn("获取磁盘信息失败", e);
        }
        
        return "磁盘信息获取失败";
    }

    /**
     * 获取系统负载
     */
    private String getSystemLoad() {
        try {
            ExecResult result = ShellUtils.exceShell("uptime");
            if (result.getExecResult()) {
                return result.getExecOut();
            }
        } catch (Exception e) {
            logger.warn("获取系统负载失败", e);
        }
        
        return "系统负载获取失败";
    }

    // 内部类定义
    public static class CollectSystemInfoCommand {
        private Long clusterId;
        private String hostname;
        
        public Long getClusterId() { return clusterId; }
        public void setClusterId(Long clusterId) { this.clusterId = clusterId; }
        public String getHostname() { return hostname; }
        public void setHostname(String hostname) { this.hostname = hostname; }
    }

    public static class SystemInfoResult extends ExecResult {
        private Long clusterId;
        private String hostname;
        private String ipAddress;
        private String cpuArchitecture;
        private String osVersion;
        private String memoryInfo;
        private String diskInfo;
        private int cpuCores;
        private String systemLoad;
        
        public SystemInfoResult() {
            super();
            setExecResult(true); // 默认设置为成功
        }
        
        // Getters and setters
        public Long getClusterId() { return clusterId; }
        public void setClusterId(Long clusterId) { this.clusterId = clusterId; }
        public String getHostname() { return hostname; }
        public void setHostname(String hostname) { this.hostname = hostname; }
        public String getIpAddress() { return ipAddress; }
        public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
        public String getCpuArchitecture() { return cpuArchitecture; }
        public void setCpuArchitecture(String cpuArchitecture) { this.cpuArchitecture = cpuArchitecture; }
        public String getOsVersion() { return osVersion; }
        public void setOsVersion(String osVersion) { this.osVersion = osVersion; }
        public String getMemoryInfo() { return memoryInfo; }
        public void setMemoryInfo(String memoryInfo) { this.memoryInfo = memoryInfo; }
        public String getDiskInfo() { return diskInfo; }
        public void setDiskInfo(String diskInfo) { this.diskInfo = diskInfo; }
        public int getCpuCores() { return cpuCores; }
        public void setCpuCores(int cpuCores) { this.cpuCores = cpuCores; }
        public String getSystemLoad() { return systemLoad; }
        public void setSystemLoad(String systemLoad) { this.systemLoad = systemLoad; }
        
        @Override
        public String toString() {
            return String.format("SystemInfo{hostname='%s', ip='%s', arch='%s', os='%s', cpuCores=%d}", 
                    hostname, ipAddress, cpuArchitecture, osVersion, cpuCores);
        }
    }
}
