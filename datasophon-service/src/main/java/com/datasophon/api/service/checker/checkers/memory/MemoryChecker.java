package com.datasophon.api.service.checker.checkers.memory;

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
public class MemoryChecker extends AbstractItemChecker {

    private static final Logger logger = LoggerFactory.getLogger(MemoryChecker.class);

    @Autowired
    private CheckerProperties checkerProperties;


    @Override
    protected CheckItem doCheck(HostInfo hostInfo, CheckItem checkItem) {
        try {
            // 从配置中获取内存要求
            int minMemoryMB = checkerProperties.getMemory().getMinMemory();
            int recommendedMemoryMB = checkerProperties.getMemory().getRecommendedMemory();
            int minSwapMB = checkerProperties.getMemory().getMinSwap();

            // 转换为GB用于显示
            double minMemoryGB = minMemoryMB / 1024.0;

            cacheLog.info("==== 内存检查开始 ====");
            cacheLog.info("主机: " + hostInfo.getIp());
            cacheLog.info("最小可用内存要求: " + minMemoryGB + "GB (" + minMemoryMB + "MB)");
            cacheLog.info("建议内存: " + (recommendedMemoryMB / 1024.0) + "GB (" + recommendedMemoryMB + "MB)");
            cacheLog.info("最小交换区: " + (minSwapMB / 1024.0) + "GB (" + minSwapMB + "MB)");

            // 更新状态为正在检查内存
            setCheckItemMessage(hostInfo, checkItem, "正在检查内存情况...");

            // 执行free命令获取内存信息
            cacheLog.debug("执行free命令获取内存信息");
            cacheLog.info("正在获取内存信息...");
            CommandResult result = execCommand(sshConnectionPoolManager.getOrCreateConnection(hostInfo), "free -m | grep -E 'Mem|内存'");

            if (!result.isSuccess() || result.getOutput().trim().isEmpty()) {
                cacheLog.warn("free命令失败或输出为空，尝试使用另一种格式...");
                // 尝试只运行free命令，然后处理第二行（通常是内存行）
                result = execCommand(sshConnectionPoolManager.getOrCreateConnection(hostInfo), "free -m | sed -n '2p'");

                if (!result.isSuccess() || result.getOutput().trim().isEmpty()) {
                    cacheLog.warn("free命令失败，尝试使用/proc/meminfo获取内存信息...");
                    CommandResult meminfoResult = execCommand(sshConnectionPoolManager.getOrCreateConnection(hostInfo),
                            "cat /proc/meminfo | grep -E 'MemTotal|MemFree|MemAvailable'");

                    if (meminfoResult.isSuccess() && !meminfoResult.getOutput().trim().isEmpty()) {
                        cacheLog.info("成功获取/proc/meminfo内存信息");
                        String memInfoOutput = meminfoResult.getOutput().trim();

                        // 从/proc/meminfo解析内存信息
                        long totalKb = 0;
                        long freeKb = 0;
                        long availableKb = 0;

                        String[] lines = memInfoOutput.split("\n");
                        for (String line : lines) {
                            if (line.contains("MemTotal")) {
                                String[] parts = line.split("\\s+");
                                if (parts.length >= 2) {
                                    try {
                                        totalKb = Long.parseLong(parts[1]);
                                    } catch (NumberFormatException e) {
                                        cacheLog.warn("无法解析MemTotal值: %s",
                                                line);
                                    }
                                }
                            } else if (line.contains("MemFree")) {
                                String[] parts = line.split("\\s+");
                                if (parts.length >= 2) {
                                    try {
                                        freeKb = Long.parseLong(parts[1]);
                                    } catch (NumberFormatException e) {
                                        cacheLog.warn("无法解析MemFree值: %s", line);
                                    }
                                }
                            } else if (line.contains("MemAvailable")) {
                                String[] parts = line.split("\\s+");
                                if (parts.length >= 2) {
                                    try {
                                        availableKb = Long.parseLong(parts[1]);
                                    } catch (NumberFormatException e) {
                                        cacheLog.warn("无法解析MemAvailable值: %s",
                                                line);
                                    }
                                }
                            }
                        }

                        // 如果没有MemAvailable，使用MemFree
                        if (availableKb == 0) {
                            availableKb = freeKb;
                            cacheLog.info("MemAvailable未找到，使用MemFree: " + freeKb + "KB");
                        }

                        double totalGB = totalKb / 1024.0 / 1024.0;
                        double freeGB = freeKb / 1024.0 / 1024.0;
                        double availableGB = availableKb / 1024.0 / 1024.0;
                        double usedGB = totalGB - freeGB;

                        // 记录解析结果
                        cacheLog.info(String.format(
                                "从/proc/meminfo解析: 总内存=%.2fGB, 已用=%.2fGB, 空闲=%.2fGB, 可用=%.2fGB",
                                totalGB, usedGB, freeGB, availableGB));

                        // 检查是否满足最低内存要求
                        boolean memoryCheckPassed = (availableKb / 1024.0 >= minMemoryMB);
                        cacheLog.info("可用内存检查 " + (memoryCheckPassed ? "通过" : "未通过") + ": "
                                + (availableKb / 1024.0) + "MB >= " + minMemoryMB
                                + "MB: "
                                + memoryCheckPassed);

                        // 构建HTML显示内容
                        StringBuilder htmlBuilder = buildMemoryHtml(hostInfo, checkItem,
                                totalGB, usedGB, freeGB, availableGB,
                                memoryCheckPassed, minMemoryGB,
                                recommendedMemoryMB / 1024.0);

                        // 设置检查结果
                        if (memoryCheckPassed) {
                            checkItem.setStatus(CheckItem.Status.SUCCESS);
                        } else {
                            checkItem.setStatus(CheckItem.Status.FAILED);
                        }
                        checkItem.setMessage(htmlBuilder.toString());

                        cacheLog.info("==== 内存检查完成 ====");
                        cacheLog.info("内存检查结果: " + checkItem.getStatus());

                    } else {
                        checkItem.setStatus(CheckItem.Status.FAILED);
                        checkItem.setMessage("无法获取内存信息: 所有方法都失败");
                        cacheLog.error("无法获取内存信息: 所有方法都失败");
                    }
                    return checkItem;
                }
            }

            // 处理free命令输出
            String[] parts = result.getOutput().trim().split("\\s+");
            cacheLog.debug("Free命令输出解析: " + String.join(", ", parts));

            // 检查是否有足够的数据
            if (parts.length < 2) {
                checkItem.setStatus(CheckItem.Status.FAILED);
                checkItem.setMessage("内存信息格式不正确: " + result.getOutput());
                cacheLog.error("内存信息格式不正确: %s", result.getOutput());
                return checkItem;
            }

            // 支持不同格式的free输出解析
            double totalGB = 0, usedGB = 0, freeGB = 0, availableGB = 0;
            boolean memoryParsed = false;

            try {
                // 识别格式：根据第一个非空元素判断
                int startIdx = 0;
                // 跳过非数字列，如"内存："或"Mem:"
                while (startIdx < parts.length && !isNumeric(parts[startIdx])) {
                    startIdx++;
                }

                if (startIdx < parts.length) {
                    // 确保我们有足够的元素来解析
                    if (startIdx + 2 < parts.length) {
                        totalGB = Double.parseDouble(parts[startIdx]) / 1024.0;
                        usedGB = Double.parseDouble(parts[startIdx + 1]) / 1024.0;
                        freeGB = Double.parseDouble(parts[startIdx + 2]) / 1024.0;

                        // 尝试解析available，如果存在
                        if (parts.length >= startIdx + 6) {
                            availableGB = Double.parseDouble(parts[startIdx + 5]) / 1024.0;
                        } else {
                            // 如果没有available列，使用free
                            availableGB = freeGB;
                        }

                        memoryParsed = true;
                        cacheLog.info(String.format(
                                "成功解析内存信息: 总内存=%.2fGB, 已用=%.2fGB, 空闲=%.2fGB, 可用=%.2fGB",
                                totalGB, usedGB, freeGB, availableGB));
                    }
                }
            } catch (Exception e) {
                cacheLog.warn("解析free命令输出失败: " + e.getMessage());
            }

            // 如果上面的解析失败，尝试其他方法
            if (!memoryParsed) {
                cacheLog.warn("尝试使用/proc/meminfo获取内存信息...");
                CommandResult meminfoResult = execCommand(sshConnectionPoolManager.getOrCreateConnection(hostInfo),
                        "cat /proc/meminfo | grep -E 'MemTotal|MemFree|MemAvailable'");

                if (meminfoResult.isSuccess()
                        && !meminfoResult.getOutput().trim().isEmpty()) {
                    long totalKb = 0;
                    long freeKb = 0;
                    long availableKb = 0;

                    String[] lines = meminfoResult.getOutput().split("\n");
                    for (String line : lines) {
                        if (line.contains("MemTotal")) {
                            String[] memParts = line.split("\\s+");
                            if (memParts.length >= 2) {
                                totalKb = Long.parseLong(memParts[1]);
                            }
                        } else if (line.contains("MemFree")) {
                            String[] memParts = line.split("\\s+");
                            if (memParts.length >= 2) {
                                freeKb = Long.parseLong(memParts[1]);
                            }
                        } else if (line.contains("MemAvailable")) {
                            String[] memParts = line.split("\\s+");
                            if (memParts.length >= 2) {
                                availableKb = Long
                                        .parseLong(memParts[1]);
                            }
                        }
                    }

                    // 如果没有MemAvailable，使用MemFree
                    if (availableKb == 0) {
                        availableKb = freeKb;
                        cacheLog.info("MemAvailable未找到，使用MemFree: " + freeKb + "KB");
                    }

                    totalGB = totalKb / 1024.0 / 1024.0;
                    freeGB = freeKb / 1024.0 / 1024.0;
                    availableGB = availableKb / 1024.0 / 1024.0;
                    usedGB = totalGB - freeGB;
                    memoryParsed = true;

                    cacheLog.info(String.format(
                            "从/proc/meminfo解析: 总内存=%.2fGB, 已用=%.2fGB, 空闲=%.2fGB, 可用=%.2fGB",
                            totalGB, usedGB, freeGB, availableGB));
                }
            }

            // 如果所有解析方法都失败，报错
            if (!memoryParsed) {
                checkItem.setStatus(CheckItem.Status.FAILED);
                checkItem.setMessage("解析内存信息失败: 格式不支持");
                cacheLog.error("解析内存信息失败: 格式不支持");
                return checkItem;
            }

            // 检查是否满足最低内存要求
            boolean memoryCheckPassed = (availableGB >= minMemoryGB);
            cacheLog.info("可用内存检查 " + (memoryCheckPassed ? "通过" : "未通过") + ": "
                    + availableGB + "GB >= " + minMemoryGB + "GB: " + memoryCheckPassed);

            // 构建HTML显示内容
            StringBuilder htmlBuilder = buildMemoryHtml(hostInfo, checkItem,
                    totalGB, usedGB, freeGB, availableGB, memoryCheckPassed,
                    minMemoryGB, recommendedMemoryMB / 1024.0);

            // 设置检查结果
            if (memoryCheckPassed) {
                checkItem.setStatus(CheckItem.Status.SUCCESS);
            } else {
                checkItem.setStatus(CheckItem.Status.FAILED);
            }
            checkItem.setMessage(htmlBuilder.toString());

            cacheLog.info("==== 内存检查完成 ====");
            cacheLog.info("内存检查结果: " + checkItem.getStatus());

            return checkItem;
        } catch (Exception e) {
            logger.error("内存检查失败: {}", e.getMessage(), e);
            cacheLog.error("内存检查失败: " + e.getMessage());
            checkItem.setStatus(CheckItem.Status.FAILED);
            checkItem.setMessage("内存检查失败: " + e.getMessage());
            return checkItem;
        }
    }

    /**
     * 构建内存HTML显示内容
     */
    private StringBuilder buildMemoryHtml(HostInfo hostInfo, CheckItem checkItem, double totalGB, double usedGB,
                                          double freeGB, double availableGB, boolean memoryCheckPassed,
                                          double minMemoryGB, double recommendedMemoryGB) {
        StringBuilder htmlBuilder = new StringBuilder();

        // 内存详情部分
        htmlBuilder.append(HtmlStyleHelper.beginGroup());
        htmlBuilder.append(HtmlStyleHelper.generatePropertyRow("总内存", String.format("%.2f GB", totalGB),
                HtmlStyleHelper.Colors.INFO));
        htmlBuilder.append(HtmlStyleHelper.generatePropertyRow("已用内存", String.format("%.2f GB", usedGB),
                HtmlStyleHelper.Colors.GRAY));
        htmlBuilder.append(HtmlStyleHelper.generatePropertyRow("空闲内存", String.format("%.2f GB", freeGB),
                HtmlStyleHelper.Colors.GRAY));
        htmlBuilder.append(HtmlStyleHelper.generatePropertyRow("可用内存", String.format("%.2f GB", availableGB),
                availableGB < minMemoryGB ? HtmlStyleHelper.Colors.ERROR
                        : availableGB < recommendedMemoryGB ? HtmlStyleHelper.Colors.WARNING
                        : HtmlStyleHelper.Colors.SUCCESS));
        htmlBuilder.append(HtmlStyleHelper.generatePropertyRow("最小要求", String.format("%.2f GB", minMemoryGB),
                HtmlStyleHelper.Colors.INFO));
        htmlBuilder.append(HtmlStyleHelper.generatePropertyRow("建议配置",
                String.format("%.2f GB", recommendedMemoryGB),
                HtmlStyleHelper.Colors.INFO));
        htmlBuilder.append(HtmlStyleHelper.endGroup());

        // 使用率可视化
        double memoryUsagePercent = (usedGB / totalGB) * 100;
        htmlBuilder.append(HtmlStyleHelper.beginGroup());
        htmlBuilder.append("<p><strong>内存使用率: </strong>").append(String.format("%.1f%%", memoryUsagePercent)).append("</p>");
        htmlBuilder.append(HtmlStyleHelper.generateProgressBar((int) memoryUsagePercent,
                memoryUsagePercent > 80 ? "danger"
                        : memoryUsagePercent > 60 ? "warning" : "success",
                String.format("%.1f%%", memoryUsagePercent)));
        htmlBuilder.append(HtmlStyleHelper.endGroup());

        // 检查结论
        if (memoryCheckPassed) {
            htmlBuilder.append(HtmlStyleHelper.generateSuccessAlert("内存检查通过",
                    "系统可用内存充足，可以满足正常运行需求"));
        } else {
            String warningMsg = "系统可用内存为 " +
                    String.format("%.2f GB", availableGB) +
                    "，低于最低要求的 " +
                    String.format("%.2f GB", minMemoryGB) +
                    "，建议增加至少 " +
                    String.format("%.2f GB", recommendedMemoryGB - availableGB) +
                    " 内存以确保系统正常运行。";

            htmlBuilder.append(HtmlStyleHelper.generateWarningAlert("内存检查未通过", warningMsg));
        }

        return htmlBuilder;
    }

    private boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }

        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    protected boolean doFix(HostInfo hostInfo, CheckItem checkItem) {
        try {
            // 获取当前内存状态
            CommandResult memResult = execCommand(sshConnectionPoolManager.getOrCreateConnection(hostInfo), "free -h | grep -E 'Mem:|Swap:'");

            cacheLog.info("==== 内存问题说明 ====");
            cacheLog.info("当前内存情况:");
            if (memResult.isSuccess()) {
                cacheLog.info(memResult.getOutput());
            } else {
                cacheLog.info("无法获取当前内存信息");
            }
            cacheLog.error("内存问题无法自动修复，请手动处理");

            // 创建HTML详细信息构建器
            StringBuilder detailsBuilder = new StringBuilder();

            // 添加问题说明
            detailsBuilder.append(HtmlStyleHelper.beginGroup());
            detailsBuilder.append("<p>内存问题无法自动修复，需要手动处理。以下是一些建议操作：</p>");

            detailsBuilder.append("<ol style='padding-left:20px;margin-bottom:15px'>");
            detailsBuilder.append(
                    "<li style='margin-bottom:5px'><span style='color:#1890ff;font-weight:bold'>增加服务器物理内存</span></li>");
            detailsBuilder.append(
                    "<li style='margin-bottom:5px'><span style='color:#1890ff;font-weight:bold'>关闭不必要的服务以释放内存</span>");
            detailsBuilder.append("<ul style='padding-left:20px;margin-top:5px'>");
            detailsBuilder.append("<li style='color:#333'>使用 ").append(HtmlStyleHelper.generateInlineCode("top")).append(" 或 ").append(HtmlStyleHelper.generateInlineCode("htop")).append(" 命令查看内存占用较高的进程</li>");
            detailsBuilder.append("<li style='color:#333'>使用 ").append(HtmlStyleHelper.generateInlineCode("systemctl stop <服务名>")).append(" 停止非必要服务</li>");
            detailsBuilder.append("</ul></li>");
            detailsBuilder.append(
                    "<li style='margin-bottom:5px'><span style='color:#1890ff;font-weight:bold'>调整应用程序内存使用配置</span></li>");
            detailsBuilder.append("<li style='margin-bottom:5px'><span style='color:#1890ff;font-weight:bold'>清理系统缓存</span>: ").append(HtmlStyleHelper.generateInlineCode(
                    "echo 3 > /proc/sys/vm/drop_caches")).append("</li>");
            detailsBuilder.append("</ol>");
            detailsBuilder.append(HtmlStyleHelper.endGroup());

            // 添加当前内存状态
            if (memResult.isSuccess()) {
                detailsBuilder.append(HtmlStyleHelper.beginGroup());
                detailsBuilder.append("<p><strong>当前内存情况：</strong></p>");
                detailsBuilder.append(HtmlStyleHelper.generateCodeBlock(memResult.getOutput()));
                detailsBuilder.append(HtmlStyleHelper.endGroup());
            }

            // 添加内存分析和清理命令
            detailsBuilder.append(HtmlStyleHelper.beginGroup());
            detailsBuilder.append("<p><strong>内存分析命令：</strong></p>");
            detailsBuilder.append(HtmlStyleHelper.generateCodeBlock(
                    """
                            # 显示进程内存使用情况
                            ps aux --sort=-%mem | head -10
                            
                            # 显示详细内存使用情况
                            free -h
                            
                            # 显示缓存使用情况
                            cat /proc/meminfo | grep -E 'Cache|Buffers'"""));
            detailsBuilder.append(HtmlStyleHelper.endGroup());

            // 添加注意事项
            detailsBuilder.append(HtmlStyleHelper.generateNoteAlert("注意事项",
                    "处理内存问题时，应该先分析原因，确定是临时使用高峰还是持续的资源不足，然后采取相应措施。" +
                            "请确保释放内存前了解相关服务的重要性。"));

            // 设置格式化的HTML消息
            setStyledHtmlMessage(hostInfo, checkItem, false, "内存问题处理建议", detailsBuilder);

            cacheLog.info("==== 该检查项需要手动处理 ====");
        } catch (Exception e) {
            logger.error("生成内存修复建议时出错: ", e);
            cacheLog.error("生成内存修复建议时出错: " + e.getMessage());
            setCheckItemMessage(hostInfo, checkItem, "内存问题需要手动处理，生成建议时出错");
        }

        return false; // 总是返回false表示修复失败，需要手动处理
    }

    @Override
    public ItemCode getCheckerType() {
        return ItemCode.MEMORY;
    }
}