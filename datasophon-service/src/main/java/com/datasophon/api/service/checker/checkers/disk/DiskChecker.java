package com.datasophon.api.service.checker.checkers.disk;

import com.datasophon.api.service.checker.checkers.disk.factory.DiskCheckerFactory;
import com.datasophon.api.service.checker.common.CommandResult;
import com.datasophon.api.service.checker.common.ItemCode;
import com.datasophon.api.service.checker.common.LinuxDistribution;
import com.datasophon.api.service.checker.common.LogEntryManager;
import com.datasophon.api.service.checker.common.OsInfo;
import com.datasophon.api.service.checker.core.AbstractItemChecker;
import com.datasophon.common.model.CheckItem;
import com.datasophon.common.model.HostInfo;
import org.apache.sshd.client.channel.ClientChannel;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.session.ClientSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.util.EnumSet;
import java.util.concurrent.TimeUnit;

/**
 * 磁盘检查器
 * 检查/opt目录的磁盘空间是否足够
 * 支持多种Linux发行版，包括CentOS、Ubuntu和Kylin
 */
@Component
public class DiskChecker extends AbstractItemChecker {

    private static final Logger log = LoggerFactory.getLogger(DiskChecker.class);

    /** 要检查的目标目录 */
    public static final String TARGET_DIR = "/opt";

    /** 警告磁盘使用率阈值 */
    public static final int WARNING_DISK_USAGE_THRESHOLD = 80;

    /** 警告inode使用率阈值 */
    public static final int WARNING_INODE_USAGE_THRESHOLD = 80;

    /** 最小所需磁盘空间（GB） */
    public static final long MIN_DISK_SPACE_GB = 100;

    @Override
    protected CheckItem doCheck(HostInfo hostInfo, CheckItem checkItem) throws InterruptedException {
        log.info("==== 开始检查磁盘空间 ====");
        cacheLog.info("==== 开始检查磁盘空间 ====");

        try {
            // 设置当前处理的主机信息
            setCurrentHostInfo(hostInfo);

            // 使用HostInfo中的SSH会话
            if (session == null) {
                // 检查hostInfo中是否有可用的会话
                if (!hostInfo.isSessionReady()) {
                    String errorMsg = "SSH会话未就绪，无法执行磁盘检查: " + hostInfo.getHostname();
                    log.error(errorMsg);
                    cacheLog.error(errorMsg);
                    checkItem.setStatus(CheckItem.Status.FAILED);
                    checkItem.setMessage(errorMsg);
                    return checkItem;
                }
            }
            hostInfo.setExternalSession(session);
            // 获取操作系统信息
            OsInfo osInfo = getOsInfo(hostInfo);
            if (osInfo == null || osInfo.getDistribution() == LinuxDistribution.OTHER) {
                String errorMsg = "无法获取操作系统信息";
                log.error(errorMsg);
                cacheLog.error(errorMsg);
                checkItem.setStatus(CheckItem.Status.FAILED);
                checkItem.setMessage(errorMsg);
                return checkItem;
            }

            cacheLog.info("检测到操作系统: %s, 版本: %s", osInfo.getDistribution(), osInfo.getVersionId());

            // 通过工厂获取适合当前操作系统的磁盘检查策略
            DiskCheckerStrategy strategy = DiskCheckerFactory.getChecker(osInfo);

            // 使用策略执行检查
            CheckItem result = strategy.check(hostInfo, checkItem, cacheLog);

            // 确保状态已正确更新
            if (result.getStatus() == CheckItem.Status.CHECKING) {
                log.warn("磁盘检查完成但状态仍未更新，需要进一步分析");
                cacheLog.warn("磁盘检查完成但状态未更新，进行额外检查");

                // 额外检查磁盘状态
                try {
                    // 执行简单的df命令检查磁盘状态
                    CommandResult dfResult = execCommand(session, "df -h " + TARGET_DIR);
                    if (dfResult.isSuccess()) {
                        String output = dfResult.getOutput();
                        log.info("额外检查df输出:\n{}", output);
                        cacheLog.info("额外检查df输出结果:\n%s", output);

                        // 按行分割输出
                        String[] lines = output.split("\n");
                        if (lines.length < 2) {
                            log.error("df命令输出格式异常，行数不足");
                            cacheLog.error("df命令输出格式异常，行数不足");
                            result.setStatus(CheckItem.Status.FAILED);
                            result.setMessage("磁盘检查异常：df命令输出格式不正确");
                            return result;
                        }

                        // 查找目标目录所在的分区
                        String targetLine = null;
                        for (int i = 1; i < lines.length; i++) {
                            String line = lines[i].trim();
                            String[] parts = line.split("\\s+");
                            if (parts.length < 6)
                                continue;

                            // 获取挂载点
                            String mountPoint = parts[5];

                            // 检查是否为目标目录或其父目录
                            if (TARGET_DIR.equals(mountPoint) || // 直接匹配
                                    TARGET_DIR.startsWith(mountPoint + "/") || // 是父目录
                                    mountPoint.equals("/")) { // 根目录是所有目录的父目录

                                // 如果找到多个匹配，优先使用最具体的挂载点
                                if (targetLine == null || parts[5].length() > targetLine.split("\\s+")[5].length()) {
                                    targetLine = line;
                                }
                            }
                        }

                        // 如果找到了目标行，分析磁盘使用情况
                        if (targetLine != null) {
                            String[] parts = targetLine.split("\\s+");
                            String device = parts[0];
                            String mountPoint = parts[5];

                            if (parts.length >= 5) {
                                String usageStr = parts[4].replace("%", "");
                                try {
                                    int usage = Integer.parseInt(usageStr);
                                    String size = parts[1];
                                    String available = parts[3];

                                    log.info("额外检查发现{}目录所在分区: 设备={}, 挂载点={}, 总大小={}, 可用={}, 使用率={}%",
                                            TARGET_DIR, device, mountPoint, size, available, usage);
                                    cacheLog.info("额外检查发现%s目录所在分区: 设备=%s, 挂载点=%s, 总大小=%s, 可用=%s, 使用率=%d%%",
                                            TARGET_DIR, device, mountPoint, size, available, usage);

                                    if (usage > WARNING_DISK_USAGE_THRESHOLD) {
                                        log.warn("磁盘使用率{}%超过警告阈值{}%", usage, WARNING_DISK_USAGE_THRESHOLD);
                                        cacheLog.warn("额外检查发现磁盘使用率{}%超过阈值{}%", usage, WARNING_DISK_USAGE_THRESHOLD);
                                        result.setStatus(CheckItem.Status.FAILED);
                                        result.setMessage(String.format("%s 目录所在分区(%s, 挂载点: %s)使用率过高: %d%% > %d%%",
                                                TARGET_DIR, device, mountPoint, usage, WARNING_DISK_USAGE_THRESHOLD));
                                    } else {
                                        log.info("磁盘使用率{}%在正常范围内", usage);
                                        cacheLog.info("额外检查发现磁盘使用率{}%在正常范围内", usage);
                                        result.setStatus(CheckItem.Status.SUCCESS);
                                        result.setMessage(String.format("%s 目录所在分区(%s, 挂载点: %s)磁盘空间充足",
                                                TARGET_DIR, device, mountPoint));
                                    }
                                    return result;
                                } catch (NumberFormatException e) {
                                    log.error("解析磁盘使用率失败: {}", usageStr, e);
                                }
                            }
                        } else {
                            log.error("在df输出中未找到{}目录所在的分区", TARGET_DIR);
                            cacheLog.error("在df输出中未找到{}目录所在的分区", TARGET_DIR);
                        }

                        // 如果无法精确分析使用率，但命令执行成功
                        log.warn("无法精确分析磁盘使用率，但命令执行成功");
                        cacheLog.warn("无法精确分析磁盘使用率，但命令执行成功，假定磁盘状态正常");
                        result.setStatus(CheckItem.Status.SUCCESS);
                        result.setMessage("无法精确分析磁盘状态，但命令执行成功，可能磁盘状态正常");
                    } else {
                        // 额外检查命令执行失败
                        log.error("额外检查磁盘状态失败: {}", dfResult.getError());
                        cacheLog.error("额外检查磁盘状态失败: {}", dfResult.getError());
                        result.setStatus(CheckItem.Status.FAILED);
                        result.setMessage("磁盘检查异常：无法获取磁盘状态信息");
                    }
                } catch (Exception e) {
                    log.error("额外检查磁盘状态时出错: ", e);
                    cacheLog.error("额外检查磁盘状态时出错: {}", e.getMessage());
                    result.setStatus(CheckItem.Status.FAILED);
                    result.setMessage("磁盘检查执行过程中发生异常: " + e.getMessage());
                }
            }

            return result;
        } catch (Exception e) {
            log.error("磁盘检查出错: ", e);
            cacheLog.error("磁盘检查出错: " + e.getMessage());
            checkItem.setStatus(CheckItem.Status.FAILED);
            checkItem.setMessage("磁盘检查出错: " + e.getMessage());
            return checkItem;
        }
    }

    @Override
    protected boolean doFix(HostInfo hostInfo, CheckItem checkItem) {
        // 磁盘空间问题需要手动修复，这里只提供修复建议
        log.info("==== 开始生成磁盘空间清理建议 ====");
        cacheLog.info("==== 开始生成磁盘空间清理建议 ====");

        try {
            // 设置当前处理的主机信息
            setCurrentHostInfo(hostInfo);

            // 使用HostInfo中的SSH会话
            if (!hostInfo.isSessionReady()) {
                String errorMsg = "SSH会话未就绪，无法生成磁盘清理建议: " + hostInfo.getHostname();
                log.error(errorMsg);
                cacheLog.error(errorMsg);
                checkItem.setMessage(errorMsg);
                return false;
            }

            // 使用hostInfo的会话
            session = hostInfo.getExternalSession();

            // 获取操作系统信息
            OsInfo osInfo = getOsInfo(hostInfo);
            if (osInfo == null || osInfo.getDistribution() == LinuxDistribution.OTHER) {
                String errorMsg = "无法获取操作系统信息";
                log.error(errorMsg);
                cacheLog.error(errorMsg);
                checkItem.setMessage(errorMsg);
                return false;
            }

            cacheLog.info("检测到操作系统: %s, 版本: %s", osInfo.getDistribution(), osInfo.getVersionId());

            // 通过工厂获取适合当前操作系统的磁盘检查策略
            DiskCheckerStrategy strategy = DiskCheckerFactory.getChecker(osInfo);

            // 提供清理建议
            strategy.provideCleanupSuggestions(cacheLog);

            // 设置检查项消息
            // 从日志管理器获取日志内容
            String logContent = LogEntryManager.getLogContent(currentLogKey);
            checkItem.setMessage(logContent);

            log.info("==== 磁盘空间清理建议生成完成 ====");
            cacheLog.info("==== 磁盘空间清理建议生成完成 ====");

            // 磁盘问题需要手动修复
            return false;
        } catch (Exception e) {
            log.error("生成磁盘清理建议时出错: ", e);
            cacheLog.error("生成磁盘清理建议时出错: " + e.getMessage());
            checkItem.setMessage("生成磁盘清理建议时出错: " + e.getMessage());
            return false;
        }
    }

    /**
     * 执行命令
     * 公开此方法供DiskCheckerStrategy实现类调用
     */
    public CommandResult execCommand(ClientSession session, String command) throws InterruptedException {
        // 检查参数
        if (session == null) {
            log.error("SSH会话为空，无法执行命令");
            cacheLog.error("SSH会话为空，无法执行命令");
            return new CommandResult("", "SSH会话为空", -1);
        }

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ByteArrayOutputStream errorStream = new ByteArrayOutputStream();

            ClientChannel channel = session.createExecChannel(command);
            channel.setOut(outputStream);
            channel.setErr(errorStream);

            // 打开通道
            channel.open().verify(30, TimeUnit.SECONDS);

            // 等待命令完成
            channel.waitFor(EnumSet.of(ClientChannelEvent.CLOSED), 30000);

            // 获取退出状态
            Integer exitStatus = channel.getExitStatus();
            String output = outputStream.toString();
            String error = errorStream.toString();

            // 关闭通道
            channel.close();

            return new CommandResult(output, error, exitStatus != null ? exitStatus : -1);
        } catch (Exception e) {
            return new CommandResult("", e.getMessage(), -1);
        }
    }

    @Override
    public ItemCode getCheckerType() {
        return ItemCode.DISK;
    }
}