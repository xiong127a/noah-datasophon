package com.datasophon.api.service.checker.impl;

import com.datasophon.api.service.checker.AbstractItemChecker;
import com.datasophon.common.model.CheckItem;
import com.datasophon.common.model.HostInfo;
import com.datasophon.common.model.ItemCode;
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
        try {
            cacheLog.debug("======== 时间同步检查开始 ========");
            cacheLog.debug("检查主机: %s，检查项ID: %d", hostInfo.getHostname(), checkItem.getId());
            cacheLog.info("开始检查主机时间同步 - 主机: %s", hostInfo.getHostname());
            cacheLog.info("允许的最大时间差: %d秒", MAX_TIME_DIFF_SECONDS);

            // 获取远程主机的Unix时间戳
            cacheLog.debug("准备获取远程主机时间戳...");
            cacheLog.info("获取远程主机时间戳...");
            String remoteTimeResult = execCommand(session, "date +%s");
            cacheLog.debug("远程主机时间戳获取命令返回: %s", remoteTimeResult);
            
            // 获取当前服务器的时间戳
            long serverTime = System.currentTimeMillis() / 1000; // 转换为秒
            cacheLog.debug("当前服务器Unix时间戳: %d", serverTime);
            cacheLog.debug("当前服务器时间: %s", new Date(serverTime * 1000).toString());
            cacheLog.info("服务器当前时间戳: %d", serverTime);
            
            if (remoteTimeResult.startsWith("ERROR")) {
                cacheLog.debug("远程主机时间戳获取失败: %s", remoteTimeResult);
                cacheLog.error("获取主机时间失败: %s", remoteTimeResult);
                checkItem.setStatus(CheckItem.Status.FAILED);
                checkItem.setMessage("获取主机时间失败: " + remoteTimeResult);
                return checkItem;
            }
            
            long remoteTime = Long.parseLong(remoteTimeResult.trim());
            cacheLog.debug("远程主机Unix时间戳: %d", remoteTime);
            cacheLog.debug("远程主机时间: %s", new Date(remoteTime * 1000).toString());
            cacheLog.info("远程主机时间戳: %d", remoteTime);
            
            // 计算时间差（取绝对值）
            long timeDiff = Math.abs(remoteTime - serverTime);
            cacheLog.debug("服务器与远程主机的时间差绝对值: %d秒", timeDiff);
            cacheLog.info("时间差: %d秒", timeDiff);
            
            // 检查远程主机的时区
            cacheLog.debug("获取远程主机的时区信息...");
            String timeZoneResult = execCommand(session, "date +%Z");
            cacheLog.debug("远程主机时区: %s", timeZoneResult.startsWith("ERROR") ? "获取失败" : timeZoneResult.trim());
            
            if (timeDiff <= MAX_TIME_DIFF_SECONDS) {
                cacheLog.debug("时间同步检查通过: 时间差在允许范围内 (%d秒 <= %d秒)", timeDiff, MAX_TIME_DIFF_SECONDS);
                cacheLog.info("时间同步检查通过，时间差在允许范围内");
                checkItem.setStatus(CheckItem.Status.SUCCESS);
                checkItem.setMessage(String.format("主机时间同步正常，与服务器时间差为 %d 秒", timeDiff));
            } else {
                cacheLog.debug("时间同步检查未通过: 时间差超出允许范围 (%d秒 > %d秒)", timeDiff, MAX_TIME_DIFF_SECONDS);
                cacheLog.warn("时间同步检查未通过，时间差超过允许范围");
                checkItem.setStatus(CheckItem.Status.FAILED);
                checkItem.setMessage(String.format("主机时间同步异常，与服务器时间差为 %d 秒，超过了允许的 %d 秒",
                        timeDiff, MAX_TIME_DIFF_SECONDS));
            }
            
            cacheLog.debug("时间同步检查结果: %s", checkItem.getStatus());
            cacheLog.debug("时间同步检查消息: %s", checkItem.getMessage());
            cacheLog.debug("======== 时间同步检查完成 ========");
            
        } catch (Exception e) {
            cacheLog.debug("时间同步检查过程中发生异常: %s", e.getMessage());
            cacheLog.debug("异常堆栈: %s", e.toString());
            logger.error("时间同步检查失败: {}", e.getMessage());
            cacheLog.error("时间同步检查失败: %s", e.getMessage());
            checkItem.setStatus(CheckItem.Status.FAILED);
            checkItem.setMessage("时间同步检查失败: " + e.getMessage());
        }
        return checkItem;
    }
    
    @Override
    protected boolean doFix(HostInfo hostInfo, CheckItem checkItem) {
        try {
            logger.info("开始修复主机 {} 的时间同步", hostInfo.getHostname());
            cacheLog.info("开始修复主机 %s 的时间同步", hostInfo.getHostname());
            
            // 第1步：获取服务器当前时区信息
            String serverTimezone = System.getProperty("user.timezone");
            logger.info("服务器当前时区: {}", serverTimezone);
            cacheLog.info("服务器当前时区: %s", serverTimezone);
            
            // 获取更详细的时区信息（如 Asia/Shanghai）
            String serverZoneInfo = execCommand(session, "cat /etc/timezone || ls -l /etc/localtime | grep -o '[A-Za-z0-9/]*$'");
            if (!serverZoneInfo.startsWith("ERROR")) {
                serverZoneInfo = serverZoneInfo.trim();
                logger.info("服务器时区信息: {}", serverZoneInfo);
                cacheLog.info("服务器时区信息: %s", serverZoneInfo);
            } else {
                // 如果无法获取详细信息，使用Java时区
                serverZoneInfo = serverTimezone;
                cacheLog.warn("无法获取详细时区信息，使用Java时区: %s", serverZoneInfo);
            }
            
            // 第2步：设置远程主机时区
            logger.info("设置远程主机时区为: {}", serverZoneInfo);
            cacheLog.info("设置远程主机时区为: %s", serverZoneInfo);
            String setTzResult;
            
            // 尝试使用timedatectl设置时区（适用于systemd系统）
            setTzResult = execCommand(session, "timedatectl set-timezone " + serverZoneInfo);
            if (setTzResult.startsWith("ERROR")) {
                // 如果timedatectl失败，尝试其他方法
                logger.info("timedatectl设置时区失败，尝试其他方法");
                cacheLog.warn("timedatectl设置时区失败，尝试其他方法");
                
                // 检查是否存在/etc/timezone文件（Debian/Ubuntu）
                String checkDebian = execCommand(session, "test -f /etc/timezone && echo 'EXISTS' || echo 'NOT_EXISTS'");
                if (checkDebian.trim().equals("EXISTS")) {
                    logger.info("使用Debian/Ubuntu方式设置时区");
                    cacheLog.info("使用Debian/Ubuntu方式设置时区");
                    setTzResult = execCommand(session, "echo '" + serverZoneInfo + "' > /etc/timezone && dpkg-reconfigure --frontend noninteractive tzdata");
                } else {
                    // 检查是否存在/etc/sysconfig/clock文件（Red Hat/CentOS）
                    String checkRedHat = execCommand(session, "test -f /etc/sysconfig/clock && echo 'EXISTS' || echo 'NOT_EXISTS'");
                    if (checkRedHat.trim().equals("EXISTS")) {
                        logger.info("使用Red Hat/CentOS方式设置时区");
                        cacheLog.info("使用Red Hat/CentOS方式设置时区");
                        setTzResult = execCommand(session, "sed -i 's/^ZONE=.*/ZONE=\"" + serverZoneInfo + "\"/' /etc/sysconfig/clock && ln -sf /usr/share/zoneinfo/" + serverZoneInfo + " /etc/localtime");
                    } else {
                        // 直接链接时区文件（通用方法）
                        logger.info("使用通用方法设置时区");
                        cacheLog.info("使用通用方法设置时区");
                        setTzResult = execCommand(session, "ln -sf /usr/share/zoneinfo/" + serverZoneInfo + " /etc/localtime");
                    }
                }
            }
            
            if (setTzResult.startsWith("ERROR")) {
                logger.error("设置时区失败: {}", setTzResult);
                cacheLog.error("设置时区失败: %s", setTzResult);
                // 继续尝试设置时间，即使时区设置失败
            } else {
                logger.info("时区设置成功");
                cacheLog.info("时区设置成功");
            }
            
            // 第3步：获取当前服务器时间
            Date currentDate = new Date();
            
            // 设置远程主机时间（使用hwclock同步硬件时钟）
            logger.info("设置远程主机系统时间");
            cacheLog.info("设置远程主机系统时间");
            
            // 使用ISO 8601格式设置日期和时间 (YYYY-MM-DD HH:MM:SS)
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String isoDateTime = isoFormat.format(currentDate);
            
            // 尝试使用timedatectl设置系统时间（现代Linux系统首选）
            String setTimeResult = execCommand(session, "timedatectl set-time \"" + isoDateTime + "\"");
            if (setTimeResult.startsWith("ERROR")) {
                // 回退到传统date命令
                logger.info("timedatectl设置时间失败，尝试使用date命令");
                cacheLog.warn("timedatectl设置时间失败，尝试使用date命令");
                
                // 格式化为Linux date命令接受的格式 (MMDDHHmmYYYY.ss)
                SimpleDateFormat dateFormat = new SimpleDateFormat("MMddHHmmyyyy.ss");
                String formattedDate = dateFormat.format(currentDate);
                
                // 直接设置远程主机的系统时间
                setTimeResult = execCommand(session, "date " + formattedDate);
                
                if (setTimeResult.startsWith("ERROR")) {
                    logger.error("设置系统时间失败: {}", setTimeResult);
                    cacheLog.error("设置系统时间失败: %s", setTimeResult);
                    return false;
                }
            }
            
            // 同步硬件时钟
            logger.info("同步远程主机硬件时钟");
            cacheLog.info("同步远程主机硬件时钟");
            String hwClockResult = execCommand(session, "hwclock --systohc");
            if (hwClockResult.startsWith("ERROR")) {
                logger.warn("同步硬件时钟失败: {}", hwClockResult);
                cacheLog.warn("同步硬件时钟失败: %s", hwClockResult);
                // 继续，因为系统时间已经设置
            }
            
            // 第4步：验证时间是否已同步
            logger.info("验证时间同步结果");
            cacheLog.info("验证时间同步结果");
            
            // 验证时区
            String remoteTimezone = execCommand(session, "date +%Z");
            if (remoteTimezone.startsWith("ERROR")) {
                logger.warn("获取远程时区失败: {}", remoteTimezone);
                cacheLog.warn("获取远程时区失败: %s", remoteTimezone);
            } else {
                logger.info("远程主机时区: {}", remoteTimezone.trim());
                cacheLog.info("远程主机时区: %s", remoteTimezone.trim());
            }
            
            // 验证时间同步
            long serverTime = System.currentTimeMillis() / 1000;
            String remoteTimeAfterFix = execCommand(session, "date +%s");
            
            if (remoteTimeAfterFix.startsWith("ERROR")) {
                logger.error("获取修复后的远程主机时间失败: {}", remoteTimeAfterFix);
                cacheLog.error("获取修复后的远程主机时间失败: %s", remoteTimeAfterFix);
                return false;
            }
            
            long remoteTimeAfter = Long.parseLong(remoteTimeAfterFix.trim());
            long timeDiffAfterFix = Math.abs(remoteTimeAfter - serverTime);
            
            logger.info("修复后的时间差: {} 秒", timeDiffAfterFix);
            cacheLog.info("修复后的时间差: %d 秒", timeDiffAfterFix);
            
            if (timeDiffAfterFix <= MAX_TIME_DIFF_SECONDS) {
                logger.info("成功同步主机时间，当前时间差为 {} 秒", timeDiffAfterFix);
                cacheLog.info("成功同步主机时间，当前时间差为 %d 秒", timeDiffAfterFix);
                return true;
            } else {
                logger.error("同步主机时间后仍存在较大时间差: {} 秒", timeDiffAfterFix);
                cacheLog.error("同步主机时间后仍存在较大时间差: %d 秒", timeDiffAfterFix);
                return false;
            }
        } catch (Exception e) {
            logger.error("时间同步修复失败: {}", e.getMessage(), e);
            cacheLog.error("时间同步修复失败: %s", e.getMessage());
            return false;
        }
    }
    
    @Override
    public ItemCode getCheckerType() {
        return ItemCode.TIME_SYNC;
    }
} 