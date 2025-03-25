package com.datasophon.api.service.checker.checkers.memory;

import com.datasophon.api.service.checker.core.AbstractItemChecker;
import com.datasophon.api.service.checker.common.CommandResult;
import com.datasophon.common.model.CheckItem;
import com.datasophon.common.model.HostInfo;
import com.datasophon.api.service.checker.common.ItemCode;
import com.datasophon.api.service.checker.helpers.HtmlStyleHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class MemoryChecker extends AbstractItemChecker {

    private static final Logger logger = LoggerFactory.getLogger(MemoryChecker.class);
    private static final int MIN_AVAILABLE_MEMORY_GB = 16; // 最小可用内存要求

    @Override
    protected CheckItem doCheck(HostInfo hostInfo, CheckItem checkItem) {
        try {
            cacheLog.info("==== 内存检查开始 ====");
            cacheLog.info("主机: " + hostInfo.getHostname());
            cacheLog.info("最小可用内存要求: " + MIN_AVAILABLE_MEMORY_GB + "GB");

            // 更新状态为正在检查内存
            setCheckItemMessage(hostInfo, checkItem, "正在检查内存情况...");

            // 获取内存信息，使用 free -m 命令以获取更精确的内存信息（MB级别）
            cacheLog.info("检查内存情况...");
            CommandResult memResult = execCommand(session, "free -m | grep Mem:");

            if (!memResult.isSuccess()) {
                cacheLog.error("获取内存信息失败: %s", memResult.getErrorOrOutput());
                checkItem.setStatus(CheckItem.Status.FAILED);
                setCheckItemMessage(hostInfo, checkItem, "获取内存信息失败: " + memResult.getErrorOrOutput());
                return checkItem;
            }

            // 解析内存信息
            String[] memInfo = memResult.getOutput().trim().split("\\s+");
            int totalMemMB = Integer.parseInt(memInfo[1]);
            int usedMemMB = Integer.parseInt(memInfo[2]);
            int freeMemMB = Integer.parseInt(memInfo[3]);

            // 计算精确到1位小数的GB值
            double totalMemGB = totalMemMB / 1024.0;
            double usedMemGB = usedMemMB / 1024.0;
            double freeMemGB = freeMemMB / 1024.0;

            // 格式化为1位小数
            String totalMemGBStr = String.format("%.1f", totalMemGB);
            String usedMemGBStr = String.format("%.1f", usedMemGB);
            String freeMemGBStr = String.format("%.1f", freeMemGB);

            // 计算内存使用率百分比
            int memUsagePercent = (int) Math.round((usedMemMB * 100.0) / totalMemMB);

            // 还需要检查available列的内存数据，这是实际可用内存
            int availableMemMB = -1;
            if (memInfo.length >= 7) {
                availableMemMB = Integer.parseInt(memInfo[6]);
            } else {
                // 如果没有available列，使用free + buffers/cache的值
                availableMemMB = freeMemMB;
                if (memInfo.length >= 6) { // 有buffers/cache列
                    availableMemMB += Integer.parseInt(memInfo[5]); // buffers/cache
                }
            }

            double availableMemGB = availableMemMB / 1024.0;
            String availableMemGBStr = String.format("%.1f", availableMemGB);

            cacheLog.info(String.format("总内存: %.1fGB, 已用: %.1fGB, 空闲: %.1fGB, 可用: %.1fGB",
                    totalMemGB, usedMemGB, freeMemGB, availableMemGB));

            // 更新状态为正在检查swap使用情况
            setCheckItemMessage(hostInfo, checkItem, "正在检查swap使用情况...");

            // 获取swap使用情况
            cacheLog.info("检查swap使用情况...");
            CommandResult swapResult = execCommand(session, "free -m | grep Swap:");

            if (!swapResult.isSuccess()) {
                cacheLog.error("获取swap信息失败: %s", swapResult.getErrorOrOutput());
                checkItem.setStatus(CheckItem.Status.FAILED);
                setCheckItemMessage(hostInfo, checkItem, "获取swap信息失败: " + swapResult.getErrorOrOutput());
                return checkItem;
            }

            String[] swapInfo = swapResult.getOutput().trim().split("\\s+");
            int totalSwapMB = Integer.parseInt(swapInfo[1]);
            int usedSwapMB = Integer.parseInt(swapInfo[2]);

            double totalSwapGB = totalSwapMB / 1024.0;
            double usedSwapGB = usedSwapMB / 1024.0;

            String totalSwapGBStr = String.format("%.1f", totalSwapGB);
            String usedSwapGBStr = String.format("%.1f", usedSwapGB);

            // 计算Swap使用率百分比
            int swapUsagePercent = totalSwapMB > 0 ? (int) Math.round((usedSwapMB * 100.0) / totalSwapMB) : 0;

            cacheLog.info(String.format("Swap总量: %.1fGB, 已用: %.1fGB", totalSwapGB, usedSwapGB));

            // 更新状态为正在分析内存状态
            setCheckItemMessage(hostInfo, checkItem, "正在分析内存状态...");

            // 检查结果 - 主要检查可用内存是否足够
            boolean availableMemorySufficient = availableMemGB >= MIN_AVAILABLE_MEMORY_GB;
            boolean swapUsageNormal = totalSwapMB == 0 || (double) usedSwapMB / totalSwapMB <= 0.5; // swap使用率阈值50%

            if (availableMemorySufficient && swapUsageNormal) {
                checkItem.setStatus(CheckItem.Status.SUCCESS);

                // 创建HTML详细信息构建器
                StringBuilder detailsBuilder = new StringBuilder();

                // 添加内存信息组
                detailsBuilder.append(HtmlStyleHelper.beginGroup());

                // 内存总量和可用内存
                detailsBuilder.append(HtmlStyleHelper.generatePropertyRowWithThreshold(
                        "可用内存", availableMemGBStr + "GB", HtmlStyleHelper.Colors.SUCCESS,
                        MIN_AVAILABLE_MEMORY_GB, "GB"));
                detailsBuilder.append(HtmlStyleHelper.generatePropertyRow(
                        "总内存", totalMemGBStr + "GB", HtmlStyleHelper.Colors.INFO));

                // 内存使用详情
                detailsBuilder.append(HtmlStyleHelper.generatePropertyRow(
                        "已用内存", usedMemGBStr + "GB", HtmlStyleHelper.Colors.ORANGE));
                detailsBuilder.append(HtmlStyleHelper.generatePropertyRow(
                        "空闲内存", freeMemGBStr + "GB", HtmlStyleHelper.Colors.CYAN));

                // 内存使用率进度条
                detailsBuilder.append("<p><strong>内存使用率:</strong></p>");
                String memUsageColor = memUsagePercent < 70 ? HtmlStyleHelper.Colors.SUCCESS
                        : (memUsagePercent < 90 ? HtmlStyleHelper.Colors.WARNING : HtmlStyleHelper.Colors.ERROR);
                detailsBuilder.append(HtmlStyleHelper.generateProgressBar(
                        memUsagePercent, memUsageColor, memUsagePercent + "%"));

                // Swap信息
                detailsBuilder.append(HtmlStyleHelper.generatePropertyRow(
                        "Swap总量", totalSwapGBStr + "GB", HtmlStyleHelper.Colors.BLUE));
                detailsBuilder.append(HtmlStyleHelper.generatePropertyRow(
                        "Swap已用", usedSwapGBStr + "GB", HtmlStyleHelper.Colors.PINK));

                // Swap使用率进度条（只有在Swap总量大于0时才显示）
                if (totalSwapMB > 0) {
                    detailsBuilder.append("<p><strong>Swap使用率:</strong></p>");
                    String swapUsageColor = swapUsagePercent < 30 ? HtmlStyleHelper.Colors.SUCCESS
                            : (swapUsagePercent < 50 ? HtmlStyleHelper.Colors.WARNING : HtmlStyleHelper.Colors.ERROR);
                    detailsBuilder.append(HtmlStyleHelper.generateProgressBar(
                            swapUsagePercent, swapUsageColor, swapUsagePercent + "%"));
                }

                detailsBuilder.append(HtmlStyleHelper.endGroup());

                // 添加成功信息提示
                detailsBuilder.append(HtmlStyleHelper.generateSuccessAlert(
                        "内存检查通过",
                        "系统内存配置充足，可以正常运行系统和应用程序。"));

                // 设置格式化的HTML消息
                setStyledHtmlMessage(hostInfo, checkItem, true, "内存配置检查通过", detailsBuilder);

                cacheLog.info("内存检查通过");
            } else {
                checkItem.setStatus(CheckItem.Status.FAILED);

                // 创建HTML详细信息构建器
                StringBuilder detailsBuilder = new StringBuilder();

                // 添加内存信息组
                detailsBuilder.append(HtmlStyleHelper.beginGroup());

                // 内存总量和可用内存
                String availableMemColor = availableMemorySufficient ? HtmlStyleHelper.Colors.SUCCESS
                        : HtmlStyleHelper.Colors.ERROR;

                detailsBuilder.append(HtmlStyleHelper.generatePropertyRowWithThreshold(
                        "可用内存", availableMemGBStr + "GB", availableMemColor,
                        MIN_AVAILABLE_MEMORY_GB, "GB"));
                detailsBuilder.append(HtmlStyleHelper.generatePropertyRow(
                        "总内存", totalMemGBStr + "GB", HtmlStyleHelper.Colors.INFO));

                // 内存使用详情
                detailsBuilder.append(HtmlStyleHelper.generatePropertyRow(
                        "已用内存", usedMemGBStr + "GB", HtmlStyleHelper.Colors.ORANGE));
                detailsBuilder.append(HtmlStyleHelper.generatePropertyRow(
                        "空闲内存", freeMemGBStr + "GB", HtmlStyleHelper.Colors.CYAN));

                // 内存使用率进度条
                detailsBuilder.append("<p><strong>内存使用率:</strong></p>");
                String memUsageColor = memUsagePercent < 70 ? HtmlStyleHelper.Colors.SUCCESS
                        : (memUsagePercent < 90 ? HtmlStyleHelper.Colors.WARNING : HtmlStyleHelper.Colors.ERROR);
                detailsBuilder.append(HtmlStyleHelper.generateProgressBar(
                        memUsagePercent, memUsageColor, memUsagePercent + "%"));

                // Swap信息
                String swapColor = swapUsageNormal ? HtmlStyleHelper.Colors.PINK : HtmlStyleHelper.Colors.ERROR;
                detailsBuilder.append(HtmlStyleHelper.generatePropertyRow(
                        "Swap总量", totalSwapGBStr + "GB", HtmlStyleHelper.Colors.BLUE));
                detailsBuilder.append(HtmlStyleHelper.generatePropertyRow(
                        "Swap已用", usedSwapGBStr + "GB", swapColor));

                // Swap使用率进度条（只有在Swap总量大于0时才显示）
                if (totalSwapMB > 0) {
                    detailsBuilder.append("<p><strong>Swap使用率:</strong></p>");
                    String swapUsageColor = swapUsagePercent < 30 ? HtmlStyleHelper.Colors.SUCCESS
                            : (swapUsagePercent < 50 ? HtmlStyleHelper.Colors.WARNING : HtmlStyleHelper.Colors.ERROR);
                    detailsBuilder.append(HtmlStyleHelper.generateProgressBar(
                            swapUsagePercent, swapUsageColor, swapUsagePercent + "%"));
                }

                detailsBuilder.append(HtmlStyleHelper.endGroup());

                // 添加警告信息
                StringBuilder warningMsg = new StringBuilder();
                if (!availableMemorySufficient) {
                    warningMsg.append(String.format("可用内存(%.1fGB)小于最低要求(%dGB)<br>",
                            availableMemGB, MIN_AVAILABLE_MEMORY_GB));
                }
                if (!swapUsageNormal) {
                    warningMsg.append(String.format("Swap使用率(%.1f%%)过高<br>",
                            swapUsagePercent));
                }

                detailsBuilder.append(HtmlStyleHelper.generateWarningAlert(
                        "内存检查未通过", warningMsg.toString()));

                // 设置格式化的HTML消息
                setStyledHtmlMessage(hostInfo, checkItem, false, "内存配置检查未通过", detailsBuilder);

                cacheLog.info("内存检查未通过: " + warningMsg);
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
        try {
            // 获取当前内存状态
            CommandResult memResult = execCommand(session, "free -h | grep -E 'Mem:|Swap:'");

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
            detailsBuilder.append("<li style='color:#333'>使用 " + HtmlStyleHelper.generateInlineCode("top") +
                    " 或 " + HtmlStyleHelper.generateInlineCode("htop") + " 命令查看内存占用较高的进程</li>");
            detailsBuilder
                    .append("<li style='color:#333'>使用 " + HtmlStyleHelper.generateInlineCode("systemctl stop <服务名>") +
                            " 停止非必要服务</li>");
            detailsBuilder.append("</ul></li>");
            detailsBuilder.append(
                    "<li style='margin-bottom:5px'><span style='color:#1890ff;font-weight:bold'>调整应用程序内存使用配置</span></li>");
            detailsBuilder.append(
                    "<li style='margin-bottom:5px'><span style='color:#1890ff;font-weight:bold'>清理系统缓存</span>: " +
                            HtmlStyleHelper.generateInlineCode("echo 3 > /proc/sys/vm/drop_caches") + "</li>");
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
                    "# 显示进程内存使用情况\nps aux --sort=-%mem | head -10\n\n" +
                            "# 显示详细内存使用情况\nfree -h\n\n" +
                            "# 显示缓存使用情况\ncat /proc/meminfo | grep -E 'Cache|Buffers'"));
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