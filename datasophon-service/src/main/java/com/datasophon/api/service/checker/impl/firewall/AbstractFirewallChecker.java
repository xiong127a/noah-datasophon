package com.datasophon.api.service.checker.impl.firewall;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datasophon.api.service.checker.CommandResult;

/**
 * 抽象防火墙检查器
 * 提供基础功能和默认实现
 */
public abstract class AbstractFirewallChecker implements IFirewallChecker {
    
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    
    /**
     * 从命令输出解析防火墙状态
     * @param result 命令执行结果
     * @return 防火墙检查结果
     */
    protected abstract FirewallCheckResult parseFirewallState(CommandResult result);
    
    /**
     * 获取检查防火墙状态的命令
     * @return 检查命令
     */
    protected abstract String getCheckCommand();
    
    /**
     * 获取启用防火墙的命令
     * @return 启用命令
     */
    protected abstract String getEnableCommand();
    
    /**
     * 获取禁用防火墙的命令
     * @return 禁用命令
     */
    protected abstract String getDisableCommand();
    
    @Override
    public FirewallCheckResult checkFirewallState(String command) {
        if (command == null || command.isEmpty()) {
            command = getCheckCommand();
        }
        
        try {
            // 在子类中实现实际执行命令的逻辑
            CommandResult result = executeCommand(command);
            return parseFirewallState(result);
        } catch (Exception e) {
            logger.error("检查防火墙状态时发生错误: {}", e.getMessage(), e);
            return FirewallCheckResult.failure("执行命令异常: " + e.getMessage(), "", -1);
        }
    }
    
    @Override
    public CommandResult fixFirewallState(boolean enable) {
        String command = enable ? getEnableCommand() : getDisableCommand();
        try {
            return executeCommand(command);
        } catch (Exception e) {
            logger.error("{}防火墙时发生错误: {}", enable ? "启用" : "禁用", e.getMessage(), e);
            return new CommandResult("", e.getMessage(), -1);
        }
    }
    
    /**
     * 执行命令
     * 需要在子类中实现，子类可以通过SSH或其他方式执行命令
     * 
     * @param command 要执行的命令
     * @return 命令执行结果
     */
    protected abstract CommandResult executeCommand(String command) throws Exception;
} 