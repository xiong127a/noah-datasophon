package com.datasophon.api.service.checker.checkers.disk.os.ubuntu;

import com.datasophon.api.service.checker.checkers.disk.DiskChecker;
import com.datasophon.api.service.checker.helpers.CheckLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ubuntu 24磁盘检查器实现
 * 专用于Ubuntu 24.04 Linux发行版
 */
public class Ubuntu24DiskChecker extends UbuntuDiskChecker {

    private static final Logger log = LoggerFactory.getLogger(Ubuntu24DiskChecker.class);

    @Override
    public void provideCleanupSuggestions(CheckLogger cacheLog) {
        cacheLog.warn("Ubuntu 24.04系统磁盘空间不足，建议以下清理措施:");
        cacheLog.warn("1. 清理APT缓存: sudo apt clean && sudo apt autoclean");
        cacheLog.warn("2. 删除不需要的软件包: sudo apt autoremove --purge");
        cacheLog.warn("3. 删除旧日志文件: sudo find /var/log -type f -name \"*.gz\" -delete");
        cacheLog.warn("4. 清理临时文件: sudo rm -rf /tmp/* /var/tmp/*");
        cacheLog.warn("5. 清理旧内核: sudo purge-old-kernels --keep 2");
        cacheLog.warn("6. 清理快照: sudo snap set system refresh.retain=2");
        cacheLog.warn("7. 清理journal日志: sudo journalctl --vacuum-time=7d");
        cacheLog.warn("8. 使用磁盘分析工具: sudo apt install ncdu && sudo ncdu /");
        cacheLog.warn("9. 考虑扩展" + DiskChecker.TARGET_DIR + "分区或挂载新磁盘");
    }
}