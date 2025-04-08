package com.datasophon.api.service.checker.checkers.disk.os.centos;

import com.datasophon.api.service.checker.checkers.disk.DiskChecker;
import com.datasophon.api.service.checker.helpers.CheckLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CentOS 8磁盘检查器实现
 * 专用于CentOS 8 Linux发行版
 */
public class CentOS8DiskChecker extends CentOSDiskChecker {

    private static final Logger log = LoggerFactory.getLogger(CentOS8DiskChecker.class);

    @Override
    public void provideCleanupSuggestions(CheckLogger cacheLog) {
        cacheLog.warn("CentOS 8系统磁盘空间不足，建议以下清理措施:");
        cacheLog.warn("1. 清理DNF缓存: sudo dnf clean all");
        cacheLog.warn("2. 删除旧日志文件: sudo find /var/log -type f -name \"*.gz\" -delete");
        cacheLog.warn("3. 清理临时文件: sudo rm -rf /tmp/* /var/tmp/*");
        cacheLog.warn("4. 移除旧内核: sudo dnf remove --oldinstallonly --setopt installonly_limit=2 kernel");
        cacheLog.warn("5. 清理journal日志: sudo journalctl --vacuum-time=7d");
        cacheLog.warn("6. 清理Docker镜像和容器(如果安装了Docker): sudo docker system prune -a");
        cacheLog.warn("7. 考虑扩展磁盘分区或挂载新磁盘");
    }
}