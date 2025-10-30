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
import com.datasophon.common.utils.ShellUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.InetAddress;

/**
 * 系统信息服务
 * 替代原来的SystemInfoActor，用于收集Worker系统信息
 */
@Service
public class SystemInfoService {

    private static final Logger logger = LoggerFactory.getLogger(SystemInfoService.class);

    /**
     * 收集系统信息
     */
    public SystemInfoResult collectSystemInfo(CollectSystemInfoCommand command) {
        logger.info("Collecting system information for cluster: {}", command.getClusterId());

        SystemInfoResult result = new SystemInfoResult();
        
        try {
            // 设置集群ID
            result.setClusterId(command.getClusterId());
            
            // 获取主机名
            String hostname = InetAddress.getLocalHost().getHostName();
            result.setHostname(hostname);
            
            // 获取IP地址
            String ipAddress = InetAddress.getLocalHost().getHostAddress();
            result.setIpAddress(ipAddress);
            
            // 获取CPU架构
            String cpuArchitecture = ShellUtils.getCpuArchitecture();
            result.setCpuArchitecture(cpuArchitecture);
            
            // 获取CPU核心数
            int cpuCores = Runtime.getRuntime().availableProcessors();
            result.setCpuCores(cpuCores);
            
            // 获取操作系统信息
            String osVersion = System.getProperty("os.name") + " " + System.getProperty("os.version");
            result.setOsVersion(osVersion);
            
            // 获取系统负载（简化版本）
            result.setSystemLoad(String.valueOf(cpuCores));
            
            result.setExecResult(true);
            logger.info("System information collected successfully for host: {}", hostname);
            
        } catch (Exception e) {
            logger.error("Failed to collect system information", e);
            result.setExecResult(false);
            result.setExecOut("Failed to collect system info: " + e.getMessage());
        }

        return result;
    }
}

