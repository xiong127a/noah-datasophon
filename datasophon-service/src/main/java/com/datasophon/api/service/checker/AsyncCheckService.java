package com.datasophon.api.service.checker;

import cn.hutool.core.util.StrUtil;
import com.datasophon.api.service.checker.common.ItemCode;
import com.datasophon.api.service.checker.common.LogEntryManager;
import com.datasophon.api.service.checker.common.SshConnectionPoolManager;
import com.datasophon.api.service.checker.config.TaskManager;
import com.datasophon.api.service.checker.core.ItemChecker;
import com.datasophon.api.service.checker.core.ItemCheckerFactory;
import com.datasophon.api.utils.MinaUtils;
import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.model.AsyncServiceStatus;
import com.datasophon.common.model.CheckItem;
import com.datasophon.common.model.HostInfo;
import com.datasophon.common.model.ScheduledTasksStatus;
import com.datasophon.common.utils.Result;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.sshd.client.session.ClientSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 异步检查服务
 * 提供基于Spring异步任务的检查项执行
 * @author 63588
 */
@Service
public class AsyncCheckService {
    private static final Logger logger = LoggerFactory.getLogger(AsyncCheckService.class);

    // 检查器工厂
    @Autowired
    private ItemCheckerFactory itemCheckerFactory;

    // 任务管理器
    @Autowired
    private TaskManager taskManager;

    // 检查任务执行器
    @Autowired
    private ExecutorService checkExecutor;

    // 修复任务执行器
    @Autowired
    private ExecutorService fixExecutor;

    // 操作系统信息获取专用执行器
    @Autowired
    private ExecutorService osInfoExecutor;

    // 硬件信息获取专用执行器
    @Autowired
    private ExecutorService hardwareInfoExecutor;

    // Hosts文件操作专用执行器
    @Autowired
    private ExecutorService hostsFileExecutor;

    // 主机名设置专用执行器
    @Autowired
    private ExecutorService hostnameExecutor;

    // 定时任务启用标志

    private AtomicBoolean scheduledTasksEnabled = new AtomicBoolean(true);

    // 定时任务执行间隔（默认值）
    private long taskCleanupIntervalMs = TimeUnit.SECONDS.toMillis(60); // 默认60秒
    private long connectionCleanupIntervalMs = TimeUnit.SECONDS.toMillis(60); // 默认60秒

    // 上次执行时间
    private volatile long lastTaskCleanupTime = 0;

    // 定时任务调度器
    @Autowired
    private TaskScheduler taskScheduler;

    // 定时任务的Future
    private ScheduledFuture<?> taskCleanupTask;

    // 添加连接池清理改进

    private Map<String, Long> connectionLastAccessTime = new ConcurrentHashMap<>();

    // 添加缓存命中和总请求计数，用于计算缓存命中率

    @Autowired
    private SshConnectionPoolManager sshConnectionPoolManager;


    @PostConstruct
    public void init() {
        logger.info("初始化异步检查服务...");
        // 注释掉自动启动定时任务的代码
        // startScheduledTasks();
        // 将定时任务标志设置为已停用
        scheduledTasksEnabled.set(false);
        logger.info("异步检查服务初始化完成，定时任务默认关闭");
    }

    /**
     * 启动定时任务
     */
    public void startScheduledTasks() {
        if (!scheduledTasksEnabled.get()) {
            scheduledTasksEnabled.set(true);
        }

        TaskScheduler actualScheduler = taskScheduler;
        if (actualScheduler == null) {
            logger.info("TaskScheduler未注入，创建自定义TaskScheduler");
            ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
            scheduler.setPoolSize(2);
            scheduler.setThreadNamePrefix("async-check-scheduler-");
            scheduler.initialize();
            actualScheduler = scheduler;
        }

        // 启动任务清理定时任务（每60秒执行一次）
        if (taskCleanupTask == null || taskCleanupTask.isCancelled()) {
            taskCleanupTask = actualScheduler.scheduleAtFixedRate(
                    this::cleanupTasks, taskCleanupIntervalMs);
            logger.info("任务清理定时任务已启动，执行间隔: {}毫秒", taskCleanupIntervalMs);
        }
    }

    /**
     * 停止定时任务
     */
    public void stopScheduledTasks() {
        // 取消任务清理定时任务
        if (taskCleanupTask != null && !taskCleanupTask.isCancelled()) {
            taskCleanupTask.cancel(false);
            logger.info("任务清理定时任务已停止");
        }

        // 设置定时任务标志为已停用
        scheduledTasksEnabled.set(false);
    }

    /**
     * 禁用定时任务
     */
    public void disableScheduledTasks() {
        if (scheduledTasksEnabled.get()) {
            stopScheduledTasks();
            logger.info("AsyncCheckService定时任务已禁用");
        }
    }

    /**
     * 获取定时任务状态
     * 
     * @return 定时任务状态对象
     */
    public ScheduledTasksStatus getScheduledTasksStatus() {
        ScheduledTasksStatus status = new ScheduledTasksStatus();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        status.setScheduledTasksEnabled(scheduledTasksEnabled.get());
        status.setTaskCleanupActive(taskCleanupTask != null && !taskCleanupTask.isCancelled());

        // 添加定时任务执行间隔
        status.setTaskCleanupIntervalMs(this.taskCleanupIntervalMs);
        status.setConnectionCleanupIntervalMs(this.connectionCleanupIntervalMs);

        // 格式化为人类可读的时间间隔
        status.setTaskCleanupInterval(formatTimeInterval(this.taskCleanupIntervalMs));
        status.setConnectionCleanupInterval(formatTimeInterval(this.connectionCleanupIntervalMs));

        // 格式化时间日期
        if (lastTaskCleanupTime > 0) {
            status.setLastTaskCleanupTime(dateFormat.format(new java.util.Date(lastTaskCleanupTime)));
        } else {
            status.setLastTaskCleanupTime("未执行");
        }

        status.setLastConnectionCleanupTime("未执行");
        return status;
    }

    /**
     * 设置任务清理定时任务执行间隔
     * 
     * @param intervalMs 间隔时间（毫秒）
     * @return 是否设置成功
     */
    public boolean setTaskCleanupInterval(long intervalMs) {
        if (intervalMs < 1000) { // 最小1秒
            logger.warn("任务清理定时任务间隔不能小于1秒，忽略此次更新");
            return false;
        }

        this.taskCleanupIntervalMs = intervalMs;

        // 如果任务已经在运行，则重新调度
        if (taskCleanupTask != null && !taskCleanupTask.isCancelled()) {
            taskCleanupTask.cancel(false);
            TaskScheduler actualScheduler = taskScheduler;
            taskCleanupTask = actualScheduler.scheduleAtFixedRate(
                    this::cleanupTasks, intervalMs);
            logger.info("任务清理定时任务已重新调度，新执行间隔: {}毫秒", intervalMs);
        }

        return true;
    }

    /**
     * 设置连接清理定时任务的执行间隔
     * 
     * @param intervalMs 间隔时间（毫秒）
     * @return 是否设置成功
     */
    public boolean setConnectionCleanupInterval(long intervalMs) {
        if (intervalMs < 1000) { // 最小1秒
            logger.warn("连接清理定时任务间隔不能小于1秒，忽略此次更新");
            return false;
        }

        this.connectionCleanupIntervalMs = intervalMs;

        return true;
    }

    /**
     * 将毫秒时间格式化为人类可读的时间间隔
     * 
     * @param ms 毫秒数
     * @return 格式化后的时间间隔
     */
    private String formatTimeInterval(long ms) {
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
     * 关闭服务
     */
    @PreDestroy
    public void shutdown() {
        logger.info("正在关闭异步检查服务...");

        // 停止定时任务
        stopScheduledTasks();
        logger.info("异步检查服务已关闭");
    }

    /**
     * 定期清理过期任务信息
     * 每小时执行一次
     */
    public void cleanupTasks() {
        if (!scheduledTasksEnabled.get()) {
            logger.debug("定时任务已禁用，跳过执行cleanupTasks()");
            return;
        }

        // 使用checkExecutor异步执行任务清理
        CompletableFuture.runAsync(() -> {
            try {
                int count = taskManager.cleanCompletedTasks(24 * 60 * 60 * 1000); // 24小时
                lastTaskCleanupTime = System.currentTimeMillis();
                logger.info("清理了 {} 个过期任务记录", count);
            } catch (Exception e) {
                logger.error("清理过期任务时发生异常: {}", e.getMessage(), e);
            }
        }, checkExecutor);
    }

    public Result updateHostname(Integer clusterId, String ip, String newHostname, boolean syncHosts) {
        logger.info("更新主机名: clusterId={}, ip={}, newHostname={}, syncHosts={}", clusterId, ip, newHostname, syncHosts);

        // 校验主机名格式
        if (StrUtil.isBlank(newHostname)) {
            return Result.error("主机名不能为空");
        }

        // 主机名格式检查 - 只允许字母、数字、短横线和下划线，不允许特殊字符
        if (!newHostname.matches("^[a-zA-Z0-9_.-]+$")) {
            return Result.error("主机名格式无效，只允许字母、数字、短横线和下划线");
        }

        try {
            // 获取存储在缓存中的主机信息
            Map<String, HostInfo> hostMap = CacheUtils.getHostMap(clusterId + Constants.HOST_MAP);
            if (!hostMap.containsKey(ip)) {
                return Result.error("主机不存在");
            }

            HostInfo hostInfo = hostMap.get(ip);
            String currentHostname = hostInfo.getHostname();

            // 如果新旧主机名相同，则无需修改
            if (newHostname.equals(currentHostname)) {
                logger.info("主机名未发生变化，无需更新: {}", currentHostname);
                return Result.success("主机名未发生变化，无需更新");
            }

            // 建立SSH连接
            ClientSession session = null;
            try {
                session = sshConnectionPoolManager.getOrCreateConnection(hostInfo);
                if (session == null) {
                    return Result.error("无法连接到主机");
                }

                // 1. 检查系统是否有sudo命令
                String checkSudoCmd = "which sudo || echo 'nosudo'";
                String checkSudoResult = MinaUtils.execCmdWithResult(session, checkSudoCmd);
                boolean hasSudo = !checkSudoResult.trim().contains("nosudo");
                logger.info("检查主机是否有sudo命令: {}", hasSudo ? "有" : "没有");

                // 2. 检查系统是否有hostnamectl命令
                String checkHostnamectlCmd = "which hostnamectl || echo 'nohostnamectl'";
                String checkHostnamectlResult = MinaUtils.execCmdWithResult(session, checkHostnamectlCmd);
                boolean hasHostnamectl = !checkHostnamectlResult.trim().contains("nohostnamectl");
                logger.info("检查主机是否有hostnamectl命令: {}", hasHostnamectl ? "有" : "没有");

                // 根据命令可用性决定使用哪种方式设置主机名
                String sudoPrefix = hasSudo ? "sudo " : "";
                String setHostnameCmd;

                if (hasHostnamectl) {
                    // 使用hostnamectl命令设置主机名
                    logger.info("使用hostnamectl命令设置主机名: {}", newHostname);
                    setHostnameCmd = sudoPrefix + "hostnamectl set-hostname " + newHostname;
                } else {
                    // 使用hostname命令设置主机名
                    logger.info("使用hostname命令设置主机名: {}", newHostname);
                    setHostnameCmd = sudoPrefix + "hostname " + newHostname;
                }

                String result = MinaUtils.execCmdWithResult(session, setHostnameCmd);
                logger.info("设置主机名执行结果: {}", result);

                // 更新/etc/hostname文件
                String updateHostnameFileCmd;
                if (hasSudo) {
                    updateHostnameFileCmd = "echo '" + newHostname + "' | " + sudoPrefix + "tee /etc/hostname";
                } else {
                    updateHostnameFileCmd = "echo '" + newHostname + "' > /etc/hostname";
                }
                MinaUtils.execCmdWithResult(session, updateHostnameFileCmd);

                // 兼容旧版本的CentOS/RHEL
                String updateSysConfigCmd = sudoPrefix + "sed -i 's/^HOSTNAME=.*/HOSTNAME=" + newHostname
                        + "/' /etc/sysconfig/network 2>/dev/null || true";
                MinaUtils.execCmdWithResult(session, updateSysConfigCmd);

                // 3. 根据syncHosts参数决定是否更新/etc/hosts文件
                if (syncHosts) {
                    logger.info("同步更新hosts文件中本机的主机名记录: {} -> {}", currentHostname, newHostname);

                    // 先获取当前hosts文件
                    String getHostsCmd = "cat /etc/hosts";
                    String currentHostsContent = MinaUtils.execCmdWithResult(session, getHostsCmd);

                    // 定义标记，用于标识由系统管理的部分
                    String startMark = "### BEGIN DATASOPHON MANAGED HOSTS ###";
                    String endMark = "### END DATASOPHON MANAGED HOSTS ###";

                    // 准备新的hosts文件内容
                    StringBuilder newHostsContent = new StringBuilder();

                    // 检查当前文件是否已经包含系统标记
                    if (currentHostsContent.contains(startMark) && currentHostsContent.contains(endMark)) {
                        // 文件已经包含系统标记，只更新标记内的主机名
                        int startIndex = currentHostsContent.indexOf(startMark);
                        int endIndex = currentHostsContent.indexOf(endMark) + endMark.length();

                        // 获取标记内的内容
                        String managedContent = currentHostsContent.substring(
                                startIndex + startMark.length(),
                                currentHostsContent.indexOf(endMark));

                        // 在标记内的内容中，只修改当前IP对应的主机名
                        StringBuilder updatedManagedContent = new StringBuilder();
                        String[] lines = managedContent.split("\n");
                        for (String line : lines) {
                            line = line.trim();
                            if (line.isEmpty() || line.startsWith("#")) {
                                // 保持注释和空行不变
                                updatedManagedContent.append(line).append("\n");
                            } else if (line.contains(ip)) {
                                // 这一行包含当前IP，需要修改主机名
                                String[] parts = line.split("\\s+");
                                if (parts.length >= 2) {
                                    // 构建新行，保留IP和其他主机名，但将匹配当前主机名的替换为新主机名
                                    StringBuilder newLine = new StringBuilder(parts[0]); // IP地址

                                    for (int i = 1; i < parts.length; i++) {
                                        if (parts[i].equals(currentHostname)) {
                                            newLine.append(" ").append(newHostname);
                                        } else {
                                            newLine.append(" ").append(parts[i]);
                                        }
                                    }
                                    updatedManagedContent.append(newLine).append("\n");
                                } else {
                                    // 如果格式不正确，添加正确的格式
                                    updatedManagedContent.append(ip).append(" ").append(newHostname).append("\n");
                                }
                            } else {
                                // 保持其他行不变
                                updatedManagedContent.append(line).append("\n");
                            }
                        }

                        // 组合新的hosts文件内容
                        newHostsContent.append(currentHostsContent, 0, startIndex);
                        newHostsContent.append(startMark).append("\n");
                        newHostsContent.append(updatedManagedContent);
                        newHostsContent.append(endMark);

                        // 如果标记后还有内容，也保留
                        if (endIndex < currentHostsContent.length()) {
                            newHostsContent.append(currentHostsContent.substring(endIndex));
                        }
                    } else {
                        // 文件不包含系统标记，以原始方式更新主机名引用
                        // 更新hosts文件中的本机记录，将旧主机名替换为新主机名
                        String[] lines = currentHostsContent.split("\n");
                        boolean foundEntry = false;

                        for (String line : lines) {
                            if (line.trim().isEmpty() || line.trim().startsWith("#")) {
                                // 保持注释和空行不变
                                newHostsContent.append(line).append("\n");
                            } else if (line.contains(ip)
                                    || (line.contains("127.0.1.1") && line.contains(currentHostname))) {
                                // 这一行包含当前IP或者包含127.0.1.1和当前主机名，需要修改主机名
                                String updatedLine = line.replace(currentHostname, newHostname);
                                newHostsContent.append(updatedLine).append("\n");
                                foundEntry = true;
                            } else {
                                // 保持其他行不变
                                newHostsContent.append(line).append("\n");
                            }
                        }

                        // 如果没有找到匹配的条目，添加一个新条目
                        if (!foundEntry) {
                            if (!newHostsContent.toString().endsWith("\n")) {
                                newHostsContent.append("\n");
                            }
                            newHostsContent.append("\n# Added by Datasophon\n");
                            newHostsContent.append(ip).append(" ").append(newHostname).append("\n");
                        }
                    }

                    // 创建临时文件
                    String tempFile = "/tmp/hosts_" + System.currentTimeMillis();
                    // 使用单引号包裹并转义内部的单引号
                    String createTempCommand = "cat > " + tempFile + " << 'EOL'\n" +
                            newHostsContent +
                            "\nEOL";
                    MinaUtils.execCmdWithResult(session, createTempCommand);

                    // 使用sudo将临时文件复制到/etc/hosts
                    String updateCommand;
                    if (hasSudo) {
                        updateCommand = sudoPrefix + "cp " + tempFile + " /etc/hosts && " + sudoPrefix
                                + "chmod 644 /etc/hosts && rm " + tempFile;
                    } else {
                        updateCommand = "cp " + tempFile + " /etc/hosts && chmod 644 /etc/hosts && rm " + tempFile;
                    }
                    String updateResult = MinaUtils.execCmdWithResult(session, updateCommand);
                    logger.info("更新hosts文件结果: {}", updateResult);

                    logger.info("hosts文件中的主机名记录已更新: {} -> {}", currentHostname, newHostname);
                } else {
                    logger.info("不更新hosts文件，仅设置主机名");
                }

                // 4. 获取更新后的主机名进行验证
                String verifyCmd = "hostname";
                String verifyResult = MinaUtils.execCmdWithResult(session, verifyCmd).trim();

                boolean hostnameSetSuccess = true; // 默认认为设置成功

                if (!verifyResult.equals(newHostname)) {
                    logger.warn("主机名可能未成功更新，当前主机名为: {}, 期望主机名为: {}", verifyResult, newHostname);

                    // 尝试再次使用直接方式设置主机名
                    logger.info("尝试使用直接方式再次设置主机名: {}", newHostname);
                    String directCmd = sudoPrefix + "hostname " + newHostname;
                    MinaUtils.execCmdWithResult(session, directCmd);

                    // 再次验证
                    verifyResult = MinaUtils.execCmdWithResult(session, verifyCmd).trim();
                    if (!verifyResult.equals(newHostname)) {
                        logger.error("重试后主机名设置仍然失败，当前主机名: {}", verifyResult);
                        hostnameSetSuccess = false;
                    } else {
                        logger.info("重试设置主机名成功: {}", newHostname);
                    }
                }

                // 5. 只有当主机名设置成功或系统重启后会生效的情况下，才更新缓存
                String oldHostname = hostInfo.getHostname();
                hostInfo.setHostname(newHostname);

                // 6. 更新缓存中的主机信息
                hostMap.remove(ip); // 移除旧的记录
                hostMap.put(ip, hostInfo); // 添加更新后的记录

                // 7. 将更新后的主机映射放回缓存
                CacheUtils.put(clusterId + Constants.HOST_MAP, hostMap);

                // 8. 刷新单个主机信息缓存
                updateHostInfoCache(clusterId, hostInfo);

                // 9. 刷新全局主机信息缓存
                updateHostMapInCache(clusterId);

                if (hostnameSetSuccess) {
                    logger.info("主机名已成功更新: {} -> {}", oldHostname, newHostname);
                    return Result.success("主机名已成功更新为: " + newHostname);
                } else {
                    logger.warn("主机名设置未立即生效，可能需要重启系统: {} -> {}", oldHostname, newHostname);
                    return Result.success("主机名已设置，但未立即生效，可能需要重启系统后生效: " + newHostname);
                }
            } finally {
                if (session != null && session.isOpen()) {
                    MinaUtils.closeConnection(session);
                }
            }
        } catch (Exception e) {
            logger.error("更新主机名时发生错误", e);
            return Result.error("更新主机名失败: " + e.getMessage());
        }
    }

    /**
     * 计算SSH会话缓存命中率
     * 
     * @return 缓存命中百分比
     */
    private int calculateSessionCacheHitRate() {
        // 现在从SshConnectionPoolManager获取缓存命中率
        return sshConnectionPoolManager.calculateSessionCacheHitRate();
    }

    /**
     * 获取异步服务状态（返回实体类）
     * 
     * @return AsyncServiceStatus对象
     */
    public AsyncServiceStatus getAsyncServiceStatus() {
        AsyncServiceStatus status = new AsyncServiceStatus();
        try {
            // 获取状态信息
            ScheduledTasksStatus statusInfo = getScheduledTasksStatus();
            if (statusInfo != null) {
                // 填充实体类
                status.setScheduledTasksEnabled(statusInfo.isScheduledTasksEnabled());
                status.setLastTaskCleanupTime(statusInfo.getLastTaskCleanupTime());
                status.setRunningTasksCount(statusInfo.getRunningTasksCount());
                status.setConnectionPoolSize(statusInfo.getConnectionPoolSize());
                status.setTaskCleanupActive(statusInfo.isTaskCleanupActive());
                status.setConnectionCleanupActive(statusInfo.isConnectionCleanupActive());
                status.setLastConnectionCleanupTime(statusInfo.getLastConnectionCleanupTime());
            } else {
                // 如果状态信息为空，设置默认值
                status.setScheduledTasksEnabled(false);
                status.setLastTaskCleanupTime("未获取到");
                status.setRunningTasksCount(0);
                status.setConnectionPoolSize(0);
                status.setTaskCleanupActive(false);
                status.setConnectionCleanupActive(false);
                status.setLastConnectionCleanupTime("未获取到");
            }

            // 添加间隔毫秒值
            status.setTaskCleanupIntervalMs(this.taskCleanupIntervalMs);
            status.setConnectionCleanupIntervalMs(this.connectionCleanupIntervalMs);

            // 添加可读间隔
            status.setTaskCleanupInterval(formatTimeInterval(this.taskCleanupIntervalMs));
            status.setConnectionCleanupInterval(formatTimeInterval(this.connectionCleanupIntervalMs));

            // 添加SSH会话缓存命中率
            int cacheHitRate = calculateSessionCacheHitRate();
            status.setSessionCacheHitRate(cacheHitRate);
        } catch (Exception e) {
            // 异常处理，设置默认值
            logger.error("获取异步服务状态时发生异常", e);
            status.setScheduledTasksEnabled(false);
            status.setLastTaskCleanupTime("获取异常");
            status.setRunningTasksCount(0);
            status.setConnectionPoolSize(0);
            status.setTaskCleanupActive(false);
            status.setConnectionCleanupActive(false);
            status.setLastConnectionCleanupTime("获取异常");
            status.setTaskCleanupIntervalMs(this.taskCleanupIntervalMs);
            status.setConnectionCleanupIntervalMs(this.connectionCleanupIntervalMs);
            status.setTaskCleanupInterval(formatTimeInterval(this.taskCleanupIntervalMs));
            status.setConnectionCleanupInterval(formatTimeInterval(this.connectionCleanupIntervalMs));
            status.setSessionCacheHitRate(0);
        }

        return status;
    }

    /**
     * 仅停止任务清理定时任务
     */
    public void stopTaskCleanup() {
        if (taskCleanupTask != null && !taskCleanupTask.isCancelled()) {
            taskCleanupTask.cancel(false);
            logger.info("任务清理定时任务已停止");
        }
    }

    /**
     * 仅启动任务清理定时任务
     */
    public void startTaskCleanup() {
        if (taskCleanupTask == null || taskCleanupTask.isCancelled()) {
            taskCleanupTask = taskScheduler.scheduleAtFixedRate(
                    this::cleanupTasks, taskCleanupIntervalMs);
            logger.info("任务清理定时任务已启动，执行间隔: {}毫秒", taskCleanupIntervalMs);
        }
    }

    /**
     * 更新任务清理定时任务执行间隔
     * 
     * @param intervalMs 执行间隔（毫秒）
     */
    public void updateTaskCleanupInterval(long intervalMs) {
        if (intervalMs < 1000) { // 最小1秒
            logger.warn("任务清理定时任务间隔不能小于1秒，忽略此次更新");
            return;
        }

        this.taskCleanupIntervalMs = intervalMs;

        if (taskCleanupTask != null && !taskCleanupTask.isCancelled()) {
            taskCleanupTask.cancel(false);
            TaskScheduler actualScheduler = taskScheduler;
            taskCleanupTask = actualScheduler.scheduleAtFixedRate(
                    this::cleanupTasks, intervalMs);
            logger.info("任务清理定时任务已重新调度，新执行间隔: {}毫秒", intervalMs);
        }
    }

    /**
     * 更新连接清理定时任务执行间隔
     * 
     * @param intervalMs 执行间隔（毫秒）
     */
    public void updateConnectionCleanupInterval(long intervalMs) {
        if (intervalMs < 1000) { // 最小1秒
            logger.warn("连接清理定时任务间隔不能小于1秒，忽略此次更新");
            return;
        }

        this.connectionCleanupIntervalMs = intervalMs;
    }

    /**
     * 批量执行检查项，复用SSH连接
     * 
     * @param clusterId  集群ID
     * @param hostInfo   主机信息
     * @param checkItems 检查项列表
     * @return 检查结果列表
     */
    public List<CheckItem> batchExecuteCheck(Integer clusterId, HostInfo hostInfo, List<CheckItem> checkItems) {
        List<CheckItem> results = new ArrayList<>();
        ClientSession session;
        String hostKey = hostInfo.getIp() + ":" + hostInfo.getSshPort();

        try {
            // 尝试获取或创建一个连接
            session = sshConnectionPoolManager.getOrCreateConnection(hostInfo);
            if (session == null || !session.isOpen()) {
                logger.error("无法建立到主机 {} 的SSH连接", hostInfo.getIp());
                // 标记所有检查项为失败
                for (CheckItem item : checkItems) {
                    item.setStatus(CheckItem.Status.FAILED);
                    item.setMessage("无法建立SSH连接");

                    // 记录失败日志到缓存日志 - 确保每个检查项都有日志记录
                    String logKey = "CHECK_ITEM_LOG_" + clusterId + "_" + hostInfo.getIp() + "_" + item.getId();
                    com.datasophon.common.model.LogEntry logEntry = new com.datasophon.common.model.LogEntry(
                            new Date(),
                            com.datasophon.common.model.LogEntry.Level.ERROR,
                            Thread.currentThread().getName(),
                            this.getClass().getSimpleName(),
                            "无法建立到主机 " + hostInfo.getIp() + " 的SSH连接",
                            com.datasophon.common.model.LogEntry.Type.CHECK);
                    LogEntryManager.addLogEntry(logKey, logEntry);

                    results.add(item);
                }
                return results;
            }

            // 执行每个检查项
            for (CheckItem item : checkItems) {
                try {
                    // 获取相应的检查器
                    ItemChecker checker = itemCheckerFactory.getChecker(ItemCode.valueOf(item.getItemCode()));
                    if (checker == null) {
                        item.setStatus(CheckItem.Status.FAILED);
                        item.setMessage("找不到检查器: " + item.getItemName());

                        // 记录失败日志到缓存日志
                        String logKey = "CHECK_ITEM_LOG_" + clusterId + "_" + hostInfo.getIp() + "_"
                                + item.getId();
                        com.datasophon.common.model.LogEntry logEntry = new com.datasophon.common.model.LogEntry(
                                new Date(),
                                com.datasophon.common.model.LogEntry.Level.ERROR,
                                Thread.currentThread().getName(),
                                this.getClass().getSimpleName(),
                                "找不到检查器: " + item.getItemName(),
                                com.datasophon.common.model.LogEntry.Type.CHECK);
                        LogEntryManager.addLogEntry(logKey, logEntry);

                        results.add(item);
                        continue;
                    }

                    // 手动创建并存储检查项的日志键和开始日志
                    String logKey = "CHECK_ITEM_LOG_" + clusterId + "_" + hostInfo.getIp() + "_" + item.getId();

                    // 记录检查开始日志
                    com.datasophon.common.model.LogEntry startLogEntry = new com.datasophon.common.model.LogEntry(
                            new Date(),
                            com.datasophon.common.model.LogEntry.Level.INFO,
                            Thread.currentThread().getName(),
                            this.getClass().getSimpleName(),
                            "开始检查项: " + item.getItemName() + ", 使用SSH连接复用机制",
                            com.datasophon.common.model.LogEntry.Type.CHECK);
                    LogEntryManager.addLogEntry(logKey, startLogEntry);

                    logger.debug("开始执行检查项 {}, 使用现有SSH连接: {}", item.getItemName(), true);

                    // 执行检查
                    CheckItem result = checker.check(clusterId, hostInfo, item);
                    results.add(result);

                    // 记录检查完成日志
                    com.datasophon.common.model.LogEntry endLogEntry = new com.datasophon.common.model.LogEntry(
                            new Date(),
                            com.datasophon.common.model.LogEntry.Level.INFO,
                            Thread.currentThread().getName(),
                            this.getClass().getSimpleName(),
                            "检查项 " + item.getItemName() + " 完成，状态: " + result.getStatus(),
                            com.datasophon.common.model.LogEntry.Type.CHECK);
                    LogEntryManager.addLogEntry(logKey, endLogEntry);

                    // 更新最后访问时间
                    connectionLastAccessTime.put(hostKey, System.currentTimeMillis());
                } catch (Exception e) {
                    logger.error("执行检查项 {} 时发生异常: {}", item.getItemName(), e.getMessage(), e);
                    item.setStatus(CheckItem.Status.FAILED);
                    item.setMessage("检查异常: " + e.getMessage());

                    // 记录异常日志到缓存日志
                    String logKey = "CHECK_ITEM_LOG_" + clusterId + "_" + hostInfo.getIp() + "_" + item.getId();
                    com.datasophon.common.model.LogEntry exceptionLogEntry = new com.datasophon.common.model.LogEntry(
                            new Date(),
                            com.datasophon.common.model.LogEntry.Level.ERROR,
                            Thread.currentThread().getName(),
                            this.getClass().getSimpleName(),
                            "执行检查项 " + item.getItemName() + " 时发生异常: " + e.getMessage(),
                            com.datasophon.common.model.LogEntry.Type.CHECK);
                    LogEntryManager.addLogEntry(logKey, exceptionLogEntry);

                    results.add(item);
                }
            }
        } catch (Exception e) {
            logger.error("批量执行检查时发生异常: {}", e.getMessage(), e);
            // 标记所有剩余检查项为失败
            for (CheckItem item : checkItems) {
                if (!results.contains(item)) {
                    item.setStatus(CheckItem.Status.FAILED);
                    item.setMessage("批量检查异常: " + e.getMessage());

                    // 记录失败日志到缓存日志
                    String logKey = "CHECK_ITEM_LOG_" + clusterId + "_" + hostInfo.getIp() + "_" + item.getId();
                    com.datasophon.common.model.LogEntry errorLogEntry = new com.datasophon.common.model.LogEntry(
                            new Date(),
                            com.datasophon.common.model.LogEntry.Level.ERROR,
                            Thread.currentThread().getName(),
                            this.getClass().getSimpleName(),
                            "批量执行检查时发生异常: " + e.getMessage(),
                            com.datasophon.common.model.LogEntry.Type.CHECK);
                    LogEntryManager.addLogEntry(logKey, errorLogEntry);

                    results.add(item);
                }
            }
        }

        return results;
    }

    /**
     * 批量执行修复项，复用SSH连接
     * 
     * @param clusterId 集群ID
     * @param hostInfo  主机信息
     * @param fixItems  修复项列表
     * @return 修复结果列表
     */
    public List<CheckItem> batchExecuteFix(Integer clusterId, HostInfo hostInfo, List<CheckItem> fixItems) {
        List<CheckItem> results = new ArrayList<>();
        ClientSession session;
        String hostKey = hostInfo.getIp() + ":" + hostInfo.getSshPort();

        try {
            // 尝试获取或创建一个连接
            session = sshConnectionPoolManager.getOrCreateConnection(hostInfo);
            if (session == null || !session.isOpen()) {
                logger.error("无法建立到主机 {} 的SSH连接", hostInfo.getIp());
                // 标记所有修复项为失败
                for (CheckItem item : fixItems) {
                    item.setStatus(CheckItem.Status.FAILED);
                    item.setMessage("无法建立SSH连接");

                    // 记录失败日志到缓存日志
                    String logKey = "CHECK_ITEM_LOG_" + clusterId + "_" + hostInfo.getIp() + "_"
                            + item.getId();
                    com.datasophon.common.model.LogEntry logEntry = new com.datasophon.common.model.LogEntry(
                            new Date(),
                            com.datasophon.common.model.LogEntry.Level.ERROR,
                            Thread.currentThread().getName(),
                            this.getClass().getSimpleName(),
                            "无法建立到主机 " + hostInfo.getIp() + " 的SSH连接",
                            com.datasophon.common.model.LogEntry.Type.FIX);
                    LogEntryManager.addLogEntry(logKey, logEntry);

                    results.add(item);
                }
                return results;
            }

            // 标记使用现有会话并设置外部会话 - 这里是关键
            logger.debug("批量执行修复 - 已设置SSH会话: session.isOpen={}", session.isOpen());

            // 更新最后访问时间
            connectionLastAccessTime.put(hostKey, System.currentTimeMillis());

            // 执行每个修复项
            for (CheckItem item : fixItems) {
                try {
                    // 获取相应的检查器
                    ItemChecker checker = itemCheckerFactory.getChecker(ItemCode.valueOf(item.getItemCode()));
                    if (checker == null) {
                        item.setStatus(CheckItem.Status.FAILED);
                        item.setMessage("找不到检查器: " + item.getItemName());

                        // 记录失败日志到缓存日志
                        String logKey = "CHECK_ITEM_LOG_" + clusterId + "_" + hostInfo.getIp() + "_"
                                + item.getId();
                        com.datasophon.common.model.LogEntry logEntry = new com.datasophon.common.model.LogEntry(
                                new Date(),
                                com.datasophon.common.model.LogEntry.Level.ERROR,
                                Thread.currentThread().getName(),
                                this.getClass().getSimpleName(),
                                "找不到检查器: " + item.getItemName(),
                                com.datasophon.common.model.LogEntry.Type.FIX);
                        LogEntryManager.addLogEntry(logKey, logEntry);

                        results.add(item);
                        continue;
                    }

                    // 手动创建并存储修复项的日志键和开始日志
                    String logKey = "CHECK_ITEM_LOG_" + clusterId + "_" + hostInfo.getIp() + "_" + item.getId();

                    // 记录修复开始日志
                    com.datasophon.common.model.LogEntry startLogEntry = new com.datasophon.common.model.LogEntry(
                            new Date(),
                            com.datasophon.common.model.LogEntry.Level.INFO,
                            Thread.currentThread().getName(),
                            this.getClass().getSimpleName(),
                            "开始修复项: " + item.getItemName() + ", 使用SSH连接复用机制",
                            com.datasophon.common.model.LogEntry.Type.FIX);
                    LogEntryManager.addLogEntry(logKey, startLogEntry);

                    // 对于免密登录检查项，始终使用独立会话
                    boolean isPasswordFreeItem = ItemCode.PASSWORD_FREE.toString().equals(item.getItemCode());
                    if (isPasswordFreeItem) {
                        // 免密检查项总是使用独立会话，不使用共享连接池
                        logger.info("执行免密登录修复项，使用独立SSH连接");

                        // 记录使用独立连接的日志
                        com.datasophon.common.model.LogEntry connLogEntry = new com.datasophon.common.model.LogEntry(
                                new Date(),
                                com.datasophon.common.model.LogEntry.Level.INFO,
                                Thread.currentThread().getName(),
                                this.getClass().getSimpleName(),
                                "免密登录修复项使用独立SSH连接",
                                com.datasophon.common.model.LogEntry.Type.FIX);
                        LogEntryManager.addLogEntry(logKey, connLogEntry);
                    } else {
                        // 非免密检查项继续使用共享会话
                        logger.debug("开始执行修复项 {}, 使用现有SSH连接", item.getItemName());
                    }

                    // 执行修复
                    boolean fixResult = checker.fix(clusterId, hostInfo, item);
                    if (fixResult) {
                        item.setStatus(CheckItem.Status.SUCCESS);
                        if (item.getMessage() == null || item.getMessage().isEmpty() ||
                                "正在修复...".equals(item.getMessage())) {
                            item.setMessage("修复成功");
                        }

                        // 记录修复成功日志
                        com.datasophon.common.model.LogEntry successLogEntry = new com.datasophon.common.model.LogEntry(
                                new Date(),
                                com.datasophon.common.model.LogEntry.Level.INFO,
                                Thread.currentThread().getName(),
                                this.getClass().getSimpleName(),
                                "修复项 " + item.getItemName() + " 成功完成",
                                com.datasophon.common.model.LogEntry.Type.FIX);
                        LogEntryManager.addLogEntry(logKey, successLogEntry);
                    } else {
                        item.setStatus(CheckItem.Status.FAILED);
                        if (item.getMessage() == null || item.getMessage().isEmpty() ||
                                "正在修复...".equals(item.getMessage())) {
                            item.setMessage("修复失败");
                        }

                        // 记录修复失败日志
                        com.datasophon.common.model.LogEntry failLogEntry = new com.datasophon.common.model.LogEntry(
                                new Date(),
                                com.datasophon.common.model.LogEntry.Level.ERROR,
                                Thread.currentThread().getName(),
                                this.getClass().getSimpleName(),
                                "修复项 " + item.getItemName() + " 失败: " + item.getMessage(),
                                com.datasophon.common.model.LogEntry.Type.FIX);
                        LogEntryManager.addLogEntry(logKey, failLogEntry);
                    }
                    results.add(item);

                    // 更新最后访问时间（如果使用共享连接）
                    if (!isPasswordFreeItem) {
                        connectionLastAccessTime.put(hostKey, System.currentTimeMillis());
                    }
                } catch (Exception e) {
                    logger.error("执行修复项 {} 时发生异常: {}", item.getItemName(), e.getMessage(), e);
                    item.setStatus(CheckItem.Status.FAILED);
                    item.setMessage("修复异常: " + e.getMessage());

                    // 记录异常日志到缓存日志
                    String logKey = "CHECK_ITEM_LOG_" + clusterId + "_" + hostInfo.getIp() + "_" + item.getId();
                    com.datasophon.common.model.LogEntry exceptionLogEntry = new com.datasophon.common.model.LogEntry(
                            new Date(),
                            com.datasophon.common.model.LogEntry.Level.ERROR,
                            Thread.currentThread().getName(),
                            this.getClass().getSimpleName(),
                            "执行修复项 " + item.getItemName() + " 时发生异常: " + e.getMessage(),
                            com.datasophon.common.model.LogEntry.Type.FIX);
                    LogEntryManager.addLogEntry(logKey, exceptionLogEntry);

                    results.add(item);
                }
            }
        } catch (Exception e) {
            logger.error("批量执行修复时发生异常: {}", e.getMessage(), e);
            // 标记所有剩余修复项为失败
            for (CheckItem item : fixItems) {
                if (!results.contains(item)) {
                    item.setStatus(CheckItem.Status.FAILED);
                    item.setMessage("批量修复异常: " + e.getMessage());

                    // 记录失败日志到缓存日志
                    String logKey = "CHECK_ITEM_LOG_" + clusterId + "_" + hostInfo.getIp() + "_" + item.getId();
                    com.datasophon.common.model.LogEntry errorLogEntry = new com.datasophon.common.model.LogEntry(
                            new Date(),
                            com.datasophon.common.model.LogEntry.Level.ERROR,
                            Thread.currentThread().getName(),
                            this.getClass().getSimpleName(),
                            "批量执行修复时发生异常: " + e.getMessage(),
                            com.datasophon.common.model.LogEntry.Type.FIX);
                    LogEntryManager.addLogEntry(logKey, errorLogEntry);

                    results.add(item);
                }
            }
        }

        return results;
    }

    /**
     * 异步执行同步hosts文件任务
     *
     * @param taskId           任务ID
     * @param clusterId        集群ID
     * @param hostMap          主机信息映射
     * @param hostsFilePreview hosts文件预览信息
     */
    public void syncHostsFileTask(String taskId, Integer clusterId, Map<String, HostInfo> hostMap,
            Object hostsFilePreview) {
        // 参数校验
        if (taskId == null || clusterId == null || hostMap == null || hostsFilePreview == null) {
            logger.error("同步hosts文件任务参数异常: taskId={}, clusterId={}, hostMap={}, hostsFilePreview={}",
                    taskId, clusterId, hostMap != null ? "非空" : "空",
                    hostsFilePreview != null ? "非空" : "空");
            return;
        }

        logger.info("开始异步执行hosts文件同步任务，集群ID: {}, 任务ID: {}", clusterId, taskId);

        // 使用hardwareInfoExecutor执行（第二阶段）而不是hostsFileExecutor
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            try {
                // 检查Spring上下文是否可用
                if (taskManager == null || taskManager.getApplicationContext() == null) {
                    logger.error("任务管理器或Spring上下文为空，无法获取HostCheckService");
                    return;
                }

                // 获取主机IP列表并排序
                List<String> ips = new ArrayList<>(hostMap.keySet());
                ips = com.datasophon.common.utils.HostUtils.sortIpAddresses(ips);

                // 获取hosts文件内容
                String hostsContent = ((com.datasophon.api.service.impl.HostCheckServiceImpl.HostsFilePreviewVO) hostsFilePreview)
                        .getHostsContent();

                // 从Spring容器获取HostCheckService
                com.datasophon.api.service.HostCheckService hostCheckService = taskManager.getApplicationContext()
                        .getBean(com.datasophon.api.service.HostCheckService.class);

                // 批量并行处理主机（每批10个）
                final int batchSize = 10;
                for (int i = 0; i < ips.size(); i += batchSize) {
                    // 获取当前批次的主机IP
                    int endIndex = Math.min(i + batchSize, ips.size());
                    List<String> batchIps = ips.subList(i, endIndex);

                    logger.info("开始并行处理第{}批主机，数量: {}", (i / batchSize) + 1, batchIps.size());

                    // 创建当前批次的任务列表
                    List<CompletableFuture<Void>> batchTasks = new ArrayList<>();

                    // 为每个主机创建异步任务
                    for (String ip : batchIps) {
                        CompletableFuture<Void> task = CompletableFuture.runAsync(() -> {
                            try {
                                logger.info("正在同步hosts文件到主机: {}", ip);

                                // 调用主机检查服务更新hosts文件
                                Result updateResult = hostCheckService.updateHostsFile(
                                        clusterId,
                                        ip, hostsContent);

                                // 更新主机处理状态
                                try {
                                    com.datasophon.api.service.impl.TaskProgressHelper.updateHostProcessStatus(
                                            taskId,
                                            ip,
                                            updateResult.isSuccess(),
                                            updateResult.isSuccess() ? null : updateResult.getMsg());
                                } catch (Exception e) {
                                    logger.error("更新任务进度状态失败: {}", e.getMessage(), e);
                                }
                            } catch (Exception e) {
                                logger.error("同步hosts文件到主机{}时发生错误", ip, e);
                                // 更新主机处理状态为失败
                                try {
                                    com.datasophon.api.service.impl.TaskProgressHelper.updateHostProcessStatus(
                                            taskId,
                                            ip,
                                            false,
                                            e.getMessage());
                                } catch (Exception ex) {
                                    logger.error("更新任务进度状态失败: {}", ex.getMessage(), ex);
                                }
                            }
                        }, hardwareInfoExecutor);

                        batchTasks.add(task);
                    }

                    // 等待当前批次的所有任务完成
                    try {
                        CompletableFuture.allOf(batchTasks.toArray(new CompletableFuture[0])).get();
                        logger.info("第{}批主机处理完成", (i / batchSize) + 1);
                    } catch (Exception e) {
                        logger.error("等待批处理任务完成时发生错误", e);
                    }
                }

                // 完成任务
                try {
                    com.datasophon.api.service.impl.TaskProgressHelper.completeTask(
                            taskId,
                            "所有主机的hosts文件已成功同步",
                            "部分主机的hosts文件同步失败，请检查详情");
                } catch (Exception e) {
                    logger.error("完成任务状态更新失败: {}", e.getMessage(), e);
                }

                logger.info("hosts文件同步任务完成，集群ID: {}, 任务ID: {}", clusterId, taskId);

            } catch (Exception e) {
                logger.error("执行hosts文件同步任务时发生错误", e);
            }
        }, hardwareInfoExecutor); // 使用硬件信息执行器（第二阶段）

        // 注册任务
        try {
            taskManager.registerTask("sync_hosts_file", "同步hosts文件 - 集群ID: " + clusterId, future);
        } catch (Exception e) {
            logger.error("注册任务时发生错误: {}", e.getMessage(), e);
        }

        // 任务完成后保留一段时间进度信息，然后移除
        future.thenRun(() -> {
            try {
                Thread.sleep(TimeUnit.MINUTES.toMillis(30));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                // 移除任务进度
                try {
                    com.datasophon.api.service.impl.TaskProgressHelper.removeTaskProgress(taskId);
                } catch (Exception e) {
                    logger.error("移除任务进度信息失败: {}", e.getMessage(), e);
                }
            }
        });
    }

    /**
     * 更新主机信息到缓存
     *
     * @param clusterId 集群ID
     * @param hostInfo  主机信息
     */
    public void updateHostInfoCache(Integer clusterId, HostInfo hostInfo) {
        if (clusterId == null || hostInfo == null) {
            return;
        }

        // 获取当前缓存中的主机信息
        Map<String, HostInfo> map = CacheUtils.getHostMap(clusterId + Constants.HOST_MAP);
        // 检查是否是修复状态的变化，如果是则记录详细日志
        HostInfo oldHostInfo = map.get(hostInfo.getIp());
        if (oldHostInfo != null) {
            CheckItem.Status oldStatus = oldHostInfo.getStatus();
            CheckItem.Status newStatus = hostInfo.getStatus();

            if (oldStatus != newStatus) {
                if (newStatus == CheckItem.Status.FIXING || newStatus == CheckItem.Status.WAITING_FIX) {
                    logger.info("主机状态变化 - clusterId: {}, 主机: {}, 状态: {} -> {}, 消息: {}",
                            clusterId, hostInfo.getIp(),
                            oldStatus != null ? oldStatus.name() : "null",
                            newStatus.name(),
                            hostInfo.getMessage());
                } else if (oldStatus == CheckItem.Status.FIXING || oldStatus == CheckItem.Status.WAITING_FIX) {
                    logger.info("主机修复状态结束 - clusterId: {}, 主机: {}, 状态: {} -> {}, 消息: {}",
                            clusterId, hostInfo.getIp(),
                            oldStatus.name(),
                            newStatus != null ? newStatus.name() : "null",
                            hostInfo.getMessage());
                }
            }

            // 检查检查项状态变化
            if (oldHostInfo.getCheckItems() != null && hostInfo.getCheckItems() != null) {
                Map<Integer, CheckItem.Status> oldItemStatusMap = new HashMap<>();
                for (CheckItem oldItem : oldHostInfo.getCheckItems()) {
                    oldItemStatusMap.put(oldItem.getId(), oldItem.getStatus());
                }

                for (CheckItem newItem : hostInfo.getCheckItems()) {
                    CheckItem.Status oldItemStatus = oldItemStatusMap.get(newItem.getId());
                    if (oldItemStatus != null && oldItemStatus != newItem.getStatus()) {
                        // 记录检查项状态变化，特别是与修复相关的状态
                        if (newItem.getStatus() == CheckItem.Status.FIXING ||
                                newItem.getStatus() == CheckItem.Status.WAITING_FIX) {
                            logger.info("检查项状态变化 - clusterId: {}, 主机: {}, 检查项ID: {}, 检查项: {}, 状态: {} -> {}, 消息: {}",
                                    clusterId, hostInfo.getIp(), newItem.getId(), newItem.getItemName(),
                                    oldItemStatus.name(), newItem.getStatus().name(),
                                    newItem.getMessage());
                        }
                    }
                }
            }
        }

        // 更新缓存中的主机信息
        map.put(hostInfo.getIp(), hostInfo);
        CacheUtils.put(clusterId + Constants.HOST_MAP, map);

        if (hostInfo.getStatus() == CheckItem.Status.FIXING ||
                hostInfo.getStatus() == CheckItem.Status.WAITING_FIX) {
            logger.info("已更新集群{}中主机{}的修复状态信息到缓存, 状态: {}, 消息: {}",
                    clusterId, hostInfo.getIp(),
                    hostInfo.getStatus() != null ? hostInfo.getStatus().name() : "null",
                    hostInfo.getMessage());
        } else {
            logger.debug("已更新集群{}中主机{}的信息到缓存", clusterId, hostInfo.getIp());
        }
    }

    /**
     * 更新主机信息到缓存
     *
     * @param clusterId 集群ID
     */
    public void updateHostMapInCache(Integer clusterId) {
        try {
            logger.info("更新集群{}的主机信息缓存", clusterId);

            // 获取当前缓存中的主机信息
            Map<String, HostInfo> map = CacheUtils.getHostMap(clusterId + Constants.HOST_MAP);
            if (map.isEmpty()) {
                logger.warn("缓存中未找到集群{}的主机信息", clusterId);
                return;
            }

            // 遍历所有主机，确保主机信息是最新的
            for (Map.Entry<String, HostInfo> entry : map.entrySet()) {
                String ip = entry.getKey();
                HostInfo hostInfo = entry.getValue();

                // 更新每个主机的信息到缓存
                updateHostInfoCache(clusterId, hostInfo);
            }

            // 将更新后的信息重新放入缓存
            CacheUtils.put(clusterId + Constants.HOST_MAP, map);

            logger.info("成功更新集群{}的主机信息缓存，共{}台主机", clusterId, map.size());
        } catch (Exception e) {
            logger.error("更新主机信息缓存时发生错误", e);
        }
    }
}