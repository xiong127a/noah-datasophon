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
            
            if (timeDiff <= maxTimeDiff) {
                log.info("时间同步检查通过: host={}, timeDiff={}秒", context.getHostIp(), timeDiff);
                return CheckResult.success(String.format("时间差异: %d秒（在允许范围内）", timeDiff));
            } else {
                log.warn("时间差异超过阈值: host={}, timeDiff={}秒, maxAllowed={}秒", 
                        context.getHostIp(), timeDiff, maxTimeDiff);
                return CheckResult.failure(
                    String.format("时间差异: %d秒（超过允许的%d秒）", timeDiff, maxTimeDiff),
                    "需要同步时间。可以点击修复按钮自动同步管理端时间",
                    false,
                    true
                );
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
        log.info("开始修复时间同步: host={}", context.getHostIp());
        
        try {
            // 步骤1：获取本地管理端时间（格式化）
            LocalDateTime localTime = LocalDateTime.now();
            String timeString = localTime.format(DATE_FORMATTER);
            
            log.info("本地管理端时间: {}", timeString);
            
            // 步骤2：SSH到目标主机执行时间同步
            String syncCommand = String.format(
                "sudo %s \"%s\" && sudo hwclock --systohc || true",
                checkerProperties.getTimeSync().getSyncCommand(),
                timeString
            );
            
            var result = getSshService().executeCommand(
                toPluginContext(context), 
                syncCommand, 
                30
            );
            
            if (!result.isSuccess()) {
                log.error("时间同步失败: host={}, error={}", context.getHostIp(), result.error());
                return RepairResult.builder()
                        .success(false)
                        .message("时间同步失败: " + result.error())
                        .build();
            }
            
            log.info("时间同步命令执行成功: host={}, output={}", context.getHostIp(), result.output());
            
            // 步骤3：验证时间差是否已在允许范围内
            Thread.sleep(1000); // 等待1秒确保时间已同步
            
            CheckResult verifyResult = execute(context);
            if (verifyResult.getStatus() == com.datasophon.common.enums.CheckItemStatus.SUCCESS) {
                return RepairResult.builder()
                        .success(true)
                        .message("时间同步成功")
                        .build();
            } else {
                return RepairResult.builder()
                        .success(false)
                        .message("时间同步后验证失败，时间差仍超过阈值")
                        .build();
            }
            
        } catch (Exception e) {
            log.error("修复时间同步失败: host={}, error={}", context.getHostIp(), e.getMessage(), e);
            return RepairResult.builder()
                    .success(false)
                    .message("修复失败: " + e.getMessage())
                    .build();
        }
    }
}

