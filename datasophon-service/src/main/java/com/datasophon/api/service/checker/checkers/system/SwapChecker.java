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
 * 交换分区检查器
 * 检查系统是否关闭了交换分区（Swap）
 * 大数据集群建议关闭交换分区以提高性能
 */
@Component
@Slf4j
public class SwapChecker extends AbstractItemChecker {

    @Override
    protected CheckItem doCheck(HostInfo hostInfo, CheckItem checkItem) {
        try {
            cacheLog.info("==== 开始交换分区检查 ====");
            cacheLog.info("主机: " + hostInfo.getIp());

            // 检查交换分区状态
            String checkSwapCommand = "cat /proc/swaps | tail -n +2";
            CommandResult swapResult = execCommand(sshConnectionPoolManager.getOrCreateConnection(hostInfo),
                    checkSwapCommand);

            // 检查系统中的交换分区是否启用 (swappiness 参数)
            String checkSwappinessCommand = "cat /proc/sys/vm/swappiness";
            CommandResult swappinessResult = execCommand(sshConnectionPoolManager.getOrCreateConnection(hostInfo),
                    checkSwappinessCommand);

            boolean isSwapDisabled = false;
            boolean isSwappinessZero = false;
            String swapDetails = "";
            String swappinessValue = "";

            if (swapResult.isSuccess()) {
                swapDetails = swapResult.getOutput().trim();
                // 如果swaps文件中除了标题行外没有内容，说明没有激活的交换分区
                isSwapDisabled = swapDetails.isEmpty();
                cacheLog.info("交换分区状态: " + (isSwapDisabled ? "已关闭" : "已启用"));
            }

            if (swappinessResult.isSuccess()) {
                swappinessValue = swappinessResult.getOutput().trim();
                isSwappinessZero = "0".equals(swappinessValue);
                cacheLog.info("交换积极度(swappiness): " + swappinessValue);
            }

            // 判断检查结果
            if (isSwapDisabled && isSwappinessZero) {
                // 完全关闭了交换分区
                cacheLog.info("交换分区已完全关闭");
                checkItem.setStatus(CheckItem.Status.SUCCESS);
                StringBuilder successDetails = createSuccessDetails(swappinessValue);
                setStyledHtmlMessage(hostInfo, checkItem, true, "交换分区检查通过", successDetails);
            } else if (isSwapDisabled) {
                // 交换分区已关闭，但swappiness不为0
                cacheLog.warn("交换分区已关闭，但交换积极度(swappiness)不为0");
                checkItem.setStatus(CheckItem.Status.FAILED);
                StringBuilder partialFailDetails = createPartialFailDetails(swappinessValue);
                setStyledHtmlMessage(hostInfo, checkItem, false, "交换分区配置不完整", partialFailDetails);
            } else {
                // 交换分区未关闭
                cacheLog.warn("交换分区未关闭，可能影响系统性能");
                checkItem.setStatus(CheckItem.Status.FAILED);
                StringBuilder failDetails = createFailDetails(swapDetails, swappinessValue);
                setStyledHtmlMessage(hostInfo, checkItem, false, "交换分区检查未通过", failDetails);
            }

            cacheLog.info("==== 交换分区检查结束 ====");
            return checkItem;
        } catch (Exception e) {
            log.error("检查交换分区时发生错误", e);
            cacheLog.error("检查过程中发生错误: " + e.getMessage());
            checkItem.setStatus(CheckItem.Status.FAILED);

            StringBuilder errorDetails = new StringBuilder();
            errorDetails.append(HtmlStyleHelper.generateWarningAlert("检查过程中发生错误", e.getMessage()));
            setStyledHtmlMessage(hostInfo, checkItem, false, "交换分区检查失败", errorDetails);
            return checkItem;
        }
    }

    @Override
    protected boolean doFix(HostInfo hostInfo, CheckItem checkItem) {
        try {
            cacheLog.info("==== 开始修复交换分区设置 ====");
            cacheLog.info("主机: " + hostInfo.getIp());

            // 1. 关闭当前所有交换分区
            String disableSwapCommand = "swapoff -a";
            CommandResult disableResult = execCommand(sshConnectionPoolManager.getOrCreateConnection(hostInfo),
                    disableSwapCommand);

            if (!disableResult.isSuccess()) {
                cacheLog.error("关闭交换分区失败: " + disableResult.getErrorOrOutput());
                return false;
            }

            cacheLog.info("已临时关闭所有交换分区");

            // 2. 设置swappiness为0
            String setSwappinessCommand = "sysctl -w vm.swappiness=0";
            CommandResult swappinessResult = execCommand(sshConnectionPoolManager.getOrCreateConnection(hostInfo),
                    setSwappinessCommand);

            if (!swappinessResult.isSuccess()) {
                cacheLog.error("设置swappiness为0失败: " + swappinessResult.getErrorOrOutput());
                return false;
            }

            cacheLog.info("已临时设置vm.swappiness=0");

            // 3. 永久关闭交换分区 (修改/etc/fstab)
            // 备份fstab文件
            String backupCommand = "cp /etc/fstab /etc/fstab.bak.$(date +%Y%m%d%H%M%S)";
            execCommand(sshConnectionPoolManager.getOrCreateConnection(hostInfo), backupCommand);

            // 注释掉swap相关行
            String commentSwapCommand = "sed -i '/swap/s/^/#/g' /etc/fstab";
            CommandResult commentResult = execCommand(sshConnectionPoolManager.getOrCreateConnection(hostInfo),
                    commentSwapCommand);

            if (!commentResult.isSuccess()) {
                cacheLog.error("修改/etc/fstab注释掉swap分区失败: " + commentResult.getErrorOrOutput());
                // 不直接返回失败，因为这不是关键步骤
                cacheLog.warn("需要手动修改/etc/fstab文件，注释掉交换分区相关行");
            } else {
                cacheLog.info("已在/etc/fstab中注释掉交换分区配置");
            }

            // 4. 永久设置swappiness
            String checkSysctlCommand = "grep -q 'vm.swappiness' /etc/sysctl.conf; echo $?";
            CommandResult checkResult = execCommand(sshConnectionPoolManager.getOrCreateConnection(hostInfo),
                    checkSysctlCommand);

            if (checkResult.isSuccess() && !"0".equals(checkResult.getOutput().trim())) {
                // 添加到sysctl.conf
                String addSwappinessCommand = "echo 'vm.swappiness=0' >> /etc/sysctl.conf";
                CommandResult addResult = execCommand(sshConnectionPoolManager.getOrCreateConnection(hostInfo),
                        addSwappinessCommand);

                if (!addResult.isSuccess()) {
                    cacheLog.error("添加vm.swappiness=0到/etc/sysctl.conf失败: " + addResult.getErrorOrOutput());
                    cacheLog.warn("需要手动修改/etc/sysctl.conf文件，添加vm.swappiness=0配置");
                } else {
                    cacheLog.info("已添加vm.swappiness=0配置到/etc/sysctl.conf");
                }
            } else {
                // 更新已有配置
                String updateSwappinessCommand = "sed -i 's/vm.swappiness=.*/vm.swappiness=0/g' /etc/sysctl.conf";
                CommandResult updateResult = execCommand(sshConnectionPoolManager.getOrCreateConnection(hostInfo),
                        updateSwappinessCommand);

                if (!updateResult.isSuccess()) {
                    cacheLog.error("更新/etc/sysctl.conf的vm.swappiness设置失败: " + updateResult.getErrorOrOutput());
                    cacheLog.warn("需要手动修改/etc/sysctl.conf文件，更新vm.swappiness=0配置");
                } else {
                    cacheLog.info("已更新/etc/sysctl.conf中的vm.swappiness为0");
                }
            }

            // 5. 应用sysctl配置
            String applySysctlCommand = "sysctl -p";
            CommandResult applyResult = execCommand(sshConnectionPoolManager.getOrCreateConnection(hostInfo),
                    applySysctlCommand);

            if (!applyResult.isSuccess()) {
                cacheLog.error("应用sysctl配置失败: " + applyResult.getErrorOrOutput());
                return false;
            }

            cacheLog.info("已应用系统配置更改");

            // 验证修复结果
            // 检查交换分区状态
            String verifySwapCommand = "cat /proc/swaps | tail -n +2";
            CommandResult verifySwapResult = execCommand(sshConnectionPoolManager.getOrCreateConnection(hostInfo),
                    verifySwapCommand);

            // 检查swappiness设置
            String verifySwappinessCommand = "cat /proc/sys/vm/swappiness";
            CommandResult verifySwappinessResult = execCommand(sshConnectionPoolManager.getOrCreateConnection(hostInfo),
                    verifySwappinessCommand);

            boolean isSwapDisabled = verifySwapResult.isSuccess() && verifySwapResult.getOutput().trim().isEmpty();
            boolean isSwappinessZero = verifySwappinessResult.isSuccess()
                    && "0".equals(verifySwappinessResult.getOutput().trim());

            if (isSwapDisabled && isSwappinessZero) {
                cacheLog.info("交换分区设置已成功修复");
                return true;
            } else {
                cacheLog.error("交换分区设置修复验证失败");
                if (!isSwapDisabled) {
                    cacheLog.error("交换分区未成功关闭");
                }
                if (!isSwappinessZero) {
                    cacheLog.error("swappiness参数未成功设置为0");
                }
                return false;
            }

        } catch (Exception e) {
            log.error("修复交换分区设置时发生错误", e);
            cacheLog.error("修复过程中发生错误: " + e.getMessage());
            return false;
        }
    }

    /**
     * 创建成功详情消息
     */
    private StringBuilder createSuccessDetails(String swappinessValue) {
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
        sb.append("<div style=\"font-size: 18px; font-weight: 600; color: #1d1d1f; line-height: 1.4;\">交换分区状态</div>");
        sb.append("</div>");

        // 详细信息区域
        sb.append("<div style=\"margin-top: 12px;\">");
        sb.append("<div style=\"font-size: 15px; color: #1d1d1f; line-height: 1.5;\">");
        sb.append("交换分区已关闭，系统配置正确。<br>");
        sb.append("当前交换积极度(swappiness)值: <strong>").append(swappinessValue).append("</strong><br>");
        sb.append("这将确保系统不会使用磁盘作为虚拟内存，有助于提高大数据应用的性能。");
        sb.append("</div>");
        sb.append("</div>");

        sb.append("</div>");
        return sb;
    }

    /**
     * 创建部分失败详情消息 (swappiness不为0)
     */
    private StringBuilder createPartialFailDetails(String swappinessValue) {
        StringBuilder sb = new StringBuilder();
        sb.append(
                "<div style=\"font-family: SF Pro Text, -apple-system, BlinkMacSystemFont, Helvetica Neue, Helvetica, Arial, sans-serif; ");
        sb.append("background: linear-gradient(to bottom, rgba(249, 249, 249, 0.95), rgba(244, 244, 244, 0.95)); ");
        sb.append("border-radius: 12px; box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08); ");
        sb.append("overflow: hidden; max-width: 100%; padding: 20px;\">");

        // 标题区域
        sb.append("<div style=\"display: flex; align-items: center; margin-bottom: 16px;\">");
        sb.append(
                "<div style=\"width: 12px; height: 12px; border-radius: 50%; background-color: #ff9500; margin-right: 10px;\"></div>");
        sb.append(
                "<div style=\"font-size: 18px; font-weight: 600; color: #1d1d1f; line-height: 1.4;\">交换分区配置不完整</div>");
        sb.append("</div>");

        // 详细信息区域
        sb.append("<div style=\"margin-top: 12px;\">");
        sb.append("<div style=\"font-size: 15px; color: #1d1d1f; line-height: 1.5;\">");
        sb.append("交换分区已关闭，但系统配置不完整。<br>");
        sb.append("当前交换积极度(swappiness)值: <strong>").append(swappinessValue).append("</strong><br>");
        sb.append("建议将swappiness设置为0，以完全禁用内存页面交换。");
        sb.append("</div>");
        sb.append("</div>");

        // 修复建议
        sb.append("<div style=\"margin-top: 20px; padding-top: 16px; border-top: 1px solid rgba(0, 0, 0, 0.1);\">");
        sb.append("<div style=\"font-size: 13px; color: #86868b; line-height: 1.5;\">");
        sb.append("点击\"修复\"按钮将永久设置交换积极度为0:<br>");
        sb.append("• 临时设置: <code>sysctl -w vm.swappiness=0</code><br>");
        sb.append("• 永久设置: 修改/etc/sysctl.conf，添加vm.swappiness=0");
        sb.append("</div>");
        sb.append("</div>");

        sb.append("</div>");
        return sb;
    }

    /**
     * 创建失败详情消息
     */
    private StringBuilder createFailDetails(String swapDetails, String swappinessValue) {
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
        sb.append("<div style=\"font-size: 18px; font-weight: 600; color: #1d1d1f; line-height: 1.4;\">交换分区未关闭</div>");
        sb.append("</div>");

        // 详细信息区域
        sb.append("<div style=\"margin-top: 12px;\">");
        sb.append("<div style=\"font-size: 15px; color: #1d1d1f; line-height: 1.5;\">");
        sb.append("系统的交换分区功能未关闭，这可能会导致以下问题：<br>");
        sb.append("• 大数据应用性能不稳定<br>");
        sb.append("• 数据处理速度变慢<br>");
        sb.append("• 内存页面交换造成的随机延迟<br>");
        sb.append("• 磁盘I/O负载增加");
        sb.append("</div>");

        // 当前设置详情
        sb.append(
                "<div style=\"margin-top: 16px; background: rgba(0, 0, 0, 0.03); border-radius: 8px; padding: 12px;\">");
        sb.append(
                "<div style=\"font-size: 14px; font-weight: 600; color: #1d1d1f; margin-bottom: 8px;\">当前交换分区设置:</div>");
        if (!swapDetails.isEmpty()) {
            sb.append("<pre style=\"margin: 0; font-size: 12px; color: #333; line-height: 1.4; overflow-x: auto;\">");
            sb.append(swapDetails);
            sb.append("</pre>");
        } else {
            sb.append("<div style=\"font-size: 12px; color: #333;\">无法获取交换分区详情</div>");
        }
        sb.append("<div style=\"font-size: 14px; margin-top: 8px;\">交换积极度(swappiness): <strong>")
                .append(swappinessValue).append("</strong></div>");
        sb.append("</div>");
        sb.append("</div>");

        // 修复建议
        sb.append("<div style=\"margin-top: 20px; padding-top: 16px; border-top: 1px solid rgba(0, 0, 0, 0.1);\">");
        sb.append("<div style=\"font-size: 13px; color: #86868b; line-height: 1.5;\">");
        sb.append("建议立即修复此问题。修复操作包括:<br>");
        sb.append("• 关闭所有交换分区: <code>swapoff -a</code><br>");
        sb.append("• 设置交换积极度为0: <code>sysctl -w vm.swappiness=0</code><br>");
        sb.append("• 修改/etc/fstab禁用交换分区的自动挂载<br>");
        sb.append("• 修改/etc/sysctl.conf确保重启后保持设置");
        sb.append("</div>");
        sb.append("</div>");

        sb.append("</div>");
        return sb;
    }

    @Override
    public ItemCode getCheckerType() {
        return ItemCode.SWAP_CHECK;
    }
}