package com.datasophon.api.service.checker.checkers.timesync;

import com.datasophon.api.config.CheckerProperties;
import com.datasophon.api.service.checker.common.CommandResult;
import com.datasophon.api.service.checker.common.ItemCode;
import com.datasophon.api.service.checker.core.AbstractItemChecker;
import com.datasophon.api.service.checker.helpers.HtmlStyleHelper;
import com.datasophon.common.model.CheckItem;
import com.datasophon.common.model.HostInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

@Component
public class TimeSyncChecker extends AbstractItemChecker {

    private static final Logger logger = LoggerFactory.getLogger(TimeSyncChecker.class);
    @Autowired
    private CheckerProperties checkerProperties;

    @Override
    protected CheckItem doCheck(HostInfo hostInfo, CheckItem checkItem) {
        try {
            // 从配置中获取最大允许时间差
            int maxTimeDiffSeconds = checkerProperties.getTimeSync().getMaxTimeDiff();

            cacheLog.info("==== 时间同步检查开始 ====");
            cacheLog.info("主机: " + hostInfo.getIp());
            cacheLog.info("最大允许时间差: " + maxTimeDiffSeconds + "秒");

            // 更新状态为正在获取远程服务器时间
            setCheckItemMessage(hostInfo, checkItem, "正在获取远程服务器时间...");

            // 1. 首先获取远程服务器的时间
            CommandResult remoteTimeResult = execCommand(sshConnectionPoolManager.getOrCreateConnection(hostInfo), "date '+%Y-%m-%d %H:%M:%S'");
            if (!remoteTimeResult.isSuccess()) {
                cacheLog.error("获取远程服务器时间失败: %s", remoteTimeResult.getErrorOrOutput());
                checkItem.setStatus(CheckItem.Status.FAILED);
                setCheckItemMessage(hostInfo, checkItem, "获取远程服务器时间失败: " + remoteTimeResult.getErrorOrOutput());
                return checkItem;
            }

            String remoteTimeStr = remoteTimeResult.getOutput().trim();
            cacheLog.info("远程服务器时间: " + remoteTimeStr);

            // 更新状态为正在获取远程服务器时区
            setCheckItemMessage(hostInfo, checkItem, "正在获取远程服务器时区...");

            // 2. 获取远程服务器时区
            CommandResult remoteTzResult = execCommand(sshConnectionPoolManager.getOrCreateConnection(hostInfo), "date '+%Z'");
            if (!remoteTzResult.isSuccess()) {
                cacheLog.warn("获取远程服务器时区失败: %s", remoteTzResult.getErrorOrOutput());
            }
            String remoteTz = remoteTzResult.isSuccess() ? remoteTzResult.getOutput().trim() : "未知";
            cacheLog.info("远程服务器时区: " + remoteTz);

            // 3. 获取本地服务器时间
            Date localDate = new Date();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String localTimeStr = sdf.format(localDate);
            cacheLog.info("本地服务器时间: " + localTimeStr);

            // 4. 获取本地服务器时区
            String localTz = TimeZone.getDefault().getID();
            cacheLog.info("本地服务器时区: " + localTz);

            // 更新状态为正在计算时间差
            setCheckItemMessage(hostInfo, checkItem, "正在计算时间差...");

            // 5. 计算时间差
            try {
                Date remoteDate = sdf.parse(remoteTimeStr);
                long diffMillis = Math.abs(remoteDate.getTime() - localDate.getTime());
                long diffSeconds = diffMillis / 1000;

                cacheLog.info("时间差: " + diffSeconds + "秒");

                boolean isTimeSynced = diffSeconds <= maxTimeDiffSeconds;

                if (isTimeSynced) {
                    checkItem.setStatus(CheckItem.Status.SUCCESS);

                    // 创建HTML详细信息构建器
                    StringBuilder detailsBuilder = new StringBuilder();

                    // 添加时间同步信息组
                    detailsBuilder.append(HtmlStyleHelper.beginGroup());

                    // 添加时间信息
                    detailsBuilder.append(HtmlStyleHelper.generatePropertyRow(
                            "远程服务器时间", remoteTimeStr, HtmlStyleHelper.Colors.INFO));
                    detailsBuilder.append(HtmlStyleHelper.generatePropertyRow(
                            "控制台服务器时间", localTimeStr, HtmlStyleHelper.Colors.INFO));
                    detailsBuilder.append(HtmlStyleHelper.generatePropertyRow(
                            "远程服务器时区", remoteTz, HtmlStyleHelper.Colors.PURPLE));
                    detailsBuilder.append(HtmlStyleHelper.generatePropertyRow(
                            "控制台服务器时区", localTz, HtmlStyleHelper.Colors.PURPLE));

                    // 添加时间差信息
                    String timeDiffColor = diffSeconds <= 2 ? HtmlStyleHelper.Colors.SUCCESS
                            : (diffSeconds <= 5 ? HtmlStyleHelper.Colors.CYAN : HtmlStyleHelper.Colors.WARNING);
                    detailsBuilder.append(HtmlStyleHelper.generatePropertyRowWithThreshold(
                            "时间差", diffSeconds + "秒", timeDiffColor,
                            maxTimeDiffSeconds, "秒"));

                    // 添加时间同步百分比
                    int syncPercentage = 100 - (int) Math.min(100, (diffSeconds * 100) / maxTimeDiffSeconds);
                    detailsBuilder.append("<p><strong>时间同步度:</strong></p>");
                    detailsBuilder.append(HtmlStyleHelper.generateProgressBar(
                            syncPercentage, HtmlStyleHelper.Colors.SUCCESS, syncPercentage + "%"));

                    detailsBuilder.append(HtmlStyleHelper.endGroup());

                    // 添加成功信息
                    detailsBuilder.append(HtmlStyleHelper.generateSuccessAlert(
                            "时间同步检查通过",
                            String.format("服务器时间同步正常，时间差为 %d 秒，小于最大允许差值 %d 秒。",
                                    diffSeconds, maxTimeDiffSeconds)));

                    // 设置格式化的HTML消息
                    setStyledHtmlMessage(hostInfo, checkItem, true, "服务器时间同步正常", detailsBuilder);

                    cacheLog.info("服务器时间同步检查通过");
                } else {
                    checkItem.setStatus(CheckItem.Status.FAILED);

                    // 创建HTML详细信息构建器
                    StringBuilder detailsBuilder = new StringBuilder();

                    // 添加时间同步信息组
                    detailsBuilder.append(HtmlStyleHelper.beginGroup());

                    // 添加时间信息
                    detailsBuilder.append(HtmlStyleHelper.generatePropertyRow(
                            "远程服务器时间", remoteTimeStr, HtmlStyleHelper.Colors.INFO));
                    detailsBuilder.append(HtmlStyleHelper.generatePropertyRow(
                            "控制台服务器时间", localTimeStr, HtmlStyleHelper.Colors.INFO));
                    detailsBuilder.append(HtmlStyleHelper.generatePropertyRow(
                            "远程服务器时区", remoteTz, HtmlStyleHelper.Colors.PURPLE));
                    detailsBuilder.append(HtmlStyleHelper.generatePropertyRow(
                            "控制台服务器时区", localTz, HtmlStyleHelper.Colors.PURPLE));

                    // 添加时间差信息
                    detailsBuilder.append(HtmlStyleHelper.generatePropertyRowWithThreshold(
                            "时间差", diffSeconds + "秒", HtmlStyleHelper.Colors.ERROR,
                            maxTimeDiffSeconds, "秒"));

                    // 添加时间同步百分比
                    int syncPercentage = Math.max(0, 100 - (int) ((diffSeconds * 100) / maxTimeDiffSeconds));
                    detailsBuilder.append("<p><strong>时间同步度:</strong></p>");
                    detailsBuilder.append(HtmlStyleHelper.generateProgressBar(
                            syncPercentage, HtmlStyleHelper.Colors.ERROR, syncPercentage + "%"));

                    detailsBuilder.append(HtmlStyleHelper.endGroup());

                    // 添加警告信息
                    detailsBuilder.append(HtmlStyleHelper.generateWarningAlert(
                            "时间同步检查未通过",
                            String.format("服务器时间不同步，时间差为 %d 秒，大于最大允许差值 %d 秒。请确保安装并配置NTP服务。",
                                    diffSeconds, maxTimeDiffSeconds)));

                    // 设置格式化的HTML消息
                    setStyledHtmlMessage(hostInfo, checkItem, false, "服务器时间不同步", detailsBuilder);

                    cacheLog.info("服务器时间同步检查未通过");
                }
            } catch (Exception e) {
                cacheLog.error("计算时间差时发生错误: %s", e.getMessage());
                checkItem.setStatus(CheckItem.Status.FAILED);
                setCheckItemMessage(hostInfo, checkItem, "计算时间差时发生错误: " + e.getMessage());
            }

        } catch (Exception e) {
            String errorMsg = "检查时间同步时发生错误: " + e.getMessage();
            logger.error(errorMsg, e);
            cacheLog.error(errorMsg);
            checkItem.setStatus(CheckItem.Status.FAILED);
            setCheckItemMessage(hostInfo, checkItem, errorMsg);
        } finally {
            cacheLog.info("==== 时间同步检查结束 ====");
        }
        return checkItem;
    }

    @Override
    protected boolean doFix(HostInfo hostInfo, CheckItem checkItem) {
        try {
            // 从配置中获取最大允许时间差
            int maxTimeDiffSeconds = checkerProperties.getTimeSync().getMaxTimeDiff();

            cacheLog.info("==== 开始修复服务器时间同步 ====");

            // 更新状态为正在获取本地时间
            setCheckItemMessage(hostInfo, checkItem, "正在获取本地时间信息...");

            // 1. 获取本地时间
            Date localDate = new Date();
            SimpleDateFormat dateFmt = new SimpleDateFormat("yyyy-MM-dd");
            SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm:ss");
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String localDateStr = dateFmt.format(localDate);
            String localTimeStr = timeFmt.format(localDate);
            String fullLocalTimeStr = sdf.format(localDate);

            cacheLog.info("本地时间: " + localDateStr + " " + localTimeStr);

            // 2. 获取本地时区
            String localTz = TimeZone.getDefault().getID();
            cacheLog.info("本地时区: " + localTz);

            String tzFile = getTimezoneFile(localTz);
            if (tzFile == null || tzFile.isEmpty()) {
                cacheLog.warn("无法找到对应的时区文件，将只同步时间而不同步时区");
            } else {
                cacheLog.info("对应的时区文件: " + tzFile);
            }

            // 更新状态为正在设置时区
            setCheckItemMessage(hostInfo, checkItem, "正在设置服务器时区...");

            // 3. 设置远程服务器时区（如果能确定对应的时区文件）
            boolean tzSetSuccess = false;
            if (tzFile != null && !tzFile.isEmpty()) {
                cacheLog.info("设置远程服务器时区...");
                CommandResult tzResult = execCommand(sshConnectionPoolManager.getOrCreateConnection(hostInfo), "ln -sf " + tzFile + " /etc/localtime");
                tzSetSuccess = tzResult.isSuccess();
                if (!tzSetSuccess) {
                    cacheLog.warn("设置时区失败: %s", tzResult.getErrorOrOutput());
                } else {
                    cacheLog.info("时区设置成功");
                }
            }

            // 更新状态为正在设置系统时间
            setCheckItemMessage(hostInfo, checkItem, "正在设置系统时间...");

            // 4. 设置远程服务器日期和时间
            cacheLog.info("设置远程服务器日期和时间...");
            String dateCmd = "date -s \"" + localDateStr + " " + localTimeStr + "\"";
            CommandResult dateResult = execCommand(sshConnectionPoolManager.getOrCreateConnection(hostInfo), dateCmd);
            boolean dateSetSuccess = dateResult.isSuccess();

            if (!dateSetSuccess) {
                cacheLog.error("设置日期和时间失败: %s", dateResult.getErrorOrOutput());

                // 创建HTML详细信息构建器
                StringBuilder detailsBuilder = new StringBuilder();

                // 添加错误信息
                detailsBuilder.append(HtmlStyleHelper.generateWarningAlert(
                        "设置系统时间失败",
                        "无法设置系统时间: " + dateResult.getErrorOrOutput()));

                // 添加手动修复指南
                detailsBuilder.append(HtmlStyleHelper.beginGroup());
                detailsBuilder.append("<p><strong>手动修复步骤:</strong></p>");
                detailsBuilder.append("<ol style='padding-left:20px;margin-bottom:15px'>");
                detailsBuilder.append("<li style='margin-bottom:5px'>安装并配置NTP服务:</li>");

                detailsBuilder.append(HtmlStyleHelper.generateCodeBlock(
                        """
                                # 安装NTP服务
                                yum install -y ntp
                                
                                # 设置NTP服务开机启动
                                systemctl enable ntpd
                                
                                # 启动NTP服务
                                systemctl start ntpd"""));

                detailsBuilder.append("<li style='margin-bottom:5px'>或者手动设置系统时间:</li>");
                detailsBuilder.append(HtmlStyleHelper.generateCodeBlock(
                        "# 设置日期和时间\ndate -s \"" + localDateStr + " " + localTimeStr + "\"\n\n" +
                                "# 同步到硬件时钟\nhwclock --systohc"));

                detailsBuilder.append("</ol>");
                detailsBuilder.append(HtmlStyleHelper.endGroup());

                // 设置格式化的HTML消息
                setStyledHtmlMessage(hostInfo, checkItem, false, "设置系统时间失败", detailsBuilder);

                return false;
            }
            cacheLog.info("日期和时间设置成功");

            // 更新状态为正在同步硬件时钟
            setCheckItemMessage(hostInfo, checkItem, "正在同步硬件时钟...");

            // 5. 将时间写入硬件时钟
            cacheLog.info("将时间同步到硬件时钟...");
            CommandResult hwClockResult = execCommand(sshConnectionPoolManager.getOrCreateConnection(hostInfo), "hwclock --systohc");
            boolean hwClockSetSuccess = hwClockResult.isSuccess();
            if (!hwClockSetSuccess) {
                cacheLog.warn("硬件时钟同步失败: %s", hwClockResult.getErrorOrOutput());
            } else {
                cacheLog.info("硬件时钟同步成功");
            }

            // 更新状态为正在验证时间同步
            setCheckItemMessage(hostInfo, checkItem, "正在验证时间同步结果...");

            // 6. 验证时间同步结果
            cacheLog.info("验证时间同步结果...");
            CommandResult verifyResult = execCommand(sshConnectionPoolManager.getOrCreateConnection(hostInfo), "date '+%Y-%m-%d %H:%M:%S'");
            boolean verifySuccess = verifyResult.isSuccess();
            String remoteTimeAfterSync = verifySuccess ? verifyResult.getOutput().trim() : "未知";

            if (verifySuccess) {
                cacheLog.info("同步后的远程服务器时间: " + remoteTimeAfterSync);

                // 再次获取本地时间进行比较
                Date newLocalDate = new Date();
                String newLocalTimeStr = sdf.format(newLocalDate);
                cacheLog.info("当前本地服务器时间: " + newLocalTimeStr);

                try {
                    Date remoteDate = sdf.parse(remoteTimeAfterSync);
                    long diffMillis = Math.abs(remoteDate.getTime() - newLocalDate.getTime());
                    long diffSeconds = diffMillis / 1000;

                    cacheLog.info("同步后的时间差: " + diffSeconds + "秒");

                    // 创建HTML详细信息构建器
                    StringBuilder detailsBuilder = new StringBuilder();

                    // 添加时间同步操作信息组
                    detailsBuilder.append(HtmlStyleHelper.beginGroup());
                    detailsBuilder.append("<p><strong>已完成的时间同步操作:</strong></p>");

                    // 添加操作列表
                    detailsBuilder.append("<ol style='padding-left:20px;margin-bottom:15px'>");

                    // 设置时区操作
                    if (tzFile != null && !tzFile.isEmpty()) {
                        String tzStatus = tzSetSuccess ? "成功" : "失败";
                        String tzColor = tzSetSuccess ? HtmlStyleHelper.Colors.SUCCESS : HtmlStyleHelper.Colors.ERROR;
                        detailsBuilder.append("<li style='margin-bottom:5px'>设置系统时区 (").append(HtmlStyleHelper.generateColoredValue(tzStatus, tzColor)).append(")</li>");
                    }

                    // 设置系统时间操作
                    detailsBuilder.append("<li style='margin-bottom:5px'>设置系统时间 (").append(HtmlStyleHelper.generateColoredValue("成功", HtmlStyleHelper.Colors.SUCCESS)).append(")</li>");

                    // 同步硬件时钟操作
                    String hwStatus = hwClockSetSuccess ? "成功" : "失败（不影响使用）";
                    String hwColor = hwClockSetSuccess ? HtmlStyleHelper.Colors.SUCCESS
                            : HtmlStyleHelper.Colors.WARNING;
                    detailsBuilder.append("<li style='margin-bottom:5px'>同步硬件时钟 (").append(HtmlStyleHelper.generateColoredValue(hwStatus, hwColor)).append(")</li>");

                    detailsBuilder.append("</ol>");
                    detailsBuilder.append(HtmlStyleHelper.endGroup());

                    // 添加同步结果信息组
                    detailsBuilder.append(HtmlStyleHelper.beginGroup());

                    // 添加时间信息
                    detailsBuilder.append(HtmlStyleHelper.generatePropertyRow(
                            "同步前控制台时间", fullLocalTimeStr, HtmlStyleHelper.Colors.INFO));
                    detailsBuilder.append(HtmlStyleHelper.generatePropertyRow(
                            "同步后远程时间", remoteTimeAfterSync, HtmlStyleHelper.Colors.SUCCESS));
                    detailsBuilder.append(HtmlStyleHelper.generatePropertyRow(
                            "验证时控制台时间", newLocalTimeStr, HtmlStyleHelper.Colors.INFO));

                    // 添加时间差信息
                    String timeDiffColor = diffSeconds <= 2 ? HtmlStyleHelper.Colors.SUCCESS
                            : (diffSeconds <= 5 ? HtmlStyleHelper.Colors.CYAN : HtmlStyleHelper.Colors.WARNING);
                    detailsBuilder.append(HtmlStyleHelper.generatePropertyRow(
                            "同步后时间差", diffSeconds + "秒", timeDiffColor));

                    // 添加同步结果百分比
                    int syncPercentage = 100 - (int) Math.min(100, (diffSeconds * 100) / maxTimeDiffSeconds);
                    detailsBuilder.append("<p><strong>时间同步度:</strong></p>");
                    detailsBuilder.append(HtmlStyleHelper.generateProgressBar(
                            syncPercentage, timeDiffColor, syncPercentage + "%"));

                    detailsBuilder.append(HtmlStyleHelper.endGroup());

                    // 添加结果提示
                    if (diffSeconds <= maxTimeDiffSeconds) {
                        detailsBuilder.append(HtmlStyleHelper.generateSuccessAlert(
                                "时间同步修复成功",
                                String.format("服务器时间已成功同步，当前时间差为 %d 秒，小于最大允许差值 %d 秒。",
                                        diffSeconds, maxTimeDiffSeconds)));
                    } else {
                        detailsBuilder.append(HtmlStyleHelper.generateWarningAlert(
                                "时间同步部分成功",
                                String.format("服务器时间同步后仍有 %d 秒的时间差，建议安装并配置NTP服务以保持时间同步。",
                                        diffSeconds)));
                    }

                    // 添加建议安装NTP服务
                    detailsBuilder.append(HtmlStyleHelper.generateNoteAlert(
                            "保持时间同步的建议",
                            """
                                    为确保服务器时间长期保持同步，建议安装NTP服务并配置为自动启动。以CentOS/RHEL为例：\
                                    <pre style='background:#f5f5f5;margin:5px 0;padding:5px;border-radius:3px'>yum install -y ntp
                                    systemctl enable ntpd
                                    systemctl start ntpd</pre>"""));

                    // 设置格式化的HTML消息
                    setStyledHtmlMessage(hostInfo, checkItem, true, "服务器时间同步已修复", detailsBuilder);

                    cacheLog.info("时间同步修复" + (diffSeconds <= maxTimeDiffSeconds ? "成功" : "部分成功"));
                } catch (Exception e) {
                    cacheLog.warn("验证时间同步结果时发生错误: %s", e.getMessage());

                    // 创建简单的成功消息
                    StringBuilder simpleDetailsBuilder = new StringBuilder();
                    simpleDetailsBuilder.append(HtmlStyleHelper.generateWarningAlert(
                            "时间同步可能成功",
                            "时间同步操作已完成，但验证结果时发生错误: " + e.getMessage()));

                    setStyledHtmlMessage(hostInfo, checkItem, true, "时间同步操作已完成", simpleDetailsBuilder);
                }
            } else {
                cacheLog.warn("获取同步后的远程时间失败: %s", verifyResult.getErrorOrOutput());

                // 创建简单的成功消息
                StringBuilder simpleDetailsBuilder = new StringBuilder();
                simpleDetailsBuilder.append(HtmlStyleHelper.generateWarningAlert(
                        "时间可能已同步",
                        "时间同步操作已完成，但无法获取同步后的时间来验证结果。"));

                setStyledHtmlMessage(hostInfo, checkItem, true, "时间同步操作已完成", simpleDetailsBuilder);
            }

            cacheLog.info("==== 服务器时间同步修复完成 ====");
            return true;

        } catch (Exception e) {
            String errorMsg = "修复时间同步时发生错误: " + e.getMessage();
            logger.error(errorMsg, e);
            cacheLog.error(errorMsg);

            // 创建HTML详细信息构建器
            StringBuilder detailsBuilder = new StringBuilder();

            // 添加错误信息
            detailsBuilder.append(HtmlStyleHelper.generateWarningAlert(
                    "时间同步修复失败",
                    "修复时间同步时发生错误: " + e.getMessage()));

            // 添加手动修复指南
            detailsBuilder.append(HtmlStyleHelper.beginGroup());
            detailsBuilder.append("<p><strong>手动修复步骤:</strong></p>");
            detailsBuilder.append("<ol style='padding-left:20px;margin-bottom:15px'>");

            // 设置系统时间
            Date now = new Date();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String nowStr = sdf.format(now);

            detailsBuilder.append("<li style='margin-bottom:5px'>手动设置系统时间:</li>");
            detailsBuilder.append(HtmlStyleHelper.generateCodeBlock(
                    "# 设置系统时间\ndate -s \"" + nowStr + "\"\n\n" +
                            "# 同步到硬件时钟\nhwclock --systohc"));

            detailsBuilder.append("<li style='margin-bottom:5px'>或者安装NTP服务:</li>");
            detailsBuilder.append(HtmlStyleHelper.generateCodeBlock(
                    """
                            # CentOS/RHEL系统
                            yum install -y ntp
                            systemctl enable ntpd
                            systemctl start ntpd
                            
                            # Debian/Ubuntu系统
                            apt-get install -y ntp
                            systemctl enable ntp
                            systemctl start ntp"""));

            detailsBuilder.append("</ol>");
            detailsBuilder.append(HtmlStyleHelper.endGroup());

            // 设置格式化的HTML消息
            setStyledHtmlMessage(hostInfo, checkItem, false, "时间同步修复失败", detailsBuilder);

            return false;
        }
    }

    /**
     * 根据时区ID获取对应的时区文件路径
     * 
     * @param tzId 时区ID，如 "Asia/Shanghai"
     * @return 时区文件路径，如 "/usr/share/zoneinfo/Asia/Shanghai"
     */
    private String getTimezoneFile(String tzId) {
        if (tzId == null || tzId.isEmpty()) {
            return null;
        }

        // 常见时区ID到文件路径的映射
        return switch (tzId) {
            case "Asia/Shanghai", "Asia/Chongqing", "Asia/Harbin", "Asia/Urumqi" -> "/usr/share/zoneinfo/Asia/Shanghai";
            case "America/New_York" -> "/usr/share/zoneinfo/America/New_York";
            case "America/Los_Angeles" -> "/usr/share/zoneinfo/America/Los_Angeles";
            case "Europe/London" -> "/usr/share/zoneinfo/Europe/London";
            case "Europe/Paris" -> "/usr/share/zoneinfo/Europe/Paris";
            default -> {
                // 如果是直接的路径形式，尝试直接使用
                if (tzId.startsWith("Asia/") || tzId.startsWith("America/") || tzId.startsWith("Europe/") ||
                        tzId.startsWith("Australia/") || tzId.startsWith("Pacific/") || tzId.startsWith("Atlantic/")) {
                    yield "/usr/share/zoneinfo/" + tzId;
                }
                yield null;
            }
        };
    }

    @Override
    public ItemCode getCheckerType() {
        return ItemCode.TIME_SYNC;
    }
}