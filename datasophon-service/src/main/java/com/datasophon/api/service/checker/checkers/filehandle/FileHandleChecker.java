package com.datasophon.api.service.checker.checkers.filehandle;

import com.datasophon.api.service.checker.core.AbstractItemChecker;
import com.datasophon.api.service.checker.common.CommandResult;
import com.datasophon.common.model.CheckItem;
import com.datasophon.common.model.HostInfo;
import com.datasophon.api.service.checker.common.ItemCode;
import com.datasophon.api.service.checker.helpers.HtmlStyleHelper;
import org.apache.sshd.client.session.ClientSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class FileHandleChecker extends AbstractItemChecker {

    private static final Logger logger = LoggerFactory.getLogger(FileHandleChecker.class);
    private static final int MIN_FILE_HANDLES = 65535;

    @Override
    protected CheckItem doCheck(HostInfo hostInfo, CheckItem checkItem) {
        try {
            cacheLog.debug("开始检查文件句柄数 - 主机: %s", hostInfo.getHostname());
            cacheLog.debug("最小建议文件句柄数: %d", MIN_FILE_HANDLES);
            cacheLog.info("正在检查系统最大文件句柄数限制...");

            // 更新状态为正在检查文件句柄数
            setCheckItemMessage(hostInfo, checkItem, "正在检查系统最大文件句柄数限制...");

            // 执行ulimit命令获取当前最大文件句柄数
            cacheLog.debug("执行命令: ulimit -n");
            cacheLog.info("执行命令: ulimit -n 获取当前文件句柄数...");
            CommandResult result = execCommand(session, "ulimit -n");

            if (!result.isSuccess()) {
                cacheLog.debug("命令执行失败: %s", result.getErrorOrOutput());
                cacheLog.error("获取文件句柄数失败: %s", result.getErrorOrOutput());
                checkItem.setStatus(CheckItem.Status.FAILED);
                setCheckItemMessage(hostInfo, checkItem, "检查失败: " + result.getErrorOrOutput());
                return checkItem;
            }

            cacheLog.debug("命令返回值: %s", result.getOutput().trim());
            cacheLog.info("当前系统最大文件句柄数: %s", result.getOutput().trim());
            int fileHandles = Integer.parseInt(result.getOutput().trim());
            boolean success = fileHandles >= MIN_FILE_HANDLES;

            cacheLog.debug("当前文件句柄数: %d, 是否满足最小要求: %s", fileHandles, success ? "是" : "否");
            cacheLog.info("检查结果: 当前文件句柄数为 %d, 最小建议值为 %d", fileHandles, MIN_FILE_HANDLES);

            // 更新状态为分析结果
            setCheckItemMessage(hostInfo, checkItem, "正在分析文件句柄数...");

            checkItem.setStatus(success ? CheckItem.Status.SUCCESS : CheckItem.Status.FAILED);

            // 创建HTML详细信息构建器
            StringBuilder detailsBuilder = new StringBuilder();

            // 添加文件句柄信息组
            detailsBuilder.append(HtmlStyleHelper.beginGroup());

            // 添加文件句柄数信息
            String fileHandleColor = success ? HtmlStyleHelper.Colors.SUCCESS : HtmlStyleHelper.Colors.ERROR;
            detailsBuilder.append(HtmlStyleHelper.generatePropertyRowWithThreshold(
                    "当前文件句柄数限制", String.valueOf(fileHandles), fileHandleColor,
                    MIN_FILE_HANDLES, ""));

            detailsBuilder.append(HtmlStyleHelper.generatePropertyRow(
                    "最小建议文件句柄数", String.valueOf(MIN_FILE_HANDLES), HtmlStyleHelper.Colors.INFO));

            // 计算并显示文件句柄充足率
            double sufficiencyRate = ((double) fileHandles / MIN_FILE_HANDLES) * 100;
            int sufficiencyPercent = Math.min(100, (int) Math.round(sufficiencyRate));

            detailsBuilder.append("<p><strong>文件句柄充足率:</strong></p>");
            String progressColor = success ? HtmlStyleHelper.Colors.SUCCESS : HtmlStyleHelper.Colors.ERROR;
            detailsBuilder.append(HtmlStyleHelper.generateProgressBar(
                    sufficiencyPercent, progressColor, sufficiencyPercent + "%"));

            detailsBuilder.append(HtmlStyleHelper.endGroup());

            // 添加命令执行信息组
            detailsBuilder.append(HtmlStyleHelper.beginGroup());
            detailsBuilder.append("<p><strong>命令执行信息:</strong></p>");
            detailsBuilder.append(HtmlStyleHelper.generateCodeBlock("$ ulimit -n\n" + result.getOutput().trim()));
            detailsBuilder.append(HtmlStyleHelper.endGroup());

            // 添加检查结果提示
            if (success) {
                detailsBuilder.append(HtmlStyleHelper.generateSuccessAlert(
                        "文件句柄检查通过",
                        String.format("系统当前文件句柄限制(%d)满足最小建议值(%d)，足够支持大规模并发操作。",
                                fileHandles, MIN_FILE_HANDLES)));
            } else {
                detailsBuilder.append(HtmlStyleHelper.generateWarningAlert(
                        "文件句柄检查未通过",
                        String.format("系统当前文件句柄限制(%d)小于最小建议值(%d)，可能导致\"Too many open files\"错误，影响系统稳定性。",
                                fileHandles, MIN_FILE_HANDLES)));
            }

            // 设置格式化的HTML消息
            setStyledHtmlMessage(hostInfo, checkItem, success, success ? "文件句柄限制检查通过" : "文件句柄限制检查未通过", detailsBuilder);

            cacheLog.debug("检查结果: %s, 消息: %s", checkItem.getStatus(), checkItem.getMessage());
            cacheLog.info("文件句柄数检查%s", success ? "通过" : "未通过");
        } catch (Exception e) {
            cacheLog.debug("检查过程异常: %s", e.getMessage());
            cacheLog.error("检查文件句柄数过程中发生异常: %s", e.getMessage());
            checkItem.setStatus(CheckItem.Status.FAILED);
            setCheckItemMessage(hostInfo, checkItem, "检查失败: " + e.getMessage());
        }
        return checkItem;
    }

    @Override
    protected boolean doFix(HostInfo hostInfo, CheckItem checkItem) {
        try {
            cacheLog.debug("开始修复文件句柄数限制 - 主机: %s", hostInfo.getHostname());

            // 更新状态为正在修改limits.conf文件
            setCheckItemMessage(hostInfo, checkItem, "正在修改系统文件句柄限制配置...");

            // 修改 /etc/security/limits.conf 文件
            String cmd = String.format(
                    "grep -q '* soft nofile %d' /etc/security/limits.conf || echo '* soft nofile %d' >> /etc/security/limits.conf && "
                            +
                            "grep -q '* hard nofile %d' /etc/security/limits.conf || echo '* hard nofile %d' >> /etc/security/limits.conf",
                    MIN_FILE_HANDLES, MIN_FILE_HANDLES, MIN_FILE_HANDLES, MIN_FILE_HANDLES);

            cacheLog.debug("执行修复命令: %s", cmd);
            CommandResult result = execCommand(session, cmd);

            if (!result.isSuccess()) {
                cacheLog.debug("修改limits.conf文件失败: %s", result.getErrorOrOutput());
                cacheLog.error("修改limits.conf文件失败: %s", result.getErrorOrOutput());
                setCheckItemMessage(hostInfo, checkItem, "修改系统文件句柄限制配置失败");
                return false;
            }
            cacheLog.debug("limits.conf文件修改结果: %s", result.getOutput().isEmpty() ? "成功" : result.getOutput());

            // 更新状态为正在检查systemd
            setCheckItemMessage(hostInfo, checkItem, "正在检查systemd配置...");

            // 检查是否存在systemd
            cacheLog.debug("检查主机是否使用systemd...");
            boolean hasSystemd = isSystemdExists(session);
            cacheLog.debug("systemd检查结果: %s", hasSystemd ? "存在" : "不存在");

            // 如果是CentOS/RHEL,还需要通过systemd配置
            if (hasSystemd) {
                // 更新状态为正在配置systemd
                setCheckItemMessage(hostInfo, checkItem, "正在配置systemd文件句柄限制...");

                cacheLog.debug("通过systemd配置文件句柄限制...");

                // 创建systemd配置目录
                cacheLog.debug("创建目录: /etc/systemd/system.conf.d");
                CommandResult mkdirResult = execCommand(session, "mkdir -p /etc/systemd/system.conf.d");
                if (!mkdirResult.isSuccess()) {
                    cacheLog.warn("创建systemd配置目录失败: %s", mkdirResult.getErrorOrOutput());
                }

                // 创建systemd配置文件
                String systemdConfig = "echo -e '[Manager]\\nDefaultLimitNOFILE=" + MIN_FILE_HANDLES
                        + "' > /etc/systemd/system.conf.d/limits.conf";
                cacheLog.debug("配置systemd文件句柄限制: %s", systemdConfig);
                CommandResult systemdResult = execCommand(session, systemdConfig);

                if (!systemdResult.isSuccess()) {
                    cacheLog.debug("配置systemd文件句柄限制失败: %s", systemdResult.getErrorOrOutput());
                    cacheLog.warn("配置systemd文件句柄限制失败，但不影响使用: %s", systemdResult.getErrorOrOutput());
                } else {
                    cacheLog.debug("systemd文件句柄限制配置成功");
                }

                // 更新状态为正在重新加载systemd配置
                setCheckItemMessage(hostInfo, checkItem, "正在重新加载systemd配置...");

                // 重新加载systemd配置
                cacheLog.debug("重新加载systemd配置");
                CommandResult reloadResult = execCommand(session, "systemctl daemon-reload");
                cacheLog.debug("systemctl daemon-reload结果: %s",
                        reloadResult.getOutput().isEmpty() ? "成功" : reloadResult.getOutput());
            }

            cacheLog.debug("文件句柄限制修复完成, 需要用户重新登录生效");

            // 创建HTML详细信息构建器
            StringBuilder detailsBuilder = new StringBuilder();

            // 添加修复详情组
            detailsBuilder.append(HtmlStyleHelper.beginGroup());

            // 添加修复步骤说明
            detailsBuilder.append("<p><strong>已完成的修复操作:</strong></p>");
            detailsBuilder.append("<ol style='padding-left:20px;margin-bottom:15px'>");
            detailsBuilder.append("<li style='margin-bottom:5px'>已修改文件 " +
                    HtmlStyleHelper.generateInlineCode("/etc/security/limits.conf") + " 添加以下配置:</li>");

            // 生成配置代码块
            String configCode = String.format("* soft nofile %d\n* hard nofile %d", MIN_FILE_HANDLES, MIN_FILE_HANDLES);
            detailsBuilder.append(HtmlStyleHelper.generateCodeBlock(configCode));

            // 添加systemd配置信息(如果适用)
            if (hasSystemd) {
                detailsBuilder.append("<li style='margin-bottom:5px'>检测到系统使用systemd，已添加systemd配置:</li>");
                String systemdConfigContent = String.format("[Manager]\nDefaultLimitNOFILE=%d", MIN_FILE_HANDLES);
                detailsBuilder.append(HtmlStyleHelper.generateCodeBlock(systemdConfigContent));
                detailsBuilder.append("<li style='margin-bottom:5px'>已执行 " +
                        HtmlStyleHelper.generateInlineCode("systemctl daemon-reload") + " 重新加载配置</li>");
            }

            detailsBuilder.append("</ol>");
            detailsBuilder.append(HtmlStyleHelper.endGroup());

            // 添加重要提示
            detailsBuilder.append(HtmlStyleHelper.generateNoteAlert(
                    "配置已更新",
                    "文件句柄限制已修改，需要<strong>重新登录</strong>才能生效。"));

            // 添加验证方法
            detailsBuilder.append(HtmlStyleHelper.beginGroup());
            detailsBuilder.append("<p><strong>验证方法:</strong></p>");
            detailsBuilder.append("<p>重新登录后，执行以下命令验证新的文件句柄限制：</p>");
            detailsBuilder.append(HtmlStyleHelper.generateCodeBlock("ulimit -n"));
            detailsBuilder.append(HtmlStyleHelper.endGroup());

            // 添加注意事项
            detailsBuilder.append(HtmlStyleHelper.generateNoteAlert("生效说明",
                    "此更改适用于新登录的会话。如需对现有进程立即生效，请重启相关服务或系统。"));

            // 设置格式化的HTML消息
            setStyledHtmlMessage(hostInfo, checkItem, true, "文件句柄限制已修改", detailsBuilder);

            return !result.getOutput().startsWith("ERROR");
        } catch (Exception e) {
            logger.error("修复文件句柄数失败: {}", e.getMessage());
            cacheLog.error("修复文件句柄数失败: %s", e.getMessage());
            cacheLog.debug("修复过程异常详情: %s", e.toString());

            // 创建HTML详细信息构建器
            StringBuilder detailsBuilder = new StringBuilder();

            // 添加错误信息
            detailsBuilder.append(HtmlStyleHelper.generateWarningAlert(
                    "修复失败",
                    "修复文件句柄限制时发生错误: " + e.getMessage()));

            // 添加手动修复指南
            detailsBuilder.append(HtmlStyleHelper.beginGroup());
            detailsBuilder.append("<p><strong>手动修复步骤:</strong></p>");
            detailsBuilder.append("<ol style='padding-left:20px;margin-bottom:15px'>");
            detailsBuilder.append("<li style='margin-bottom:5px'>编辑文件 " +
                    HtmlStyleHelper.generateInlineCode("/etc/security/limits.conf") + " 添加以下配置:</li>");

            // 生成配置代码块
            String configCode = String.format("* soft nofile %d\n* hard nofile %d", MIN_FILE_HANDLES, MIN_FILE_HANDLES);
            detailsBuilder.append(HtmlStyleHelper.generateCodeBlock(configCode));

            detailsBuilder.append("<li style='margin-bottom:5px'>如果系统使用systemd，创建目录:</li>");
            detailsBuilder.append(HtmlStyleHelper.generateCodeBlock("mkdir -p /etc/systemd/system.conf.d"));

            detailsBuilder.append("<li style='margin-bottom:5px'>创建文件 " +
                    HtmlStyleHelper.generateInlineCode("/etc/systemd/system.conf.d/limits.conf") + " 内容如下:</li>");
            String systemdConfigContent = String.format("[Manager]\nDefaultLimitNOFILE=%d", MIN_FILE_HANDLES);
            detailsBuilder.append(HtmlStyleHelper.generateCodeBlock(systemdConfigContent));

            detailsBuilder.append("<li style='margin-bottom:5px'>重新加载systemd配置:</li>");
            detailsBuilder.append(HtmlStyleHelper.generateCodeBlock("systemctl daemon-reload"));

            detailsBuilder.append("<li style='margin-bottom:5px'>重新登录或重启系统使配置生效</li>");
            detailsBuilder.append("</ol>");
            detailsBuilder.append(HtmlStyleHelper.endGroup());

            // 设置格式化的HTML消息
            setStyledHtmlMessage(hostInfo, checkItem, false, "修复文件句柄限制失败", detailsBuilder);

            return false;
        }
    }

    private boolean isSystemdExists(ClientSession session) throws InterruptedException {
        cacheLog.debug("检查systemd目录是否存在...");
        CommandResult result = execCommand(session, "[ -d /etc/systemd ] && echo 'true' || echo 'false'");
        boolean exists = "true".equals(result.getOutput().trim());
        cacheLog.debug("systemd目录检查结果: %s", exists ? "存在" : "不存在");
        return exists;
    }

    @Override
    public ItemCode getCheckerType() {
        return ItemCode.FILE_HANDLE;
    }
}