package com.datasophon.api.service;

import com.datasophon.common.model.HostInfo;
import com.datasophon.common.model.OsInfo;

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
     * 同步获取主机操作系统信息
     * 
     * @param hostInfo 主机信息
     * @return 操作系统信息
     */
    OsInfo getHostOsInfo(HostInfo hostInfo);

    /**
     * 更新主机信息缓存
     * 
     * @param hostInfo 主机信息
     */
    void updateHostInfoCache(HostInfo hostInfo);
}