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

package com.datasophon.common.model;

import com.datasophon.common.enums.OsInfoStatusEnum;
import com.datasophon.common.enums.OsType;
import com.datasophon.common.model.hardware.*;
import lombok.Builder;
import lombok.Data;
import lombok.With;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;

/**
 * 主机完整信息模型
 * 整合操作系统、硬件、网络、主机等所有信息的综合模型
 * 
 * 设计原则：
 * 1. 模块化组织 - 按功能分组信息
 * 2. 状态管理 - 每个模块都有独立的收集状态
 * 3. 扩展性 - 预留扩展字段和接口
 * 4. 一致性 - 统一的时间戳和状态枚举
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-28
 */
@Data
@Builder
@With
public class OsInfo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    // ==================== 基本标识信息 ====================
    
    /**
     * 主机IP地址（主键标识）
     */
    private String hostIp;
    
    /**
     * 主机名
     */
    private String hostname;
    
    /**
     * 信息收集时间戳
     */
    @Builder.Default
    private LocalDateTime collectTime = LocalDateTime.now();
    
    /**
     * 最后更新时间
     */
    private LocalDateTime lastUpdateTime;
    
    /**
     * 整体收集状态
     */
    @Builder.Default
    private OsInfoStatusEnum overallStatus = OsInfoStatusEnum.LOADING;

    // ==================== 操作系统信息 ====================
    
    /**
     * 操作系统信息
     */
    @Builder.Default
    private OperatingSystemInfo osInfo = OperatingSystemInfo.builder().build();
    
    /**
     * 操作系统信息收集状态
     */
    @Builder.Default 
    private OsInfoStatusEnum osInfoStatus = OsInfoStatusEnum.LOADING;

    // ==================== 硬件信息集合 ====================
    
    /**
     * CPU信息
     */
    private CpuInfo cpuInfo;
    
    /**
     * CPU信息收集状态
     */
    @Builder.Default
    private OsInfoStatusEnum cpuInfoStatus = OsInfoStatusEnum.LOADING;
    
    /**
     * 内存信息
     */
    private MemoryInfo memoryInfo;
    
    /**
     * 内存信息收集状态
     */
    @Builder.Default
    private OsInfoStatusEnum memoryInfoStatus = OsInfoStatusEnum.LOADING;
    
    /**
     * 磁盘信息
     */
    private DiskInfo diskInfo;
    
    /**
     * 磁盘信息收集状态
     */
    @Builder.Default
    private OsInfoStatusEnum diskInfoStatus = OsInfoStatusEnum.LOADING;
    
    /**
     * GPU信息
     */
    private GpuInfo gpuInfo;
    
    /**
     * GPU信息收集状态
     */
    @Builder.Default
    private OsInfoStatusEnum gpuInfoStatus = OsInfoStatusEnum.LOADING;
    
    /**
     * 交换分区信息
     */
    private SwapInfo swapInfo;
    
    /**
     * 交换分区信息收集状态
     */
    @Builder.Default
    private OsInfoStatusEnum swapInfoStatus = OsInfoStatusEnum.LOADING;

    // ==================== 网络信息 ====================
    
    /**
     * 网络接口信息
     */
    private NetworkInfo networkInfo;
    
    /**
     * 网络信息收集状态
     */
    @Builder.Default
    private OsInfoStatusEnum networkInfoStatus = OsInfoStatusEnum.LOADING;
    
    /**
     * DNS信息
     */
    private DnsInfo dnsInfo;
    
    /**
     * DNS信息收集状态
     */
    @Builder.Default
    private OsInfoStatusEnum dnsInfoStatus = OsInfoStatusEnum.LOADING;

    // ==================== 主机运行信息 ====================
    
    /**
     * 主机运行信息
     */
    @Builder.Default
    private HostRuntimeInfo runtimeInfo = HostRuntimeInfo.builder().build();
    
    /**
     * 运行信息收集状态
     */
    @Builder.Default
    private OsInfoStatusEnum runtimeInfoStatus = OsInfoStatusEnum.LOADING;

    // ==================== 扩展信息 ====================
    
    /**
     * 扩展属性映射
     * 用于存储不在标准字段中的额外信息
     */
    private Map<String, Object> extensionProperties;
    
    /**
     * 收集过程中的错误信息列表
     */
    private List<String> errorMessages;
    
    /**
     * 收集器版本信息
     */
    private String collectorVersion;

    // ==================== 嵌套信息类 ====================
    
    /**
     * 操作系统基础信息
     */
    @Data
    @Builder
    @With
    public static class OperatingSystemInfo implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        
        /**
         * 操作系统类型
         */
        private OsType osType;
        
        /**
         * 发行版名称
         */
        private String distribution;
        
        /**
         * 系统版本
         */
        private String version;
        
        /**
         * 系统架构
         */
        private String architecture;
        
        /**
         * 内核版本
         */
        private String kernelVersion;
        
        /**
         * 系统ID（来自/etc/os-release）
         */
        private String systemId;
        
        /**
         * 系统完整名称
         */
        private String fullName;
        
        /**
         * 系统构建信息
         */
        private String buildInfo;
        
        /**
         * 包管理器类型（yum, apt, zypper等）
         */
        private String packageManager;
        
        /**
         * 是否支持systemd
         */
        private Boolean supportsSystemd;
        
        /**
         * 原始系统信息
         */
        private String rawOsInfo;
    }
    
    /**
     * 主机运行时信息
     */
    @Data
    @Builder  
    @With
    public static class HostRuntimeInfo implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        
        /**
         * 系统启动时间
         */
        private LocalDateTime bootTime;
        
        /**
         * 系统运行时间（秒）
         */
        private Long uptimeSeconds;
        
        /**
         * 系统运行时间（格式化字符串）
         */
        private String uptimeFormatted;
        
        /**
         * 当前登录用户数
         */
        private Integer loginUserCount;
        
        /**
         * 系统平均负载（1分钟）
         */
        private Double loadAverage1Min;
        
        /**
         * 系统平均负载（5分钟）
         */
        private Double loadAverage5Min;
        
        /**
         * 系统平均负载（15分钟）
         */
        private Double loadAverage15Min;
        
        /**
         * 进程总数
         */
        private Integer totalProcesses;
        
        /**
         * 正在运行的进程数
         */
        private Integer runningProcesses;
        
        /**
         * 系统时区
         */
        private String timezone;
        
        /**
         * 系统语言环境
         */
        private String locale;
        
        /**
         * 文件句柄限制
         */
        private Integer fileHandleLimit;
        
        /**
         * 当前打开的文件句柄数
         */
        private Integer openFileHandles;
    }

    // ==================== 工具方法 ====================
    
    /**
     * 检查所有信息是否收集完成
     * 
     * @return 如果所有必要信息都收集成功则返回true
     */
    public boolean isCollectionComplete() {
        return osInfoStatus == OsInfoStatusEnum.SUCCESS &&
               cpuInfoStatus == OsInfoStatusEnum.SUCCESS &&
               memoryInfoStatus == OsInfoStatusEnum.SUCCESS &&
               diskInfoStatus == OsInfoStatusEnum.SUCCESS &&
               networkInfoStatus == OsInfoStatusEnum.SUCCESS &&
               runtimeInfoStatus == OsInfoStatusEnum.SUCCESS;
    }
    
    /**
     * 检查是否有任何收集失败
     * 
     * @return 如果有任何信息收集失败则返回true
     */
    public boolean hasCollectionErrors() {
        return osInfoStatus == OsInfoStatusEnum.ERROR ||
               cpuInfoStatus == OsInfoStatusEnum.ERROR ||
               memoryInfoStatus == OsInfoStatusEnum.ERROR ||
               diskInfoStatus == OsInfoStatusEnum.ERROR ||
               networkInfoStatus == OsInfoStatusEnum.ERROR ||
               runtimeInfoStatus == OsInfoStatusEnum.ERROR;
    }
    
    /**
     * 获取收集完成百分比
     * 
     * @return 收集完成的百分比 (0-100)
     */
    public int getCollectionProgress() {
        int totalComponents = 6; // os, cpu, memory, disk, network, runtime
        int completedComponents = 0;
        
        if (osInfoStatus == OsInfoStatusEnum.SUCCESS) completedComponents++;
        if (cpuInfoStatus == OsInfoStatusEnum.SUCCESS) completedComponents++;
        if (memoryInfoStatus == OsInfoStatusEnum.SUCCESS) completedComponents++;
        if (diskInfoStatus == OsInfoStatusEnum.SUCCESS) completedComponents++;
        if (networkInfoStatus == OsInfoStatusEnum.SUCCESS) completedComponents++;
        if (runtimeInfoStatus == OsInfoStatusEnum.SUCCESS) completedComponents++;
        
        return (int) ((double) completedComponents / totalComponents * 100);
    }
    
    /**
     * 更新整体收集状态
     * 根据各个组件的状态自动更新整体状态
     */
    public void updateOverallStatus() {
        if (isCollectionComplete()) {
            this.overallStatus = OsInfoStatusEnum.SUCCESS;
        } else if (hasCollectionErrors()) {
            this.overallStatus = OsInfoStatusEnum.ERROR;
        } else {
            this.overallStatus = OsInfoStatusEnum.LOADING;
        }
        this.lastUpdateTime = LocalDateTime.now();
    }
}
