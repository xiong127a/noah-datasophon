package com.datasophon.api.service.checker.impl;

import com.datasophon.api.service.checker.AbstractItemChecker;
import com.datasophon.api.service.checker.CommandResult;
import com.datasophon.common.model.CheckItem;
import com.datasophon.common.model.HostInfo;
import com.datasophon.common.model.ItemCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

@Component
public class TimeSyncChecker extends AbstractItemChecker {
    
    private static final Logger logger = LoggerFactory.getLogger(TimeSyncChecker.class);
    private static final long MAX_TIME_DIFF_SECONDS = 10; // 最大允许的时间差为10秒
    
    @Override
    protected CheckItem doCheck(HostInfo hostInfo, CheckItem checkItem) {
        try {
            cacheLog.info("==== 时间同步检查开始 ====");
            cacheLog.info("主机: " + hostInfo.getHostname());
            cacheLog.info("最大允许时间差: " + MAX_TIME_DIFF_SECONDS + "秒");
            
            // 更新状态为正在获取远程服务器时间
            checkItem.setMessage("正在获取远程服务器时间...");
            
            // 1. 首先获取远程服务器的时间
            CommandResult remoteTimeResult = execCommand(session, "date '+%Y-%m-%d %H:%M:%S'");
            if (!remoteTimeResult.isSuccess()) {
                cacheLog.error("获取远程服务器时间失败: %s", remoteTimeResult.getErrorOrOutput());
                checkItem.setStatus(CheckItem.Status.FAILED);
                checkItem.setMessage("获取远程服务器时间失败: " + remoteTimeResult.getErrorOrOutput());
                return checkItem;
            }
            
            String remoteTimeStr = remoteTimeResult.getOutput().trim();
            cacheLog.info("远程服务器时间: " + remoteTimeStr);
            
            // 更新状态为正在获取远程服务器时区
            checkItem.setMessage("正在获取远程服务器时区...");
            
            // 2. 获取远程服务器时区
            CommandResult remoteTzResult = execCommand(session, "date '+%Z'");
            if (!remoteTzResult.isSuccess()) {
                cacheLog.warn("获取远程服务器时区失败: %s", remoteTzResult.getErrorOrOutput());
            }
            String remoteTz = remoteTzResult.isSuccess() ? remoteTzResult.getOutput().trim() : "未知";
            cacheLog.info("远程服务器时区: " + remoteTz);
            
            // 3. 获取本地服务器时间
            Date localDate = new Date();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String localTimeStr = sdf.format(localDate);
            cacheLog.info("本地服务器时间: " + localTimeStr);
            
            // 4. 获取本地服务器时区
            String localTz = TimeZone.getDefault().getID();
            cacheLog.info("本地服务器时区: " + localTz);
            
            // 更新状态为正在计算时间差
            checkItem.setMessage("正在计算时间差...");
            
            // 5. 计算时间差
            try {
                Date remoteDate = sdf.parse(remoteTimeStr);
                long diffMillis = Math.abs(remoteDate.getTime() - localDate.getTime());
                long diffSeconds = diffMillis / 1000;
                
                cacheLog.info("时间差: " + diffSeconds + "秒");
                
                boolean isTimeSynced = diffSeconds <= MAX_TIME_DIFF_SECONDS;
                
                if (isTimeSynced) {
                    checkItem.setStatus(CheckItem.Status.SUCCESS);
                    checkItem.setMessage("服务器时间同步正常，时间差: " + diffSeconds + "秒");
                    cacheLog.info("服务器时间同步检查通过");
                } else {
                    checkItem.setStatus(CheckItem.Status.FAILED);
                    checkItem.setMessage("服务器时间不同步，时间差: " + diffSeconds + "秒 (大于" + MAX_TIME_DIFF_SECONDS + "秒)");
                    cacheLog.info("服务器时间同步检查未通过");
                }
            } catch (Exception e) {
                cacheLog.error("计算时间差时发生错误: %s", e.getMessage());
                checkItem.setStatus(CheckItem.Status.FAILED);
                checkItem.setMessage("计算时间差时发生错误: " + e.getMessage());
            }
            
        } catch (Exception e) {
            String errorMsg = "检查时间同步时发生错误: " + e.getMessage();
            logger.error(errorMsg, e);
            cacheLog.error(errorMsg);
            checkItem.setStatus(CheckItem.Status.FAILED);
            checkItem.setMessage(errorMsg);
        } finally {
            cacheLog.info("==== 时间同步检查结束 ====");
        }
        return checkItem;
    }
    
    @Override
    protected boolean doFix(HostInfo hostInfo, CheckItem checkItem) {
        try {
            cacheLog.info("==== 开始修复服务器时间同步 ====");
            
            // 更新状态为正在获取本地时间
            checkItem.setMessage("正在获取本地时间信息...");
            
            // 1. 获取本地时间
            Date localDate = new Date();
            SimpleDateFormat dateFmt = new SimpleDateFormat("yyyy-MM-dd");
            SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm:ss");
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String localDateStr = dateFmt.format(localDate);
            String localTimeStr = timeFmt.format(localDate);
            
            cacheLog.info("本地时间: " + localDateStr + " " + localTimeStr);
            
            // 2. 获取本地时区
            String localTz = TimeZone.getDefault().getID();
            cacheLog.info("本地时区: " + localTz);
            
            String tzFile = getTimezoneFile(localTz);
            if (tzFile == null || tzFile.isEmpty()) {
                cacheLog.warn("无法找到对应的时区文件，将只同步时间而不同步时区");
            } else {
                cacheLog.info("对应的时区文件: " + tzFile);
            }
            
            // 更新状态为正在设置时区
            checkItem.setMessage("正在设置服务器时区...");
            
            // 3. 设置远程服务器时区（如果能确定对应的时区文件）
            if (tzFile != null && !tzFile.isEmpty()) {
                cacheLog.info("设置远程服务器时区...");
                CommandResult tzResult = execCommand(session, "ln -sf " + tzFile + " /etc/localtime");
                if (!tzResult.isSuccess()) {
                    cacheLog.warn("设置时区失败: %s", tzResult.getErrorOrOutput());
                } else {
                    cacheLog.info("时区设置成功");
                }
            }
            
            // 更新状态为正在设置系统时间
            checkItem.setMessage("正在设置系统时间...");
            
            // 4. 设置远程服务器日期和时间
            cacheLog.info("设置远程服务器日期和时间...");
            String dateCmd = "date -s \"" + localDateStr + " " + localTimeStr + "\"";
            CommandResult dateResult = execCommand(session, dateCmd);
            
            if (!dateResult.isSuccess()) {
                cacheLog.error("设置日期和时间失败: %s", dateResult.getErrorOrOutput());
                checkItem.setMessage("设置系统时间失败: " + dateResult.getErrorOrOutput());
                return false;
            }
            cacheLog.info("日期和时间设置成功");
            
            // 更新状态为正在同步硬件时钟
            checkItem.setMessage("正在同步硬件时钟...");
            
            // 5. 将时间写入硬件时钟
            cacheLog.info("将时间同步到硬件时钟...");
            CommandResult hwClockResult = execCommand(session, "hwclock --systohc");
            if (!hwClockResult.isSuccess()) {
                cacheLog.warn("硬件时钟同步失败: %s", hwClockResult.getErrorOrOutput());
            } else {
                cacheLog.info("硬件时钟同步成功");
            }
            
            // 更新状态为正在验证时间同步
            checkItem.setMessage("正在验证时间同步结果...");
            
            // 6. 验证时间同步结果
            cacheLog.info("验证时间同步结果...");
            CommandResult verifyResult = execCommand(session, "date '+%Y-%m-%d %H:%M:%S'");
            if (verifyResult.isSuccess()) {
                String remoteTimeAfterSync = verifyResult.getOutput().trim();
                cacheLog.info("同步后的远程服务器时间: " + remoteTimeAfterSync);
                
                // 再次获取本地时间进行比较
                Date newLocalDate = new Date();
                String newLocalTimeStr = sdf.format(newLocalDate);
                cacheLog.info("当前本地服务器时间: " + newLocalTimeStr);
                
                try {
                    Date remoteDate = sdf.parse(remoteTimeAfterSync);
                    long diffMillis = Math.abs(remoteDate.getTime() - newLocalDate.getTime());
                    long diffSeconds = diffMillis / 1000;
                    
                    cacheLog.info("同步后的时间差: " + diffSeconds + "秒");
                    
                    if (diffSeconds <= MAX_TIME_DIFF_SECONDS) {
                        cacheLog.info("时间同步修复成功");
                        checkItem.setMessage("时间同步修复成功，当前时间差: " + diffSeconds + "秒");
                    } else {
                        cacheLog.warn("时间同步后仍有较大差异: " + diffSeconds + "秒");
                        checkItem.setMessage("警告：时间同步后仍有较大差异: " + diffSeconds + "秒");
                    }
                } catch (Exception e) {
                    cacheLog.warn("验证时间同步结果时发生错误: %s", e.getMessage());
                    checkItem.setMessage("警告：验证时间同步结果时发生错误");
                }
            } else {
                cacheLog.warn("获取同步后的远程时间失败: %s", verifyResult.getErrorOrOutput());
                checkItem.setMessage("警告：无法获取同步后的时间");
            }
            
            cacheLog.info("==== 服务器时间同步修复完成 ====");
            return true;
            
        } catch (Exception e) {
            String errorMsg = "修复时间同步时发生错误: " + e.getMessage();
            logger.error(errorMsg, e);
            cacheLog.error(errorMsg);
            checkItem.setMessage("修复时间同步失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 根据时区ID获取对应的时区文件路径
     * @param tzId 时区ID，如 "Asia/Shanghai"
     * @return 时区文件路径，如 "/usr/share/zoneinfo/Asia/Shanghai"
     */
    private String getTimezoneFile(String tzId) {
        if (tzId == null || tzId.isEmpty()) {
            return null;
        }
        
        // 常见时区ID到文件路径的映射
        switch (tzId) {
            case "Asia/Shanghai":
            case "Asia/Chongqing":
            case "Asia/Harbin":
            case "Asia/Urumqi":
                return "/usr/share/zoneinfo/Asia/Shanghai";
                
            case "America/New_York":
                return "/usr/share/zoneinfo/America/New_York";
                
            case "America/Los_Angeles":
                return "/usr/share/zoneinfo/America/Los_Angeles";
                
            case "Europe/London":
                return "/usr/share/zoneinfo/Europe/London";
                
            case "Europe/Paris":
                return "/usr/share/zoneinfo/Europe/Paris";
                
            default:
                // 尝试直接使用时区ID作为相对路径
                if (tzId.contains("/")) {
                    return "/usr/share/zoneinfo/" + tzId;
                }
                return null;
        }
    }
    
    @Override
    public ItemCode getCheckerType() {
        return ItemCode.TIME_SYNC;
    }
} 