package com.datasophon.api.service.checker.impl.firewall.impl;

import com.datasophon.api.service.checker.CommandResult;
import com.datasophon.api.service.checker.impl.firewall.AbstractFirewallChecker;
import com.datasophon.api.service.checker.impl.firewall.FirewallCheckResult;
import com.datasophon.common.model.OSInfo;

/**
 * CentOS防火墙检查器
 * 主要用于处理CentOS 7及以上版本的firewalld服务
 */
public class CentOSFirewallChecker extends AbstractFirewallChecker {
    
    private final CommandExecutor commandExecutor;
    
    public CentOSFirewallChecker(CommandExecutor commandExecutor) {
        this.commandExecutor = commandExecutor;
    }
    
    @Override
    public boolean isApplicable(OSInfo osInfo) {
        return osInfo != null && osInfo.isCentOS() && osInfo.getMajorVersion() >= 7;
    }
    
    @Override
    protected String getCheckCommand() {
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
                return FirewallCheckResult.failure("服务不存在", result.getErrorOrOutput(), result.getExitCode());
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