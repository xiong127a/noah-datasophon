package com.datasophon.api.service.checker.impl;

import com.datasophon.api.service.checker.AbstractItemChecker;
import com.datasophon.common.model.CheckItem;
import com.datasophon.common.model.HostInfo;
import com.datasophon.common.model.ItemCode;
import org.apache.sshd.client.session.ClientSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;

@Component
public class TimeSyncChecker extends AbstractItemChecker {
    
    private static final Logger logger = LoggerFactory.getLogger(TimeSyncChecker.class);
    private static final long MAX_TIME_DIFF_SECONDS = 10; // 最大允许的时间差为10秒
    
    @Override
    protected CheckItem doCheck(HostInfo hostInfo, CheckItem checkItem) {
        ClientSession session = hostInfo.getSession();
        try {
            // 获取当前服务器的时间戳
            long serverTime = System.currentTimeMillis() / 1000; // 转换为秒
            
            // 获取远程主机的Unix时间戳
            String remoteTimeResult = execCommand(session, "date +%s");
            if (remoteTimeResult.startsWith("ERROR")) {
                checkItem.setStatus(CheckItem.Status.FAILED);
                checkItem.setMessage("获取主机时间失败: " + remoteTimeResult);
                return checkItem;
            }
            
            long remoteTime = Long.parseLong(remoteTimeResult.trim());
            
            // 计算时间差（取绝对值）
            long timeDiff = Math.abs(remoteTime - serverTime);
            
            if (timeDiff <= MAX_TIME_DIFF_SECONDS) {
                checkItem.setStatus(CheckItem.Status.SUCCESS);
                checkItem.setMessage(String.format("主机时间同步正常，与服务器时间差为 %d 秒", timeDiff));
            } else {
                checkItem.setStatus(CheckItem.Status.FAILED);
                checkItem.setMessage(String.format("主机时间同步异常，与服务器时间差为 %d 秒，超过了允许的 %d 秒",
                        timeDiff, MAX_TIME_DIFF_SECONDS));
            }
            
        } catch (Exception e) {
            logger.error("时间同步检查失败: {}", e.getMessage());
            checkItem.setStatus(CheckItem.Status.FAILED);
            checkItem.setMessage("时间同步检查失败: " + e.getMessage());
        }
        return checkItem;
    }
    
    @Override
    protected boolean doFix(HostInfo hostInfo, CheckItem checkItem) {
        ClientSession session = hostInfo.getSession();
        try {
            // 获取当前服务器时间
            Date currentDate = new Date();
            
            // 格式化为Linux date命令接受的格式 (MMDDHHmmYYYY.ss)
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMddHHmmyyyy.ss");
            String formattedDate = dateFormat.format(currentDate);
            
            // 直接设置远程主机的系统时间为当前服务器时间
            String setTimeResult = execCommand(session, "date " + formattedDate);
            
            if (setTimeResult.startsWith("ERROR")) {
                logger.error("设置远程主机时间失败: {}", setTimeResult);
                return false;
            }
            
            // 验证时间是否已同步
            long serverTime = System.currentTimeMillis() / 1000;
            String remoteTimeAfterFix = execCommand(session, "date +%s");
            
            if (remoteTimeAfterFix.startsWith("ERROR")) {
                logger.error("获取修复后的远程主机时间失败: {}", remoteTimeAfterFix);
                return false;
            }
            
            long remoteTimeAfter = Long.parseLong(remoteTimeAfterFix.trim());
            long timeDiffAfterFix = Math.abs(remoteTimeAfter - serverTime);
            
            if (timeDiffAfterFix <= MAX_TIME_DIFF_SECONDS) {
                logger.info("成功同步主机时间，当前时间差为 {} 秒", timeDiffAfterFix);
                return true;
            } else {
                logger.error("同步主机时间后仍存在较大时间差: {} 秒", timeDiffAfterFix);
                return false;
            }
        } catch (Exception e) {
            logger.error("时间同步修复失败: {}", e.getMessage());
            return false;
        }
    }
    
    @Override
    protected ItemCode getCheckerType() {
        return ItemCode.TIME_SYNC;
    }
    
    private String execCommand(ClientSession session, String command) {
        try {
            // TODO: 实现命令执行逻辑
            return "1634567890"; // 临时返回一个模拟的Unix时间戳
        } catch (Exception e) {
            logger.error("执行命令 {} 失败: {}", command, e.getMessage());
            return "ERROR: " + e.getMessage();
        }
    }
} 