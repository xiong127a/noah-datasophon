package com.datasophon.api.service.checker.core;

import com.datasophon.api.service.OsInfoService;
import com.datasophon.api.service.checker.common.CommandResult;
import com.datasophon.api.service.checker.common.ItemCode;
import com.datasophon.api.service.checker.helpers.CheckLogger;
import com.datasophon.api.service.checker.helpers.HtmlStyleHelper;
import com.datasophon.api.utils.MinaUtils;
import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.enums.OsDistribution;
import com.datasophon.common.model.CheckItem;
import com.datasophon.common.model.HostInfo;
import com.datasophon.common.model.LogEntry;
import com.datasophon.common.model.OsInfo;
import org.apache.sshd.client.session.ClientSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public abstract class AbstractItemChecker implements ItemChecker {
    private static final Logger logger = LoggerFactory.getLogger(AbstractItemChecker.class);
    private static final String CHECK_ITEM_LOG_PREFIX = "CHECK_ITEM_LOG_";

    // 主机操作系统信息缓存，用于避免重复检测
    private static final Map<String, OsInfo> hostOsInfoCache = new ConcurrentHashMap<>();

    @Autowired
    protected OsInfoService osInfoService;

    protected ClientSession session;
    // 当前检查项的日志缓存键
    protected String currentLogKey;
    // 当前操作类型
    protected LogEntry.Type operationType = LogEntry.Type.CHECK;

    // 供子类使用的日志记录器实例，同时记录到缓存和控制台
    protected final CheckLogger cacheLog;

    // 添加一个成员变量来存储当前正在处理的主机信息
    public ThreadLocal<HostInfo> currentHostInfo = new ThreadLocal<>();

    /**
     * 构造函数
     */
    public AbstractItemChecker() {
        // 初始化一个默认的日志记录器
        // 注意：此时currentLogKey为null，初始日志会发送到slf4j但不会缓存
        // 在setCurrentLogKey方法调用后，日志会正确缓存
        this.cacheLog = CheckLogger.createLogger(null, this.getClass().getSimpleName());
    }

    // 设置当前检查项的日志缓存键
    protected void setCurrentLogKey(Integer clusterId, String hostname, Integer itemId) {
        // 使用统一的日志键格式
        this.currentLogKey = String.format("%s%d_%s_%d", CHECK_ITEM_LOG_PREFIX, clusterId, hostname, itemId);
        logger.debug("设置日志键: {}, 类型: {}", this.currentLogKey, operationType.getDisplayName());

        // 更新日志记录器的logKey和类型
        CheckLogger.LoggerImpl loggerImpl = (CheckLogger.LoggerImpl) this.cacheLog;
        loggerImpl.updateLogKey(this.currentLogKey);
        loggerImpl.setLogType(operationType);
    }

    /**
     * 获取远程主机的操作系统信息
     * 
     * @param hostInfo 主机信息
     * @return 操作系统信息对象
     * @throws InterruptedException 如果命令执行被中断
     */
    protected OsInfo getOsInfo(HostInfo hostInfo) throws InterruptedException {
        // 使用主机名作为缓存键
        String cacheKey = hostInfo.getIp();

        // 检查缓存中是否已存在该主机的OS信息
        OsInfo cachedInfo = hostOsInfoCache.get(cacheKey);
        if (cachedInfo != null && cachedInfo.isValid()) {
            logger.debug("使用缓存的操作系统信息: {}, {}", hostInfo.getIp(), cachedInfo);
            return cachedInfo;
        }

        // 创建一个新的OS信息对象
        OsInfo osInfo = new OsInfo();

        try {
            cacheLog.info("检测操作系统类型和版本...");

            // 尝试读取/etc/os-release文件，这是大多数现代Linux发行版共有的
            CommandResult osReleaseResult = execCommand(session, "cat /etc/os-release 2>/dev/null || echo 'Not Found'");

            if (osReleaseResult.isSuccess() && !osReleaseResult.getOutput().contains("Not Found")) {
                // 解析/etc/os-release文件内容
                String osRelease = osReleaseResult.getOutput();

                // 获取ID字段 (例如: ID=centos, ID=ubuntu)
                String distroId = extractValue(osRelease, "ID=");
                osInfo.setDistribution(distroId);

                // 获取VERSION_ID字段 (例如: VERSION_ID="7", VERSION_ID="20.04")
                String versionId = extractValue(osRelease, "VERSION_ID=");
                osInfo.setVersionId(versionId);

                // 获取PRETTY_NAME字段，通常包含更友好的描述
                String prettyName = extractValue(osRelease, "PRETTY_NAME=");
                osInfo.setFullName(prettyName);

                // 设置Linux发行版类型
                osInfo.setOsDistribution(determineDistribution(distroId));

                // 确保操作系统类型与ID保持一致
                if (osInfo.getOsDistribution() == OsDistribution.OTHER && !distroId.isEmpty()) {
                    logger.warn("操作系统类型识别异常，distroId='{}' 但 distribution={}，尝试强制更新",
                            distroId, osInfo.getOsDistribution());
                    osInfo.forceUpdateDistribution();
                    logger.info("更新后的操作系统类型：{}", osInfo.getOsDistribution());
                }

                cacheLog.info("操作系统: %s, 版本: %s, 类型: %s", prettyName, versionId, osInfo.getOsDistribution());
            } else {
                // 如果/etc/os-release不存在，尝试其他方法

                // 检查是否是CentOS/RHEL (查看/etc/redhat-release)
                CommandResult redhatReleaseResult = execCommand(session,
                        "cat /etc/redhat-release 2>/dev/null || echo 'Not Found'");
                if (redhatReleaseResult.isSuccess() && !redhatReleaseResult.getOutput().contains("Not Found")) {
                    String release = redhatReleaseResult.getOutput().trim();
                    osInfo.setFullName(release);

                    if (release.toLowerCase().contains("centos")) {
                        osInfo.setOsDistribution(OsDistribution.CENTOS);
                        osInfo.setDistribution("centos");
                    } else if (release.toLowerCase().contains("red hat")) {
                        osInfo.setOsDistribution(OsDistribution.REDHAT);
                        osInfo.setDistribution("rhel");
                    }

                    // 尝试提取版本号
                    String version = extractVersionFromString(release);
                    osInfo.setVersionId(version);

                    cacheLog.info("操作系统: %s, 版本: %s", release, version);
                } else {
                    // 使用通用方法检测
                    CommandResult lsbReleaseResult = execCommand(session,
                            "lsb_release -a 2>/dev/null || echo 'Not Found'");
                    if (lsbReleaseResult.isSuccess() && !lsbReleaseResult.getOutput().contains("Not Found")) {
                        String lsbOutput = lsbReleaseResult.getOutput();
                        String distro = extractValueFromLine(lsbOutput, "Distributor ID:");
                        String version = extractValueFromLine(lsbOutput, "Release:");
                        String description = extractValueFromLine(lsbOutput, "Description:");

                        osInfo.setFullName(description);
                        osInfo.setDistribution(distro.toLowerCase());
                        osInfo.setVersionId(version);
                        osInfo.setOsDistribution(determineDistribution(distro.toLowerCase()));

                        cacheLog.info("操作系统: %s, 版本: %s", description, version);
                    } else {
                        // 最后使用uname作为后备方案
                        CommandResult unameResult = execCommand(session, "uname -a");
                        if (unameResult.isSuccess()) {
                            osInfo.setFullName(unameResult.getOutput().trim());
                            osInfo.setOsDistribution(OsDistribution.OTHER);
                            cacheLog.info("无法确定具体Linux发行版，uname输出: %s", unameResult.getOutput().trim());
                        } else {
                            cacheLog.warn("无法确定操作系统类型和版本");
                            osInfo.setOsDistribution(OsDistribution.OTHER);
                        }
                    }
                }
            }

            // 获取Linux内核版本
            CommandResult kernelResult = execCommand(session, "uname -r");
            if (kernelResult.isSuccess()) {
                osInfo.setKernelVersion(kernelResult.getOutput().trim());
                cacheLog.info("内核版本: %s", kernelResult.getOutput().trim());
            }

        } catch (Exception e) {
            logger.error("获取操作系统信息时发生错误: {}", e.getMessage(), e);
            cacheLog.error("获取操作系统信息失败: %s", e.getMessage());
            osInfo.setOsDistribution(OsDistribution.OTHER);
        }

        // 设置有效性并缓存结果
        osInfo.setValid(true);

        // 最后一次确保操作系统类型与ID保持一致
        if (osInfo.getOsDistribution() == OsDistribution.OTHER &&
                osInfo.getDistribution() != null && !osInfo.getDistribution().isEmpty()) {
            logger.warn("缓存前检测到操作系统类型可能不一致: distribution='{}' 但 distribution={}",
                    osInfo.getDistribution(), osInfo.getOsDistribution());
            osInfo.forceUpdateDistribution();
            logger.info("缓存前更新后的操作系统类型: {}", osInfo.getOsDistribution());
        }

        logger.info("最终确定的操作系统信息: {}", osInfo);
        hostOsInfoCache.put(cacheKey, osInfo);
        return osInfo;
    }

    /**
     * 从字符串中提取键值对中的值
     * 
     * @param content 要解析的字符串内容
     * @param key     要查找的键，格式如 "KEY="
     * @return 提取的值，如果未找到则返回空字符串
     */
    private String extractValue(String content, String key) {
        if (content == null || key == null) {
            return "";
        }

        for (String line : content.split("\n")) {
            line = line.trim();
            if (line.startsWith(key)) {
                String value = line.substring(key.length()).trim();
                // 移除引号 (如果有)
                if ((value.startsWith("\"") && value.endsWith("\"")) ||
                        (value.startsWith("'") && value.endsWith("'"))) {
                    value = value.substring(1, value.length() - 1);
                }
                return value;
            }
        }
        return "";
    }

    /**
     * 从lsb_release -a输出中提取值
     */
    private String extractValueFromLine(String content, String prefix) {
        if (content == null || prefix == null) {
            return "";
        }

        for (String line : content.split("\n")) {
            line = line.trim();
            if (line.startsWith(prefix)) {
                return line.substring(prefix.length()).trim();
            }
        }
        return "";
    }

    /**
     * 从发行版描述字符串中提取版本号
     */
    private String extractVersionFromString(String release) {
        if (release == null) {
            return "";
        }

        // 尝试匹配版本号模式 (如 7.9.2009, 8.3, 等)
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\d+(\\.\\d+)+");
        java.util.regex.Matcher matcher = pattern.matcher(release);
        if (matcher.find()) {
            return matcher.group();
        }

        // 尝试匹配单个数字 (如 7, 8, 等)
        pattern = java.util.regex.Pattern.compile("\\s+\\d+\\s+");
        matcher = pattern.matcher(release);
        if (matcher.find()) {
            return matcher.group().trim();
        }

        return "";
    }

    /**
     * 根据发行版ID确定Linux发行版类型
     */
    private OsDistribution determineDistribution(String distroId) {
        if (distroId == null || distroId.isEmpty()) {
            return OsDistribution.OTHER;
        }

        String lowerDistroId = distroId.toLowerCase();
        if (lowerDistroId.contains("centos")) {
            return OsDistribution.CENTOS;
        } else if (lowerDistroId.contains("redhat") || lowerDistroId.contains("rhel")) {
            return OsDistribution.REDHAT;
        } else if (lowerDistroId.contains("ubuntu")) {
            return OsDistribution.UBUNTU;
        } else if (lowerDistroId.contains("debian")) {
            return OsDistribution.DEBIAN;
        } else if (lowerDistroId.contains("fedora")) {
            return OsDistribution.OTHER; // 新枚举中不包含Fedora，暂时映射到OTHER
        } else if (lowerDistroId.contains("suse")) {
            return OsDistribution.OTHER; // 新枚举中不包含SUSE，暂时映射到OTHER
        } else if (lowerDistroId.contains("kylin")) {
            return OsDistribution.KYLIN;
        } else if (lowerDistroId.contains("openeuler")) {
            return OsDistribution.OTHER; // 新枚举中不包含OpenEuler，暂时映射到OTHER
        }

        return OsDistribution.OTHER;
    }

    /**
     * 获取检查器类型
     */
    @Override
    public ItemCode getCheckerType() {
        // 默认返回null，子类需要覆盖实现
        return null;
    }

    /**
     * 格式化日期为标准格式
     * 
     * @param date 日期对象
     * @return 格式化后的日期字符串
     */
    protected String formatDate(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(date);
    }

    /**
     * 获取当前时间的格式化字符串
     * 
     * @return 当前时间的标准格式字符串
     */
    protected String getCurrentTime() {
        return formatDate(new Date());
    }

    // 格式化连接地址
    private String formatAddress(HostInfo hostInfo) {
        if (hostInfo == null) {
            return "";
        }
        return String.format("主机: %s, 端口: %d, 用户: %s", hostInfo.getIp(), hostInfo.getSshPort(),
                hostInfo.getSshUser());
    }

    /**
     * 执行命令，采用异步方式优化中断处理
     * 
     * @param session SSH会话
     * @param command 要执行的命令
     * @return 命令执行结果
     * @throws InterruptedException 如果命令执行被中断
     */
    protected CommandResult execCommand(ClientSession session, String command) throws InterruptedException {
        // 检查参数
        if (session == null) {
            logger.error("SSH会话为空，无法执行命令");
            cacheLog.error("SSH会话为空，无法执行命令");
            return new CommandResult("", "SSH会话为空", -1);
        }

        // 从当前上下文中获取主机信息
        HostInfo currentHostInfo = getCurrentHostInfo();
        String formattedAddress = formatAddress(currentHostInfo);

        logger.debug("准备执行命令: {} 在主机: {}", command, formattedAddress);
        cacheLog.debug("准备执行命令: %s 在主机: %s", command, formattedAddress);

        try {
            // 创建执行命令的通道
            try (org.apache.sshd.client.channel.ClientChannel channel = session.createExecChannel(command)) {
                logger.debug("命令通道已创建，正在打开通道");
                cacheLog.debug("命令通道已创建，正在打开通道");

                // 启动命令
                channel.open().verify(30, TimeUnit.SECONDS);
                logger.debug("命令通道已打开，开始执行命令");
                cacheLog.debug("命令通道已打开，开始执行命令");

                // 读取命令输出
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                ByteArrayOutputStream err = new ByteArrayOutputStream();
                channel.setOut(out);
                channel.setErr(err);

                // 等待命令完成，使用CompletableFuture处理超时和中断
                long timeoutMs = TimeUnit.SECONDS.toMillis(30);
                logger.debug("等待命令执行完成，超时时间: {}ms", timeoutMs);
                cacheLog.debug("等待命令执行完成，超时时间: %dms", timeoutMs);

                long startTime = System.currentTimeMillis();

                // 创建一个CompletableFuture来等待命令完成
                CompletableFuture<Set<org.apache.sshd.client.channel.ClientChannelEvent>> future = new CompletableFuture<>();

                // 异步等待命令完成
                Thread waitThread = new Thread(() -> {
                    try {
                        Set<org.apache.sshd.client.channel.ClientChannelEvent> events = channel.waitFor(
                                EnumSet.of(
                                        org.apache.sshd.client.channel.ClientChannelEvent.CLOSED,
                                        org.apache.sshd.client.channel.ClientChannelEvent.EOF),
                                timeoutMs);
                        future.complete(events);
                    } catch (Exception e) {
                        future.completeExceptionally(e);
                    }
                });
                waitThread.setName("command-wait-" + System.currentTimeMillis());
                waitThread.start();

                try {
                    // 等待命令完成或超时
                    future.get(timeoutMs, TimeUnit.MILLISECONDS);
                } catch (java.util.concurrent.TimeoutException e) {
                    // 处理超时
                    logger.warn("命令执行超时: {}, 主机: {}, 已等待时间: {}ms",
                            command, formattedAddress, System.currentTimeMillis() - startTime);
                    cacheLog.warn("命令执行超时: %s, 主机: %s, 已等待时间: %dms",
                            command, formattedAddress, System.currentTimeMillis() - startTime);

                    // 中断等待线程
                    waitThread.interrupt();
                    return new CommandResult("", "命令执行超时，请检查网络或主机状态", -1);
                } catch (java.util.concurrent.CancellationException e) {
                    // 处理取消
                    logger.info("命令执行被取消: {}", command);
                    cacheLog.info("命令执行被取消: %s", command);

                    // 中断等待线程
                    waitThread.interrupt();
                    throw new InterruptedException("命令执行被取消");
                } catch (InterruptedException e) {
                    // 处理中断
                    logger.info("命令执行等待被中断: {}", command);
                    cacheLog.info("命令执行等待被中断: %s", command);

                    // 中断等待线程
                    waitThread.interrupt();
                    throw e;
                } catch (Exception e) {
                    // 处理其他异常
                    logger.error("命令执行等待时发生异常: {}", e.getMessage(), e);
                    cacheLog.error("命令执行等待时发生异常: %s", e.getMessage());

                    // 中断等待线程
                    waitThread.interrupt();
                    return new CommandResult("", "命令执行异常: " + e.getMessage(), -1);
                }

                long endTime = System.currentTimeMillis();
                logger.debug("命令执行完成，耗时: {}ms", (endTime - startTime));
                cacheLog.debug("命令执行完成，耗时: %dms", (endTime - startTime));

                // 获取命令执行的退出状态
                Integer exitStatus = channel.getExitStatus();
                String output = out.toString();
                String error = err.toString();

                // 记录命令执行结果
                if (exitStatus != null && exitStatus != 0) {
                    logger.warn("命令执行失败，退出状态: {}, 错误信息: {}, 主机: {}",
                            exitStatus, error, formattedAddress);
                    cacheLog.warn("命令执行失败，退出状态: %d, 错误信息: %s, 主机: %s",
                            exitStatus, error, formattedAddress);
                } else {
                    if (output.length() > 100) {
                        logger.debug("命令执行成功，输出(前100字符): {}", output.substring(0, 100) + "...");
                        cacheLog.debug("命令执行成功，输出(前100字符): %s", output.substring(0, 100) + "...");
                    } else {
                        logger.debug("命令执行成功，输出: {}", output);
                        cacheLog.debug("命令执行成功，输出: %s", output);
                    }
                }

                return new CommandResult(output, error, exitStatus != null ? exitStatus : -1);
            }
        } catch (InterruptedException e) {
            // 重新抛出中断异常，确保调用方知道发生了中断
            Thread.currentThread().interrupt();
            throw e;
        } catch (Exception e) {
            logger.error("执行命令 {} 失败: {}, 异常类型: {}",
                    command, e.getMessage(), e.getClass().getName(), e);
            cacheLog.error("执行命令 %s 失败: %s, 异常类型: %s",
                    command, e.getMessage(), e.getClass().getName());

            // 检查是否是由于中断导致的异常
            if (e instanceof InterruptedException || e.getCause() instanceof InterruptedException) {
                Thread.currentThread().interrupt();
                throw new InterruptedException("命令执行过程中被中断: " + e.getMessage());
            }

            return new CommandResult("", e.getMessage(), -1);
        }
    }

    /**
     * 打开SSH会话
     */
    protected void openSession(HostInfo hostInfo) {
        try {
            // 通过Mina工具打开SSH连接
            cacheLog.info("开始连接到主机 %s, 端口: %d, 用户: %s", hostInfo.getIp(),
                    hostInfo.getSshPort(), hostInfo.getSshUser());

            // 明确初始化为null，确保之前可能的有效session被清理
            session = null;

            // 尝试建立会话连接
            session = MinaUtils.openConnection(hostInfo);

            // 验证session是否成功建立
            if (session == null) {
                cacheLog.error("建立SSH连接失败：会话对象为null");
                throw new RuntimeException("无法建立SSH连接：会话对象为null");
            }

            cacheLog.info("成功建立SSH连接");

            // 确保cacheLog在日志记录前被设置的currentLogKey
            if (currentLogKey == null) {
                logger.warn("检测到currentLogKey未设置，日志可能无法正确存储到缓存");
            }

        } catch (Exception e) {
            // 记录详细的异常信息到缓存日志
            cacheLog.error("建立SSH连接失败: %s", e.getMessage());
            cacheLog.error("异常详情: %s", e.toString());

            // 获取错误堆栈并记录
            try {
                java.io.StringWriter sw = new java.io.StringWriter();
                java.io.PrintWriter pw = new java.io.PrintWriter(sw);
                e.printStackTrace(pw);
                cacheLog.error("错误堆栈: %s", sw.toString());
            } catch (Exception ex) {
                // 忽略获取堆栈时的错误
            }

            // 确保session为null
            session = null;

            // 再抛出异常给上层处理
            throw new RuntimeException("打开SSH连接失败: " + e.getMessage(), e);
        }
    }

    protected void closeSession() {
        if (session != null) {
            try {
                // 从当前上下文中获取主机信息
                HostInfo currentHostInfo = getCurrentHostInfo();
                String formattedAddress = formatAddress(currentHostInfo);

                logger.debug("正在关闭SSH会话: {}", formattedAddress);
                cacheLog.debug("正在关闭SSH会话: {}", formattedAddress);

                long startTime = System.currentTimeMillis();
                session.close();
                long endTime = System.currentTimeMillis();

                logger.debug("SSH会话关闭成功，耗时: {}ms", (endTime - startTime));
                cacheLog.debug("SSH会话关闭成功，耗时: %dms", (endTime - startTime));
            } catch (java.io.IOException e) {
                logger.error("关闭SSH会话异常: {}", e.getMessage(), e);
                cacheLog.error("关闭SSH会话异常: %s", e.getMessage());
            } catch (Exception e) {
                logger.error("关闭SSH会话时发生未预期的异常: {}", e.getMessage(), e);
                cacheLog.error("关闭SSH会话时发生未预期的异常: %s", e.getMessage());
            } finally {
                session = null;
            }
        }
    }

    @Override
    public final CheckItem check(Integer clusterId, HostInfo hostInfo, CheckItem checkItem) throws Exception {
        // 优先使用AsyncCheckService中的连接复用方法
        // 只有当外部未提供复用机制时才创建新连接
        if (hostInfo != null && hostInfo.isUseExistingSession()) {
            return checkWithExistingSession(clusterId, hostInfo, checkItem);
        }

        hostInfo.setClusterId(clusterId);
        logger.info("开始检查项: {}, 主机: {}, 检查项ID: {}", checkItem.getItemName(), hostInfo.getIp(), checkItem.getId());

        // 设置为检查操作
        operationType = LogEntry.Type.CHECK;

        // 设置当前检查项的日志缓存键
        setCurrentLogKey(clusterId, hostInfo.getIp(), checkItem.getId());

        // 更新日志记录器的类型
        CheckLogger.LoggerImpl loggerImpl = (CheckLogger.LoggerImpl) this.cacheLog;
        loggerImpl.setLogType(operationType);

        // 先将状态设置为检查中
        checkItem.setStatus(CheckItem.Status.CHECKING);
        checkItem.setMessage("检查中...");
        updateCheckStatus(clusterId, hostInfo, checkItem);

        try {
            // 设置当前主机信息
            setCurrentHostInfo(hostInfo);

            logger.info("开始建立SSH连接到主机: {}, 端口: {}, 用户: {}",
                    hostInfo.getIp(), hostInfo.getSshPort(), hostInfo.getSshUser());
            cacheLog.info("开始建立SSH连接到主机: %s, 端口: %d, 用户: %s",
                    hostInfo.getIp(), hostInfo.getSshPort(), hostInfo.getSshUser());

            try {
                openSession(hostInfo);
            } catch (Exception e) {
                logger.error("SSH连接失败: {}", e.getMessage(), e);
                cacheLog.error("SSH连接失败: %s", e.getMessage());

                // 明确设置状态为失败
                checkItem.setStatus(CheckItem.Status.FAILED);
                checkItem.setMessage("无法建立SSH连接: " + e.getMessage());
                updateCheckStatus(clusterId, hostInfo, checkItem);

                // 记录详细的状态信息
                logger.info("检查项 {} 状态已设置为FAILED, 消息: {}",
                        checkItem.getItemName(), checkItem.getMessage());
                cacheLog.info("检查项状态已设置为FAILED, 详细信息: %s", checkItem.getMessage());

                return checkItem;
            }

            // 明确检查session是否成功建立 - 增强处理
            if (session == null) {
                String errorMsg = "无法建立SSH连接到主机: " + hostInfo.getIp();
                logger.error(errorMsg);
                cacheLog.error(errorMsg);

                // 确保状态被设置为失败
                checkItem.setStatus(CheckItem.Status.FAILED);
                checkItem.setMessage("无法建立SSH连接");

                // 立即更新缓存状态
                updateCheckStatus(clusterId, hostInfo, checkItem);

                // 记录详细的状态信息
                logger.info("检查项 {} 状态已设置为FAILED (session为null), 消息: {}",
                        checkItem.getItemName(), checkItem.getMessage());
                cacheLog.info("检查项状态已设置为FAILED (session为null), 详细信息: %s",
                        checkItem.getMessage());

                return checkItem;
            }

            logger.info("成功连接到主机: {}, 开始执行检查项: {}", hostInfo.getIp(), checkItem.getItemName());

            try {
                // 确保cacheLog记录日志
                cacheLog.info("开始执行检查 %s...", checkItem.getItemName());

                // 执行具体检查逻辑，确保捕获InterruptedException
                try {
                    doCheck(hostInfo, checkItem);
                    // 添加日志确认状态
                    logger.info("doCheck执行后检查项状态: {}, 消息: {}", checkItem.getStatus(), checkItem.getMessage());
                    // 立即更新一次状态
                    updateCheckStatus(clusterId, hostInfo, checkItem);
                } catch (InterruptedException e) {
                    // 捕获中断异常
                    logger.info("检查项在执行过程中被中断: {}", checkItem.getItemName());
                    cacheLog.info("检查项在执行过程中被中断");
                    checkItem.setStatus(CheckItem.Status.SKIPPED);
                    checkItem.setMessage("检查已终止");
                    updateCheckStatus(clusterId, hostInfo, checkItem);
                    Thread.currentThread().interrupt(); // 重置中断状态
                    return checkItem;
                }

                // 特殊检查：如果doCheck执行完成后状态仍为CHECKING，则强制设置为FAILED
                if (checkItem.getStatus() == CheckItem.Status.CHECKING) {
                    logger.warn("检查项 {} 执行完毕但状态仍为CHECKING，强制设置为FAILED",
                            checkItem.getItemName());
                    cacheLog.warn("检查执行完毕但状态未更新，强制设置为失败");

                    checkItem.setStatus(CheckItem.Status.FAILED);
                    checkItem.setMessage("检查执行过程中状态未正确更新");
                    updateCheckStatus(clusterId, hostInfo, checkItem);
                }

                logger.info("检查项 {} 执行完成, 状态: {}, 消息: {}",
                        checkItem.getItemName(), checkItem.getStatus(), checkItem.getMessage());
            } catch (Exception e) {
                logger.error("执行检查项 {} 时发生异常: {}", checkItem.getItemName(), e.getMessage(), e);
                checkItem.setStatus(CheckItem.Status.FAILED);
                checkItem.setMessage("检查执行异常: " + e.getMessage());
                updateCheckStatus(clusterId, hostInfo, checkItem);
            }
        } catch (Exception e) {
            logger.error("连接主机 {} 时发生异常: {}", hostInfo.getIp(), e.getMessage(), e);
            checkItem.setStatus(CheckItem.Status.FAILED);
            checkItem.setMessage("连接主机失败: " + e.getMessage());
            updateCheckStatus(clusterId, hostInfo, checkItem);
        } finally {
            if (session != null && !hostInfo.isUseExistingSession()) {
                // 只有当连接是由当前方法创建时才关闭它
                logger.info("正在关闭到主机 {} 的SSH连接", hostInfo.getIp());
                closeSession();
                logger.info("已关闭到主机 {} 的SSH连接", hostInfo.getIp());
            } else if (session != null) {
                // 连接是外部提供的，不关闭
                logger.debug("不关闭SSH连接，由外部管理: {}", hostInfo.getIp());
                session = null; // 仅清除引用
            }
            // 清理当前主机信息
            clearCurrentHostInfo();
        }

        // 最后更新一次状态，确保前端能看到最终结果
        updateCheckStatus(clusterId, hostInfo, checkItem);
        logger.info("检查项 {} 最终状态: {}, 消息: {}",
                checkItem.getItemName(), checkItem.getStatus(), checkItem.getMessage());

        return checkItem;
    }

    @Override
    public boolean fix(Integer clusterId, HostInfo hostInfo, CheckItem checkItem) throws Exception {
        // 优先使用AsyncCheckService中的连接复用方法
        // 只有当外部未提供复用机制时才创建新连接
        if (hostInfo != null && hostInfo.isUseExistingSession()) {
            return fixWithExistingSession(clusterId, hostInfo, checkItem);
        }

        hostInfo.setClusterId(clusterId);
        logger.info("开始修复检查项: {}, 主机: {}, 检查项ID: {}", checkItem.getItemName(), hostInfo.getIp(),
                checkItem.getId());

        // 设置为修复操作
        operationType = LogEntry.Type.FIX;

        // 设置当前检查项的日志缓存键
        setCurrentLogKey(clusterId, hostInfo.getIp(), checkItem.getId());

        // 更新日志记录器的类型
        CheckLogger.LoggerImpl loggerImpl = (CheckLogger.LoggerImpl) this.cacheLog;
        loggerImpl.setLogType(operationType);

        // 记录修复开始
        cacheLog.info("===============================================");
        cacheLog.info("开始修复检查项: " + checkItem.getItemName());
        cacheLog.info("主机: " + hostInfo.getIp());
        cacheLog.info("检查项ID: " + checkItem.getId());
        cacheLog.info("开始时间: " + getCurrentTime());
        cacheLog.info("===============================================");

        try {
            // 设置当前主机信息
            setCurrentHostInfo(hostInfo);

            // 设置状态为修复中
            checkItem.setStatus(CheckItem.Status.FIXING);
            checkItem.setMessage("正在修复...");
            updateCheckStatus(clusterId, hostInfo, checkItem);

            // 建立SSH连接
            cacheLog.info("正在建立SSH连接...");
            openSession(hostInfo);

            if (session == null) {
                String errorMsg = "无法建立SSH连接到主机: " + hostInfo.getIp();
                logger.error(errorMsg);
                cacheLog.error("错误: " + errorMsg);
                cacheLog.error("修复失败: 无法连接到主机");

                // 更新状态为失败
                checkItem.setStatus(CheckItem.Status.FAILED);
                checkItem.setMessage("修复失败: 无法连接到主机");
                updateCheckStatus(clusterId, hostInfo, checkItem);

                return false;
            }

            cacheLog.info("SSH连接建立成功，开始执行修复操作");

            // 执行具体修复逻辑
            boolean doFixResult = false;
            try {
                cacheLog.info("正在执行修复逻辑...");
                doFixResult = doFix(hostInfo, checkItem);
                cacheLog.info("修复逻辑执行" + (doFixResult ? "成功" : "失败"));

                // 更新状态
                if (doFixResult) {
                    checkItem.setStatus(CheckItem.Status.SUCCESS);
                    checkItem.setMessage("修复成功");
                } else {
                    checkItem.setStatus(CheckItem.Status.FAILED);
                    checkItem.setMessage("修复失败");
                }
                updateCheckStatus(clusterId, hostInfo, checkItem);

            } catch (Exception e) {
                String errorMsg = "执行修复逻辑时发生异常: " + e.getMessage();
                logger.error(errorMsg, e);
                cacheLog.error("错误: " + errorMsg);

                // 更新状态为失败
                checkItem.setStatus(CheckItem.Status.FAILED);
                checkItem.setMessage("修复异常: " + e.getMessage());
                updateCheckStatus(clusterId, hostInfo, checkItem);

                return false;
            }

            // 再次检查，验证修复结果
            try {
                cacheLog.info("正在验证修复结果...");
                CheckItem checkResult = doCheck(hostInfo, checkItem);
                boolean verified = checkResult.getStatus() == CheckItem.Status.SUCCESS;
                cacheLog.info("验证结果: " + (verified ? "成功" : "失败"));
                cacheLog.info("验证信息: " + checkResult.getMessage());

                // 如果验证失败但修复成功，添加警告信息但不改变修复结果
                if (!verified && doFixResult) {
                    cacheLog.warn("警告: 修复操作成功完成，但验证检查未通过。这可能需要手动干预或重新检查。");
                }

            } catch (Exception e) {
                String errorMsg = "验证修复结果时发生异常: " + e.getMessage();
                logger.error(errorMsg, e);
                cacheLog.warn("警告: " + errorMsg);
                // 不因为验证异常而影响修复结果
            }

            // 关闭会话
            cacheLog.info("正在关闭SSH连接...");
            closeSession();
            cacheLog.info("SSH连接已关闭");

            // 记录最终结果
            cacheLog.info("修复操作" + (doFixResult ? "成功完成" : "失败"));

            return doFixResult;
        } catch (Exception e) {
            String errorMsg = "修复过程中发生异常: " + e.getMessage();
            logger.error(errorMsg, e);
            cacheLog.error("错误: " + errorMsg);

            // 确保会话被关闭
            if (session != null) {
                cacheLog.info("正在关闭SSH连接...");
                closeSession();
                cacheLog.info("SSH连接已关闭");
            }

            // 更新状态为失败
            checkItem.setStatus(CheckItem.Status.FAILED);
            checkItem.setMessage("修复异常: " + e.getMessage());
            updateCheckStatus(clusterId, hostInfo, checkItem);

            return false;
        } finally {
            // 检查连接是否由外部提供
            if (session != null && !hostInfo.isUseExistingSession()) {
                // 只有当连接是由当前方法创建时才关闭它
                cacheLog.info("正在关闭SSH连接...");
                closeSession();
                cacheLog.info("SSH连接已关闭");
            } else if (session != null) {
                // 连接是外部提供的，不关闭
                logger.debug("不关闭SSH连接，由外部管理: {}", hostInfo.getIp());
                session = null; // 仅清除引用
            }

            // 记录修复结束
            cacheLog.info("===============================================");
            cacheLog.info("修复操作结束");
            cacheLog.info("结束时间: " + getCurrentTime());
            cacheLog.info("===============================================");
            // 清理当前主机信息
            clearCurrentHostInfo();
        }
    }

    /**
     * 通过调试日志监控会话状态检查
     */
    private void logSessionStatus(HostInfo hostInfo) {
        if (hostInfo != null) {
            logger.debug("会话状态检查: useExistingSession={}, externalSession={}",
                    hostInfo.isUseExistingSession(),
                    hostInfo.getExternalSession() != null ? "已设置" : "未设置");
        }
    }

    /**
     * 使用现有会话进行检查的处理
     */
    private CheckItem checkWithExistingSession(Integer clusterId, HostInfo hostInfo, CheckItem checkItem) {
        try {
            // 设置初始状态
            hostInfo.setClusterId(clusterId);
            logger.info("使用已存在的SSH会话进行检查: {}, 主机: {}, 检查项ID: {}",
                    checkItem.getItemName(), hostInfo.getIp(), checkItem.getId());

            // 添加会话状态日志
            logSessionStatus(hostInfo);

            // 设置为检查操作
            operationType = LogEntry.Type.CHECK;

            // 设置当前检查项的日志缓存键
            setCurrentLogKey(clusterId, hostInfo.getIp(), checkItem.getId());

            // 更新日志记录器的类型
            CheckLogger.LoggerImpl loggerImpl = (CheckLogger.LoggerImpl) this.cacheLog;
            loggerImpl.setLogType(operationType);

            // 先将状态设置为检查中
            checkItem.setStatus(CheckItem.Status.CHECKING);
            checkItem.setMessage("检查中...");
            updateCheckStatus(clusterId, hostInfo, checkItem);

            // 设置当前主机信息
            setCurrentHostInfo(hostInfo);

            // 等待外部提供的Session可用
            int retryCount = 0;
            int maxRetries = 3; // 减少重试次数，不必等待太久
            while (!hostInfo.isSessionReady() && retryCount < maxRetries) {
                logger.debug("等待外部会话变为可用状态，重试次数: {}, isSessionReady={}, externalSession={}",
                        retryCount,
                        hostInfo.isSessionReady(),
                        hostInfo.getExternalSession() != null ? "存在" : "不存在");
                Thread.sleep(100); // 减少等待时间
                retryCount++;
                // 再次记录会话状态
                logSessionStatus(hostInfo);
            }

            if (!hostInfo.isSessionReady()) {
                logger.error("等待外部Session超时，无法进行检查，useExistingSession={}, externalSession={}, 主机: {}",
                        hostInfo.isUseExistingSession(),
                        hostInfo.getExternalSession() != null ? "已设置" : "未设置",
                        hostInfo.getIp());
                checkItem.setStatus(CheckItem.Status.FAILED);
                checkItem.setMessage("无法获取SSH会话");
                updateCheckStatus(clusterId, hostInfo, checkItem);
                return checkItem;
            }

            // 设置Session
            session = hostInfo.getExternalSession();

            // 确保Session有效
            if (session == null || !session.isOpen()) {
                logger.error("外部提供的SSH会话无效");
                checkItem.setStatus(CheckItem.Status.FAILED);
                checkItem.setMessage("SSH会话无效");
                updateCheckStatus(clusterId, hostInfo, checkItem);
                return checkItem;
            }

            // 执行检查
            logger.info("使用外部会话执行检查项: {}", checkItem.getItemName());
            cacheLog.info("开始执行检查 %s...", checkItem.getItemName());

            try {
                doCheck(hostInfo, checkItem);
                logger.info("doCheck执行后检查项状态: {}, 消息: {}",
                        checkItem.getStatus(), checkItem.getMessage());
                updateCheckStatus(clusterId, hostInfo, checkItem);
            } catch (InterruptedException e) {
                logger.info("检查项在执行过程中被中断: {}", checkItem.getItemName());
                cacheLog.info("检查项在执行过程中被中断");
                checkItem.setStatus(CheckItem.Status.SKIPPED);
                checkItem.setMessage("检查已终止");
                updateCheckStatus(clusterId, hostInfo, checkItem);
                Thread.currentThread().interrupt();
                return checkItem;
            }

            // 特殊检查：如果doCheck执行完成后状态仍为CHECKING，则强制设置为FAILED
            if (checkItem.getStatus() == CheckItem.Status.CHECKING) {
                logger.warn("检查项 {} 执行完毕但状态仍为CHECKING，强制设置为FAILED",
                        checkItem.getItemName());
                cacheLog.warn("检查执行完毕但状态未更新，强制设置为失败");

                checkItem.setStatus(CheckItem.Status.FAILED);
                checkItem.setMessage("检查执行过程中状态未正确更新");
                updateCheckStatus(clusterId, hostInfo, checkItem);
            }

            logger.info("检查项 {} 执行完成, 状态: {}, 消息: {}",
                    checkItem.getItemName(), checkItem.getStatus(), checkItem.getMessage());
        } catch (Exception e) {
            logger.error("执行检查 {} 时发生异常: {}", checkItem.getItemName(), e.getMessage(), e);
            checkItem.setStatus(CheckItem.Status.FAILED);
            checkItem.setMessage("检查失败: " + e.getMessage());
            updateCheckStatus(clusterId, hostInfo, checkItem);
        } finally {
            // 不关闭会话，由外部管理
            this.session = null;
            clearCurrentHostInfo();
        }

        updateCheckStatus(clusterId, hostInfo, checkItem);
        logger.info("检查项 {} 最终状态: {}, 消息: {}",
                checkItem.getItemName(), checkItem.getStatus(), checkItem.getMessage());

        return checkItem;
    }

    /**
     * 使用现有会话进行修复的处理
     */
    private boolean fixWithExistingSession(Integer clusterId, HostInfo hostInfo, CheckItem checkItem) {
        try {
            // 设置初始状态
            hostInfo.setClusterId(clusterId);
            logger.info("使用已存在的SSH会话进行修复: {}, 主机: {}, 检查项ID: {}",
                    checkItem.getItemName(), hostInfo.getIp(), checkItem.getId());

            // 设置为修复操作
            operationType = LogEntry.Type.FIX;

            // 设置当前检查项的日志缓存键
            setCurrentLogKey(clusterId, hostInfo.getIp(), checkItem.getId());

            // 更新日志记录器的类型
            CheckLogger.LoggerImpl loggerImpl = (CheckLogger.LoggerImpl) this.cacheLog;
            loggerImpl.setLogType(operationType);

            // 记录修复开始
            cacheLog.info("===============================================");
            cacheLog.info("开始修复检查项: " + checkItem.getItemName());
            cacheLog.info("主机: " + hostInfo.getIp());
            cacheLog.info("检查项ID: " + checkItem.getId());
            cacheLog.info("开始时间: " + getCurrentTime());
            cacheLog.info("使用已存在的SSH会话");
            cacheLog.info("===============================================");

            // 设置当前主机信息
            setCurrentHostInfo(hostInfo);

            // 设置状态为修复中
            checkItem.setStatus(CheckItem.Status.FIXING);
            checkItem.setMessage("正在修复...");
            updateCheckStatus(clusterId, hostInfo, checkItem);

            // 等待外部提供的Session可用
            int retryCount = 0;
            int maxRetries = 10;
            while (!hostInfo.isSessionReady() && retryCount < maxRetries) {
                Thread.sleep(500); // 等待外部设置Session
                retryCount++;
            }

            if (!hostInfo.isSessionReady()) {
                logger.error("等待外部Session超时，无法进行修复");
                checkItem.setStatus(CheckItem.Status.FAILED);
                checkItem.setMessage("无法获取SSH会话");
                updateCheckStatus(clusterId, hostInfo, checkItem);
                return false;
            }

            // 设置Session
            session = hostInfo.getExternalSession();

            // 确保Session有效
            if (session == null || !session.isOpen()) {
                logger.error("外部提供的SSH会话无效");
                cacheLog.error("外部提供的SSH会话无效");

                checkItem.setStatus(CheckItem.Status.FAILED);
                checkItem.setMessage("SSH会话无效");
                updateCheckStatus(clusterId, hostInfo, checkItem);
                return false;
            }

            // 执行修复
            boolean doFixResult = false;
            try {
                cacheLog.info("正在执行修复逻辑...");
                doFixResult = doFix(hostInfo, checkItem);
                cacheLog.info("修复逻辑执行" + (doFixResult ? "成功" : "失败"));

                // 更新状态
                if (doFixResult) {
                    checkItem.setStatus(CheckItem.Status.SUCCESS);
                    checkItem.setMessage("修复成功");
                } else {
                    checkItem.setStatus(CheckItem.Status.FAILED);
                    checkItem.setMessage("修复失败");
                }
                updateCheckStatus(clusterId, hostInfo, checkItem);
            } catch (Exception e) {
                String errorMsg = "执行修复逻辑时发生异常: " + e.getMessage();
                logger.error(errorMsg, e);
                cacheLog.error("错误: " + errorMsg);

                checkItem.setStatus(CheckItem.Status.FAILED);
                checkItem.setMessage("修复异常: " + e.getMessage());
                updateCheckStatus(clusterId, hostInfo, checkItem);

                return false;
            }

            // 验证修复结果
            try {
                cacheLog.info("正在验证修复结果...");
                CheckItem checkResult = doCheck(hostInfo, checkItem);
                boolean verified = checkResult.getStatus() == CheckItem.Status.SUCCESS;
                cacheLog.info("验证结果: " + (verified ? "成功" : "失败"));
                cacheLog.info("验证信息: " + checkResult.getMessage());

                if (!verified && doFixResult) {
                    cacheLog.warn("警告: 修复操作成功完成，但验证检查未通过。这可能需要手动干预或重新检查。");
                }
            } catch (Exception e) {
                String errorMsg = "验证修复结果时发生异常: " + e.getMessage();
                logger.error(errorMsg, e);
                cacheLog.warn("警告: " + errorMsg);
            }

            // 记录最终结果
            cacheLog.info("修复操作" + (doFixResult ? "成功完成" : "失败"));

            return doFixResult;
        } catch (Exception e) {
            String errorMsg = "修复过程中发生异常: " + e.getMessage();
            logger.error(errorMsg, e);
            cacheLog.error("错误: " + errorMsg);

            // 更新状态为失败
            checkItem.setStatus(CheckItem.Status.FAILED);
            checkItem.setMessage("修复异常: " + e.getMessage());
            updateCheckStatus(clusterId, hostInfo, checkItem);

            return false;
        } finally {
            // 不关闭会话，由外部管理
            this.session = null;

            // 记录修复结束
            cacheLog.info("===============================================");
            cacheLog.info("修复操作结束");
            cacheLog.info("结束时间: " + getCurrentTime());
            cacheLog.info("===============================================");

            // 清理当前主机信息
            clearCurrentHostInfo();
        }
    }

    /**
     * 执行具体的检查逻辑
     * 
     * @throws InterruptedException 如果检查过程被中断
     */
    protected abstract CheckItem doCheck(HostInfo hostInfo, CheckItem checkItem) throws InterruptedException;

    /**
     * 执行具体的修复逻辑
     */
    protected abstract boolean doFix(HostInfo hostInfo, CheckItem checkItem);

    private void updateCheckStatus(Integer clusterId, HostInfo hostInfo, CheckItem checkItem) {
        String cacheKey = clusterId + Constants.HOST_MAP;
        logger.debug("更新检查状态: 主机={}, 检查项ID={}, 状态={}, 消息={}",
                hostInfo.getIp(), checkItem.getId(), checkItem.getStatus(), checkItem.getMessage());

        try {
            // 记录更新前的状态
            logger.info("正在更新检查项状态 - 主机: {}, 检查项: {}, 当前状态: {}, 新状态: {}",
                    hostInfo.getIp(), checkItem.getItemName(),
                    "更新前", checkItem.getStatus());

            Map<String, HostInfo> hostInfoMap = (Map<String, HostInfo>) CacheUtils.get(cacheKey);
            if (hostInfoMap != null) {
                HostInfo cachedHostInfo = hostInfoMap.get(hostInfo.getIp());
                if (cachedHostInfo != null) {
                    boolean updated = false;
                    for (CheckItem item : cachedHostInfo.getCheckItems()) {
                        if (item.getId().equals(checkItem.getId())) {
                            // 记录状态变化
                            logger.info("检查项状态变更: {} -> {}, 消息: {} -> {}",
                                    item.getStatus(), checkItem.getStatus(),
                                    item.getMessage(), checkItem.getMessage());

                            item.setStatus(checkItem.getStatus());
                            item.setMessage(checkItem.getMessage());
                            updated = true;
                            logger.debug("检查项状态已更新: ID={}, 新状态={}", item.getId(), item.getStatus());
                            break;
                        }
                    }

                    if (!updated) {
                        logger.warn("未找到要更新的检查项: 主机={}, 检查项ID={}", hostInfo.getIp(), checkItem.getId());
                    } else {
                        // 更新主机的整体状态（根据检查项状态计算）
                        cachedHostInfo.calculateStatus();
                        hostInfoMap.put(hostInfo.getIp(), cachedHostInfo);
                        CacheUtils.put(cacheKey, hostInfoMap);
                        logger.debug("缓存已更新: cacheKey={}, 主机状态={}",
                                cacheKey, cachedHostInfo.getStatus());
                    }
                } else {
                    logger.warn("缓存中未找到主机信息: hostname={}", hostInfo.getIp());
                }
            } else {
                logger.warn("缓存中未找到主机映射: cacheKey={}", cacheKey);
            }
        } catch (Exception e) {
            logger.error("更新检查状态时发生异常: {}", e.getMessage(), e);
            // 记录更多异常信息
            cacheLog.error("更新检查状态失败，请检查系统日志: %s", e.getMessage());
        }
    }

    /**
     * 创建日志记录器
     * 
     * @param clusterId     集群ID
     * @param hostname      主机名
     * @param itemId        检查项ID
     * @param operationType 操作类型
     * @return 日志记录器
     */
    protected CheckLogger createLogger(Integer clusterId, String hostname, Integer itemId,
            LogEntry.Type operationType) {
        String logKey = String.format("%s%d_%s_%d", CHECK_ITEM_LOG_PREFIX, clusterId, hostname, itemId);
        return CheckLogger.createLogger(logKey, getClass().getSimpleName(), operationType);
    }

    // 设置当前主机信息
    protected void setCurrentHostInfo(HostInfo hostInfo) {
        currentHostInfo.set(hostInfo);
    }

    // 获取当前主机信息
    protected HostInfo getCurrentHostInfo() {
        return currentHostInfo.get();
    }

    // 清理当前主机信息
    protected void clearCurrentHostInfo() {
        currentHostInfo.remove();
    }

    /**
     * 设置检查项消息并立即更新状态
     * 这个方法确保消息更新能够实时同步到前端
     * 
     * @param hostInfo  主机信息
     * @param checkItem 检查项
     * @param message   要设置的消息
     */

    protected void setCheckItemMessage(HostInfo hostInfo, CheckItem checkItem, String message) {
        checkItem.setMessage(message);
        logger.debug("正在实时更新检查状态消息: {}", message);
        // 立即更新状态
        updateCheckStatus(hostInfo.getClusterId(), hostInfo, checkItem);
    }

    /**
     * 格式化HTML样式的检查结果消息
     * 便于所有检查器统一使用美化的HTML样式
     * 
     * @param hostInfo       主机信息
     * @param checkItem      检查项
     * @param isSuccess      是否检查成功
     * @param titleText      标题文本
     * @param detailsBuilder HTML格式的详细内容构建器
     */
    protected void setStyledHtmlMessage(HostInfo hostInfo, CheckItem checkItem, boolean isSuccess,
            String titleText, StringBuilder detailsBuilder) {

        StringBuilder html = new StringBuilder();

        // 开始HTML容器
        html.append(HtmlStyleHelper.beginContainer());

        // 添加标题
        html.append(HtmlStyleHelper.generateTitle(titleText, isSuccess));

        // 添加主机基本信息组
        html.append(HtmlStyleHelper.beginGroup());
        html.append(HtmlStyleHelper.generatePropertyRow("主机", hostInfo.getIp(), HtmlStyleHelper.Colors.INFO));
        html.append(HtmlStyleHelper.generatePropertyRow("IP地址", hostInfo.getIp(), HtmlStyleHelper.Colors.INFO));
        html.append(
                HtmlStyleHelper.generatePropertyRow("检查时间", getCurrentTime(), HtmlStyleHelper.Colors.GRAY));
        html.append(HtmlStyleHelper.endGroup());

        // 添加详细内容
        html.append(detailsBuilder.toString());

        // 结束HTML容器
        html.append(HtmlStyleHelper.endContainer());

        // 设置检查项消息
        setCheckItemMessage(hostInfo, checkItem, html.toString());
    }

    /**
     * 获取操作系统信息
     */
    protected boolean collectOsInfo(HostInfo hostInfo, ClientSession clientSession) {
        try {
            if (clientSession == null) {
                logger.error("SSH会话为空，无法收集操作系统信息");
                // 设置失败消息
                hostInfo.setMessage("SSH会话为空，无法收集操作系统信息");
                return false;
            }

            logger.info("开始收集主机 {} 的操作系统信息", hostInfo.getIp());

            // 由于HostInfo已经包含了SSH会话信息，可以直接调用异步方法，让OsInfoService自己处理会话管理
            // 将数据设置到HostInfo对象，并通过异步过程更新缓存
            osInfoService.getHostOsInfoAsync(hostInfo);

            // 设置成功消息
            logger.info("主机 {} 操作系统信息收集请求已提交", hostInfo.getIp());
            return true;
        } catch (Exception e) {
            logger.error("提交操作系统信息收集请求时出错: {}", e.getMessage(), e);

            // 设置失败消息
            hostInfo.setMessage("提交操作系统信息收集请求失败: " + e.getMessage());
            return false;
        }
    }
}