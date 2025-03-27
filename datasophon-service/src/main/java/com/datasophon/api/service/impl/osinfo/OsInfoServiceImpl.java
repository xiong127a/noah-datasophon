package com.datasophon.api.service.impl.osinfo;

import com.datasophon.api.service.OsInfoService;
import com.datasophon.api.utils.MinaUtils;
import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.model.HostInfo;
import com.datasophon.common.model.OsInfo;
import org.apache.commons.lang.StringUtils;
import org.apache.sshd.client.session.ClientSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 操作系统信息服务实现类
 * 负责管理主机操作系统信息的获取和缓存
 */
@Service
public class OsInfoServiceImpl implements OsInfoService {

    private static final Logger logger = LoggerFactory.getLogger(OsInfoServiceImpl.class);

    @Autowired
    private OsInfoCollectorFactory osInfoCollectorFactory;

    @Override
    public void getHostOsInfoAsync(HostInfo hostInfo) {
        hostInfo.setOsInfoStatus("loading");
        // 立即更新缓存，让前端看到加载状态
        updateHostInfoCache(hostInfo);

        CompletableFuture.runAsync(() -> {
            try {
                // 获取操作系统信息
                OsInfo osInfo = getHostOsInfoInternal(hostInfo);

                // 设置硬件收集状态为collecting，并立即更新缓存
                osInfo.setHardwareCollectionStatus("collecting");
                hostInfo.setOsInfo(osInfo);
                hostInfo.setOsInfoStatus("success");
                updateHostInfoCache(hostInfo);

                // 异步收集硬件信息
                CompletableFuture.runAsync(() -> {
                    try {
                        // 检查是否能获取到会话
                        ClientSession session = getOrCreateSession(hostInfo);
                        if (session != null) {
                            // 开始收集硬件信息
                            collectHardwareInfo(osInfo, session);
                            // 更新收集状态为成功
                            osInfo.setHardwareCollectionStatus("success");
                            hostInfo.setOsInfo(osInfo);
                            updateHostInfoCache(hostInfo);
                            logger.info("硬件信息收集完成: {}", hostInfo.getIp());
                        } else {
                            // 无法获取会话，设置为错误状态
                            osInfo.setHardwareCollectionStatus("error");
                            osInfo.setLastUpdatedItem("会话连接失败");
                            hostInfo.setOsInfo(osInfo);
                            updateHostInfoCache(hostInfo);
                            logger.error("无法获取SSH会话，硬件信息收集失败: {}", hostInfo.getIp());
                        }
                    } catch (Exception e) {
                        logger.error("收集硬件信息时出错: " + e.getMessage(), e);
                        osInfo.setHardwareCollectionStatus("error");
                        osInfo.setLastUpdatedItem("硬件收集出错");
                        hostInfo.setOsInfo(osInfo);
                        updateHostInfoCache(hostInfo);
                    }
                });
            } catch (Exception e) {
                logger.error("获取操作系统信息时出错: {}", e.getMessage(), e);
                hostInfo.setOsInfoStatus("error");
                updateHostInfoCache(hostInfo);
            }
        });
    }

    @Override
    public OsInfo getHostOsInfo(HostInfo hostInfo) {
        // 如果已经有操作系统信息，直接返回
        if (hostInfo.getOsInfo() != null && hostInfo.getOsInfo().isValid()) {
            return hostInfo.getOsInfo();
        }

        // 否则同步获取操作系统信息
        return getHostOsInfoInternal(hostInfo);
    }

    @Override
    public void updateHostInfoCache(HostInfo hostInfo) {
        try {
            // 获取缓存中的主机信息
            Integer clusterId = hostInfo.getClusterId();
            if (clusterId == null) {
                logger.warn("主机 {} 未关联集群ID，无法更新缓存", hostInfo.getIp());
                return;
            }

            String cacheKey = clusterId + Constants.HOST_MAP;
            if (!CacheUtils.constainsKey(cacheKey)) {
                logger.warn("找不到集群 {} 的主机缓存，无法更新", clusterId);
                return;
            }

            Map<String, HostInfo> hostMap = (Map<String, HostInfo>) CacheUtils.get(cacheKey);
            if (hostMap != null) {
                // 更新缓存中的主机信息
                hostMap.put(hostInfo.getIp(), hostInfo);
                CacheUtils.put(cacheKey, hostMap);
                logger.debug("已更新集群 {} 中主机 {} 的缓存信息", clusterId, hostInfo.getIp());
            }
        } catch (Exception e) {
            logger.error("更新主机 {} 的缓存信息时出错: {}", hostInfo.getIp(), e.getMessage(), e);
        }
    }

    /**
     * 内部方法：获取主机操作系统信息
     */
    private OsInfo getHostOsInfoInternal(HostInfo hostInfo) {
        OsInfo osInfo = new OsInfo();
        osInfo.setHostInfo(hostInfo);
        hostInfo.setOsInfo(osInfo);

        ClientSession session = null;
        try {
            session = getOrCreateSession(hostInfo);
            if (session == null) {
                logger.warn("无法创建SSH会话");
                return osInfo;
            }

            // 首先确定操作系统类型（Windows或Linux）
            String osType = detectOperatingSystemType(session);
            logger.info("主机 {} 的操作系统类型为: {}", hostInfo.getIp(), osType);

            // 使用工厂获取相应的操作系统信息收集器
            IOsInfoCollector collector = osInfoCollectorFactory.getCollector(osType);
            if (collector != null) {
                return collector.collectOsInfo(hostInfo, session, osInfo);
            } else {
                logger.warn("未找到适用于{}操作系统的信息收集器", osType);
                return osInfo;
            }
        } catch (Exception e) {
            logger.error("获取主机操作系统信息时出错: {}", e.getMessage(), e);
            return osInfo;
        }
    }

    /**
     * 检测操作系统类型（Windows或Linux）
     */
    private String detectOperatingSystemType(ClientSession session) {
        try {
            // 尝试执行Windows命令
            String windowsCheck = MinaUtils.execCmdWithResult(session, "powershell -command \"$env:OS\"");
            if (windowsCheck != null && windowsCheck.toLowerCase().contains("windows")) {
                return "windows";
            }

            // 尝试执行Linux命令
            String linuxCheck = MinaUtils.execCmdWithResult(session, "uname -a");
            if (linuxCheck != null && linuxCheck.toLowerCase().contains("linux")) {
                return "linux";
            }

            // 尝试检查/etc/os-release文件
            String osReleaseCheck = MinaUtils.execCmdWithResult(session, "test -f /etc/os-release && echo 'exists'");
            if (osReleaseCheck != null && osReleaseCheck.trim().equals("exists")) {
                return "linux";
            }

            // 尝试执行lsb_release命令
            String lsbReleaseCheck = MinaUtils.execCmdWithResult(session, "which lsb_release && echo 'exists'");
            if (lsbReleaseCheck != null && lsbReleaseCheck.trim().equals("exists")) {
                return "linux";
            }

            // 默认假设为Linux
            logger.warn("无法确定操作系统类型，默认为Linux");
            return "linux";
        } catch (Exception e) {
            logger.error("检测操作系统类型时出错: {}", e.getMessage(), e);
            return "linux";
        }
    }

    /**
     * 收集硬件信息
     */
    private void collectHardwareInfo(OsInfo osInfo, ClientSession session) {
        // 获取操作系统类型
        String osType = osInfo.getDistributionId().toLowerCase().contains("windows") ? "windows" : "linux";

        // 使用工厂获取相应的操作系统信息收集器
        IOsInfoCollector collector = osInfoCollectorFactory.getCollector(osType);
        if (collector != null) {
            collector.collectHardwareInfo(osInfo, session);
        } else {
            logger.warn("未找到适用于{}操作系统的信息收集器", osType);
            osInfo.setHardwareCollectionStatus("error");
        }
    }

    /**
     * 获取或创建SSH会话
     */
    private ClientSession getOrCreateSession(HostInfo hostInfo) {
        try {
            ClientSession session = null;

            String ip = hostInfo.getIp();
            Integer sshPort = hostInfo.getSshPort();
            String sshUser = hostInfo.getSshUser();
            String sshPassword = hostInfo.getSshPassword();

            if (StringUtils.isBlank(ip) || sshPort == null || StringUtils.isBlank(sshUser)
                    || StringUtils.isBlank(sshPassword)) {
                logger.warn("创建SSH会话失败: 缺少必要的连接参数");
                return null;
            }

            // 使用密码方式连接
            session = MinaUtils.openConnectionWithPassword(hostInfo);

            if (session == null) {
                logger.warn("使用密码连接失败，尝试使用免密方式");
                session = MinaUtils.openConnection(hostInfo);
            }

            return session;
        } catch (Exception e) {
            logger.error("创建SSH会话时出错: {}", e.getMessage(), e);
            return null;
        }
    }
}