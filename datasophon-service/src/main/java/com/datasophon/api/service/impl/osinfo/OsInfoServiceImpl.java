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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
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

    // 会话最后使用时间
    private final Map<String, Long> sessionLastUsedTime = new ConcurrentHashMap<>();

    // 硬件信息缓存
    private final Map<String, OsInfo> hardwareInfoCache = new ConcurrentHashMap<>();

    // 硬件信息上次收集时间
    private final Map<String, Long> hardwareInfoLastCollectTime = new ConcurrentHashMap<>();

    // 硬件信息缓存有效期(毫秒) - 5分钟
    private static final long HARDWARE_INFO_CACHE_TTL = 300_000;

    // 会话最大空闲时间(毫秒) - 2分钟
    private static final long SESSION_MAX_IDLE_TIME = 120_000;

    // 清理线程
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor();

    // 连接锁，用于避免多线程对同一主机的并发连接
    private final ConcurrentMap<String, Object> connectionLocks = new ConcurrentHashMap<>();

    // 每个主机的最大连接数
    private final ConcurrentMap<String, AtomicInteger> hostConnectionCounter = new ConcurrentHashMap<>();

    // 每个主机的最大连接数
    private static final int MAX_CONNECTIONS_PER_HOST = 2;

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

        // 启动定时清理任务
        startCleanupTasks();
    }

    /**
     * 启动定时清理任务
     */
    private void startCleanupTasks() {
        // 会话清理任务 - 每60秒执行一次
        cleanupExecutor.scheduleAtFixedRate(this::cleanupExpiredSessions, 60, 60, TimeUnit.SECONDS);

        // 硬件信息缓存清理任务 - 每5分钟执行一次
        cleanupExecutor.scheduleAtFixedRate(this::cleanupHardwareInfoCache, 5, 5, TimeUnit.MINUTES);

        logger.info("定时清理任务已启动");
    }

    /**
     * 清理过期的SSH会话
     */
    private synchronized void cleanupExpiredSessions() {
        long now = System.currentTimeMillis();
        List<String> keysToRemove = new ArrayList<>();

        logger.debug("开始清理过期SSH会话...");

        for (Map.Entry<String, Long> entry : sessionLastUsedTime.entrySet()) {
            if (now - entry.getValue() > SESSION_MAX_IDLE_TIME) {
                String key = entry.getKey();
                logger.info("会话 {} 已超过最大空闲时间，准备关闭", key);

                ClientSession session = sessionCache.get(key);
                if (session != null) {
                    try {
                        closeSession(session);
                    } catch (Exception e) {
                        logger.debug("关闭过期会话时出错: {}", e.getMessage());
                    }
                }

                keysToRemove.add(key);

                // 重置主机连接计数
                String host = key.split(":")[0];
                AtomicInteger counter = hostConnectionCounter.get(host);
                if (counter != null && counter.get() > 0) {
                    counter.decrementAndGet();
                }
            }
        }

        // 移除过期会话
        for (String key : keysToRemove) {
            sessionCache.remove(key);
            sessionLastUsedTime.remove(key);
            logger.debug("已移除过期会话: {}", key);
        }

        logger.debug("过期SSH会话清理完成，已清理 {} 个会话", keysToRemove.size());
    }

    /**
     * 清理过期的硬件信息缓存
     */
    private synchronized void cleanupHardwareInfoCache() {
        long now = System.currentTimeMillis();
        List<String> keysToRemove = new ArrayList<>();

        logger.debug("开始清理过期硬件信息缓存...");

        for (Map.Entry<String, Long> entry : hardwareInfoLastCollectTime.entrySet()) {
            // 清理超过有效期两倍的缓存，避免频繁清理
            if (now - entry.getValue() > HARDWARE_INFO_CACHE_TTL * 2) {
                keysToRemove.add(entry.getKey());
            }
        }

        // 移除过期缓存
        for (String key : keysToRemove) {
            hardwareInfoCache.remove(key);
            hardwareInfoLastCollectTime.remove(key);
            logger.debug("已移除过期硬件信息缓存: {}", key);
        }

        logger.debug("过期硬件信息缓存清理完成，已清理 {} 个缓存项", keysToRemove.size());
    }

    @PreDestroy
    public void destroy() {
        logger.info("OsInfoServiceImpl正在关闭...");

        // 关闭清理线程
        try {
            cleanupExecutor.shutdown();
            if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow();
            }
            logger.info("清理线程已关闭");
        } catch (Exception e) {
            logger.warn("关闭清理线程时出错: {}", e.getMessage());
            cleanupExecutor.shutdownNow();
        }

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
                            processHostDetailInfo(hostInfo, false);
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
         * 处理主机详细信息（硬件信息）
         * 
         * @param hostInfo      主机信息
         * @param isLongProcess 是否是长时间处理
         */
        private void processHostDetailInfo(HostInfo hostInfo, boolean isLongProcess) {
            if (hostInfo == null) {
                return;
            }

            String hostIp = hostInfo.getIp();
            if (org.apache.commons.lang.StringUtils.isBlank(hostIp)) {
                return;
            }

            // 添加到处理队列并增加计数
            processingHostCount.incrementAndGet();

            try {
                // 检查是否已经有硬件信息缓存
                OsInfo osInfo = hostInfo.getOsInfo();
                boolean isComplete = false;
                boolean isCollecting = false;

                // 使用try-catch包装可能出错的方法调用
                if (osInfo != null) {
                    try {
                        isComplete = osInfo.isHardwareInfoComplete();
                    } catch (Exception e) {
                        // 兼容处理：如果方法不存在，使用状态字段检查
                        isComplete = osInfo.getCpuStatus() == OsInfoStatusEnum.SUCCESS &&
                                osInfo.getMemoryStatus() == OsInfoStatusEnum.SUCCESS &&
                                osInfo.getDiskStatus() == OsInfoStatusEnum.SUCCESS &&
                                osInfo.getNetworkStatus() == OsInfoStatusEnum.SUCCESS;
                    }

                    try {
                        isCollecting = osInfo.isHardwareInfoCollecting();
                    } catch (Exception e) {
                        // 兼容处理：如果方法不存在，使用状态字段检查
                        isCollecting = osInfo.getCpuStatus() == OsInfoStatusEnum.LOADING ||
                                osInfo.getMemoryStatus() == OsInfoStatusEnum.LOADING ||
                                osInfo.getDiskStatus() == OsInfoStatusEnum.LOADING ||
                                osInfo.getNetworkStatus() == OsInfoStatusEnum.LOADING;
                    }
                }

                if (osInfo != null && isComplete) {
                    logger.info("主机 {} 硬件信息已存在且完整，跳过收集", hostIp);
                    return;
                }

                // 检查是否正在收集中
                if (osInfo != null && isCollecting) {
                    logger.info("主机 {} 正在收集硬件信息中，跳过本次收集", hostIp);
                    return;
                }

                // 如果osInfo为空，创建一个新的
                if (osInfo == null) {
                    osInfo = new OsInfo();
                    hostInfo.setOsInfo(osInfo);
                }

                // 更新状态为加载中
                osInfo.setHardwareCollectionStatus(OsInfoStatusEnum.LOADING);
                service.updateHostInfoCache(hostInfo);

                // 获取或创建SSH连接
                ClientSession session = null;
                String lockKey = hostIp;

                synchronized (connectionLocks.computeIfAbsent(lockKey, k -> new Object())) {
                    try {
                        logger.info("尝试获取主机 {} 的SSH连接，用于收集硬件信息", hostIp);
                        // 检查是否有现有的有效连接可用
                        session = sessionCache.get(hostIp);

                        // 验证连接是否有效
                        if (session == null || !MinaUtils.isSessionValid(session)) {
                            if (session != null) {
                                // 如果连接无效，关闭它
                                try {
                                    MinaUtils.closeConnection(session);
                                } catch (Exception e) {
                                    logger.warn("关闭无效SSH连接失败: {}", e.getMessage());
                                }
                                sessionCache.remove(hostIp);
                            }

                            // 创建新连接
                            logger.info("为主机 {} 创建新的SSH连接", hostIp);
                            session = MinaUtils.openConnection(hostInfo);

                            if (session != null) {
                                // 缓存新的有效连接
                                sessionCache.put(hostIp, session);
                            }
                        } else {
                            logger.info("成功复用主机 {} 的现有SSH连接", hostIp);
                        }

                        // 如果无法连接，更新状态并返回
                        if (session == null) {
                            logger.error("无法连接到主机 {}", hostIp);
                            osInfo.setHardwareCollectionStatus(OsInfoStatusEnum.ERROR);
                            hostInfo.setMessage("无法建立SSH连接");
                            service.updateHostInfoCache(hostInfo);
                            return;
                        }

                        final ClientSession finalSession = session;

                        // 收集详细硬件信息
                        IOsInfoCollector collector = osInfoCollectorFactory.getCollector("linux");
                        if (collector != null) {
                            // 成功创建会话，开始收集硬件信息
                            logger.info("开始收集主机 {} 的硬件信息", hostIp);

                            // 定义更新缓存的回调
                            IOsInfoCollector.CacheUpdater cacheUpdater = (hostInfoToUpdate) -> {
                                if (hostInfoToUpdate != null) {
                                    service.updateHostInfoCache(hostInfoToUpdate);
                                }
                            };

                            try {
                                // 执行收集过程
                                collector.collectHardwareInfo(osInfo, finalSession, cacheUpdater);

                                // 更新状态
                                osInfo.setHardwareCollectionStatus(OsInfoStatusEnum.SUCCESS);
                                logger.info("成功收集主机 {} 的硬件信息", hostIp);
                            } catch (Exception e) {
                                logger.error("收集主机 {} 硬件信息时出错: {}", hostIp, e.getMessage(), e);
                                osInfo.setHardwareCollectionStatus(OsInfoStatusEnum.ERROR);
                            }

                            // 再次更新缓存
                            service.updateHostInfoCache(hostInfo);
                        } else {
                            logger.error("无法找到主机 {} 适用的操作系统信息收集器", hostIp);
                            osInfo.setHardwareCollectionStatus(OsInfoStatusEnum.ERROR);
                            hostInfo.setMessage("不支持的操作系统类型");
                            service.updateHostInfoCache(hostInfo);
                        }
                    } catch (Exception e) {
                        logger.error("处理主机 {} 详细信息时出错: {}", hostIp, e.getMessage(), e);
                        osInfo.setHardwareCollectionStatus(OsInfoStatusEnum.ERROR);
                        hostInfo.setMessage("处理详细信息时出错: " + e.getMessage());
                        service.updateHostInfoCache(hostInfo);
                    }
                }
            } finally {
                // 减少处理计数
                processingHostCount.decrementAndGet();
            }
        }

        /**
         * 连接到主机并获取会话
         */
        private ClientSession connectToHost(HostInfo hostInfo) {
            if (hostInfo == null) {
                logger.error("主机信息为空，无法建立连接");
                return null;
            }

            String hostIp = hostInfo.getIp();

            // 获取主机连接计数器
            AtomicInteger connectionCounter = hostConnectionCounter.computeIfAbsent(hostIp, k -> new AtomicInteger(0));

            // 检查是否超过最大连接数
            if (connectionCounter.get() >= MAX_CONNECTIONS_PER_HOST) {
                logger.warn("主机 {} 已达到最大连接数 {}", hostIp, MAX_CONNECTIONS_PER_HOST);

                // 尝试查找现有可用连接
                return findExistingSession(hostInfo);
            }

            // 生成主机会话的唯一标识，格式为: IP:PORT:USER
            final String hostKey = hostIp + ":" + hostInfo.getSshPort() + ":" + hostInfo.getSshUser();

            // 获取或创建主机连接锁
            Object lock = connectionLocks.computeIfAbsent(hostKey, k -> new Object());

            // 使用连接锁进行同步，避免并发创建连接
            synchronized (lock) {
                // 检查缓存中是否已有会话
                ClientSession session = sessionCache.get(hostKey);

                if (session != null) {
                    try {
                        // 简单测试会话是否可用，使用更快的命令
                        CommandResult testResult = MinaUtils.execCmdWithResultObject(session,
                                "echo connection_test", 5); // 设置5秒超时
                        if (testResult.isSuccess() && testResult.getOutput().trim().contains("connection_test")) {
                            logger.debug("复用主机 {} 的现有SSH连接", hostInfo.getIp());
                            // 更新最后使用时间
                            sessionLastUsedTime.put(hostKey, System.currentTimeMillis());
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
                        sessionLastUsedTime.remove(hostKey);
                        // 减少连接计数
                        connectionCounter.decrementAndGet();
                    }
                }

                // 创建新会话，设置合理的超时时间
                logger.info("创建主机 {} 的新SSH连接", hostInfo.getIp());
                session = MinaUtils.openConnectionWithPassword(hostInfo);

                if (session != null) {
                    // 不设置额外属性，避免版本兼容性问题
                    sessionCache.put(hostKey, session);
                    // 更新最后使用时间
                    sessionLastUsedTime.put(hostKey, System.currentTimeMillis());
                    // 增加连接计数
                    connectionCounter.incrementAndGet();
                    logger.info("成功创建主机 {} 的SSH连接", hostInfo.getIp());
                }
                return session;
            }
        }

        /**
         * 查找现有可用会话
         */
        private ClientSession findExistingSession(HostInfo hostInfo) {
            String hostIp = hostInfo.getIp();
            int port = hostInfo.getSshPort();
            String user = hostInfo.getSshUser();

            // 查找以该主机开头的所有会话
            final String keyPrefix = hostIp + ":" + port + ":" + user;

            // 找出最久未使用的会话
            String oldestKey = null;
            long oldestTime = Long.MAX_VALUE;

            for (Map.Entry<String, Long> entry : sessionLastUsedTime.entrySet()) {
                if (entry.getKey().startsWith(keyPrefix) && entry.getValue() < oldestTime) {
                    oldestKey = entry.getKey();
                    oldestTime = entry.getValue();
                }
            }

            if (oldestKey != null) {
                ClientSession session = sessionCache.get(oldestKey);
                if (session != null) {
                    try {
                        // 测试会话是否可用
                        CommandResult testResult = MinaUtils.execCmdWithResultObject(session,
                                "echo reuse_connection", 5); // 设置5秒超时
                        if (testResult.isSuccess() && testResult.getOutput().trim().contains("reuse_connection")) {
                            logger.debug("复用主机 {} 的现有SSH连接（共享会话）", hostInfo.getIp());
                            // 更新最后使用时间
                            sessionLastUsedTime.put(oldestKey, System.currentTimeMillis());
                            return session;
                        }
                    } catch (Exception e) {
                        logger.debug("测试共享会话失败: {}", e.getMessage());
                        // 不关闭会话，留给原始拥有者处理
                    }
                }
            }

            // 找不到可用会话，返回null
            return null;
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
        if (session != null) {
            try {
                MinaUtils.closeConnection(session);
            } catch (Exception e) {
                logger.debug("关闭SSH会话时出错: {}", e.getMessage());
            }
        }
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

    /**
     * 获取适用于特定操作系统的收集器
     * 
     * @param osInfo 操作系统信息
     * @return 收集器实现
     */
    private IOsInfoCollector getOsInfoCollector(OsInfo osInfo) {
        if (osInfo == null) {
            logger.error("无法获取收集器: osInfo为空");
            return null;
        }

        // 默认使用Linux收集器
        return osInfoCollectorFactory.getCollector("linux");
    }
}
