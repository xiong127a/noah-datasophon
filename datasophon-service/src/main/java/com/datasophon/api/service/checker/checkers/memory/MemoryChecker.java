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

@Component
public class MemoryChecker extends AbstractItemChecker {

        private static final Logger logger = LoggerFactory.getLogger(MemoryChecker.class);
        private static final int MIN_AVAILABLE_MEMORY_GB = 16; // 最小可用内存要求

        @Override
        protected CheckItem doCheck(HostInfo hostInfo, CheckItem checkItem) {
                try {
                        cacheLog.info("==== 内存检查开始 ====");
                        cacheLog.info("主机: " + hostInfo.getIp());
                        cacheLog.info("最小可用内存要求: " + MIN_AVAILABLE_MEMORY_GB + "GB");

                        // 更新状态为正在检查内存
                        setCheckItemMessage(hostInfo, checkItem, "正在检查内存情况...");

                        // 执行free命令获取内存信息
                        cacheLog.debug("执行free命令获取内存信息");
                        cacheLog.info("正在获取内存信息...");
                        CommandResult result = execCommand(session, "free -m | grep -E 'Mem|内存'");

                        if (!result.isSuccess() || result.getOutput().trim().isEmpty()) {
                                cacheLog.warn("free命令失败或输出为空，尝试使用另一种格式...");
                                // 尝试只运行free命令，然后处理第二行（通常是内存行）
                                result = execCommand(session, "free -m | sed -n '2p'");

                                if (!result.isSuccess() || result.getOutput().trim().isEmpty()) {
                                        cacheLog.warn("free命令失败，尝试使用/proc/meminfo获取内存信息...");
                                        CommandResult meminfoResult = execCommand(session,
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
                                                boolean memoryCheckPassed = (availableGB >= MIN_AVAILABLE_MEMORY_GB);
                                                cacheLog.info("可用内存检查 " + (memoryCheckPassed ? "通过" : "未通过") + ": "
                                                                + availableGB + "GB >= " + MIN_AVAILABLE_MEMORY_GB
                                                                + "GB: "
                                                                + memoryCheckPassed);

                                                // 构建HTML显示内容
                                                StringBuilder htmlBuilder = buildMemoryHtml(hostInfo, checkItem,
                                                                totalGB, usedGB, freeGB, availableGB,
                                                                memoryCheckPassed);

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
                                        } else {
                                                checkItem.setStatus(CheckItem.Status.FAILED);
                                                checkItem.setMessage("无法获取内存信息: 所有方法都失败");
                                                cacheLog.error("无法获取内存信息: 所有方法都失败");
                                                return checkItem;
                                        }
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
                                CommandResult meminfoResult = execCommand(session,
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

                        // 格式化为1位小数
                        String totalMemGBStr = String.format("%.1f", totalGB);
                        String usedMemGBStr = String.format("%.1f", usedGB);
                        String freeMemGBStr = String.format("%.1f", freeGB);
                        String availableMemGBStr = String.format("%.1f", availableGB);

                        // 计算内存使用率百分比
                        int memUsagePercent = (int) Math.round((usedGB * 100.0) / totalGB);

                        cacheLog.info(String.format("总内存: %.1fGB, 已用: %.1fGB, 空闲: %.1fGB, 可用: %.1fGB",
                                        totalGB, usedGB, freeGB, availableGB));

                        // 更新状态为正在检查swap使用情况
                        setCheckItemMessage(hostInfo, checkItem, "正在检查swap使用情况...");

                        // 获取swap使用情况
                        cacheLog.info("检查swap使用情况...");
                        CommandResult swapResult = execCommand(session, "free -m | grep -E 'Swap|交换'");

                        double totalSwapGB = 0.0;
                        double usedSwapGB = 0.0;
                        boolean swapParsed = false;

                        // 尝试处理Swap信息
                        try {
                                if (swapResult.isSuccess() && !swapResult.getOutput().trim().isEmpty()) {
                                        String[] swapParts = swapResult.getOutput().trim().split("\\s+");

                                        // 识别Swap行格式
                                        int swapStartIdx = 0;
                                        // 跳过非数字列，如"Swap:"或"交换："
                                        while (swapStartIdx < swapParts.length && !isNumeric(swapParts[swapStartIdx])) {
                                                swapStartIdx++;
                                        }

                                        if (swapStartIdx < swapParts.length && swapStartIdx + 2 < swapParts.length) {
                                                int totalSwapMB = Integer.parseInt(swapParts[swapStartIdx]);
                                                int usedSwapMB = Integer.parseInt(swapParts[swapStartIdx + 1]);

                                                totalSwapGB = totalSwapMB / 1024.0;
                                                usedSwapGB = usedSwapMB / 1024.0;
                                                swapParsed = true;

                                                cacheLog.info(String.format("成功解析Swap信息: 总Swap=%.2fGB, 已用=%.2fGB",
                                                                totalSwapGB, usedSwapGB));
                                        }
                                }
                        } catch (Exception e) {
                                cacheLog.warn("解析Swap信息失败: " + e.getMessage());
                        }

                        // 如果Swap解析失败，尝试从/proc/swaps获取
                        if (!swapParsed) {
                                try {
                                        CommandResult swapsResult = execCommand(session,
                                                        "cat /proc/swaps | grep -v Filename");
                                        if (swapsResult.isSuccess() && !swapsResult.getOutput().trim().isEmpty()) {
                                                // 有swap分区
                                                totalSwapGB = 0.0;
                                                usedSwapGB = 0.0;

                                                String[] swapLines = swapsResult.getOutput().trim().split("\n");
                                                for (String line : swapLines) {
                                                        String[] swapInfo = line.trim().split("\\s+");
                                                        if (swapInfo.length >= 4) {
                                                                try {
                                                                        double swapSizeKB = Double
                                                                                        .parseDouble(swapInfo[2]);
                                                                        double swapUsedKB = Double
                                                                                        .parseDouble(swapInfo[3]);

                                                                        totalSwapGB += swapSizeKB / 1024.0 / 1024.0;
                                                                        usedSwapGB += swapUsedKB / 1024.0 / 1024.0;
                                                                } catch (NumberFormatException e) {
                                                                        cacheLog.warn("解析/proc/swaps行失败: " + line);
                                                                }
                                                        }
                                                }
                                                swapParsed = true;
                                                cacheLog.info(String.format("从/proc/swaps解析: 总Swap=%.2fGB, 已用=%.2fGB",
                                                                totalSwapGB, usedSwapGB));
                                        } else {
                                                // 无swap分区
                                                totalSwapGB = 0.0;
                                                usedSwapGB = 0.0;
                                                swapParsed = true;
                                                cacheLog.info("系统未配置Swap分区");
                                        }
                                } catch (Exception e) {
                                        cacheLog.warn("检查/proc/swaps失败: " + e.getMessage());
                                }
                        }

                        String totalSwapGBStr = String.format("%.1f", totalSwapGB);
                        String usedSwapGBStr = String.format("%.1f", usedSwapGB);

                        // 计算Swap使用率百分比
                        int swapUsagePercent = totalSwapGB > 0 ? (int) Math.round((usedSwapGB * 100.0) / totalSwapGB)
                                        : 0;

                        cacheLog.info(String.format("Swap总量: %.1fGB, 已用: %.1fGB", totalSwapGB, usedSwapGB));

                        // 更新状态为正在分析内存状态
                        setCheckItemMessage(hostInfo, checkItem, "正在分析内存状态...");

                        // 检查结果 - 主要检查可用内存是否足够
                        boolean availableMemorySufficient = availableGB >= MIN_AVAILABLE_MEMORY_GB;
                        boolean swapUsageNormal = totalSwapGB == 0 || (usedSwapGB / totalSwapGB <= 0.5); // swap使用率阈值50%

                        // 最终检查结果
                        boolean memoryCheckPassed = availableMemorySufficient; // && swapUsageNormal;

                        // 构建HTML显示
                        StringBuilder htmlBuilder = buildMemoryHtml(hostInfo, checkItem, totalGB, usedGB, freeGB,
                                        availableGB, memoryCheckPassed);

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
                        checkItem.setStatus(CheckItem.Status.FAILED);
                        checkItem.setMessage("处理内存信息时出错: " + e.getMessage());
                        cacheLog.error("处理内存信息时出错: %s", e.getMessage());
                        return checkItem;
                }
        }

        // 构建内存检查HTML显示
        private StringBuilder buildMemoryHtml(HostInfo hostInfo, CheckItem checkItem, double totalGB, double usedGB,
                        double freeGB, double availableGB, boolean memoryCheckPassed) {
                StringBuilder htmlBuilder = new StringBuilder();
                htmlBuilder.append("<div style='line-height:1.6'>");

                if (memoryCheckPassed) {
                        htmlBuilder.append(
                                        "<h3 style='color:#52c41a;margin-bottom:10px'>内存配置检查通过</h3>");
                } else {
                        htmlBuilder.append(
                                        "<h3 style='color:#f5222d;margin-bottom:10px'>内存配置检查未通过</h3>");
                }

                // 主机信息部分
                htmlBuilder.append("<div style='margin-bottom:15px'>");
                htmlBuilder.append("<p><strong>主机:</strong> <span style='color:#1890ff;font-weight:bold'>"
                                + hostInfo.getIp() + "</span></p>");
                htmlBuilder.append("<p><strong>IP地址:</strong> <span style='color:#1890ff;font-weight:bold'>"
                                + hostInfo.getIp() + "</span></p>");
                htmlBuilder.append("<p><strong>检查时间:</strong> <span style='color:#8c8c8c;font-weight:bold'>"
                                + getCurrentTime() + "</span></p>");
                htmlBuilder.append("</div>");

                // 内存详情部分
                htmlBuilder.append("<div style='margin-bottom:15px'>");
                htmlBuilder.append("<p><strong>总内存:</strong> <span style='color:#1890ff;font-weight:bold'>"
                                + String.format("%.2f", totalGB) + "GB</span></p>");
                htmlBuilder.append("<p><strong>已用内存:</strong> <span style='color:"
                                + (usedGB / totalGB > 0.8 ? "#f5222d" : "#52c41a") + ";font-weight:bold'>"
                                + String.format("%.2f", usedGB) + "GB ("
                                + String.format("%.1f", (usedGB * 100.0 / totalGB)) + "%)</span></p>");

                htmlBuilder.append("<p><strong>可用内存:</strong> <span style='color:"
                                + (availableGB >= MIN_AVAILABLE_MEMORY_GB ? "#52c41a" : "#f5222d")
                                + ";font-weight:bold'>" + String.format("%.2f", availableGB)
                                + "GB</span> (最低要求: <span style='color:#1890ff;font-weight:bold'>"
                                + MIN_AVAILABLE_MEMORY_GB + "GB</span>)</p>");

                htmlBuilder.append("<p><strong>内存占用率:</strong></p>");
                int memUsagePercent = (int) Math.round((usedGB * 100.0) / totalGB);
                htmlBuilder.append(HtmlStyleHelper.createProgressBar(memUsagePercent, memUsagePercent > 80));
                htmlBuilder.append("</div>");

                // 结果总结部分
                if (memoryCheckPassed) {
                        htmlBuilder.append(
                                        "<div style='background:#f6ffed;border-left:4px solid #52c41a;padding:10px;border-radius:0 4px 4px 0;margin-top:10px'>");
                        htmlBuilder.append(
                                        "<p style='margin:0;color:#52c41a;font-weight:bold'>内存检查通过</p>");
                        htmlBuilder.append(
                                        "<p style='margin-top:5px;margin-bottom:0;'>内存配置充足，可以正常运行系统和应用程序。可用内存 "
                                                        + String.format("%.2f", availableGB) + "GB 满足最低要求 "
                                                        + MIN_AVAILABLE_MEMORY_GB + "GB。</p>");
                } else {
                        htmlBuilder.append(
                                        "<div style='background:#fff2f0;border-left:4px solid #f5222d;padding:10px;border-radius:0 4px 4px 0;margin-top:10px'>");
                        htmlBuilder.append(
                                        "<p style='margin:0;color:#f5222d;font-weight:bold'>内存检查未通过</p>");
                        htmlBuilder.append("<p style='margin-top:5px;margin-bottom:0;'>可用内存不足。系统当前可用内存为 "
                                        + String.format("%.2f", availableGB) + "GB，不满足最低要求 "
                                        + MIN_AVAILABLE_MEMORY_GB + "GB。</p>");
                }
                htmlBuilder.append("</div>");

                htmlBuilder.append("</div>");

                return htmlBuilder;
        }

        // 辅助方法：检查字符串是否可以解析为数字
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
                                        .append("<li style='color:#333'>使用 "
                                                        + HtmlStyleHelper.generateInlineCode("systemctl stop <服务名>") +
                                                        " 停止非必要服务</li>");
                        detailsBuilder.append("</ul></li>");
                        detailsBuilder.append(
                                        "<li style='margin-bottom:5px'><span style='color:#1890ff;font-weight:bold'>调整应用程序内存使用配置</span></li>");
                        detailsBuilder.append(
                                        "<li style='margin-bottom:5px'><span style='color:#1890ff;font-weight:bold'>清理系统缓存</span>: "
                                                        +
                                                        HtmlStyleHelper.generateInlineCode(
                                                                        "echo 3 > /proc/sys/vm/drop_caches")
                                                        + "</li>");
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