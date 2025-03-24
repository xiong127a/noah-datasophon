package com.datasophon.api.service.checker.checkers.diskspace;

import com.datasophon.api.service.checker.core.AbstractItemChecker;
import com.datasophon.api.service.checker.common.CommandResult;
import com.datasophon.api.service.checker.common.OsInfo;
import com.datasophon.common.model.CheckItem;
import com.datasophon.common.model.HostInfo;
import com.datasophon.common.model.ItemCode;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 磁盘空间检查器
 * 检查主机磁盘空间使用情况，对空间不足的分区发出警告
 */
@Component
public class DiskSpaceChecker extends AbstractItemChecker {

    private static final Logger logger = LoggerFactory.getLogger(DiskSpaceChecker.class);

    // 默认警告阈值：磁盘使用率超过此值将发出警告（单位：百分比）
    private static final int WARNING_THRESHOLD = 80;

    // 默认严重阈值：磁盘使用率超过此值将视为严重问题（单位：百分比）
    private static final int CRITICAL_THRESHOLD = 90;

    // 最小要求可用空间（单位：GB）
    private static final int MIN_FREE_SPACE_GB = 10;

    // 要忽略的文件系统类型列表
    private static final String[] IGNORED_FS_TYPES = {
            "tmpfs", "devtmpfs", "devfs", "iso9660", "overlay",
            "aufs", "squashfs", "udf", "fuse", "fuse.lxcfs",
            "ecryptfs"
    };

    // 要忽略的挂载点列表
    private static final String[] IGNORED_MOUNT_POINTS = {
            "/dev", "/sys", "/proc", "/run", "/boot", "/var/lib/docker",
            "/var/lib/containerd", "/var/lib/lxcfs", "/snap"
    };

    @Override
    protected CheckItem doCheck(HostInfo hostInfo, CheckItem checkItem) throws InterruptedException {
        try {
            cacheLog.info("==== 磁盘空间检查开始 ====");

            // 更新状态为正在检查磁盘空间
            setCheckItemMessage(hostInfo, checkItem, "正在检查磁盘空间使用情况...");

            // 获取操作系统信息
            OsInfo osInfo = getOsInfo(hostInfo);
            cacheLog.info("检测到操作系统: %s, 版本: %s", osInfo.getFullName(), osInfo.getVersionId());

            // 获取磁盘使用情况
            List<DiskPartition> partitions = getDiskUsage();

            // 分析磁盘使用情况
            List<DiskPartition> problematicPartitions = analyzePartitions(partitions);

            // 根据分析结果设置检查状态
            if (problematicPartitions.isEmpty()) {
                cacheLog.info("所有磁盘分区空间充足");
                checkItem.setStatus(CheckItem.Status.SUCCESS);
                setCheckItemMessage(hostInfo, checkItem, "所有磁盘分区空间充足");
            } else {
                boolean hasCritical = problematicPartitions.stream()
                        .anyMatch(p -> p.getUsedPercent() >= CRITICAL_THRESHOLD);

                // 对于所有空间不足的情况，都设置为FAILED状态
                // 但使用不同的消息区分严重程度
                checkItem.setStatus(CheckItem.Status.FAILED);
                if (hasCritical) {
                    setCheckItemMessage(hostInfo, checkItem, "发现磁盘空间严重不足，请尽快清理");
                } else {
                    setCheckItemMessage(hostInfo, checkItem, "发现磁盘空间不足，建议清理");
                }

                // 记录问题分区的详细信息并提供清理建议
                provideDiskCleanupSuggestions(problematicPartitions);
            }

            // 记录所有分区的使用情况
            cacheLog.info("磁盘分区使用情况:");
            for (DiskPartition partition : partitions) {
                cacheLog.info("%s (%s): 总大小 %.2f GB, 已使用 %.2f GB (使用率 %d%%), 可用 %.2f GB",
                        partition.getMountPoint(),
                        partition.getFileSystem(),
                        partition.getTotalGB(),
                        partition.getUsedGB(),
                        partition.getUsedPercent(),
                        partition.getAvailableGB());
            }

            return checkItem;
        } catch (Exception e) {
            String errorMsg = "磁盘空间检查失败: " + e.getMessage();
            logger.error(errorMsg, e);
            cacheLog.error("错误: %s", errorMsg);
            checkItem.setStatus(CheckItem.Status.FAILED);
            setCheckItemMessage(hostInfo, checkItem, errorMsg);
            return checkItem;
        } finally {
            cacheLog.info("==== 磁盘空间检查完成 ====");
        }
    }

    /**
     * 获取磁盘使用情况
     */
    private List<DiskPartition> getDiskUsage() throws InterruptedException {
        List<DiskPartition> partitions = new ArrayList<>();

        // 使用df命令获取分区信息
        CommandResult result = execCommand(session, "df -BG -P | grep -v Filesystem");
        if (!result.isSuccess()) {
            cacheLog.error("获取磁盘使用情况失败: %s", result.getErrorOrOutput());
            return partitions;
        }

        // 解析df命令输出
        String output = result.getOutput();
        for (String line : output.split("\n")) {
            if (line.trim().isEmpty()) {
                continue;
            }

            String[] parts = line.trim().split("\\s+");
            if (parts.length < 6) {
                continue;
            }

            // 提取分区信息
            String filesystem = parts[0];
            String sizeStr = parts[1].replace("G", "");
            String usedStr = parts[2].replace("G", "");
            String availableStr = parts[3].replace("G", "");
            String usedPercentStr = parts[4].replace("%", "");
            String mountPoint = parts[5];

            // 跳过不需要检查的文件系统类型
            if (shouldIgnoreFilesystem(filesystem)) {
                continue;
            }

            // 跳过不需要检查的挂载点
            if (shouldIgnoreMountPoint(mountPoint)) {
                continue;
            }

            try {
                double totalGB = Double.parseDouble(sizeStr);
                double usedGB = Double.parseDouble(usedStr);
                double availableGB = Double.parseDouble(availableStr);
                int usedPercent = Integer.parseInt(usedPercentStr);

                DiskPartition partition = new DiskPartition();
                partition.setFileSystem(filesystem);
                partition.setMountPoint(mountPoint);
                partition.setTotalGB(totalGB);
                partition.setUsedGB(usedGB);
                partition.setAvailableGB(availableGB);
                partition.setUsedPercent(usedPercent);

                partitions.add(partition);
            } catch (NumberFormatException e) {
                cacheLog.warn("解析磁盘信息时出错: %s", line);
            }
        }

        return partitions;
    }

    /**
     * 判断是否应该忽略特定的文件系统类型
     */
    private boolean shouldIgnoreFilesystem(String filesystem) {
        for (String ignoredFs : IGNORED_FS_TYPES) {
            if (filesystem.contains(ignoredFs)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断是否应该忽略特定的挂载点
     */
    private boolean shouldIgnoreMountPoint(String mountPoint) {
        for (String ignoredMount : IGNORED_MOUNT_POINTS) {
            if (mountPoint.equals(ignoredMount) || mountPoint.startsWith(ignoredMount + "/")) {
                return true;
            }
        }
        return false;
    }

    /**
     * 分析分区使用情况，找出存在问题的分区
     */
    private List<DiskPartition> analyzePartitions(List<DiskPartition> partitions) {
        List<DiskPartition> problematicPartitions = new ArrayList<>();

        for (DiskPartition partition : partitions) {
            // 检查使用率是否超过警告阈值
            if (partition.getUsedPercent() >= WARNING_THRESHOLD) {
                problematicPartitions.add(partition);
                continue;
            }

            // 检查可用空间是否低于最小要求（对于大于MIN_FREE_SPACE_GB*2的分区）
            if (partition.getTotalGB() > MIN_FREE_SPACE_GB * 2 &&
                    partition.getAvailableGB() < MIN_FREE_SPACE_GB) {
                problematicPartitions.add(partition);
            }
        }

        return problematicPartitions;
    }

    /**
     * 查找大文件
     */
    private List<String> findLargeFiles(String directory, int topN, double minSizeGB) throws InterruptedException {
        List<String> largeFiles = new ArrayList<>();

        // 查找大于指定大小的文件，并按大小降序排列
        String command = String.format(
                "find %s -type f -size +%.0fG -exec ls -lh {} \\; 2>/dev/null | sort -rh -k5 | head -%d",
                directory, minSizeGB, topN);

        CommandResult result = execCommand(session, command);
        if (result.isSuccess() && !result.getOutput().trim().isEmpty()) {
            for (String line : result.getOutput().split("\n")) {
                if (!line.trim().isEmpty()) {
                    largeFiles.add(line);
                }
            }
        }

        return largeFiles;
    }

    /**
     * 查找占用空间较大的目录
     */
    private List<String> findLargeDirectories(String parentDir, int topN) throws InterruptedException {
        List<String> largeDirectories = new ArrayList<>();

        // 查找大目录并按大小降序排列
        String command = String.format(
                "du -h --max-depth=2 %s 2>/dev/null | sort -rh | head -%d",
                parentDir, topN);

        CommandResult result = execCommand(session, command);
        if (result.isSuccess() && !result.getOutput().trim().isEmpty()) {
            for (String line : result.getOutput().split("\n")) {
                if (!line.trim().isEmpty()) {
                    largeDirectories.add(line);
                }
            }
        }

        return largeDirectories;
    }

    @Override
    protected boolean doFix(HostInfo hostInfo, CheckItem checkItem) {
        // 磁盘空间检查器不执行实际的清理操作，只提供清理建议
        cacheLog.info("==== 开始生成磁盘空间清理建议 ====");

        try {
            // 获取磁盘使用情况
            List<DiskPartition> partitions = getDiskUsage();
            List<DiskPartition> problematicPartitions = analyzePartitions(partitions);

            if (problematicPartitions.isEmpty()) {
                cacheLog.info("所有磁盘分区空间充足，无需清理");
                setCheckItemMessage(hostInfo, checkItem, "所有磁盘分区空间充足，无需清理");
            } else {
                // 提供清理建议
                provideDiskCleanupSuggestions(problematicPartitions);

                setCheckItemMessage(hostInfo, checkItem, "已生成磁盘空间清理建议，请查看日志了解详细信息");
            }

            // 由于不进行实际清理，返回 false 表示此修复操作需要用户手动执行
            return false;

        } catch (Exception e) {
            String errorMsg = "生成磁盘空间清理建议失败: " + e.getMessage();
            logger.error(errorMsg, e);
            cacheLog.error("错误: " + errorMsg);
            setCheckItemMessage(hostInfo, checkItem, errorMsg);
            return false;
        } finally {
            cacheLog.info("==== 磁盘空间清理建议生成完成 ====");
        }
    }

    /**
     * 提供磁盘清理建议
     */
    private void provideDiskCleanupSuggestions(List<DiskPartition> problematicPartitions) {
        cacheLog.warn("发现以下分区空间不足，需要清理:");

        for (DiskPartition partition : problematicPartitions) {
            cacheLog.warn("%s 分区：使用率 %d%%，可用空间 %.2f GB",
                    partition.getMountPoint(),
                    partition.getUsedPercent(),
                    partition.getAvailableGB());

            try {
                // 查找大文件
                List<String> largeFiles = findLargeFiles(partition.getMountPoint(), 5, 1.0);
                if (!largeFiles.isEmpty()) {
                    cacheLog.warn("在 %s 分区发现大文件:", partition.getMountPoint());
                    for (String fileInfo : largeFiles) {
                        cacheLog.warn("  %s", fileInfo);
                    }
                }

                // 查找大目录
                List<String> largeDirectories = findLargeDirectories(partition.getMountPoint(), 5);
                if (!largeDirectories.isEmpty()) {
                    cacheLog.warn("在 %s 分区发现占用空间较大的目录:", partition.getMountPoint());
                    for (String dirInfo : largeDirectories) {
                        cacheLog.warn("  %s", dirInfo);
                    }
                }
            } catch (Exception e) {
                cacheLog.warn("无法获取 %s 分区的详细信息: %s", partition.getMountPoint(), e.getMessage());
            }
        }

        // 提供通用清理建议
        cacheLog.warn("建议清理措施:");
        cacheLog.warn("1. 删除不需要的大文件");
        cacheLog.warn("2. 清理日志文件: rm -f /var/log/*.gz");
        cacheLog.warn("3. 清理软件包缓存:");
        cacheLog.warn("   - CentOS: yum clean all");
        cacheLog.warn("   - Ubuntu: apt-get clean");
        cacheLog.warn("4. 清理临时文件: rm -rf /tmp/* /var/tmp/*");
        cacheLog.warn("5. 考虑增加磁盘容量或迁移数据到更大的磁盘");
    }

    @Override
    public ItemCode getCheckerType() {
        return ItemCode.DISK;
    }

    /**
     * 磁盘分区信息类
     */
    public static class DiskPartition {
        private String fileSystem;
        private String mountPoint;
        private double totalGB;
        private double usedGB;
        private double availableGB;
        private int usedPercent;

        public String getFileSystem() {
            return fileSystem;
        }

        public void setFileSystem(String fileSystem) {
            this.fileSystem = fileSystem;
        }

        public String getMountPoint() {
            return mountPoint;
        }

        public void setMountPoint(String mountPoint) {
            this.mountPoint = mountPoint;
        }

        public double getTotalGB() {
            return totalGB;
        }

        public void setTotalGB(double totalGB) {
            this.totalGB = totalGB;
        }

        public double getUsedGB() {
            return usedGB;
        }

        public void setUsedGB(double usedGB) {
            this.usedGB = usedGB;
        }

        public double getAvailableGB() {
            return availableGB;
        }

        public void setAvailableGB(double availableGB) {
            this.availableGB = availableGB;
        }

        public int getUsedPercent() {
            return usedPercent;
        }

        public void setUsedPercent(int usedPercent) {
            this.usedPercent = usedPercent;
        }
    }
}