package com.datasophon.api.service.checker.impl;

import com.datasophon.api.service.checker.AbstractItemChecker;
import com.datasophon.api.service.checker.CommandResult;
import com.datasophon.common.model.CheckItem;
import com.datasophon.common.model.HostInfo;
import com.datasophon.common.model.ItemCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DiskChecker extends AbstractItemChecker {

    private static final Logger logger = LoggerFactory.getLogger(DiskChecker.class);
    private static final int MIN_DISK_SPACE_GB = 100;
    private static final String TARGET_DIRECTORY = "/opt";

    @Override
    protected CheckItem doCheck(HostInfo hostInfo, CheckItem checkItem) {
        try {
            cacheLog.info("==== 磁盘空间检查开始 ====");
            cacheLog.info("主机: " + hostInfo.getHostname());
            cacheLog.info("目标目录: " + TARGET_DIRECTORY);
            cacheLog.info("最小磁盘空间要求: " + MIN_DISK_SPACE_GB + "GB");

            // 获取目标目录磁盘使用情况
            cacheLog.info("检查" + TARGET_DIRECTORY + "目录磁盘使用情况...");
            CommandResult dfResult = execCommand(session, "df -BG " + TARGET_DIRECTORY + " | tail -n 1");
            
            if (!dfResult.isSuccess()) {
                cacheLog.error("获取磁盘信息失败: %s", dfResult.getErrorOrOutput());
                checkItem.setStatus(CheckItem.Status.FAILED);
                checkItem.setMessage("获取磁盘信息失败: " + dfResult.getErrorOrOutput());
                return checkItem;
            }

            // 解析df命令输出
            String[] parts = dfResult.getOutput().trim().split("\\s+");
            if (parts.length < 4) {
                cacheLog.error("无法解析磁盘信息: %s", dfResult.getOutput());
                checkItem.setStatus(CheckItem.Status.FAILED);
                checkItem.setMessage("无法解析磁盘信息");
                return checkItem;
            }

            // 获取可用空间（去掉G后缀）
            String availableSpace = parts[3].replace("G", "");
            int availableGB = Integer.parseInt(availableSpace);
            
            cacheLog.info(TARGET_DIRECTORY + "可用空间: " + availableGB + "GB");
            boolean hasEnoughSpace = availableGB >= MIN_DISK_SPACE_GB;

            // 获取磁盘使用率
            String usagePercent = parts[4].replace("%", "");
            int diskUsage = Integer.parseInt(usagePercent);
            cacheLog.info(TARGET_DIRECTORY + "使用率: " + diskUsage + "%");

            // 检查inode使用情况
            cacheLog.info("检查inode使用情况...");
            CommandResult inodeResult = execCommand(session, "df -i " + TARGET_DIRECTORY + " | tail -n 1");
            
            if (!inodeResult.isSuccess()) {
                cacheLog.error("获取inode信息失败: %s", inodeResult.getErrorOrOutput());
                checkItem.setStatus(CheckItem.Status.FAILED);
                checkItem.setMessage("获取inode信息失败: " + inodeResult.getErrorOrOutput());
                return checkItem;
            }

            parts = inodeResult.getOutput().trim().split("\\s+");
            if (parts.length < 5) {
                cacheLog.error("无法解析inode信息: %s", inodeResult.getOutput());
                checkItem.setStatus(CheckItem.Status.FAILED);
                checkItem.setMessage("无法解析inode信息");
                return checkItem;
            }

            String inodeUsagePercent = parts[4].replace("%", "");
            int inodeUsage = Integer.parseInt(inodeUsagePercent);
            cacheLog.info("inode使用率: " + inodeUsage + "%");

            // 设置检查结果
            if (hasEnoughSpace && diskUsage < 90 && inodeUsage < 90) {
                checkItem.setStatus(CheckItem.Status.SUCCESS);
                checkItem.setMessage(String.format(TARGET_DIRECTORY + "磁盘空间充足: 可用%dGB, 使用率%d%%, inode使用率%d%%",
                        availableGB, diskUsage, inodeUsage));
                cacheLog.info("磁盘空间检查通过");
            } else {
                checkItem.setStatus(CheckItem.Status.FAILED);
                StringBuilder message = new StringBuilder("磁盘空间检查未通过: ");
                if (!hasEnoughSpace) {
                    message.append(String.format(TARGET_DIRECTORY + "可用空间%dGB小于最低要求%dGB; ", availableGB, MIN_DISK_SPACE_GB));
                }
                if (diskUsage >= 90) {
                    message.append(String.format(TARGET_DIRECTORY + "磁盘使用率%d%%过高; ", diskUsage));
                }
                if (inodeUsage >= 90) {
                    message.append(String.format("inode使用率%d%%过高; ", inodeUsage));
                }
                checkItem.setMessage(message.toString());
                cacheLog.info("磁盘空间检查未通过: " + message);
            }

        } catch (Exception e) {
            String errorMsg = "检查磁盘空间时发生错误: " + e.getMessage();
            logger.error(errorMsg, e);
            cacheLog.error(errorMsg);
            checkItem.setStatus(CheckItem.Status.FAILED);
            checkItem.setMessage(errorMsg);
        } finally {
            cacheLog.info("==== 磁盘空间检查结束 ====");
        }
        return checkItem;
    }

    @Override
    protected boolean doFix(HostInfo hostInfo, CheckItem checkItem) {
        cacheLog.info("==== 磁盘空间问题说明 ====");
        cacheLog.error("磁盘空间问题无法自动修复，请手动处理");
        cacheLog.info("推荐操作:");
        cacheLog.info("1. 清理不必要的文件和日志");
        cacheLog.info("2. 扩展" + TARGET_DIRECTORY + "分区大小");
        cacheLog.info("3. 挂载额外的磁盘到" + TARGET_DIRECTORY + "目录");
        cacheLog.info("==== 该检查项需要手动处理 ====");
        
        return false;
    }

    @Override
    public ItemCode getCheckerType() {
        return ItemCode.DISK;
    }
} 