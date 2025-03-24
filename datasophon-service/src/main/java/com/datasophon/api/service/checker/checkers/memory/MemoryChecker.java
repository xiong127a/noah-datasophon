package com.datasophon.api.service.checker.checkers.memory;

import com.datasophon.api.service.checker.core.AbstractItemChecker;
import com.datasophon.api.service.checker.common.CommandResult;
import com.datasophon.common.model.CheckItem;
import com.datasophon.common.model.HostInfo;
import com.datasophon.common.model.ItemCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MemoryChecker extends AbstractItemChecker {

    private static final Logger logger = LoggerFactory.getLogger(MemoryChecker.class);
    private static final int MIN_MEMORY_GB = 16;

    @Override
    protected CheckItem doCheck(HostInfo hostInfo, CheckItem checkItem) {
        try {
            cacheLog.info("==== 内存检查开始 ====");
            cacheLog.info("主机: " + hostInfo.getHostname());
            cacheLog.info("最小内存要求: " + MIN_MEMORY_GB + "GB");

            // 更新状态为正在检查总内存大小
            setCheckItemMessage(hostInfo, checkItem, "正在检查总内存大小...");

            // 获取总内存大小
            cacheLog.info("检查总内存大小...");
            CommandResult memResult = execCommand(session, "free -g | grep Mem:");
            
            if (!memResult.isSuccess()) {
                cacheLog.error("获取内存信息失败: %s", memResult.getErrorOrOutput());
                checkItem.setStatus(CheckItem.Status.FAILED);
                setCheckItemMessage(hostInfo, checkItem, "获取内存信息失败: " + memResult.getErrorOrOutput());
                return checkItem;
            }

            String[] memInfo = memResult.getOutput().trim().split("\\s+");
            int totalMemGB = Integer.parseInt(memInfo[1]);
            int usedMemGB = Integer.parseInt(memInfo[2]);
            int freeMemGB = Integer.parseInt(memInfo[3]);
            
            cacheLog.info(String.format("总内存: %dGB, 已用: %dGB, 空闲: %dGB", 
                totalMemGB, usedMemGB, freeMemGB));

            // 更新状态为正在检查swap使用情况
            setCheckItemMessage(hostInfo, checkItem, "正在检查swap使用情况...");

            // 获取swap使用情况
            cacheLog.info("检查swap使用情况...");
            CommandResult swapResult = execCommand(session, "free -g | grep Swap:");
            
            if (!swapResult.isSuccess()) {
                cacheLog.error("获取swap信息失败: %s", swapResult.getErrorOrOutput());
                checkItem.setStatus(CheckItem.Status.FAILED);
                setCheckItemMessage(hostInfo, checkItem, "获取swap信息失败: " + swapResult.getErrorOrOutput());
                return checkItem;
            }

            String[] swapInfo = swapResult.getOutput().trim().split("\\s+");
            int totalSwapGB = Integer.parseInt(swapInfo[1]);
            int usedSwapGB = Integer.parseInt(swapInfo[2]);
            
            cacheLog.info(String.format("Swap总量: %dGB, 已用: %dGB", totalSwapGB, usedSwapGB));

            // 更新状态为正在分析内存状态
            setCheckItemMessage(hostInfo, checkItem, "正在分析内存状态...");

            // 检查结果
            boolean memorySufficient = totalMemGB >= MIN_MEMORY_GB;
            boolean memoryUsageNormal = (double)usedMemGB / totalMemGB <= 0.9; // 内存使用率阈值90%
            boolean swapUsageNormal = totalSwapGB == 0 || (double)usedSwapGB / totalSwapGB <= 0.5; // swap使用率阈值50%

            if (memorySufficient && memoryUsageNormal && swapUsageNormal) {
                checkItem.setStatus(CheckItem.Status.SUCCESS);
                setCheckItemMessage(hostInfo, checkItem, String.format("内存配置正常: 总内存%dGB, 使用率%.1f%%, Swap使用率%.1f%%",
                    totalMemGB,
                    (double)usedMemGB / totalMemGB * 100,
                    totalSwapGB == 0 ? 0 : (double)usedSwapGB / totalSwapGB * 100));
                cacheLog.info("内存检查通过");
            } else {
                checkItem.setStatus(CheckItem.Status.FAILED);
                StringBuilder message = new StringBuilder("内存检查未通过: ");
                if (!memorySufficient) {
                    message.append(String.format("总内存(%dGB)小于最低要求(%dGB); ", 
                        totalMemGB, MIN_MEMORY_GB));
                }
                if (!memoryUsageNormal) {
                    message.append(String.format("内存使用率(%.1f%%)过高; ",
                        (double)usedMemGB / totalMemGB * 100));
                }
                if (!swapUsageNormal) {
                    message.append(String.format("Swap使用率(%.1f%%)过高; ",
                        (double)usedSwapGB / totalSwapGB * 100));
                }
                setCheckItemMessage(hostInfo, checkItem, message.toString());
                cacheLog.info("内存检查未通过: " + message);
            }

        } catch (Exception e) {
            String errorMsg = "检查内存时发生错误: " + e.getMessage();
            logger.error(errorMsg, e);
            cacheLog.error(errorMsg);
            checkItem.setStatus(CheckItem.Status.FAILED);
            setCheckItemMessage(hostInfo, checkItem, errorMsg);
        } finally {
            cacheLog.info("==== 内存检查结束 ====");
        }
        return checkItem;
    }

    @Override
    protected boolean doFix(HostInfo hostInfo, CheckItem checkItem) {
        cacheLog.info("==== 内存问题说明 ====");
        cacheLog.error("内存问题无法自动修复，请手动处理");
        cacheLog.info("推荐操作:");
        cacheLog.info("1. 增加服务器物理内存");
        cacheLog.info("2. 关闭不必要的服务以释放内存");
        cacheLog.info("3. 调整应用程序内存使用配置");
        cacheLog.info("==== 该检查项需要手动处理 ====");
        
        setCheckItemMessage(hostInfo, checkItem, "内存问题需要手动处理，请参考日志中的建议");
        
        return false; // 总是返回false表示修复失败，需要手动处理
    }

    @Override
    public ItemCode getCheckerType() {
        return ItemCode.MEMORY;
    }
} 