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

    protected String execCommand(ClientSession session, String command) {
        try {
            if (session == null) {
                logger.error("SSH会话为空，无法执行命令");
                return "ERROR: SSH会话为空";
            }
            
            logger.debug("执行命令: {}", command);
            
            // 创建执行命令的通道
            try (org.apache.sshd.client.channel.ClientChannel channel = session.createExecChannel(command)) {
                // 启动命令
                channel.open().verify(30, TimeUnit.SECONDS);
                
                // 读取命令输出
                java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
                java.io.ByteArrayOutputStream err = new java.io.ByteArrayOutputStream();
                channel.setOut(out);
                channel.setErr(err);
                
                // 等待命令完成
                long waitTime = TimeUnit.SECONDS.toMillis(30);
                if (channel.waitFor(java.util.EnumSet.of(
                        org.apache.sshd.client.channel.ClientChannelEvent.CLOSED,
                        org.apache.sshd.client.channel.ClientChannelEvent.EOF), waitTime) == null) {
                    logger.warn("命令执行超时: {}", command);
                    return "ERROR: 命令执行超时";
                }
                
                // 获取命令执行的退出状态
                Integer exitStatus = channel.getExitStatus();
                if (exitStatus != null && exitStatus != 0) {
                    String errorMsg = err.toString();
                    logger.warn("命令执行失败，退出状态: {}, 错误信息: {}", exitStatus, errorMsg);
                    return "ERROR: " + (errorMsg.isEmpty() ? "未知错误，退出状态: " + exitStatus : errorMsg);
                }
                
                // 获取命令输出结果
                String result = out.toString();
                logger.debug("命令执行成功，输出: {}", result);
                return result;
            }
        } catch (Exception e) {
            logger.error("执行命令 {} 失败: {}", command, e.getMessage(), e);
            return "ERROR: " + e.getMessage();
        }
    }

    protected void openSession(HostInfo hostInfo){
        session = MinaUtils.openConnection(hostInfo.getHostname(), hostInfo.getSshPort(), hostInfo.getSshUser());
    }

    protected void closeSession(){
        if(session != null){
            try {
                session.close();
            } catch (java.io.IOException e) {
                logger.error("Error closing SSH session: {}", e.getMessage(), e);
            }
        }
    }

    @Override
    public final CheckItem check(Integer clusterId, HostInfo hostInfo, CheckItem checkItem) {

        // 先将状态设置为检查中
        checkItem.setStatus(CheckItem.Status.CHECKING);
        checkItem.setMessage("检查中...");
        updateCheckStatus(clusterId, hostInfo, checkItem);
        // 等待10秒，防止检查项失败
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            logger.error("检查等待失败", e);
        }
        openSession(hostInfo);
        doCheck(hostInfo, checkItem);
        closeSession();
        return checkItem;    
    }

    @Override
    public boolean fix(Integer clusterId, HostInfo hostInfo, CheckItem checkItem) {
        try {
            // 等待10秒，防止检查项失败
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                logger.error("检查等待失败", e);
            }
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
     */
    protected abstract CheckItem doCheck(HostInfo hostInfo, CheckItem checkItem);

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