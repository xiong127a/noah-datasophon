/*
 *
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
 *
 */

package com.datasophon.worker.service;

import com.datasophon.common.command.CollectSystemInfoCommand;
import com.datasophon.common.command.SystemInfoResult;
import com.datasophon.common.utils.HardwareInfoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.InetAddress;

/**
 * 系统信息服务
 * 替代原来的SystemInfoActor，用于收集Worker系统信息
 * 使用 OSHI 库获取跨平台的硬件信息，替代原有的 Shell 脚本调用
 */
@Service
public class SystemInfoService {

    private static final Logger logger = LoggerFactory.getLogger(SystemInfoService.class);

    /**
     * 收集系统信息
     * 使用 OSHI 库获取准确的系统硬件信息
     */
    public SystemInfoResult collectSystemInfo(CollectSystemInfoCommand command) {
        logger.info("Collecting system information for cluster: {}", command.getClusterId());

        SystemInfoResult result = new SystemInfoResult();
        
        try {
            // 设置集群ID
            result.setClusterId(command.getClusterId());
            
            // 获取主机名和IP地址
            String hostname = InetAddress.getLocalHost().getHostName();
            String ipAddress = InetAddress.getLocalHost().getHostAddress();
            result.setHostname(hostname);
            result.setIpAddress(ipAddress);
            logger.debug("Host: {} ({})", hostname, ipAddress);
            
            // 获取CPU架构（使用OSHI）
            String cpuArchitecture = HardwareInfoUtils.getCpuArchitecture();
            result.setCpuArchitecture(cpuArchitecture);
            logger.debug("CPU Architecture: {}", cpuArchitecture);
            
            // 获取CPU核心数（使用OSHI）
            int cpuCores = HardwareInfoUtils.getCpuCores();
            result.setCpuCores(cpuCores);
            logger.debug("CPU Logical Cores: {}", cpuCores);
            
            // 获取操作系统信息（使用OSHI）
            String osInfo = HardwareInfoUtils.getOsInfo();
            result.setOsVersion(osInfo);
            logger.debug("OS: {}", osInfo);
            
            // 获取系统物理内存信息（使用OSHI，而非JVM内存）
            String memoryInfo = HardwareInfoUtils.getMemoryInfoString();
            result.setMemoryInfo(memoryInfo);
            logger.debug("Memory: {}", memoryInfo);
            
            // 获取磁盘信息（使用OSHI，包含所有文件系统）
            String diskInfo = HardwareInfoUtils.getDiskInfoString();
            result.setDiskInfo(diskInfo);
            logger.debug("Disk: {}", diskInfo);
            
            // 获取系统负载（使用OSHI）
            String systemLoad = HardwareInfoUtils.getSystemLoad();
            result.setSystemLoad(systemLoad);
            logger.debug("System Load (1min): {}", systemLoad);
            
            result.setExecResult(true);
            logger.info("System information collected successfully - Host: {}, IP: {}, OS: {}, CPU: {} cores, Arch: {}", 
                hostname, ipAddress, osInfo, cpuCores, cpuArchitecture);
            
        } catch (Exception e) {
            logger.error("Failed to collect system information", e);
            result.setExecResult(false);
            result.setExecOut("Failed to collect system info: " + e.getMessage());
        }

        return result;
    }
}

