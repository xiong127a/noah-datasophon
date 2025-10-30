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

package com.datasophon.common.utils;

import lombok.extern.slf4j.Slf4j;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.FileSystem;
import oshi.software.os.OSFileStore;
import oshi.software.os.OperatingSystem;

/**
 * 系统硬件信息工具类（基于 OSHI）
 * 提供跨平台的系统信息获取能力，替代原有的 Shell 脚本调用
 * 
 * @author DataSophon Team
 */
@Slf4j
public class HardwareInfoUtils {
    
    private static final SystemInfo SYSTEM_INFO = new SystemInfo();
    private static final HardwareAbstractionLayer HAL = SYSTEM_INFO.getHardware();
    private static final OperatingSystem OS = SYSTEM_INFO.getOperatingSystem();
    
    /**
     * 获取 CPU 架构
     * 
     * @return CPU 架构字符串 (如: x86_64, aarch64, arm64)
     */
    public static String getCpuArchitecture() {
        try {
            CentralProcessor processor = HAL.getProcessor();
            String arch = processor.getProcessorIdentifier().getMicroarchitecture();
            
            // 如果微架构信息为空，使用系统属性作为后备
            if (arch == null || arch.isEmpty() || arch.equals("unknown")) {
                arch = System.getProperty("os.arch");
            }
            
            log.debug("CPU Architecture: {}", arch);
            return arch;
        } catch (Exception e) {
            log.warn("Failed to get CPU architecture via OSHI, falling back to system property", e);
            return System.getProperty("os.arch", "unknown");
        }
    }
    
    /**
     * 获取逻辑处理器核心数
     * 
     * @return CPU 核心数
     */
    public static int getCpuCores() {
        try {
            CentralProcessor processor = HAL.getProcessor();
            int cores = processor.getLogicalProcessorCount();
            log.debug("CPU Logical Cores: {}", cores);
            return cores;
        } catch (Exception e) {
            log.warn("Failed to get CPU cores via OSHI, falling back to Runtime", e);
            return Runtime.getRuntime().availableProcessors();
        }
    }
    
    /**
     * 获取物理处理器核心数
     * 
     * @return 物理 CPU 核心数
     */
    public static int getPhysicalCpuCores() {
        try {
            CentralProcessor processor = HAL.getProcessor();
            int cores = processor.getPhysicalProcessorCount();
            log.debug("CPU Physical Cores: {}", cores);
            return cores;
        } catch (Exception e) {
            log.warn("Failed to get physical CPU cores", e);
            return getCpuCores();
        }
    }
    
    /**
     * 获取操作系统信息
     * 
     * @return 操作系统描述字符串
     */
    public static String getOsInfo() {
        try {
            String osInfo = String.format("%s %s (%d-bit)", 
                OS.getFamily(), 
                OS.getVersionInfo().getVersion(),
                OS.getBitness());
            log.debug("OS Info: {}", osInfo);
            return osInfo;
        } catch (Exception e) {
            log.warn("Failed to get OS info via OSHI, falling back to system properties", e);
            return System.getProperty("os.name") + " " + System.getProperty("os.version");
        }
    }
    
    /**
     * 获取系统内存信息（物理内存）
     * 
     * @return 内存信息对象
     */
    public static MemoryInfo getMemoryInfo() {
        try {
            GlobalMemory memory = HAL.getMemory();
            long totalBytes = memory.getTotal();
            long availableBytes = memory.getAvailable();
            long usedBytes = totalBytes - availableBytes;
            
            MemoryInfo info = new MemoryInfo();
            info.setTotalBytes(totalBytes);
            info.setUsedBytes(usedBytes);
            info.setAvailableBytes(availableBytes);
            info.setTotalGB(totalBytes / 1024 / 1024 / 1024);
            info.setUsedGB(usedBytes / 1024 / 1024 / 1024);
            info.setAvailableGB(availableBytes / 1024 / 1024 / 1024);
            info.setUsagePercent((double) usedBytes / totalBytes * 100);
            
            log.debug("Memory Info: Total={}GB, Used={}GB, Available={}GB", 
                info.getTotalGB(), info.getUsedGB(), info.getAvailableGB());
            
            return info;
        } catch (Exception e) {
            log.error("Failed to get memory info", e);
            return new MemoryInfo();
        }
    }
    
    /**
     * 获取内存信息的格式化字符串
     * 
     * @return 格式化的内存信息
     */
    public static String getMemoryInfoString() {
        MemoryInfo info = getMemoryInfo();
        return String.format("Total: %d GB, Used: %d GB, Free: %d GB (%.1f%% used)", 
            info.getTotalGB(), 
            info.getUsedGB(), 
            info.getAvailableGB(),
            info.getUsagePercent());
    }
    
    /**
     * 获取磁盘信息（所有文件系统）
     * 
     * @return 磁盘信息对象
     */
    public static DiskInfo getDiskInfo() {
        try {
            FileSystem fileSystem = OS.getFileSystem();
            long totalBytes = 0;
            long usableBytes = 0;
            
            for (OSFileStore store : fileSystem.getFileStores()) {
                totalBytes += store.getTotalSpace();
                usableBytes += store.getUsableSpace();
            }
            
            long usedBytes = totalBytes - usableBytes;
            
            DiskInfo info = new DiskInfo();
            info.setTotalBytes(totalBytes);
            info.setUsedBytes(usedBytes);
            info.setUsableBytes(usableBytes);
            info.setTotalGB(totalBytes / 1024 / 1024 / 1024);
            info.setUsedGB(usedBytes / 1024 / 1024 / 1024);
            info.setUsableGB(usableBytes / 1024 / 1024 / 1024);
            info.setUsagePercent(totalBytes > 0 ? (double) usedBytes / totalBytes * 100 : 0);
            
            log.debug("Disk Info: Total={}GB, Used={}GB, Usable={}GB", 
                info.getTotalGB(), info.getUsedGB(), info.getUsableGB());
            
            return info;
        } catch (Exception e) {
            log.error("Failed to get disk info", e);
            return new DiskInfo();
        }
    }
    
    /**
     * 获取磁盘信息的格式化字符串
     * 
     * @return 格式化的磁盘信息
     */
    public static String getDiskInfoString() {
        DiskInfo info = getDiskInfo();
        return String.format("Total: %d GB, Used: %d GB, Free: %d GB (%.1f%% used)", 
            info.getTotalGB(), 
            info.getUsedGB(), 
            info.getUsableGB(),
            info.getUsagePercent());
    }
    
    /**
     * 获取系统负载平均值（1分钟）
     * 
     * @return 系统负载字符串
     */
    public static String getSystemLoad() {
        try {
            CentralProcessor processor = HAL.getProcessor();
            double[] loadAverage = processor.getSystemLoadAverage(1);
            
            if (loadAverage[0] >= 0) {
                String load = String.format("%.2f", loadAverage[0]);
                log.debug("System Load (1min): {}", load);
                return load;
            } else {
                log.debug("System load not available on this platform");
                return "N/A";
            }
        } catch (Exception e) {
            log.warn("Failed to get system load", e);
            return "N/A";
        }
    }
    
    /**
     * 获取 CPU 使用率（需要间隔测量）
     * 
     * @param waitTimeMs 等待时间（毫秒），建议 1000ms
     * @return CPU 使用率百分比
     */
    public static double getCpuUsage(long waitTimeMs) {
        try {
            CentralProcessor processor = HAL.getProcessor();
            long[] prevTicks = processor.getSystemCpuLoadTicks();
            
            // 等待指定时间
            Thread.sleep(waitTimeMs);
            
            double cpuUsage = processor.getSystemCpuLoadBetweenTicks(prevTicks) * 100;
            log.debug("CPU Usage: {:.2f}%", cpuUsage);
            return cpuUsage;
        } catch (Exception e) {
            log.warn("Failed to get CPU usage", e);
            return -1;
        }
    }
    
    /**
     * 内存信息数据类
     */
    public static class MemoryInfo {
        private long totalBytes;
        private long usedBytes;
        private long availableBytes;
        private long totalGB;
        private long usedGB;
        private long availableGB;
        private double usagePercent;
        
        // Getters and Setters
        public long getTotalBytes() { return totalBytes; }
        public void setTotalBytes(long totalBytes) { this.totalBytes = totalBytes; }
        
        public long getUsedBytes() { return usedBytes; }
        public void setUsedBytes(long usedBytes) { this.usedBytes = usedBytes; }
        
        public long getAvailableBytes() { return availableBytes; }
        public void setAvailableBytes(long availableBytes) { this.availableBytes = availableBytes; }
        
        public long getTotalGB() { return totalGB; }
        public void setTotalGB(long totalGB) { this.totalGB = totalGB; }
        
        public long getUsedGB() { return usedGB; }
        public void setUsedGB(long usedGB) { this.usedGB = usedGB; }
        
        public long getAvailableGB() { return availableGB; }
        public void setAvailableGB(long availableGB) { this.availableGB = availableGB; }
        
        public double getUsagePercent() { return usagePercent; }
        public void setUsagePercent(double usagePercent) { this.usagePercent = usagePercent; }
    }
    
    /**
     * 磁盘信息数据类
     */
    public static class DiskInfo {
        private long totalBytes;
        private long usedBytes;
        private long usableBytes;
        private long totalGB;
        private long usedGB;
        private long usableGB;
        private double usagePercent;
        
        // Getters and Setters
        public long getTotalBytes() { return totalBytes; }
        public void setTotalBytes(long totalBytes) { this.totalBytes = totalBytes; }
        
        public long getUsedBytes() { return usedBytes; }
        public void setUsedBytes(long usedBytes) { this.usedBytes = usedBytes; }
        
        public long getUsableBytes() { return usableBytes; }
        public void setUsableBytes(long usableBytes) { this.usableBytes = usableBytes; }
        
        public long getTotalGB() { return totalGB; }
        public void setTotalGB(long totalGB) { this.totalGB = totalGB; }
        
        public long getUsedGB() { return usedGB; }
        public void setUsedGB(long usedGB) { this.usedGB = usedGB; }
        
        public long getUsableGB() { return usableGB; }
        public void setUsableGB(long usableGB) { this.usableGB = usableGB; }
        
        public double getUsagePercent() { return usagePercent; }
        public void setUsagePercent(double usagePercent) { this.usagePercent = usagePercent; }
    }
}

