package com.datasophon.api.service.checker.checkers.disk.os.kylin;

import com.datasophon.api.service.checker.checkers.disk.generic.GenericDiskChecker;
import com.datasophon.api.service.checker.helpers.CheckLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Kylin磁盘检查器实现
 * 适用于银河麒麟操作系统
 */
public class KylinDiskChecker extends GenericDiskChecker {

    private static final Logger log = LoggerFactory.getLogger(KylinDiskChecker.class);

    @Override
    public void provideCleanupSuggestions(CheckLogger cacheLog) {
        cacheLog.warn("银河麒麟系统磁盘空间不足，建议以下清理措施:");
        cacheLog.warn("1. 清理APT缓存: sudo apt-get clean");
        cacheLog.warn("2. 删除旧日志文件: sudo find /var/log -type f -name \"*.gz\" -delete");
        cacheLog.warn("3. 清理临时文件: sudo rm -rf /tmp/* /var/tmp/*");
        cacheLog.warn("4. 清理journal日志: sudo journalctl --vacuum-time=7d");
        cacheLog.warn("5. 清理Docker镜像和容器(如果安装了Docker): sudo docker system prune -a");
        cacheLog.warn("6. 考虑扩展磁盘分区或挂载新磁盘");
    }
}