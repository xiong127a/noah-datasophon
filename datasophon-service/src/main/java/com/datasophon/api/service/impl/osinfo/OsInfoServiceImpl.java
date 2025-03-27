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

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * 操作系统信息服务实现类
 * 负责管理主机操作系统信息的获取和缓存
 */
@Service
public class OsInfoServiceImpl implements OsInfoService {

    private static final Logger logger = LoggerFactory.getLogger(OsInfoServiceImpl.class);

    @Autowired
    private OsInfoCollectorFactory osInfoCollectorFactory;

    // 为OS信息收集创建专用的高优先级线程池
    private ExecutorService osInfoExecutor;
    private ExecutorService hardwareInfoExecutor;

    @PostConstruct
    public void init() {
        // 创建一个自定义线程工厂，设置线程为守护线程并设置最高优先级
        ThreadFactory osInfoThreadFactory = r -> {
            Thread t = new Thread(r, "os-info-collector");
            t.setDaemon(true);
            t.setPriority(Thread.MAX_PRIORITY); // 设置最高优先级
            return t;
        };

        ThreadFactory hardwareInfoThreadFactory = r -> {
            Thread t = new Thread(r, "hardware-info-collector");
            t.setDaemon(true);
            t.setPriority(Thread.MAX_PRIORITY); // 设置最高优先级
            return t;
        };

        // 创建固定大小的线程池，专用于OS信息收集
        osInfoExecutor = Executors.newFixedThreadPool(2, osInfoThreadFactory);
        hardwareInfoExecutor = Executors.newFixedThreadPool(4, hardwareInfoThreadFactory);

        logger.info("已初始化OS信息收集高优先级线程池");
    }

    @PreDestroy
    public void destroy() {
        // 程序关闭时，关闭线程池
        if (osInfoExecutor != null) {
            osInfoExecutor.shutdown();
        }
        if (hardwareInfoExecutor != null) {
            hardwareInfoExecutor.shutdown();
        }
        logger.info("已关闭OS信息收集线程池");
    }

    @Override
    public void getHostOsInfoAsync(HostInfo hostInfo) {
        hostInfo.setOsInfoStatus("loading");
        // 立即更新缓存，让前端看到加载状态
        updateHostInfoCache(hostInfo);

        // 使用高优先级线程池执行OS信息收集任务
        CompletableFuture.runAsync(() -> {
            try {
                // 获取操作系统信息
                OsInfo osInfo = getHostOsInfoInternal(hostInfo);

                // 设置硬件收集状态为collecting，并立即更新缓存
                osInfo.setHardwareCollectionStatus("collecting");
                hostInfo.setOsInfo(osInfo);
                hostInfo.setOsInfoStatus("success");
                updateHostInfoCache(hostInfo);

                // 使用专用线程池异步收集硬件信息
                CompletableFuture.runAsync(() -> {
                    try {
                        // 检查是否能获取到会话
                        ClientSession session = getOrCreateSession(hostInfo);
                        if (session != null) {
                            // 开始收集硬件信息
                            // 使用收集器并传入缓存更新函数
                            String osType = osInfo.getDistributionId().toLowerCase().contains("windows") ? "windows"
                                    : "linux";
                            IOsInfoCollector collector = osInfoCollectorFactory.getCollector(osType);
                            if (collector != null) {
                                collector.collectHardwareInfo(osInfo, session, this::updateHostInfoCache);
                                logger.info("硬件信息收集完成: {}", hostInfo.getIp());
                            } else {
                                // 无法获取收集器，设置为错误状态
                                osInfo.setHardwareCollectionStatus("error");
                                osInfo.setLastUpdatedItem("无法获取收集器");
                                hostInfo.setOsInfo(osInfo);
                                updateHostInfoCache(hostInfo);
                                logger.error("无法获取适用于{}的信息收集器: {}", osType, hostInfo.getIp());
                            }
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
                }, hardwareInfoExecutor);
            } catch (Exception e) {
                logger.error("获取操作系统信息时出错: {}", e.getMessage(), e);
                hostInfo.setOsInfoStatus("error");
                updateHostInfoCache(hostInfo);
            }
        }, osInfoExecutor);
    }

    @Override
    public synchronized void updateHostInfoCache(HostInfo hostInfo) {
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
                // 记录更新前的状态
                String ip = hostInfo.getIp();
                String hostname = hostInfo.getHostname();
                String osInfoStatus = hostInfo.getOsInfoStatus();
                OsInfo osInfo = hostInfo.getOsInfo();
                String hardwareStatus = osInfo != null ? osInfo.getHardwareCollectionStatus() : "unknown";
                String lastUpdatedItem = osInfo != null ? osInfo.getLastUpdatedItem() : "unknown";

                logger.debug("准备更新主机{}(IP:{})的缓存: 状态={}, 硬件收集状态={}, 最后更新项={}",
                        hostname, ip, osInfoStatus, hardwareStatus, lastUpdatedItem);

                // 创建主机信息的深拷贝，避免引用问题
                HostInfo hostInfoCopy = deepCopyHostInfo(hostInfo);

                // 更新缓存中的主机信息
                hostMap.put(ip, hostInfoCopy);

                // 立即更新缓存
                boolean updateSuccess = false;
                try {
                    CacheUtils.put(cacheKey, hostMap);
                    updateSuccess = true;
                } catch (Exception e) {
                    logger.error("更新缓存失败: {}", e.getMessage(), e);
                    // 尝试重新更新
                    try {
                        Thread.sleep(50); // 等待一段时间
                        CacheUtils.put(cacheKey, hostMap);
                        updateSuccess = true;
                        logger.info("重试更新缓存成功");
                    } catch (Exception e2) {
                        logger.error("重试更新缓存失败: {}", e2.getMessage(), e2);
                    }
                }

                if (updateSuccess) {
                    logger.info("已成功更新集群 {} 中主机 {} 的缓存信息: 状态={}, 硬件收集状态={}, 最后更新项={}",
                            clusterId, ip, osInfoStatus, hardwareStatus, lastUpdatedItem);
                }
            } else {
                logger.warn("获取到的主机缓存映射为null, 集群ID: {}", clusterId);
            }
        } catch (Exception e) {
            logger.error("更新主机 {} 的缓存信息时出错: {}", hostInfo.getIp(), e.getMessage(), e);
        }
    }

    /**
     * 创建主机信息的深拷贝，避免引用问题
     */
    private HostInfo deepCopyHostInfo(HostInfo source) {
        if (source == null) {
            return null;
        }

        HostInfo copy = new HostInfo();

        // 复制基本属性
        copy.setIp(source.getIp());
        copy.setHostname(source.getHostname());
        copy.setSshUser(source.getSshUser());
        copy.setSshPort(source.getSshPort());
        copy.setSshPassword(source.getSshPassword());
        copy.setManaged(source.isManaged());
        copy.setProgress(source.getProgress());
        copy.setInstallState(source.getInstallState());
        copy.setInstallStateCode(source.getInstallStateCode());
        copy.setMessage(source.getMessage());
        copy.setErrMsg(source.getErrMsg());
        copy.setClusterId(source.getClusterId());
        copy.setCreateTime(source.getCreateTime());
        copy.setCheckResult(source.getCheckResult());
        copy.setCheckItems(source.getCheckItems());
        copy.setOsInfoStatus(source.getOsInfoStatus());

        // 复制OS信息
        if (source.getOsInfo() != null) {
            copy.setOsInfo(source.getOsInfo());
        }

        return copy;
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
                // 使用收集器并传入缓存更新函数
                return collector.collectOsInfo(hostInfo, session, osInfo, this::updateHostInfoCache);
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
     * 获取或创建SSH会话
     */
    private ClientSession getOrCreateSession(HostInfo hostInfo) {
        ClientSession session = null;

        try {
            String ip = hostInfo.getIp();
            Integer sshPort = hostInfo.getSshPort();
            String sshUser = hostInfo.getSshUser();
            String sshPassword = hostInfo.getSshPassword();

            if (StringUtils.isBlank(ip) || sshPort == null || StringUtils.isBlank(sshUser)
                    || StringUtils.isBlank(sshPassword)) {
                logger.warn("创建SSH会话失败: 缺少必要的连接参数");
                return null;
            }

            logger.info("创建到主机 {} 的SSH会话，用户: {}, 端口: {}", ip, sshUser, sshPort);

            // 使用密码方式连接
            try {
                session = MinaUtils.openConnectionWithPassword(hostInfo);
                if (session != null) {
                    logger.info("成功使用密码连接到主机: {}", ip);
                    return session;
                }
            } catch (Exception e) {
                logger.warn("使用密码连接到主机 {} 失败: {}", ip, e.getMessage());
            }

            // 尝试使用免密方式连接
            try {
                logger.info("尝试使用免密方式连接到主机: {}", ip);
                session = MinaUtils.openConnection(hostInfo);
                if (session != null) {
                    logger.info("成功使用免密方式连接到主机: {}", ip);
                    return session;
                }
            } catch (Exception e) {
                logger.warn("使用免密方式连接到主机 {} 失败: {}", ip, e.getMessage());
            }

            logger.error("无法创建到主机 {} 的SSH会话", ip);
            return null;
        } catch (Exception e) {
            logger.error("创建SSH会话时出错: {}", e.getMessage(), e);
            return null;
        }
    }
}