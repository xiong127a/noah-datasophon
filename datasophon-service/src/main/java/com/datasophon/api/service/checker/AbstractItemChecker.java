package com.datasophon.api.service.checker;

import com.datasophon.api.utils.MinaUtils;
import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.model.CheckItem;
import com.datasophon.common.model.HostInfo;
import com.datasophon.common.model.ItemCode;
import org.apache.sshd.client.session.ClientSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public abstract class AbstractItemChecker implements ItemChecker {
    private static final Logger logger = LoggerFactory.getLogger(AbstractItemChecker.class);
    private static final String CHECK_TASK_STATUS_PREFIX = "CHECK_TASK_STATUS_";

    protected ClientSession session;

    protected String execCommand(ClientSession session, String command) throws InterruptedException {
        // 检查线程是否已被中断
        if (Thread.currentThread().isInterrupted()) {
            logger.info("命令执行被中断: {}", command);
            throw new InterruptedException("命令执行被中断");
        }
        
        try {
            if (session == null) {
                logger.error("SSH会话为空，无法执行命令");
                return "ERROR: SSH会话为空";
            }
            
            logger.debug("准备执行命令: {} 在主机: {}", command, session.getConnectAddress());
            
            // 创建执行命令的通道
            try (org.apache.sshd.client.channel.ClientChannel channel = session.createExecChannel(command)) {
                logger.debug("命令通道已创建，正在打开通道");
                
                // 再次检查线程是否已被中断
                if (Thread.currentThread().isInterrupted()) {
                    logger.info("命令通道创建后执行被中断: {}", command);
                    throw new InterruptedException("命令执行被中断");
                }
                
                // 启动命令
                channel.open().verify(30, TimeUnit.SECONDS);
                logger.debug("命令通道已打开，开始执行命令");
                
                // 读取命令输出
                java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
                java.io.ByteArrayOutputStream err = new java.io.ByteArrayOutputStream();
                channel.setOut(out);
                channel.setErr(err);
                
                // 等待命令完成
                long waitTime = TimeUnit.SECONDS.toMillis(30);
                logger.debug("等待命令执行完成，超时时间: {}ms", waitTime);
                
                long startTime = System.currentTimeMillis();
                long remainingTime = waitTime;
                
                // 使用分段等待，每秒检查一次线程是否被中断
                while (remainingTime > 0) {
                    // 检查线程是否已被中断
                    if (Thread.currentThread().isInterrupted()) {
                        logger.info("命令执行等待过程中被中断: {}", command);
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
                    return "ERROR: 命令执行超时，请检查网络或主机状态";
                }
                
                logger.debug("命令执行完成，耗时: {}ms", (endTime - startTime));
                
                // 获取命令执行的退出状态
                Integer exitStatus = channel.getExitStatus();
                if (exitStatus != null && exitStatus != 0) {
                    String errorMsg = err.toString();
                    logger.warn("命令执行失败，退出状态: {}, 错误信息: {}, 主机: {}", 
                        exitStatus, errorMsg, session.getConnectAddress());
                    return "ERROR: " + (errorMsg.isEmpty() ? "未知错误，退出状态: " + exitStatus : errorMsg);
                }
                
                // 获取命令输出结果
                String result = out.toString();
                if (result.length() > 100) {
                    logger.debug("命令执行成功，输出(前100字符): {}", result.substring(0, 100) + "...");
                } else {
                    logger.debug("命令执行成功，输出: {}", result);
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
            
            // 检查是否是由于中断导致的异常
            if (e.getCause() instanceof InterruptedException || Thread.currentThread().isInterrupted()) {
                Thread.currentThread().interrupt();
                throw new InterruptedException("命令执行过程中被中断: " + e.getMessage());
            }
            
            return "ERROR: " + e.getMessage();
        }
    }

    protected void openSession(HostInfo hostInfo) throws InterruptedException {
        // 检查线程是否已被中断
        if (Thread.currentThread().isInterrupted()) {
            logger.info("建立SSH连接被中断, 主机: {}", hostInfo.getHostname());
            throw new InterruptedException("建立SSH连接被中断");
        }
        
        // 记录开始尝试建立连接
        logger.info("尝试建立SSH连接: {}@{}:{}", 
            hostInfo.getSshUser(), hostInfo.getHostname(), hostInfo.getSshPort());
        
        // 等待5秒，防止过快发送请求导致检查失败
        logger.info("等待5秒后开始连接主机 {}", hostInfo.getHostname());
        try {
            // 分成5次，每次等待1秒，这样可以更频繁地检查中断状态
            for (int i = 0; i < 5; i++) {
                // 在每次等待前检查中断状态
                if (Thread.currentThread().isInterrupted()) {
                    logger.info("建立SSH连接等待过程被中断, 主机: {}", hostInfo.getHostname());
                    throw new InterruptedException("建立SSH连接被中断");
                }
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            logger.error("连接等待被中断", e);
            Thread.currentThread().interrupt(); // 重置中断状态
            throw e; // 重新抛出中断异常
        }
        
        try {
            // 再次检查中断状态
            if (Thread.currentThread().isInterrupted()) {
                logger.info("建立SSH连接准备连接时被中断, 主机: {}", hostInfo.getHostname());
                throw new InterruptedException("建立SSH连接被中断");
            }
            
            long startTime = System.currentTimeMillis();
            session = MinaUtils.openConnection(hostInfo.getHostname(), hostInfo.getSshPort(), hostInfo.getSshUser());
            long endTime = System.currentTimeMillis();
            
            if (session != null && session.isOpen()) {
                logger.info("SSH连接建立成功: {}@{}:{}, 耗时: {}ms", 
                    hostInfo.getSshUser(), hostInfo.getHostname(), hostInfo.getSshPort(), 
                    (endTime - startTime));
            } else {
                logger.error("SSH连接建立失败: {}@{}:{}, MinaUtils.openConnection方法返回null或未打开的会话", 
                    hostInfo.getSshUser(), hostInfo.getHostname(), hostInfo.getSshPort());
            }
        } catch (Exception e) {
            logger.error("SSH连接建立异常: {}@{}:{}, 错误信息: {}", 
                hostInfo.getSshUser(), hostInfo.getHostname(), hostInfo.getSshPort(), 
                e.getMessage(), e);
            
            // 检查是否是由于中断导致的异常
            if (e.getCause() instanceof InterruptedException || Thread.currentThread().isInterrupted()) {
                Thread.currentThread().interrupt();
                throw new InterruptedException("建立SSH连接过程中被中断: " + e.getMessage());
            }
            
            session = null;
        }
    }

    protected void closeSession() {
        if (session != null) {
            try {
                logger.debug("正在关闭SSH会话: {}", 
                    session.getConnectAddress() != null ? session.getConnectAddress() : "未知地址");
                
                long startTime = System.currentTimeMillis();
                session.close();
                long endTime = System.currentTimeMillis();
                
                logger.debug("SSH会话关闭成功，耗时: {}ms", (endTime - startTime));
            } catch (java.io.IOException e) {
                logger.error("关闭SSH会话异常: {}", e.getMessage(), e);
            } catch (Exception e) {
                logger.error("关闭SSH会话时发生未预期的异常: {}", e.getMessage(), e);
            } finally {
                session = null;
            }
        }
    }

    @Override
    public final CheckItem check(Integer clusterId, HostInfo hostInfo, CheckItem checkItem) {
        logger.info("开始检查项: {}, 主机: {}, 检查项ID: {}", checkItem.getItemName(), hostInfo.getHostname(), checkItem.getId());
        
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
            openSession(hostInfo);
            
            // 再次检查是否被中断 - 新增
            if (Thread.currentThread().isInterrupted()) {
                logger.info("检查项在建立SSH连接后被中断: {}", checkItem.getItemName());
                checkItem.setStatus(CheckItem.Status.SKIPPED);
                checkItem.setMessage("检查已终止");
                updateCheckStatus(clusterId, hostInfo, checkItem);
                closeSession(); // 确保关闭已建立的会话
                return checkItem;
            }
            
            if (session == null) {
                logger.error("无法建立SSH连接到主机: {}", hostInfo.getHostname());
                checkItem.setStatus(CheckItem.Status.FAILED);
                checkItem.setMessage("无法建立SSH连接");
                updateCheckStatus(clusterId, hostInfo, checkItem);
                return checkItem;
            }
            
            logger.info("成功连接到主机: {}, 开始执行检查项: {}", hostInfo.getHostname(), checkItem.getItemName());
            
            try {
                // 执行具体检查逻辑，确保捕获InterruptedException
                try {
                    doCheck(hostInfo, checkItem);
                } catch (InterruptedException e) {
                    // 捕获中断异常
                    logger.info("检查项在执行过程中被中断: {}", checkItem.getItemName());
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
        try {
            // 获取SSH会话并执行具体修复逻辑
            openSession(hostInfo);
            boolean doFix = doFix(hostInfo, checkItem);
            doCheck(hostInfo,checkItem);
            closeSession();
            return doFix;
        } catch (Exception e) {
            logger.error("修复失败", e);
            return false;
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
        Map<String, HostInfo> hostInfoMap = (Map<String, HostInfo>) CacheUtils.get(cacheKey);
        if (hostInfoMap != null) {
            HostInfo cachedHostInfo = hostInfoMap.get(hostInfo.getHostname());
            if (cachedHostInfo != null) {
                cachedHostInfo.getCheckItems().stream()
                        .filter(item -> item.getId().equals(checkItem.getId()))
                        .findFirst()
                        .ifPresent(item -> {
                            item.setStatus(checkItem.getStatus());
                            item.setMessage(checkItem.getMessage());
                        });
                hostInfoMap.put(hostInfo.getHostname(), cachedHostInfo);
                CacheUtils.put(cacheKey, hostInfoMap);
            }
        }
    }

    

} 