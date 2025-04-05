package com.datasophon.api.service;

import com.datasophon.common.model.HostInfo;

/**
 * 操作系统信息服务接口
 * 负责管理主机操作系统信息的获取和缓存
 */
public interface OsInfoService {

    /**
     * 异步获取主机操作系统信息
     * 
     * @param hostInfo 主机信息
     */
    void getHostOsInfoAsync(HostInfo hostInfo);

    /**
     * 更新主机信息缓存
     * 
     * @param hostInfo 主机信息
     */
    void updateHostInfoCache(HostInfo hostInfo);

    /**
     * 收集主机名信息
     * 
     * @param hostInfo 主机信息
     */
    void collectHostnameInfo(HostInfo hostInfo);

    /**
     * 收集操作系统基本信息
     * 
     * @param hostInfo 主机信息
     */
    void collectOsBasicInfo(HostInfo hostInfo);

    /**
     * 收集DNS配置信息
     * 
     * @param hostInfo 主机信息
     */
    void collectDnsInfo(HostInfo hostInfo);

    /**
     * 收集hosts文件信息
     * 
     * @param hostInfo 主机信息
     */
    void collectHostsFileInfo(HostInfo hostInfo);

    /**
     * 收集CPU信息
     * 
     * @param hostInfo 主机信息
     */
    void collectCpuInfo(HostInfo hostInfo);

    /**
     * 收集内存信息
     * 
     * @param hostInfo 主机信息
     */
    void collectMemoryInfo(HostInfo hostInfo);

    /**
     * 收集磁盘信息
     * 
     * @param hostInfo 主机信息
     */
    void collectDiskInfo(HostInfo hostInfo);

    /**
     * 收集网络接口信息
     * 
     * @param hostInfo 主机信息
     */
    void collectNetworkInfo(HostInfo hostInfo);

    /**
     * 收集GPU信息
     * 
     * @param hostInfo 主机信息
     */
    void collectGpuInfo(HostInfo hostInfo);

    /**
     * 第一阶段信息收集（主机名和操作系统基本信息）
     * 供前端主列表显示使用
     * 
     * @param hostInfo 主机信息
     */
    void collectPhaseOneInfo(HostInfo hostInfo);

    /**
     * 第二阶段信息收集（详细硬件和系统配置）
     * 供前端悬浮卡片显示使用
     * 
     * @param hostInfo 主机信息
     */
    void collectPhaseTwoInfo(HostInfo hostInfo);
}