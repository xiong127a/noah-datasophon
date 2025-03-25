package com.datasophon.api.service.checker.checkers.disk.os.ubuntu;

import com.datasophon.api.service.checker.checkers.disk.DiskChecker;
import com.datasophon.api.service.checker.checkers.disk.generic.GenericDiskChecker;
import com.datasophon.api.service.checker.helpers.CheckLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ubuntu磁盘检查器实现
 * 适用于Ubuntu Linux发行版
 */
public class UbuntuDiskChecker extends GenericDiskChecker {

    private static final Logger log = LoggerFactory.getLogger(UbuntuDiskChecker.class);

    @Override
    public void provideCleanupSuggestions(CheckLogger cacheLog) {
        cacheLog.warn("Ubuntu系统磁盘空间不足，建议以下清理措施:");
        cacheLog.warn("1. 清理APT缓存: sudo apt clean && sudo apt autoclean");
        cacheLog.warn("2. 删除不需要的软件包: sudo apt autoremove --purge");
        cacheLog.warn("3. 删除旧日志文件: sudo find /var/log -type f -name \"*.gz\" -delete");
        cacheLog.warn("4. 清理临时文件: sudo rm -rf /tmp/* /var/tmp/*");
        cacheLog.warn("5. 清理旧内核: sudo purge-old-kernels --keep 2");
        cacheLog.warn("6. 考虑扩展" + DiskChecker.TARGET_DIR + "分区或挂载新磁盘");
    }
}