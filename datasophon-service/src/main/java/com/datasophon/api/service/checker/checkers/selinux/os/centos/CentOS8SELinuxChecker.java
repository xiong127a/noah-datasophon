package com.datasophon.api.service.checker.checkers.selinux.os.centos;

import com.datasophon.api.service.checker.helpers.CheckLogger;
import com.datasophon.common.model.CheckItem;
import com.datasophon.common.model.HostInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CentOS 8系统专用的SELinux检查器实现
 */
public class CentOS8SELinuxChecker extends CentOSSELinuxChecker {

    private static final Logger log = LoggerFactory.getLogger(CentOS8SELinuxChecker.class);

    @Override
    public CheckItem check(HostInfo hostInfo, CheckItem checkItem, CheckLogger cacheLog) {
        cacheLog.info("使用CentOS 8专用的SELinux检查器...");

        // 调用CentOS通用检查
        return super.check(hostInfo, checkItem, cacheLog);
    }

    @Override
    public boolean fix(HostInfo hostInfo, CheckItem checkItem, CheckLogger cacheLog) throws InterruptedException {
        cacheLog.info("使用CentOS 8专用的SELinux修复方法...");

        // 调用CentOS通用修复方法
        return super.fix(hostInfo, checkItem, cacheLog);
    }
}