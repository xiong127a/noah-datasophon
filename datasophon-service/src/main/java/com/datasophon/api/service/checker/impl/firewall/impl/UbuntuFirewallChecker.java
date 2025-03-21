package com.datasophon.api.service.checker.impl.firewall.impl;

import com.datasophon.api.service.checker.CommandResult;
import com.datasophon.api.service.checker.impl.firewall.AbstractFirewallChecker;
import com.datasophon.api.service.checker.impl.firewall.FirewallCheckResult;
import com.datasophon.common.model.OSInfo;

/**
 * Ubuntu防火墙检查器
 * 适用于Ubuntu系统的ufw防火墙
 */
public class UbuntuFirewallChecker extends AbstractFirewallChecker {
    
    private final CommandExecutor commandExecutor;
    
    public UbuntuFirewallChecker(CommandExecutor commandExecutor) {
        this.commandExecutor = commandExecutor;
    }
    
    @Override
    public boolean isApplicable(OSInfo osInfo) {
        return osInfo != null && osInfo.isUbuntu();
    }
    
    @Override
    protected String getCheckCommand() {
        return "ufw status";
    }
    
    @Override
    protected String getEnableCommand() {
        // 非交互模式下启用ufw
        return "echo 'y' | ufw enable";
    }
    
    @Override
    protected String getDisableCommand() {
        return "ufw disable";
    }
    
    @Override
    protected CommandResult executeCommand(String command) throws Exception {
        return commandExecutor.execute(command);
    }
    
    @Override
    protected FirewallCheckResult parseFirewallState(CommandResult result) {
        if (!result.isSuccess()) {
            // 如果命令不存在
            if (result.getErrorOrOutput().contains("command not found")) {
                return FirewallCheckResult.failure("ufw未安装", result.getErrorOrOutput(), result.getExitCode());
            } else {
                return FirewallCheckResult.failure("命令执行失败", result.getErrorOrOutput(), result.getExitCode());
            }
        }
        
        String output = result.getOutput().toLowerCase();
        if (output.contains("active") || output.contains("启用")) {
            return FirewallCheckResult.success(true, result.getOutput(), result.getExitCode());
        } else if (output.contains("inactive") || output.contains("disabled") || output.contains("未启用")) {
            return FirewallCheckResult.success(false, result.getOutput(), result.getExitCode());
        } else {
            // 无法确定状态
            return FirewallCheckResult.failure("无法确定防火墙状态", result.getOutput(), result.getExitCode());
        }
    }
    
    /**
     * 命令执行器接口
     * 用于解耦命令执行方式，便于单元测试
     */
    public interface CommandExecutor {
        CommandResult execute(String command) throws Exception;
    }
} 