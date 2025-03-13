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

            // 获取远程主机的Unix时间戳
            String remoteTimeResult = execCommand(session, "date +%s");
            // 获取当前服务器的时间戳
            long serverTime = System.currentTimeMillis() / 1000; // 转换为秒
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
        try {
            logger.info("开始修复主机 {} 的时间同步", hostInfo.getHostname());
            
            // 第1步：获取服务器当前时区信息
            String serverTimezone = System.getProperty("user.timezone");
            logger.info("服务器当前时区: {}", serverTimezone);
            
            // 获取更详细的时区信息（如 Asia/Shanghai）
            String serverZoneInfo = execCommand(session, "cat /etc/timezone || ls -l /etc/localtime | grep -o '[A-Za-z0-9/]*$'");
            if (!serverZoneInfo.startsWith("ERROR")) {
                serverZoneInfo = serverZoneInfo.trim();
                logger.info("服务器时区信息: {}", serverZoneInfo);
            } else {
                // 如果无法获取详细信息，使用Java时区
                serverZoneInfo = serverTimezone;
            }
            
            // 第2步：设置远程主机时区
            logger.info("设置远程主机时区为: {}", serverZoneInfo);
            String setTzResult;
            
            // 尝试使用timedatectl设置时区（适用于systemd系统）
            setTzResult = execCommand(session, "timedatectl set-timezone " + serverZoneInfo);
            if (setTzResult.startsWith("ERROR")) {
                // 如果timedatectl失败，尝试其他方法
                logger.info("timedatectl设置时区失败，尝试其他方法");
                
                // 检查是否存在/etc/timezone文件（Debian/Ubuntu）
                String checkDebian = execCommand(session, "test -f /etc/timezone && echo 'EXISTS' || echo 'NOT_EXISTS'");
                if (checkDebian.trim().equals("EXISTS")) {
                    logger.info("使用Debian/Ubuntu方式设置时区");
                    setTzResult = execCommand(session, "echo '" + serverZoneInfo + "' > /etc/timezone && dpkg-reconfigure --frontend noninteractive tzdata");
                } else {
                    // 检查是否存在/etc/sysconfig/clock文件（Red Hat/CentOS）
                    String checkRedHat = execCommand(session, "test -f /etc/sysconfig/clock && echo 'EXISTS' || echo 'NOT_EXISTS'");
                    if (checkRedHat.trim().equals("EXISTS")) {
                        logger.info("使用Red Hat/CentOS方式设置时区");
                        setTzResult = execCommand(session, "sed -i 's/^ZONE=.*/ZONE=\"" + serverZoneInfo + "\"/' /etc/sysconfig/clock && ln -sf /usr/share/zoneinfo/" + serverZoneInfo + " /etc/localtime");
                    } else {
                        // 直接链接时区文件（通用方法）
                        logger.info("使用通用方法设置时区");
                        setTzResult = execCommand(session, "ln -sf /usr/share/zoneinfo/" + serverZoneInfo + " /etc/localtime");
                    }
                }
            }
            
            if (setTzResult.startsWith("ERROR")) {
                logger.error("设置时区失败: {}", setTzResult);
                // 继续尝试设置时间，即使时区设置失败
            } else {
                logger.info("时区设置成功");
            }
            
            // 第3步：获取当前服务器时间
            Date currentDate = new Date();
            
            // 设置远程主机时间（使用hwclock同步硬件时钟）
            logger.info("设置远程主机系统时间");
            
            // 使用ISO 8601格式设置日期和时间 (YYYY-MM-DD HH:MM:SS)
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String isoDateTime = isoFormat.format(currentDate);
            
            // 尝试使用timedatectl设置系统时间（现代Linux系统首选）
            String setTimeResult = execCommand(session, "timedatectl set-time \"" + isoDateTime + "\"");
            if (setTimeResult.startsWith("ERROR")) {
                // 回退到传统date命令
                logger.info("timedatectl设置时间失败，尝试使用date命令");
                
                // 格式化为Linux date命令接受的格式 (MMDDHHmmYYYY.ss)
                SimpleDateFormat dateFormat = new SimpleDateFormat("MMddHHmmyyyy.ss");
                String formattedDate = dateFormat.format(currentDate);
                
                // 直接设置远程主机的系统时间
                setTimeResult = execCommand(session, "date " + formattedDate);
                
                if (setTimeResult.startsWith("ERROR")) {
                    logger.error("设置系统时间失败: {}", setTimeResult);
                    return false;
                }
            }
            
            // 同步硬件时钟
            logger.info("同步远程主机硬件时钟");
            String hwClockResult = execCommand(session, "hwclock --systohc");
            if (hwClockResult.startsWith("ERROR")) {
                logger.warn("同步硬件时钟失败: {}", hwClockResult);
                // 继续，因为系统时间已经设置
            }
            
            // 第4步：验证时间是否已同步
            logger.info("验证时间同步结果");
            
            // 验证时区
            String remoteTimezone = execCommand(session, "date +%Z");
            if (remoteTimezone.startsWith("ERROR")) {
                logger.warn("获取远程时区失败: {}", remoteTimezone);
            } else {
                logger.info("远程主机时区: {}", remoteTimezone.trim());
            }
            
            // 验证时间同步
            long serverTime = System.currentTimeMillis() / 1000;
            String remoteTimeAfterFix = execCommand(session, "date +%s");
            
            if (remoteTimeAfterFix.startsWith("ERROR")) {
                logger.error("获取修复后的远程主机时间失败: {}", remoteTimeAfterFix);
                return false;
            }
            
            long remoteTimeAfter = Long.parseLong(remoteTimeAfterFix.trim());
            long timeDiffAfterFix = Math.abs(remoteTimeAfter - serverTime);
            
            logger.info("修复后的时间差: {} 秒", timeDiffAfterFix);
            
            if (timeDiffAfterFix <= MAX_TIME_DIFF_SECONDS) {
                logger.info("成功同步主机时间，当前时间差为 {} 秒", timeDiffAfterFix);
                return true;
            } else {
                logger.error("同步主机时间后仍存在较大时间差: {} 秒", timeDiffAfterFix);
                return false;
            }
        } catch (Exception e) {
            logger.error("时间同步修复失败: {}", e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    protected ItemCode getCheckerType() {
        return ItemCode.TIME_SYNC;
    }
} 