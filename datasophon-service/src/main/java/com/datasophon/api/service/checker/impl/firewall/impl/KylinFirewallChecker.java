package com.datasophon.api.service.checker.impl.firewall.impl;

import com.datasophon.api.service.checker.CommandResult;
import com.datasophon.api.service.checker.impl.firewall.AbstractFirewallChecker;
import com.datasophon.api.service.checker.impl.firewall.FirewallCheckResult;
import com.datasophon.common.model.OSInfo;

/**
 * 麒麟Linux防火墙检查器
 * 适用于麒麟Linux系统(基于RedHat)
 */
public class KylinFirewallChecker extends AbstractFirewallChecker {
    
    private final CommandExecutor commandExecutor;
    
    public KylinFirewallChecker(CommandExecutor commandExecutor) {
        this.commandExecutor = commandExecutor;
    }
    
    @Override
    public boolean isApplicable(OSInfo osInfo) {
        return osInfo != null && osInfo.isKylin();
    }
    
    @Override
    protected String getCheckCommand() {
        // 麒麟系统支持systemctl，使用firewalld服务
        return "systemctl status firewalld";
    }
    
    @Override
    protected String getEnableCommand() {
        return "systemctl start firewalld && systemctl enable firewalld";
    }
    
    @Override
    protected String getDisableCommand() {
        return "systemctl stop firewalld && systemctl disable firewalld";
    }
    
    @Override
    protected CommandResult executeCommand(String command) throws Exception {
        return commandExecutor.execute(command);
    }
    
    @Override
    protected FirewallCheckResult parseFirewallState(CommandResult result) {
        if (!result.isSuccess()) {
            // 当退出码为3时，表示服务未运行
            if (result.getExitCode() == 3 || result.getErrorOrOutput().contains("inactive")) {
                return FirewallCheckResult.success(false, result.getErrorOrOutput(), result.getExitCode());
            } else if (result.getExitCode() == 4) {
                // 退出码为4时，表示服务不存在
                logger.warn("麒麟系统中未找到firewalld服务，尝试检查iptables服务");
                try {
                    // 尝试检查iptables服务
                    CommandResult iptablesResult = commandExecutor.execute("systemctl status iptables");
                    if (iptablesResult.isSuccess() || iptablesResult.getExitCode() == 3) {
                        String output = iptablesResult.getOutput().toLowerCase();
                        boolean enabled = output.contains("active (exited)") || output.contains("active (running)");
                        return FirewallCheckResult.success(enabled, iptablesResult.getOutput(), iptablesResult.getExitCode());
                    }
                } catch (Exception e) {
                    logger.error("检查iptables服务状态失败: {}", e.getMessage());
                }
                return FirewallCheckResult.failure("防火墙服务不存在", result.getErrorOrOutput(), result.getExitCode());
            } else {
                return FirewallCheckResult.failure("命令执行失败", result.getErrorOrOutput(), result.getExitCode());
            }
        }
        
        String output = result.getOutput().toLowerCase();
        if (output.contains("active (running)") || output.contains("active (exited)")) {
            return FirewallCheckResult.success(true, result.getOutput(), result.getExitCode());
        } else if (output.contains("inactive") || output.contains("dead")) {
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