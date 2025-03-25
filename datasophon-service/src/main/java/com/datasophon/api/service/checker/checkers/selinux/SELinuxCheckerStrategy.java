package com.datasophon.api.service.checker.checkers.selinux;

import com.datasophon.api.service.checker.helpers.CheckLogger;
import com.datasophon.common.model.CheckItem;
import com.datasophon.common.model.HostInfo;

/**
 * SELinux检查器策略接口
 * 不同操作系统的SELinux检查策略实现此接口
 */
public interface SELinuxCheckerStrategy {

    /**
     * 执行SELinux检查
     * 
     * @param hostInfo  主机信息
     * @param checkItem 检查项
     * @param cacheLog  日志缓存
     * @return 检查结果
     */
    CheckItem check(HostInfo hostInfo, CheckItem checkItem, CheckLogger cacheLog);

    /**
     * 修复SELinux配置
     * 
     * @param hostInfo  主机信息
     * @param checkItem 检查项
     * @param cacheLog  日志缓存
     * @return 修复是否成功
     * @throws InterruptedException 如果修复过程被中断
     */
    boolean fix(HostInfo hostInfo, CheckItem checkItem, CheckLogger cacheLog) throws InterruptedException;
}