package com.datasophon.api.service.checker.checkers.firewall.os.kylin;

import com.datasophon.api.service.checker.checkers.firewall.os.centos.CentOSFirewallChecker;
import com.datasophon.api.service.checker.common.CommandResult;
import com.datasophon.api.service.checker.helpers.CheckLogger;
import com.datasophon.common.model.CheckItem;
import com.datasophon.common.model.HostInfo;
import org.apache.sshd.client.session.ClientSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 麒麟V10防火墙检查器
 * 专门处理麒麟V10系统的防火墙检查和修复
 * 麒麟V10基于更现代的架构，使用nftables和firewalld
 */
public class KylinV10FirewallChecker extends CentOSFirewallChecker {

    private static final Logger log = LoggerFactory.getLogger(KylinV10FirewallChecker.class);

    @Override
    public CheckItem check(HostInfo hostInfo, CheckItem checkItem, CheckLogger cacheLog) throws InterruptedException {
        cacheLog.info("==== 麒麟V10防火墙检查开始 ====");

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

        // 创建HTML输出
        StringBuilder htmlOutput = new StringBuilder();
        htmlOutput.append("<div class='firewall-check'>");
        htmlOutput.append("<h3>麒麟V10防火墙检查结果</h3>");

        // 检查麒麟系统版本信息
        CommandResult versionResult = execCommand(session, "cat /etc/*-release", cacheLog);
        if (versionResult.isSuccess()) {
            htmlOutput.append("<div class='section'>");
            htmlOutput.append("<h4>系统版本信息</h4>");
            htmlOutput.append("<pre class='command-output'>").append(versionResult.getOutput().trim()).append("</pre>");
            htmlOutput.append("</div>");
        }

        // 麒麟V10特有的检查
        checkKylinV10SpecificServices(session, cacheLog);

        // 确定防火墙类型
        FirewallType firewallType = detectFirewallType(session, cacheLog);
        htmlOutput.append("<div class='section'>");
        htmlOutput.append("<h4>防火墙类型检测</h4>");
        htmlOutput.append("<p>检测到防火墙类型: <span class='firewall-type'>").append(firewallType.name())
                .append("</span></p>");
        htmlOutput.append("</div>");

        // 执行具体的防火墙检查
        if (firewallType == FirewallType.FIREWALLD) {
            // 麒麟V10默认使用firewalld
            cacheLog.info("检测到麒麟V10使用firewalld防火墙(默认)");
            htmlOutput.append("<div class='section'>");
            htmlOutput.append("<h4>firewalld防火墙检查</h4>");

            // 检查firewalld服务状态
            CommandResult statusResult = execCommand(session, "systemctl status firewalld", cacheLog);
            htmlOutput.append("<div class='subsection'>");
            htmlOutput.append("<h5>服务状态</h5>");
            htmlOutput.append("<pre class='command-output'>").append(statusResult.getOutput().trim()).append("</pre>");
            htmlOutput.append("</div>");

            // 检查firewalld版本
            CommandResult versionCmdResult = execCommand(session, "firewall-cmd --version", cacheLog);
            htmlOutput.append("<div class='subsection'>");
            htmlOutput.append("<h5>版本信息</h5>");
            htmlOutput.append("<pre class='command-output'>").append(versionCmdResult.getOutput().trim())
                    .append("</pre>");
            htmlOutput.append("</div>");

            // 检查活动区域
            CommandResult zonesResult = execCommand(session, "firewall-cmd --get-active-zones", cacheLog);
            htmlOutput.append("<div class='subsection'>");
            htmlOutput.append("<h5>活动区域</h5>");
            htmlOutput.append("<pre class='command-output'>").append(zonesResult.getOutput().trim()).append("</pre>");
            htmlOutput.append("</div>");

            // 检查所有规则
            CommandResult rulesResult = execCommand(session, "firewall-cmd --list-all", cacheLog);
            htmlOutput.append("<div class='subsection'>");
            htmlOutput.append("<h5>防火墙规则</h5>");
            htmlOutput.append("<pre class='command-output'>").append(rulesResult.getOutput().trim()).append("</pre>");
            htmlOutput.append("</div>");

            // 检查开机自启状态
            CommandResult enabledResult = execCommand(session, "systemctl is-enabled firewalld", cacheLog);
            htmlOutput.append("<div class='subsection'>");
            htmlOutput.append("<h5>开机自启状态</h5>");
            htmlOutput.append("<pre class='command-output'>").append(enabledResult.getOutput().trim()).append("</pre>");
            htmlOutput.append("</div>");

            // 分析结果
            htmlOutput.append("<div class='analysis'>");
            htmlOutput.append("<h4>分析结果</h4>");
            boolean isRunning = statusResult.getOutput().contains("active (running)");
            boolean isEnabled = enabledResult.getOutput().trim().equals("enabled");

            if (isRunning || isEnabled) {
                checkItem.setStatus(CheckItem.Status.FAILED);
                htmlOutput.append("<p class='warning'>警告：防火墙服务正在运行或已启用开机自启，建议关闭</p>");
                htmlOutput.append("<div class='suggestions'>");
                htmlOutput.append("<h5>修复建议</h5>");
                htmlOutput.append("<ol>");
                htmlOutput.append("<li>停止防火墙服务：<code>systemctl stop firewalld</code></li>");
                htmlOutput.append("<li>禁用开机自启：<code>systemctl disable firewalld</code></li>");
                htmlOutput.append("</ol>");
                htmlOutput.append("</div>");
            } else {
                checkItem.setStatus(CheckItem.Status.SUCCESS);
                htmlOutput.append("<p class='success'>防火墙服务已停止且未启用开机自启</p>");
            }
            htmlOutput.append("</div>");
            htmlOutput.append("</div>");
        } else if (firewallType == FirewallType.IPTABLES) {
            // 处理iptables检查
            htmlOutput.append("<div class='section'>");
            htmlOutput.append("<h4>iptables防火墙检查</h4>");

            // 检查iptables服务状态
            CommandResult statusResult = execCommand(session, "systemctl status iptables", cacheLog);
            htmlOutput.append("<div class='subsection'>");
            htmlOutput.append("<h5>服务状态</h5>");
            htmlOutput.append("<pre class='command-output'>").append(statusResult.getOutput().trim()).append("</pre>");
            htmlOutput.append("</div>");

            // 检查iptables规则
            CommandResult rulesResult = execCommand(session, "iptables -L -n", cacheLog);
            htmlOutput.append("<div class='subsection'>");
            htmlOutput.append("<h5>防火墙规则</h5>");
            htmlOutput.append("<pre class='command-output'>").append(rulesResult.getOutput().trim()).append("</pre>");
            htmlOutput.append("</div>");

            // 分析结果
            htmlOutput.append("<div class='analysis'>");
            htmlOutput.append("<h4>分析结果</h4>");
            boolean hasRules = rulesResult.getOutput().contains("REJECT") || rulesResult.getOutput().contains("DROP");

            if (hasRules) {
                checkItem.setStatus(CheckItem.Status.FAILED);
                htmlOutput.append("<p class='warning'>警告：检测到iptables限制规则，建议清除</p>");
                htmlOutput.append("<div class='suggestions'>");
                htmlOutput.append("<h5>修复建议</h5>");
                htmlOutput.append("<ol>");
                htmlOutput.append("<li>清空所有规则：<code>iptables -F</code></li>");
                htmlOutput.append("<li>停止iptables服务：<code>systemctl stop iptables</code></li>");
                htmlOutput.append("<li>禁用开机自启：<code>systemctl disable iptables</code></li>");
                htmlOutput.append("</ol>");
                htmlOutput.append("</div>");
            } else {
                checkItem.setStatus(CheckItem.Status.SUCCESS);
                htmlOutput.append("<p class='success'>未检测到限制性iptables规则</p>");
            }
            htmlOutput.append("</div>");
            htmlOutput.append("</div>");
        } else {
            checkItem.setStatus(CheckItem.Status.SUCCESS);
            htmlOutput.append("<div class='section'>");
            htmlOutput.append("<h4>分析结果</h4>");
            htmlOutput.append("<p class='success'>未检测到防火墙服务</p>");
            htmlOutput.append("</div>");
        }

        htmlOutput.append("</div>");
        checkItem.setMessage(htmlOutput.toString());
        return checkItem;
    }

    @Override
    public boolean fix(HostInfo hostInfo, CheckItem checkItem, CheckLogger cacheLog) {
        cacheLog.info("==== 麒麟V10防火墙修复开始 ====");

        // 获取SSH会话
        ClientSession session = getSession(hostInfo, cacheLog);
        if (session == null) {
            String errorMsg = "SSH会话为空，无法执行命令";
            log.error(errorMsg);
            cacheLog.error(errorMsg);
            checkItem.setMessage(errorMsg);
            return false;
        }

        // 确定防火墙类型
        FirewallType firewallType = detectFirewallType(session, cacheLog);
        cacheLog.info("检测到麒麟V10防火墙类型: %s", firewallType.name());

        // 执行具体的防火墙修复
        if (firewallType == FirewallType.FIREWALLD) {
            // 麒麟V10默认使用firewalld
            cacheLog.info("检测到麒麟V10使用firewalld防火墙(默认)，正在修复...");
            return fixFirewalld(session, checkItem, cacheLog);
        } else if (firewallType == FirewallType.IPTABLES) {
            cacheLog.info("检测到麒麟V10使用iptables防火墙，正在修复...");

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
            checkItem.setMessage("未检测到防火墙服务，无需修复");
            return true;
        }
    }

    /**
     * 检查麒麟V10特有的服务
     */
    private void checkKylinV10SpecificServices(ClientSession session, CheckLogger cacheLog) {
        // 调用父类的通用检查
        checkCentOSSpecificServices(session, cacheLog);

        // 检查是否启用了nftables（麒麟V10默认使用nftables）
        CommandResult nftablesResult = execCommand(session, "which nft 2>/dev/null || echo 'Not Found'", cacheLog);
        if (nftablesResult.isSuccess() && !nftablesResult.getOutput().contains("Not Found")) {
            cacheLog.info("检测到nftables工具，麒麟V10可能使用nftables作为防火墙后端");

            // 检查nftables规则
            CommandResult nftRulesResult = execCommand(session, "nft list tables 2>/dev/null || echo 'No tables'",
                    cacheLog);
            if (nftRulesResult.isSuccess() && !nftRulesResult.getOutput().contains("No tables")) {
                cacheLog.info("nftables表信息: %s", nftRulesResult.getOutput().trim());
            }
        }

        // 检查麒麟特有的安全模块 - ksc
        CommandResult kscResult = execCommand(session, "which ksc 2>/dev/null || echo 'Not Found'", cacheLog);
        if (kscResult.isSuccess() && !kscResult.getOutput().contains("Not Found")) {
            cacheLog.info("检测到麒麟安全控制器(KSC)，这可能会影响防火墙规则");

            // 检查KSC状态
            CommandResult kscStatusResult = execCommand(session, "systemctl status ksc 2>/dev/null || echo 'Not Found'",
                    cacheLog);
            if (kscStatusResult.isSuccess() && !kscStatusResult.getOutput().contains("Not Found")) {
                if (kscStatusResult.getExitCode() == 0) {
                    cacheLog.warn("麒麟安全控制器(KSC)正在运行，这可能会影响防火墙管理");
                }
            }
        }

        // 检查麒麟V10特有的安全服务 - nscd
        CommandResult nscdResult = execCommand(session, "systemctl status nscd 2>/dev/null || echo 'Not Found'",
                cacheLog);
        if (nscdResult.isSuccess() && !nscdResult.getOutput().contains("Not Found")) {
            if (nscdResult.getExitCode() == 0) {
                cacheLog.info("检测到名称服务缓存守护进程(nscd)正在运行");
            }
        }

        // 检查麒麟V10特有的容器服务 - podman
        CommandResult podmanResult = execCommand(session, "which podman 2>/dev/null || echo 'Not Found'", cacheLog);
        if (podmanResult.isSuccess() && !podmanResult.getOutput().contains("Not Found")) {
            cacheLog.info("检测到podman容器工具，可能会使用iptables/nftables规则");

            // 检查podman是否正在运行容器
            CommandResult podmanPsResult = execCommand(session, "podman ps 2>/dev/null || echo 'Error'", cacheLog);
            if (podmanPsResult.isSuccess() && !podmanPsResult.getOutput().contains("Error")) {
                cacheLog.info("podman容器状态: %s", podmanPsResult.getOutput().trim());
            }
        }

        // 检查系统内核版本（麒麟V10可能有自定义内核）
        CommandResult kernelResult = execCommand(session, "uname -r", cacheLog);
        if (kernelResult.isSuccess()) {
            cacheLog.info("内核版本: %s", kernelResult.getOutput().trim());

            // 检查是否是麒麟定制内核
            if (kernelResult.getOutput().contains("kylin")) {
                cacheLog.info("检测到麒麟定制内核");
            }
        }
    }
}