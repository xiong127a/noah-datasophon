package com.datasophon.api.service.checker.checkers.diskspace;

import com.datasophon.api.service.checker.common.CommandResult;
import com.datasophon.api.service.checker.common.OsInfo;
import com.datasophon.common.model.CheckItem;
import com.datasophon.common.model.HostInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CentOS磁盘空间检查器
 * 针对CentOS系统的磁盘空间检查和清理
 */
public class CentOSDiskSpaceChecker extends DiskSpaceChecker {

    private static final Logger logger = LoggerFactory.getLogger(CentOSDiskSpaceChecker.class);

    /**
     * CentOS特有的检查磁盘空间
     */
    public CheckItem checkCentOSDiskSpace(HostInfo hostInfo, CheckItem checkItem, OsInfo osInfo)
            throws InterruptedException {
        // 记录CentOS版本信息
        String version = osInfo.getVersionId();
        cacheLog.info("正在对CentOS %s版本执行磁盘空间检查", version);

        // 检查/var/cache/yum目录
        checkYumCacheSize(hostInfo, checkItem);

        // 检查/var/lib/rpm目录大小
        checkRpmDbSize(hostInfo, checkItem);

        // 检查是否有系统备份占用空间
        checkSystemBackups(hostInfo, checkItem);

        // 基础检查由父类完成
        return super.doCheck(hostInfo, checkItem);
    }

    /**
     * 检查YUM缓存大小
     */
    private void checkYumCacheSize(HostInfo hostInfo, CheckItem checkItem) throws InterruptedException {
        cacheLog.info("检查YUM缓存大小...");

        CommandResult result = execCommand(session, "du -sh /var/cache/yum 2>/dev/null || echo '0'");
        if (result.isSuccess() && !result.getOutput().trim().equals("0")) {
            String output = result.getOutput().trim();
            cacheLog.info("YUM缓存大小: %s", output);

            // 提取大小值
            String sizeStr = output.split("\\s+")[0];
            if (sizeStr.endsWith("G") || sizeStr.endsWith("GB")) {
                try {
                    double sizeGB = Double.parseDouble(sizeStr.replaceAll("[^0-9.]", ""));
                    if (sizeGB > 1.0) {
                        cacheLog.warn("YUM缓存占用较大空间 (%.2f GB)，建议清理", sizeGB);
                    }
                } catch (NumberFormatException e) {
                    // 忽略解析错误
                }
            }
        }
    }

    /**
     * 检查RPM数据库大小
     */
    private void checkRpmDbSize(HostInfo hostInfo, CheckItem checkItem) throws InterruptedException {
        cacheLog.info("检查RPM数据库大小...");

        CommandResult result = execCommand(session, "du -sh /var/lib/rpm 2>/dev/null || echo '0'");
        if (result.isSuccess() && !result.getOutput().trim().equals("0")) {
            String output = result.getOutput().trim();
            cacheLog.info("RPM数据库大小: %s", output);

            // 提取大小值
            String sizeStr = output.split("\\s+")[0];
            if (sizeStr.endsWith("G") || sizeStr.endsWith("GB")) {
                try {
                    double sizeGB = Double.parseDouble(sizeStr.replaceAll("[^0-9.]", ""));
                    if (sizeGB > 0.5) {
                        cacheLog.warn("RPM数据库占用较大空间 (%.2f GB)，建议重建数据库", sizeGB);
                    }
                } catch (NumberFormatException e) {
                    // 忽略解析错误
                }
            }
        }
    }

    /**
     * 检查系统备份
     */
    private void checkSystemBackups(HostInfo hostInfo, CheckItem checkItem) throws InterruptedException {
        cacheLog.info("检查系统备份文件...");

        // 查找旧内核文件
        CommandResult kernelResult = execCommand(session, "ls -la /boot/*{vmlinuz,initramfs}* 2>/dev/null | wc -l");
        if (kernelResult.isSuccess()) {
            try {
                int kernelCount = Integer.parseInt(kernelResult.getOutput().trim());
                if (kernelCount > 6) { // 通常保留2-3个内核版本即可
                    cacheLog.warn("发现过多内核文件 (%d)，建议清理旧内核", kernelCount);
                }
            } catch (NumberFormatException e) {
                // 忽略解析错误
            }
        }

        // 检查用户备份
        CommandResult backupResult = execCommand(session,
                "find /home -name \"*.bak\" -o -name \"*backup*\" -o -name \"*.old\" -type f -size +100M 2>/dev/null | wc -l");
        if (backupResult.isSuccess()) {
            try {
                int backupCount = Integer.parseInt(backupResult.getOutput().trim());
                if (backupCount > 0) {
                    cacheLog.warn("发现大型备份文件 (%d 个)，建议检查并清理", backupCount);
                }
            } catch (NumberFormatException e) {
                // 忽略解析错误
            }
        }
    }

    /**
     * CentOS特有的磁盘空间清理
     */
    public boolean cleanCentOSDiskSpace(HostInfo hostInfo, CheckItem checkItem, OsInfo osInfo)
            throws InterruptedException {
        cacheLog.info("执行CentOS特定的磁盘空间清理...");

        boolean success = true;

        // 清理YUM缓存
        cacheLog.info("清理YUM缓存...");
        CommandResult yumCleanResult = execCommand(session, "yum clean all 2>/dev/null || true");

        // 清理旧内核（保留最新的两个）
        if (osInfo.isVersion("7") || osInfo.isVersion("8")) {
            cacheLog.info("清理旧内核（保留最新的两个）...");
            CommandResult kernelCleanResult = execCommand(session,
                    "yum install -y yum-utils 2>/dev/null && package-cleanup --oldkernels --count=2 -y 2>/dev/null || true");
        } else {
            // CentOS 6及更早版本
            cacheLog.info("在CentOS 6上清理旧内核（通过rpm命令）...");
            CommandResult oldKernelList = execCommand(session,
                    "rpm -q kernel | sort | head -n -2 | xargs rpm -e --nodeps 2>/dev/null || true");
        }

        // 清理日志文件
        cacheLog.info("清理过大的日志文件...");
        execCommand(session,
                "find /var/log -type f -name \"*.log\" -size +100M -exec truncate -s 10M {} \\; 2>/dev/null || true");

        // 清理软件包文件
        cacheLog.info("清理下载的软件包...");
        execCommand(session, "rm -rf /var/cache/yum/*/packages/* 2>/dev/null || true");

        // 清理临时目录
        cacheLog.info("清理临时文件...");
        execCommand(session, "find /tmp -type f -atime +10 -delete 2>/dev/null || true");
        execCommand(session, "find /var/tmp -type f -atime +10 -delete 2>/dev/null || true");

        return success;
    }

    /**
     * 提供CentOS特定的磁盘清理建议
     */
    public void provideCentOSCleanupSuggestions() {
        cacheLog.info("CentOS系统磁盘空间不足，建议以下清理措施:");
        cacheLog.info("1. 清理YUM缓存: yum clean all");
        cacheLog.info("2. 移除旧内核: package-cleanup --oldkernels --count=2 -y");
        cacheLog.info("3. 重建RPM数据库: rpm --rebuilddb");
        cacheLog.info("4. 检查并清理/var/log目录下的大文件");
        cacheLog.info("5. 使用logrotate处理日志文件: logrotate -f /etc/logrotate.conf");
        cacheLog.info("6. 清理core文件: find / -name core -type f -delete");
    }
}