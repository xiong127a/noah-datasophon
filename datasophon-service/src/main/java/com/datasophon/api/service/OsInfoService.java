package com.datasophon.api.service;

import com.datasophon.common.model.HostInfo;

/**
 * 操作系统信息收集服务接口
 * 负责收集主机操作系统和硬件信息
 * 
 * 注意: 系统仅支持Linux操作系统
 */
public interface OsInfoService {

    /**
     * 异步获取主机操作系统信息
     * 
     * @param hostInfo 主机信息对象
     */
    void getHostOsInfoAsync(HostInfo hostInfo);

    /**
     * 收集主机名信息
     * 
     * @param hostInfo 主机信息对象
     */
    void collectHostnameInfo(HostInfo hostInfo);

    /**
     * 收集Linux基本操作系统信息
     * 包括发行版类型、版本等
     * 
     * @param hostInfo 主机信息对象
     */
    void collectOsBasicInfo(HostInfo hostInfo);

    /**
     * 收集DNS配置信息
     * 
     * @param hostInfo 主机信息对象
     */
    void collectDnsInfo(HostInfo hostInfo);

    /**
     * 收集hosts文件信息
     * 
     * @param hostInfo 主机信息对象
     */
    void collectHostsFileInfo(HostInfo hostInfo);

    /**
     * 收集CPU信息
     * 
     * @param hostInfo 主机信息对象
     */
    void collectCpuInfo(HostInfo hostInfo);

    /**
     * 收集内存信息
     * 
     * @param hostInfo 主机信息对象
     */
    void collectMemoryInfo(HostInfo hostInfo);

    /**
     * 收集磁盘信息
     * 
     * @param hostInfo 主机信息对象
     */
    void collectDiskInfo(HostInfo hostInfo);

    /**
     * 收集GPU信息
     * 
     * @param hostInfo 主机信息对象
     */
    void collectGpuInfo(HostInfo hostInfo);

    /**
     * 收集网络信息
     * 
     * @param hostInfo 主机信息对象
     */
    void collectNetworkInfo(HostInfo hostInfo);

    /**
     * 收集交换空间信息
     * 
     * @param hostInfo 主机信息对象
     */
    void collectSwapInfo(HostInfo hostInfo);

    /**
     * 第一阶段信息收集（主机名和操作系统基本信息）
     * 供前端主列表显示使用
     * 
     * @param hostInfo 主机信息对象
     */
    void collectPhaseOneInfo(HostInfo hostInfo);

    /**
     * 第二阶段信息收集（详细硬件和系统配置）
     * 供前端悬浮卡片显示使用
     * 
     * @param hostInfo 主机信息对象
     */
    void collectPhaseTwoInfo(HostInfo hostInfo);

    /**
     * 更新主机信息缓存
     * 
     * @param hostInfo 要更新的主机信息
     */
    void updateHostInfoCache(HostInfo hostInfo);

    /**
     * 重置信息收集队列
     */
    void resetCollectionQueue();

    /**
     * 将主机添加到信息收集队列
     * 
     * @param hostInfo 要添加的主机信息
     */
    void addHostToCollectionQueue(HostInfo hostInfo);
}