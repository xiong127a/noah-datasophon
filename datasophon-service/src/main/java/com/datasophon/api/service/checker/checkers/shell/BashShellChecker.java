package com.datasophon.api.service.checker.checkers.shell;

import com.datasophon.api.service.checker.common.CommandResult;
import com.datasophon.api.service.checker.common.ItemCode;
import com.datasophon.api.service.checker.core.AbstractItemChecker;
import com.datasophon.api.service.checker.helpers.HtmlStyleHelper;
import com.datasophon.common.model.CheckItem;
import com.datasophon.common.model.HostInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Bash Shell检查器
 * 检查系统是否使用bash作为默认shell
 */
@Component
@Slf4j
public class BashShellChecker extends AbstractItemChecker {

    private String bashPath;

    @Override
    protected CheckItem doCheck(HostInfo hostInfo, CheckItem checkItem) throws InterruptedException {
        try {
            cacheLog.info("==== 开始Bash Shell检查 ====");
            cacheLog.info("主机: " + hostInfo.getIp());

            // 先搜索bash的位置
            cacheLog.info("搜索bash命令位置...");
            String searchBashCommand = "which bash || whereis bash | awk '{print $2}'";
            CommandResult searchResult = execCommand(sshConnectionPoolManager.getOrCreateConnection(hostInfo),
                    searchBashCommand);

            if (searchResult.isSuccess() && !searchResult.getOutput().trim().isEmpty()) {
                bashPath = searchResult.getOutput().trim();
                cacheLog.info("找到bash命令位置: " + bashPath);
            } else {
                // 如果which和whereis都找不到，尝试常见路径
                cacheLog.info("未找到bash命令，尝试常见路径...");
                String[] commonPaths = { "/usr/bin/bash", "/bin/bash", "/usr/local/bin/bash" };
                for (String path : commonPaths) {
                    String checkPathCommand = "test -x " + path + " && echo " + path;
                    CommandResult pathResult = execCommand(sshConnectionPoolManager.getOrCreateConnection(hostInfo),
                            checkPathCommand);
                    if (pathResult.isSuccess() && !pathResult.getOutput().trim().isEmpty()) {
                        bashPath = pathResult.getOutput().trim();
                        cacheLog.info("在常见路径找到bash: " + bashPath);
                        break;
                    }
                }
            }

            if (bashPath == null) {
                cacheLog.error("未找到bash命令");
                checkItem.setStatus(CheckItem.Status.FAILED);
                setStyledHtmlMessage(hostInfo, checkItem, false, "Bash Shell检查未通过", createFailDetails());
                return checkItem;
            }

            // 检查/bin/sh是否链接到bash
            cacheLog.info("检查/bin/sh是否链接到bash...");
            String checkCommand = "ls -l /bin/sh | grep bash";
            CommandResult result = execCommand(sshConnectionPoolManager.getOrCreateConnection(hostInfo), checkCommand);

            if (result.isSuccess()) {
                cacheLog.info("/bin/sh已链接到bash");
                checkItem.setStatus(CheckItem.Status.SUCCESS);
                setStyledHtmlMessage(hostInfo, checkItem, true, "Bash Shell检查通过", createSuccessDetails());
            } else {
                cacheLog.warn("/bin/sh未链接到bash");
                checkItem.setStatus(CheckItem.Status.FAILED);
                setStyledHtmlMessage(hostInfo, checkItem, false, "Bash Shell检查未通过", createFailDetails());
            }

            cacheLog.info("==== Bash Shell检查结束 ====");
            return checkItem;
        } catch (Exception e) {
            log.error("检查Bash Shell时发生错误", e);
            cacheLog.error("检查过程中发生错误: " + e.getMessage());
            checkItem.setStatus(CheckItem.Status.FAILED);
            StringBuilder errorDetails = new StringBuilder();
            errorDetails.append(HtmlStyleHelper.generateWarningAlert("检查过程中发生错误", e.getMessage()));
            setStyledHtmlMessage(hostInfo, checkItem, false, "Bash Shell检查失败", errorDetails);
            return checkItem;
        }
    }

    @Override
    protected boolean doFix(HostInfo hostInfo, CheckItem checkItem) {
        try {
            cacheLog.info("==== 开始修复Bash Shell ====");
            cacheLog.info("主机: " + hostInfo.getIp());

            if (bashPath == null) {
                cacheLog.error("未找到bash路径，无法修复");
                return false;
            }

            cacheLog.info("创建符号链接: /bin/sh -> " + bashPath);
            String fixCommand = "ln -sf " + bashPath + " /bin/sh";
            CommandResult result = execCommand(sshConnectionPoolManager.getOrCreateConnection(hostInfo), fixCommand);

            if (!result.isSuccess()) {
                cacheLog.error("创建符号链接失败: " + result.getErrorOrOutput());
                return false;
            }

            // 验证修复是否成功
            cacheLog.info("验证修复结果...");
            String verifyCommand = "ls -l /bin/sh | grep bash";
            CommandResult verifyResult = execCommand(sshConnectionPoolManager.getOrCreateConnection(hostInfo),
                    verifyCommand);

            if (verifyResult.isSuccess()) {
                cacheLog.info("修复成功");
                return true;
            } else {
                cacheLog.error("修复失败，验证未通过");
                return false;
            }
        } catch (Exception e) {
            log.error("修复Bash Shell时发生错误", e);
            cacheLog.error("修复过程中发生错误: " + e.getMessage());
            return false;
        } finally {
            cacheLog.info("==== Bash Shell修复结束 ====");
        }
    }

    /**
     * 创建成功详情消息
     */
    private StringBuilder createSuccessDetails() {
        StringBuilder sb = new StringBuilder();
        sb.append(
                "<div style=\"font-family: SF Pro Text, -apple-system, BlinkMacSystemFont, Helvetica Neue, Helvetica, Arial, sans-serif; ");
        sb.append("background: linear-gradient(to bottom, rgba(249, 249, 249, 0.95), rgba(244, 244, 244, 0.95)); ");
        sb.append("border-radius: 12px; box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08); ");
        sb.append("overflow: hidden; max-width: 100%; padding: 20px;\">");

        // 标题区域
        sb.append("<div style=\"display: flex; align-items: center; margin-bottom: 16px;\">");
        sb.append(
                "<div style=\"width: 12px; height: 12px; border-radius: 50%; background-color: #34c759; margin-right: 10px;\"></div>");
        sb.append(
                "<div style=\"font-size: 18px; font-weight: 600; color: #1d1d1f; line-height: 1.4;\">Bash Shell检查状态</div>");
        sb.append("</div>");

        // 详细信息区域
        sb.append("<div style=\"margin-top: 12px;\">");
        sb.append("<div style=\"font-size: 15px; color: #1d1d1f; line-height: 1.5;\">");
        sb.append("系统已正确配置bash作为默认shell。");
        sb.append("</div>");
        sb.append("</div>");

        sb.append("</div>");
        return sb;
    }

    /**
     * 创建失败详情消息
     */
    private StringBuilder createFailDetails() {
        StringBuilder sb = new StringBuilder();
        sb.append(
                "<div style=\"font-family: SF Pro Text, -apple-system, BlinkMacSystemFont, Helvetica Neue, Helvetica, Arial, sans-serif; ");
        sb.append("background: linear-gradient(to bottom, rgba(249, 249, 249, 0.95), rgba(244, 244, 244, 0.95)); ");
        sb.append("border-radius: 12px; box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08); ");
        sb.append("overflow: hidden; max-width: 100%; padding: 20px;\">");

        // 标题区域
        sb.append("<div style=\"display: flex; align-items: center; margin-bottom: 16px;\">");
        sb.append(
                "<div style=\"width: 12px; height: 12px; border-radius: 50%; background-color: #ff3b30; margin-right: 10px;\"></div>");
        sb.append(
                "<div style=\"font-size: 18px; font-weight: 600; color: #1d1d1f; line-height: 1.4;\">Bash Shell检查未通过</div>");
        sb.append("</div>");

        // 详细信息区域
        sb.append("<div style=\"margin-top: 12px;\">");
        sb.append("<div style=\"font-size: 15px; color: #1d1d1f; line-height: 1.5;\">");
        sb.append("系统未配置bash作为默认shell。");
        if (bashPath != null) {
            sb.append("<br>找到bash路径: ").append(bashPath);
        }
        sb.append("</div>");
        sb.append("</div>");

        // 修复建议
        sb.append("<div style=\"margin-top: 20px; padding-top: 16px; border-top: 1px solid rgba(0, 0, 0, 0.1);\">");
        sb.append("<div style=\"font-size: 13px; color: #86868b; line-height: 1.5;\">");
        sb.append("点击\"修复\"按钮自动创建符号链接。");
        sb.append("</div>");
        sb.append("</div>");

        sb.append("</div>");
        return sb;
    }

    @Override
    public ItemCode getCheckerType() {
        return ItemCode.BASH_SHELL_CHECK;
    }
}