package com.datasophon.api.service.checker.common;

import com.datasophon.api.utils.MinaUtils;
import com.datasophon.common.model.HostInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ClientChannel;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.session.ClientSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.nio.charset.StandardCharsets;

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

    // 定时任务执行间隔（默认值）
    private long connectionCleanupIntervalMs = TimeUnit.SECONDS.toMillis(60); // 默认60秒

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

    // 默认超时时间（30秒）
    private static final long DEFAULT_TIMEOUT = 30000;

    /**
     * 默认构造方法
     */
    public SshConnectionPoolManager() {
        // 无参构造函数，依赖通过@Autowired注入
    }

    @PostConstruct
    public void init() {
        log.info("初始化SSH连接池管理器...");
        // 将定时任务标志设置为已停用
        scheduledTasksEnabled.set(false);
        log.info("SSH连接池管理器初始化完成，定时任务默认关闭");
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

        // 检查必要的Map对象是否初始化
        if (connectionLocks == null || hostConnectionPool == null || connectionLastAccessTime == null) {
            log.error("连接相关的Map对象未初始化，无法创建或获取连接");
            return null;
        }

        String hostKey = hostInfo.getIp() + ":" + hostInfo.getSshPort();

        // 增加总请求计数
        if (hostCacheRequests != null) {
            long requests = hostCacheRequests.getOrDefault(hostKey, 0L) + 1;
            hostCacheRequests.put(hostKey, requests);
        }

        // 获取连接锁，确保同一主机的连接操作串行化
        Object lock = connectionLocks.computeIfAbsent(hostKey, k -> new Object());

        synchronized (lock) {
            ClientSession session = hostConnectionPool.get(hostKey);

            // 检查连接是否存在且有效
            if (session != null) {
                try {
                    // 检查连接是否仍然可用
                    if (session.isOpen()) {
                        // 尝试发送一个无害的命令来验证连接是否真正有效
                        CommandResult testResult = execCommand(session, "echo connection_test");
                        if (testResult != null && testResult.isSuccess()
                                && testResult.getOutput().trim().contains("connection_test")) {
                            log.debug("复用主机 {} 的现有SSH连接 (健康检查通过)", hostInfo.getIp());
                            // 更新最后访问时间
                            connectionLastAccessTime.put(hostKey, System.currentTimeMillis());

                            // 增加缓存命中计数
                            if (hostCacheHits != null) {
                                long hits = hostCacheHits.getOrDefault(hostKey, 0L) + 1;
                                hostCacheHits.put(hostKey, hits);
                            }

                            return session;
                        } else {
                            log.warn("主机 {} 的SSH连接健康检查失败，将创建新连接", hostInfo.getIp());
                        }
                    } else {
                        log.info("主机 {} 的SSH连接已关闭，将创建新连接", hostInfo.getIp());
                    }
                } catch (Exception e) {
                    log.warn("测试SSH连接时发生异常: {}", e.getMessage());
                }

                // 关闭无效连接
                try {
                    session.close();
                } catch (Exception e) {
                    log.debug("关闭失效连接时发生异常: {}", e.getMessage());
                } finally {
                    hostConnectionPool.remove(hostKey);
                }
            }

            // 创建新连接
            try {
                log.info("创建主机 {} 的新SSH连接", hostInfo.getIp());
                session = MinaUtils.openConnection(hostInfo);

                if (session != null) {
                    hostConnectionPool.put(hostKey, session);

                    // 设置初始访问时间
                    connectionLastAccessTime.put(hostKey, System.currentTimeMillis());

                    log.info("成功创建主机 {} 的SSH连接", hostInfo.getIp());
                }
                return session;
            } catch (Exception e) {
                log.error("建立SSH连接失败: {}", e.getMessage(), e);
                return null;
            }
        }
    }

    /**
     * 异步获取或创建SSH连接
     * 
     * @param hostInfo 主机信息
     * @return 包含SSH会话的CompletableFuture，如果创建失败则返回包含null的CompletableFuture
     */
    public CompletableFuture<ClientSession> getOrCreateConnectionAsync(HostInfo hostInfo) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return getOrCreateConnection(hostInfo);
            } catch (Exception e) {
                log.error("异步创建SSH连接时发生异常: {}", e.getMessage(), e);
                return null;
            }
        }, checkExecutor);
    }

    /**
     * 执行命令并按命令类型选择合适的执行器
     *
     * @param session              SSH会话
     * @param command              要执行的命令
     * @param hardwareInfoExecutor 硬件信息执行器
     * @param osInfoExecutor       操作系统信息执行器
     * @param defaultExecutor      默认执行器
     * @return 命令执行结果的Future
     */
    public CompletableFuture<CommandResult> execCommandByType(ClientSession session, String command,
            ExecutorService hardwareInfoExecutor,
            ExecutorService osInfoExecutor,
            ExecutorService defaultExecutor) {
        // 根据命令内容选择合适的执行器
        if (command.contains("dmidecode") || command.contains("lspci") ||
                command.contains("lscpu") || command.contains("free") ||
                command.contains("fdisk") || command.contains("df")) {
            // 硬件信息相关命令
            return execHardwareInfoCommandAsync(session, command, hardwareInfoExecutor);
        } else if (command.contains("uname") || command.contains("cat /etc") ||
                command.contains("cat /proc") || command.contains("hostname")) {
            // 操作系统信息相关命令
            return execOsInfoCommandAsync(session, command, osInfoExecutor);
        } else {
            // 默认命令
            return execCommandAsync(session, command, defaultExecutor);
        }
    }

    /**
     * 异步执行硬件信息相关命令
     *
     * @param session  SSH会话
     * @param command  要执行的命令
     * @param executor 执行器
     * @return 命令执行结果的Future
     */
    public CompletableFuture<CommandResult> execHardwareInfoCommandAsync(ClientSession session, String command,
            ExecutorService executor) {
        if (session == null) {
            CompletableFuture<CommandResult> future = new CompletableFuture<>();
            future.complete(new CommandResult("", "无法创建SSH连接", -1));
            return future;
        }

        return execCommandAsync(session, command, executor);
    }

    /**
     * 异步执行操作系统信息相关命令
     *
     * @param session  SSH会话
     * @param command  要执行的命令
     * @param executor 执行器
     * @return 命令执行结果的Future
     */
    public CompletableFuture<CommandResult> execOsInfoCommandAsync(ClientSession session, String command,
            ExecutorService executor) {
        if (session == null) {
            CompletableFuture<CommandResult> future = new CompletableFuture<>();
            future.complete(new CommandResult("", "无法创建SSH连接", -1));
            return future;
        }

        return execCommandAsync(session, command, executor);
    }

    /**
     * 异步执行命令
     *
     * @param session  SSH会话
     * @param command  要执行的命令
     * @param executor 执行器
     * @return 命令执行结果的Future
     */
    public CompletableFuture<CommandResult> execCommandAsync(ClientSession session, String command,
            ExecutorService executor) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return execCommand(session, command);
            } catch (Exception e) {
                log.error("执行命令时出错: {}", command, e);
                return new CommandResult("", e.getMessage(), -1);
            }
        }, executor);
    }

    /**
     * 直接执行命令
     * 
     * @param session SSH会话
     * @param command 要执行的命令
     * @return 命令执行结果
     */
    public CommandResult execCommand(ClientSession session, String command) {
        try {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            ByteArrayOutputStream errStream = new ByteArrayOutputStream();

            ClientChannel channel = session.createExecChannel(command);
            channel.setOut(outStream);
            channel.setErr(errStream);

            // 打开通道并等待完成
            channel.open().verify(DEFAULT_TIMEOUT);
            channel.waitFor(EnumSet.of(ClientChannelEvent.CLOSED), DEFAULT_TIMEOUT);

            // 获取退出状态
            Integer exitStatus = channel.getExitStatus();

            // 关闭通道
            channel.close(false);

            String output = outStream.toString(StandardCharsets.UTF_8.name());
            String error = errStream.toString(StandardCharsets.UTF_8.name());

            // 清理资源
            outStream.close();
            errStream.close();

            return new CommandResult(output, error, exitStatus != null ? exitStatus : -1);
        } catch (Exception e) {
            log.error("执行SSH命令时发生错误: {}", command, e);
            return new CommandResult("", e.getMessage(), -1);
        }
    }

    /**
     * 定期清理不活跃连接
     */
    @Scheduled(fixedDelay = 60000) // 每分钟执行一次
    public void cleanupConnections() {
        if (scheduledTasksEnabled == null || !scheduledTasksEnabled.get()) {
            log.debug("定时任务已禁用，跳过执行cleanupConnections()");
            return;
        }

        if (hostConnectionPool == null || hostConnectionPool.isEmpty()) {
            log.debug("连接池为空，跳过清理");
            return;
        }

        // 使用checkExecutor异步执行连接清理
        CompletableFuture.runAsync(() -> {
            try {
                int closedCount = 0;
                int idleClosedCount = 0;
                log.info("开始清理不活跃SSH连接...");

                long currentTime = System.currentTimeMillis();
                long idleTimeout = TimeUnit.MINUTES.toMillis(1); // 1分钟没有使用的连接将被关闭

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
                        Long lastAccess = connectionLastAccessTime != null ? connectionLastAccessTime.get(key) : null;
                        if (lastAccess != null && (currentTime - lastAccess) > idleTimeout) {
                            try {
                                log.info("关闭空闲超时的连接: {}, 空闲时长: {}分钟",
                                        key, (currentTime - lastAccess) / 60000);
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
                        if (connectionLastAccessTime != null) {
                            connectionLastAccessTime.remove(key);
                        }
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
        if (hostCacheRequests == null || hostCacheHits == null) {
            return 0;
        }

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

        if (lastConnectionCleanupTime > 0) {
            status.setLastCleanupTime(dateFormat.format(new java.util.Date(lastConnectionCleanupTime)));
        } else {
            status.setLastCleanupTime("未执行");
        }

        return status;
    }
}