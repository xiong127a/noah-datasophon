package com.datasophon.api.service.checker.checkers.filehandle;

import com.datasophon.api.config.CheckerProperties;
import com.datasophon.api.service.checker.core.AbstractItemChecker;
import com.datasophon.api.service.checker.common.CommandResult;
import com.datasophon.common.model.CheckItem;
import com.datasophon.common.model.HostInfo;
import com.datasophon.api.service.checker.common.ItemCode;
import com.datasophon.api.service.checker.helpers.HtmlStyleHelper;
import org.apache.sshd.client.session.ClientSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FileHandleChecker extends AbstractItemChecker {

    private static final Logger logger = LoggerFactory.getLogger(FileHandleChecker.class);

    private final CheckerProperties checkerProperties;

    @Override
    protected CheckItem doCheck(HostInfo hostInfo, CheckItem checkItem) {
        try {
            // 从配置中获取文件句柄数限制
            int minFileHandles = checkerProperties.getFileHandle().getMinLimit();

            cacheLog.debug("开始检查文件句柄数 - 主机: %s", hostInfo.getIp());
            cacheLog.debug("最小建议文件句柄数: %d", minFileHandles);
            cacheLog.info("正在检查系统文件句柄数限制配置...");

            // 更新状态为正在检查文件句柄数
            setCheckItemMessage(hostInfo, checkItem, "正在检查系统文件句柄数限制配置...");

            // 执行ulimit命令获取当前最大文件句柄数（仅供参考）
            cacheLog.debug("执行命令: ulimit -n（仅供参考）");
            cacheLog.info("执行命令: ulimit -n 获取当前会话文件句柄数（仅供参考）...");
            CommandResult result = execCommand(sshConnectionPoolManager.getOrCreateConnection(hostInfo), "ulimit -n");

            int currentFileHandles = 0;
            if (result.isSuccess()) {
                try {
                    currentFileHandles = Integer.parseInt(result.getOutput().trim());
                    cacheLog.info("当前会话文件句柄数: %s（仅供参考，不影响检查结果）", result.getOutput().trim());
                } catch (NumberFormatException e) {
                    cacheLog.warn("无法解析当前文件句柄数: %s", result.getOutput());
                }
            } else {
                cacheLog.debug("获取当前会话文件句柄数失败: %s", result.getErrorOrOutput());
            }

            // 检查/etc/security/limits.conf配置是否已设置
            cacheLog.info("检查系统配置文件是否已正确设置...");
            CommandResult limitsConfResult = execCommand(sshConnectionPoolManager.getOrCreateConnection(hostInfo),
                    "grep -E '\\* (soft|hard) nofile' /etc/security/limits.conf");
            boolean limitsConfSet = false;

            // 验证limits.conf配置是否至少包含目标值
            if (limitsConfResult.isSuccess() && !limitsConfResult.getOutput().isEmpty()) {
                String confOutput = limitsConfResult.getOutput();
                cacheLog.debug("文件句柄配置: %s", confOutput);

                boolean softSet = false;
                boolean hardSet = false;

                // 解析配置输出
                String[] lines = confOutput.split("\n");
                for (String line : lines) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }

                    if (line.contains("soft nofile")) {
                        String[] parts = line.split("\\s+");
                        if (parts.length >= 4) {
                            try {
                                int softLimit = Integer.parseInt(parts[3]);
                                softSet = softLimit >= minFileHandles;
                                cacheLog.debug("发现soft配置: %d, 是否满足: %s", softLimit, softSet ? "是" : "否");
                            } catch (NumberFormatException e) {
                                cacheLog.debug("无法解析soft配置: %s", line);
                            }
                        }
                    }

                    if (line.contains("hard nofile")) {
                        String[] parts = line.split("\\s+");
                        if (parts.length >= 4) {
                            try {
                                int hardLimit = Integer.parseInt(parts[3]);
                                hardSet = hardLimit >= minFileHandles;
                                cacheLog.debug("发现hard配置: %d, 是否满足: %s", hardLimit, hardSet ? "是" : "否");
                            } catch (NumberFormatException e) {
                                cacheLog.debug("无法解析hard配置: %s", line);
                            }
                        }
                    }
                }

                limitsConfSet = softSet && hardSet;
                cacheLog.info("文件句柄配置检查: soft配置: %s, hard配置: %s",
                        softSet ? "已设置且满足要求" : "未设置或不满足要求",
                        hardSet ? "已设置且满足要求" : "未设置或不满足要求");
            }

            // 检查结果只基于系统配置
            boolean success = limitsConfSet;

            if (success) {
                cacheLog.info("系统文件句柄配置已正确设置，检查通过");
                if (currentFileHandles < minFileHandles) {
                    cacheLog.info("注意: 当前会话文件句柄数 %d 小于最小建议值 %d，但系统配置已正确设置，重新登录后将生效",
                            currentFileHandles, minFileHandles);
                }
            } else {
                cacheLog.info("系统文件句柄配置未正确设置，检查未通过");
            }

            cacheLog.debug("当前会话文件句柄数: %d（仅供参考）", currentFileHandles);
            cacheLog.debug("系统配置是否已设置: %s", limitsConfSet ? "是" : "否");
            cacheLog.info("检查结果: 系统配置状态: %s", limitsConfSet ? "已正确配置" : "未正确配置");

            // 更新状态为分析结果
            setCheckItemMessage(hostInfo, checkItem, "正在分析系统文件句柄配置...");

            checkItem.setStatus(success ? CheckItem.Status.SUCCESS : CheckItem.Status.FAILED);

            // 创建HTML详细信息构建器
            StringBuilder detailsBuilder = new StringBuilder();

            // 添加文件句柄信息组
            detailsBuilder.append(HtmlStyleHelper.beginGroup());

            // 添加文件句柄数信息 - 当前会话值仅作为参考
            if (currentFileHandles > 0) {
                detailsBuilder.append(HtmlStyleHelper.generatePropertyRow(
                        "当前会话文件句柄数", String.valueOf(currentFileHandles) + " (仅供参考)", HtmlStyleHelper.Colors.GRAY));
            }

            detailsBuilder.append(HtmlStyleHelper.generatePropertyRow(
                    "最小建议文件句柄数", String.valueOf(minFileHandles), HtmlStyleHelper.Colors.INFO));

            // 系统配置状态是主要检查项
            detailsBuilder.append(HtmlStyleHelper.generatePropertyRow(
                    "系统配置状态", limitsConfSet ? "已正确配置" : "未配置或配置不正确",
                    limitsConfSet ? HtmlStyleHelper.Colors.SUCCESS : HtmlStyleHelper.Colors.ERROR));

            detailsBuilder.append(HtmlStyleHelper.endGroup());

            // 添加命令执行信息组
            detailsBuilder.append(HtmlStyleHelper.beginGroup());

            // 显示当前会话值（如果有）
            if (result.isSuccess()) {
                detailsBuilder.append("<p><strong>当前会话信息（仅供参考）:</strong></p>");
                detailsBuilder.append(HtmlStyleHelper.generateCodeBlock("$ ulimit -n\n" + result.getOutput().trim()));
            }

            // 如果配置已设置，显示配置信息
            if (limitsConfResult.isSuccess()) {
                detailsBuilder.append("<p><strong>系统配置信息:</strong></p>");
                if (limitsConfResult.getOutput().isEmpty()) {
                    detailsBuilder.append(HtmlStyleHelper.generateCodeBlock("未找到文件句柄限制相关配置"));
                } else {
                    detailsBuilder.append(HtmlStyleHelper.generateCodeBlock(limitsConfResult.getOutput()));
                }
            }

            detailsBuilder.append(HtmlStyleHelper.endGroup());

            // 添加检查结果提示
            if (success) {
                detailsBuilder.append(HtmlStyleHelper.generateSuccessAlert(
                        "系统文件句柄配置检查通过",
                        String.format("系统文件句柄限制已正确配置为不小于 %d，满足系统运行要求。%s",
                                minFileHandles,
                                currentFileHandles < minFileHandles ? "当前会话值仍可能较小，重新登录后生效。" : "")));
            } else {
                detailsBuilder.append(HtmlStyleHelper.generateWarningAlert(
                        "系统文件句柄配置检查未通过",
                        String.format("系统文件句柄限制未正确配置至少 %d，请点击修复按钮或手动修改系统配置。", minFileHandles)));
            }

            // 设置格式化的HTML消息
            setStyledHtmlMessage(hostInfo, checkItem, success, success ? "系统文件句柄配置检查通过" : "系统文件句柄配置检查未通过",
                    detailsBuilder);

            cacheLog.debug("检查结果: %s, 消息: %s", checkItem.getStatus(), checkItem.getMessage());
            cacheLog.info("系统文件句柄配置检查%s", success ? "通过" : "未通过");
        } catch (Exception e) {
            cacheLog.debug("检查过程异常: %s", e.getMessage());
            cacheLog.error("检查系统文件句柄配置过程中发生异常: %s", e.getMessage());
            checkItem.setStatus(CheckItem.Status.FAILED);
            setCheckItemMessage(hostInfo, checkItem, "检查失败: " + e.getMessage());
        }
        return checkItem;
    }

    @Override
    protected boolean doFix(HostInfo hostInfo, CheckItem checkItem) {
        try {
            // 从配置中获取文件句柄数限制
            int minFileHandles = checkerProperties.getFileHandle().getMinLimit();

            cacheLog.debug("开始修复系统文件句柄配置 - 主机: %s", hostInfo.getIp());

            // 更新状态为正在修改limits.conf文件
            setCheckItemMessage(hostInfo, checkItem, "正在修改系统文件句柄限制配置...");

            // 先获取当前文件句柄数（仅供参考）
            CommandResult currentLimitResult = execCommand(sshConnectionPoolManager.getOrCreateConnection(hostInfo), "ulimit -n");
            int currentLimit = 0;
            if (currentLimitResult.isSuccess()) {
                try {
                    currentLimit = Integer.parseInt(currentLimitResult.getOutput().trim());
                    cacheLog.info("当前会话文件句柄数限制: %d（仅供参考）", currentLimit);
                } catch (NumberFormatException e) {
                    cacheLog.warn("无法解析当前文件句柄数: %s", currentLimitResult.getOutput());
                }
            }

            // 修改 /etc/security/limits.conf 文件
            String cmd = String.format(
                    "grep -q '* soft nofile %d' /etc/security/limits.conf || echo '* soft nofile %d' >> /etc/security/limits.conf && "
                            +
                            "grep -q '* hard nofile %d' /etc/security/limits.conf || echo '* hard nofile %d' >> /etc/security/limits.conf",
                    minFileHandles, minFileHandles, minFileHandles, minFileHandles);

            cacheLog.debug("执行修复命令: %s", cmd);
            CommandResult result = execCommand(sshConnectionPoolManager.getOrCreateConnection(hostInfo), cmd);

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
            boolean hasSystemd = isSystemdExists(sshConnectionPoolManager.getOrCreateConnection(hostInfo));
            cacheLog.debug("systemd检查结果: %s", hasSystemd ? "存在" : "不存在");

            // 如果是CentOS/RHEL,还需要通过systemd配置
            if (hasSystemd) {
                // 更新状态为正在配置systemd
                setCheckItemMessage(hostInfo, checkItem, "正在配置systemd文件句柄限制...");

                cacheLog.debug("通过systemd配置文件句柄限制...");

                // 创建systemd配置目录
                cacheLog.debug("创建目录: /etc/systemd/system.conf.d");
                CommandResult mkdirResult = execCommand(sshConnectionPoolManager.getOrCreateConnection(hostInfo), "mkdir -p /etc/systemd/system.conf.d");
                if (!mkdirResult.isSuccess()) {
                    cacheLog.warn("创建systemd配置目录失败: %s", mkdirResult.getErrorOrOutput());
                }

                // 创建systemd配置文件
                String systemdConfig = "echo -e '[Manager]\\nDefaultLimitNOFILE=" + minFileHandles
                        + "' > /etc/systemd/system.conf.d/limits.conf";
                cacheLog.debug("配置systemd文件句柄限制: %s", systemdConfig);
                CommandResult systemdResult = execCommand(sshConnectionPoolManager.getOrCreateConnection(hostInfo), systemdConfig);

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
                CommandResult reloadResult = execCommand(sshConnectionPoolManager.getOrCreateConnection(hostInfo), "systemctl daemon-reload");
                cacheLog.debug("systemctl daemon-reload结果: %s",
                        reloadResult.getOutput().isEmpty() ? "成功" : reloadResult.getOutput());
            }

            cacheLog.debug("系统文件句柄限制修复完成, 需要用户重新登录生效");

            // 创建HTML详细信息构建器
            StringBuilder detailsBuilder = new StringBuilder();

            // 添加修复详情组
            detailsBuilder.append(HtmlStyleHelper.beginGroup());

            // 当前会话值信息（仅供参考）
            if (currentLimit > 0) {
                detailsBuilder.append("<p><strong>当前状态（仅供参考）:</strong></p>");
                detailsBuilder.append(HtmlStyleHelper.generatePropertyRow(
                        "当前会话文件句柄数", String.valueOf(currentLimit),
                        currentLimit >= minFileHandles ? HtmlStyleHelper.Colors.SUCCESS
                                : HtmlStyleHelper.Colors.GRAY));
            }

            detailsBuilder.append(HtmlStyleHelper.generatePropertyRow(
                    "系统配置目标值", String.valueOf(minFileHandles), HtmlStyleHelper.Colors.SUCCESS));

            // 添加修复步骤说明
            detailsBuilder.append("<p><strong>已完成的修复操作:</strong></p>");
            detailsBuilder.append("<ol style='padding-left:20px;margin-bottom:15px'>");
            detailsBuilder.append("<li style='margin-bottom:5px'>已修改系统配置文件 " +
                    HtmlStyleHelper.generateInlineCode("/etc/security/limits.conf") + " 添加以下配置:</li>");

            // 获取配置值用于显示
            int limitValue = checkerProperties.getFileHandle().getMinLimit();

            // 生成配置代码块
            String configCode = String.format("* soft nofile %d\n* hard nofile %d", limitValue, limitValue);
            detailsBuilder.append(HtmlStyleHelper.generateCodeBlock(configCode));

            // 添加systemd配置信息(如果适用)
            if (hasSystemd) {
                detailsBuilder.append("<li style='margin-bottom:5px'>检测到系统使用systemd，已添加systemd配置:</li>");
                String systemdConfigContent = String.format("[Manager]\nDefaultLimitNOFILE=%d", limitValue);
                detailsBuilder.append(HtmlStyleHelper.generateCodeBlock(systemdConfigContent));
                detailsBuilder.append("<li style='margin-bottom:5px'>已执行 " +
                        HtmlStyleHelper.generateInlineCode("systemctl daemon-reload") + " 重新加载配置</li>");
            }

            detailsBuilder.append("</ol>");
            detailsBuilder.append(HtmlStyleHelper.endGroup());

            // 添加修复完成，验证配置是否正确添加
            StringBuilder verificationCmd = new StringBuilder();
            verificationCmd.append("grep '\\* soft nofile' /etc/security/limits.conf | tail -n 1 && ");
            verificationCmd.append("grep '\\* hard nofile' /etc/security/limits.conf | tail -n 1");

            CommandResult verifyResult = execCommand(sshConnectionPoolManager.getOrCreateConnection(hostInfo), verificationCmd.toString());
            if (verifyResult.isSuccess() && !verifyResult.getOutput().isEmpty()) {
                detailsBuilder.append(HtmlStyleHelper.beginGroup());
                detailsBuilder.append("<p><strong>配置验证:</strong></p>");
                detailsBuilder.append(HtmlStyleHelper.generateCodeBlock(verifyResult.getOutput()));
                detailsBuilder.append(HtmlStyleHelper.endGroup());

                // 添加成功提示
                detailsBuilder.append(HtmlStyleHelper.generateSuccessAlert(
                        "系统配置修改成功",
                        "系统文件句柄限制配置已成功修改，新配置将在用户<strong>重新登录</strong>或<strong>重启系统</strong>后生效。"));
            }

            // 添加重要提示，重点强调
            detailsBuilder.append(HtmlStyleHelper.generateWarningAlert(
                    "生效说明",
                    "系统配置已修改完成，<strong>但只对新登录的会话有效</strong>。当前会话的文件句柄限制值不会改变，这是正常的。" +
                            "系统检查工具会根据系统配置判断检查状态，而不是当前会话值。"));

            // 设置格式化的HTML消息
            setStyledHtmlMessage(hostInfo, checkItem, true, "系统文件句柄限制已修改", detailsBuilder);

            // 修复操作成功
            return true;
        } catch (Exception e) {
            logger.error("修复文件句柄数失败: {}", e.getMessage());
            cacheLog.error("修复系统文件句柄配置失败: %s", e.getMessage());
            cacheLog.debug("修复过程异常详情: %s", e.toString());

            // 创建HTML详细信息构建器
            StringBuilder detailsBuilder = new StringBuilder();

            // 添加错误信息
            detailsBuilder.append(HtmlStyleHelper.generateWarningAlert(
                    "修复失败",
                    "修复系统文件句柄限制时发生错误: " + e.getMessage()));

            // 添加手动修复指南
            detailsBuilder.append(HtmlStyleHelper.beginGroup());
            detailsBuilder.append("<p><strong>手动修复步骤:</strong></p>");
            detailsBuilder.append("<ol style='padding-left:20px;margin-bottom:15px'>");
            detailsBuilder.append("<li style='margin-bottom:5px'>编辑系统配置文件 " +
                    HtmlStyleHelper.generateInlineCode("/etc/security/limits.conf") + " 添加以下配置:</li>");

            // 获取配置值用于显示
            int limitValue = checkerProperties.getFileHandle().getMinLimit();

            // 生成配置代码块
            String configCode = String.format("* soft nofile %d\n* hard nofile %d", limitValue, limitValue);
            detailsBuilder.append(HtmlStyleHelper.generateCodeBlock(configCode));

            detailsBuilder.append("<li style='margin-bottom:5px'>如果系统使用systemd，创建目录:</li>");
            detailsBuilder.append(HtmlStyleHelper.generateCodeBlock("mkdir -p /etc/systemd/system.conf.d"));

            detailsBuilder.append("<li style='margin-bottom:5px'>创建文件 " +
                    HtmlStyleHelper.generateInlineCode("/etc/systemd/system.conf.d/limits.conf") + " 内容如下:</li>");
            String systemdConfigContent = String.format("[Manager]\nDefaultLimitNOFILE=%d", limitValue);
            detailsBuilder.append(HtmlStyleHelper.generateCodeBlock(systemdConfigContent));

            detailsBuilder.append("<li style='margin-bottom:5px'>重新加载systemd配置:</li>");
            detailsBuilder.append(HtmlStyleHelper.generateCodeBlock("systemctl daemon-reload"));

            detailsBuilder.append("<li style='margin-bottom:5px'>重新登录或重启系统使配置生效</li>");
            detailsBuilder.append("</ol>");
            detailsBuilder.append(HtmlStyleHelper.endGroup());

            // 设置格式化的HTML消息
            setStyledHtmlMessage(hostInfo, checkItem, false, "修复系统文件句柄限制失败", detailsBuilder);

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