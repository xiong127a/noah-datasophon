package com.datasophon.api.service.checker.checkers.firewall.os.centos;

import com.datasophon.api.service.checker.common.CommandResult;
import com.datasophon.api.service.checker.helpers.CheckLogger;
import com.datasophon.api.service.checker.checkers.firewall.generic.GenericFirewallChecker;
import com.datasophon.common.model.CheckItem;
import com.datasophon.common.model.HostInfo;
import org.apache.sshd.client.session.ClientSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CentOS系统防火墙检查器
 * 适用于所有CentOS版本的通用检查逻辑
 */
public class CentOSFirewallChecker extends GenericFirewallChecker {

    private static final Logger log = LoggerFactory.getLogger(CentOSFirewallChecker.class);

    @Override
    public CheckItem check(HostInfo hostInfo, CheckItem checkItem, CheckLogger cacheLog) throws InterruptedException {
        cacheLog.info("==== CentOS防火墙检查开始 ====");

        checkItem.setMessage("正在检查CentOS系统防火墙状态...");

        // 获取SSH会话
        ClientSession session = getSession(hostInfo, cacheLog);
        if (session == null) {
            String errorMsg = "SSH会话为空，无法执行命令";
            log.error(errorMsg);
            cacheLog.error(errorMsg);
            checkItem.setStatus(CheckItem.Status.FAILED);
            checkItem.setMessage(errorMsg);
            return checkItem;
        }

        // 首先检查是否安装了firewalld
        FirewallType firewallType = detectFirewallType(session, cacheLog);
        cacheLog.info("检测到CentOS防火墙类型: %s", firewallType.name());

        if (firewallType == FirewallType.FIREWALLD) {
            cacheLog.info("检测到CentOS使用firewalld防火墙");
            return checkFirewalld(session, checkItem, cacheLog);
        } else if (firewallType == FirewallType.IPTABLES) {
            cacheLog.info("检测到CentOS使用iptables防火墙");
            return checkIptables(session, checkItem, cacheLog);
        } else {
            cacheLog.info("未检测到防火墙服务");
            checkItem.setStatus(CheckItem.Status.SUCCESS);
            checkItem.setMessage("CentOS系统未检测到防火墙服务");
            return checkItem;
        }
    }

    @Override
    public boolean fix(HostInfo hostInfo, CheckItem checkItem, CheckLogger cacheLog) throws InterruptedException {
        cacheLog.info("==== CentOS防火墙修复开始 ====");

        checkItem.setMessage("正在修复CentOS系统防火墙配置...");

        // 获取SSH会话
        ClientSession session = getSession(hostInfo, cacheLog);
        if (session == null) {
            String errorMsg = "SSH会话为空，无法执行命令";
            log.error(errorMsg);
            cacheLog.error(errorMsg);
            checkItem.setMessage(errorMsg);
            return false;
        }

        // 首先检查是否安装了firewalld
        FirewallType firewallType = detectFirewallType(session, cacheLog);
        cacheLog.info("检测到CentOS防火墙类型: %s", firewallType.name());

        if (firewallType == FirewallType.FIREWALLD) {
            cacheLog.info("检测到CentOS使用firewalld防火墙，正在修复...");
            return fixFirewalld(session, checkItem, cacheLog);
        } else if (firewallType == FirewallType.IPTABLES) {
            cacheLog.info("检测到CentOS使用iptables防火墙，正在修复...");

            // 先检查是否有iptables服务
            CommandResult serviceResult = execCommand(session,
                    "systemctl status iptables 2>/dev/null || echo 'Not Found'", cacheLog);
            if (serviceResult.isSuccess() && !serviceResult.getOutput().contains("Not Found")) {
                // 停止并禁用iptables服务
                cacheLog.info("正在停止iptables服务...");
                execCommand(session, "systemctl stop iptables", cacheLog);

                cacheLog.info("正在禁用iptables服务自启动...");
                execCommand(session, "systemctl disable iptables", cacheLog);
            }

            // 清空iptables规则
            return fixIptables(session, checkItem, cacheLog);
        } else {
            cacheLog.info("未检测到防火墙服务，无需修复");
            checkItem.setMessage("CentOS系统未检测到防火墙服务，无需修复");
            return true;
        }
    }

    /**
     * 检查CentOS系统的firewalld防火墙
     */
    protected CheckItem checkFirewalld(ClientSession session, CheckItem checkItem, CheckLogger cacheLog) {
        cacheLog.info("执行检查命令: systemctl status firewalld");
        CommandResult result = execCommand(session, "systemctl status firewalld", cacheLog);

        // 获取系统实际类型，用于显示正确的系统名称
        CommandResult osTypeResult = execCommand(session, "cat /etc/os-release | grep -i \"^NAME=\"", cacheLog);
        String osTypeStr = osTypeResult.isSuccess() ? osTypeResult.getOutput() : "";
        String osName = "Linux";

        if (osTypeStr.toLowerCase().contains("kylin")) {
            osName = "银河麒麟";
        } else if (osTypeStr.toLowerCase().contains("centos")) {
            osName = "CentOS";
        } else if (osTypeStr.toLowerCase().contains("ubuntu")) {
            osName = "Ubuntu";
        } else {
            cacheLog.info("无法确定系统类型，使用默认系统名称");
        }

        // 创建HTML格式的消息
        StringBuilder message = new StringBuilder();
        message.append("<div style='line-height:1.6'>");

        // 添加命令执行信息
        message.append("<div style='margin-bottom:15px'>");
        message.append("<p><strong>执行命令:</strong></p>");
        message.append("<pre style='background:#002b36;color:#839496;padding:10px;border-radius:4px;margin:5px 0'>");
        message.append("$ systemctl status firewalld");
        message.append("</pre>");
        message.append("<p><strong>命令输出:</strong></p>");
        message.append("<pre style='background:#002b36;color:#839496;padding:10px;border-radius:4px;margin:5px 0'>");
        message.append(result.getOutput().isEmpty() ? result.getError() : result.getOutput());
        message.append("</pre>");
        message.append("</div>");

        switch (result.getExitCode()) {
            case 0:
                // 服务正在运行
                cacheLog.info(osName + " firewalld状态: 正在运行");
                message.append("<h3 style='color:#f5222d;margin-bottom:10px'>" + osName + " 防火墙状态: 正在运行</h3>");

                // 获取防火墙详细配置
                message.append("<div style='margin-bottom:15px'>");
                message.append("<p><strong>执行命令:</strong></p>");
                message.append(
                        "<pre style='background:#002b36;color:#839496;padding:10px;border-radius:4px;margin:5px 0'>");
                message.append("$ firewall-cmd --get-active-zones");
                message.append("</pre>");

                CommandResult zoneResult = execCommand(session, "firewall-cmd --get-active-zones", cacheLog);
                if (zoneResult.isSuccess()) {
                    message.append("<p><strong>活动区域:</strong></p>");
                    message.append(
                            "<pre style='background:#002b36;color:#839496;padding:10px;border-radius:4px;margin:5px 0'>");
                    message.append(zoneResult.getOutput());
                    message.append("</pre>");
                }

                message.append("<p><strong>执行命令:</strong></p>");
                message.append(
                        "<pre style='background:#002b36;color:#839496;padding:10px;border-radius:4px;margin:5px 0'>");
                message.append("$ firewall-cmd --list-all");
                message.append("</pre>");

                CommandResult configResult = execCommand(session, "firewall-cmd --list-all", cacheLog);
                if (configResult.isSuccess()) {
                    message.append("<p><strong>防火墙配置:</strong></p>");
                    message.append(
                            "<pre style='background:#002b36;color:#839496;padding:10px;border-radius:4px;margin:5px 0'>");
                    message.append(configResult.getOutput());
                    message.append("</pre>");
                }
                message.append("</div>");

                // 检查防火墙版本
                message.append("<div style='margin-bottom:15px'>");
                message.append("<p><strong>执行命令:</strong></p>");
                message.append(
                        "<pre style='background:#002b36;color:#839496;padding:10px;border-radius:4px;margin:5px 0'>");
                message.append("$ firewall-cmd --version");
                message.append("</pre>");

                CommandResult versionResult = execCommand(session, "firewall-cmd --version", cacheLog);
                if (versionResult.isSuccess()) {
                    message.append("<p><strong>防火墙版本:</strong></p>");
                    message.append(
                            "<pre style='background:#002b36;color:#839496;padding:10px;border-radius:4px;margin:5px 0'>");
                    message.append(versionResult.getOutput());
                    message.append("</pre>");
                }
                message.append("</div>");

                // 添加警告信息
                message.append(
                        "<div style='background:#fff2f0;border-left:4px solid #f5222d;padding:10px;border-radius:0 4px 4px 0;'>");
                message.append("<p style='margin:0;color:#f5222d;font-weight:bold'>警告: 防火墙正在运行</p>");
                message.append("<p style='margin-top:5px;margin-bottom:0;'>建议关闭防火墙以确保集群节点之间的正常通信。</p>");
                message.append("<p style='margin-top:5px;margin-bottom:0;'>可以执行以下命令关闭防火墙：</p>");
                message.append(
                        "<pre style='background:#002b36;color:#839496;padding:10px;border-radius:4px;margin:5px 0'>");
                message.append("$ systemctl stop firewalld\n");
                message.append("$ systemctl disable firewalld");
                message.append("</pre>");
                message.append("</div>");

                checkItem.setStatus(CheckItem.Status.FAILED);
                break;

            case 3:
                // 服务已停止
                cacheLog.info(osName + " firewalld状态: 已停止");
                message.append("<h3 style='color:#52c41a;margin-bottom:10px'>" + osName + " 防火墙状态: 已停止</h3>");

                // 检查自启动状态
                message.append("<div style='margin-bottom:15px'>");
                message.append("<p><strong>执行命令:</strong></p>");
                message.append(
                        "<pre style='background:#002b36;color:#839496;padding:10px;border-radius:4px;margin:5px 0'>");
                message.append("$ systemctl is-enabled firewalld");
                message.append("</pre>");

                CommandResult enabledResult = execCommand(session,
                        "systemctl is-enabled firewalld 2>/dev/null || echo 'Unknown'", cacheLog);
                message.append("<p><strong>自启动状态:</strong></p>");
                message.append(
                        "<pre style='background:#002b36;color:#839496;padding:10px;border-radius:4px;margin:5px 0'>");
                message.append(enabledResult.getOutput());
                message.append("</pre>");
                message.append("</div>");

                if (enabledResult.isSuccess() && enabledResult.getOutput().trim().equals("enabled")) {
                    cacheLog.info(osName + " firewalld自启动状态: 已启用");
                    message.append(
                            "<div style='background:#fff2f0;border-left:4px solid #f5222d;padding:10px;border-radius:0 4px 4px 0;'>");
                    message.append("<p style='margin:0;color:#f5222d;font-weight:bold'>警告: 防火墙已配置为自启动</p>");
                    message.append("<p style='margin-top:5px;margin-bottom:0;'>建议禁用防火墙自启动，以防止系统重启后防火墙自动启动。</p>");
                    message.append("<p style='margin-top:5px;margin-bottom:0;'>可以执行以下命令禁用自启动：</p>");
                    message.append(
                            "<pre style='background:#002b36;color:#839496;padding:10px;border-radius:4px;margin:5px 0'>");
                    message.append("$ systemctl disable firewalld");
                    message.append("</pre>");
                    message.append("</div>");
                    checkItem.setStatus(CheckItem.Status.FAILED);
                } else {
                    message.append(
                            "<div style='background:#f6ffed;border-left:4px solid #52c41a;padding:10px;border-radius:0 4px 4px 0;'>");
                    message.append("<p style='margin:0;color:#52c41a;font-weight:bold'>防火墙检查通过</p>");
                    message.append("<p style='margin-top:5px;margin-bottom:0;'>防火墙已关闭且未配置自启动，符合集群要求。</p>");
                    message.append("</div>");
                    checkItem.setStatus(CheckItem.Status.SUCCESS);
                }
                break;

            case 4:
                // 服务不存在
                cacheLog.info(osName + " firewalld状态: 服务不存在");
                message.append("<h3 style='color:#52c41a;margin-bottom:10px'>" + osName + " 防火墙状态: 未安装</h3>");
                message.append(
                        "<div style='background:#f6ffed;border-left:4px solid #52c41a;padding:10px;border-radius:0 4px 4px 0;'>");
                message.append("<p style='margin:0;color:#52c41a;font-weight:bold'>防火墙检查通过</p>");
                message.append("<p style='margin-top:5px;margin-bottom:0;'>系统未安装防火墙服务，符合集群要求。</p>");
                message.append("</div>");
                checkItem.setStatus(CheckItem.Status.SUCCESS);
                break;

            default:
                // 其他状态，可能是命令执行出错
                cacheLog.warn("获取" + osName + " firewalld防火墙状态失败，退出状态码: %d", result.getExitCode());
                message.append("<h3 style='color:#f5222d;margin-bottom:10px'>" + osName + " 防火墙检查失败</h3>");
                message.append(
                        "<div style='background:#fff2f0;border-left:4px solid #f5222d;padding:10px;border-radius:0 4px 4px 0;'>");
                message.append("<p style='margin:0;color:#f5222d;font-weight:bold'>错误: 无法获取防火墙状态</p>");
                message.append("<p style='margin-top:5px;margin-bottom:0;'>请手动检查防火墙状态。</p>");
                message.append("<p style='margin-top:5px;margin-bottom:0;'>可以尝试执行以下命令：</p>");
                message.append(
                        "<pre style='background:#002b36;color:#839496;padding:10px;border-radius:4px;margin:5px 0'>");
                message.append("$ systemctl status firewalld\n");
                message.append("$ firewall-cmd --state\n");
                message.append("$ firewall-cmd --list-all");
                message.append("</pre>");
                message.append("</div>");
                checkItem.setStatus(CheckItem.Status.FAILED);
                break;
        }

        message.append("</div>");
        checkItem.setMessage(message.toString());
        return checkItem;
    }

    /**
     * 检查CentOS系统的iptables防火墙
     */
    protected CheckItem checkIptables(ClientSession session, CheckItem checkItem, CheckLogger cacheLog)
            throws InterruptedException {
        // 获取系统实际类型，用于显示正确的系统名称
        CommandResult osTypeResult = execCommand(session, "cat /etc/os-release | grep -i \"^NAME=\"", cacheLog);
        String osTypeStr = osTypeResult.isSuccess() ? osTypeResult.getOutput() : "";
        String osName = "Linux";

        if (osTypeStr.toLowerCase().contains("kylin")) {
            osName = "银河麒麟";
        } else if (osTypeStr.toLowerCase().contains("centos")) {
            osName = "CentOS";
        } else if (osTypeStr.toLowerCase().contains("ubuntu")) {
            osName = "Ubuntu";
        } else {
            cacheLog.info("无法确定系统类型，使用默认系统名称");
        }

        cacheLog.info("执行检查命令: service iptables status");
        CommandResult result = execCommand(session, "service iptables status 2>/dev/null || echo 'Not Found'",
                cacheLog);

        if (result.isSuccess() && !result.getOutput().contains("Not Found")) {
            String output = result.getOutput().toLowerCase();

            if (output.contains("not running") || output.contains("stopped")) {
                // 服务已停止
                cacheLog.info(osName + " iptables状态: 已停止");

                // 检查自启动状态
                CommandResult chkconfigResult = execCommand(session,
                        "chkconfig --list iptables | grep on || echo 'Not enabled'", cacheLog);
                if (chkconfigResult.isSuccess() && !chkconfigResult.getOutput().contains("Not enabled")) {
                    cacheLog.info(osName + " iptables自启动状态: 已启用");
                    checkItem.setStatus(CheckItem.Status.FAILED);
                    checkItem.setMessage(osName + "系统iptables防火墙已配置为自启动，建议禁用");
                } else {
                    checkItem.setStatus(CheckItem.Status.SUCCESS);
                    checkItem.setMessage(osName + "系统iptables防火墙已关闭");
                }
            } else if (output.contains("running")) {
                // 服务正在运行
                cacheLog.info(osName + " iptables状态: 正在运行");

                // 获取iptables规则
                CommandResult rulesResult = execCommand(session, "iptables -L", cacheLog);
                if (rulesResult.isSuccess()) {
                    cacheLog.info("当前iptables规则:");
                    cacheLog.info(rulesResult.getOutput());
                }

                checkItem.setStatus(CheckItem.Status.FAILED);
                checkItem.setMessage(osName + "系统iptables防火墙正在运行，建议关闭");
            } else {
                // 状态不明
                cacheLog.warn("无法确定" + osName + " iptables状态: %s", output);
                checkItem.setStatus(CheckItem.Status.FAILED);
                checkItem.setMessage("无法确定" + osName + "系统iptables防火墙状态，请手动检查");
            }
        } else {
            // 尝试直接检查iptables规则
            return super.checkIptables(session, checkItem, cacheLog);
        }

        return checkItem;
    }

    /**
     * 修复CentOS系统的firewalld防火墙
     */
    protected boolean fixFirewalld(ClientSession session, CheckItem checkItem, CheckLogger cacheLog) {
        // 获取系统实际类型，用于显示正确的系统名称
        CommandResult osTypeResult = execCommand(session, "cat /etc/os-release | grep -i \"^NAME=\"", cacheLog);
        String osTypeStr = osTypeResult.isSuccess() ? osTypeResult.getOutput() : "";
        String osName = "Linux";

        if (osTypeStr.toLowerCase().contains("kylin")) {
            osName = "银河麒麟";
        } else if (osTypeStr.toLowerCase().contains("centos")) {
            osName = "CentOS";
        } else if (osTypeStr.toLowerCase().contains("ubuntu")) {
            osName = "Ubuntu";
        } else {
            cacheLog.info("无法确定系统类型，使用默认系统名称");
        }

        // 更新状态为正在检查防火墙当前状态
        checkItem.setMessage("正在检查" + osName + "系统firewalld防火墙当前状态...");

        // 检查firewalld状态
        cacheLog.info("检查" + osName + "系统firewalld防火墙当前状态...");
        CommandResult statusResult = execCommand(session, "systemctl status firewalld", cacheLog);

        // 如果服务不存在，直接返回成功
        if (statusResult.getExitCode() == 4) {
            cacheLog.info(osName + "系统firewalld防火墙服务未安装，无需修复");
            checkItem.setMessage(osName + "系统firewalld防火墙服务未安装，无需修复");
            return true;
        }

        // 如果服务已停止，检查是否已禁用自启动
        if (statusResult.getExitCode() == 3) {
            // 更新状态为正在检查自启动状态
            checkItem.setMessage("正在检查" + osName + "系统firewalld防火墙自启动状态...");

            CommandResult isEnabledResult = execCommand(session, "systemctl is-enabled firewalld", cacheLog);
            if (isEnabledResult.isSuccess() && isEnabledResult.getOutput().trim().equals("disabled")) {
                cacheLog.info(osName + "系统firewalld防火墙已关闭且已禁用自启动，无需修复");
                checkItem.setMessage(osName + "系统firewalld防火墙已关闭且已禁用自启动");
                return true;
            }
        }

        // 更新状态为正在停止服务
        checkItem.setMessage("正在停止" + osName + "系统firewalld防火墙服务...");

        // 停止服务
        cacheLog.info("执行命令: systemctl stop firewalld");
        CommandResult stopResult = execCommand(session, "systemctl stop firewalld", cacheLog);

        if (!stopResult.isSuccess()) {
            cacheLog.error("停止" + osName + "系统firewalld防火墙服务失败: %s", stopResult.getErrorOrOutput());
            checkItem.setMessage("停止" + osName + "系统firewalld防火墙服务失败: " + stopResult.getErrorOrOutput());
            return false;
        }

        // 更新状态为正在禁用自启动
        checkItem.setMessage("正在禁用" + osName + "系统firewalld防火墙自启动...");

        // 禁用自启动
        cacheLog.info("执行命令: systemctl disable firewalld");
        CommandResult disableResult = execCommand(session, "systemctl disable firewalld", cacheLog);

        if (!disableResult.isSuccess()) {
            cacheLog.error("禁用" + osName + "系统firewalld防火墙自启动失败: %s", disableResult.getErrorOrOutput());
            checkItem.setMessage("禁用" + osName + "系统firewalld防火墙自启动失败: " + disableResult.getErrorOrOutput());
            return false;
        }

        // 更新状态为正在验证
        checkItem.setMessage("正在验证" + osName + "系统firewalld防火墙状态...");

        // 验证服务状态
        cacheLog.info("执行命令: systemctl status firewalld");
        CommandResult verifyResult = execCommand(session, "systemctl status firewalld", cacheLog);

        if (verifyResult.getExitCode() == 3) {
            cacheLog.info(osName + "系统firewalld防火墙已成功停止");
            checkItem.setMessage(osName + "系统firewalld防火墙已成功停止并禁用自启动");
            return true;
        } else {
            cacheLog.error("验证" + osName + "系统firewalld防火墙状态失败，服务可能仍在运行");
            checkItem.setMessage("验证" + osName + "系统firewalld防火墙状态失败，服务可能仍在运行");
            return false;
        }
    }

    /**
     * 修复CentOS系统的iptables防火墙
     */
    protected boolean fixIptables(ClientSession session, CheckItem checkItem, CheckLogger cacheLog) {
        // 更新状态为正在检查iptables当前状态
        checkItem.setMessage("正在检查CentOS系统iptables防火墙当前状态...");

        // 先检查iptables服务状态
        cacheLog.info("检查CentOS系统iptables防火墙当前状态...");
        CommandResult statusResult = execCommand(session, "service iptables status 2>/dev/null || echo 'Not Found'",
                cacheLog);

        if (statusResult.isSuccess() && !statusResult.getOutput().contains("Not Found")) {
            if (!statusResult.getOutput().toLowerCase().contains("not running") &&
                    !statusResult.getOutput().toLowerCase().contains("stopped")) {

                // 停止iptables服务
                cacheLog.info("正在停止CentOS系统iptables服务...");
                checkItem.setMessage("正在停止CentOS系统iptables服务...");

                CommandResult stopResult = execCommand(session, "service iptables stop", cacheLog);
                if (!stopResult.isSuccess()) {
                    cacheLog.error("停止CentOS系统iptables服务失败: %s", stopResult.getErrorOrOutput());
                    checkItem.setMessage("停止CentOS系统iptables服务失败: " + stopResult.getErrorOrOutput());
                    // 继续尝试清空规则，不直接返回失败
                } else {
                    cacheLog.info("停止CentOS系统iptables服务完成");
                }

                // 禁用iptables自启动
                cacheLog.info("正在禁用CentOS系统iptables服务自启动...");
                checkItem.setMessage("正在禁用CentOS系统iptables服务自启动...");

                CommandResult chkconfigResult = execCommand(session, "chkconfig iptables off", cacheLog);
                if (!chkconfigResult.isSuccess()) {
                    cacheLog.error("禁用CentOS系统iptables服务自启动失败: %s", chkconfigResult.getErrorOrOutput());
                    checkItem.setMessage("禁用CentOS系统iptables服务自启动失败: " + chkconfigResult.getErrorOrOutput());
                    // 继续尝试清空规则，不直接返回失败
                } else {
                    cacheLog.info("禁用CentOS系统iptables服务自启动完成");
                }
            }
        }

        // 清空所有iptables规则
        cacheLog.info("正在清空CentOS系统iptables规则...");
        checkItem.setMessage("正在清空CentOS系统iptables规则...");

        // 设置默认策略为ACCEPT
        String[] chains = { "INPUT", "FORWARD", "OUTPUT" };
        for (String chain : chains) {
            CommandResult policyResult = execCommand(session, "iptables -P " + chain + " ACCEPT", cacheLog);
            if (!policyResult.isSuccess()) {
                cacheLog.error("设置CentOS系统iptables %s链默认策略失败: %s", chain, policyResult.getErrorOrOutput());
                checkItem.setMessage("设置CentOS系统iptables " + chain + "链默认策略失败: " + policyResult.getErrorOrOutput());
                // 继续尝试其他链
            }
        }

        // 清空所有规则
        CommandResult flushResult = execCommand(session, "iptables -F", cacheLog);
        if (!flushResult.isSuccess()) {
            cacheLog.error("清空CentOS系统iptables规则失败: %s", flushResult.getErrorOrOutput());
            checkItem.setMessage("清空CentOS系统iptables规则失败: " + flushResult.getErrorOrOutput());
            return false;
        }

        // 清空所有自定义链
        CommandResult chainResult = execCommand(session, "iptables -X", cacheLog);
        if (!chainResult.isSuccess()) {
            cacheLog.error("清空CentOS系统iptables自定义链失败: %s", chainResult.getErrorOrOutput());
            checkItem.setMessage("清空CentOS系统iptables自定义链失败: " + chainResult.getErrorOrOutput());
            // 不直接返回失败，因为可能没有自定义链
        }

        // 验证iptables规则已清空
        cacheLog.info("正在验证CentOS系统iptables规则...");
        checkItem.setMessage("正在验证CentOS系统iptables规则...");

        CommandResult verifyResult = execCommand(session, "iptables -L", cacheLog);
        if (verifyResult.isSuccess() &&
                verifyResult.getOutput().contains("Chain INPUT (policy ACCEPT)") &&
                verifyResult.getOutput().contains("Chain FORWARD (policy ACCEPT)") &&
                verifyResult.getOutput().contains("Chain OUTPUT (policy ACCEPT)") &&
                !verifyResult.getOutput().contains("REJECT") &&
                !verifyResult.getOutput().contains("DROP")) {

            cacheLog.info("验证成功: CentOS系统iptables规则已清空");
            checkItem.setMessage("CentOS系统iptables防火墙已成功关闭并清空规则");
            return true;
        } else {
            cacheLog.warn("警告: CentOS系统iptables规则可能未成功清空，请手动检查");
            checkItem.setMessage("警告: CentOS系统iptables规则可能未成功清空，请手动检查");
            return false;
        }
    }

    /**
     * 提供额外的CentOS特定检查
     */
    protected void checkCentOSSpecificServices(ClientSession session, CheckLogger cacheLog) {
        // 检查NetworkManager防火墙服务
        CommandResult nmResult = execCommand(session,
                "systemctl status NetworkManager-wait-online.service 2>/dev/null || echo 'Not Found'", cacheLog);
        if (nmResult.isSuccess() && !nmResult.getOutput().contains("Not Found")) {
            cacheLog.info("检测到NetworkManager服务，该服务可能影响网络配置");
        }

        // 检查是否安装了fail2ban
        CommandResult fail2banResult = execCommand(session, "which fail2ban-server 2>/dev/null || echo 'Not Found'",
                cacheLog);
        if (fail2banResult.isSuccess() && !fail2banResult.getOutput().contains("Not Found")) {
            cacheLog.info("检测到fail2ban服务，该服务可能会影响SSH连接");

            CommandResult fail2banStatusResult = execCommand(session,
                    "systemctl status fail2ban 2>/dev/null || echo 'Not Found'", cacheLog);
            if (fail2banStatusResult.isSuccess() && !fail2banStatusResult.getOutput().contains("Not Found")) {
                if (fail2banStatusResult.getExitCode() == 0) {
                    cacheLog.warn("fail2ban服务正在运行，可能会影响SSH连接");
                }
            }
        }
    }
}