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
 * 检查系统是否安装了sudo命令且能正常执行
 */
@Component
@Slf4j
public class SudoCommandChecker extends AbstractItemChecker {

    @Override
    protected CheckItem doCheck(HostInfo hostInfo, CheckItem checkItem) throws InterruptedException {
        try {
            cacheLog.info("==== 开始Sudo命令检查 ====");
            cacheLog.info("主机: " + hostInfo.getIp());

            // 检查sudo命令是否存在且可执行
            cacheLog.info("检查sudo命令是否可执行...");
            String command = "sudo -V >/dev/null 2>&1 && echo 'SUDO_OK' || echo 'SUDO_FAIL'";
            CommandResult result = execCommand(sshConnectionPoolManager.getOrCreateConnection(hostInfo), command);

            if (result.isSuccess() && "SUDO_OK".equals(result.getOutput().trim())) {
                // 获取sudo命令路径
                cacheLog.info("获取sudo命令路径...");
                String pathCommand = "which sudo";
                CommandResult pathResult = execCommand(sshConnectionPoolManager.getOrCreateConnection(hostInfo),
                        pathCommand);
                String sudoPath = pathResult.isSuccess() ? pathResult.getOutput().trim() : "未知";

                // 检查sudo命令版本
                cacheLog.info("检查sudo命令版本...");
                String versionCommand = "sudo --version | head -n 1";
                CommandResult versionResult = execCommand(sshConnectionPoolManager.getOrCreateConnection(hostInfo),
                        versionCommand);
                String versionInfo = versionResult.isSuccess() ? versionResult.getOutput().trim() : "未知";

                cacheLog.info("sudo命令检查通过");
                cacheLog.info("sudo路径: " + sudoPath);
                cacheLog.info("sudo版本: " + versionInfo);

                checkItem.setStatus(CheckItem.Status.SUCCESS);
                setStyledHtmlMessage(hostInfo, checkItem, true, "Sudo命令检查通过",
                        createSuccessDetails(sudoPath, versionInfo));
            } else {
                cacheLog.warn("sudo命令不可用或未安装");
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
    private StringBuilder createSuccessDetails(String sudoPath, String versionInfo) {
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
        sb.append("系统已安装且可正常使用sudo命令。<br>");
        sb.append("sudo路径: ").append(sudoPath).append("<br>");
        sb.append("sudo版本: ").append(versionInfo);
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
        sb.append("系统未安装sudo命令或sudo命令不可用。");
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