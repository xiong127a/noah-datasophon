package com.datasophon.api.service.checker.checkers.firewall;

import com.datasophon.api.service.checker.helpers.CheckLogger;
import com.datasophon.common.enums.OsDistribution;
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
     */
    boolean fix(HostInfo hostInfo, CheckItem checkItem, CheckLogger cacheLog);

    /**
     * 获取支持的操作系统类型
     * 
     * @return 支持的操作系统类型
     */
    OsDistribution getSupportedOs();

    /**
     * 设置支持的操作系统类型
     * 
     * @param osDistribution 支持的操作系统类型
     */
    void setSupportedOs(OsDistribution osDistribution);

    /**
     * 获取版本前缀（例如"7"表示支持CentOS 7.x）
     * 通用检查器可以没有此方法的实际实现（可以返回null）
     * 
     * @return 版本前缀
     */
    default String getVersionPrefix() {
        return null;
    }

    /**
     * 设置版本前缀
     * 
     * @param versionPrefix 版本前缀
     */
    default void setVersionPrefix(String versionPrefix) {
        // 默认实现为空，由具体的版本特定检查器实现
    }
}