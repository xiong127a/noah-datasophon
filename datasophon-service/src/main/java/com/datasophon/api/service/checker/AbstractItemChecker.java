package com.datasophon.api.service.checker;

import com.datasophon.api.utils.MinaUtils;
import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.model.CheckItem;
import com.datasophon.common.model.HostInfo;
import com.datasophon.common.model.ItemCode;
import com.datasophon.common.model.LogEntry;
import org.apache.sshd.client.session.ClientSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public abstract class AbstractItemChecker implements ItemChecker {
    private static final Logger logger = LoggerFactory.getLogger(AbstractItemChecker.class);
    private static final String CHECK_TASK_STATUS_PREFIX = "CHECK_TASK_STATUS_";
    private static final String CHECK_ITEM_LOG_PREFIX = "CHECK_ITEM_LOG_";

    protected ClientSession session;
    // 当前检查项的日志缓存键
    protected String currentLogKey;
    
    /**
     * 基于检查项的日志实现，同时写入缓存和控制台日志
     */
    protected class CacheItemLogger implements CheckLogger {
        private final String className;
        private final Logger slf4jLogger;

        public CacheItemLogger() {
            this.className = AbstractItemChecker.this.getClass().getSimpleName();
            this.slf4jLogger = LoggerFactory.getLogger(AbstractItemChecker.this.getClass());
        }

        private void addLogEntry(String levelStr, String message) {
            if (currentLogKey != null) {
                try {
                    // 创建结构化日志记录
                    Date timestamp = new Date();
                    String threadName = Thread.currentThread().getName();
                    LogEntry.Level level = LogEntry.Level.valueOf(levelStr);
                    
                    LogEntry logEntry = new LogEntry(timestamp, level, threadName, className, message);
                    
                    // 添加到日志管理器
                    LogEntryManager.addLogEntry(currentLogKey, logEntry);
                    
                    // 同时输出到标准日志
                    switch (level) {
                        case INFO:
                            slf4jLogger.info(message);
                            break;
                        case WARN:
                            slf4jLogger.warn(message);
                            break;
                        case ERROR:
                            slf4jLogger.error(message);
                            break;
                        case DEBUG:
                            slf4jLogger.debug(message);
                            break;
                    }
                } catch (Exception e) {
                    slf4jLogger.error("添加日志记录失败: {}", e.getMessage(), e);
                }
            } else {
                // 如果没有设置日志键，只输出到标准日志
                slf4jLogger.debug("缓存日志未存储(currentLogKey为空): {}", message);
            }
        }

        @Override
        public void info(String message) {
            addLogEntry("INFO", message);
        }

        @Override
        public void info(String format, Object... args) {
            String message = String.format(format, args);
            addLogEntry("INFO", message);
        }

        @Override
        public void warn(String message) {
            addLogEntry("WARN", message);
        }

        @Override
        public void warn(String format, Object... args) {
            String message = String.format(format, args);
            addLogEntry("WARN", message);
        }

        @Override
        public void error(String message) {
            addLogEntry("ERROR", message);
        }

        @Override
        public void error(String format, Object... args) {
            String message = String.format(format, args);
            addLogEntry("ERROR", message);
        }

        @Override
        public void debug(String message) {
            addLogEntry("DEBUG", message);
        }

        @Override
        public void debug(String format, Object... args) {
            String message = String.format(format, args);
            addLogEntry("DEBUG", message);
        }
    }
    
    // 供子类使用的日志记录器实例，同时记录到缓存和控制台
    protected final CheckLogger cacheLog = new CacheItemLogger();

    // 设置当前检查项的日志缓存键
    protected void setCurrentLogKey(Integer clusterId, String hostname, Integer itemId) {
        this.currentLogKey = CHECK_ITEM_LOG_PREFIX + clusterId + "_" + hostname + "_" + itemId;
    }
    
    /**
     * 格式化日期为中文格式
     * @param date 日期对象
     * @return 格式化后的中文日期字符串
     */
    protected String formatDateToChinese(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日 HH时mm分ss秒");
        return sdf.format(date);
    }

    /**
     * 获取当前时间的中文格式
     * @return 当前时间的中文格式字符串
     */
    protected String getCurrentTimeInChinese() {
        return formatDateToChinese(new Date());
    }

    protected String execCommand(ClientSession session, String command) throws InterruptedException {
        // 检查线程是否已被中断
        if (Thread.currentThread().isInterrupted()) {
            logger.info("命令执行被中断: {}", command);
            cacheLog.info("命令执行被中断: %s", command);
            throw new InterruptedException("命令执行被中断");
        }
        
        try {
            if (session == null) {
                logger.error("SSH会话为空，无法执行命令");
                cacheLog.error("SSH会话为空，无法执行命令");
                return "ERROR: SSH会话为空";
            }
            
            logger.debug("准备执行命令: {} 在主机: {}", command, session.getConnectAddress());
            cacheLog.debug("准备执行命令: %s 在主机: %s", command, session.getConnectAddress());
            
            // 创建执行命令的通道
            try (org.apache.sshd.client.channel.ClientChannel channel = session.createExecChannel(command)) {
                logger.debug("命令通道已创建，正在打开通道");
                cacheLog.debug("命令通道已创建，正在打开通道");
                
                // 再次检查线程是否已被中断
                if (Thread.currentThread().isInterrupted()) {
                    logger.info("命令通道创建后执行被中断: {}", command);
                    cacheLog.info("命令通道创建后执行被中断: %s", command);
                    throw new InterruptedException("命令执行被中断");
                }
                
                // 启动命令
                channel.open().verify(30, TimeUnit.SECONDS);
                logger.debug("命令通道已打开，开始执行命令");
                cacheLog.debug("命令通道已打开，开始执行命令");
                
                // 读取命令输出
                java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
                java.io.ByteArrayOutputStream err = new java.io.ByteArrayOutputStream();
                channel.setOut(out);
                channel.setErr(err);
                
                // 等待命令完成
                long waitTime = TimeUnit.SECONDS.toMillis(30);
                logger.debug("等待命令执行完成，超时时间: {}ms", waitTime);
                cacheLog.debug("等待命令执行完成，超时时间: %dms", waitTime);
                
                long startTime = System.currentTimeMillis();
                long remainingTime = waitTime;
                
                // 使用分段等待，每秒检查一次线程是否被中断
                while (remainingTime > 0) {
                    // 检查线程是否已被中断
                    if (Thread.currentThread().isInterrupted()) {
                        logger.info("命令执行等待过程中被中断: {}", command);
                        cacheLog.info("命令执行等待过程中被中断: %s", command);
                        throw new InterruptedException("命令执行被中断");
                    }
                    
                    // 等待较短的时间，这样可以更频繁地检查中断状态
                    long waitSegment = Math.min(1000, remainingTime);
                    
                    java.util.Set<org.apache.sshd.client.channel.ClientChannelEvent> events = 
                        channel.waitFor(java.util.EnumSet.of(
                            org.apache.sshd.client.channel.ClientChannelEvent.CLOSED,
                            org.apache.sshd.client.channel.ClientChannelEvent.EOF), waitSegment);
                    
                    // 如果命令已完成，则退出循环
                    if (events != null && !events.isEmpty()) {
                        break;
                    }
                    
                    // 更新剩余时间
                    remainingTime -= waitSegment;
                }
                
                long endTime = System.currentTimeMillis();
                
                // 如果时间用完了还没有完成，则视为超时
                if (System.currentTimeMillis() - startTime >= waitTime) {
                    logger.warn("命令执行超时: {}, 主机: {}, 已等待时间: {}ms", 
                        command, session.getConnectAddress(), (endTime - startTime));
                    cacheLog.warn("命令执行超时: %s, 主机: %s, 已等待时间: %dms", 
                        command, session.getConnectAddress(), (endTime - startTime));
                    return "ERROR: 命令执行超时，请检查网络或主机状态";
                }
                
                logger.debug("命令执行完成，耗时: {}ms", (endTime - startTime));
                cacheLog.debug("命令执行完成，耗时: %dms", (endTime - startTime));
                
                // 获取命令执行的退出状态
                Integer exitStatus = channel.getExitStatus();
                if (exitStatus != null && exitStatus != 0) {
                    String errorMsg = err.toString();
                    logger.warn("命令执行失败，退出状态: {}, 错误信息: {}, 主机: {}", 
                        exitStatus, errorMsg, session.getConnectAddress());
                    cacheLog.warn("命令执行失败，退出状态: %d, 错误信息: %s, 主机: %s", 
                        exitStatus, errorMsg, session.getConnectAddress());
                    return "ERROR: " + (errorMsg.isEmpty() ? "未知错误，退出状态: " + exitStatus : errorMsg);
                }
                
                // 获取命令输出结果
                String result = out.toString();
                if (result.length() > 100) {
                    logger.debug("命令执行成功，输出(前100字符): {}", result.substring(0, 100) + "...");
                    cacheLog.debug("命令执行成功，输出(前100字符): %s", result.substring(0, 100) + "...");
                } else {
                    logger.debug("命令执行成功，输出: {}", result);
                    cacheLog.debug("命令执行成功，输出: %s", result);
                }
                return result;
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
            if (e.getCause() instanceof InterruptedException || Thread.currentThread().isInterrupted()) {
                Thread.currentThread().interrupt();
                throw new InterruptedException("命令执行过程中被中断: " + e.getMessage());
            }
            
            return "ERROR: " + e.getMessage();
        }
    }

    /**
     * 打开SSH会话
     */
    protected void openSession(HostInfo hostInfo) {
        try {
            // 通过Mina工具打开SSH连接
            cacheLog.info("开始连接到主机 %s, 端口: %d, 用户: %s", hostInfo.getHostname(), 
                    hostInfo.getSshPort(), hostInfo.getSshUser());
            
            // 明确初始化为null，确保之前可能的有效session被清理
            session = null;
            
            // 尝试建立会话连接
            session = MinaUtils.openConnection(hostInfo.getHostname(), 
                    hostInfo.getSshPort(), hostInfo.getSshUser());
            
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
                logger.debug("正在关闭SSH会话: {}", 
                    session.getConnectAddress() != null ? session.getConnectAddress() : "未知地址");
                cacheLog.debug("正在关闭SSH会话: %s", 
                    session.getConnectAddress() != null ? session.getConnectAddress() : "未知地址");
                
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
    public final CheckItem check(Integer clusterId, HostInfo hostInfo, CheckItem checkItem) {
        logger.info("开始检查项: {}, 主机: {}, 检查项ID: {}", checkItem.getItemName(), hostInfo.getHostname(), checkItem.getId());
        
        // 设置当前检查项的日志缓存键
        setCurrentLogKey(clusterId, hostInfo.getHostname(), checkItem.getId());
        
        // 先将状态设置为检查中
        checkItem.setStatus(CheckItem.Status.CHECKING);
        checkItem.setMessage("检查中...");
        updateCheckStatus(clusterId, hostInfo, checkItem);
        
        try {
            // 检查是否被中断 - 新增
            if (Thread.currentThread().isInterrupted()) {
                logger.info("检查项在开始前已被中断: {}", checkItem.getItemName());
                checkItem.setStatus(CheckItem.Status.SKIPPED);
                checkItem.setMessage("检查已终止");
                updateCheckStatus(clusterId, hostInfo, checkItem);
                return checkItem;
            }
            
            logger.info("开始建立SSH连接到主机: {}, 端口: {}, 用户: {}", 
                hostInfo.getHostname(), hostInfo.getSshPort(), hostInfo.getSshUser());
            cacheLog.info("开始建立SSH连接到主机: %s, 端口: %d, 用户: %s", 
                hostInfo.getHostname(), hostInfo.getSshPort(), hostInfo.getSshUser());
                
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
            
            // 再次检查是否被中断 - 新增
            if (Thread.currentThread().isInterrupted()) {
                logger.info("检查项在建立SSH连接后被中断: {}", checkItem.getItemName());
                checkItem.setStatus(CheckItem.Status.SKIPPED);
                checkItem.setMessage("检查已终止");
                updateCheckStatus(clusterId, hostInfo, checkItem);
                closeSession(); // 确保关闭已建立的会话
                return checkItem;
            }
            
            // 明确检查session是否成功建立 - 增强处理
            if (session == null) {
                String errorMsg = "无法建立SSH连接到主机: " + hostInfo.getHostname();
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
            
            logger.info("成功连接到主机: {}, 开始执行检查项: {}", hostInfo.getHostname(), checkItem.getItemName());
            
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
                
                // 检查执行后再次检查是否被中断
                if (Thread.currentThread().isInterrupted()) {
                    logger.info("检查项在执行后被中断: {}", checkItem.getItemName());
                    // 仅当检查项还是"检查中"状态时才修改
                    if (checkItem.getStatus() == CheckItem.Status.CHECKING) {
                        checkItem.setStatus(CheckItem.Status.SKIPPED);
                        checkItem.setMessage("检查已终止");
                    }
                    updateCheckStatus(clusterId, hostInfo, checkItem);
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
            logger.error("连接主机 {} 时发生异常: {}", hostInfo.getHostname(), e.getMessage(), e);
            checkItem.setStatus(CheckItem.Status.FAILED);
            checkItem.setMessage("连接主机失败: " + e.getMessage());
            updateCheckStatus(clusterId, hostInfo, checkItem);
        } finally {
            if (session != null) {
                logger.info("正在关闭到主机 {} 的SSH连接", hostInfo.getHostname());
                closeSession();
                logger.info("已关闭到主机 {} 的SSH连接", hostInfo.getHostname());
            }
        }
        
        // 最后更新一次状态，确保前端能看到最终结果
        updateCheckStatus(clusterId, hostInfo, checkItem);
        logger.info("检查项 {} 最终状态: {}, 消息: {}", 
            checkItem.getItemName(), checkItem.getStatus(), checkItem.getMessage());
        
        return checkItem;    
    }

    @Override
    public boolean fix(Integer clusterId, HostInfo hostInfo, CheckItem checkItem) {
        logger.info("开始修复检查项: {}, 主机: {}, 检查项ID: {}", checkItem.getItemName(), hostInfo.getHostname(), checkItem.getId());
        
        // 设置当前检查项的日志缓存键
        setCurrentLogKey(clusterId, hostInfo.getHostname(), checkItem.getId());
        
        // 记录修复开始
        cacheLog.info("===============================================");
        cacheLog.info("开始修复检查项: " + checkItem.getItemName());
        cacheLog.info("主机: " + hostInfo.getHostname());
        cacheLog.info("检查项ID: " + checkItem.getId());
        cacheLog.info("开始时间: " + getCurrentTimeInChinese());
        cacheLog.info("===============================================");
        
        try {
            // 建立SSH连接
            cacheLog.info("正在建立SSH连接...");
            openSession(hostInfo);
            
            if (session == null) {
                String errorMsg = "无法建立SSH连接到主机: " + hostInfo.getHostname();
                logger.error(errorMsg);
                cacheLog.error("错误: " + errorMsg);
                cacheLog.error("修复失败: 无法连接到主机");
                return false;
            }
            
            cacheLog.info("SSH连接建立成功，开始执行修复操作");
            
            // 执行具体修复逻辑
            boolean doFixResult = false;
            try {
                cacheLog.info("正在执行修复逻辑...");
                doFixResult = doFix(hostInfo, checkItem);
                cacheLog.info("修复逻辑执行" + (doFixResult ? "成功" : "失败"));
            } catch (Exception e) {
                String errorMsg = "执行修复逻辑时发生异常: " + e.getMessage();
                logger.error(errorMsg, e);
                cacheLog.error("错误: " + errorMsg);
                return false;
            }
            
            // 再次检查，验证修复结果
            try {
                cacheLog.info("正在验证修复结果...");
                CheckItem checkResult = doCheck(hostInfo, checkItem);
                cacheLog.info("验证结果: " + (checkResult.getStatus() == CheckItem.Status.SUCCESS ? "成功" : "失败"));
                cacheLog.info("验证信息: " + checkResult.getMessage());
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
            
            return false;
        } finally {
            // 记录修复结束
            cacheLog.info("===============================================");
            cacheLog.info("修复操作结束");
            cacheLog.info("结束时间: " + getCurrentTimeInChinese());
            cacheLog.info("===============================================");
        }
    }

    /**
     * 获取检查器类型
     */
    protected abstract ItemCode getCheckerType();

    /**
     * 执行具体的检查逻辑
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
                hostInfo.getHostname(), checkItem.getId(), checkItem.getStatus(), checkItem.getMessage());
        
        try {
            // 记录更新前的状态
            logger.info("正在更新检查项状态 - 主机: {}, 检查项: {}, 当前状态: {}, 新状态: {}", 
                hostInfo.getHostname(), checkItem.getItemName(), 
                "更新前", checkItem.getStatus());
                
        Map<String, HostInfo> hostInfoMap = (Map<String, HostInfo>) CacheUtils.get(cacheKey);
        if (hostInfoMap != null) {
            HostInfo cachedHostInfo = hostInfoMap.get(hostInfo.getHostname());
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
                        logger.warn("未找到要更新的检查项: 主机={}, 检查项ID={}", hostInfo.getHostname(), checkItem.getId());
                    } else {
                        // 更新主机的整体状态（根据检查项状态计算）
                        cachedHostInfo.calculateStatus();
                hostInfoMap.put(hostInfo.getHostname(), cachedHostInfo);
                CacheUtils.put(cacheKey, hostInfoMap);
                        logger.debug("缓存已更新: cacheKey={}, 主机状态={}", 
                            cacheKey, cachedHostInfo.getStatus());
                    }
                } else {
                    logger.warn("缓存中未找到主机信息: hostname={}", hostInfo.getHostname());
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
} 