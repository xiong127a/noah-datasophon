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
 * 连字符函数名检查器
 * 检查系统是否支持带连字符的函数名
 */
@Component
@Slf4j
public class HyphenFunctionChecker extends AbstractItemChecker {

    @Override
    protected CheckItem doCheck(HostInfo hostInfo, CheckItem checkItem) {
        try {
            cacheLog.info("==== 开始连字符函数名检查 ====");
            cacheLog.info("主机: " + hostInfo.getIp());

            // 创建一个测试脚本
            String testScript = "test-hyphen-function() { echo 'SUCCESS'; }; test-hyphen-function";
            String command = "bash -c '" + testScript + "' 2>&1";

            cacheLog.info("执行测试脚本...");
            CommandResult result = execCommand(sshConnectionPoolManager.getOrCreateConnection(hostInfo), command);

            if (result.isSuccess() && "SUCCESS".equals(result.getOutput().trim())) {
                // 检查当前shell类型
                cacheLog.info("检查当前shell类型...");
                String shellCommand = "echo $SHELL";
                CommandResult shellResult = execCommand(sshConnectionPoolManager.getOrCreateConnection(hostInfo),
                        shellCommand);
                String shellType = shellResult.isSuccess() ? shellResult.getOutput().trim() : "未知";

                // 检查bash版本
                cacheLog.info("检查bash版本...");
                String versionCommand = "bash --version | head -n 1";
                CommandResult versionResult = execCommand(sshConnectionPoolManager.getOrCreateConnection(hostInfo),
                        versionCommand);
                String versionInfo = versionResult.isSuccess() ? versionResult.getOutput().trim() : "未知";

                cacheLog.info("连字符函数名检查通过");
                cacheLog.info("当前shell: " + shellType);
                cacheLog.info("bash版本: " + versionInfo);

                checkItem.setStatus(CheckItem.Status.SUCCESS);
                setStyledHtmlMessage(hostInfo, checkItem, true, "连字符函数名检查通过",
                        createSuccessDetails(shellType, versionInfo));
            } else {
                cacheLog.warn("系统不支持带连字符的函数名");
                checkItem.setStatus(CheckItem.Status.FAILED);
                setStyledHtmlMessage(hostInfo, checkItem, false, "连字符函数名检查未通过",
                        createFailDetails(result.getErrorOrOutput()));
            }

            cacheLog.info("==== 连字符函数名检查结束 ====");
            return checkItem;
        } catch (Exception e) {
            log.error("检查连字符函数名时发生错误", e);
            cacheLog.error("检查过程中发生错误: " + e.getMessage());
            checkItem.setStatus(CheckItem.Status.FAILED);
            StringBuilder errorDetails = new StringBuilder();
            errorDetails.append(HtmlStyleHelper.generateWarningAlert("检查过程中发生错误", e.getMessage()));
            setStyledHtmlMessage(hostInfo, checkItem, false, "连字符函数名检查失败", errorDetails);
            return checkItem;
        }
    }

    @Override
    protected boolean doFix(HostInfo hostInfo, CheckItem checkItem) {
        cacheLog.info("==== 连字符函数名检查器不支持自动修复 ====");
        cacheLog.info("主机: " + hostInfo.getIp());
        cacheLog.warn("需要手动修改脚本中的函数名，将连字符替换为下划线");
        return false;
    }

    /**
     * 创建成功详情消息
     */
    private StringBuilder createSuccessDetails(String shellType, String versionInfo) {
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
                "<div style=\"font-size: 18px; font-weight: 600; color: #1d1d1f; line-height: 1.4;\">连字符函数名检查状态</div>");
        sb.append("</div>");

        // 详细信息区域
        sb.append("<div style=\"margin-top: 12px;\">");
        sb.append("<div style=\"font-size: 15px; color: #1d1d1f; line-height: 1.5;\">");
        sb.append("系统支持带连字符的函数名。<br>");
        sb.append("当前shell: ").append(shellType).append("<br>");
        sb.append("bash版本: ").append(versionInfo);
        sb.append("</div>");
        sb.append("</div>");

        sb.append("</div>");
        return sb;
    }

    /**
     * 创建失败详情消息
     */
    private StringBuilder createFailDetails(String errorMessage) {
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
                "<div style=\"font-size: 18px; font-weight: 600; color: #1d1d1f; line-height: 1.4;\">连字符函数名检查未通过</div>");
        sb.append("</div>");

        // 详细信息区域
        sb.append("<div style=\"margin-top: 12px;\">");
        sb.append("<div style=\"font-size: 15px; color: #1d1d1f; line-height: 1.5;\">");
        sb.append("系统不支持带连字符的函数名。<br>");
        sb.append("错误信息: ").append(errorMessage);
        sb.append("</div>");
        sb.append("</div>");

        // 修复建议
        sb.append("<div style=\"margin-top: 20px; padding-top: 16px; border-top: 1px solid rgba(0, 0, 0, 0.1);\">");
        sb.append("<div style=\"font-size: 13px; color: #86868b; line-height: 1.5;\">");
        sb.append("需要修改脚本中的函数名：<br>");
        sb.append("<div style=\"margin-top: 8px;\">");
        sb.append("1. 将带连字符的函数名改为使用下划线：<br>");
        sb.append("<code style=\"background: rgba(0, 0, 0, 0.05); padding: 2px 4px; border-radius: 4px;\">");
        sb.append("test-hyphen-function -> test_hyphen_function");
        sb.append("</code><br><br>");
        sb.append("2. 确保脚本使用bash执行：<br>");
        sb.append("<code style=\"background: rgba(0, 0, 0, 0.05); padding: 2px 4px; border-radius: 4px;\">");
        sb.append("#!/bin/bash");
        sb.append("</code>");
        sb.append("</div>");
        sb.append("</div>");
        sb.append("</div>");

        sb.append("</div>");
        return sb;
    }

    @Override
    public ItemCode getCheckerType() {
        return ItemCode.HYPHEN_FUNCTION_CHECK;
    }
}