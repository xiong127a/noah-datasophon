package com.datasophon.api.service.impl.osinfo;

import com.datasophon.common.model.HostInfo;
import com.datasophon.common.model.OsInfo;
import org.apache.sshd.client.session.ClientSession;

/**
 * 操作系统信息收集器接口
 * 定义收集操作系统和硬件信息的方法
 */
public interface IOsInfoCollector {

    /**
     * 获取操作系统支持的系统类型
     * 
     * @return 系统类型："linux" 或 "windows"
     */
    String getSupportedOsType();

    /**
     * 收集操作系统信息
     * 
     * @param hostInfo 主机信息
     * @param session  SSH会话
     * @param osInfo   操作系统信息对象（输出参数）
     * @return 操作系统信息对象
     */
    OsInfo collectOsInfo(HostInfo hostInfo, ClientSession session, OsInfo osInfo);

    /**
     * 收集硬件信息（CPU、内存、存储等）
     * 
     * @param osInfo  操作系统信息对象（将被更新）
     * @param session SSH会话
     */
    void collectHardwareInfo(OsInfo osInfo, ClientSession session);
}