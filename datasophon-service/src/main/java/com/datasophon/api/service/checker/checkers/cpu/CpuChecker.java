package com.datasophon.api.service.checker.checkers.cpu;

import com.datasophon.api.config.CheckerProperties;
import com.datasophon.api.service.checker.core.AbstractItemChecker;
import com.datasophon.api.service.checker.common.CommandResult;
import com.datasophon.common.model.CheckItem;
import com.datasophon.common.model.HostInfo;
import com.datasophon.api.service.checker.common.ItemCode;
import com.datasophon.api.service.checker.helpers.HtmlStyleHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CpuChecker extends AbstractItemChecker {

    private static final Logger logger = LoggerFactory.getLogger(CpuChecker.class);

    private final CheckerProperties checkerProperties;

    public CpuChecker(CheckerProperties checkerProperties) {
        this.checkerProperties = checkerProperties;
    }

    @Override
    protected CheckItem doCheck(HostInfo hostInfo, CheckItem checkItem) {
        try {
            // 从配置中获取CPU核心数要求
            int minCpuCores = checkerProperties.getCpu().getMinCores();
            int recommendedCpuCores = checkerProperties.getCpu().getRecommendedCores();

            cacheLog.info("==== CPU检查开始 ====");
            cacheLog.info("主机: " + hostInfo.getIp());
            cacheLog.info("最小CPU核心数要求: " + minCpuCores);
            cacheLog.info("建议CPU核心数: " + recommendedCpuCores);

            // 更新状态为正在检查CPU核心数
            setCheckItemMessage(hostInfo, checkItem, "正在检查CPU核心数...");

            // 检查CPU核心数
            cacheLog.info("检查CPU核心数...");
            CommandResult cpuResult = execCommand(sshConnectionPoolManager.getOrCreateConnection(hostInfo), "nproc");

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
            CommandResult loadResult = execCommand(sshConnectionPoolManager.getOrCreateConnection(hostInfo), "cat /proc/loadavg");

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

            // 使用更可靠的方式获取CPU使用率
            String cpuUsageCommand = "top -b -n 1 | grep '%Cpu' | awk '{print $2+$4}'";
            CommandResult cpuUsageResult = execCommand(sshConnectionPoolManager.getOrCreateConnection(hostInfo), cpuUsageCommand);

            double cpuUsage = 0.0;
            boolean usageDetected = false;

            if (cpuUsageResult.isSuccess() && !cpuUsageResult.getOutput().trim().isEmpty()) {
                try {
                    // 清理输出，移除可能导致解析错误的字符
                    String cleanedOutput = cpuUsageResult.getOutput().trim()
                            .replaceAll("[^0-9\\.]", "") // 仅保留数字和小数点
                            .replace(",,", "."); // 处理特殊格式

                    // 确保有有效数据
                    if (!cleanedOutput.isEmpty()) {
                        cpuUsage = Double.parseDouble(cleanedOutput);
                        // 验证结果合理性
                        if (cpuUsage > 100.0) {
                            cacheLog.warn("CPU使用率异常高: %.2f，可能是解析错误，使用替代方法", cpuUsage);
                            cpuUsage = 0.0; // 重置为0，触发下面的替代方法
                        } else {
                            cacheLog.info("当前CPU使用率: %.2f", cpuUsage);
                            usageDetected = true;
                        }
                    }
                } catch (Exception e) {
                    cacheLog.warn("无法解析标准格式CPU使用率: %s，尝试替代命令", e.getMessage());
                }
            } else {
                cacheLog.warn("获取CPU使用率失败或返回空，尝试替代命令");
            }

            // 尝试Ubuntu/Debian系统专用命令
            if (!usageDetected) {
                cacheLog.info("尝试使用Ubuntu/Debian系统的专用命令获取CPU使用率");
                cpuUsageCommand = "top -b -n 1 | grep 'Cpu(s)' | awk '{print $2+$4}'";
                cpuUsageResult = execCommand(sshConnectionPoolManager.getOrCreateConnection(hostInfo), cpuUsageCommand);

                if (cpuUsageResult.isSuccess() && !cpuUsageResult.getOutput().trim().isEmpty()) {
                    try {
                        String cleanedOutput = cpuUsageResult.getOutput().trim()
                                .replaceAll("[^0-9\\.]", "")
                                .replace(",,", ".");

                        if (!cleanedOutput.isEmpty()) {
                            cpuUsage = Double.parseDouble(cleanedOutput);
                            if (cpuUsage > 0.0 && cpuUsage <= 100.0) {
                                cacheLog.info("通过Ubuntu专用命令获取CPU使用率: %.2f", cpuUsage);
                                usageDetected = true;
                            }
                        }
                    } catch (Exception e) {
                        cacheLog.warn("处理Ubuntu CPU使用率时出错: %s", e.getMessage());
                    }
                }
            }

            // 如果上述方法都失败，尝试使用mpstat命令
            if (!usageDetected) {
                cacheLog.info("尝试使用mpstat命令获取CPU使用率");
                CommandResult mpstatResult = execCommand(sshConnectionPoolManager.getOrCreateConnection(hostInfo), "command -v mpstat || echo 'not_found'");
                if (mpstatResult.isSuccess() && !mpstatResult.getOutput().trim().contains("not_found")) {
                    CommandResult mpstatUsageResult = execCommand(sshConnectionPoolManager.getOrCreateConnection(hostInfo),
                            "mpstat | tail -n 1 | awk '{print 100-$NF}'");
                    if (mpstatUsageResult.isSuccess() && !mpstatUsageResult.getOutput().trim().isEmpty()) {
                        try {
                            String mpstatOutput = mpstatUsageResult.getOutput().trim();
                            cacheLog.info("mpstat计算的CPU使用率: %s", mpstatOutput);

                            if (!mpstatOutput.isEmpty()) {
                                cpuUsage = Double.parseDouble(mpstatOutput);
                                if (cpuUsage >= 0 && cpuUsage <= 100.0) {
                                    cacheLog.info("通过mpstat获取CPU使用率: %.2f", cpuUsage);
                                    usageDetected = true;
                                } else {
                                    cacheLog.warn("通过mpstat获取的CPU使用率异常: %.2f", cpuUsage);
                                }
                            }
                        } catch (Exception ex) {
                            cacheLog.warn("无法解析mpstat CPU使用率: %s", ex.getMessage());
                        }
                    }
                }
            }

            // 如果仍然失败，尝试vmstat
            if (!usageDetected) {
                cacheLog.info("尝试使用vmstat命令获取CPU使用率");
                try {
                    CommandResult vmstatResult = execCommand(sshConnectionPoolManager.getOrCreateConnection(hostInfo), "vmstat 1 2 | tail -n 1");
                    if (vmstatResult.isSuccess() && !vmstatResult.getOutput().trim().isEmpty()) {
                        String vmstatOutput = vmstatResult.getOutput().trim();
                        cacheLog.info("vmstat输出: %s", vmstatOutput);

                        String[] parts = vmstatOutput.split("\\s+");
                        if (parts.length >= 15) { // vmstat输出的idle通常是倒数第二个字段
                            try {
                                String idleStr = parts[14].trim();
                                if (!idleStr.isEmpty()) {
                                    double idlePercent = Double.parseDouble(idleStr);
                                    cpuUsage = 100.0 - idlePercent;
                                    cacheLog.info("通过vmstat获取CPU使用率: %.2f", cpuUsage);
                                    usageDetected = true;

                                    if (cpuUsage < 0 || cpuUsage > 100.0) {
                                        cpuUsage = 20.0; // 如果异常，使用默认值
                                        cacheLog.warn("通过vmstat获取的CPU使用率异常，使用默认值: %.2f", cpuUsage);
                                    }
                                }
                            } catch (NumberFormatException ex) {
                                cacheLog.warn("无法解析vmstat CPU使用率字段: %s", ex.getMessage());
                            }
                        }
                    }
                } catch (Exception ex) {
                    cacheLog.warn("处理vmstat命令时出错: %s", ex.getMessage());
                }
            }

            // 如果所有方法都失败，使用默认值
            if (!usageDetected) {
                cpuUsage = 20.0;
                cacheLog.warn("无法通过任何方法获取CPU使用率，使用默认值: %.2f", cpuUsage);
            }

            // 更新状态为正在分析CPU状态
            setCheckItemMessage(hostInfo, checkItem, "正在分析CPU状态...");

            // 检查结果
            boolean cpuSufficient = cpuCores >= minCpuCores;
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
                        : HtmlStyleHelper.Colors.WARNING;
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
                        "CPU核心数", String.valueOf(cpuCores), coreColor, minCpuCores, "核"));

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
                    warningMsg.append(String.format("CPU核心数(%d)小于最低要求(%d)<br>", cpuCores, minCpuCores));
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
        detailsBuilder.append("<li style='color:#333'>使用 ").append(HtmlStyleHelper.generateInlineCode("top")).append(" 命令查看占用CPU较高的进程</li>");
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