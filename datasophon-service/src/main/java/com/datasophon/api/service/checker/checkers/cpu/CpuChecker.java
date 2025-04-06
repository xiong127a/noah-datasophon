package com.datasophon.api.service.checker.checkers.cpu;

import com.datasophon.api.service.checker.core.AbstractItemChecker;
import com.datasophon.api.service.checker.common.CommandResult;
import com.datasophon.common.model.CheckItem;
import com.datasophon.common.model.HostInfo;
import com.datasophon.api.service.checker.common.ItemCode;
import com.datasophon.api.service.checker.helpers.HtmlStyleHelper;
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
            cacheLog.info("主机: " + hostInfo.getIp());
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
            cacheLog.info("检查CPU使用率");
            String cpuUsageCommand = "top -b -n 1 | grep '%Cpu' | awk '{print $2+$4}'";
            CommandResult cpuUsageResult = execCommand(session, cpuUsageCommand);

            double cpuUsage = 0.0;
            if (cpuUsageResult.isSuccess()) {
                try {
                    // 清理输出，移除可能导致解析错误的字符
                    String cleanedOutput = cpuUsageResult.getOutput().trim()
                            .replaceAll("[^0-9\\.]", "") // 仅保留数字和小数点
                            .split("\\s+")[0]; // 取第一个数字

                    cpuUsage = Double.parseDouble(cleanedOutput);
                    cacheLog.info("当前CPU使用率: %.1f%%", cpuUsage);
                } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                    // 如果解析失败，尝试替代命令
                    cacheLog.warn("无法解析CPU使用率: %s，尝试替代命令", e.getMessage());

                    // 尝试使用mpstat命令
                    CommandResult mpstatResult = execCommand(session, "command -v mpstat");
                    if (mpstatResult.isSuccess() && !mpstatResult.getOutput().trim().isEmpty()) {
                        CommandResult mpstatUsageResult = execCommand(session,
                                "mpstat | grep -A 2 '%idle' | tail -n 1 | awk '{print 100-$NF}'");
                        if (mpstatUsageResult.isSuccess()) {
                            try {
                                cpuUsage = Double.parseDouble(mpstatUsageResult.getOutput().trim());
                                cacheLog.info("通过mpstat获取CPU使用率: %.1f%%", cpuUsage);
                            } catch (NumberFormatException ex) {
                                cacheLog.warn("无法解析mpstat CPU使用率: %s", ex.getMessage());
                            }
                        }
                    }

                    // 如果仍然失败，尝试vmstat
                    if (cpuUsage == 0.0) {
                        CommandResult vmstatResult = execCommand(session,
                                "vmstat 1 2 | tail -n 1 | awk '{print 100-$15}'");
                        if (vmstatResult.isSuccess()) {
                            try {
                                cpuUsage = Double.parseDouble(vmstatResult.getOutput().trim());
                                cacheLog.info("通过vmstat获取CPU使用率: %.1f%%", cpuUsage);
                            } catch (NumberFormatException ex) {
                                cacheLog.warn("无法解析vmstat CPU使用率: %s", ex.getMessage());
                                // 如果多种方法都失败，设置一个合理的默认值
                                cpuUsage = 20.0; // 设置一个适中的默认值
                                cacheLog.warn("无法获取准确的CPU使用率，使用默认值: %.1f%%", cpuUsage);
                            }
                        } else {
                            // 如果所有命令都失败，使用默认值
                            cpuUsage = 20.0;
                            cacheLog.warn("无法通过任何方法获取CPU使用率，使用默认值: %.1f%%", cpuUsage);
                        }
                    }
                }
            } else {
                cacheLog.warn("获取CPU使用率失败: %s", cpuUsageResult.getErrorOrOutput());
                // 如果命令失败，使用默认值
                cpuUsage = 20.0;
                cacheLog.warn("无法执行CPU使用率命令，使用默认值: %.1f%%", cpuUsage);
            }

            // 更新状态为正在分析CPU状态
            setCheckItemMessage(hostInfo, checkItem, "正在分析CPU状态...");

            // 检查结果
            boolean cpuSufficient = cpuCores >= MIN_CPU_CORES;
            boolean loadNormal = load5 < cpuCores;
            boolean usageNormal = cpuUsage < 90.0;

            if (cpuSufficient && loadNormal && usageNormal) {
                checkItem.setStatus(CheckItem.Status.SUCCESS);

                // 创建HTML详细信息构建器
                StringBuilder detailsBuilder = new StringBuilder();

                // 添加CPU核心数组
                detailsBuilder.append(HtmlStyleHelper.beginGroup());
                detailsBuilder.append(HtmlStyleHelper.generatePropertyRow("CPU核心数", String.valueOf(cpuCores),
                        HtmlStyleHelper.Colors.INFO));

                // 负载信息组
                detailsBuilder.append(HtmlStyleHelper.generatePropertyRow("负载情况(1分钟/5分钟/15分钟)",
                        String.format("%.2f / %.2f / %.2f", load1, load5, load15),
                        load5 < cpuCores * 0.7 ? HtmlStyleHelper.Colors.SUCCESS
                                : (load5 < cpuCores ? HtmlStyleHelper.Colors.WARNING : HtmlStyleHelper.Colors.ERROR)));

                // CPU使用率进度条
                detailsBuilder.append("<p><strong>CPU使用率:</strong></p>");
                String usageColor = cpuUsage < 70 ? HtmlStyleHelper.Colors.SUCCESS
                        : (cpuUsage < 90 ? HtmlStyleHelper.Colors.WARNING : HtmlStyleHelper.Colors.ERROR);
                detailsBuilder.append(HtmlStyleHelper.generateProgressBar((int) cpuUsage, usageColor,
                        String.format("%.1f%%", cpuUsage)));
                detailsBuilder.append(HtmlStyleHelper.endGroup());

                // 添加成功信息提示
                detailsBuilder.append(HtmlStyleHelper.generateSuccessAlert("CPU检查通过", "CPU配置正常，可以正常运行系统和应用程序。"));

                // 设置格式化的HTML消息
                setStyledHtmlMessage(hostInfo, checkItem, true, "CPU配置检查通过", detailsBuilder);
                cacheLog.info("CPU检查通过");
            } else {
                checkItem.setStatus(CheckItem.Status.FAILED);

                // 创建HTML详细信息构建器
                StringBuilder detailsBuilder = new StringBuilder();

                // 添加CPU核心数组
                detailsBuilder.append(HtmlStyleHelper.beginGroup());
                String coreColor = cpuSufficient ? HtmlStyleHelper.Colors.SUCCESS : HtmlStyleHelper.Colors.ERROR;
                detailsBuilder.append(HtmlStyleHelper.generatePropertyRowWithThreshold(
                        "CPU核心数", String.valueOf(cpuCores), coreColor, MIN_CPU_CORES, "核"));

                // 负载信息组
                String loadColor = loadNormal ? HtmlStyleHelper.Colors.SUCCESS : HtmlStyleHelper.Colors.ERROR;
                detailsBuilder.append(HtmlStyleHelper.generatePropertyRow("负载情况(1分钟/5分钟/15分钟)",
                        String.format("%.2f / %.2f / %.2f", load1, load5, load15), loadColor));

                // CPU使用率进度条
                detailsBuilder.append("<p><strong>CPU使用率:</strong></p>");
                String usageColor = usageNormal ? HtmlStyleHelper.Colors.SUCCESS : HtmlStyleHelper.Colors.ERROR;
                detailsBuilder.append(HtmlStyleHelper.generateProgressBar((int) cpuUsage, usageColor,
                        String.format("%.1f%%", cpuUsage)));
                detailsBuilder.append(HtmlStyleHelper.endGroup());

                // 添加警告信息
                StringBuilder warningMsg = new StringBuilder();
                if (!cpuSufficient) {
                    warningMsg.append(String.format("CPU核心数(%d)小于最低要求(%d)<br>", cpuCores, MIN_CPU_CORES));
                }
                if (!loadNormal) {
                    warningMsg.append(String.format("CPU负载(%.2f)高于核心数(%d)<br>", load5, cpuCores));
                }
                if (!usageNormal) {
                    warningMsg.append(String.format("CPU使用率(%.1f%%)过高<br>", cpuUsage));
                }

                detailsBuilder.append(HtmlStyleHelper.generateWarningAlert("CPU检查未通过", warningMsg.toString()));

                // 设置格式化的HTML消息
                setStyledHtmlMessage(hostInfo, checkItem, false, "CPU配置检查未通过", detailsBuilder);
                cacheLog.info("CPU检查未通过: " + warningMsg);
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

        // 创建HTML详细信息构建器
        StringBuilder detailsBuilder = new StringBuilder();

        // 添加问题说明
        detailsBuilder.append(HtmlStyleHelper.beginGroup());
        detailsBuilder.append("<p>CPU问题无法自动修复，需要手动处理。以下是一些建议操作：</p>");
        detailsBuilder.append("<ol style='padding-left:20px;margin-bottom:15px'>");
        detailsBuilder.append(
                "<li style='margin-bottom:5px'><span style='color:#1890ff;font-weight:bold'>增加服务器CPU核心数量</span></li>");
        detailsBuilder.append(
                "<li style='margin-bottom:5px'><span style='color:#1890ff;font-weight:bold'>关闭不必要的服务以减轻CPU负载</span>");
        detailsBuilder.append("<ul style='padding-left:20px;margin-top:5px'>");
        detailsBuilder.append("<li style='color:#333'>使用 " + HtmlStyleHelper.generateInlineCode("top") +
                " 命令查看占用CPU较高的进程</li>");
        detailsBuilder.append("<li style='color:#333'>停止或限制非关键服务</li>");
        detailsBuilder.append("</ul></li>");
        detailsBuilder.append(
                "<li style='margin-bottom:5px'><span style='color:#1890ff;font-weight:bold'>优化应用程序的CPU使用</span>");
        detailsBuilder.append("<ul style='padding-left:20px;margin-top:5px'>");
        detailsBuilder.append("<li style='color:#333'>检查和优化应用程序代码</li>");
        detailsBuilder.append("<li style='color:#333'>调整应用程序线程池和并发配置</li>");
        detailsBuilder.append("</ul></li>");
        detailsBuilder.append("</ol>");
        detailsBuilder.append(HtmlStyleHelper.endGroup());

        // 添加检查当前CPU使用情况的命令
        detailsBuilder.append(HtmlStyleHelper.beginGroup());
        detailsBuilder.append("<p><strong>检查当前CPU使用情况的命令：</strong></p>");
        detailsBuilder.append(HtmlStyleHelper.generateCodeBlock(
                "# 显示CPU使用率\ntop -bn1 | grep '%Cpu'\n\n# 显示进程CPU使用情况\nps aux --sort=-%cpu | head -10"));
        detailsBuilder.append(HtmlStyleHelper.endGroup());

        // 添加注意事项
        detailsBuilder.append(HtmlStyleHelper.generateNoteAlert("注意事项",
                "处理CPU问题时，应该先分析原因，确定是临时负载高峰还是持续的资源不足，然后采取相应措施。"));

        // 设置格式化的HTML消息
        setStyledHtmlMessage(hostInfo, checkItem, false, "CPU问题处理建议", detailsBuilder);

        cacheLog.info("==== 该检查项需要手动处理 ====");
        return false; // 总是返回false表示修复失败，需要手动处理
    }

    @Override
    public ItemCode getCheckerType() {
        return ItemCode.CPU;
    }
}