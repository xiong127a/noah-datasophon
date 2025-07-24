package com.datasophon.api.service.checker.checkers.disk;

import com.datasophon.api.config.CheckerProperties;
import com.datasophon.api.service.checker.checkers.disk.factory.DiskCheckerFactory;
import com.datasophon.api.service.checker.common.CommandResult;
import com.datasophon.api.service.checker.common.ItemCode;
import com.datasophon.api.service.checker.core.AbstractItemChecker;
import com.datasophon.common.model.CheckItem;
import com.datasophon.common.model.HostInfo;
import com.datasophon.common.model.OsInfo;
import org.apache.sshd.client.channel.ClientChannel;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.session.ClientSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 磁盘检查器
 * 检查配置的目录列表中的磁盘空间是否足够
 * 支持多种Linux发行版，包括CentOS、Ubuntu和Kylin
 */
@Component
public class DiskChecker extends AbstractItemChecker {

    private static final Logger log = LoggerFactory.getLogger(DiskChecker.class);

    private final CheckerProperties checkerProperties;

    private final DiskCheckerFactory diskCheckerFactory;

    /** 警告磁盘使用率阈值 */
    public static final int WARNING_DISK_USAGE_THRESHOLD = 80;

    /**
     * 获取全局最小可用空间百分比
     * 
     * @return 全局最小可用空间百分比
     */
    public int getMinAvailablePercent() {
        return checkerProperties.getDisk().getMinAvailablePercent();
    }

    /**
     * 获取需要检查的目录列表
     * 
     * @return 需要检查的目录列表
     */
    public List<CheckerProperties.DiskDirectoryConfig> getCheckDirectories() {
        return checkerProperties.getDisk().getCheckDirectories();
    }

    @Override
    protected CheckItem doCheck(HostInfo hostInfo, CheckItem checkItem) throws InterruptedException {
        // 获取操作系统信息
        OsInfo osInfo = getOsInfo(hostInfo);
        if (osInfo == null) {
            log.error("无法获取操作系统信息，无法进行磁盘检查");
            checkItem.setStatus(CheckItem.Status.FAILED);
            checkItem.setMessage("无法获取操作系统信息，无法进行磁盘检查");
            return checkItem;
        }

        // 使用工厂获取对应的磁盘检查器
        DiskCheckerStrategy diskChecker = diskCheckerFactory.getChecker(osInfo);

        try {
            // 执行磁盘检查
            return diskChecker.check(hostInfo, checkItem, cacheLog);
        } catch (Exception e) {
            log.error("磁盘检查过程中发生错误", e);
            checkItem.setStatus(CheckItem.Status.FAILED);
            checkItem.setMessage("磁盘检查过程中发生错误: " + e.getMessage());
            return checkItem;
        }
    }

    @Override
    protected boolean doFix(HostInfo hostInfo, CheckItem checkItem) {
        // 获取操作系统信息
        OsInfo osInfo;
        try {
            osInfo = getOsInfo(hostInfo);
            if (osInfo == null) {
                log.error("无法获取操作系统信息，无法进行磁盘修复");
                checkItem.setStatus(CheckItem.Status.FAILED);
                checkItem.setMessage("无法获取操作系统信息，无法进行磁盘修复");
                return false;
            }
        } catch (Exception e) {
            log.error("获取操作系统信息时发生错误", e);
            checkItem.setStatus(CheckItem.Status.FAILED);
            checkItem.setMessage("获取操作系统信息时发生错误: " + e.getMessage());
            return false;
        }

        // 使用工厂获取对应的磁盘检查器
        DiskCheckerStrategy diskChecker = diskCheckerFactory.getChecker(osInfo);

        try {
            // 对于磁盘检查，我们只提供建议，不执行自动修复
            CheckItem checkResult = diskChecker.check(hostInfo, checkItem, cacheLog);

            // 如果检查失败，提供清理建议
            if (checkResult.getStatus() == CheckItem.Status.FAILED) {
                cacheLog.info("磁盘检查失败，提供清理建议...");
                diskChecker.provideCleanupSuggestions(cacheLog);
                return false;
            } else {
                cacheLog.info("磁盘检查通过，无需修复");
                return true;
            }
        } catch (Exception e) {
            log.error("磁盘检查过程中发生错误", e);
            checkItem.setStatus(CheckItem.Status.FAILED);
            checkItem.setMessage("磁盘检查过程中发生错误: " + e.getMessage());
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