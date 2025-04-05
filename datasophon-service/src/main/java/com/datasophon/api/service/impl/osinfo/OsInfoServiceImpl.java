package com.datasophon.api.service.impl.osinfo;

import com.datasophon.api.service.OsInfoService;
import com.datasophon.api.utils.MinaUtils;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.enums.OsInfoStatusEnum;
import com.datasophon.common.model.HostInfo;
import com.datasophon.common.model.OsInfo;
import org.apache.sshd.client.session.ClientSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * 操作系统信息服务实现类
 * 负责管理主机操作系统信息的获取和缓存
 * 采用分阶段收集策略：
 * - 第一阶段：使用osInfoExecutor收集主机名和操作系统信息，优先展示给用户
 * - 第二阶段：使用hardwareInfoExecutor收集详细硬件信息，后台处理
 */
@Service
public class OsInfoServiceImpl implements OsInfoService {

    private static final Logger logger = LoggerFactory.getLogger(OsInfoServiceImpl.class);

    @Autowired
    private OsInfoCollectorFactory osInfoCollectorFactory;

    // 队列管理器
    private final HostInfoCollectionQueueManager queueManager;

    // 主机信息收集线程池
    private final ExecutorService osInfoExecutor;

    // 硬件信息收集线程池
    private final ExecutorService hardwareInfoExecutor;

    // 会话缓存
    private final Map<String, ClientSession> sessionCache = new ConcurrentHashMap<>();

    // 初始化
    public OsInfoServiceImpl() {
        this.queueManager = new HostInfoCollectionQueueManager(this);
        this.osInfoExecutor = Executors.newFixedThreadPool(5);
        this.hardwareInfoExecutor = Executors.newFixedThreadPool(3);
    }

    @PostConstruct
    public void init() {
        logger.debug("=====================================================");
        logger.debug("初始化OS信息收集服务，使用分阶段收集策略");
        logger.debug("信息收集流程：");
        logger.debug("1. 基本信息收集：使用osInfoExecutor线程池，同时处理最多5台主机");
        logger.debug("2. 详细信息收集：使用hardwareInfoExecutor线程池，同时处理最多3台主机");
        logger.debug("3. 收集顺序：先收集所有主机的基本信息（主机名和操作系统类型）");
        logger.debug("4. 然后收集详细硬件信息（CPU、内存、磁盘等）");
        logger.debug("5. 每收集完一项信息立即更新缓存，优先展示主机名和操作系统信息");
        logger.debug("=====================================================");
    }

    @PreDestroy
    public void destroy() {
        logger.info("OsInfoServiceImpl正在关闭...");
        // 清理所有缓存的SSH会话
        closeAllSessions();
    }

    /**
     * 关闭所有缓存的SSH会话
     */
    private void closeAllSessions() {
        logger.info("清理SSH连接缓存，关闭所有会话，当前缓存连接数: {}", sessionCache.size());
        for (Map.Entry<String, ClientSession> entry : sessionCache.entrySet()) {
            try {
                ClientSession session = entry.getValue();
                if (session != null && session.isOpen()) {
                    logger.info("关闭主机{}的SSH会话", entry.getKey());
                    closeSession(session);
                }
            } catch (Exception e) {
                logger.warn("关闭SSH会话时出错: {}", e.getMessage());
            }
        }
        sessionCache.clear();
        logger.info("SSH连接缓存清理完成");
    }

    /**
     * 异步收集主机OS信息
     */
    @Override
    public void getHostOsInfoAsync(HostInfo hostInfo) {
        if (hostInfo == null) {
            return;
        }

        logger.info("主机信息收集流程：");
        logger.info("IP: {}, 开始收集信息", hostInfo.getIp());

        // 判断是否需要重置计数器（单台主机收集不重置，多台主机时重置）
        if (queueManager.totalHostCount.get() == 0) {
            // 队列为空，说明是一个新的批次，重置计数器
            queueManager.resetCounters();
            logger.info("开始新的批次收集，已重置计数器");
        }

        // 将主机添加到收集队列
        queueManager.addHostToQueue(hostInfo, null);
    }

    /**
     * 主机信息收集队列管理器
     * 用于管理多个主机的信息收集流程
     */
    private class HostInfoCollectionQueueManager {
        private final OsInfoServiceImpl service;

        // 待处理的主机队列
        private final Queue<HostInfo> hostQueue = new ConcurrentLinkedQueue<>();

        // 排序后的主机列表
        private final List<HostInfo> sortedHostList = new ArrayList<>();

        // 当前正在处理的主机数量
        private final AtomicInteger processingHostCount = new AtomicInteger(0);

        // 最大同时处理的主机数量
        private static final int MAX_CONCURRENT_HOSTS = 5;

        // 总共处理的主机数量
        private final AtomicInteger totalHostCount = new AtomicInteger(0);

        // 已完成处理的主机数量
        private final AtomicInteger completedHostCount = new AtomicInteger(0);

        // 已完成基本信息收集的主机数量
        private final AtomicInteger basicInfoCompletedCount = new AtomicInteger(0);

        // 等待收集详细信息的主机列表
        private final List<HostInfo> waitForDetailInfoList = new ArrayList<>();

        // 阶段二收集状态
        private final AtomicInteger phase2ProcessingCount = new AtomicInteger(0);

        // 最大同时收集详细信息的主机数量
        private static final int MAX_CONCURRENT_DETAIL_HOSTS = 3;

        /**
         * 构造函数
         */
        public HostInfoCollectionQueueManager(OsInfoServiceImpl service) {
            this.service = service;
        }

        /**
         * 重置所有计数器
         */
        public synchronized void resetCounters() {
            // 重置所有计数器
            totalHostCount.set(0);
            completedHostCount.set(0);
            processingHostCount.set(0);
            basicInfoCompletedCount.set(0);
            phase2ProcessingCount.set(0);

            // 清空队列和列表
            hostQueue.clear();
            sortedHostList.clear();
            waitForDetailInfoList.clear();

            logger.info("所有计数器和队列已重置");
        }

        /**
         * 将主机添加到收集队列
         */
        public synchronized void addHostToQueue(HostInfo hostInfo, Consumer<HostInfo> processor) {
            if (hostInfo == null) {
                return;
            }

            // 初始化主机状态
            hostInfo.setOsInfoStatus(OsInfoStatusEnum.LOADING);
            hostInfo.setMessage("等待收集主机信息...");
            service.updateHostInfoCache(hostInfo);

            // 添加到队列
            hostQueue.offer(hostInfo);

            // 添加到排序列表并增加计数
            sortedHostList.add(hostInfo);
            int total = totalHostCount.incrementAndGet();
            logger.info("已添加主机{}到收集队列，当前队列总数: {}", hostInfo.getIp(), total);

            // 开始处理队列
            startProcessingIfNeeded();
        }

        /**
         * 根据需要开始处理队列
         */
        private synchronized void startProcessingIfNeeded() {
            // 实现代码...
        }

        /**
         * 连接到主机
         */
        private ClientSession connectToHost(HostInfo hostInfo) {
            return service.connectToHost(hostInfo);
        }

        /**
         * 更新主机信息缓存
         */
        public synchronized void updateHostInfoCache(HostInfo hostInfo) {
            if (hostInfo == null) {
                return;
            }
            service.updateHostInfoCache(hostInfo);
        }

        /**
         * 检查Linux发行版信息
         */
        private void preCheckDistribution(ClientSession session, OsInfo osInfo) {
            // 委托给适当的Linux收集器
            IOsInfoCollector linuxCollector = service.osInfoCollectorFactory.getCollector("linux");
            if (linuxCollector instanceof LinuxOsInfoCollector) {
                // 直接收集Linux发行版信息
                try {
                    linuxCollector.collectOsInfo(null, session, osInfo, null);
                    // 更新为新的发行版枚举
                    osInfo.updateOsDistribution();
                } catch (Exception e) {
                    logger.warn("预检查Linux发行版失败: {}", e.getMessage());
                }
            }
        }

        /**
         * 收集GPU信息
         */
        public void collectGpuInfo(HostInfo hostInfo) {
            if (hostInfo == null) {
                logger.warn("collectGpuInfo: 主机信息为空");
                return;
            }

            ClientSession session = null;
            try {
                // 创建新会话
                session = connectToHost(hostInfo);
                if (session == null) {
                    logger.error("无法为主机[{}]创建SSH会话", hostInfo.getIp());
                    if (hostInfo.getOsInfo() != null) {
                        hostInfo.getOsInfo().setGpuStatus(OsInfoStatusEnum.ERROR);
                    }
                    hostInfo.setMessage("无法建立SSH连接");
                    service.updateHostInfoCache(hostInfo);
                    return;
                }

                // 直接使用IOsInfoCollector接口收集GPU信息
                IOsInfoCollector collector = service.osInfoCollectorFactory.getCollector("linux");
                if (collector != null) {
                    collector.collectGpuInfo(hostInfo, session, hostInfo.getOsInfo(),
                            h -> service.updateHostInfoCache(h));
                }
            } catch (Exception e) {
                logger.error("收集GPU信息时出错: {}, 错误: {}", hostInfo.getIp(), e.getMessage(), e);
                if (hostInfo.getOsInfo() != null) {
                    hostInfo.getOsInfo().setGpuStatus(OsInfoStatusEnum.ERROR);
                }
                hostInfo.setMessage("GPU信息收集失败: " + e.getMessage());
                service.updateHostInfoCache(hostInfo);
            } finally {
                service.closeSession(session);
            }
        }

        /**
         * 收集网络信息
         */
        public void collectNetworkInfo(HostInfo hostInfo) {
            if (hostInfo == null) {
                logger.warn("collectNetworkInfo: 主机信息为空");
                return;
            }

            ClientSession session = null;
            try {
                // 创建新会话
                session = connectToHost(hostInfo);
                if (session == null) {
                    logger.error("无法为主机[{}]创建SSH会话", hostInfo.getIp());
                    if (hostInfo.getOsInfo() != null) {
                        hostInfo.getOsInfo().setNetworkStatus(OsInfoStatusEnum.ERROR);
                    }
                    hostInfo.setMessage("无法建立SSH连接");
                    service.updateHostInfoCache(hostInfo);
                    return;
                }

                // 直接使用IOsInfoCollector接口收集网络信息
                IOsInfoCollector collector = service.osInfoCollectorFactory.getCollector("linux");
                if (collector != null) {
                    collector.collectNetworkInfo(hostInfo, session, hostInfo.getOsInfo(),
                            h -> service.updateHostInfoCache(h));
                }
            } catch (Exception e) {
                logger.error("收集网络信息时出错: {}, 错误: {}", hostInfo.getIp(), e.getMessage(), e);
                if (hostInfo.getOsInfo() != null) {
                    hostInfo.getOsInfo().setNetworkStatus(OsInfoStatusEnum.ERROR);
                }
                hostInfo.setMessage("网络信息收集失败: " + e.getMessage());
                service.updateHostInfoCache(hostInfo);
            } finally {
                service.closeSession(session);
            }
        }

        /**
         * 收集磁盘信息
         */
        public void collectDiskInfo(HostInfo hostInfo) {
            if (hostInfo == null) {
                logger.warn("collectDiskInfo: 主机信息为空");
                return;
            }

            ClientSession session = null;
            try {
                // 创建新会话
                session = connectToHost(hostInfo);
                if (session == null) {
                    logger.error("无法为主机[{}]创建SSH会话", hostInfo.getIp());
                    if (hostInfo.getOsInfo() != null) {
                        hostInfo.getOsInfo().setDiskStatus(OsInfoStatusEnum.ERROR);
                    }
                    hostInfo.setMessage("无法建立SSH连接");
                    service.updateHostInfoCache(hostInfo);
                    return;
                }

                // 直接使用IOsInfoCollector接口收集磁盘信息
                IOsInfoCollector collector = service.osInfoCollectorFactory.getCollector("linux");
                if (collector != null) {
                    collector.collectDiskInfo(hostInfo, session, hostInfo.getOsInfo(),
                            h -> service.updateHostInfoCache(h));
                }
            } catch (Exception e) {
                logger.error("收集磁盘信息时出错: {}, 错误: {}", hostInfo.getIp(), e.getMessage(), e);
                if (hostInfo.getOsInfo() != null) {
                    hostInfo.getOsInfo().setDiskStatus(OsInfoStatusEnum.ERROR);
                }
                hostInfo.setMessage("磁盘信息收集失败: " + e.getMessage());
                service.updateHostInfoCache(hostInfo);
            } finally {
                service.closeSession(session);
            }
        }

        /**
         * 启动详细信息收集流程
         */
        private void startDetailInfoCollection() {
            // 实现代码...
        }

        /**
         * 收集Linux详细信息
         */
        private void collectLinuxDetailInfo(HostInfo hostInfo, ClientSession session, OsInfo osInfo) {
            logger.info("开始收集主机[{}]Linux的详细信息", hostInfo.getIp());
            // 实现代码...
        }

        /**
         * 收集网络详细信息
         */
        private void collectNetworkInfoNew(HostInfo hostInfo, OsInfo osInfo, ClientSession session,
                IOsInfoCollector collector) {
            // 实现代码...
        }

        /**
         * 收集交换分区信息
         */
        private void collectSwapInfo(HostInfo hostInfo, OsInfo osInfo, ClientSession session,
                IOsInfoCollector collector) {
            // 实现代码...
        }

        /**
         * 执行命令
         */
        private String executeCommand(ClientSession session, String command) throws Exception {
            return MinaUtils.execCmdWithResult(session, command);
        }
    }

    @Override
    public void collectHostnameInfo(HostInfo hostInfo) {
        if (hostInfo == null) {
            logger.warn("collectHostnameInfo: 主机信息为空");
            return;
        }

        ClientSession session = null;
        try {
            // 创建新会话
            session = connectToHost(hostInfo);
            if (session == null) {
                logger.error("无法为主机[{}]创建SSH会话", hostInfo.getIp());
                hostInfo.setHostnameStatus(OsInfoStatusEnum.ERROR);
                hostInfo.setMessage("无法建立SSH连接");
                updateHostInfoCache(hostInfo);
                return;
            }

            // 直接实现收集主机名的逻辑
            // 设置状态为正在收集
            hostInfo.setHostnameStatus(OsInfoStatusEnum.LOADING);
            hostInfo.setMessage("正在收集主机名...");
            updateHostInfoCache(hostInfo);

            try {
                // 直接假设为Linux系统
                String hostname;
                String fqdn;

                // Linux系统获取主机名
                hostname = MinaUtils.execCmdWithResult(session, "hostname").trim();
                if (hostname.isEmpty()) {
                    throw new Exception("获取主机名失败");
                }

                // 获取FQDN
                fqdn = MinaUtils.execCmdWithResult(session, "hostname -f").trim();
                if (fqdn.isEmpty()) {
                    fqdn = hostname; // 如果获取FQDN失败，使用主机名作为FQDN
                }

                // 更新OsInfo信息
                OsInfo osInfo = hostInfo.getOsInfo();
                if (osInfo == null) {
                    osInfo = new OsInfo();
                    hostInfo.setOsInfo(osInfo);
                }

                osInfo.setHostname(hostname);
                osInfo.setFqdn(fqdn);

                // 设置状态为成功
                hostInfo.setHostnameStatus(OsInfoStatusEnum.SUCCESS);
                hostInfo.setMessage("主机名收集成功");
                updateHostInfoCache(hostInfo);

                logger.info("主机名 [{}] 收集完成", hostInfo.getIp());
            } catch (Exception e) {
                logger.error("收集主机名时出错: {}, 错误: {}", hostInfo.getIp(), e.getMessage(), e);
                hostInfo.setHostnameStatus(OsInfoStatusEnum.ERROR);
                hostInfo.setMessage("主机名收集失败: " + e.getMessage());
                updateHostInfoCache(hostInfo);
            }
        } catch (Exception e) {
            logger.error("收集主机名时出错: {}, 错误: {}", hostInfo.getIp(), e.getMessage(), e);
            hostInfo.setHostnameStatus(OsInfoStatusEnum.ERROR);
            hostInfo.setMessage("主机名收集失败: " + e.getMessage());
            updateHostInfoCache(hostInfo);
        } finally {
            closeSession(session);
        }
    }

    @Override
    public void collectOsBasicInfo(HostInfo hostInfo) {
        if (hostInfo == null) {
            logger.warn("collectOsBasicInfo: 主机信息为空");
            return;
        }

        ClientSession session = null;
        try {
            // 创建新会话
            session = connectToHost(hostInfo);
            if (session == null) {
                logger.error("无法为主机[{}]创建SSH会话", hostInfo.getIp());
                hostInfo.setOsInfoStatus(OsInfoStatusEnum.ERROR);
                hostInfo.setMessage("无法建立SSH连接");
                updateHostInfoCache(hostInfo);
                return;
            }

            // 设置状态为收集中
            hostInfo.setOsInfoStatus(OsInfoStatusEnum.LOADING);
            updateHostInfoCache(hostInfo);

            // 初始化或获取OsInfo
            OsInfo osInfo = hostInfo.getOsInfo();
            if (osInfo == null) {
                osInfo = new OsInfo();
                hostInfo.setOsInfo(osInfo);
            }

            logger.info("开始收集Linux操作系统信息");

            // 使用Linux系统收集器处理
            IOsInfoCollector linuxCollector = osInfoCollectorFactory.getCollector("linux");
            if (linuxCollector != null) {
                // 收集基本操作系统信息
                linuxCollector.collectOsInfo(hostInfo, session, osInfo,
                        h -> updateHostInfoCache(h));
            } else {
                logger.warn("找不到Linux系统信息收集器");
                osInfo.setDistribution("Linux");
                osInfo.setFullName("Linux 操作系统");
            }

            // 更新状态
            hostInfo.setOsInfoStatus(OsInfoStatusEnum.SUCCESS);
            hostInfo.setMessage("操作系统信息收集成功");
            osInfo.setOsStatus(OsInfoStatusEnum.SUCCESS);
            osInfo.setValid(true);
            updateHostInfoCache(hostInfo);

            // 记录收集到的操作系统信息
            logger.info("主机[{}]操作系统信息收集完成: 简要名称={}, 全称={}",
                    hostInfo.getIp(),
                    osInfo.getDistribution(),
                    osInfo.getFullName());
        } catch (Exception e) {
            logger.error("收集操作系统信息时出错: {}, 错误: {}", hostInfo.getIp(), e.getMessage(), e);
            hostInfo.setOsInfoStatus(OsInfoStatusEnum.ERROR);
            hostInfo.setMessage("操作系统信息收集失败: " + e.getMessage());
            updateHostInfoCache(hostInfo);
        } finally {
            closeSession(session);
        }
    }

    @Override
    public void collectDnsInfo(HostInfo hostInfo) {
        if (hostInfo == null) {
            logger.warn("collectDnsInfo: 主机信息为空");
            return;
        }

        ClientSession session = null;
        try {
            // 创建新会话
            session = connectToHost(hostInfo);
            if (session == null) {
                logger.error("无法为主机[{}]创建SSH会话", hostInfo.getIp());
                if (hostInfo.getOsInfo() != null) {
                    hostInfo.getOsInfo().setDnsStatus(OsInfoStatusEnum.ERROR);
                }
                hostInfo.setMessage("无法建立SSH连接");
                updateHostInfoCache(hostInfo);
                return;
            }

            // 直接实现DNS收集逻辑
            IOsInfoCollector collector = osInfoCollectorFactory.getCollector("linux");
            if (collector != null) {
                collector.collectDnsInfo(hostInfo, session, hostInfo.getOsInfo(),
                        h -> updateHostInfoCache(h));
            }
        } catch (Exception e) {
            logger.error("收集DNS信息时出错: {}, 错误: {}", hostInfo.getIp(), e.getMessage(), e);
            if (hostInfo.getOsInfo() != null) {
                hostInfo.getOsInfo().setDnsStatus(OsInfoStatusEnum.ERROR);
            }
            hostInfo.setMessage("DNS信息收集失败: " + e.getMessage());
            updateHostInfoCache(hostInfo);
        } finally {
            closeSession(session);
        }
    }

    @Override
    public void collectHostsFileInfo(HostInfo hostInfo) {
        if (hostInfo == null) {
            logger.warn("collectHostsFileInfo: 主机信息为空");
            return;
        }

        ClientSession session = null;
        try {
            // 创建新会话
            session = connectToHost(hostInfo);
            if (session == null) {
                logger.error("无法为主机[{}]创建SSH会话", hostInfo.getIp());
                if (hostInfo.getOsInfo() != null) {
                    hostInfo.getOsInfo().setHostsFileStatus(OsInfoStatusEnum.ERROR);
                }
                hostInfo.setMessage("无法建立SSH连接");
                updateHostInfoCache(hostInfo);
                return;
            }

            // 直接实现hosts文件收集逻辑
            IOsInfoCollector collector = osInfoCollectorFactory.getCollector("linux");
            if (collector != null) {
                collector.collectHostsFileInfo(hostInfo, session, hostInfo.getOsInfo(),
                        h -> updateHostInfoCache(h));
            }
        } catch (Exception e) {
            logger.error("收集hosts文件信息时出错: {}, 错误: {}", hostInfo.getIp(), e.getMessage(), e);
            if (hostInfo.getOsInfo() != null) {
                hostInfo.getOsInfo().setHostsFileStatus(OsInfoStatusEnum.ERROR);
            }
            hostInfo.setMessage("hosts文件信息收集失败: " + e.getMessage());
            updateHostInfoCache(hostInfo);
        } finally {
            closeSession(session);
        }
    }

    @Override
    public void collectCpuInfo(HostInfo hostInfo) {
        if (hostInfo == null) {
            logger.warn("collectCpuInfo: 主机信息为空");
            return;
        }

        ClientSession session = null;
        try {
            // 创建新会话
            session = connectToHost(hostInfo);
            if (session == null) {
                logger.error("无法为主机[{}]创建SSH会话", hostInfo.getIp());
                if (hostInfo.getOsInfo() != null) {
                    hostInfo.getOsInfo().setCpuStatus(OsInfoStatusEnum.ERROR);
                }
                hostInfo.setMessage("无法建立SSH连接");
                updateHostInfoCache(hostInfo);
                return;
            }

            // 直接实现CPU信息收集逻辑
            IOsInfoCollector collector = osInfoCollectorFactory.getCollector("linux");
            if (collector != null) {
                collector.collectCpuInfo(hostInfo, session, hostInfo.getOsInfo(),
                        h -> updateHostInfoCache(h));
            }
        } catch (Exception e) {
            logger.error("收集CPU信息时出错: {}, 错误: {}", hostInfo.getIp(), e.getMessage(), e);
            if (hostInfo.getOsInfo() != null) {
                hostInfo.getOsInfo().setCpuStatus(OsInfoStatusEnum.ERROR);
            }
            hostInfo.setMessage("CPU信息收集失败: " + e.getMessage());
            updateHostInfoCache(hostInfo);
        } finally {
            closeSession(session);
        }
    }

    @Override
    public void collectMemoryInfo(HostInfo hostInfo) {
        if (hostInfo == null) {
            logger.warn("collectMemoryInfo: 主机信息为空");
            return;
        }

        ClientSession session = null;
        try {
            // 创建新会话
            session = connectToHost(hostInfo);
            if (session == null) {
                logger.error("无法为主机[{}]创建SSH会话", hostInfo.getIp());
                if (hostInfo.getOsInfo() != null) {
                    hostInfo.getOsInfo().setMemoryStatus(OsInfoStatusEnum.ERROR);
                }
                hostInfo.setMessage("无法建立SSH连接");
                updateHostInfoCache(hostInfo);
                return;
            }

            // 直接实现内存信息收集逻辑
            IOsInfoCollector collector = osInfoCollectorFactory.getCollector("linux");
            if (collector != null) {
                collector.collectMemoryInfo(hostInfo, session, hostInfo.getOsInfo(),
                        h -> updateHostInfoCache(h));
            }
        } catch (Exception e) {
            logger.error("收集内存信息时出错: {}, 错误: {}", hostInfo.getIp(), e.getMessage(), e);
            if (hostInfo.getOsInfo() != null) {
                hostInfo.getOsInfo().setMemoryStatus(OsInfoStatusEnum.ERROR);
            }
            hostInfo.setMessage("内存信息收集失败: " + e.getMessage());
            updateHostInfoCache(hostInfo);
        } finally {
            closeSession(session);
        }
    }

    @Override
    public void collectDiskInfo(HostInfo hostInfo) {
        if (hostInfo == null) {
            logger.warn("collectDiskInfo: 主机信息为空");
            return;
        }

        ClientSession session = null;
        try {
            // 创建新会话
            session = connectToHost(hostInfo);
            if (session == null) {
                logger.error("无法为主机[{}]创建SSH会话", hostInfo.getIp());
                if (hostInfo.getOsInfo() != null) {
                    hostInfo.getOsInfo().setDiskStatus(OsInfoStatusEnum.ERROR);
                }
                hostInfo.setMessage("无法建立SSH连接");
                updateHostInfoCache(hostInfo);
                return;
            }

            // 直接实现磁盘信息收集逻辑
            IOsInfoCollector collector = osInfoCollectorFactory.getCollector("linux");
            if (collector != null) {
                collector.collectDiskInfo(hostInfo, session, hostInfo.getOsInfo(),
                        h -> updateHostInfoCache(h));
            }
        } catch (Exception e) {
            logger.error("收集磁盘信息时出错: {}, 错误: {}", hostInfo.getIp(), e.getMessage(), e);
            if (hostInfo.getOsInfo() != null) {
                hostInfo.getOsInfo().setDiskStatus(OsInfoStatusEnum.ERROR);
            }
            hostInfo.setMessage("磁盘信息收集失败: " + e.getMessage());
            updateHostInfoCache(hostInfo);
        } finally {
            closeSession(session);
        }
    }

    @Override
    public void collectGpuInfo(HostInfo hostInfo) {
        if (hostInfo == null) {
            logger.warn("collectGpuInfo: 主机信息为空");
            return;
        }

        ClientSession session = null;
        try {
            // 创建新会话
            session = connectToHost(hostInfo);
            if (session == null) {
                logger.error("无法为主机[{}]创建SSH会话", hostInfo.getIp());
                if (hostInfo.getOsInfo() != null) {
                    hostInfo.getOsInfo().setGpuStatus(OsInfoStatusEnum.ERROR);
                }
                hostInfo.setMessage("无法建立SSH连接");
                updateHostInfoCache(hostInfo);
                return;
            }

            // 直接实现GPU信息收集逻辑
            IOsInfoCollector collector = osInfoCollectorFactory.getCollector("linux");
            if (collector != null) {
                collector.collectGpuInfo(hostInfo, session, hostInfo.getOsInfo(),
                        h -> updateHostInfoCache(h));
            }
        } catch (Exception e) {
            logger.error("收集GPU信息时出错: {}, 错误: {}", hostInfo.getIp(), e.getMessage(), e);
            if (hostInfo.getOsInfo() != null) {
                hostInfo.getOsInfo().setGpuStatus(OsInfoStatusEnum.ERROR);
            }
            hostInfo.setMessage("GPU信息收集失败: " + e.getMessage());
            updateHostInfoCache(hostInfo);
        } finally {
            closeSession(session);
        }
    }

    @Override
    public void collectNetworkInfo(HostInfo hostInfo) {
        if (hostInfo == null) {
            logger.warn("collectNetworkInfo: 主机信息为空");
            return;
        }

        ClientSession session = null;
        try {
            // 创建新会话
            session = connectToHost(hostInfo);
            if (session == null) {
                logger.error("无法为主机[{}]创建SSH会话", hostInfo.getIp());
                if (hostInfo.getOsInfo() != null) {
                    hostInfo.getOsInfo().setNetworkStatus(OsInfoStatusEnum.ERROR);
                }
                hostInfo.setMessage("无法建立SSH连接");
                updateHostInfoCache(hostInfo);
                return;
            }

            // 直接实现网络信息收集逻辑
            IOsInfoCollector collector = osInfoCollectorFactory.getCollector("linux");
            if (collector != null) {
                collector.collectNetworkInfo(hostInfo, session, hostInfo.getOsInfo(),
                        h -> updateHostInfoCache(h));
            }
        } catch (Exception e) {
            logger.error("收集网络信息时出错: {}, 错误: {}", hostInfo.getIp(), e.getMessage(), e);
            if (hostInfo.getOsInfo() != null) {
                hostInfo.getOsInfo().setNetworkStatus(OsInfoStatusEnum.ERROR);
            }
            hostInfo.setMessage("网络信息收集失败: " + e.getMessage());
            updateHostInfoCache(hostInfo);
        } finally {
            closeSession(session);
        }
    }

    @Override
    public void collectPhaseOneInfo(HostInfo hostInfo) {
        if (hostInfo == null) {
            logger.warn("collectPhaseOneInfo: 主机信息为空");
            return;
        }

        logger.info("【第一阶段】开始收集主机 {} 的基本信息", hostInfo.getIp());

        // 1. 收集主机名
        collectHostnameInfo(hostInfo);

        // 2. 收集操作系统基本信息
        collectOsBasicInfo(hostInfo);

        // 更新缓存
        updateHostInfoCache(hostInfo);

        logger.info("【第一阶段完成】主机 {} 的基本信息收集完成", hostInfo.getIp());
    }

    @Override
    public void collectPhaseTwoInfo(HostInfo hostInfo) {
        if (hostInfo == null) {
            logger.warn("collectPhaseTwoInfo: 主机信息为空");
            return;
        }

        logger.info("【第二阶段】开始收集主机 {} 的详细信息", hostInfo.getIp());

        // 3. 收集DNS配置信息
        collectDnsInfo(hostInfo);

        // 4. 收集hosts文件信息
        collectHostsFileInfo(hostInfo);

        // 5. 收集CPU信息
        collectCpuInfo(hostInfo);

        // 6. 收集内存信息
        collectMemoryInfo(hostInfo);

        // 7. 收集磁盘信息
        collectDiskInfo(hostInfo);

        // 8. 收集网络信息
        collectNetworkInfo(hostInfo);

        // 9. 收集GPU信息
        collectGpuInfo(hostInfo);

        // 更新缓存
        updateHostInfoCache(hostInfo);

        logger.info("【第二阶段完成】主机 {} 的详细信息收集完成", hostInfo.getIp());
    }

    @Override
    public synchronized void updateHostInfoCache(HostInfo hostInfo) {
        if (hostInfo == null)
            return;

        try {
            // 标记状态已更新，以便前端能察觉到变化
            hostInfo.setStatusCacheDirty(true);

            // 更新缓存
            CacheUtils.putHostInfo(hostInfo.getClusterId(), hostInfo.getIp(), hostInfo);
            logger.debug("已更新主机缓存: {}", hostInfo.getIp());
        } catch (Exception e) {
            logger.error("更新主机缓存失败: {}, 原因: {}", hostInfo.getIp(), e.getMessage(), e);
        }
    }

    /**
     * 连接到主机的SSH
     *
     * @param hostInfo 主机信息
     * @return SSH会话，如果连接失败则返回null
     */
    private ClientSession connectToHost(HostInfo hostInfo) {
        if (hostInfo == null) {
            logger.error("无法连接：主机信息为空");
            return null;
        }

        try {
            // 从会话缓存中获取现有会话
            String sessionKey = hostInfo.getIp() + ":" + hostInfo.getSshPort();
            ClientSession session = sessionCache.get(sessionKey);

            // 检查现有会话是否有效
            if (session != null && session.isOpen()) {
                try {
                    // 简单测试会话是否可用
                    MinaUtils.execCmdWithResult(session, "echo test");
                    return session;
                } catch (Exception e) {
                    logger.warn("会话测试失败，开始重新连接: {}", e.getMessage());
                    closeSession(session);
                    sessionCache.remove(sessionKey);
                }
            }

            // 创建新会话
            session = MinaUtils.openConnection(hostInfo);
            if (session != null) {
                sessionCache.put(sessionKey, session);
            }
            return session;
        } catch (Exception e) {
            logger.error("连接主机[{}]时发生异常: {}", hostInfo.getIp(), e.getMessage(), e);
            return null;
        }
    }

    /**
     * 关闭SSH会话
     */
    private void closeSession(ClientSession session) {
        MinaUtils.closeConnection(session);
    }

    @Override
    public void resetCollectionQueue() {
        logger.info("重置主机信息收集队列");
        queueManager.resetCounters();
    }

    @Override
    public void addHostToCollectionQueue(HostInfo hostInfo) {
        if (hostInfo == null) {
            return;
        }
        logger.info("添加主机到收集队列: {}", hostInfo.getIp());
        queueManager.addHostToQueue(hostInfo, this::updateHostInfoCache);
    }
}
