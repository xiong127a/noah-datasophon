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
public class CpuChecker extends AbstractItemChecker {

    private static final Logger logger = LoggerFactory.getLogger(CpuChecker.class);
    private static final int MIN_CPU_CORES = 4;

    @Override
    protected CheckItem doCheck(HostInfo hostInfo, CheckItem checkItem) {
        try {
            cacheLog.info("==== CPU检查开始 ====");
            cacheLog.info("主机: " + hostInfo.getHostname());
            cacheLog.info("最小CPU核心数要求: " + MIN_CPU_CORES);

            // 更新状态为正在检查CPU核心数
            setCheckItemMessage(hostInfo, checkItem, "正在检查CPU核心数...");

            // 检查CPU核心数
            cacheLog.info("检查CPU核心数...");
            CommandResult cpuResult = execCommand(session, "nproc");
            
            if (!cpuResult.isSuccess()) {
                cacheLog.error("获取CPU核心数失败: %s", cpuResult.getErrorOrOutput());
                checkItem.setStatus(CheckItem.Status.FAILED);
                setCheckItemMessage(hostInfo, checkItem, "获取CPU核心数失败: " + cpuResult.getErrorOrOutput());
                return checkItem;
            }

            int cpuCores = Integer.parseInt(cpuResult.getOutput().trim());
            cacheLog.info("CPU核心数: " + cpuCores);

            // 更新状态为正在检查CPU负载
            setCheckItemMessage(hostInfo, checkItem, "正在检查CPU负载...");

            // 检查CPU负载
            cacheLog.info("检查CPU负载...");
            CommandResult loadResult = execCommand(session, "cat /proc/loadavg");
            
            if (!loadResult.isSuccess()) {
                cacheLog.error("获取CPU负载失败: %s", loadResult.getErrorOrOutput());
                checkItem.setStatus(CheckItem.Status.FAILED);
                setCheckItemMessage(hostInfo, checkItem, "获取CPU负载失败: " + loadResult.getErrorOrOutput());
                return checkItem;
            }

            String[] loadAvg = loadResult.getOutput().trim().split(" ");
            double load1 = Double.parseDouble(loadAvg[0]);
            double load5 = Double.parseDouble(loadAvg[1]);
            double load15 = Double.parseDouble(loadAvg[2]);
            
            cacheLog.info(String.format("CPU负载(1分钟/5分钟/15分钟): %.2f/%.2f/%.2f", 
                load1, load5, load15));

            // 更新状态为正在检查CPU使用率
            setCheckItemMessage(hostInfo, checkItem, "正在检查CPU使用率...");

            // 检查CPU使用率
            cacheLog.info("检查CPU使用率...");
            CommandResult usageResult = execCommand(session, 
                "top -bn1 | grep '%Cpu' | awk '{print $2 + $4}'");
            
            if (!usageResult.isSuccess()) {
                cacheLog.error("获取CPU使用率失败: %s", usageResult.getErrorOrOutput());
                checkItem.setStatus(CheckItem.Status.FAILED);
                setCheckItemMessage(hostInfo, checkItem, "获取CPU使用率失败: " + usageResult.getErrorOrOutput());
                return checkItem;
            }

            double cpuUsage = Double.parseDouble(usageResult.getOutput().trim());
            cacheLog.info("CPU使用率: " + cpuUsage + "%");

            // 更新状态为正在分析CPU状态
            setCheckItemMessage(hostInfo, checkItem, "正在分析CPU状态...");

            // 检查结果
            boolean cpuSufficient = cpuCores >= MIN_CPU_CORES;
            boolean loadNormal = load5 < cpuCores;
            boolean usageNormal = cpuUsage < 90.0;

            if (cpuSufficient && loadNormal && usageNormal) {
                checkItem.setStatus(CheckItem.Status.SUCCESS);
                setCheckItemMessage(hostInfo, checkItem, String.format("CPU配置正常: %d核心, 负载%.2f, 使用率%.1f%%",
                    cpuCores, load5, cpuUsage));
                cacheLog.info("CPU检查通过");
            } else {
                checkItem.setStatus(CheckItem.Status.FAILED);
                StringBuilder message = new StringBuilder("CPU检查未通过: ");
                if (!cpuSufficient) {
                    message.append(String.format("CPU核心数(%d)小于最低要求(%d); ", 
                        cpuCores, MIN_CPU_CORES));
                }
                if (!loadNormal) {
                    message.append(String.format("CPU负载(%.2f)高于核心数(%d); ", 
                        load5, cpuCores));
                }
                if (!usageNormal) {
                    message.append(String.format("CPU使用率(%.1f%%)过高; ", cpuUsage));
                }
                setCheckItemMessage(hostInfo, checkItem, message.toString());
                cacheLog.info("CPU检查未通过: " + message);
            }

        } catch (Exception e) {
            String errorMsg = "检查CPU时发生错误: " + e.getMessage();
            logger.error(errorMsg, e);
            cacheLog.error(errorMsg);
            checkItem.setStatus(CheckItem.Status.FAILED);
            setCheckItemMessage(hostInfo, checkItem, errorMsg);
        } finally {
            cacheLog.info("==== CPU检查结束 ====");
        }
        return checkItem;
    }

    @Override
    protected boolean doFix(HostInfo hostInfo, CheckItem checkItem) {
        cacheLog.info("==== CPU问题说明 ====");
        cacheLog.error("CPU问题无法自动修复，请手动处理");
        cacheLog.info("推荐操作:");
        cacheLog.info("1. 增加服务器CPU核心数量");
        cacheLog.info("2. 关闭不必要的服务以减轻CPU负载");
        cacheLog.info("3. 优化应用程序的CPU使用");
        cacheLog.info("==== 该检查项需要手动处理 ====");
        
        setCheckItemMessage(hostInfo, checkItem, "CPU问题需要手动处理，请参考日志中的建议");
        
        return false; // 总是返回false表示修复失败，需要手动处理
    }

    @Override
    public ItemCode getCheckerType() {
        return ItemCode.CPU;
    }
} 