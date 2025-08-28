package com.datasophon.api.service.impl.osinfo;

import com.datasophon.common.model.HostInfo;
import com.datasophon.common.model.OsInfo;


/**
 * 操作系统信息收集器接口
 * 定义收集操作系统和硬件信息的方法
 */
public interface IOsInfoCollector {

    /**
     * 获取操作系统支持的系统类型
     * 
     * @return 系统类型："linux"
     */
    String getSupportedOsType();

    /**
     * 收集操作系统信息
     *
     * @param hostInfo     主机信息（包含SSH连接信息）
     * @param osInfo       操作系统信息对象（输出参数）
     * @param cacheUpdater 缓存更新函数
     */
    void collectOsInfo(HostInfo hostInfo, OsInfo osInfo, CacheUpdater cacheUpdater);

    /**
     * 收集硬件信息（CPU、内存、存储等）
     * 
     * @param hostInfo     主机信息（包含SSH连接信息）
     * @param osInfo       操作系统信息对象（将被更新）
     * @param cacheUpdater 缓存更新函数
     */
    void collectHardwareInfo(HostInfo hostInfo, OsInfo osInfo, CacheUpdater cacheUpdater);

    /**
     * 收集CPU信息
     * 
     * @param hostInfo     主机信息
     * @param session      SSH会话
     * @param osInfo       操作系统信息对象
     * @param cacheUpdater 缓存更新函数
     */
    void collectCpuInfo(HostInfo hostInfo, ClientSession session, OsInfo osInfo, CacheUpdater cacheUpdater);

    /**
     * 收集内存信息
     * 
     * @param hostInfo     主机信息
     * @param session      SSH会话
     * @param osInfo       操作系统信息对象
     * @param cacheUpdater 缓存更新函数
     */
    void collectMemoryInfo(HostInfo hostInfo, ClientSession session, OsInfo osInfo, CacheUpdater cacheUpdater);

    /**
     * 收集磁盘信息
     * 
     * @param hostInfo     主机信息
     * @param session      SSH会话
     * @param osInfo       操作系统信息对象
     * @param cacheUpdater 缓存更新函数
     */
    void collectDiskInfo(HostInfo hostInfo, ClientSession session, OsInfo osInfo, CacheUpdater cacheUpdater);

    /**
     * 收集GPU信息
     * 
     * @param hostInfo     主机信息
     * @param session      SSH会话
     * @param osInfo       操作系统信息对象
     * @param cacheUpdater 缓存更新函数
     */
    void collectGpuInfo(HostInfo hostInfo, ClientSession session, OsInfo osInfo, CacheUpdater cacheUpdater);

    /**
     * 收集网络信息
     * 
     * @param hostInfo     主机信息
     * @param session      SSH会话
     * @param osInfo       操作系统信息对象
     * @param cacheUpdater 缓存更新函数
     */
    void collectNetworkInfo(HostInfo hostInfo, ClientSession session, OsInfo osInfo, CacheUpdater cacheUpdater);

    /**
     * 收集DNS信息
     * 
     * @param hostInfo     主机信息
     * @param session      SSH会话
     * @param osInfo       操作系统信息对象
     * @param cacheUpdater 缓存更新函数
     */
    void collectDnsInfo(HostInfo hostInfo, ClientSession session, OsInfo osInfo, CacheUpdater cacheUpdater);

    /**
     * 收集Hosts文件信息
     * 
     * @param hostInfo     主机信息
     * @param session      SSH会话
     * @param osInfo       操作系统信息对象
     * @param cacheUpdater 缓存更新函数
     */
    void collectHostsFileInfo(HostInfo hostInfo, ClientSession session, OsInfo osInfo, CacheUpdater cacheUpdater);

    /**
     * 收集交换空间信息
     * 
     * @param hostInfo     主机信息
     * @param session      SSH会话
     * @param osInfo       操作系统信息对象
     * @param cacheUpdater 缓存更新函数
     */
    void collectSwapInfo(HostInfo hostInfo, ClientSession session, OsInfo osInfo, CacheUpdater cacheUpdater);

    /**
     * 缓存更新器接口
     */
    @FunctionalInterface
    interface CacheUpdater {
        /**
         * 更新主机信息缓存
         * 
         * @param hostInfo 要更新的主机信息
         */
        void updateCache(HostInfo hostInfo);
    }
}