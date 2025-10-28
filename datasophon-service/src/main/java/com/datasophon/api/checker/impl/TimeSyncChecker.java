package com.datasophon.api.checker.impl;

import com.datasophon.api.checker.CheckResult;
import com.datasophon.api.checker.EnvironmentCheckItem;
import com.datasophon.api.checker.HostCheckContext;
import com.datasophon.api.checker.util.CheckLogWriter;
import com.datasophon.api.config.CheckerProperties;
import com.datasophon.common.vo.environment.RepairResult;
import com.datasophon.plugins.api.factory.SshConnectionServiceFactory;
import com.datasophon.plugins.api.service.SshConnectionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 时间同步检查器
 * 检查远程主机时间与本地管理端时间的差异
 * 
 * @author 任相鹏
 * @date 2025-01-27
 */
@Slf4j
@Component
public class TimeSyncChecker implements EnvironmentCheckItem {
    
    @Autowired
    private CheckerProperties checkerProperties;
    
    @Autowired
    private CheckLogWriter checkLogWriter;
    
    private SshConnectionService sshService;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * 获取SSH服务（延迟加载）
     */
    private SshConnectionService getSshService() {
        if (sshService == null) {
            sshService = SshConnectionServiceFactory.getInstance().getDefaultSshConnectionService();
        }
        return sshService;
    }
    
    /**
     * 转换为插件API的HostCheckContext
     */
    private com.datasophon.plugins.api.model.HostCheckContext toPluginContext(HostCheckContext context) {
        return com.datasophon.plugins.api.model.HostCheckContext.builder()
                .hostIp(context.getHostIp())
                .sshUser(context.getSshUser())
                .sshPassword(context.getSshPassword())
                .sshPort(context.getSshPort())
                .build();
    }
    
    @Override
    public String getCheckKey() {
        return "time-sync";
    }
    
    @Override
    public String getDisplayName() {
        return "时间同步检查";
    }
    
    @Override
    public int getPriority() {
        return checkerProperties.getTimeSync().getPriority();
    }
    
    @Override
    public boolean isEnabled() {
        return checkerProperties.getTimeSync().isEnabled();
    }
    
    @Override
    public CheckResult execute(HostCheckContext context) {
        log.info("检查时间同步: host={}", context.getHostIp());
        
        try {
            // 步骤1：获取本地管理端时间戳（秒）
            long localTimestamp = Instant.now().getEpochSecond();
            
            // 步骤2：SSH到目标主机获取远程时间戳
            var result = getSshService().executeCommand(
                toPluginContext(context), 
                "date +%s", 
                10
            );
            
            if (!result.isSuccess()) {
                log.error("获取远程主机时间失败: host={}, error={}", context.getHostIp(), result.error());
                return CheckResult.failure(
                    "无法获取远程主机时间",
                    "请检查date命令是否可用",
                    false,
                    false
                );
            }
            
            long remoteTimestamp;
            try {
                remoteTimestamp = Long.parseLong(result.output().trim());
            } catch (NumberFormatException e) {
                log.error("解析远程时间戳失败: host={}, output={}", context.getHostIp(), result.output());
                return CheckResult.failure(
                    "解析远程时间戳失败",
                    "远程主机返回的时间格式不正确",
                    false,
                    false
                );
            }
            
            // 步骤3：计算时间差（绝对值）
            long timeDiff = Math.abs(localTimestamp - remoteTimestamp);
            
            // 步骤4：检查是否超过配置的最大时间差
            int maxTimeDiff = checkerProperties.getTimeSync().getMaxTimeDiffSeconds();
            
            // 构建details
            Map<String, Object> details = new HashMap<>();
            details.put("timeDiff", timeDiff);
            details.put("localTimestamp", localTimestamp);
            details.put("remoteTimestamp", remoteTimestamp);
            details.put("maxAllowed", maxTimeDiff);
            
            if (timeDiff <= maxTimeDiff) {
                log.info("时间同步检查通过: host={}, timeDiff={}秒", context.getHostIp(), timeDiff);
                return CheckResult.builder()
                        .success(true)
                        .status(com.datasophon.common.enums.CheckItemStatus.SUCCESS)
                        .message(String.format("时间差异: %d秒（在允许范围内）", timeDiff))
                        .details(details)
                        .canSkip(false)
                        .canRepair(false)
                        .build();
            } else {
                log.warn("时间差异超过阈值: host={}, timeDiff={}秒, maxAllowed={}秒", 
                        context.getHostIp(), timeDiff, maxTimeDiff);
                return CheckResult.builder()
                        .success(false)
                        .status(com.datasophon.common.enums.CheckItemStatus.FAILED)
                        .message(String.format("时间差异: %d秒（超过允许的%d秒）", timeDiff, maxTimeDiff))
                        .recommendation("需要同步时间。可以点击修复按钮自动同步管理端时间")
                        .details(details)
                        .canSkip(false)
                        .canRepair(true)
                        .build();
            }
            
        } catch (Exception e) {
            log.error("时间同步检查失败: host={}, error={}", context.getHostIp(), e.getMessage(), e);
            return CheckResult.failure(
                "时间同步检查失败: " + e.getMessage(),
                "请检查SSH连接是否正常",
                false,
                false
            );
        }
    }
    
    @Override
    public RepairResult repair(HostCheckContext context, Map<String, Object> params) {
        Long clusterId = context.getClusterId();
        String hostIp = context.getHostIp();
        String checkKey = getCheckKey();
        
        log.info("开始修复时间同步: host={}", hostIp);
        checkLogWriter.logRepairStart(clusterId, hostIp, checkKey, "开始修复时间同步");
        
        try {
            // 步骤1：获取修复前的时间差信息
            CheckResult beforeResult = execute(context);
            log.info("修复前检查结果: host={}, status={}, message={}", 
                    hostIp, beforeResult.getStatus(), beforeResult.getMessage());
            
            Map<String, Object> beforeDetails = new HashMap<>();
            beforeDetails.put("status", beforeResult.getStatus().name());
            beforeDetails.put("message", beforeResult.getMessage());
            checkLogWriter.logRepairInfo(clusterId, hostIp, checkKey, 
                    "修复前检查: " + beforeResult.getMessage(), beforeDetails);
            
            // 步骤2：获取本地管理端时间和时区
            ZoneId localZoneId = ZoneId.systemDefault();
            String timeZone = localZoneId.getId(); // 例如: Asia/Shanghai
            LocalDateTime localTime = LocalDateTime.now();
            String timeString = localTime.format(DATE_FORMATTER);
            long localTimestamp = System.currentTimeMillis() / 1000;
            
            log.info("本地管理端时间: timeString={}, timezone={}, timestamp={}", timeString, timeZone, localTimestamp);
            
            Map<String, Object> timeDetails = new HashMap<>();
            timeDetails.put("managementTime", timeString);
            timeDetails.put("timezone", timeZone);
            timeDetails.put("timestamp", localTimestamp);
            checkLogWriter.logRepairInfo(clusterId, hostIp, checkKey, 
                    "管理端当前时间: " + timeString + " (时区: " + timeZone + ")", timeDetails);
            
            // 步骤3：先设置远程主机的时区为管理端时区
            String timezoneCommand = String.format("sudo timedatectl set-timezone %s", timeZone);
            log.info("设置远程主机时区: host={}, timezone={}, command={}", hostIp, timeZone, timezoneCommand);
            checkLogWriter.logRepairInfo(clusterId, hostIp, checkKey, 
                    "设置远程主机时区: " + timeZone, Map.of("command", timezoneCommand));
            
            var timezoneResult = getSshService().executeCommand(
                toPluginContext(context), 
                timezoneCommand, 
                30
            );
            
            if (!timezoneResult.isSuccess()) {
                // 如果timedatectl失败，尝试使用传统方法设置时区
                log.warn("timedatectl设置时区失败，尝试传统方法: host={}, error={}", hostIp, timezoneResult.error());
                String fallbackCommand = String.format("sudo ln -sf /usr/share/zoneinfo/%s /etc/localtime", timeZone);
                checkLogWriter.logRepairInfo(clusterId, hostIp, checkKey, 
                        "使用传统方法设置时区", Map.of("command", fallbackCommand));
                
                var fallbackResult = getSshService().executeCommand(
                    toPluginContext(context), 
                    fallbackCommand, 
                    30
                );
                
                if (!fallbackResult.isSuccess()) {
                    String errorMsg = "设置时区失败，但继续尝试同步时间";
                    log.warn("{}: host={}", errorMsg, hostIp);
                    checkLogWriter.logRepairInfo(clusterId, hostIp, checkKey, errorMsg, Map.of("error", fallbackResult.error()));
                }
            } else {
                log.info("时区设置成功: host={}, timezone={}", hostIp, timeZone);
                checkLogWriter.logRepairInfo(clusterId, hostIp, checkKey, 
                        "时区设置成功", Map.of("timezone", timeZone, "output", timezoneResult.output()));
            }
            
            // 步骤4：分步执行时间同步（先date -s，成功后再hwclock）
            String dateCommand = String.format("sudo %s \"%s\"", 
                    checkerProperties.getTimeSync().getSyncCommand(), timeString);
            
            log.info("执行时间同步命令: host={}, command={}", hostIp, dateCommand);
            checkLogWriter.logRepairInfo(clusterId, hostIp, checkKey, 
                    "执行系统时间同步命令", Map.of("command", dateCommand));
            
            var dateResult = getSshService().executeCommand(
                toPluginContext(context), 
                dateCommand, 
                30
            );
            
            if (!dateResult.isSuccess()) {
                String errorMsg = String.format("设置系统时间失败: %s", dateResult.error());
                log.error("时间同步失败: host={}, error={}", hostIp, errorMsg);
                
                Map<String, Object> errorDetails = new HashMap<>();
                errorDetails.put("command", dateCommand);
                errorDetails.put("error", dateResult.error());
                checkLogWriter.logRepairError(clusterId, hostIp, checkKey, errorMsg, errorDetails);
                
                return RepairResult.builder()
                        .success(false)
                        .message(errorMsg)
                        .details("请检查是否有sudo权限，以及date命令是否可用")
                        .build();
            }
            
            log.info("设置系统时间成功: host={}, output={}", hostIp, dateResult.output());
            checkLogWriter.logRepairInfo(clusterId, hostIp, checkKey, 
                    "系统时间设置成功", Map.of("output", dateResult.output()));
            
            // 步骤5：同步到硬件时钟
            String hwclockCommand = "sudo hwclock --systohc";
            checkLogWriter.logRepairInfo(clusterId, hostIp, checkKey, 
                    "同步到硬件时钟", Map.of("command", hwclockCommand));
            
            var hwclockResult = getSshService().executeCommand(
                toPluginContext(context), 
                hwclockCommand, 
                10
            );
            
            if (!hwclockResult.isSuccess()) {
                log.warn("硬件时钟同步失败（不影响系统时间）: host={}, error={}", 
                        hostIp, hwclockResult.error());
                checkLogWriter.logRepairInfo(clusterId, hostIp, checkKey, 
                        "硬件时钟同步失败（不影响系统时间）", 
                        Map.of("error", hwclockResult.error()));
            } else {
                log.info("硬件时钟同步成功: host={}", hostIp);
                checkLogWriter.logRepairInfo(clusterId, hostIp, checkKey, 
                        "硬件时钟同步成功", Map.of("output", hwclockResult.output()));
            }
            
            // 步骤6：等待2秒后验证（给足够时间让时间同步生效）
            checkLogWriter.logRepairInfo(clusterId, hostIp, checkKey, 
                    "等待时间同步生效...", null);
            Thread.sleep(2000);
            
            // 步骤7：验证时间差是否已在允许范围内
            checkLogWriter.logRepairInfo(clusterId, hostIp, checkKey, 
                    "验证时间同步结果...", null);
            
            CheckResult verifyResult = execute(context);
            
            // 获取当前时间差（从details中）
            Object timeDiffObj = verifyResult.getDetails() != null ? 
                    verifyResult.getDetails().get("timeDiff") : null;
            String timeDiffInfo = timeDiffObj != null ? String.valueOf(timeDiffObj) + "秒" : "未知";
            
            log.info("验证结果: host={}, status={}, message={}, timeDiff={}", 
                    hostIp, verifyResult.getStatus(), 
                    verifyResult.getMessage(), timeDiffInfo);
            
            Map<String, Object> verifyDetails = new HashMap<>();
            verifyDetails.put("status", verifyResult.getStatus().name());
            verifyDetails.put("message", verifyResult.getMessage());
            verifyDetails.put("timeDiff", timeDiffInfo);
            
            if (verifyResult.getStatus() == com.datasophon.common.enums.CheckItemStatus.SUCCESS) {
                String successMsg = String.format("时间同步成功，当前时间差: %s", timeDiffInfo);
                checkLogWriter.logRepairSuccess(clusterId, hostIp, checkKey, successMsg, verifyDetails);
                
                return RepairResult.builder()
                        .success(true)
                        .message(successMsg)
                        .build();
            } else {
                String failMsg = String.format("时间同步后验证失败，当前时间差: %s（超过允许的%d秒）", 
                        timeDiffInfo, checkerProperties.getTimeSync().getMaxTimeDiffSeconds());
                checkLogWriter.logRepairError(clusterId, hostIp, checkKey, failMsg, verifyDetails);
                
                return RepairResult.builder()
                        .success(false)
                        .message(failMsg)
                        .details(verifyResult.getRecommendation())
                        .build();
            }
            
        } catch (Exception e) {
            log.error("修复时间同步失败: host={}, error={}", hostIp, e.getMessage(), e);
            
            Map<String, Object> errorDetails = new HashMap<>();
            errorDetails.put("exception", e.getClass().getSimpleName());
            errorDetails.put("error", e.getMessage());
            checkLogWriter.logRepairError(clusterId, hostIp, checkKey, 
                    "修复失败: " + e.getMessage(), errorDetails);
            
            return RepairResult.builder()
                    .success(false)
                    .message("修复失败: " + e.getMessage())
                    .details("请检查SSH连接和sudo权限是否正常")
                    .build();
        }
    }
}

