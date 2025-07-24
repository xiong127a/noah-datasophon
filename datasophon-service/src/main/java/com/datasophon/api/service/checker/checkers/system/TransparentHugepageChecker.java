package com.datasophon.api.service.checker.checkers.system;

import com.datasophon.api.service.checker.common.CommandResult;
import com.datasophon.api.service.checker.common.ItemCode;
import com.datasophon.api.service.checker.core.AbstractItemChecker;
import com.datasophon.api.service.checker.helpers.HtmlStyleHelper;
import com.datasophon.common.model.CheckItem;
import com.datasophon.common.model.HostInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 透明大页检查器
 * 检查系统是否关闭了透明大页（Transparent Hugepage）功能
 * 透明大页会导致大数据组件性能不稳定
 */
@Component
@Slf4j
public class TransparentHugepageChecker extends AbstractItemChecker {

    @Override
    protected CheckItem doCheck(HostInfo hostInfo, CheckItem checkItem) {
        try {
            cacheLog.info("==== 开始透明大页检查 ====");
            cacheLog.info("主机: " + hostInfo.getIp());

            // 检查透明大页启用状态
            String checkEnabledCommand = "cat /sys/kernel/mm/transparent_hugepage/enabled";
            CommandResult enabledResult = execCommand(sshConnectionPoolManager.getOrCreateConnection(hostInfo),
                    checkEnabledCommand);

            // 检查透明大页碎片整理状态
            String checkDefragCommand = "cat /sys/kernel/mm/transparent_hugepage/defrag";
            CommandResult defragResult = execCommand(sshConnectionPoolManager.getOrCreateConnection(hostInfo),
                    checkDefragCommand);

            boolean isDisabled = false;

            if (enabledResult.isSuccess() && defragResult.isSuccess()) {
                String enabledStatus = enabledResult.getOutput().trim();
                String defragStatus = defragResult.getOutput().trim();

                cacheLog.info("透明大页enabled状态: " + enabledStatus);
                cacheLog.info("透明大页defrag状态: " + defragStatus);

                // 检查透明大页是否已关闭 (enabled文件中应该包含 [never])
                isDisabled = enabledStatus.contains("[never]") && defragStatus.contains("[never]");
            }

            if (isDisabled) {
                cacheLog.info("透明大页已正确关闭");
                checkItem.setStatus(CheckItem.Status.SUCCESS);
                StringBuilder successDetails = createSuccessDetails();
                setStyledHtmlMessage(hostInfo, checkItem, true, "透明大页检查通过", successDetails);
            } else {
                cacheLog.warn("透明大页未关闭，这可能导致系统性能不稳定");
                checkItem.setStatus(CheckItem.Status.FAILED);
                StringBuilder failDetails = createFailDetails();
                setStyledHtmlMessage(hostInfo, checkItem, false, "透明大页检查未通过", failDetails);
            }

            cacheLog.info("==== 透明大页检查结束 ====");
            return checkItem;
        } catch (Exception e) {
            log.error("检查透明大页时发生错误", e);
            cacheLog.error("检查过程中发生错误: " + e.getMessage());
            checkItem.setStatus(CheckItem.Status.FAILED);

            StringBuilder errorDetails = new StringBuilder();
            errorDetails.append(HtmlStyleHelper.generateWarningAlert("检查过程中发生错误", e.getMessage()));
            setStyledHtmlMessage(hostInfo, checkItem, false, "透明大页检查失败", errorDetails);
            return checkItem;
        }
    }

    @Override
    protected boolean doFix(HostInfo hostInfo, CheckItem checkItem) {
        try {
            cacheLog.info("==== 开始修复透明大页设置 ====");
            cacheLog.info("主机: " + hostInfo.getIp());

            // 临时关闭透明大页
            String disableCommand = "echo never > /sys/kernel/mm/transparent_hugepage/enabled && " +
                    "echo never > /sys/kernel/mm/transparent_hugepage/defrag";

            CommandResult disableResult = execCommand(sshConnectionPoolManager.getOrCreateConnection(hostInfo),
                    disableCommand);

            if (!disableResult.isSuccess()) {
                cacheLog.error("临时关闭透明大页失败: " + disableResult.getErrorOrOutput());
                return false;
            }

            cacheLog.info("已临时关闭透明大页");

            // 永久关闭透明大页，添加到rc.local
            String checkRcLocalCommand = "test -f /etc/rc.local && grep -q 'transparent_hugepage/enabled' /etc/rc.local; echo $?";
            CommandResult checkResult = execCommand(sshConnectionPoolManager.getOrCreateConnection(hostInfo),
                    checkRcLocalCommand);

            if (checkResult.isSuccess() && !"0".equals(checkResult.getOutput().trim())) {
                // 添加到rc.local
                String addToRcLocalCommand = "echo \"echo never > /sys/kernel/mm/transparent_hugepage/enabled\" >> /etc/rc.local && "
                        +
                        "echo \"echo never > /sys/kernel/mm/transparent_hugepage/defrag\" >> /etc/rc.local && " +
                        "chmod +x /etc/rc.local";

                CommandResult addResult = execCommand(sshConnectionPoolManager.getOrCreateConnection(hostInfo),
                        addToRcLocalCommand);

                if (!addResult.isSuccess()) {
                    cacheLog.error("添加到rc.local失败: " + addResult.getErrorOrOutput());
                    return false;
                }

                cacheLog.info("已添加透明大页禁用命令到rc.local文件");
            } else {
                cacheLog.info("rc.local文件已包含透明大页禁用命令");
            }

            // 再次检查以验证修复结果
            String verifyCommand = "cat /sys/kernel/mm/transparent_hugepage/enabled";
            CommandResult verifyResult = execCommand(sshConnectionPoolManager.getOrCreateConnection(hostInfo),
                    verifyCommand);

            if (verifyResult.isSuccess() && verifyResult.getOutput().contains("[never]")) {
                cacheLog.info("透明大页已成功关闭");
                return true;
            } else {
                cacheLog.error("验证透明大页关闭失败");
                return false;
            }
        } catch (Exception e) {
            log.error("修复透明大页设置时发生错误", e);
            cacheLog.error("修复过程中发生错误: " + e.getMessage());
            return false;
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
        sb.append("<div style=\"font-size: 18px; font-weight: 600; color: #1d1d1f; line-height: 1.4;\">透明大页状态</div>");
        sb.append("</div>");

        // 详细信息区域
        sb.append("<div style=\"margin-top: 12px;\">");
        sb.append("<div style=\"font-size: 15px; color: #1d1d1f; line-height: 1.5;\">");
        sb.append("透明大页已关闭，系统配置正确。<br>");
        sb.append("透明大页功能会导致大数据组件运行性能不稳定，已确认系统设置正确。");
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
        sb.append("<div style=\"font-size: 18px; font-weight: 600; color: #1d1d1f; line-height: 1.4;\">透明大页未关闭</div>");
        sb.append("</div>");

        // 详细信息区域
        sb.append("<div style=\"margin-top: 12px;\">");
        sb.append("<div style=\"font-size: 15px; color: #1d1d1f; line-height: 1.5;\">");
        sb.append("系统的透明大页功能未关闭，这可能会导致以下问题：<br>");
        sb.append("• 系统内存分配不均匀<br>");
        sb.append("• 大数据组件性能不稳定<br>");
        sb.append("• 内存碎片整理导致的随机延迟<br>");
        sb.append("• CPU使用率突然升高");
        sb.append("</div>");
        sb.append("</div>");

        // 修复建议
        sb.append("<div style=\"margin-top: 20px; padding-top: 16px; border-top: 1px solid rgba(0, 0, 0, 0.1);\">");
        sb.append("<div style=\"font-size: 13px; color: #86868b; line-height: 1.5;\">");
        sb.append("建议立即修复此问题。修复后需要持久化设置，以确保系统重启后设置仍然有效。");
        sb.append("</div>");
        sb.append("</div>");

        sb.append("</div>");
        return sb;
    }

    @Override
    public ItemCode getCheckerType() {
        return ItemCode.TRANSPARENT_HUGEPAGE_CHECK;
    }
}