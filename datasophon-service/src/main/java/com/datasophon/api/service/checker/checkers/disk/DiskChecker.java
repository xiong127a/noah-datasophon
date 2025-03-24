package com.datasophon.api.service.checker.checkers.disk;

import com.datasophon.api.service.checker.core.AbstractItemChecker;
import com.datasophon.api.service.checker.common.CommandResult;
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

            // 更新状态为正在检查目标目录磁盘使用情况
            setCheckItemMessage(hostInfo, checkItem, "正在检查" + TARGET_DIRECTORY + "目录磁盘使用情况...");

            // 获取目标目录磁盘使用情况
            cacheLog.info("检查" + TARGET_DIRECTORY + "目录磁盘使用情况...");
            // 使用grep替代tail，更好地适应SSH会话
            CommandResult dfResult = execCommand(session, "df -BG " + TARGET_DIRECTORY + " | grep " + TARGET_DIRECTORY);
            
            // 如果失败尝试其他命令
            if (!dfResult.isSuccess() || dfResult.getOutput() == null || dfResult.getOutput().trim().isEmpty()) {
                cacheLog.info("使用grep提取信息失败，尝试使用awk...");
                dfResult = execCommand(session, "df -BG " + TARGET_DIRECTORY + " | awk 'END{print}'");
            }
            
            // 如果仍然失败，尝试不使用管道
            if (!dfResult.isSuccess() || dfResult.getOutput() == null || dfResult.getOutput().trim().isEmpty()) {
                cacheLog.info("使用管道命令失败，尝试不使用管道...");
                dfResult = execCommand(session, "df -BG " + TARGET_DIRECTORY);
                
                // 如果成功且有多行输出，记录下来
                if (dfResult.isSuccess() && dfResult.getOutput() != null && !dfResult.getOutput().trim().isEmpty()) {
                    cacheLog.info("df完整输出:\n" + dfResult.getOutput());
                    
                    String[] lines = dfResult.getOutput().trim().split("\n");
                    if (lines.length > 1) {
                        // 通常第二行是我们需要的数据
                        for (int i = 1; i < lines.length; i++) {
                            if (lines[i].contains(TARGET_DIRECTORY)) {
                                cacheLog.info("找到目标目录行: " + lines[i]);
                                dfResult = execCommand(session, "echo '" + lines[i] + "'");
                                break;
                            }
                        }
                    }
                }
            }
            
            if (!dfResult.isSuccess()) {
                cacheLog.error("获取磁盘信息失败: %s", dfResult.getErrorOrOutput());
                checkItem.setStatus(CheckItem.Status.FAILED);
                setCheckItemMessage(hostInfo, checkItem, "获取磁盘信息失败: " + dfResult.getErrorOrOutput());
                return checkItem;
            }
            
            // 记录完整的命令输出以便调试
            cacheLog.info("df命令原始输出: " + dfResult.getOutput());
            
            // 检查命令输出是否为空
            if (dfResult.getOutput() == null || dfResult.getOutput().trim().isEmpty()) {
                cacheLog.error("磁盘信息为空，可能是命令执行失败或目标目录不存在");
                // 尝试不带路径的命令
                cacheLog.info("尝试获取根文件系统的磁盘使用情况...");
                dfResult = execCommand(session, "df -BG / | tail -n 1");
                
                if (!dfResult.isSuccess() || dfResult.getOutput() == null || dfResult.getOutput().trim().isEmpty()) {
                    cacheLog.error("获取根文件系统磁盘信息也失败: %s", dfResult.getErrorOrOutput());
                    checkItem.setStatus(CheckItem.Status.FAILED);
                    setCheckItemMessage(hostInfo, checkItem, "无法获取文件系统磁盘信息，请确保目标目录存在且有权限访问");
                    return checkItem;
                }
                
                cacheLog.info("使用根文件系统的磁盘信息替代: " + dfResult.getOutput());
            }

            // 解析df命令输出
            String[] parts = dfResult.getOutput().trim().split("\\s+");
            if (parts.length < 4) {
                cacheLog.error("无法解析磁盘信息: %s", dfResult.getOutput());
                checkItem.setStatus(CheckItem.Status.FAILED);
                setCheckItemMessage(hostInfo, checkItem, "无法解析磁盘信息");
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

            // 更新状态为正在检查inode使用情况
            setCheckItemMessage(hostInfo, checkItem, "正在检查inode使用情况...");

            // 检查inode使用情况
            cacheLog.info("检查inode使用情况...");
            // 修改命令避免使用管道，直接使用awk提取信息
            // 备选命令1: df -i /opt | grep /opt
            // 备选命令2: df -i /opt | awk 'END{print}'
            CommandResult inodeResult = execCommand(session, "df -i " + TARGET_DIRECTORY + " | grep " + TARGET_DIRECTORY);
            
            // 如果第一个命令失败，尝试其他选项
            if (!inodeResult.isSuccess() || inodeResult.getOutput() == null || inodeResult.getOutput().trim().isEmpty()) {
                cacheLog.info("使用grep提取信息失败，尝试使用awk...");
                inodeResult = execCommand(session, "df -i " + TARGET_DIRECTORY + " | awk 'END{print}'");
            }
            
            // 如果仍然失败，尝试不使用管道
            if (!inodeResult.isSuccess() || inodeResult.getOutput() == null || inodeResult.getOutput().trim().isEmpty()) {
                cacheLog.info("使用管道命令失败，尝试不使用管道...");
                inodeResult = execCommand(session, "df -i " + TARGET_DIRECTORY);
                
                // 如果有多行输出，只保留最后一行
                if (inodeResult.isSuccess() && inodeResult.getOutput() != null && !inodeResult.getOutput().trim().isEmpty()) {
                    String[] lines = inodeResult.getOutput().trim().split("\n");
                    if (lines.length > 1) {
                        String lastLine = lines[lines.length - 1];
                        cacheLog.info("多行输出，使用最后一行: " + lastLine);
                        // 将原始输出替换为处理后的输出
                        cacheLog.info("使用最后一行数据进行后续处理");
                    }
                }
            }
            
            if (!inodeResult.isSuccess() || inodeResult.getOutput() == null || inodeResult.getOutput().trim().isEmpty()) {
                cacheLog.error("获取inode信息失败: %s", inodeResult.getErrorOrOutput());
                // 尝试获取根文件系统信息
                cacheLog.info("尝试获取根文件系统的inode使用情况...");
                inodeResult = execCommand(session, "df -i / | grep /");
                
                if (!inodeResult.isSuccess() || inodeResult.getOutput() == null || inodeResult.getOutput().trim().isEmpty()) {
                    cacheLog.error("获取根文件系统inode信息也失败: %s", inodeResult.getErrorOrOutput());
                    checkItem.setStatus(CheckItem.Status.FAILED);
                    setCheckItemMessage(hostInfo, checkItem, "无法获取文件系统inode信息，请确保目标目录存在且有权限访问");
                    return checkItem;
                }
                
                cacheLog.info("使用根文件系统的inode信息替代: %s", inodeResult.getOutput());
            }

            parts = inodeResult.getOutput().trim().split("\\s+");
            if (parts.length < 5) {
                cacheLog.error("无法解析inode信息: %s", inodeResult.getOutput());
                checkItem.setStatus(CheckItem.Status.FAILED);
                setCheckItemMessage(hostInfo, checkItem, "无法解析inode信息");
                return checkItem;
            }

            String inodeUsagePercent = parts[4].replace("%", "");
            int inodeUsage = Integer.parseInt(inodeUsagePercent);
            cacheLog.info("inode使用率: " + inodeUsage + "%");

            // 更新状态为正在分析磁盘状态
            setCheckItemMessage(hostInfo, checkItem, "正在分析磁盘状态...");

            // 设置检查结果
            if (hasEnoughSpace && diskUsage < 90 && inodeUsage < 90) {
                checkItem.setStatus(CheckItem.Status.SUCCESS);
                setCheckItemMessage(hostInfo, checkItem, String.format(TARGET_DIRECTORY + "磁盘空间充足: 可用%dGB, 使用率%d%%, inode使用率%d%%",
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
                setCheckItemMessage(hostInfo, checkItem, message.toString());
                cacheLog.info("磁盘空间检查未通过: " + message);
            }

        } catch (Exception e) {
            String errorMsg = "检查磁盘空间时发生错误: " + e.getMessage();
            logger.error(errorMsg, e);
            cacheLog.error(errorMsg);
            checkItem.setStatus(CheckItem.Status.FAILED);
            setCheckItemMessage(hostInfo, checkItem, errorMsg);
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
        
        setCheckItemMessage(hostInfo, checkItem, "磁盘空间问题需要手动处理，请参考日志中的建议");
        
        return false;
    }

    @Override
    public ItemCode getCheckerType() {
        return ItemCode.DISK;
    }
} 