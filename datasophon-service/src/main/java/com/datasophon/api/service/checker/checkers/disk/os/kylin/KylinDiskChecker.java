package com.datasophon.api.service.checker.checkers.disk.os.kylin;

import com.datasophon.api.service.checker.checkers.disk.DiskChecker;
import com.datasophon.api.service.checker.checkers.disk.generic.GenericDiskChecker;
import com.datasophon.api.service.checker.helpers.CheckLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Kylin磁盘检查器实现
 * 适用于Kylin Linux发行版
 */
public class KylinDiskChecker extends GenericDiskChecker {

    private static final Logger log = LoggerFactory.getLogger(KylinDiskChecker.class);

    @Override
    public void provideCleanupSuggestions(CheckLogger cacheLog) {
        cacheLog.warn("Kylin系统磁盘空间不足，建议以下清理措施:");
        cacheLog.warn("1. 清理YUM/APT缓存: sudo yum clean all 或 sudo apt clean");
        cacheLog.warn("2. 删除旧日志文件: sudo find /var/log -type f -name \"*.gz\" -delete");
        cacheLog.warn("3. 清理临时文件: sudo rm -rf /tmp/* /var/tmp/*");
        cacheLog.warn("4. 移除旧内核(YUM系统): sudo package-cleanup --oldkernels --count=2");
        cacheLog.warn("5. 移除旧内核(APT系统): sudo apt autoremove --purge");
        cacheLog.warn("6. 考虑扩展" + DiskChecker.TARGET_DIR + "分区或挂载新磁盘");
    }
}