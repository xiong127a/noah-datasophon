package com.datasophon.api.service.impl.osinfo;

import com.datasophon.api.service.OsInfoService;
import com.datasophon.api.utils.MinaUtils;
import com.datasophon.api.utils.MinaUtils.CommandResult;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.enums.OsInfoStatusEnum;
import com.datasophon.common.model.HostInfo;
import com.datasophon.common.model.OsInfo;
import org.apache.sshd.client.session.ClientSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * 操作系统信息服务实现类
 * 负责管理主机操作系统信息的获取和缓存
 * 采用分阶段收集策略：
 * - 第一阶段：使用hostInfoExecutor收集主机名和操作系统信息，优先展示给用户
 * - 第二阶段：使用hardwareInfoExecutor收集详细硬件信息，后台处理
 */
@Service
public class OsInfoServiceImpl implements OsInfoService {

    private static final Logger logger = LoggerFactory.getLogger(OsInfoServiceImpl.class);

    @Autowired
    private OsInfoCollectorFactory osInfoCollectorFactory;

    // 线程池配置
    @Autowired
    @Qualifier("osInfoExecutor")
    private ExecutorService hostInfoExecutor;

    @Autowired
    @Qualifier("hardwareInfoExecutor")
    private ExecutorService hardwareInfoExecutor;

    // 队列管理器
    private final HostInfoCollectionQueueManager queueManager;

    // 会话缓存
    private final Map<String, ClientSession> sessionCache = new ConcurrentHashMap<>();

    // 连接锁，用于避免多线程对同一主机的并发连接
    private final ConcurrentMap<String, Object> connectionLocks = new ConcurrentHashMap<>();

    // 初始化
    public OsInfoServiceImpl() {
        this.queueManager = new HostInfoCollectionQueueManager(this);
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
            // 检查是否有可用的处理槽
            int currentProcessing = processingHostCount.get();
            if (currentProcessing >= MAX_CONCURRENT_HOSTS) {
                logger.debug("当前处理中的主机数量已达上限: {}", currentProcessing);
                return;
            }

            // 检查队列是否为空
            if (hostQueue.isEmpty()) {
                logger.debug("主机队列为空，无法开始新的处理");
                return;
            }

            // 计算可以开始处理的主机数量
            int availableSlots = MAX_CONCURRENT_HOSTS - currentProcessing;
            int toProcess = Math.min(availableSlots, hostQueue.size());
            logger.info("开始处理队列中的{}台主机，当前处理中: {}, 最大同时处理: {}",
                    toProcess, currentProcessing, MAX_CONCURRENT_HOSTS);

            // 从队列中取出主机进行处理
            for (int i = 0; i < toProcess; i++) {
                HostInfo hostInfo = hostQueue.poll();
                if (hostInfo == null) {
                    break;
                }

                processingHostCount.incrementAndGet();

                // 使用ExecutorService线程池
                service.hostInfoExecutor.execute(() -> {
                    try {
                        processHost(hostInfo);
                    } catch (Exception e) {
                        logger.error("处理主机信息时发生异常: {}", e.getMessage(), e);
                        hostInfo.setOsInfoStatus(OsInfoStatusEnum.ERROR);
                        hostInfo.setMessage("处理主机信息时发生异常: " + e.getMessage());
                        service.updateHostInfoCache(hostInfo);
                    } finally {
                        // 无论成功失败，都减少处理中的计数
                        processingHostCount.decrementAndGet();
                        // 标记为已完成
                        completedHostCount.incrementAndGet();
                        // 检查是否需要处理下一批
                        startProcessingIfNeeded();
                    }
                });
            }
        }

        /**
         * 检查第二阶段队列的处理情况
         */
        private synchronized void checkPhase2Queue() {
            int currentPhase2Processing = phase2ProcessingCount.get();
            if (currentPhase2Processing >= MAX_CONCURRENT_DETAIL_HOSTS) {
                return;
            }

            synchronized (waitForDetailInfoList) {
                if (waitForDetailInfoList.isEmpty()) {
                    return;
                }

                int availableSlots = MAX_CONCURRENT_DETAIL_HOSTS - currentPhase2Processing;
                int toProcess = Math.min(availableSlots, waitForDetailInfoList.size());

                for (int i = 0; i < toProcess; i++) {
                    if (waitForDetailInfoList.isEmpty()) {
                        break;
                    }

                    HostInfo hostInfo = waitForDetailInfoList.remove(0);
                    phase2ProcessingCount.incrementAndGet();

                    // 使用ExecutorService线程池
                    service.hardwareInfoExecutor.execute(() -> {
                        try {
                            processHostDetailInfo(hostInfo);
                        } catch (Exception e) {
                            logger.error("处理主机详细信息时发生异常: {}", e.getMessage(), e);
                        } finally {
                            phase2ProcessingCount.decrementAndGet();
                            checkPhase2Queue();
                        }
                    });
                }
            }
        }

        /**
         * 处理单个主机的信息收集
         * 这个方法实现第一阶段收集：主机名和基本操作系统信息
         */
        private void processHost(HostInfo hostInfo) {
            if (hostInfo == null) {
                logger.error("无法处理空的主机信息");
                return;
            }

            logger.info("开始收集主机 [{}] 的基本信息", hostInfo.getIp());
            hostInfo.setOsInfoStatus(OsInfoStatusEnum.LOADING);
            hostInfo.setMessage("正在收集主机信息...");
            service.updateHostInfoCache(hostInfo);

            try {
                // 第一步：收集主机名（最基本信息）
                collectHostname(hostInfo);

                // 第二步：收集基本操作系统信息
                collectBasicOsInfo(hostInfo);

                // 收集完成后，将主机添加到第二阶段队列
                logger.info("主机 [{}] 基本信息收集完成，添加到详细信息收集队列", hostInfo.getIp());
                synchronized (waitForDetailInfoList) {
                    waitForDetailInfoList.add(hostInfo);
                }
                basicInfoCompletedCount.incrementAndGet();

                // 检查是否开始第二阶段收集
                checkPhase2Queue();

            } catch (Exception e) {
                logger.error("收集主机 [{}] 信息时发生异常: {}", hostInfo.getIp(), e.getMessage(), e);
                hostInfo.setOsInfoStatus(OsInfoStatusEnum.ERROR);
                hostInfo.setMessage("收集信息失败: " + e.getMessage());
                service.updateHostInfoCache(hostInfo);
            }
        }

        /**
         * 收集主机名信息
         */
        private void collectHostname(HostInfo hostInfo) {
            logger.info("收集主机 [{}] 的主机名信息", hostInfo.getIp());

            // 设置状态为收集中
            hostInfo.setHostnameStatus(OsInfoStatusEnum.LOADING);
            hostInfo.setMessage("正在收集主机名...");
            service.updateHostInfoCache(hostInfo);

            ClientSession session = null;
            try {
                // 创建SSH会话
                session = connectToHost(hostInfo);
                if (session == null) {
                    logger.error("无法为主机 [{}] 创建SSH会话", hostInfo.getIp());
                    hostInfo.setHostnameStatus(OsInfoStatusEnum.ERROR);
                    hostInfo.setMessage("无法建立SSH连接");
                    service.updateHostInfoCache(hostInfo);
                    return;
                }

                // 执行命令获取主机名
                try {
                    // 执行命令获取主机名
                    CommandResult hostnameResult = MinaUtils.execCmdWithResultObject(session, "hostname");
                    if (!hostnameResult.isSuccess()) {
                        throw new Exception("获取主机名失败: " + hostnameResult.getError());
                    }

                    String hostname = hostnameResult.getOutput().trim();
                    if (hostname.isEmpty()) {
                        throw new Exception("获取的主机名为空");
                    }

                    // 获取FQDN
                    CommandResult fqdnResult = MinaUtils.execCmdWithResultObject(session, "hostname -f");
                    String fqdn = hostname; // 默认使用主机名作为FQDN
                    if (fqdnResult.isSuccess() && !fqdnResult.getOutput().trim().isEmpty()) {
                        fqdn = fqdnResult.getOutput().trim();
                    }

                    // 更新OsInfo
                    OsInfo osInfo = hostInfo.getOsInfo();
                    if (osInfo == null) {
                        osInfo = new OsInfo();
                        hostInfo.setOsInfo(osInfo);
                    }

                    osInfo.setHostname(hostname);
                    osInfo.setFqdn(fqdn);

                    // 更新状态
                    hostInfo.setHostnameStatus(OsInfoStatusEnum.SUCCESS);
                    hostInfo.setMessage("主机名收集成功");
                    service.updateHostInfoCache(hostInfo);

                    logger.info("主机 [{}] 的主机名收集完成: {}", hostInfo.getIp(), hostname);
                } catch (Exception e) {
                    logger.error("收集主机 [{}] 主机名时出错: {}", hostInfo.getIp(), e.getMessage(), e);
                    hostInfo.setHostnameStatus(OsInfoStatusEnum.ERROR);
                    hostInfo.setMessage("主机名收集失败: " + e.getMessage());
                    service.updateHostInfoCache(hostInfo);
                }
            } catch (Exception e) {
                logger.error("收集主机 [{}] 主机名过程中出现异常: {}", hostInfo.getIp(), e.getMessage(), e);
                hostInfo.setHostnameStatus(OsInfoStatusEnum.ERROR);
                hostInfo.setMessage("主机名收集失败: " + e.getMessage());
                service.updateHostInfoCache(hostInfo);
            } finally {
                // 关闭会话
                service.closeSession(session);
            }
        }

        /**
         * 收集基本操作系统信息
         */
        private void collectBasicOsInfo(HostInfo hostInfo) {
            logger.info("收集主机 [{}] 的基本操作系统信息", hostInfo.getIp());

            // 设置状态为收集中
            hostInfo.setOsInfoStatus(OsInfoStatusEnum.LOADING);
            service.updateHostInfoCache(hostInfo);

            ClientSession session = null;
            try {
                // 创建SSH会话，设置15秒连接超时
                session = connectToHost(hostInfo);
                if (session == null) {
                    logger.error("无法为主机 [{}] 创建SSH会话", hostInfo.getIp());
                    hostInfo.setOsInfoStatus(OsInfoStatusEnum.ERROR);
                    hostInfo.setMessage("无法建立SSH连接");
                    service.updateHostInfoCache(hostInfo);
                    return;
                }

                // 初始化或获取OsInfo
                OsInfo osInfo = hostInfo.getOsInfo();
                if (osInfo == null) {
                    osInfo = new OsInfo();
                    hostInfo.setOsInfo(osInfo);
                }

                logger.info("开始收集Linux操作系统信息");

                // 使用Linux系统收集器处理
                IOsInfoCollector linuxCollector = service.osInfoCollectorFactory.getCollector("linux");
                if (linuxCollector != null) {
                    // 收集基本操作系统信息，设置回调更新缓存
                    linuxCollector.collectOsInfo(hostInfo, session, osInfo, h -> service.updateHostInfoCache(h));
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
                service.updateHostInfoCache(hostInfo);

                logger.info("主机 [{}] 操作系统信息收集完成: {}, {}",
                        hostInfo.getIp(), osInfo.getDistribution(), osInfo.getFullName());

            } catch (Exception e) {
                logger.error("收集主机 [{}] 操作系统信息时出错: {}", hostInfo.getIp(), e.getMessage(), e);
                hostInfo.setOsInfoStatus(OsInfoStatusEnum.ERROR);
                hostInfo.setMessage("操作系统信息收集失败: " + e.getMessage());
                service.updateHostInfoCache(hostInfo);
            } finally {
                // 关闭会话
                service.closeSession(session);
            }
        }

        /**
         * 处理主机详细硬件信息收集
         * 这个方法实现第二阶段收集：CPU、内存、磁盘等详细硬件信息
         */
        private void processHostDetailInfo(HostInfo hostInfo) {
            if (hostInfo == null) {
                logger.error("无法处理空的主机信息");
                return;
            }

            logger.info("开始收集主机 [{}] 的详细硬件信息", hostInfo.getIp());

            try {
                // 收集CPU信息
                collectCpuInfo(hostInfo);

                // 收集内存信息
                collectMemoryInfo(hostInfo);

                // 收集磁盘信息
                collectDiskInfo(hostInfo);

                // 收集网络信息
                collectNetworkInfo(hostInfo);

                // 收集GPU信息（可选）
                collectGpuInfo(hostInfo);

                // 收集DNS信息
                collectDnsInfo(hostInfo);

                // 收集hosts文件信息
                collectHostsFileInfo(hostInfo);

                logger.info("主机 [{}] 详细硬件信息收集完成", hostInfo.getIp());

            } catch (Exception e) {
                logger.error("收集主机 [{}] 详细硬件信息时发生异常: {}", hostInfo.getIp(), e.getMessage(), e);
            }
        }

        /**
         * 连接到主机并获取会话
         */
        private ClientSession connectToHost(HostInfo hostInfo) {
            if (hostInfo == null) {
                logger.error("无法连接：主机信息为空");
                return null;
            }

            String hostKey = hostInfo.getIp() + ":" + hostInfo.getSshPort();

            // 使用外部类中定义的connectionLocks
            Object lock = service.connectionLocks.computeIfAbsent(hostKey, k -> new Object());

            // 对同一主机的连接进行同步，避免并发连接冲突
            synchronized (lock) {
                try {
                    // 从会话缓存中获取现有会话
                    ClientSession session = sessionCache.get(hostKey);

                    // 检查现有会话是否有效
                    if (session != null && session.isOpen()) {
                        try {
                            // 简单测试会话是否可用，使用更快的命令
                            CommandResult testResult = MinaUtils.execCmdWithResultObject(session,
                                    "echo connection_test");
                            if (testResult.isSuccess() && testResult.getOutput().trim().contains("connection_test")) {
                                logger.debug("复用主机 {} 的现有SSH连接", hostInfo.getIp());
                                return session;
                            } else {
                                logger.warn("会话测试失败，开始重新连接");
                            }
                        } catch (Exception e) {
                            logger.warn("会话测试失败，开始重新连接: {}", e.getMessage());
                        }

                        // 关闭无效会话
                        try {
                            service.closeSession(session);
                        } catch (Exception e) {
                            logger.debug("关闭失效连接时发生异常: {}", e.getMessage());
                        } finally {
                            sessionCache.remove(hostKey);
                        }
                    }

                    // 创建新会话，设置合理的超时时间
                    logger.info("创建主机 {} 的新SSH连接", hostInfo.getIp());
                    session = MinaUtils.openConnectionWithPassword(hostInfo);

                    if (session != null) {
                        // 不设置额外属性，避免版本兼容性问题
                        sessionCache.put(hostKey, session);
                        logger.info("成功创建主机 {} 的SSH连接", hostInfo.getIp());
                    }
                    return session;
                } catch (Exception e) {
                    logger.error("连接主机[{}]时发生异常: {}", hostInfo.getIp(), e.getMessage(), e);
                    return null;
                }
            }
        }

        /**
         * 带超时的命令执行
         * 
         * @param session        SSH会话
         * @param command        要执行的命令
         * @param timeoutSeconds 超时时间（秒）
         * @return 命令执行结果
         */
        private CommandResult execCommandWithTimeout(ClientSession session, String command, int timeoutSeconds) {
            if (session == null) {
                return CommandResult.exception(command, "SSH会话为空");
            }

            try {
                // 使用带超时参数的命令执行方法
                return MinaUtils.execCmdWithResultObject(session, command, timeoutSeconds);
            } catch (Exception e) {
                logger.error("执行命令出错: {}, 错误: {}", command, e.getMessage(), e);
                return CommandResult.exception(command, e.getMessage());
            }
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
            session = queueManager.connectToHost(hostInfo);
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
                // 执行命令获取主机名
                CommandResult hostnameResult = MinaUtils.execCmdWithResultObject(session, "hostname");
                if (!hostnameResult.isSuccess()) {
                    throw new Exception("获取主机名失败: " + hostnameResult.getError());
                }

                String hostname = hostnameResult.getOutput().trim();
                if (hostname.isEmpty()) {
                    throw new Exception("获取的主机名为空");
                }

                // 获取FQDN
                CommandResult fqdnResult = MinaUtils.execCmdWithResultObject(session, "hostname -f");
                String fqdn = hostname; // 默认使用主机名作为FQDN
                if (fqdnResult.isSuccess() && !fqdnResult.getOutput().trim().isEmpty()) {
                    fqdn = fqdnResult.getOutput().trim();
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
            session = queueManager.connectToHost(hostInfo);
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
            session = queueManager.connectToHost(hostInfo);
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
            session = queueManager.connectToHost(hostInfo);
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
            session = queueManager.connectToHost(hostInfo);
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
            session = queueManager.connectToHost(hostInfo);
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
            session = queueManager.connectToHost(hostInfo);
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
            session = queueManager.connectToHost(hostInfo);
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
            session = queueManager.connectToHost(hostInfo);
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
