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
 * Sudo命令检查器
 * 检查系统是否安装了sudo命令
 */
@Component
@Slf4j
public class SudoCommandChecker extends AbstractItemChecker {

    @Override
    protected CheckItem doCheck(HostInfo hostInfo, CheckItem checkItem) throws InterruptedException {
        try {
            cacheLog.info("==== 开始Sudo命令检查 ====");
            cacheLog.info("主机: " + hostInfo.getIp());

            // 检查sudo命令是否存在
            cacheLog.info("搜索sudo命令位置...");
            String command = "which sudo || whereis sudo | awk '{print $2}'";
            CommandResult result = execCommand(sshConnectionPoolManager.getOrCreateConnection(hostInfo), command);

            if (result.isSuccess() && !result.getOutput().trim().isEmpty()) {
                String sudoPath = result.getOutput().trim();
                cacheLog.info("找到sudo命令位置: " + sudoPath);

                // 检查sudo命令版本
                cacheLog.info("检查sudo命令版本...");
                String versionCommand = "sudo --version | head -n 1";
                CommandResult versionResult = execCommand(sshConnectionPoolManager.getOrCreateConnection(hostInfo),
                        versionCommand);
                if (versionResult.isSuccess()) {
                    cacheLog.info("sudo版本信息: " + versionResult.getOutput().trim());
                }

                checkItem.setStatus(CheckItem.Status.SUCCESS);
                setStyledHtmlMessage(hostInfo, checkItem, true, "Sudo命令检查通过", createSuccessDetails(sudoPath));
            } else {
                cacheLog.warn("未找到sudo命令");
                checkItem.setStatus(CheckItem.Status.FAILED);
                setStyledHtmlMessage(hostInfo, checkItem, false, "Sudo命令检查未通过", createFailDetails());
            }

            cacheLog.info("==== Sudo命令检查结束 ====");
            return checkItem;
        } catch (Exception e) {
            log.error("检查Sudo命令时发生错误", e);
            cacheLog.error("检查过程中发生错误: " + e.getMessage());
            checkItem.setStatus(CheckItem.Status.FAILED);
            StringBuilder errorDetails = new StringBuilder();
            errorDetails.append(HtmlStyleHelper.generateWarningAlert("检查过程中发生错误", e.getMessage()));
            setStyledHtmlMessage(hostInfo, checkItem, false, "Sudo命令检查失败", errorDetails);
            return checkItem;
        }
    }

    @Override
    protected boolean doFix(HostInfo hostInfo, CheckItem checkItem) {
        cacheLog.info("==== Sudo命令检查器不支持自动修复 ====");
        cacheLog.info("主机: " + hostInfo.getIp());
        cacheLog.warn("sudo命令的安装需要root权限，无法自动修复");
        return false;
    }

    /**
     * 创建成功详情消息
     */
    private StringBuilder createSuccessDetails(String sudoPath) {
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
                "<div style=\"font-size: 18px; font-weight: 600; color: #1d1d1f; line-height: 1.4;\">Sudo命令检查状态</div>");
        sb.append("</div>");

        // 详细信息区域
        sb.append("<div style=\"margin-top: 12px;\">");
        sb.append("<div style=\"font-size: 15px; color: #1d1d1f; line-height: 1.5;\">");
        sb.append("系统已安装sudo命令。<br>");
        sb.append("sudo路径: ").append(sudoPath);
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
                "<div style=\"font-size: 18px; font-weight: 600; color: #1d1d1f; line-height: 1.4;\">Sudo命令检查未通过</div>");
        sb.append("</div>");

        // 详细信息区域
        sb.append("<div style=\"margin-top: 12px;\">");
        sb.append("<div style=\"font-size: 15px; color: #1d1d1f; line-height: 1.5;\">");
        sb.append("系统未安装sudo命令。");
        sb.append("</div>");
        sb.append("</div>");

        // 修复建议
        sb.append("<div style=\"margin-top: 20px; padding-top: 16px; border-top: 1px solid rgba(0, 0, 0, 0.1);\">");
        sb.append("<div style=\"font-size: 13px; color: #86868b; line-height: 1.5;\">");
        sb.append("需要手动安装sudo命令：<br>");
        sb.append("<div style=\"margin-top: 8px;\">");
        sb.append("1. 对于Debian/Ubuntu系统：<br>");
        sb.append("<code style=\"background: rgba(0, 0, 0, 0.05); padding: 2px 4px; border-radius: 4px;\">");
        sb.append("apt-get update && apt-get install -y sudo");
        sb.append("</code><br><br>");
        sb.append("2. 对于CentOS/RHEL系统：<br>");
        sb.append("<code style=\"background: rgba(0, 0, 0, 0.05); padding: 2px 4px; border-radius: 4px;\">");
        sb.append("yum install -y sudo");
        sb.append("</code>");
        sb.append("</div>");
        sb.append("</div>");
        sb.append("</div>");

        sb.append("</div>");
        return sb;
    }

    @Override
    public ItemCode getCheckerType() {
        return ItemCode.SUDO_COMMAND_CHECK;
    }
}