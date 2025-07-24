package com.datasophon.api.service.checker.checkers.disk.generic;

import com.datasophon.api.config.CheckerProperties;
import com.datasophon.api.service.checker.checkers.disk.DiskChecker;
import com.datasophon.api.service.checker.checkers.disk.DiskCheckerStrategy;
import com.datasophon.api.service.checker.common.CommandResult;
import com.datasophon.api.service.checker.common.SshConnectionPoolManager;
import com.datasophon.api.service.checker.helpers.CheckLogger;
import com.datasophon.common.enums.OsDistribution;
import com.datasophon.common.model.CheckItem;
import com.datasophon.common.model.HostInfo;
import lombok.Setter;
import org.apache.sshd.client.session.ClientSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

/**
 * 通用磁盘检查器实现
 * 适用于所有Linux发行版的基本磁盘检查
 */

public class GenericDiskChecker implements DiskCheckerStrategy {

        private static final Logger log = LoggerFactory.getLogger(GenericDiskChecker.class);

        // 注入SSH连接池管理器
        @Autowired
        protected SshConnectionPoolManager sshConnectionPoolManager;

        // 磁盘检查器实例，用于执行命令
        @Autowired
        protected DiskChecker diskChecker;

        /**
         * 支持的操作系统类型
         */

        @Setter
        private OsDistribution supportedOs;

        public GenericDiskChecker() {
                // 初始化磁盘空间要求配置
        }

        @Override
        public CheckItem check(HostInfo hostInfo, CheckItem checkItem, CheckLogger cacheLog)
                        throws InterruptedException {
                cacheLog.info("==== 通用磁盘检查开始 ====");

                List<CheckerProperties.DiskDirectoryConfig> directories = diskChecker.getCheckDirectories();
                if (directories == null || directories.isEmpty()) {
                        cacheLog.warn("未配置检查目录，磁盘检查将跳过");
                        checkItem.setStatus(CheckItem.Status.SUCCESS);
                        checkItem.setMessage("未配置检查目录，磁盘检查跳过");
                        return checkItem;
                }

                // 记录检查结果
                List<String> failedDirectories = new ArrayList<>();
                StringBuilder resultMessage = new StringBuilder();
                resultMessage.append("<div style='line-height:1.6'>");

                for (CheckerProperties.DiskDirectoryConfig dirConfig : directories) {
                        String dir = dirConfig.getPath();
                        int minDiskSpaceGB = dirConfig.getMinAvailableGb();

                        cacheLog.info("检查目录: {}, 最小所需空间: {}GB", dir, minDiskSpaceGB);

                        // 打开SSH会话
                        ClientSession session = openSession(hostInfo);
                        if (session == null) {
                                String errorMsg = "无法连接到主机: " + hostInfo.getHostname();
                                log.error(errorMsg);
                                cacheLog.error(errorMsg);
                                checkItem.setStatus(CheckItem.Status.FAILED);
                                checkItem.setMessage(errorMsg);
                                return checkItem;
                        }

                        // 获取磁盘信息
                        String command = "df -h " + dir;
                        CommandResult dfResult = execCommand(session, command);

                        // 处理检查结果
                        CheckItem dirCheckResult = processDfResult(hostInfo, new CheckItem(), dfResult,
                                        cacheLog, dir, minDiskSpaceGB);

                        if (dirCheckResult.getStatus() == CheckItem.Status.FAILED) {
                                failedDirectories.add(dir);
                        }
                    resultMessage.append(dirCheckResult.getMessage());
                    resultMessage.append(
                                    "<hr style='border:none;height:1px;background-color:#f0f0f0;margin:20px 0'>");
                }

                resultMessage.append("</div>");

                // 设置最终结果
                if (!failedDirectories.isEmpty()) {
                        checkItem.setStatus(CheckItem.Status.FAILED);
                        cacheLog.error("磁盘检查失败，以下目录空间不足: {}", String.join(", ", failedDirectories));
                } else {
                        checkItem.setStatus(CheckItem.Status.SUCCESS);
                        cacheLog.info("所有目录磁盘空间检查通过");
                }

                checkItem.setMessage(resultMessage.toString());
                return checkItem;
        }

        /**
         * 执行命令的辅助方法
         */
        protected CommandResult execCommand(ClientSession session, String command) {
                log.debug("执行命令: {}", command);

                if (session == null) {
                        log.error("SSH会话为空，无法执行命令");
                        return new CommandResult("", "SSH会话为空", -1);
                }

                if (diskChecker != null) {
                        return diskChecker.execCommand(session, command);
                } else {
                        // 如果没有diskChecker，返回错误结果
                        return new CommandResult("", "磁盘检查器未初始化", -1);
                }
        }

        /**
         * 处理df命令结果的辅助方法
         */
        protected CheckItem processDfResult(HostInfo hostInfo, CheckItem checkItem, CommandResult dfResult,
                        CheckLogger cacheLog, String targetDir, int minDiskSpaceGB) {
                if (dfResult.getExitCode() != 0) {
                        String errorMsg = "获取磁盘信息失败: " + dfResult.getError();
                        log.error(errorMsg);
                        cacheLog.error(errorMsg);
                        checkItem.setStatus(CheckItem.Status.FAILED);
                        checkItem.setMessage(errorMsg);
                        return checkItem;
                }

                String output = dfResult.getOutput();
                String[] lines = output.split("\n");

                if (lines.length < 2) {
                        String errorMsg = "无法解析磁盘信息: " + output;
                        log.error(errorMsg);
                        cacheLog.error(errorMsg);
                        checkItem.setStatus(CheckItem.Status.FAILED);
                        checkItem.setMessage(errorMsg);
                        return checkItem;
                }

                cacheLog.info("df命令输出:\n%s", output);

                // 找到包含目标目录的行
                String targetLine = null;
                for (int i = 1; i < lines.length; i++) {
                        String line = lines[i].trim();
                        String[] parts = line.split("\\s+");
                        if (parts.length < 6) {
                                continue;
                        }

                        // 获取挂载点
                        String mountPoint = parts[5];

                        // 检查是否为目标目录或其父目录
                        if (targetDir.equals(mountPoint) || // 直接匹配
                                        targetDir.startsWith(mountPoint + "/") || // 是父目录
                                        mountPoint.equals("/")) { // 根目录是所有目录的父目录

                                // 如果找到多个匹配，优先使用最具体的挂载点
                                if (targetLine == null || parts[5].length() > targetLine.split("\\s+")[5].length()) {
                                        targetLine = line;
                                }
                        }
                }

                // 如果没有找到挂载点，报错
                if (targetLine == null) {
                        String errorMsg = "在df输出中未找到" + targetDir + "目录所在的分区";
                        log.error(errorMsg);
                        cacheLog.error(errorMsg);
                        checkItem.setStatus(CheckItem.Status.FAILED);
                        checkItem.setMessage(errorMsg);
                        return checkItem;
                }

                // 解析找到的行
                String[] parts = targetLine.split("\\s+");
                String mountPoint = parts[5];
                String device = parts[0];

                String usageStr = parts[4].replace("%", "");
                try {
                        int usage = Integer.parseInt(usageStr);
                        String size = parts[1];
                        String available = parts[3];

                        log.info("检测到 {} 目录所在分区: 设备={}, 挂载点={}, 总大小={}, 可用空间={}, 使用率={}%",
                                        targetDir, device, mountPoint, size, available, usage);
                        cacheLog.info(String.format("检测到 %s 目录所在分区: 设备=%s, 挂载点=%s, 总大小=%s, 可用空间=%s, 使用率=%d%%",
                                        targetDir, device, mountPoint, size, available, usage));

                        // 检查磁盘使用率
                        if (usage > DiskChecker.WARNING_DISK_USAGE_THRESHOLD) {
                                // 创建带HTML样式的消息
                                StringBuilder message = new StringBuilder();
                                message.append("<div style='line-height:1.6'>");

                                // 状态标题
                                message.append(String.format(
                                                "<h3 style='color:#f5222d;margin-bottom:10px'>%s 目录磁盘空间不足</h3>",
                                                targetDir));

                                // 磁盘详情
                                message.append("<div style='margin-bottom:15px'>");
                                message.append(String.format(
                                                "<p><strong>挂载点:</strong> <span style='color:#1890ff'>%s</span></p>",
                                                mountPoint));
                                message.append(
                                                String.format("<p><strong>设备:</strong> <span style='color:#722ed1'>%s</span></p>",
                                                                device));
                                message.append(String.format(
                                                "<p><strong>总空间:</strong> <span style='color:#1890ff;font-weight:bold'>%s</span></p>",
                                                size));
                                message.append(String.format(
                                                "<p><strong>可用空间:</strong> <span style='color:#f5222d;font-weight:bold'>%s</span> (阈值: <span style='color:#722ed1;font-weight:bold'>%dGB</span>)</p>",
                                                available, minDiskSpaceGB));
                                message.append("</div>");

                                // 磁盘空间可视化
                                message.append("<div style='margin-bottom:15px'>");
                                message.append("<p><strong>磁盘可用空间:</strong></p>");

                                // 计算百分比
                                float percentAvailable = (Float.parseFloat(available.replace("G", "")) / minDiskSpaceGB)
                                                * 100;
                                if (percentAvailable > 100)
                                        percentAvailable = 100;

                                message.append(
                                                "<div style='background:#f0f0f0;border-radius:8px;height:20px;width:100%;position:relative;overflow:hidden;'>");
                                message.append(String.format(
                                                "<div style='background:#f5222d;height:100%%;width:%d%%;border-radius:8px;'></div>",
                                                Math.round(percentAvailable)));
                                message.append(String.format(
                                                "<div style='position:absolute;top:0;left:0;right:0;bottom:0;text-align:center;color:%s;line-height:20px;font-weight:bold;'>%.2f GB / %d GB</div>",
                                                percentAvailable > 70 ? "white" : "#333",
                                                Float.parseFloat(available.replace("G", "")),
                                                minDiskSpaceGB));
                                message.append("</div>");
                                message.append("</div>");

                                // 警告信息
                                message.append(
                                                "<div style='background:#fff2f0;border-left:4px solid #f5222d;padding:10px;border-radius:0 4px 4px 0;'>");
                                message.append(String.format(
                                                "<p style='margin:0;color:#f5222d;font-weight:bold'>警告: 磁盘可用空间(%.2f GB)小于最低要求(%d GB)</p>",
                                                Float.parseFloat(available.replace("G", "")), minDiskSpaceGB));
                                message.append("<p style='margin-top:5px;margin-bottom:0;'>建议清理磁盘空间或扩展存储容量，以确保系统正常运行。</p>");
                                message.append("</div>");

                                message.append("</div>");

                                String errorMsg = String.format("磁盘可用空间不足: %.2f GB < %d GB",
                                                Float.parseFloat(available.replace("G", "")), minDiskSpaceGB);
                                log.error(errorMsg);
                                cacheLog.error(errorMsg);
                                checkItem.setStatus(CheckItem.Status.FAILED);
                                checkItem.setMessage(message.toString());
                                return checkItem;
                        }

                        // 检查可用空间
                        available = available.toUpperCase();
                        float availableGB;
                        if (available.endsWith("G")) {
                                availableGB = Float.parseFloat(available.substring(0, available.length() - 1));
                        } else if (available.endsWith("T")) {
                                availableGB = Float.parseFloat(available.substring(0, available.length() - 1)) * 1024;
                        } else if (available.endsWith("M")) {
                                availableGB = Float.parseFloat(available.substring(0, available.length() - 1)) / 1024;
                        } else {
                                String errorMsg = "无法解析可用空间单位: " + available;
                                log.error(errorMsg);
                                cacheLog.error(errorMsg);
                                checkItem.setStatus(CheckItem.Status.FAILED);
                                checkItem.setMessage(errorMsg);
                                return checkItem;
                        }

                        // 获取全局最小可用空间百分比
                        int minAvailablePercent = getMinAvailablePercent();

                        if (availableGB < minDiskSpaceGB) {
                                // 创建带HTML样式的消息
                                StringBuilder message = new StringBuilder();
                                message.append("<div style='line-height:1.6'>");

                                // 状态标题
                                message.append(String.format(
                                                "<h3 style='color:#f5222d;margin-bottom:10px'>%s 目录磁盘可用空间不足</h3>",
                                                targetDir));

                                // 磁盘详情
                                message.append("<div style='margin-bottom:15px'>");
                                message.append(String.format(
                                                "<p><strong>挂载点:</strong> <span style='color:#1890ff'>%s</span></p>",
                                                mountPoint));
                                message.append(
                                                String.format("<p><strong>设备:</strong> <span style='color:#722ed1'>%s</span></p>",
                                                                device));
                                message.append(String.format(
                                                "<p><strong>总空间:</strong> <span style='color:#1890ff;font-weight:bold'>%s</span></p>",
                                                size));
                                message.append(String.format(
                                                "<p><strong>可用空间:</strong> <span style='color:#f5222d;font-weight:bold'>%s</span> (阈值: <span style='color:#722ed1;font-weight:bold'>%dGB</span>)</p>",
                                                available, minDiskSpaceGB));
                                message.append("</div>");

                                // 磁盘空间可视化
                                message.append("<div style='margin-bottom:15px'>");
                                message.append("<p><strong>磁盘可用空间:</strong></p>");

                                // 计算百分比
                                float percentAvailable = (availableGB / minDiskSpaceGB) * 100;
                                if (percentAvailable > 100)
                                        percentAvailable = 100;

                                message.append(
                                                "<div style='background:#f0f0f0;border-radius:8px;height:20px;width:100%;position:relative;overflow:hidden;'>");
                                message.append(String.format(
                                                "<div style='background:#f5222d;height:100%%;width:%d%%;border-radius:8px;'></div>",
                                                Math.round(percentAvailable)));
                                message.append(String.format(
                                                "<div style='position:absolute;top:0;left:0;right:0;bottom:0;text-align:center;color:%s;line-height:20px;font-weight:bold;'>%.2f GB / %d GB</div>",
                                                percentAvailable > 70 ? "white" : "#333", availableGB, minDiskSpaceGB));
                                message.append("</div>");
                                message.append("</div>");

                                // 警告信息
                                message.append(
                                                "<div style='background:#fff2f0;border-left:4px solid #f5222d;padding:10px;border-radius:0 4px 4px 0;'>");
                                message.append(String.format(
                                                "<p style='margin:0;color:#f5222d;font-weight:bold'>警告: 磁盘可用空间(%.2f GB)小于最低要求(%d GB)</p>",
                                                availableGB, minDiskSpaceGB));
                                message.append("<p style='margin-top:5px;margin-bottom:0;'>建议清理磁盘空间或扩展存储容量，以确保系统正常运行。</p>");
                                message.append("</div>");

                                message.append("</div>");

                                String errorMsg = String.format("磁盘可用空间不足: %.2f GB < %d GB", availableGB,
                                                minDiskSpaceGB);
                                log.error(errorMsg);
                                cacheLog.error(errorMsg);
                                checkItem.setStatus(CheckItem.Status.FAILED);
                                checkItem.setMessage(message.toString());
                                return checkItem;
                        }

                        // 检查可用空间百分比
                        // 获取总空间大小
                        float totalGB;
                        size = size.toUpperCase();
                        if (size.endsWith("G")) {
                                totalGB = Float.parseFloat(size.substring(0, size.length() - 1));
                        } else if (size.endsWith("T")) {
                                totalGB = Float.parseFloat(size.substring(0, size.length() - 1)) * 1024;
                        } else if (size.endsWith("M")) {
                                totalGB = Float.parseFloat(size.substring(0, size.length() - 1)) / 1024;
                        } else {
                                totalGB = Float.parseFloat(size);
                        }

                        float availablePercent = (availableGB / totalGB) * 100;
                        if (availablePercent < minAvailablePercent) {
                                // 创建带HTML样式的消息

                            String message = "<div style='line-height:1.6'>" +

                                    // 状态标题
                                    String.format(
                                            "<h3 style='color:#f5222d;margin-bottom:10px'>%s 目录磁盘可用空间百分比不足</h3>",
                                            targetDir) +

                                    // 磁盘详情
                                    "<div style='margin-bottom:15px'>" +
                                    String.format(
                                            "<p><strong>挂载点:</strong> <span style='color:#1890ff'>%s</span></p>",
                                            mountPoint) +
                                    String.format("<p><strong>设备:</strong> <span style='color:#722ed1'>%s</span></p>",
                                            device) +
                                    String.format(
                                            "<p><strong>总空间:</strong> <span style='color:#1890ff;font-weight:bold'>%s</span></p>",
                                            size) +
                                    String.format(
                                            "<p><strong>可用空间:</strong> <span style='color:#f5222d;font-weight:bold'>%s</span> (%.2f%%)</p>",
                                            available, availablePercent) +
                                    String.format(
                                            "<p><strong>最小可用空间百分比:</strong> <span style='color:#722ed1;font-weight:bold'>%d%%</span></p>",
                                            minAvailablePercent) +
                                    "</div>" +

                                    // 磁盘空间可视化
                                    "<div style='margin-bottom:15px'>" +
                                    "<p><strong>磁盘可用空间百分比:</strong></p>" +
                                    "<div style='background:#f0f0f0;border-radius:8px;height:20px;width:100%;position:relative;overflow:hidden;'>" +
                                    String.format(
                                            "<div style='background:#f5222d;height:100%%;width:%d%%;border-radius:8px;'></div>",
                                            Math.round(availablePercent)) +
                                    String.format(
                                            "<div style='position:absolute;top:0;left:0;right:0;bottom:0;text-align:center;color:%s;line-height:20px;font-weight:bold;'>%.2f%% / %d%%</div>",
                                            availablePercent > 70 ? "white" : "#333", availablePercent,
                                            minAvailablePercent) +
                                    "</div>" +
                                    "</div>" +

                                    // 警告信息
                                    "<div style='background:#fff2f0;border-left:4px solid #f5222d;padding:10px;border-radius:0 4px 4px 0;'>" +
                                    String.format(
                                            "<p style='margin:0;color:#f5222d;font-weight:bold'>警告: 磁盘可用空间百分比(%.2f%%)小于最低要求(%d%%)</p>",
                                            availablePercent, minAvailablePercent) +
                                    "<p style='margin-top:5px;margin-bottom:0;'>建议清理磁盘空间或扩展存储容量，以确保系统正常运行。</p>" +
                                    "</div>" +
                                    "</div>";

                                String errorMsg = String.format("磁盘可用空间百分比不足: %.2f%% < %d%%", availablePercent,
                                                minAvailablePercent);
                                log.error(errorMsg);
                                cacheLog.error(errorMsg);
                                checkItem.setStatus(CheckItem.Status.FAILED);
                                checkItem.setMessage(message);
                                return checkItem;
                        }

                        // 如果检查通过，设置为成功
                        checkItem.setStatus(CheckItem.Status.SUCCESS);

                        // 创建带HTML样式的成功消息
                        StringBuilder message = new StringBuilder();
                        message.append("<div style='line-height:1.6'>");

                        // 状态标题
                        message.append(String.format("<h3 style='color:#52c41a;margin-bottom:10px'>%s 目录磁盘空间充足</h3>",
                                        targetDir));

                        // 磁盘详情
                        message.append("<div style='margin-bottom:15px'>");
                        message.append(
                                        String.format("<p><strong>挂载点:</strong> <span style='color:#1890ff'>%s</span></p>",
                                                        mountPoint));
                        message.append(String.format(
                                        "<p><strong>设备:</strong> <span style='color:#722ed1'>%s</span></p>", device));
                        message.append(String.format(
                                        "<p><strong>总空间:</strong> <span style='color:#1890ff;font-weight:bold'>%s</span></p>",
                                        size));
                        message.append(String.format(
                                        "<p><strong>可用空间:</strong> <span style='color:#52c41a;font-weight:bold'>%s</span> (阈值: <span style='color:#722ed1;font-weight:bold'>%dGB</span>)</p>",
                                        available, minDiskSpaceGB));
                        message.append(String.format(
                                        "<p><strong>可用空间百分比:</strong> <span style='color:#52c41a;font-weight:bold'>%.2f%%</span> (阈值: <span style='color:#722ed1;font-weight:bold'>%d%%</span>)</p>",
                                        availablePercent, minAvailablePercent));
                        message.append("</div>");

                        // 磁盘使用率可视化
                        message.append("<div style='margin-bottom:15px'>");
                        message.append("<p><strong>磁盘使用率:</strong></p>");

                        // 颜色等级
                        String usageColor = usage < 50 ? "#52c41a" : (usage < 80 ? "#faad14" : "#f5222d");

                        message.append(
                                        "<div style='background:#f0f0f0;border-radius:8px;height:20px;width:100%;position:relative;overflow:hidden;'>");
                        message.append(String.format(
                                        "<div style='background:%s;height:100%%;width:%d%%;border-radius:8px;'></div>",
                                        usageColor, usage));
                        message.append(String.format(
                                        "<div style='position:absolute;top:0;left:0;right:0;bottom:0;text-align:center;color:%s;line-height:20px;font-weight:bold;'>%d%%</div>",
                                        usage > 70 ? "white" : "#333", usage));
                        message.append("</div>");
                        message.append("</div>");

                        // 磁盘可用空间可视化
                        message.append("<div style='margin-bottom:15px'>");
                        message.append("<p><strong>磁盘可用空间:</strong></p>");

                        // 计算可用空间百分比
                        float percentAvailable = (availableGB / minDiskSpaceGB) * 100;
                        if (percentAvailable > 100)
                                percentAvailable = 100;

                        message.append(
                                        "<div style='background:#f0f0f0;border-radius:8px;height:20px;width:100%;position:relative;overflow:hidden;'>");
                        message.append(
                                        String.format("<div style='background:#52c41a;height:100%%;width:%d%%;border-radius:8px;'></div>",
                                                        Math.round(percentAvailable)));
                        message.append(String.format(
                                        "<div style='position:absolute;top:0;left:0;right:0;bottom:0;text-align:center;color:%s;line-height:20px;font-weight:bold;'>%.2f GB / %d GB</div>",
                                        percentAvailable > 70 ? "white" : "#333", availableGB, minDiskSpaceGB));
                        message.append("</div>");
                        message.append("</div>");

                        // 成功信息
                        message.append(
                                        "<div style='background:#f6ffed;border-left:4px solid #52c41a;padding:10px;border-radius:0 4px 4px 0;'>");
                        message.append("<p style='margin:0;color:#52c41a;font-weight:bold'>磁盘检查通过</p>");
                        message.append("<p style='margin-top:5px;margin-bottom:0;'>磁盘空间充足，可以正常运行系统和应用程序。</p>");
                        message.append("</div>");

                        message.append("</div>");

                        checkItem.setMessage(message.toString());
                        cacheLog.info("磁盘检查通过，磁盘空间充足");

                } catch (NumberFormatException e) {
                        String errorMsg = "无法解析磁盘使用率: " + usageStr;
                        log.error(errorMsg, e);
                        cacheLog.error(errorMsg);
                        checkItem.setStatus(CheckItem.Status.FAILED);
                        checkItem.setMessage(errorMsg);
                        return checkItem;
                }

                return checkItem;
        }

        // 打开SSH会话
        protected ClientSession openSession(HostInfo hostInfo) {
                try {
                        return sshConnectionPoolManager.getOrCreateConnection(hostInfo);
                } catch (Exception e) {
                        log.error("无法创建SSH会话", e);
                        return null;
                }
        }

        @Override
        public void provideCleanupSuggestions(CheckLogger cacheLog) {
                cacheLog.warn("磁盘空间不足，建议以下通用清理措施:");
                cacheLog.warn("1. 删除临时文件: rm -rf /tmp/* /var/tmp/*");
                cacheLog.warn("2. 删除旧日志文件: find /var/log -type f -name \"*.gz\" -delete");
                cacheLog.warn("3. 清理软件包缓存");
                cacheLog.warn("4. 移除不需要的大文件: du -sh /* | sort -hr");
                cacheLog.warn("5. 考虑扩展磁盘分区或挂载新磁盘");
        }

        // 获取全局最小可用空间百分比
        protected int getMinAvailablePercent() {
                if (diskChecker != null) {
                        return diskChecker.getMinAvailablePercent();
                }
                return 20; // 默认值
        }

        @Override
        public OsDistribution getSupportedOs() {
                return OsDistribution.OTHER;
        }

        @Override
        public CheckItem fix(HostInfo hostInfo, CheckItem checkItem) {
                // 通用磁盘检查器不执行自动修复，只提供清理建议
                log.info("通用磁盘检查器不执行自动修复，只提供清理建议");

                // 创建日志记录器
                CheckLogger cacheLog = CheckLogger.createLogger(null, this.getClass().getSimpleName());

                // 提供清理建议
                provideCleanupSuggestions(cacheLog);

                // 返回检查项，状态保持不变
                return checkItem;
        }
}