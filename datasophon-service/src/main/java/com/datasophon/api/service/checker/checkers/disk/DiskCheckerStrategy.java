package com.datasophon.api.service.checker.checkers.disk;

import com.datasophon.api.service.checker.helpers.CheckLogger;
import com.datasophon.common.model.CheckItem;
import com.datasophon.common.model.HostInfo;

/**
 * 磁盘检查器策略接口
 * 不同操作系统的磁盘检查策略实现此接口
 */
public interface DiskCheckerStrategy {

    /**
     * 执行磁盘空间检查
     * 
     * @param hostInfo  主机信息
     * @param checkItem 检查项
     * @param cacheLog  日志缓存
     * @return 检查结果
     * @throws InterruptedException 如果检查过程被中断
     */
    CheckItem check(HostInfo hostInfo, CheckItem checkItem, CheckLogger cacheLog) throws InterruptedException;

    /**
     * 提供磁盘清理建议
     * 
     * @param cacheLog 日志缓存
     */
    void provideCleanupSuggestions(CheckLogger cacheLog);
}