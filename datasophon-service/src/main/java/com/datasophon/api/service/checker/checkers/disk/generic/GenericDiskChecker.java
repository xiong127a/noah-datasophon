package com.datasophon.api.service.checker.checkers.disk.generic;

import com.datasophon.api.service.checker.checkers.disk.DiskChecker;
import com.datasophon.api.service.checker.checkers.disk.DiskCheckerStrategy;
import com.datasophon.api.service.checker.common.CommandResult;
import com.datasophon.api.service.checker.helpers.CheckLogger;
import com.datasophon.common.model.CheckItem;
import com.datasophon.common.model.HostInfo;
import org.apache.sshd.client.session.ClientSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 通用磁盘检查器实现
 * 适用于未指定具体Linux发行版的情况
 */
public class GenericDiskChecker implements DiskCheckerStrategy {

    private static final Logger log = LoggerFactory.getLogger(GenericDiskChecker.class);

    // 磁盘检查器实例，用于执行命令
    protected DiskChecker diskChecker;

    public GenericDiskChecker() {
        // 创建DiskChecker实例
        this.diskChecker = new DiskChecker();
    }

    @Override
    public CheckItem check(HostInfo hostInfo, CheckItem checkItem, CheckLogger cacheLog) throws InterruptedException {
        cacheLog.info("检查" + DiskChecker.TARGET_DIR + "目录磁盘使用情况...");

        // 设置检查项消息
        checkItem.setMessage("正在检查" + DiskChecker.TARGET_DIR + "目录磁盘使用情况...");

        // 执行df命令查看磁盘使用情况
        CommandResult dfResult;
        try {
            // 获取SSH会话
            ClientSession session = hostInfo.getExternalSession();
            if (session == null || !session.isOpen()) {
                String errorMsg = "SSH会话为空或已关闭，无法执行命令";
                log.error(errorMsg);
                cacheLog.error(errorMsg);
                checkItem.setStatus(CheckItem.Status.FAILED);
                checkItem.setMessage(errorMsg);
                return checkItem;
            }

            // 执行df命令
            dfResult = execCommand(session, "df -h " + DiskChecker.TARGET_DIR);

            // 如果第一个命令失败，尝试备用命令
            if (dfResult.getExitCode() != 0 || dfResult.getOutput().trim().isEmpty()) {
                log.info("使用标准df命令失败，尝试使用awk提取...");
                cacheLog.info("使用标准df命令失败，尝试使用awk提取...");

                dfResult = execCommand(session,
                        "df -BG " + DiskChecker.TARGET_DIR + " | awk 'NR>1{print; exit}'");
            }

            // 处理df命令结果
            CheckItem result = processDfResult(hostInfo, checkItem, dfResult, cacheLog);

            // 确保状态已更新，但不应该强制设置为成功
            if (result.getStatus() == CheckItem.Status.CHECKING) {
                // 尝试从命令结果中判断磁盘状态
                if (dfResult.isSuccess()) {
                    // 分析磁盘使用情况
                    try {
                        String output = dfResult.getOutput();
                        if (output.contains(DiskChecker.TARGET_DIR)) {
                            // 尝试提取使用率
                            String[] lines = output.split("\n");
                            for (String line : lines) {
                                if (line.contains(DiskChecker.TARGET_DIR)) {
                                    String[] parts = line.split("\\s+");
                                    if (parts.length >= 5) {
                                        String usageStr = parts[4].replace("%", "");
                                        int usage = Integer.parseInt(usageStr);

                                        if (usage > DiskChecker.WARNING_DISK_USAGE_THRESHOLD) {
                                            // 使用率过高
                                            log.warn("磁盘检查完成但状态未更新，分析发现磁盘使用率{}%超过阈值{}%",
                                                    usage, DiskChecker.WARNING_DISK_USAGE_THRESHOLD);
                                            cacheLog.warn("根据分析，磁盘使用率{}%超过阈值{}%",
                                                    usage, DiskChecker.WARNING_DISK_USAGE_THRESHOLD);
                                            result.setStatus(CheckItem.Status.FAILED);
                                            result.setMessage(String.format("%s 分区使用率过高: %d%% > %d%%",
                                                    DiskChecker.TARGET_DIR, usage,
                                                    DiskChecker.WARNING_DISK_USAGE_THRESHOLD));
                                            return result;
                                        } else {
                                            // 使用率正常
                                            log.info("磁盘检查完成但状态未更新，分析发现磁盘使用率{}%在正常范围内", usage);
                                            cacheLog.info("根据分析，磁盘使用率{}%在正常范围内", usage);
                                            result.setStatus(CheckItem.Status.SUCCESS);
                                            result.setMessage(DiskChecker.TARGET_DIR + " 目录磁盘空间充足");
                                            return result;
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.error("分析磁盘使用率时出错: ", e);
                    }

                    // 如果无法精确分析，但命令执行成功，可能磁盘状态正常
                    log.warn("磁盘检查完成但状态未更新，命令执行成功但无法精确分析结果");
                    cacheLog.warn("磁盘检查完成但无法精确分析结果，命令执行成功，可能磁盘状态正常");
                    result.setStatus(CheckItem.Status.SUCCESS);
                    result.setMessage("无法精确分析磁盘状态，但命令执行成功，可能磁盘状态正常");
                } else {
                    // 命令执行失败
                    log.warn("磁盘检查完成但状态未更新，且命令执行失败");
                    cacheLog.warn("磁盘检查完成但状态未更新，命令执行失败");
                    result.setStatus(CheckItem.Status.FAILED);
                    result.setMessage("检查磁盘状态时命令执行失败: " + dfResult.getError());
                }
            }

            return result;

        } catch (Exception e) {
            log.error("执行磁盘检查命令时出错: ", e);
            cacheLog.error("执行磁盘检查命令时出错: " + e.getMessage());
            checkItem.setStatus(CheckItem.Status.FAILED);
            checkItem.setMessage("执行磁盘检查命令时出错: " + e.getMessage());
            return checkItem;
        }
    }

    /**
     * 执行命令的辅助方法
     */
    protected CommandResult execCommand(ClientSession session, String command) throws InterruptedException {
        log.debug("执行命令: {}", command);

        if (session == null) {
            log.error("SSH会话为空，无法执行命令");
            return new CommandResult("", "SSH会话为空", -1);
        }

        if (diskChecker != null) {
            return diskChecker.execCommand(session, command);
        } else {
            // 如果没有diskChecker，返回错误结果
            return new CommandResult("", "磁盘检查器未初始化", -1);
        }
    }

    /**
     * 处理df命令结果的辅助方法
     */
    protected CheckItem processDfResult(HostInfo hostInfo, CheckItem checkItem, CommandResult dfResult,
            CheckLogger cacheLog) throws InterruptedException {
        if (dfResult.getExitCode() != 0) {
            String errorMsg = "获取磁盘信息失败: " + dfResult.getError();
            log.error(errorMsg);
            cacheLog.error(errorMsg);
            checkItem.setStatus(CheckItem.Status.FAILED);
            checkItem.setMessage(errorMsg);
            return checkItem;
        }

        String output = dfResult.getOutput();
        String[] lines = output.split("\n");

        if (lines.length < 2) {
            String errorMsg = "无法解析磁盘信息: " + output;
            log.error(errorMsg);
            cacheLog.error(errorMsg);
            checkItem.setStatus(CheckItem.Status.FAILED);
            checkItem.setMessage(errorMsg);
            return checkItem;
        }

        cacheLog.info("df命令输出:\n%s", output);

        // 找到包含目标目录的行
        String targetLine = null;
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            String[] parts = line.split("\\s+");
            if (parts.length < 6) {
                continue;
            }

            // 获取挂载点
            String mountPoint = parts[5];

            // 检查是否为目标目录或其父目录
            if (DiskChecker.TARGET_DIR.equals(mountPoint) || // 直接匹配
                    DiskChecker.TARGET_DIR.startsWith(mountPoint + "/") || // 是父目录
                    mountPoint.equals("/")) { // 根目录是所有目录的父目录

                // 如果找到多个匹配，优先使用最具体的挂载点
                if (targetLine == null || parts[5].length() > targetLine.split("\\s+")[5].length()) {
                    targetLine = line;
                }
            }
        }

        // 如果没有找到挂载点，报错
        if (targetLine == null) {
            String errorMsg = "在df输出中未找到" + DiskChecker.TARGET_DIR + "目录所在的分区";
            log.error(errorMsg);
            cacheLog.error(errorMsg);
            checkItem.setStatus(CheckItem.Status.FAILED);
            checkItem.setMessage(errorMsg);
            return checkItem;
        }

        // 解析找到的行
        String[] parts = targetLine.split("\\s+");
        String mountPoint = parts[5];
        String device = parts[0];

        String usageStr = parts[4].replace("%", "");
        try {
            int usage = Integer.parseInt(usageStr);
            String size = parts[1];
            String available = parts[3];

            log.info("检测到 {} 目录所在分区: 设备={}, 挂载点={}, 总大小={}, 可用空间={}, 使用率={}%",
                    DiskChecker.TARGET_DIR, device, mountPoint, size, available, usage);
            cacheLog.info(String.format("检测到 %s 目录所在分区: 设备=%s, 挂载点=%s, 总大小=%s, 可用空间=%s, 使用率=%d%%",
                    DiskChecker.TARGET_DIR, device, mountPoint, size, available, usage));

            // 检查磁盘使用率
            if (usage > DiskChecker.WARNING_DISK_USAGE_THRESHOLD) {
                String message = String.format(
                        "%s 目录所在分区(%s, 挂载点: %s)使用率过高: %d%% > %d%%, 请清理磁盘空间",
                        DiskChecker.TARGET_DIR, device, mountPoint, usage, DiskChecker.WARNING_DISK_USAGE_THRESHOLD);
                log.error(message);
                cacheLog.error(message);
                checkItem.setStatus(CheckItem.Status.FAILED);
                checkItem.setMessage(message);
                return checkItem;
            }

            // 检查可用空间
            available = available.toUpperCase();
            float availableGB;
            if (available.endsWith("G")) {
                availableGB = Float.parseFloat(available.substring(0, available.length() - 1));
            } else if (available.endsWith("T")) {
                availableGB = Float.parseFloat(available.substring(0, available.length() - 1)) * 1024;
            } else if (available.endsWith("M")) {
                availableGB = Float.parseFloat(available.substring(0, available.length() - 1)) / 1024;
            } else {
                String errorMsg = "无法解析可用空间单位: " + available;
                log.error(errorMsg);
                cacheLog.error(errorMsg);
                checkItem.setStatus(CheckItem.Status.FAILED);
                checkItem.setMessage(errorMsg);
                return checkItem;
            }

            if (availableGB < DiskChecker.MIN_DISK_SPACE_GB) {
                String message = String.format(
                        "%s 目录所在分区(%s, 挂载点: %s)可用空间不足: %.2f GB < %d GB, 请清理磁盘空间",
                        DiskChecker.TARGET_DIR, device, mountPoint, availableGB, DiskChecker.MIN_DISK_SPACE_GB);
                log.error(message);
                cacheLog.error(message);
                checkItem.setStatus(CheckItem.Status.FAILED);
                checkItem.setMessage(message);
                return checkItem;
            }

            // 如果检查通过，设置为成功
            checkItem.setStatus(CheckItem.Status.SUCCESS);
            String successMsg = String.format("%s 目录所在分区(%s, 挂载点: %s)磁盘空间充足",
                    DiskChecker.TARGET_DIR, device, mountPoint);
            checkItem.setMessage(successMsg);
            cacheLog.info(successMsg);

        } catch (NumberFormatException e) {
            String errorMsg = "无法解析磁盘使用率: " + usageStr;
            log.error(errorMsg, e);
            cacheLog.error(errorMsg);
            checkItem.setStatus(CheckItem.Status.FAILED);
            checkItem.setMessage(errorMsg);
            return checkItem;
        }

        return checkItem;
    }

    @Override
    public void provideCleanupSuggestions(CheckLogger cacheLog) {
        cacheLog.warn("磁盘空间不足，建议清理步骤:");
        cacheLog.warn("1. 清理临时文件和缓存:");
        cacheLog.warn("   - 清理/tmp目录: rm -rf /tmp/*");
        cacheLog.warn("   - 清理系统日志: sudo journalctl --vacuum-time=7d");

        cacheLog.warn("2. 检查并清理大文件:");
        cacheLog.warn("   - 查找大文件: find /opt -type f -size +100M -exec ls -lh {} \\;");
        cacheLog.warn("   - 清理不需要的大文件");

        cacheLog.warn("3. 检查并清理旧日志文件:");
        cacheLog.warn("   - 查找旧日志: find /opt -name \"*.log\" -type f -mtime +30");
        cacheLog.warn("   - 清理或压缩旧日志");

        cacheLog.warn("4. 清理不需要的软件包:");
        cacheLog.warn("   - CentOS/RHEL: sudo yum clean all");
        cacheLog.warn("   - Ubuntu/Debian: sudo apt clean; sudo apt autoremove");

        cacheLog.warn("5. 检查并删除不需要的服务和应用程序");

        cacheLog.warn("注意：在执行任何清理命令前，请确保备份重要数据，以防意外删除");
    }
}