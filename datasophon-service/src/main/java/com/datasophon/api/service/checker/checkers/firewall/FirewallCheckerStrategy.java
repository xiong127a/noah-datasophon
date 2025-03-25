package com.datasophon.api.service.checker.checkers.firewall;

import com.datasophon.api.service.checker.helpers.CheckLogger;
import com.datasophon.common.model.CheckItem;
import com.datasophon.common.model.HostInfo;

/**
 * 防火墙检查器策略接口
 * 不同操作系统的防火墙检查策略实现此接口
 */
public interface FirewallCheckerStrategy {

    /**
     * 执行防火墙检查
     * 
     * @param hostInfo  主机信息
     * @param checkItem 检查项
     * @param cacheLog  日志缓存
     * @return 检查结果
     * @throws InterruptedException 如果检查过程被中断
     */
    CheckItem check(HostInfo hostInfo, CheckItem checkItem, CheckLogger cacheLog) throws InterruptedException;

    /**
     * 执行防火墙修复
     * 
     * @param hostInfo  主机信息
     * @param checkItem 检查项
     * @param cacheLog  日志缓存
     * @return 修复是否成功
     * @throws InterruptedException 如果修复过程被中断
     */
    boolean fix(HostInfo hostInfo, CheckItem checkItem, CheckLogger cacheLog) throws InterruptedException;
}