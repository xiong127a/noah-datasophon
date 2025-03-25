package com.datasophon.api.service.checker.checkers.disk.os.kylin;

import com.datasophon.api.service.checker.checkers.disk.DiskChecker;
import com.datasophon.api.service.checker.helpers.CheckLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Kylin V4磁盘检查器实现
 * 专用于Kylin V4 Linux发行版
 */
public class KylinV4DiskChecker extends KylinDiskChecker {

    private static final Logger log = LoggerFactory.getLogger(KylinV4DiskChecker.class);

    @Override
    public void provideCleanupSuggestions(CheckLogger cacheLog) {
        cacheLog.warn("Kylin V4系统磁盘空间不足，建议以下清理措施:");
        cacheLog.warn("1. 清理APT缓存: sudo apt clean && sudo apt autoclean");
        cacheLog.warn("2. 删除不需要的软件包: sudo apt autoremove --purge");
        cacheLog.warn("3. 删除旧日志文件: sudo find /var/log -type f -name \"*.gz\" -delete");
        cacheLog.warn("4. 清理临时文件: sudo rm -rf /tmp/* /var/tmp/*");
        cacheLog.warn("5. 清理旧内核: sudo apt-get purge -y linux-image-old*");
        cacheLog.warn("6. 清理journal日志: sudo journalctl --vacuum-time=7d");
        cacheLog.warn("7. 考虑扩展" + DiskChecker.TARGET_DIR + "分区或挂载新磁盘");
    }
}