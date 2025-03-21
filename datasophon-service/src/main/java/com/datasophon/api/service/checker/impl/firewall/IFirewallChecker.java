package com.datasophon.api.service.checker.impl.firewall;

import com.datasophon.api.service.checker.CommandResult;
import com.datasophon.common.model.OSInfo;

/**
 * 防火墙检查器接口
 * 定义了防火墙检查器的基本方法
 */
public interface IFirewallChecker {
    
    /**
     * 检查防火墙状态
     * @return 防火墙检查结果
     */
    FirewallCheckResult checkFirewallState(String command);
    
    /**
     * 修复防火墙状态（启用或禁用）
     * @param enable 是否启用防火墙
     * @return 操作结果
     */
    CommandResult fixFirewallState(boolean enable);
    
    /**
     * 判断当前检查器是否适用于给定的操作系统
     * @param osInfo 操作系统信息
     * @return 如果适用返回true，否则返回false
     */
    boolean isApplicable(OSInfo osInfo);
} 