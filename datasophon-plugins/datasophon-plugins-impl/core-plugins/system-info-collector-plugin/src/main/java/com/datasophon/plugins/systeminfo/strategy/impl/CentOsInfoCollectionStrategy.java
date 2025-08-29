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

package com.datasophon.plugins.systeminfo.strategy.impl;

import com.datasophon.common.enums.OsType;
import com.datasophon.plugins.api.model.HostCheckContext;
import com.datasophon.plugins.api.model.SystemInfo;
import com.datasophon.plugins.api.service.SshConnectionService;
import com.datasophon.plugins.systeminfo.strategy.OsInfoCollectionStrategy;
import lombok.extern.slf4j.Slf4j;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CentOS系统信息收集策略
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-28
 */
@Slf4j
public class CentOsInfoCollectionStrategy implements OsInfoCollectionStrategy {

    @Override
    public OsType getSupportedOsType() {
        return OsType.CENTOS;
    }

    @Override
    public SystemInfo collectSystemInfo(HostCheckContext context, SshConnectionService sshService) {
        try {
            log.debug("开始收集CentOS系统信息: hostIp={}", context.getHostIp());

            SystemInfo.SystemInfoBuilder builder = SystemInfo.builder();

            // 基本系统信息
            String hostnameCommand = "hostname";
            var hostnameResult = sshService.executeCommand(context, hostnameCommand);
            String hostname = hostnameResult != null && hostnameResult.isSuccess() ? 
                hostnameResult.output().trim() : "unknown";
            builder.hostname(hostname);

            String osVersionCommand = "cat /etc/os-release | grep PRETTY_NAME | cut -d'\"' -f2";
            var osVersionResult = sshService.executeCommand(context, osVersionCommand);
            String osVersion = osVersionResult != null && osVersionResult.isSuccess() ? 
                osVersionResult.output().trim() : "unknown";
            builder.osType(OsType.CENTOS)
                   .osVersion(osVersion);

            String kernelCommand = "uname -r";
            var kernelResult = sshService.executeCommand(context, kernelCommand);
            String kernelVersion = kernelResult != null && kernelResult.isSuccess() ? 
                kernelResult.output().trim() : "unknown";
            builder.kernelVersion(kernelVersion);

            String archCommand = "uname -m";
            var archResult = sshService.executeCommand(context, archCommand);
            String cpuArchitecture = archResult != null && archResult.isSuccess() ? 
                archResult.output().trim() : "unknown";
            builder.cpuArchitecture(cpuArchitecture);

            // 收集CPU信息
            String cpuInfoCommand = "cat /proc/cpuinfo";
            var cpuInfoResult = sshService.executeCommand(context, cpuInfoCommand);
            String coreCountCommand = "nproc";
            var coreCountResult = sshService.executeCommand(context, coreCountCommand);
            String cpuUsageCommand = "top -bn1 | grep 'Cpu(s)' | head -1";
            var cpuUsageResult = sshService.executeCommand(context, cpuUsageCommand);
            
            SystemInfo.CpuInfo cpuInfo = parseCpuInfo(
                cpuInfoResult != null && cpuInfoResult.isSuccess() ? cpuInfoResult.output() : null,
                coreCountResult != null && coreCountResult.isSuccess() ? coreCountResult.output() : null,
                cpuUsageResult != null && cpuUsageResult.isSuccess() ? cpuUsageResult.output() : null
            );
            builder.cpuCoreCount(cpuInfo.coreCount())
                   .cpuModelName(cpuInfo.modelName());

            // 收集内存信息
            String memInfoCommand = "cat /proc/meminfo";
            var memInfoResult = sshService.executeCommand(context, memInfoCommand);
            SystemInfo.MemoryInfo memoryInfo = parseMemoryInfo(
                memInfoResult != null && memInfoResult.isSuccess() ? memInfoResult.output() : null
            );
            builder.totalMemoryMB(memoryInfo.totalMB())
                   .freeMemoryMB(memoryInfo.availableMB())
                   .totalSwapMB(memoryInfo.swapTotalMB())
                   .freeSwapMB(memoryInfo.swapFreeMB());

            // 收集Java信息
            String javaVersionCommand = "java -version 2>&1 | head -1";
            var javaVersionResult = sshService.executeCommand(context, javaVersionCommand);
            String javaHomeCommand = "echo $JAVA_HOME";
            var javaHomeResult = sshService.executeCommand(context, javaHomeCommand);
            SystemInfo.JavaInfo javaInfo = parseJavaInfo(
                javaVersionResult != null && javaVersionResult.isSuccess() ? javaVersionResult.output() : null,
                javaHomeResult != null && javaHomeResult.isSuccess() ? javaHomeResult.output() : null
            );
            builder.javaInfo(javaInfo);

            // 收集防火墙信息
            SystemInfo.FirewallInfo firewallInfo = collectFirewallInfo(context, sshService);
            builder.firewallInfo(firewallInfo);

            // 收集SELinux信息
            SystemInfo.SelinuxInfo selinuxInfo = collectSelinuxInfo(context, sshService);
            builder.selinuxInfo(selinuxInfo);

            return builder.build();

        } catch (Exception e) {
            log.error("收集CentOS系统信息失败: hostIp={}, error={}", context.getHostIp(), e.getMessage(), e);
            throw new RuntimeException("收集CentOS系统信息失败: " + e.getMessage(), e);
        }
    }

    @Override
    public SystemInfo.CpuInfo parseCpuInfo(String cpuInfo, String coreCount, String cpuUsage) {
        try {
            int cores = 1;
            String modelName = "Unknown CPU";
            double usagePercent = 0.0;

            // 解析核心数
            if (coreCount != null && !coreCount.trim().isEmpty()) {
                try {
                    cores = Integer.parseInt(coreCount.trim());
                } catch (NumberFormatException e) {
                    cores = 1;
                }
            }

            // 解析CPU型号名称
            if (cpuInfo != null) {
                Pattern modelPattern = Pattern.compile("model name\\s*:\\s*(.+)", Pattern.CASE_INSENSITIVE);
                Matcher matcher = modelPattern.matcher(cpuInfo);
                if (matcher.find()) {
                    modelName = matcher.group(1).trim();
                }
            }

            // 解析CPU使用率（从top命令输出）
            if (cpuUsage != null) {
                Pattern usagePattern = Pattern.compile("(\\d+\\.\\d+)%\\s*us");
                Matcher matcher = usagePattern.matcher(cpuUsage);
                if (matcher.find()) {
                    try {
                        usagePercent = Double.parseDouble(matcher.group(1));
                    } catch (NumberFormatException e) {
                        usagePercent = 0.0;
                    }
                }
            }

            return new SystemInfo.CpuInfo(cores, modelName, usagePercent);

        } catch (Exception e) {
            log.warn("解析CPU信息失败: {}", e.getMessage());
            return new SystemInfo.CpuInfo(1, "Unknown CPU", 0.0);
        }
    }

    @Override
    public SystemInfo.MemoryInfo parseMemoryInfo(String memInfo) {
        try {
            long totalMB = 0L;
            long availableMB = 0L;
            long swapTotalMB = 0L;
            long swapFreeMB = 0L;

            if (memInfo != null) {
                // 解析总内存
                Pattern totalPattern = Pattern.compile("MemTotal:\\s+(\\d+)\\s+kB");
                Matcher totalMatcher = totalPattern.matcher(memInfo);
                if (totalMatcher.find()) {
                    long totalKB = Long.parseLong(totalMatcher.group(1));
                    totalMB = totalKB / 1024;
                }

                // 解析可用内存
                Pattern availablePattern = Pattern.compile("MemAvailable:\\s+(\\d+)\\s+kB");
                Matcher availableMatcher = availablePattern.matcher(memInfo);
                if (availableMatcher.find()) {
                    long availableKB = Long.parseLong(availableMatcher.group(1));
                    availableMB = availableKB / 1024;
                } else {
                    // 如果没有MemAvailable，使用MemFree + Buffers + Cached
                    Pattern freePattern = Pattern.compile("MemFree:\\s+(\\d+)\\s+kB");
                    Pattern buffersPattern = Pattern.compile("Buffers:\\s+(\\d+)\\s+kB");
                    Pattern cachedPattern = Pattern.compile("Cached:\\s+(\\d+)\\s+kB");

                    long free = 0, buffers = 0, cached = 0;
                    
                    Matcher freeMatcher = freePattern.matcher(memInfo);
                    if (freeMatcher.find()) {
                        free = Long.parseLong(freeMatcher.group(1));
                    }
                    
                    Matcher buffersMatcher = buffersPattern.matcher(memInfo);
                    if (buffersMatcher.find()) {
                        buffers = Long.parseLong(buffersMatcher.group(1));
                    }
                    
                    Matcher cachedMatcher = cachedPattern.matcher(memInfo);
                    if (cachedMatcher.find()) {
                        cached = Long.parseLong(cachedMatcher.group(1));
                    }
                    
                    availableMB = (free + buffers + cached) / 1024;
                }

                // 解析Swap信息
                Pattern swapTotalPattern = Pattern.compile("SwapTotal:\\s+(\\d+)\\s+kB");
                Matcher swapTotalMatcher = swapTotalPattern.matcher(memInfo);
                if (swapTotalMatcher.find()) {
                    long swapTotalKB = Long.parseLong(swapTotalMatcher.group(1));
                    swapTotalMB = swapTotalKB / 1024;
                }

                Pattern swapFreePattern = Pattern.compile("SwapFree:\\s+(\\d+)\\s+kB");
                Matcher swapFreeMatcher = swapFreePattern.matcher(memInfo);
                if (swapFreeMatcher.find()) {
                    long swapFreeKB = Long.parseLong(swapFreeMatcher.group(1));
                    swapFreeMB = swapFreeKB / 1024;
                }
            }

            return new SystemInfo.MemoryInfo(totalMB, availableMB, swapTotalMB, swapFreeMB);

        } catch (Exception e) {
            log.warn("解析内存信息失败: {}", e.getMessage());
            return new SystemInfo.MemoryInfo(0L, 0L, 0L, 0L);
        }
    }

    @Override
    public SystemInfo.DiskInfo parseDiskInfo(String diskInfo) {
        // 这里简化实现，只返回基本信息
        return new SystemInfo.DiskInfo(0L, 0L, 0.0);
    }

    @Override
    public SystemInfo.JavaInfo parseJavaInfo(String javaVersion, String javaHome) {
        try {
            boolean installed = false;
            String version = "Not installed";
            String home = null;

            if (javaVersion != null && !javaVersion.trim().isEmpty() && 
                !javaVersion.contains("not found") && !javaVersion.contains("command not found")) {
                
                installed = true;
                
                // 解析Java版本
                Pattern versionPattern = Pattern.compile("version \"([^\"]+)\"");
                Matcher matcher = versionPattern.matcher(javaVersion);
                if (matcher.find()) {
                    version = matcher.group(1);
                } else {
                    version = javaVersion.trim();
                }
                
                // 设置JAVA_HOME
                if (javaHome != null && !javaHome.trim().isEmpty()) {
                    home = javaHome.trim();
                }
            }

            return new SystemInfo.JavaInfo(installed, version, home);

        } catch (Exception e) {
            log.warn("解析Java信息失败: {}", e.getMessage());
            return new SystemInfo.JavaInfo(false, "Parse error", null);
        }
    }

    @Override
    public SystemInfo.NetworkInfo parseNetworkInfo(String interfaceInfo, String routeInfo) {
        // 这里简化实现
        return new SystemInfo.NetworkInfo();
    }

    /**
     * 收集防火墙信息（CentOS特有）
     */
    private SystemInfo.FirewallInfo collectFirewallInfo(HostCheckContext context, SshConnectionService sshService) {
        try {
            // 检查firewalld状态
            String firewalldCommand = "systemctl is-active firewalld 2>/dev/null || echo 'inactive'";
            var firewalldResult = sshService.executeCommand(context, firewalldCommand);
            String firewalldStatus = firewalldResult != null && firewalldResult.isSuccess() ? 
                firewalldResult.output().trim() : "inactive";
            
            // 检查iptables状态
            String iptablesCommand = "systemctl is-active iptables 2>/dev/null || echo 'inactive'";
            var iptablesResult = sshService.executeCommand(context, iptablesCommand);
            String iptablesStatus = iptablesResult != null && iptablesResult.isSuccess() ? 
                iptablesResult.output().trim() : "inactive";

            boolean firewallActive = false;
            String firewallType = "none";
            
            if ("active".equals(firewalldStatus)) {
                firewallActive = true;
                firewallType = "firewalld";
            } else if ("active".equals(iptablesStatus)) {
                firewallActive = true;
                firewallType = "iptables";
            }

            return new SystemInfo.FirewallInfo(firewallActive, firewallType);

        } catch (Exception e) {
            log.warn("收集防火墙信息失败: {}", e.getMessage());
            return new SystemInfo.FirewallInfo(false, "unknown");
        }
    }

    /**
     * 收集SELinux信息（CentOS特有）
     */
    private SystemInfo.SelinuxInfo collectSelinuxInfo(HostCheckContext context, SshConnectionService sshService) {
        try {
            String command = "getenforce 2>/dev/null || echo 'Disabled'";
            var result = sshService.executeCommand(context, command);
            String status = result != null && result.isSuccess() ? 
                result.output().trim() : "Disabled";
            
            boolean enabled = false;
            String mode = "Disabled";
            
            if ("Enforcing".equalsIgnoreCase(status) || "Permissive".equalsIgnoreCase(status)) {
                enabled = true;
                mode = status;
            }

            return new SystemInfo.SelinuxInfo(enabled, mode);

        } catch (Exception e) {
            log.warn("收集SELinux信息失败: {}", e.getMessage());
            return new SystemInfo.SelinuxInfo(false, "unknown");
        }
    }
}
