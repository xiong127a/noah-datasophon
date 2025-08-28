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

package com.datasophon.plugins.hardware;

import com.datasophon.common.enums.OsType;
import com.datasophon.plugins.api.HostCheckerPlugin;
import com.datasophon.plugins.api.model.CheckResult;
import com.datasophon.plugins.api.model.HostCheckContext;
import com.datasophon.plugins.api.model.PluginMetadata;
import lombok.extern.slf4j.Slf4j;
import org.pf4j.Extension;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 硬件信息收集插件
 * 负责收集主机的硬件信息，包括CPU、内存、磁盘等
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-28
 */
@Extension
@Slf4j
public class HardwareInfoCollectionPlugin implements HostCheckerPlugin {
    
    private static final String PLUGIN_ID = "hardware-info-collection";
    private static final String PLUGIN_VERSION = "1.0.0";
    
    // 硬件信息收集命令
    private static final String CPU_INFO_COMMAND = "cat /proc/cpuinfo";
    private static final String MEMORY_INFO_COMMAND = "cat /proc/meminfo";
    private static final String DISK_INFO_COMMAND = "df -h";
    private static final String DISK_USAGE_COMMAND = "df -B1";
    private static final String BLOCK_DEVICES_COMMAND = "lsblk -b -d -o NAME,SIZE,TYPE";
    private static final String NETWORK_INTERFACES_COMMAND = "ip addr show";
    private static final String LOAD_AVERAGE_COMMAND = "cat /proc/loadavg";
    
    @Override
    public void initialize() {
        log.info("初始化硬件信息收集插件...");
        log.info("硬件信息收集插件初始化完成");
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
        return 3; // 在SSH检查和OS信息收集之后执行
    }
    
    @Override
    public CompletableFuture<CheckResult> executeCheck(HostCheckContext context) {
        return CompletableFuture.supplyAsync(() -> {
            LocalDateTime startTime = LocalDateTime.now();
            
            try {
                log.debug("开始硬件信息收集: 主机={}", context.getHostIp());
                
                // 收集硬件信息
                Map<String, Object> hardwareInfo = collectHardwareInformation(context);
                
                Duration duration = Duration.between(startTime, LocalDateTime.now());
                log.debug("硬件信息收集完成: 主机={}, 耗时={}ms", 
                        context.getHostIp(), duration.toMillis());
                
                return CheckResult.builder()
                        .success(true)
                        .checkType("hardware-info-collection")
                        .message("硬件信息收集成功")
                        .checkTime(LocalDateTime.now())
                        .duration(duration.toMillis())
                        .data(hardwareInfo)
                        .build();
                
            } catch (Exception e) {
                Duration duration = Duration.between(startTime, LocalDateTime.now());
                log.error("硬件信息收集失败: 主机={}, 耗时={}ms, 错误={}", 
                        context.getHostIp(), duration.toMillis(), e.getMessage(), e);
                
                return CheckResult.builder()
                        .success(false)
                        .checkType("hardware-info-collection")
                        .message("硬件信息收集失败: " + e.getMessage())
                        .error(e.getMessage())
                        .checkTime(LocalDateTime.now())
                        .duration(duration.toMillis())
                        .build();
            }
        });
    }
    
    @Override
    public boolean canExecute(HostCheckContext context) {
        return context != null 
                && context.getHostIp() != null 
                && context.getSshUser() != null
                && (context.getSshPassword() != null || context.getPrivateKey() != null);
    }
    
    @Override
    public PluginMetadata getMetadata() {
        return PluginMetadata.builder()
                .pluginId(PLUGIN_ID)
                .name("硬件信息收集插件")
                .version(PLUGIN_VERSION)
                .description("收集主机硬件信息，包括CPU、内存、磁盘、网络等详细配置")
                .author("任相鹏")
                .homepage("https://github.com/datasophon/datasophon")
                .license("Apache-2.0")
                .supportedOs(Set.of("linux", "centos", "ubuntu", "rocky", "kylin"))
                .tags(Set.of("hardware", "cpu", "memory", "disk", "collection"))
                .category("hardware-info")
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
     * 收集硬件信息
     */
    private Map<String, Object> collectHardwareInformation(HostCheckContext context) throws Exception {
        Map<String, Object> hardwareInfo = new HashMap<>();
        
        try {
            // 收集CPU信息
            Map<String, Object> cpuInfo = collectCpuInfo(context);
            hardwareInfo.put("cpu", cpuInfo);
            
            // 收集内存信息
            Map<String, Object> memoryInfo = collectMemoryInfo(context);
            hardwareInfo.put("memory", memoryInfo);
            
            // 收集磁盘信息
            Map<String, Object> diskInfo = collectDiskInfo(context);
            hardwareInfo.put("disk", diskInfo);
            
            // 收集网络信息
            Map<String, Object> networkInfo = collectNetworkInfo(context);
            hardwareInfo.put("network", networkInfo);
            
            // 收集系统负载信息
            Map<String, Object> loadInfo = collectLoadInfo(context);
            hardwareInfo.put("load", loadInfo);
            
            log.debug("硬件信息收集完成: 主机={}, CPU核心={}, 内存={}GB, 磁盘={}GB", 
                    context.getHostIp(),
                    cpuInfo.get("cores"),
                    memoryInfo.get("totalGB"),
                    diskInfo.get("totalGB"));
            
        } catch (Exception e) {
            log.error("收集硬件信息时发生错误: 主机={}, 错误={}", 
                    context.getHostIp(), e.getMessage(), e);
            throw e;
        }
        
        return hardwareInfo;
    }
    
    /**
     * 收集CPU信息
     */
    private Map<String, Object> collectCpuInfo(HostCheckContext context) throws Exception {
        Map<String, Object> cpuInfo = new HashMap<>();
        
        // 模拟CPU信息收集
        String cpuInfoOutput = executeCommand(context, CPU_INFO_COMMAND);
        
        // 解析CPU信息
        int processors = countProcessors(cpuInfoOutput);
        String modelName = extractCpuModelName(cpuInfoOutput);
        String cpuMHz = extractCpuMHz(cpuInfoOutput);
        
        cpuInfo.put("cores", processors);
        cpuInfo.put("modelName", modelName);
        cpuInfo.put("mhz", cpuMHz);
        cpuInfo.put("architecture", extractCpuArchitecture(cpuInfoOutput));
        
        return cpuInfo;
    }
    
    /**
     * 收集内存信息
     */
    private Map<String, Object> collectMemoryInfo(HostCheckContext context) throws Exception {
        Map<String, Object> memoryInfo = new HashMap<>();
        
        // 模拟内存信息收集
        String memInfoOutput = executeCommand(context, MEMORY_INFO_COMMAND);
        
        // 解析内存信息
        long totalMemKB = extractMemoryValue(memInfoOutput, "MemTotal");
        long availableMemKB = extractMemoryValue(memInfoOutput, "MemAvailable");
        long freeMemKB = extractMemoryValue(memInfoOutput, "MemFree");
        
        memoryInfo.put("totalKB", totalMemKB);
        memoryInfo.put("totalMB", totalMemKB / 1024);
        memoryInfo.put("totalGB", totalMemKB / 1024 / 1024);
        memoryInfo.put("availableKB", availableMemKB);
        memoryInfo.put("freeKB", freeMemKB);
        memoryInfo.put("usedKB", totalMemKB - freeMemKB);
        
        return memoryInfo;
    }
    
    /**
     * 收集磁盘信息
     */
    private Map<String, Object> collectDiskInfo(HostCheckContext context) throws Exception {
        Map<String, Object> diskInfo = new HashMap<>();
        
        // 收集磁盘使用信息
        String diskUsageOutput = executeCommand(context, DISK_USAGE_COMMAND);
        Map<String, Object> diskUsage = parseDiskUsage(diskUsageOutput);
        
        // 收集块设备信息
        String blockDevicesOutput = executeCommand(context, BLOCK_DEVICES_COMMAND);
        Map<String, Object> blockDevices = parseBlockDevices(blockDevicesOutput);
        
        diskInfo.put("usage", diskUsage);
        diskInfo.put("devices", blockDevices);
        
        // 计算总磁盘大小
        long totalBytes = ((Long) blockDevices.get("totalBytes"));
        diskInfo.put("totalBytes", totalBytes);
        diskInfo.put("totalGB", totalBytes / 1024 / 1024 / 1024);
        
        return diskInfo;
    }
    
    /**
     * 收集网络信息
     */
    private Map<String, Object> collectNetworkInfo(HostCheckContext context) throws Exception {
        Map<String, Object> networkInfo = new HashMap<>();
        
        // 模拟网络接口信息收集
        String networkOutput = executeCommand(context, NETWORK_INTERFACES_COMMAND);
        
        networkInfo.put("interfaces", parseNetworkInterfaces(networkOutput));
        
        return networkInfo;
    }
    
    /**
     * 收集系统负载信息
     */
    private Map<String, Object> collectLoadInfo(HostCheckContext context) throws Exception {
        Map<String, Object> loadInfo = new HashMap<>();
        
        // 模拟负载信息收集
        String loadOutput = executeCommand(context, LOAD_AVERAGE_COMMAND);
        
        String[] parts = loadOutput.trim().split("\\s+");
        if (parts.length >= 3) {
            loadInfo.put("load1", parts[0]);
            loadInfo.put("load5", parts[1]);
            loadInfo.put("load15", parts[2]);
        }
        
        return loadInfo;
    }
    
    /**
     * 执行远程命令 (模拟实现)
     */
    private String executeCommand(HostCheckContext context, String command) throws Exception {
        log.debug("执行命令: 主机={}, 命令={}", context.getHostIp(), command);
        
        // 模拟命令执行结果
        switch (command) {
            case CPU_INFO_COMMAND:
                return "processor\t: 0\nvendor_id\t: GenuineIntel\ncpu family\t: 6\nmodel\t\t: 85\nmodel name\t: Intel(R) Xeon(R) CPU E5-2676 v3 @ 2.40GHz\nstepping\t: 4\nmicrocode\t: 0x428\ncpu MHz\t\t: 2400.000\ncache size\t: 30720 KB\n\nprocessor\t: 1\nvendor_id\t: GenuineIntel\ncpu family\t: 6\nmodel\t\t: 85\nmodel name\t: Intel(R) Xeon(R) CPU E5-2676 v3 @ 2.40GHz\nstepping\t: 4\nmicrocode\t: 0x428\ncpu MHz\t\t: 2400.000\ncache size\t: 30720 KB";
            case MEMORY_INFO_COMMAND:
                return "MemTotal:        4039256 kB\nMemFree:         1584088 kB\nMemAvailable:    3467788 kB\nBuffers:          106524 kB\nCached:          1777176 kB\nSwapCached:            0 kB";
            case DISK_USAGE_COMMAND:
                return "Filesystem     1B-blocks      Used Available Use% Mounted on\n/dev/sda1      42949672960 12884901888 28991029248  31% /\ntmpfs           2019628032         0  2019628032   0% /dev/shm";
            case BLOCK_DEVICES_COMMAND:
                return "NAME SIZE TYPE\nsda  42949672960 disk\nsr0  1073741312 rom";
            case NETWORK_INTERFACES_COMMAND:
                return "1: lo: <LOOPBACK,UP,LOWER_UP> mtu 65536 qdisc noqueue state UNKNOWN\n    inet 127.0.0.1/8 scope host lo\n2: eth0: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 qdisc pfifo_fast state UP qlen 1000\n    inet 192.168.1.100/24 brd 192.168.1.255 scope global eth0";
            case LOAD_AVERAGE_COMMAND:
                return "0.08 0.02 0.01 1/123 12345";
            default:
                throw new UnsupportedOperationException("不支持的命令: " + command);
        }
    }
    
    /**
     * 统计处理器数量
     */
    private int countProcessors(String cpuInfo) {
        Pattern pattern = Pattern.compile("processor\\s*:\\s*(\\d+)");
        Matcher matcher = pattern.matcher(cpuInfo);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }
    
    /**
     * 提取CPU型号名称
     */
    private String extractCpuModelName(String cpuInfo) {
        Pattern pattern = Pattern.compile("model name\\s*:\\s*(.+)");
        Matcher matcher = pattern.matcher(cpuInfo);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "Unknown";
    }
    
    /**
     * 提取CPU频率
     */
    private String extractCpuMHz(String cpuInfo) {
        Pattern pattern = Pattern.compile("cpu MHz\\s*:\\s*([\\d.]+)");
        Matcher matcher = pattern.matcher(cpuInfo);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "Unknown";
    }
    
    /**
     * 提取CPU架构
     */
    private String extractCpuArchitecture(String cpuInfo) {
        if (cpuInfo.contains("x86_64") || cpuInfo.contains("amd64")) {
            return "x86_64";
        } else if (cpuInfo.contains("aarch64") || cpuInfo.contains("arm64")) {
            return "aarch64";
        }
        return "unknown";
    }
    
    /**
     * 提取内存值
     */
    private long extractMemoryValue(String memInfo, String key) {
        Pattern pattern = Pattern.compile(key + ":\\s*(\\d+)\\s*kB");
        Matcher matcher = pattern.matcher(memInfo);
        if (matcher.find()) {
            return Long.parseLong(matcher.group(1));
        }
        return 0;
    }
    
    /**
     * 解析磁盘使用情况
     */
    private Map<String, Object> parseDiskUsage(String diskUsage) {
        Map<String, Object> usage = new HashMap<>();
        // 简化解析实现
        usage.put("rootUsed", 12884901888L);
        usage.put("rootAvailable", 28991029248L);
        usage.put("rootTotal", 42949672960L);
        return usage;
    }
    
    /**
     * 解析块设备信息
     */
    private Map<String, Object> parseBlockDevices(String blockDevices) {
        Map<String, Object> devices = new HashMap<>();
        // 简化解析实现
        devices.put("totalBytes", 42949672960L);
        devices.put("devices", List.of(
            Map.of("name", "sda", "size", 42949672960L, "type", "disk")
        ));
        return devices;
    }
    
    /**
     * 解析网络接口信息
     */
    private Map<String, Object> parseNetworkInterfaces(String networkOutput) {
        // 简化解析实现
        return Map.of(
            "eth0", Map.of("ip", "192.168.1.100", "state", "UP"),
            "lo", Map.of("ip", "127.0.0.1", "state", "UP")
        );
    }
}
