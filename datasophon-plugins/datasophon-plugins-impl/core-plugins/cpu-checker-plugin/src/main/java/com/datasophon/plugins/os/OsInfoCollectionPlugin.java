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

package com.datasophon.plugins.os;

import com.datasophon.common.enums.OsType;
import com.datasophon.plugins.api.HostCheckerPlugin;
import com.datasophon.plugins.api.model.CheckResult;
import com.datasophon.plugins.api.model.HostCheckContext;
import com.datasophon.plugins.api.model.PluginMetadata;
import com.datasophon.plugins.api.service.SshConnectionService;
import lombok.extern.slf4j.Slf4j;
import org.pf4j.Extension;

import java.util.List;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 操作系统信息收集插件
 * 负责收集主机的操作系统基本信息
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-28
 */
@Extension
@Slf4j
public class OsInfoCollectionPlugin implements HostCheckerPlugin {
    
    private static final String PLUGIN_ID = "os-info-collection";
    private static final String PLUGIN_VERSION = "1.0.0";
    
    // 系统信息收集命令
    private static final String OS_RELEASE_COMMAND = "cat /etc/os-release";
    private static final String HOSTNAME_COMMAND = "hostname";
    private static final String UNAME_COMMAND = "uname -a";
    private static final String KERNEL_VERSION_COMMAND = "uname -r";
    private static final String ARCHITECTURE_COMMAND = "uname -m";
    private static final String UPTIME_COMMAND = "uptime";
    
    private SshConnectionService sshService;
    
    @Override
    public void initialize() {
        log.info("初始化操作系统信息收集插件...");
        // TODO: 注入SSH连接服务
        log.info("操作系统信息收集插件初始化完成");
    }
    
    @Override
    public Set<OsType> getSupportedOperatingSystems() {
        return Set.of(
            OsType.CENTOS,
            OsType.RHEL,
            OsType.UBUNTU,
            OsType.DEBIAN,
            OsType.KYLIN_V10,
            OsType.KYLIN_V4
        );
    }
    
    @Override
    public int getPriority() {
        return 2; // SSH检查之后执行
    }
    
    @Override
    public CompletableFuture<CheckResult> executeCheck(HostCheckContext context) {
        return CompletableFuture.supplyAsync(() -> {
            LocalDateTime startTime = LocalDateTime.now();
            
            try {
                log.debug("开始操作系统信息收集: 主机={}", context.getHostIp());
                
                // 收集操作系统信息
                Map<String, Object> osInfo = collectOsInformation(context);
                
                Duration duration = Duration.between(startTime, LocalDateTime.now());
                log.debug("操作系统信息收集完成: 主机={}, 耗时={}ms", 
                        context.getHostIp(), duration.toMillis());
                
                return CheckResult.builder()
                        .success(true)
                        .checkType("os-info-collection")
                        .message("操作系统信息收集成功")
                        .checkTime(LocalDateTime.now())
                        .duration(duration.toMillis())
                        .data(osInfo)
                        .build();
                
            } catch (Exception e) {
                Duration duration = Duration.between(startTime, LocalDateTime.now());
                log.error("操作系统信息收集失败: 主机={}, 耗时={}ms, 错误={}", 
                        context.getHostIp(), duration.toMillis(), e.getMessage(), e);
                
                return CheckResult.builder()
                        .success(false)
                        .checkType("os-info-collection")
                        .message("操作系统信息收集失败: " + e.getMessage())
                        .error(e.getMessage())
                        .checkTime(LocalDateTime.now())
                        .duration(duration.toMillis())
                        .build();
            }
        });
    }
    
    @Override
    public boolean canExecute(HostCheckContext context) {
        // 需要SSH连接信息
        return context != null 
                && context.getHostIp() != null 
                && context.getSshUser() != null
                && (context.getSshPassword() != null || context.getPrivateKey() != null);
    }
    
    @Override
    public PluginMetadata getMetadata() {
        return PluginMetadata.builder()
                .pluginId(PLUGIN_ID)
                .name("操作系统信息收集插件")
                .version(PLUGIN_VERSION)
                .description("收集主机操作系统的基本信息，包括版本、内核、架构等")
                .author("任相鹏")
                .homepage("https://github.com/datasophon/datasophon")
                .license("Apache-2.0")
                .supportedOs(Set.of("linux", "centos", "ubuntu", "rocky", "kylin"))
                .tags(Set.of("os", "system-info", "collection"))
                .category("system-info")
                .minJavaVersion("21")
                .corePlugin(true)
                .enabled(true)
                .dependencies(List.of("ssh-connectivity-check"))
                .build();
    }
    
    @Override
    public String getPluginId() {
        return PLUGIN_ID;
    }
    
    @Override
    public String getVersion() {
        return PLUGIN_VERSION;
    }
    
    /**
     * 收集操作系统信息
     */
    private Map<String, Object> collectOsInformation(HostCheckContext context) throws Exception {
        Map<String, Object> osInfo = new HashMap<>();
        
        try {
            // 收集主机名
            String hostname = executeCommand(context, HOSTNAME_COMMAND);
            osInfo.put("hostname", hostname.trim());
            
            // 收集系统基本信息
            String unameOutput = executeCommand(context, UNAME_COMMAND);
            osInfo.put("uname", unameOutput.trim());
            
            // 收集内核版本
            String kernelVersion = executeCommand(context, KERNEL_VERSION_COMMAND);
            osInfo.put("kernelVersion", kernelVersion.trim());
            
            // 收集架构信息
            String architecture = executeCommand(context, ARCHITECTURE_COMMAND);
            osInfo.put("architecture", architecture.trim());
            
            // 收集系统运行时间
            String uptime = executeCommand(context, UPTIME_COMMAND);
            osInfo.put("uptime", uptime.trim());
            
            // 收集OS发行版信息
            try {
                String osRelease = executeCommand(context, OS_RELEASE_COMMAND);
                Map<String, String> osReleaseInfo = parseOsRelease(osRelease);
                osInfo.putAll(osReleaseInfo);
            } catch (Exception e) {
                log.warn("无法读取/etc/os-release文件: 主机={}, 错误={}", 
                        context.getHostIp(), e.getMessage());
                osInfo.put("osType", "Unknown");
                osInfo.put("osVersion", "Unknown");
            }
            
            // 推断操作系统类型
            OsType detectedOsType = detectOsType(osInfo);
            osInfo.put("detectedOsType", detectedOsType.name());
            
            log.debug("操作系统信息收集完成: 主机={}, 系统={}, 版本={}, 架构={}", 
                    context.getHostIp(), 
                    osInfo.get("osType"), 
                    osInfo.get("osVersion"), 
                    osInfo.get("architecture"));
            
        } catch (Exception e) {
            log.error("收集操作系统信息时发生错误: 主机={}, 错误={}", 
                    context.getHostIp(), e.getMessage(), e);
            throw e;
        }
        
        return osInfo;
    }
    
    /**
     * 执行远程命令
     */
    private String executeCommand(HostCheckContext context, String command) throws Exception {
        // TODO: 使用SSH连接服务执行命令
        // 这里需要实现SSH命令执行逻辑
        
        // 临时模拟实现
        log.debug("执行命令: 主机={}, 命令={}", context.getHostIp(), command);
        
        // 根据命令返回模拟结果
        switch (command) {
            case HOSTNAME_COMMAND:
                return "test-host-" + context.getHostIp().replace(".", "-");
            case UNAME_COMMAND:
                return "Linux test-host 5.4.0-74-generic #83-Ubuntu SMP Sat May 8 02:35:39 UTC 2021 x86_64 x86_64 x86_64 GNU/Linux";
            case KERNEL_VERSION_COMMAND:
                return "5.4.0-74-generic";
            case ARCHITECTURE_COMMAND:
                return "x86_64";
            case UPTIME_COMMAND:
                return " 14:30:01 up 5 days,  2:15,  1 user,  load average: 0.08, 0.02, 0.01";
            case OS_RELEASE_COMMAND:
                return "NAME=\"Ubuntu\"\nVERSION=\"20.04.2 LTS (Focal Fossa)\"\nID=ubuntu\nID_LIKE=debian\nPRETTY_NAME=\"Ubuntu 20.04.2 LTS\"\nVERSION_ID=\"20.04\"\nHOME_URL=\"https://www.ubuntu.com/\"\nSUPPORT_URL=\"https://help.ubuntu.com/\"\nBUG_REPORT_URL=\"https://bugs.launchpad.net/ubuntu/\"\nPRIVACY_POLICY_URL=\"https://www.ubuntu.com/legal/terms-and-policies/privacy-policy\"\nVERSION_CODENAME=focal\nUBUNTU_CODENAME=focal";
            default:
                throw new UnsupportedOperationException("不支持的命令: " + command);
        }
    }
    
    /**
     * 解析/etc/os-release文件内容
     */
    private Map<String, String> parseOsRelease(String osReleaseContent) {
        Map<String, String> osInfo = new HashMap<>();
        
        String[] lines = osReleaseContent.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            
            int equalIndex = line.indexOf('=');
            if (equalIndex > 0) {
                String key = line.substring(0, equalIndex).trim();
                String value = line.substring(equalIndex + 1).trim();
                
                // 移除引号
                if (value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                }
                
                // 映射到标准字段
                switch (key) {
                    case "NAME":
                        osInfo.put("osType", value);
                        break;
                    case "VERSION":
                        osInfo.put("osVersion", value);
                        break;
                    case "VERSION_ID":
                        osInfo.put("osVersionId", value);
                        break;
                    case "PRETTY_NAME":
                        osInfo.put("osPrettyName", value);
                        break;
                    case "ID":
                        osInfo.put("osId", value);
                        break;
                    case "ID_LIKE":
                        osInfo.put("osIdLike", value);
                        break;
                    case "VERSION_CODENAME":
                        osInfo.put("osCodename", value);
                        break;
                }
            }
        }
        
        return osInfo;
    }
    
    /**
     * 根据收集的信息推断操作系统类型
     */
    private OsType detectOsType(Map<String, Object> osInfo) {
        String osType = (String) osInfo.get("osType");
        String osId = (String) osInfo.get("osId");
        
        if (osType == null && osId == null) {
            return OsType.OTHER;
        }
        
        String osString = (osType + " " + osId).toLowerCase();
        
        if (osString.contains("centos")) {
            return OsType.CENTOS;
        } else if (osString.contains("ubuntu")) {
            return OsType.UBUNTU;
        } else if (osString.contains("red hat") || osString.contains("rhel")) {
            return OsType.RHEL;
        } else if (osString.contains("debian")) {
            return OsType.DEBIAN;
        } else if (osString.contains("kylin")) {
            // 检测麒麟操作系统版本
            if (osString.contains("v10") || osString.contains("10")) {
                return OsType.KYLIN_V10;
            } else if (osString.contains("v4") || osString.contains("4.0")) {
                return OsType.KYLIN_V4;
            } else {
                return OsType.KYLIN_V10; // 默认返回V10
            }
        } else {
            return OsType.OTHER;
        }
    }
}
