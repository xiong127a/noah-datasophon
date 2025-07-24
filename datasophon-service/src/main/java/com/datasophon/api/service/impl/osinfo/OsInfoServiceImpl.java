package com.datasophon.api.service.impl.osinfo;

import com.datasophon.api.service.OsInfoService;
import com.datasophon.api.service.checker.common.CommandResult;
import com.datasophon.api.service.checker.common.SshConnectionPoolManager;
import com.datasophon.api.utils.MinaUtils;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.enums.OsInfoStatusEnum;
import com.datasophon.common.model.HostInfo;
import com.datasophon.common.model.OsInfo;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.apache.sshd.client.session.ClientSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
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

    private OsInfoCollectorFactory osInfoCollectorFactory;

    private SshConnectionPoolManager sshConnectionPoolManager;

    // 线程池配置
    private ExecutorService hostInfoExecutor;

    @Autowired
    private ExecutorService hardwareInfoExecutor;

    // 队列管理器
    private HostInfoCollectionQueueManager queueManager;

    // 会话缓存
    private Map<String, ClientSession> sessionCache = new ConcurrentHashMap<>();

    // 会话最后使用时间
    private Map<String, Long> sessionLastUsedTime = new ConcurrentHashMap<>();

    // 硬件信息缓存
    private Map<String, OsInfo> hardwareInfoCache = new ConcurrentHashMap<>();

    // 硬件信息上次收集时间
    private Map<String, Long> hardwareInfoLastCollectTime = new ConcurrentHashMap<>();

    // 硬件信息缓存有效期(毫秒) - 5分钟
    private static final long HARDWARE_INFO_CACHE_TTL = 300_000;

    // 会话最大空闲时间(毫秒) - 2分钟
    private static final long SESSION_MAX_IDLE_TIME = 120_000;

    // 清理线程
    private ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor();

    // 连接锁，用于避免多线程对同一主机的并发连接
    @Autowired
    private ConcurrentMap<String, Object> connectionLocks = new ConcurrentHashMap<>();

    // 每个主机的最大连接数
    private ConcurrentMap<String, AtomicInteger> hostConnectionCounter = new ConcurrentHashMap<>();

    // 每个主机的最大连接数
    private static final int MAX_CONNECTIONS_PER_HOST = 2;

    // 初始化


    @PostConstruct
    public void init() {
        logger.info("初始化OsInfoService...");

        // 启动清理任务
        startCleanupTasks();

        // 启动超时检查任务
        startTimeoutCheckTask();

        logger.info("OsInfoService初始化完成");
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
     * 启动超时检查任务
     */
    private void startTimeoutCheckTask() {
        cleanupExecutor.scheduleAtFixedRate(() -> {
            try {
                queueManager.checkTimeouts();
            } catch (Exception e) {
                logger.error("执行超时检查时发生异常: {}", e.getMessage(), e);
            }
        }, 10, 10, TimeUnit.SECONDS); // 每10秒检查一次超时

        logger.info("主机处理超时检查任务已启动，间隔: 10秒");
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

        private OsInfoServiceImpl service;

        // 替换为优先级队列和等待列表

        private PriorityQueue<PriorityHostInfo> priorityHostQueue = new PriorityQueue<>();

        private List<PriorityHostInfo> waitingList = new ArrayList<>();

        // 保留原始列表用于按序查找

        private List<HostInfo> sortedHostList = new ArrayList<>();


        private AtomicInteger processingHostCount = new AtomicInteger(0);

        // 增加并行度
        private static final int MAX_CONCURRENT_HOSTS = 10;


        private AtomicInteger totalHostCount = new AtomicInteger(0);

        private AtomicInteger completedHostCount = new AtomicInteger(0);

        private AtomicInteger basicInfoCompletedCount = new AtomicInteger(0);


        private List<HostInfo> waitForDetailInfoList = new ArrayList<>();

        private AtomicInteger phase2ProcessingCount = new AtomicInteger(0);
        private static final int MAX_CONCURRENT_DETAIL_HOSTS = 10;

        // 主机超时设置 - 缩短到15秒
        private static final long HOST_TIMEOUT = 15000L; // 15秒超时

        // 慢速主机记录

        private Map<String, Integer> slowHostMap = new ConcurrentHashMap<>();

        /**
         * 包装HostInfo并添加优先级信息
         */
        @Getter
        private static class PriorityHostInfo implements Comparable<PriorityHostInfo> {

            private HostInfo hostInfo;

            private int priority; // 低数字 = 高优先级

            private long addTime;
            @Setter
            private long processStartTime = 0;

            public PriorityHostInfo(HostInfo hostInfo, int priority) {
                this.hostInfo = hostInfo;
                this.priority = priority;
                this.addTime = System.currentTimeMillis();
            }

            public boolean isTimeout() {
                if (processStartTime == 0) {
                    return false;
                }
                return (System.currentTimeMillis() - processStartTime) > HOST_TIMEOUT;
            }

            @Override
            public int compareTo(PriorityHostInfo other) {
                // 优先级排序 (低数字 = 高优先级)
                return Integer.compare(this.priority, other.priority);
            }
        }

        public HostInfoCollectionQueueManager(OsInfoServiceImpl service) {
            this.service = service;
        }

        public synchronized void resetCounters() {
            priorityHostQueue.clear();
            waitingList.clear();
            sortedHostList.clear();
            processingHostCount.set(0);
            totalHostCount.set(0);
            completedHostCount.set(0);
            basicInfoCompletedCount.set(0);
            waitForDetailInfoList.clear();
            phase2ProcessingCount.set(0);
            logger.info("HostInfoCollectionQueueManager计数器已重置");
        }

        public synchronized void addHostToQueue(HostInfo hostInfo, Consumer<HostInfo> processor) {
            if (hostInfo == null) {
                logger.error("无法添加空的主机信息到队列");
                return;
            }

            // 第一个主机添加时，重置计数器
            if (totalHostCount.get() == 0) {
                logger.info("第一个主机添加到队列，重置所有计数器");
                resetCounters();
            }

            // 确保状态重置
            hostInfo.setHostnameStatus(OsInfoStatusEnum.LOADING);
            hostInfo.setOsInfoStatus(OsInfoStatusEnum.LOADING);
            hostInfo.setMessage("等待处理...");

            // 记录主机信息并分配优先级
            sortedHostList.add(hostInfo);

            // 检查是否为慢速主机
            int priority = 1; // 默认优先级为1（正常）

            String hostKey = hostInfo.getIp();
            Integer slowCount = slowHostMap.get(hostKey);
            if (slowCount != null && slowCount > 0) {
                // 根据慢速记录调整优先级（优先级递减 = 重要性递减）
                priority = Math.min(5, slowCount + 1);
                logger.info("主机 [{}] 有慢速记录({}次)，设置低优先级: {}",
                        hostInfo.getIp(), slowCount, priority);
            }

            // 添加到优先级队列
            priorityHostQueue.add(new PriorityHostInfo(hostInfo, priority));

            // 更新总数
            totalHostCount.incrementAndGet();

            // 更新缓存
            processor.accept(hostInfo);

            logger.info("已添加主机 [{}] 到队列，当前队列大小: {}，处理中: {}, 优先级: {}",
                    hostInfo.getIp(), priorityHostQueue.size(), processingHostCount.get(), priority);

            // 尝试开始处理
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
            if (priorityHostQueue.isEmpty() && waitingList.isEmpty()) {
                logger.debug("主机队列为空，无法开始新的处理");
                return;
            }

            // 计算可以开始处理的主机数量
            int availableSlots = MAX_CONCURRENT_HOSTS - currentProcessing;
            int priorityQueueSize = priorityHostQueue.size();
            int waitingListSize = waitingList.size();
            int toProcess = Math.min(availableSlots, priorityQueueSize + waitingListSize);

            logger.info("开始处理队列中的{}台主机，当前处理中: {}, 最大同时处理: {}, 优先队列: {}, 等待队列: {}",
                    toProcess, currentProcessing, MAX_CONCURRENT_HOSTS,
                    priorityQueueSize, waitingListSize);

            // 从队列中取出主机进行处理
            for (int i = 0; i < toProcess; i++) {
                final PriorityHostInfo priorityHostInfo;

                // 首先尝试从优先级队列中获取
                if (!priorityHostQueue.isEmpty()) {
                    priorityHostInfo = priorityHostQueue.poll();
                }
                // 如果优先级队列为空但等待列表不为空，则从等待列表获取
                else if (!waitingList.isEmpty()) {
                    priorityHostInfo = waitingList.removeFirst();
                    logger.info("从等待列表取出主机 [{}] 重新处理",
                            priorityHostInfo.getHostInfo().getIp());
                } else {
                    break;
                }

                final HostInfo hostInfo = priorityHostInfo.getHostInfo();
                priorityHostInfo.setProcessStartTime(System.currentTimeMillis());

                // 先增加处理中的计数
                processingHostCount.incrementAndGet();

                // 使用ExecutorService线程池异步处理主机
                service.hostInfoExecutor.execute(() -> {
                    try {
                        logger.info("开始异步处理主机 [{}] 的信息", hostInfo.getIp());
                        processHost(hostInfo);
                    } catch (Exception e) {
                        logger.error("处理主机信息时发生异常: {}", e.getMessage(), e);
                        hostInfo.setOsInfoStatus(OsInfoStatusEnum.ERROR);
                        hostInfo.setMessage("处理主机信息时发生异常: " + e.getMessage());
                        service.updateHostInfoCache(hostInfo);
                    } finally {
                        // 检查是否超时
                        if (priorityHostInfo.isTimeout()) {
                            String hostKey = hostInfo.getIp();
                            // 记录慢速主机
                            int count = slowHostMap.getOrDefault(hostKey, 0) + 1;
                            slowHostMap.put(hostKey, count);

                            logger.warn("主机 [{}] 处理超时(超过{}秒)，标记为慢速主机，当前记录: {}次",
                                    hostKey, HOST_TIMEOUT / 1000, count);
                        }

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
         * 检查主机处理是否超时，如有必要将其移动到等待列表
         */
        public synchronized void checkTimeouts() {
            // 创建一个临时列表以避免ConcurrentModificationException
            List<PriorityHostInfo> timeoutItems = new ArrayList<>();

            // 遍历所有正在处理的主机，查找超时的
            for (PriorityHostInfo info : priorityHostQueue) {
                if (info.isTimeout()) {
                    timeoutItems.add(info);
                }
            }

            // 处理超时的主机
            for (PriorityHostInfo priorityHostInfo : timeoutItems) {
                HostInfo hostInfo = priorityHostInfo.getHostInfo();
                logger.warn("主机 [{}] 处理超时，移至等待列表", hostInfo.getIp());

                // 创建新的优先级对象（优先级降低）
                int newPriority = priorityHostInfo.getPriority() + 1;
                PriorityHostInfo newPriorityInfo = new PriorityHostInfo(hostInfo, newPriority);

                // 添加到等待列表
                waitingList.add(newPriorityInfo);

                // 从主队列移除
                priorityHostQueue.remove(priorityHostInfo);

                // 记录慢速主机
                String hostKey = hostInfo.getIp();
                int count = slowHostMap.getOrDefault(hostKey, 0) + 1;
                slowHostMap.put(hostKey, count);

                logger.warn("主机 [{}] 处理超时，已降低优先级至 {}，慢速记录: {}次",
                        hostKey, newPriority, count);
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
                // 使用CompletableFuture来并行收集主机名和基本操作系统信息
                CompletableFuture<Boolean> hostnameCollectionFuture = CompletableFuture.supplyAsync(() -> {
                    try {
                        // 第一步：收集主机名（最基本信息）
                        return collectHostname(hostInfo);
                    } catch (Exception e) {
                        logger.error("收集主机 [{}] 的主机名信息时发生异常: {}", hostInfo.getIp(), e.getMessage(), e);
                        return false;
                    }
                }, service.hostInfoExecutor);

                CompletableFuture<Boolean> osInfoCollectionFuture = CompletableFuture.supplyAsync(() -> {
                    try {
                        // 第二步：收集基本操作系统信息
                        return collectBasicOsInfo(hostInfo);
                    } catch (Exception e) {
                        logger.error("收集主机 [{}] 的基本操作系统信息时发生异常: {}", hostInfo.getIp(), e.getMessage(), e);
                        return false;
                    }
                }, service.hostInfoExecutor);

                // 等待两个任务完成
                CompletableFuture.allOf(hostnameCollectionFuture, osInfoCollectionFuture).join();

                // 检查是否有任何一个任务失败
                boolean hostnameSuccess = hostnameCollectionFuture.get();
                boolean osInfoSuccess = osInfoCollectionFuture.get();

                if (hostnameSuccess && osInfoSuccess) {
                    // 收集完成后，将主机添加到第二阶段队列
                    logger.info("主机 [{}] 基本信息收集完成，添加到详细信息收集队列", hostInfo.getIp());
                    synchronized (waitForDetailInfoList) {
                        waitForDetailInfoList.add(hostInfo);
                    }
                    basicInfoCompletedCount.incrementAndGet();

                    // 检查是否开始第二阶段收集
                    checkPhase2Queue();
                } else {
                    logger.warn("主机 [{}] 基本信息收集部分失败，不会进行详细信息收集", hostInfo.getIp());
                    hostInfo.setOsInfoStatus(OsInfoStatusEnum.ERROR);
                    hostInfo.setMessage("收集基本信息部分失败");
                    service.updateHostInfoCache(hostInfo);
                }
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
        private boolean collectHostname(HostInfo hostInfo) {
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
                    return false;
                }

                // 执行命令获取主机名
                CommandResult hostnameResult = MinaUtils.execCmdWithResultObject(session,
                        "hostname");
                if (!hostnameResult.isSuccess()) {
                    throw new Exception("获取主机名失败: " + hostnameResult.getError());
                }

                String hostname = hostnameResult.getOutput().trim();
                if (hostname.isEmpty()) {
                    throw new Exception("获取的主机名为空");
                }

                // 获取FQDN
                CommandResult fqdnResult = MinaUtils
                        .execCmdWithResultObject(session, "hostname -f");
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

                hostInfo.setHostname(hostname);
                hostInfo.setFqdn(fqdn);

                // 更新状态
                hostInfo.setHostnameStatus(OsInfoStatusEnum.SUCCESS);
                hostInfo.setMessage("主机名收集成功");
                service.updateHostInfoCache(hostInfo);

                logger.info("主机 [{}] 的主机名收集完成: {}", hostInfo.getIp(), hostname);
                return true;
            } catch (Exception e) {
                logger.error("收集主机 [{}] 主机名时出错: {}", hostInfo.getIp(), e.getMessage(), e);
                hostInfo.setHostnameStatus(OsInfoStatusEnum.ERROR);
                hostInfo.setMessage("主机名收集失败: " + e.getMessage());
                service.updateHostInfoCache(hostInfo);
                return false;
            } finally {
                // 关闭会话
                service.closeSession(session);
            }
        }

        /**
         * 收集基本操作系统信息
         */
        private boolean collectBasicOsInfo(HostInfo hostInfo) {
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
                    return false;
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
                    linuxCollector.collectOsInfo(hostInfo, session, osInfo, service::updateHostInfoCache);
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
                return true;
            } catch (Exception e) {
                logger.error("收集主机 [{}] 操作系统信息时出错: {}", hostInfo.getIp(), e.getMessage(), e);
                hostInfo.setOsInfoStatus(OsInfoStatusEnum.ERROR);
                hostInfo.setMessage("操作系统信息收集失败: " + e.getMessage());
                service.updateHostInfoCache(hostInfo);
                return false;
            } finally {
                // 关闭会话
                service.closeSession(session);
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

            // 检查是否超过半数主机完成了第一阶段
            int totalHosts = totalHostCount.get();
            int completedBasicInfo = basicInfoCompletedCount.get();
            boolean shouldStartPhase2 = totalHosts > 0 && completedBasicInfo >= totalHosts / 2;

            if (shouldStartPhase2) {
                logger.info("已有{}个主机完成第一阶段收集（总共{}个主机），超过半数，开始处理第二阶段任务",
                        completedBasicInfo, totalHosts);
            } else if (waitForDetailInfoList.isEmpty()) {
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

                    HostInfo hostInfo = waitForDetailInfoList.removeFirst();
                    phase2ProcessingCount.incrementAndGet();

                    // 使用ExecutorService线程池
                    service.hardwareInfoExecutor.execute(() -> {
                        try {
                            logger.info("开始处理主机[{}]的第二阶段详细信息收集", hostInfo.getIp());
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
         * 处理主机详细信息（硬件信息）
         *
         * @param hostInfo 主机信息
         */
        private void processHostDetailInfo(HostInfo hostInfo) {
            if (hostInfo == null) {
                return;
            }

            String hostIp = hostInfo.getIp();
            if (StringUtils.isBlank(hostIp)) {
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
                ClientSession session;

                synchronized (connectionLocks.computeIfAbsent(hostIp, k -> new Object())) {
                    try {
                        logger.info("尝试获取主机 {} 的SSH连接，用于收集硬件信息", hostIp);
                        // 检查是否有现有的有效连接可用
                        session = sessionCache.get(hostIp);

                        // 验证连接是否有效
                        if (!MinaUtils.isSessionValid(session)) {
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
                            session = MinaUtils.openConnectionWithPassword(hostInfo);

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
                        IOsInfoCollector collector = service.osInfoCollectorFactory.getCollector("linux");
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

            // 添加日志显示当前正在处理的主机数量和队列中等待的主机数量
            logger.info("当前正在处理的主机数量: {}, 队列中等待的主机: {}, 正在处理主机: {}",
                    processingHostCount.get(), priorityHostQueue.size() + waitingList.size(), hostIp);

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
                        // 简单测试会话是否可用，减少超时时间到3秒
                        CommandResult testResult = MinaUtils
                                .execCmdWithResultObject(session,
                                        "echo connection_test", 3); // 设置3秒超时
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
                logger.info("创建主机 {} 的新SSH连接，使用15秒连接超时时间", hostInfo.getIp());

                try {
                    // 使用MinaUtils中提供的15秒超时
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
                } catch (Exception e) {
                    logger.error("创建SSH连接失败: {}", e.getMessage());
                    return null;
                }
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
                        CommandResult testResult = MinaUtils
                                .execCmdWithResultObject(session,
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
                CommandResult hostnameResult = MinaUtils.execCmdWithResultObject(session,
                        "hostname");
                if (!hostnameResult.isSuccess()) {
                    throw new Exception("获取主机名失败: " + hostnameResult.getError());
                }

                String hostname = hostnameResult.getOutput().trim();
                if (hostname.isEmpty()) {
                    throw new Exception("获取的主机名为空");
                }

                // 获取FQDN
                CommandResult fqdnResult = MinaUtils
                        .execCmdWithResultObject(session, "hostname -f");
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

                hostInfo.setHostname(hostname);
                hostInfo.setFqdn(fqdn);

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
                        this::updateHostInfoCache);
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
                        this::updateHostInfoCache);
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
                    hostInfo.getOsInfo().setDnsStatus(OsInfoStatusEnum.ERROR);
                }
                hostInfo.setMessage("无法建立SSH连接");
                updateHostInfoCache(hostInfo);
                return;
            }

            // 直接实现hosts文件收集逻辑
            IOsInfoCollector collector = osInfoCollectorFactory.getCollector("linux");
            if (collector != null) {
                collector.collectHostsFileInfo(hostInfo, session, hostInfo.getOsInfo(),
                        this::updateHostInfoCache);
            }
        } catch (Exception e) {
            logger.error("收集hosts文件信息时出错: {}, 错误: {}", hostInfo.getIp(), e.getMessage(), e);
            if (hostInfo.getOsInfo() != null) {
                hostInfo.getOsInfo().setDnsStatus(OsInfoStatusEnum.ERROR);
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

        ClientSession session;
        try {
            // 使用SSH连接池管理器创建或获取连接
            session = sshConnectionPoolManager.getOrCreateConnection(hostInfo);
            if (session == null) {
                logger.error("无法为主机[{}]创建SSH会话", hostInfo.getIp());
                if (hostInfo.getOsInfo() != null) {
                    hostInfo.getOsInfo().setCpuStatus(OsInfoStatusEnum.ERROR);
                }
                hostInfo.setMessage("无法建立SSH连接");
                updateHostInfoCache(hostInfo);
                return;
            }

            // 直接使用IOsInfoCollector接口收集CPU信息
            IOsInfoCollector collector = osInfoCollectorFactory.getCollector("linux");
            if (collector != null) {
                collector.collectCpuInfo(hostInfo, session, hostInfo.getOsInfo(),
                        this::updateHostInfoCache);
            }
        } catch (Exception e) {
            logger.error("收集CPU信息时出错: {}, 错误: {}", hostInfo.getIp(), e.getMessage(), e);
            if (hostInfo.getOsInfo() != null) {
                hostInfo.getOsInfo().setCpuStatus(OsInfoStatusEnum.ERROR);
            }
            hostInfo.setMessage("CPU信息收集失败: " + e.getMessage());
            updateHostInfoCache(hostInfo);
        }
    }

    @Override
    public void collectMemoryInfo(HostInfo hostInfo) {
        if (hostInfo == null) {
            logger.warn("collectMemoryInfo: 主机信息为空");
            return;
        }

        ClientSession session;
        try {
            // 使用SSH连接池管理器创建或获取连接
            session = sshConnectionPoolManager.getOrCreateConnection(hostInfo);
            if (session == null) {
                logger.error("无法为主机[{}]创建SSH会话", hostInfo.getIp());
                if (hostInfo.getOsInfo() != null) {
                    hostInfo.getOsInfo().setMemoryStatus(OsInfoStatusEnum.ERROR);
                }
                hostInfo.setMessage("无法建立SSH连接");
                updateHostInfoCache(hostInfo);
                return;
            }

            // 直接使用IOsInfoCollector接口收集内存信息
            IOsInfoCollector collector = osInfoCollectorFactory.getCollector("linux");
            if (collector != null) {
                collector.collectMemoryInfo(hostInfo, session, hostInfo.getOsInfo(),
                        this::updateHostInfoCache);
            }
        } catch (Exception e) {
            logger.error("收集内存信息时出错: {}, 错误: {}", hostInfo.getIp(), e.getMessage(), e);
            if (hostInfo.getOsInfo() != null) {
                hostInfo.getOsInfo().setMemoryStatus(OsInfoStatusEnum.ERROR);
            }
            hostInfo.setMessage("内存信息收集失败: " + e.getMessage());
            updateHostInfoCache(hostInfo);
        }
    }

    @Override
    public void collectGpuInfo(HostInfo hostInfo) {
        if (hostInfo == null) {
            logger.warn("collectGpuInfo: 主机信息为空");
            return;
        }

        ClientSession session;
        try {
            // 使用SSH连接池管理器创建或获取连接
            session = sshConnectionPoolManager.getOrCreateConnection(hostInfo);
            if (session == null) {
                logger.error("无法为主机[{}]创建SSH会话", hostInfo.getIp());
                if (hostInfo.getOsInfo() != null) {
                    hostInfo.getOsInfo().setGpuStatus(OsInfoStatusEnum.ERROR);
                }
                hostInfo.setMessage("无法建立SSH连接");
                updateHostInfoCache(hostInfo);
                return;
            }

            // 直接使用IOsInfoCollector接口收集GPU信息
            IOsInfoCollector collector = osInfoCollectorFactory.getCollector("linux");
            if (collector != null) {
                collector.collectGpuInfo(hostInfo, session, hostInfo.getOsInfo(),
                        this::updateHostInfoCache);
            }
        } catch (Exception e) {
            logger.error("收集GPU信息时出错: {}, 错误: {}", hostInfo.getIp(), e.getMessage(), e);
            if (hostInfo.getOsInfo() != null) {
                hostInfo.getOsInfo().setGpuStatus(OsInfoStatusEnum.ERROR);
            }
            hostInfo.setMessage("GPU信息收集失败: " + e.getMessage());
            updateHostInfoCache(hostInfo);
        }
    }

    @Override
    public void collectNetworkInfo(HostInfo hostInfo) {
        if (hostInfo == null) {
            logger.warn("collectNetworkInfo: 主机信息为空");
            return;
        }

        ClientSession session;
        try {
            // 使用SSH连接池管理器创建或获取连接
            session = sshConnectionPoolManager.getOrCreateConnection(hostInfo);
            if (session == null) {
                logger.error("无法为主机[{}]创建SSH会话", hostInfo.getIp());
                if (hostInfo.getOsInfo() != null) {
                    hostInfo.getOsInfo().setNetworkStatus(OsInfoStatusEnum.ERROR);
                }
                hostInfo.setMessage("无法建立SSH连接");
                updateHostInfoCache(hostInfo);
                return;
            }

            // 直接使用IOsInfoCollector接口收集网络信息
            IOsInfoCollector collector = osInfoCollectorFactory.getCollector("linux");
            if (collector != null) {
                collector.collectNetworkInfo(hostInfo, session, hostInfo.getOsInfo(),
                        this::updateHostInfoCache);
            }
        } catch (Exception e) {
            logger.error("收集网络信息时出错: {}, 错误: {}", hostInfo.getIp(), e.getMessage(), e);
            if (hostInfo.getOsInfo() != null) {
                hostInfo.getOsInfo().setNetworkStatus(OsInfoStatusEnum.ERROR);
            }
            hostInfo.setMessage("网络信息收集失败: " + e.getMessage());
            updateHostInfoCache(hostInfo);
        }
    }

    @Override
    public void collectSwapInfo(HostInfo hostInfo) {
        if (hostInfo == null) {
            logger.warn("collectSwapInfo: 主机信息为空");
            return;
        }

        logger.info("开始收集交换空间信息: {}", hostInfo.getIp());
        ClientSession session;

        try {
            // 使用SSH连接池管理器创建或获取连接
            session = sshConnectionPoolManager.getOrCreateConnection(hostInfo);
            if (session == null) {
                logger.error("无法为主机[{}]创建SSH会话", hostInfo.getIp());
                if (hostInfo.getOsInfo() != null) {
                    hostInfo.getOsInfo().setSwapStatus(OsInfoStatusEnum.ERROR);
                }
                hostInfo.setMessage("无法建立SSH连接");
                updateHostInfoCache(hostInfo);
                return;
            }

            // 使用收集器收集交换空间信息
            OsInfo osInfo = hostInfo.getOsInfo();
            // 创建更新缓存的回调函数
            IOsInfoCollector.CacheUpdater cacheUpdater = this::updateHostInfoCache;
            // 使用linux收集器
            IOsInfoCollector collector = osInfoCollectorFactory.getCollector("linux");

            if (collector != null) {
                collector.collectSwapInfo(hostInfo, session, osInfo, cacheUpdater);
                logger.info("交换空间信息收集完成: {}", hostInfo.getIp());
            } else {
                logger.error("无法获取合适的系统信息收集器");
                if (osInfo != null) {
                    osInfo.setSwapStatus(OsInfoStatusEnum.ERROR);
                }
                updateHostInfoCache(hostInfo);
            }
        } catch (Exception e) {
            logger.error("收集交换空间信息失败: {}", e.getMessage(), e);
            if (hostInfo.getOsInfo() != null) {
                hostInfo.getOsInfo().setSwapStatus(OsInfoStatusEnum.ERROR);
            }
            hostInfo.setMessage("交换空间信息收集失败: " + e.getMessage());
            updateHostInfoCache(hostInfo);
        }
        // 不再显式关闭连接，由连接池管理器管理连接的生命周期
    }

    @Override
    public void collectDiskInfo(HostInfo hostInfo) {
        if (hostInfo == null) {
            logger.warn("collectDiskInfo: 主机信息为空");
            return;
        }

        ClientSession session;
        try {
            // 使用SSH连接池管理器创建或获取连接
            session = sshConnectionPoolManager.getOrCreateConnection(hostInfo);
            if (session == null) {
                logger.error("无法为主机[{}]创建SSH会话", hostInfo.getIp());
                if (hostInfo.getOsInfo() != null) {
                    hostInfo.getOsInfo().setDiskStatus(OsInfoStatusEnum.ERROR);
                }
                hostInfo.setMessage("无法建立SSH连接");
                updateHostInfoCache(hostInfo);
                return;
            }

            // 直接使用IOsInfoCollector接口收集磁盘信息
            IOsInfoCollector collector = osInfoCollectorFactory.getCollector("linux");
            if (collector != null) {
                collector.collectDiskInfo(hostInfo, session, hostInfo.getOsInfo(),
                        this::updateHostInfoCache);
            }
        } catch (Exception e) {
            logger.error("收集磁盘信息时出错: {}, 错误: {}", hostInfo.getIp(), e.getMessage(), e);
            if (hostInfo.getOsInfo() != null) {
                hostInfo.getOsInfo().setDiskStatus(OsInfoStatusEnum.ERROR);
            }
            hostInfo.setMessage("磁盘信息收集失败: " + e.getMessage());
            updateHostInfoCache(hostInfo);
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

        // 8. 收集交换空间信息
        collectSwapInfo(hostInfo);

        // 9. 收集网络信息
        collectNetworkInfo(hostInfo);

        // 10. 收集GPU信息
        collectGpuInfo(hostInfo);

        // 更新缓存
        updateHostInfoCache(hostInfo);

        logger.info("【第二阶段完成】主机 {} 的详细信息收集完成", hostInfo.getIp());
    }

    @Override
    public synchronized void updateHostInfoCache(HostInfo hostInfo) {
        if (hostInfo == null) {
            return;
        }

        try {
            // 标记状态已更新，以便前端能察觉到变化
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
        // 不再关闭会话，由连接池管理器管理连接的生命周期
        // 只记录日志
        if (session != null) {
            logger.debug("SSH会话由连接池管理器管理，不需要手动关闭");
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
}
