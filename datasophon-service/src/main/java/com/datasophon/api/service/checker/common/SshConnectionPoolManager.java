package com.datasophon.api.service.checker.common;

import com.datasophon.api.utils.MinaUtils;
import com.datasophon.common.model.HostInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.client.session.ClientSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * SSH连接池管理器
 * 负责管理、创建、复用SSH连接
 */
@Component
@Slf4j
public class SshConnectionPoolManager {

    // SSH连接池 - 按主机名缓存SSH连接
    private final Map<String, ClientSession> hostConnectionPool = new ConcurrentHashMap<>();

    // 连接锁，防止并发问题
    private final Map<String, Object> connectionLocks = new ConcurrentHashMap<>();

    // 添加连接池清理改进
    private final Map<String, Long> connectionLastAccessTime = new ConcurrentHashMap<>();

    // 添加缓存命中和总请求计数，用于计算缓存命中率
    private final Map<String, Long> hostCacheHits = new ConcurrentHashMap<>();
    private final Map<String, Long> hostCacheRequests = new ConcurrentHashMap<>();

    // 定时任务启用标志
    private final AtomicBoolean scheduledTasksEnabled = new AtomicBoolean(true);

    // 从配置文件读取配置，如果未配置则使用默认值
    @Value("${datasophon.checker.ssh-connection-pool.idle-timeout-ms:30000}")
    private long idleTimeoutMs;

    @Value("${datasophon.checker.ssh-connection-pool.cleanup-interval-ms:30000}")
    private long connectionCleanupIntervalMs;

    @Value("${datasophon.checker.ssh-connection-pool.max-pool-size:100}")
    private int maxPoolSize;

    @Value("${datasophon.checker.ssh-connection-pool.health-check-command:echo connection_test}")
    private String healthCheckCommand;

    // 上次执行时间
    private volatile long lastConnectionCleanupTime = 0;

    // 定时任务调度器
    @Autowired
    private TaskScheduler taskScheduler;

    // 定时任务的Future
    private ScheduledFuture<?> connectionCleanupTask;

    // 检查执行器 - 用于异步执行清理任务
    @Autowired
    @Qualifier("checkExecutor")
    private ExecutorService checkExecutor;

    // 连接池是否已初始化
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    // 默认超时时间（减少到15秒）
    private static final long DEFAULT_TIMEOUT = 15000;

    // 添加连接超时监控
    private final Map<String, Integer> hostConnectFailCount = new ConcurrentHashMap<>();
    private final Map<String, Long> hostLastFailTime = new ConcurrentHashMap<>();

    // 重试等待时间上限（10分钟）
    private static final long MAX_RETRY_WAIT_TIME = TimeUnit.MINUTES.toMillis(10);
    // 基础重试等待时间（5秒）
    private static final long BASE_RETRY_WAIT_TIME = TimeUnit.SECONDS.toMillis(5);

    /**
     * 默认构造方法
     */
    public SshConnectionPoolManager() {
        // 无参构造函数，依赖通过@Autowired注入
    }

    @PostConstruct
    public void init() {
        log.info("初始化SSH连接池管理器...");
        log.info("SSH连接池配置: 空闲超时={}毫秒, 清理间隔={}毫秒, 最大池大小={}",
                idleTimeoutMs, connectionCleanupIntervalMs, maxPoolSize);

        // 将定时任务标志设置为已停用
        scheduledTasksEnabled.set(true);
        log.info("SSH连接池管理器初始化完成，定时任务默认开启");
    }

    /**
     * 停止定时任务
     */
    public void stopScheduledTasks() {
        // 取消连接清理定时任务
        if (connectionCleanupTask != null && !connectionCleanupTask.isCancelled()) {
            connectionCleanupTask.cancel(false);
            log.info("连接清理定时任务已停止");
        }

        // 设置定时任务标志为已停用
        scheduledTasksEnabled.set(false);
    }

    /**
     * 启动定时任务
     */
    public void startScheduledTasks() {
        if (!scheduledTasksEnabled.get()) {
            scheduledTasksEnabled.set(true);
        }

        // 启动连接清理定时任务
        if (connectionCleanupTask == null || connectionCleanupTask.isCancelled()) {
            connectionCleanupTask = taskScheduler.scheduleAtFixedRate(
                    this::cleanupConnections, connectionCleanupIntervalMs);
            log.info("连接清理定时任务已启动，执行间隔: {}毫秒", connectionCleanupIntervalMs);
        }
    }

    /**
     * 关闭连接池
     */
    @PreDestroy
    public void shutdown() {
        log.info("正在关闭SSH连接池管理器...");

        // 停止定时任务
        stopScheduledTasks();

        // 关闭所有SSH连接
        for (Map.Entry<String, ClientSession> entry : hostConnectionPool.entrySet()) {
            try {
                ClientSession session = entry.getValue();
                if (session != null && session.isOpen()) {
                    session.close();
                    log.info("关闭SSH连接: {}", entry.getKey());
                }
            } catch (Exception e) {
                log.warn("关闭SSH连接时发生异常: {}", e.getMessage());
            }
        }

        // 清空连接池
        hostConnectionPool.clear();
        connectionLocks.clear();

        log.info("SSH连接池管理器已关闭");
    }

    /**
     * 获取或创建SSH连接
     * 
     * @param hostInfo 主机信息
     * @return SSH会话，如果创建失败则返回null
     */
    public ClientSession getOrCreateConnection(HostInfo hostInfo) {
        if (hostInfo == null) {
            log.error("主机信息为空，无法创建连接");
            return null;
        }

        if (hostInfo.getIp() == null || hostInfo.getSshPort() == null) {
            log.error("主机IP或SSH端口为空，无法创建连接: {}", hostInfo.getIp());
            return null;
        }

        // 日志显示当前连接池状态
        log.debug("SSH连接池当前状态: 池大小={}, 主机IP={}",
                hostConnectionPool.size(), hostInfo.getIp());

        // 检查主机是否处于退避期间
        String hostKey = hostInfo.getIp() + ":" + hostInfo.getSshPort();
        if (shouldBackoff(hostKey)) {
            log.warn("主机 {} 处于退避期间，暂不尝试连接", hostInfo.getIp());
            return null;
        }

        // 增加总请求计数
        long requests = hostCacheRequests.getOrDefault(hostKey, 0L) + 1;
        hostCacheRequests.put(hostKey, requests);

        // 获取连接锁，确保同一主机的连接操作串行化
        // 但不要让其他主机的连接请求被同一个锁阻塞
        Object lock = connectionLocks.computeIfAbsent(hostKey, k -> new Object());

        ClientSession session;
        synchronized (lock) {
            session = hostConnectionPool.get(hostKey);

            // 检查连接是否存在且有效
            if (session != null && checkSessionValid(session, hostInfo)) {
                // 更新最后访问时间
                connectionLastAccessTime.put(hostKey, System.currentTimeMillis());

                // 增加缓存命中计数
                long hits = hostCacheHits.getOrDefault(hostKey, 0L) + 1;
                hostCacheHits.put(hostKey, hits);

                // 重置失败计数
                resetFailCounter(hostKey);

                return session;
            }
        }

        // 如果没有有效的会话，则在锁外创建新连接
        // 这样其他主机的连接请求不会被阻塞
        try {
            log.info("创建主机 {} 的新SSH连接，当前连接池大小: {}",
                    hostInfo.getIp(), hostConnectionPool.size());

            // 设置短超时以减少慢主机影响
            long startTime = System.currentTimeMillis();
            session = MinaUtils.openConnectionWithPassword(hostInfo);
            long connectionTime = System.currentTimeMillis() - startTime;

            // 再次获取锁，确保更新连接池的操作是线程安全的
            synchronized (lock) {
                if (session != null) {
                    hostConnectionPool.put(hostKey, session);

                    // 设置初始访问时间
                    connectionLastAccessTime.put(hostKey, System.currentTimeMillis());

                    log.info("成功创建主机 {} 的SSH连接，耗时: {}ms，连接池新大小: {}",
                            hostInfo.getIp(), connectionTime, hostConnectionPool.size());

                    // 连接成功后重置失败计数
                    resetFailCounter(hostKey);

                    // 连接时间超过5秒的警告
                    if (connectionTime > 5000) {
                        log.warn("警告: 主机 {} 的SSH连接建立时间超过5秒 ({}ms)，可能存在网络问题",
                                hostInfo.getIp(), connectionTime);
                    }
                } else {
                    log.error("创建主机 {} 的SSH连接失败，耗时: {}ms", hostInfo.getIp(), connectionTime);

                    // 增加失败计数
                    incrementFailCounter(hostKey);
                }
            }
            return session;
        } catch (Exception e) {
            log.error("创建SSH连接时发生异常: {}", e.getMessage());

            // 增加失败计数
            incrementFailCounter(hostKey);

            return null;
        }
    }

    /**
     * 检查会话是否有效
     * 
     * @param session  SSH会话
     * @param hostInfo 主机信息
     * @return 是否有效
     */
    private boolean checkSessionValid(ClientSession session, HostInfo hostInfo) {
        try {
            // 检查连接是否仍然可用
            if (session.isOpen()) {
                // 尝试发送一个无害的命令来验证连接是否真正有效
                CommandResult testResult = execCommand(session, healthCheckCommand);
                if (testResult != null && testResult.isSuccess()
                        && testResult.getOutput().trim().contains("connection_test")) {
                    log.debug("复用主机 {} 的现有SSH连接 (健康检查通过)", hostInfo.getIp());
                    return true;
                } else {
                    log.warn("主机 {} 的SSH连接健康检查失败，将创建新连接", hostInfo.getIp());
                }
            } else {
                log.info("主机 {} 的SSH连接已关闭，将创建新连接", hostInfo.getIp());
            }

            // 关闭无效连接
            try {
                session.close();
            } catch (Exception e) {
                log.debug("关闭失效连接时发生异常: {}", e.getMessage());
            } finally {
                String hostKey = hostInfo.getIp() + ":" + hostInfo.getSshPort();
                hostConnectionPool.remove(hostKey);
            }
        } catch (Exception e) {
            log.warn("测试SSH连接时发生异常: {}", e.getMessage());
            return false;
        }
        return false;
    }

    /**
     * 重置指定主机的失败计数
     */
    private void resetFailCounter(String hostKey) {
        hostConnectFailCount.remove(hostKey);
        hostLastFailTime.remove(hostKey);
    }

    /**
     * 增加指定主机的失败计数
     */
    private void incrementFailCounter(String hostKey) {
        int failCount = hostConnectFailCount.getOrDefault(hostKey, 0) + 1;
        hostConnectFailCount.put(hostKey, failCount);
        hostLastFailTime.put(hostKey, System.currentTimeMillis());

        long waitTime = calculateBackoffTime(failCount);
        log.warn("主机 {} 连接失败 {} 次，将等待 {} 秒后再次尝试",
                hostKey, failCount, waitTime / 1000);
    }

    /**
     * 计算退避时间（指数增长）
     */
    private long calculateBackoffTime(int failCount) {
        // 使用指数退避策略: 5秒 * 2^(失败次数-1)，上限为10分钟
        long waitTime = BASE_RETRY_WAIT_TIME * (long) Math.pow(2, failCount - 1);
        return Math.min(waitTime, MAX_RETRY_WAIT_TIME);
    }

    /**
     * 判断是否应该退避（不尝试连接）
     */
    private boolean shouldBackoff(String hostKey) {
        Integer failCount = hostConnectFailCount.get(hostKey);
        Long lastFailTime = hostLastFailTime.get(hostKey);

        if (failCount == null || failCount == 0 || lastFailTime == null) {
            return false;
        }

        long waitTime = calculateBackoffTime(failCount);
        long elapsedTime = System.currentTimeMillis() - lastFailTime;

        return elapsedTime < waitTime;
    }

    /**
     * 直接执行命令
     * 
     * @param session SSH会话
     * @param command 要执行的命令
     * @return 命令执行结果
     */
    public CommandResult execCommand(ClientSession session, String command) {
        if (session == null) {
            log.error("无法执行命令，会话为空");
            return new CommandResult("", "SSH会话为空", -1);
        }

        // 获取主机IP地址
        String hostAddress = "unknown";
        try {
            hostAddress = session.getIoSession().getRemoteAddress().toString();
            if (hostAddress.startsWith("/")) {
                hostAddress = hostAddress.substring(1);
            }
            if (hostAddress.contains(":")) {
                hostAddress = hostAddress.substring(0, hostAddress.indexOf(":"));
            }
        } catch (Exception e) {
            // 忽略错误，使用默认值
        }

        // 保存原始线程名称
        Thread currentThread = Thread.currentThread();
        String originalThreadName = currentThread.getName();

        // 设置新的线程名称，包含线程池名称和主机IP
        String threadPoolName = originalThreadName;
        if (threadPoolName.contains("-")) {
            threadPoolName = threadPoolName.substring(0, threadPoolName.lastIndexOf("-"));
        }
        currentThread.setName(threadPoolName + "-" + hostAddress);

        try {
            // 执行命令
            return MinaUtils.execCommand(session, command);
        } finally {
            // 恢复原始线程名称
            currentThread.setName(originalThreadName);
        }
    }

    /**
     * 定期清理不活跃连接
     */
    @Scheduled(fixedDelayString = "${datasophon.checker.ssh-connection-pool.cleanup-interval-ms:30000}")
    public void cleanupConnections() {
        if (!scheduledTasksEnabled.get()) {
            log.debug("定时任务已禁用，跳过执行cleanupConnections()");
            return;
        }

        if (hostConnectionPool.isEmpty()) {
            log.debug("连接池为空，跳过清理");
            return;
        }

        // 检查连接池是否超过最大大小
        if (maxPoolSize > 0 && hostConnectionPool.size() > maxPoolSize) {
            log.warn("连接池大小({})超过最大限制({}), 将进行额外清理",
                    hostConnectionPool.size(), maxPoolSize);
        }

        // 使用checkExecutor异步执行连接清理
        CompletableFuture.runAsync(() -> {
            try {
                int closedCount = 0;
                int idleClosedCount = 0;
                log.info("开始清理不活跃SSH连接...");

                long currentTime = System.currentTimeMillis();
                // 使用配置的空闲超时时间
                long idleTimeout = idleTimeoutMs;

                List<String> keysToRemove = new ArrayList<>();

                for (Map.Entry<String, ClientSession> entry : hostConnectionPool.entrySet()) {
                    String key = entry.getKey();
                    if (key == null) {
                        continue;
                    }

                    try {
                        ClientSession session = entry.getValue();
                        // 检查连接是否有效
                        if (session == null || !session.isOpen()) {
                            keysToRemove.add(key);
                            closedCount++;
                            log.debug("已移除无效连接: {}", key);
                            continue;
                        }

                        // 检查连接是否空闲超时
                        Long lastAccess = connectionLastAccessTime.get(key);
                        if (lastAccess != null && (currentTime - lastAccess) > idleTimeout) {
                            try {
                                log.info("关闭空闲超时的连接: {}, 空闲时长: {}秒",
                                        key, (currentTime - lastAccess) / 1000);
                                session.close();
                                keysToRemove.add(key);
                                idleClosedCount++;
                            } catch (Exception e) {
                                log.warn("关闭空闲连接时发生异常: {}", e.getMessage());
                            }
                        }
                    } catch (Exception e) {
                        log.warn("检查连接时发生异常: {}", e.getMessage());
                    }
                }

                // 移除已关闭的连接
                for (String key : keysToRemove) {
                    if (key != null) {
                        hostConnectionPool.remove(key);
                        // 同时也要移除对应的访问时间记录
                        connectionLastAccessTime.remove(key);
                    }
                }

                lastConnectionCleanupTime = System.currentTimeMillis();
                log.info("SSH连接清理完成，关闭{}个失效连接，{}个空闲连接，当前连接池大小: {}",
                        closedCount, idleClosedCount, hostConnectionPool.size());

                // 日志记录当前缓存命中率
                try {
                    int hitRate = calculateSessionCacheHitRate();
                    log.info("当前SSH会话缓存命中率: {}%", hitRate);
                } catch (Exception e) {
                    log.warn("计算缓存命中率时发生异常: {}", e.getMessage());
                }
            } catch (Exception e) {
                log.error("清理连接池时发生异常: {}", e.getMessage(), e);
            }
        }, checkExecutor);
    }

    /**
     * 计算SSH会话缓存命中率
     * 
     * @return 缓存命中百分比
     */
    public int calculateSessionCacheHitRate() {

        long totalHits = 0;
        long totalRequests = 0;

        for (String hostKey : hostCacheRequests.keySet()) {
            totalHits += hostCacheHits.getOrDefault(hostKey, 0L);
            totalRequests += hostCacheRequests.getOrDefault(hostKey, 0L);
        }

        if (totalRequests == 0) {
            return 0;
        }

        return (int) ((totalHits * 100) / totalRequests);
    }

    /**
     * 将毫秒时间格式化为人类可读的时间间隔
     * 
     * @param ms 毫秒数
     * @return 格式化后的时间间隔
     */
    public String formatTimeInterval(long ms) {
        if (ms <= 0) {
            return "0秒";
        }

        long seconds = ms / 1000;
        if (seconds < 60) {
            return seconds + "秒";
        } else if (seconds < 3600) {
            return (seconds / 60) + "分钟";
        } else if (seconds < 86400) {
            return (seconds / 3600) + "小时";
        } else {
            return (seconds / 86400) + "天";
        }
    }

    /**
     * 手动触发连接清理
     */
    public void manualCleanupConnections() {
        this.cleanupConnections();
    }

    /**
     * 停止连接清理定时任务
     */
    public void stopConnectionCleanup() {
        if (connectionCleanupTask != null && !connectionCleanupTask.isCancelled()) {
            connectionCleanupTask.cancel(false);
            log.info("连接清理定时任务已停止");
        }
    }

    /**
     * 启动连接清理定时任务
     */
    public void startConnectionCleanup() {
        if (connectionCleanupTask == null || connectionCleanupTask.isCancelled()) {
            connectionCleanupTask = taskScheduler.scheduleAtFixedRate(
                    this::cleanupConnections, connectionCleanupIntervalMs);
            log.info("连接清理定时任务已启动，执行间隔: {}毫秒", connectionCleanupIntervalMs);
        }
    }

    /**
     * 更新连接清理定时任务执行间隔
     * 
     * @param intervalMs 执行间隔（毫秒）
     */
    public void updateConnectionCleanupInterval(long intervalMs) {
        if (intervalMs < 1000) { // 最小1秒
            log.warn("连接清理定时任务间隔不能小于1秒，忽略此次更新");
            return;
        }

        this.connectionCleanupIntervalMs = intervalMs;

        if (connectionCleanupTask != null && !connectionCleanupTask.isCancelled()) {
            connectionCleanupTask.cancel(false);
            connectionCleanupTask = taskScheduler.scheduleAtFixedRate(
                    this::cleanupConnections, intervalMs);
            log.info("连接清理定时任务已重新调度，新执行间隔: {}毫秒", intervalMs);
        }
    }

    /**
     * 更新空闲连接超时时间
     * 
     * @param timeoutMs 超时时间（毫秒）
     */
    public void updateIdleTimeout(long timeoutMs) {
        if (timeoutMs < 1000) { // 最小1秒
            log.warn("空闲连接超时时间不能小于1秒，忽略此次更新");
            return;
        }

        this.idleTimeoutMs = timeoutMs;
        log.info("已更新空闲连接超时时间: {}毫秒", timeoutMs);
    }

    /**
     * 更新连接池最大大小
     * 
     * @param size 最大大小
     */
    public void updateMaxPoolSize(int size) {
        if (size <= 0) {
            log.warn("连接池最大大小必须大于0，忽略此次更新");
            return;
        }

        this.maxPoolSize = size;
        log.info("已更新连接池最大大小: {}", size);

        // 如果当前连接池大小超过新的最大大小，触发清理
        if (hostConnectionPool.size() > size) {
            log.info("当前连接池大小({})超过新设置的最大大小({}), 触发清理", hostConnectionPool.size(), size);
            manualCleanupConnections();
        }
    }

    /**
     * 获取SSH连接池的状态信息
     * 
     * @return 连接池状态对象
     */
    public SshConnectionPoolStatus getStatus() {
        SshConnectionPoolStatus status = new SshConnectionPoolStatus();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        status.setEnabled(scheduledTasksEnabled.get());
        status.setConnectionPoolSize(hostConnectionPool.size());
        status.setCleanupTaskActive(connectionCleanupTask != null && !connectionCleanupTask.isCancelled());
        status.setCleanupIntervalMs(this.connectionCleanupIntervalMs);
        status.setCleanupInterval(formatTimeInterval(this.connectionCleanupIntervalMs));
        status.setSessionCacheHitRate(calculateSessionCacheHitRate());
        status.setIdleTimeoutMs(this.idleTimeoutMs);
        status.setIdleTimeout(formatTimeInterval(this.idleTimeoutMs));
        status.setMaxPoolSize(this.maxPoolSize);

        if (lastConnectionCleanupTime > 0) {
            status.setLastCleanupTime(dateFormat.format(new java.util.Date(lastConnectionCleanupTime)));
        } else {
            status.setLastCleanupTime("未执行");
        }

        return status;
    }
}