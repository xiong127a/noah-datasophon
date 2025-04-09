package com.datasophon.api.service.checker.checkers.firewall.generic;

import com.datasophon.api.service.checker.checkers.firewall.FirewallCheckerStrategy;
import com.datasophon.api.service.checker.common.CommandResult;
import com.datasophon.api.service.checker.common.SshConnectionPoolManager;
import com.datasophon.api.service.checker.helpers.CheckLogger;
import com.datasophon.common.enums.OsDistribution;
import com.datasophon.common.model.CheckItem;
import com.datasophon.common.model.HostInfo;
import org.apache.sshd.client.session.ClientSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 通用防火墙检查器
 * 提供基本的防火墙检查和修复功能
 */
public class GenericFirewallChecker implements FirewallCheckerStrategy {

    private static final Logger log = LoggerFactory.getLogger(GenericFirewallChecker.class);

    // 注入SSH连接池管理器
    @Autowired
    protected SshConnectionPoolManager sshConnectionPoolManager;

    // 支持的操作系统类型
    private OsDistribution supportedOs;

    // 版本前缀（可选）
    private String versionPrefix;

    /**
     * 防火墙类型枚举
     */
    protected enum FirewallType {
        FIREWALLD, // CentOS 7/8, RHEL 7/8, Kylin
        UFW, // Ubuntu
        IPTABLES, // 传统防火墙
        NONE // 未检测到防火墙
    }

    /**
     * 获取支持的操作系统类型
     */
    @Override
    public OsDistribution getSupportedOs() {
        return supportedOs;
    }

    /**
     * 设置支持的操作系统类型
     */
    @Override
    public void setSupportedOs(OsDistribution osDistribution) {
        this.supportedOs = osDistribution;
    }

    /**
     * 获取版本前缀
     */
    @Override
    public String getVersionPrefix() {
        return versionPrefix;
    }

    /**
     * 设置版本前缀
     */
    @Override
    public void setVersionPrefix(String versionPrefix) {
        this.versionPrefix = versionPrefix;
    }

    /**
     * 获取SSH会话
     * 使用连接池管理器获取SSH会话
     * 
     * @param hostInfo 主机信息
     * @param cacheLog 日志记录器
     * @return SSH会话
     */
    protected ClientSession getSession(HostInfo hostInfo, CheckLogger cacheLog) {
        try {
            if (sshConnectionPoolManager == null) {
                String errorMsg = "SSH连接池管理器未注入，无法获取SSH会话";
                log.error(errorMsg);
                cacheLog.error(errorMsg);
                return null;
            }

            // 使用连接池管理器获取SSH会话
            ClientSession session = sshConnectionPoolManager.getOrCreateConnection(hostInfo);
            if (session == null) {
                String errorMsg = "无法获取SSH会话：主机 " + hostInfo.getIp();
                log.error(errorMsg);
                cacheLog.error(errorMsg);
                return null;
            }

            return session;
        } catch (Exception e) {
            String errorMsg = "获取SSH会话时发生异常: " + e.getMessage();
            log.error(errorMsg, e);
            cacheLog.error(errorMsg);
            return null;
        }
    }

    @Override
    public CheckItem check(HostInfo hostInfo, CheckItem checkItem, CheckLogger cacheLog) throws InterruptedException {
        cacheLog.info("==== 通用防火墙检查开始 ====");

        checkItem.setMessage("正在检查防火墙状态...");

        // 获取SSH会话
        ClientSession session = getSession(hostInfo, cacheLog);
        if (session == null || !session.isOpen()) {
            String errorMsg = "SSH会话为空或已关闭，无法执行命令";
            log.error(errorMsg);
            cacheLog.error(errorMsg);
            checkItem.setStatus(CheckItem.Status.FAILED);
            checkItem.setMessage(errorMsg);
            return checkItem;
        }

        // 确定防火墙类型
        FirewallType firewallType = detectFirewallType(session, cacheLog);
        cacheLog.info("检测到防火墙类型: %s", firewallType.name());

        // 根据防火墙类型执行检查
        CheckItem result;
        if (firewallType == FirewallType.FIREWALLD) {
            result = checkFirewalld(session, checkItem, cacheLog);
        } else if (firewallType == FirewallType.UFW) {
            result = checkUfw(session, checkItem, cacheLog);
        } else if (firewallType == FirewallType.IPTABLES) {
            result = checkIptables(session, checkItem, cacheLog);
        } else {
            cacheLog.info("未检测到防火墙服务");
            checkItem.setStatus(CheckItem.Status.SUCCESS);
            checkItem.setMessage("未检测到防火墙服务");
            result = checkItem;
        }

        // 确保状态已更新
        if (result.getStatus() == CheckItem.Status.CHECKING) {
            log.warn("防火墙检查完成但状态仍未更新，进行额外检查");
            cacheLog.warn("防火墙检查完成但状态未更新，进行额外检查");

            // 额外检查防火墙状态
            try {
                // 根据防火墙类型执行额外检查
                if (firewallType == FirewallType.FIREWALLD) {
                    // 检查firewalld状态
                    CommandResult statusResult = execCommand(session, "systemctl is-active firewalld", cacheLog);
                    if (statusResult.isSuccess()) {
                        if (statusResult.getOutput().trim().equals("active")) {
                            log.warn("额外检查发现firewalld服务处于active状态");
                            cacheLog.warn("额外检查发现firewalld服务处于active状态");
                            result.setStatus(CheckItem.Status.FAILED);
                            result.setMessage("firewalld防火墙正在运行，建议关闭");
                        } else {
                            log.info("额外检查发现firewalld服务已停止");
                            cacheLog.info("额外检查发现firewalld服务已停止");
                            result.setStatus(CheckItem.Status.SUCCESS);
                            result.setMessage("firewalld防火墙已关闭");
                        }
                    } else {
                        log.warn("额外检查firewalld状态失败");
                        cacheLog.warn("额外检查firewalld状态失败，假定防火墙未启用");
                        result.setStatus(CheckItem.Status.SUCCESS);
                        result.setMessage("无法确定firewalld状态，假定未启用");
                    }
                } else if (firewallType == FirewallType.UFW) {
                    // 检查ufw状态
                    CommandResult statusResult = execCommand(session, "ufw status | grep Status", cacheLog);
                    if (statusResult.isSuccess()) {
                        if (statusResult.getOutput().contains("active")) {
                            log.warn("额外检查发现ufw服务处于active状态");
                            cacheLog.warn("额外检查发现ufw服务处于active状态");
                            result.setStatus(CheckItem.Status.FAILED);
                            result.setMessage("ufw防火墙正在运行，建议关闭");
                        } else {
                            log.info("额外检查发现ufw服务已停止");
                            cacheLog.info("额外检查发现ufw服务已停止");
                            result.setStatus(CheckItem.Status.SUCCESS);
                            result.setMessage("ufw防火墙已关闭");
                        }
                    } else {
                        log.warn("额外检查ufw状态失败");
                        cacheLog.warn("额外检查ufw状态失败，假定防火墙未启用");
                        result.setStatus(CheckItem.Status.SUCCESS);
                        result.setMessage("无法确定ufw状态，假定未启用");
                    }
                } else if (firewallType == FirewallType.IPTABLES) {
                    // 检查iptables状态
                    CommandResult rulesResult = execCommand(session, "iptables -L | grep -E 'DROP|REJECT'", cacheLog);
                    if (rulesResult.isSuccess()) {
                        if (!rulesResult.getOutput().trim().isEmpty()) {
                            log.warn("额外检查发现iptables存在DROP或REJECT规则");
                            cacheLog.warn("额外检查发现iptables存在DROP或REJECT规则");
                            result.setStatus(CheckItem.Status.FAILED);
                            result.setMessage("iptables防火墙存在限制规则，建议清除");
                        } else {
                            log.info("额外检查发现iptables无限制规则");
                            cacheLog.info("额外检查发现iptables无限制规则");
                            result.setStatus(CheckItem.Status.SUCCESS);
                            result.setMessage("iptables防火墙无限制规则");
                        }
                    } else {
                        log.warn("额外检查iptables状态失败");
                        cacheLog.warn("额外检查iptables状态失败，假定防火墙未启用");
                        result.setStatus(CheckItem.Status.SUCCESS);
                        result.setMessage("无法确定iptables状态，假定未启用");
                    }
                } else {
                    // NONE类型，没有防火墙
                    log.info("额外检查确认无防火墙服务");
                    cacheLog.info("额外检查确认无防火墙服务");
                    result.setStatus(CheckItem.Status.SUCCESS);
                    result.setMessage("未检测到防火墙服务");
                }
            } catch (Exception e) {
                log.error("额外检查防火墙状态时出错: ", e);
                cacheLog.error("额外检查防火墙状态时出错: {}", e.getMessage());
                result.setStatus(CheckItem.Status.FAILED);
                result.setMessage("防火墙检查执行过程中发生异常: " + e.getMessage());
            }
        }

        return result;
    }

    @Override
    public boolean fix(HostInfo hostInfo, CheckItem checkItem, CheckLogger cacheLog) throws InterruptedException {
        cacheLog.info("==== 通用防火墙修复开始 ====");

        checkItem.setMessage("正在修复防火墙配置...");

        // 获取SSH会话
        ClientSession session = getSession(hostInfo, cacheLog);
        if (session == null || !session.isOpen()) {
            String errorMsg = "SSH会话为空或已关闭，无法执行命令";
            log.error(errorMsg);
            cacheLog.error(errorMsg);
            checkItem.setStatus(CheckItem.Status.FAILED);
            checkItem.setMessage(errorMsg);
            return false;
        }

        // 确定防火墙类型
        FirewallType firewallType = detectFirewallType(session, cacheLog);
        cacheLog.info("检测到防火墙类型: %s", firewallType.name());

        // 根据防火墙类型执行修复
        boolean result;
        if (firewallType == FirewallType.FIREWALLD) {
            result = fixFirewalld(session, checkItem, cacheLog);
        } else if (firewallType == FirewallType.UFW) {
            result = fixUfw(session, checkItem, cacheLog);
        } else if (firewallType == FirewallType.IPTABLES) {
            result = fixIptables(session, checkItem, cacheLog);
        } else {
            cacheLog.info("未检测到防火墙服务，无需修复");
            checkItem.setStatus(CheckItem.Status.SUCCESS);
            checkItem.setMessage("未检测到防火墙服务，无需修复");
            result = true;
        }

        // 根据修复结果更新状态
        if (result) {
            checkItem.setStatus(CheckItem.Status.SUCCESS);
        } else {
            checkItem.setStatus(CheckItem.Status.FAILED);
        }

        return result;
    }

    /**
     * 检测防火墙类型
     */
    protected FirewallType detectFirewallType(ClientSession session, CheckLogger cacheLog) {
        cacheLog.info("检测防火墙类型...");

        // 检查会话是否有效
        if (session == null || !session.isOpen()) {
            log.error("SSH会话为空或已关闭，无法检测防火墙类型");
            cacheLog.error("SSH会话为空或已关闭，无法检测防火墙类型");
            return FirewallType.NONE;
        }

        // 检查firewalld
        CommandResult firewalldResult = execCommand(session, "which firewall-cmd 2>/dev/null || echo 'Not Found'",
                cacheLog);
        if (firewalldResult.isSuccess() && !firewalldResult.getOutput().contains("Not Found")) {
            // 再次检查服务是否安装
            CommandResult systemctlResult = execCommand(session,
                    "systemctl list-unit-files | grep firewalld.service || echo 'Not Installed'", cacheLog);
            if (systemctlResult.isSuccess() && !systemctlResult.getOutput().contains("Not Installed")) {
                cacheLog.info("检测到firewalld防火墙");
                return FirewallType.FIREWALLD;
            }
        }

        // 检查ufw
        CommandResult ufwResult = execCommand(session, "which ufw 2>/dev/null || echo 'Not Found'", cacheLog);
        if (ufwResult.isSuccess() && !ufwResult.getOutput().contains("Not Found")) {
            // 再次检查ufw是否安装
            CommandResult dpkgResult = execCommand(session, "dpkg -l | grep ufw || echo 'Not Installed'", cacheLog);
            if (dpkgResult.isSuccess() && !dpkgResult.getOutput().contains("Not Installed")) {
                cacheLog.info("检测到ufw防火墙");
                return FirewallType.UFW;
            }
        }

        // 检查iptables
        CommandResult iptablesResult = execCommand(session, "which iptables 2>/dev/null || echo 'Not Found'", cacheLog);
        if (iptablesResult.isSuccess() && !iptablesResult.getOutput().contains("Not Found")) {
            // 检查iptables规则
            CommandResult rulesResult = execCommand(session, "iptables -L", cacheLog);
            if (rulesResult.isSuccess()) {
                cacheLog.info("检测到iptables防火墙");
                return FirewallType.IPTABLES;
            }
        }

        cacheLog.info("未检测到防火墙");
        return FirewallType.NONE;
    }

    /**
     * 检查firewalld防火墙
     */
    protected CheckItem checkFirewalld(ClientSession session, CheckItem checkItem, CheckLogger cacheLog)
            throws InterruptedException {
        StringBuilder htmlOutput = new StringBuilder();
        htmlOutput.append("<div class='firewall-check'>");
        htmlOutput.append("<h3>Firewalld 防火墙检查</h3>");

        // 检查firewalld服务状态
        CommandResult statusResult = execCommand(session, "systemctl status firewalld", cacheLog);
        htmlOutput.append("<div class='command-block'>");
        htmlOutput.append("<h4>1. 检查firewalld服务状态</h4>");
        htmlOutput.append("<pre class='command'>$ systemctl status firewalld</pre>");
        htmlOutput.append("<pre class='output'>").append(statusResult.getOutput()).append("</pre>");
        htmlOutput.append("</div>");

        // 检查firewalld版本
        CommandResult versionResult = execCommand(session, "firewall-cmd --version", cacheLog);
        htmlOutput.append("<div class='command-block'>");
        htmlOutput.append("<h4>2. 检查firewalld版本</h4>");
        htmlOutput.append("<pre class='command'>$ firewall-cmd --version</pre>");
        htmlOutput.append("<pre class='output'>").append(versionResult.getOutput()).append("</pre>");
        htmlOutput.append("</div>");

        // 检查活动区域
        CommandResult zonesResult = execCommand(session, "firewall-cmd --get-active-zones", cacheLog);
        htmlOutput.append("<div class='command-block'>");
        htmlOutput.append("<h4>3. 检查活动区域</h4>");
        htmlOutput.append("<pre class='command'>$ firewall-cmd --get-active-zones</pre>");
        htmlOutput.append("<pre class='output'>").append(zonesResult.getOutput()).append("</pre>");
        htmlOutput.append("</div>");

        // 检查所有规则
        CommandResult listAllResult = execCommand(session, "firewall-cmd --list-all", cacheLog);
        htmlOutput.append("<div class='command-block'>");
        htmlOutput.append("<h4>4. 检查所有规则</h4>");
        htmlOutput.append("<pre class='command'>$ firewall-cmd --list-all</pre>");
        htmlOutput.append("<pre class='output'>").append(listAllResult.getOutput()).append("</pre>");
        htmlOutput.append("</div>");

        // 检查服务是否开机自启
        CommandResult enabledResult = execCommand(session, "systemctl is-enabled firewalld", cacheLog);
        htmlOutput.append("<div class='command-block'>");
        htmlOutput.append("<h4>5. 检查开机自启状态</h4>");
        htmlOutput.append("<pre class='command'>$ systemctl is-enabled firewalld</pre>");
        htmlOutput.append("<pre class='output'>").append(enabledResult.getOutput()).append("</pre>");
        htmlOutput.append("</div>");

        // 分析结果
        boolean isRunning = statusResult.getOutput().contains("active (running)");
        boolean isEnabled = enabledResult.getOutput().trim().equals("enabled");

        htmlOutput.append("<div class='analysis'>");
        htmlOutput.append("<h4>分析结果</h4>");
        htmlOutput.append("<ul>");
        htmlOutput.append("<li>服务状态: ").append(
                isRunning ? "<span class='status-running'>运行中</span>" : "<span class='status-stopped'>已停止</span>")
                .append("</li>");
        htmlOutput.append("<li>开机自启: ").append(
                isEnabled ? "<span class='status-enabled'>已启用</span>" : "<span class='status-disabled'>未启用</span>")
                .append("</li>");
        htmlOutput.append("</ul>");
        htmlOutput.append("</div>");

        // 添加修复建议
        htmlOutput.append("<div class='recommendations'>");
        htmlOutput.append("<h4>修复建议</h4>");
        if (isRunning || isEnabled) {
            htmlOutput.append("<p class='warning'>检测到防火墙正在运行或已启用开机自启，建议执行以下操作：</p>");
            htmlOutput.append("<ol>");
            htmlOutput.append("<li>停止防火墙服务：<code>systemctl stop firewalld</code></li>");
            htmlOutput.append("<li>禁用开机自启：<code>systemctl disable firewalld</code></li>");
            htmlOutput.append("<li>确认服务状态：<code>systemctl status firewalld</code></li>");
            htmlOutput.append("</ol>");
        } else {
            htmlOutput.append("<p class='success'>防火墙已停止且未启用开机自启，无需修复。</p>");
        }
        htmlOutput.append("</div>");

        htmlOutput.append("</div>");

        // 设置检查结果
        checkItem.setMessage(htmlOutput.toString());
        checkItem.setStatus(isRunning || isEnabled ? CheckItem.Status.FAILED : CheckItem.Status.SUCCESS);

        return checkItem;
    }

    /**
     * 检查ufw防火墙
     */
    protected CheckItem checkUfw(ClientSession session, CheckItem checkItem, CheckLogger cacheLog) {
        StringBuilder htmlOutput = new StringBuilder();
        htmlOutput.append("<div class='firewall-check'>");
        htmlOutput.append("<h3>UFW 防火墙检查</h3>");

        // 检查ufw服务状态
        CommandResult statusResult = execCommand(session, "ufw status", cacheLog);
        htmlOutput.append("<div class='command-block'>");
        htmlOutput.append("<h4>1. 检查UFW服务状态</h4>");
        htmlOutput.append("<pre class='command'>$ ufw status</pre>");
        htmlOutput.append("<pre class='output'>").append(statusResult.getOutput()).append("</pre>");
        htmlOutput.append("</div>");

        // 检查ufw版本
        CommandResult versionResult = execCommand(session, "ufw --version", cacheLog);
        htmlOutput.append("<div class='command-block'>");
        htmlOutput.append("<h4>2. 检查UFW版本</h4>");
        htmlOutput.append("<pre class='command'>$ ufw --version</pre>");
        htmlOutput.append("<pre class='output'>").append(versionResult.getOutput()).append("</pre>");
        htmlOutput.append("</div>");

        // 检查详细规则
        CommandResult rulesResult = execCommand(session, "ufw status verbose", cacheLog);
        htmlOutput.append("<div class='command-block'>");
        htmlOutput.append("<h4>3. 检查详细规则</h4>");
        htmlOutput.append("<pre class='command'>$ ufw status verbose</pre>");
        htmlOutput.append("<pre class='output'>").append(rulesResult.getOutput()).append("</pre>");
        htmlOutput.append("</div>");

        // 检查开机自启状态
        CommandResult enabledResult = execCommand(session, "systemctl is-enabled ufw", cacheLog);
        htmlOutput.append("<div class='command-block'>");
        htmlOutput.append("<h4>4. 检查开机自启状态</h4>");
        htmlOutput.append("<pre class='command'>$ systemctl is-enabled ufw</pre>");
        htmlOutput.append("<pre class='output'>").append(enabledResult.getOutput()).append("</pre>");
        htmlOutput.append("</div>");

        // 分析结果
        boolean isActive = statusResult.getOutput().contains("Status: active");
        boolean isEnabled = enabledResult.getOutput().trim().equals("enabled");

        htmlOutput.append("<div class='analysis'>");
        htmlOutput.append("<h4>分析结果</h4>");
        htmlOutput.append("<ul>");
        htmlOutput.append("<li>服务状态: ").append(
                isActive ? "<span class='status-running'>运行中</span>" : "<span class='status-stopped'>已停止</span>")
                .append("</li>");
        htmlOutput.append("<li>开机自启: ").append(
                isEnabled ? "<span class='status-enabled'>已启用</span>" : "<span class='status-disabled'>未启用</span>")
                .append("</li>");
        htmlOutput.append("</ul>");
        htmlOutput.append("</div>");

        // 添加修复建议
        htmlOutput.append("<div class='recommendations'>");
        htmlOutput.append("<h4>修复建议</h4>");
        if (isActive || isEnabled) {
            htmlOutput.append("<p class='warning'>检测到UFW防火墙正在运行或已启用开机自启，建议执行以下操作：</p>");
            htmlOutput.append("<ol>");
            htmlOutput.append("<li>停止防火墙服务：<code>ufw disable</code></li>");
            htmlOutput.append("<li>禁用开机自启：<code>systemctl disable ufw</code></li>");
            htmlOutput.append("<li>确认服务状态：<code>ufw status</code></li>");
            htmlOutput.append("</ol>");
        } else {
            htmlOutput.append("<p class='success'>UFW防火墙已停止且未启用开机自启，无需修复。</p>");
        }
        htmlOutput.append("</div>");

        htmlOutput.append("</div>");

        // 设置检查结果
        checkItem.setMessage(htmlOutput.toString());
        checkItem.setStatus(isActive || isEnabled ? CheckItem.Status.FAILED : CheckItem.Status.SUCCESS);

        return checkItem;
    }

    /**
     * 检查iptables防火墙
     */
    protected CheckItem checkIptables(ClientSession session, CheckItem checkItem, CheckLogger cacheLog)
            throws InterruptedException {
        cacheLog.info("执行检查命令: iptables -L");
        CommandResult result = execCommand(session, "iptables -L", cacheLog);

        if (result.isSuccess()) {
            String output = result.getOutput();
            boolean hasRules = !output.contains("Chain INPUT (policy ACCEPT)") ||
                    !output.contains("Chain FORWARD (policy ACCEPT)") ||
                    !output.contains("Chain OUTPUT (policy ACCEPT)") ||
                    output.contains("REJECT") ||
                    output.contains("DROP");

            // 检查是否有非默认规则

            if (hasRules) {
                cacheLog.info("iptables状态: 有活动规则");
                cacheLog.info("当前iptables规则:");
                cacheLog.info(output);

                checkItem.setStatus(CheckItem.Status.FAILED);
                checkItem.setMessage("iptables防火墙有活动规则，建议清除");
            } else {
                cacheLog.info("iptables状态: 无活动规则");
                checkItem.setStatus(CheckItem.Status.SUCCESS);
                checkItem.setMessage("iptables防火墙无活动规则");
            }
        } else {
            // 命令执行失败
            cacheLog.warn("获取iptables规则失败: %s", result.getErrorOrOutput());
            checkItem.setStatus(CheckItem.Status.FAILED);
            checkItem.setMessage("获取iptables规则失败: " + result.getErrorOrOutput());
        }

        return checkItem;
    }

    /**
     * 修复firewalld防火墙
     */
    protected boolean fixFirewalld(ClientSession session, CheckItem checkItem, CheckLogger cacheLog)
            throws InterruptedException {
        // 更新状态为正在检查防火墙当前状态
        checkItem.setMessage("正在检查firewalld防火墙当前状态...");

        // 检查firewalld状态
        cacheLog.info("检查firewalld防火墙当前状态...");
        CommandResult statusResult = execCommand(session, "systemctl status firewalld", cacheLog);

        // 如果服务不存在，直接返回成功
        if (statusResult.getExitCode() == 4) {
            cacheLog.info("firewalld防火墙服务未安装，无需修复");
            checkItem.setMessage("firewalld防火墙服务未安装，无需修复");
            return true;
        }

        // 如果服务已停止，检查是否已禁用自启动
        if (statusResult.getExitCode() == 3) {
            // 更新状态为正在检查自启动状态
            checkItem.setMessage("正在检查firewalld防火墙自启动状态...");

            CommandResult isEnabledResult = execCommand(session, "systemctl is-enabled firewalld", cacheLog);
            if (isEnabledResult.isSuccess() && isEnabledResult.getOutput().trim().equals("disabled")) {
                cacheLog.info("firewalld防火墙已关闭且已禁用自启动，无需修复");
                checkItem.setMessage("firewalld防火墙已关闭且已禁用自启动");
                return true;
            }
        }

        // 更新状态为正在停止防火墙服务
        checkItem.setMessage("正在停止firewalld防火墙服务...");

        // 停止并禁用防火墙
        cacheLog.info("正在停止firewalld防火墙服务...");
        CommandResult stopResult = execCommand(session, "systemctl stop firewalld", cacheLog);
        if (!stopResult.isSuccess()) {
            cacheLog.error("停止firewalld防火墙服务失败: %s", stopResult.getErrorOrOutput());
            checkItem.setMessage("停止firewalld防火墙服务失败: " + stopResult.getErrorOrOutput());
            return false;
        }
        cacheLog.info("停止firewalld防火墙服务完成");

        // 更新状态为正在禁用防火墙自启动
        checkItem.setMessage("正在禁用firewalld防火墙自启动...");

        cacheLog.info("正在禁用firewalld防火墙自启动...");
        CommandResult disableResult = execCommand(session, "systemctl disable firewalld", cacheLog);
        if (!disableResult.isSuccess()) {
            cacheLog.error("禁用firewalld防火墙自启动失败: %s", disableResult.getErrorOrOutput());
            checkItem.setMessage("禁用firewalld防火墙自启动失败: " + disableResult.getErrorOrOutput());
            return false;
        }
        cacheLog.info("禁用firewalld防火墙自启动完成");

        // 更新状态为正在验证防火墙状态
        checkItem.setMessage("正在验证firewalld防火墙状态...");

        // 再次检查确认防火墙已关闭
        cacheLog.info("验证firewalld防火墙状态...");
        CommandResult verifyResult = execCommand(session, "systemctl status firewalld", cacheLog);
        if (verifyResult.getExitCode() != 3) {
            cacheLog.warn("警告: firewalld防火墙服务可能未成功关闭，请手动检查");
            checkItem.setMessage("警告: firewalld防火墙服务可能未成功关闭，请手动检查");
            return false;
        }

        cacheLog.info("验证成功: firewalld防火墙已关闭");
        checkItem.setMessage("firewalld防火墙已成功关闭并禁用自启动");
        return true;
    }

    /**
     * 修复ufw防火墙
     */
    protected boolean fixUfw(ClientSession session, CheckItem checkItem, CheckLogger cacheLog) {
        // 检查ufw状态
        cacheLog.info("检查ufw防火墙当前状态...");
        checkItem.setMessage("正在检查ufw防火墙当前状态...");

        CommandResult statusResult = execCommand(session, "ufw status", cacheLog);
        if (!statusResult.isSuccess()) {
            cacheLog.info("ufw命令执行失败，可能未安装，无需修复");
            checkItem.setMessage("ufw防火墙未安装，无需修复");
            return true;
        }

        String output = statusResult.getOutput().toLowerCase();

        // 如果ufw已经处于inactive状态，只需要确保不会自启动
        if (output.contains("inactive") || output.contains("disabled")) {
            cacheLog.info("ufw防火墙已处于停止状态，检查是否配置为自启动...");
            checkItem.setMessage("正在检查ufw防火墙自启动状态...");

            CommandResult enabledResult = execCommand(session,
                    "grep -q 'ENABLED=yes' /etc/ufw/ufw.conf && echo 'enabled' || echo 'disabled'", cacheLog);
            if (enabledResult.isSuccess() && enabledResult.getOutput().trim().equals("enabled")) {
                // 修改配置文件，禁用自启动
                cacheLog.info("正在禁用ufw防火墙自启动...");
                checkItem.setMessage("正在禁用ufw防火墙自启动...");

                CommandResult disableResult = execCommand(session,
                        "sudo sed -i 's/ENABLED=yes/ENABLED=no/' /etc/ufw/ufw.conf", cacheLog);
                if (!disableResult.isSuccess()) {
                    cacheLog.error("禁用ufw防火墙自启动失败: %s", disableResult.getErrorOrOutput());
                    checkItem.setMessage("禁用ufw防火墙自启动失败: " + disableResult.getErrorOrOutput());
                    return false;
                }

                cacheLog.info("ufw防火墙自启动已禁用");
                checkItem.setMessage("ufw防火墙已关闭并禁用自启动");
            } else {
                cacheLog.info("ufw防火墙已关闭且未配置自启动，无需修复");
                checkItem.setMessage("ufw防火墙已关闭且未配置自启动");
            }

            return true;
        }

        // 如果ufw处于active状态，需要停止并禁用
        if (output.contains("active") || output.contains("enabled")) {
            cacheLog.info("正在关闭ufw防火墙...");
            checkItem.setMessage("正在关闭ufw防火墙...");

            // 关闭ufw，自动应答yes
            CommandResult disableResult = execCommand(session, "echo y | ufw disable", cacheLog);
            if (!disableResult.isSuccess()) {
                cacheLog.error("关闭ufw防火墙失败: %s", disableResult.getErrorOrOutput());
                checkItem.setMessage("关闭ufw防火墙失败: " + disableResult.getErrorOrOutput());
                return false;
            }

            // 确保ufw配置为不自启动
            CommandResult configResult = execCommand(session,
                    "sudo sed -i 's/ENABLED=yes/ENABLED=no/' /etc/ufw/ufw.conf", cacheLog);
            if (!configResult.isSuccess()) {
                cacheLog.warn("修改ufw防火墙配置文件失败: %s", configResult.getErrorOrOutput());
                // 不影响整体结果
            }

            // 验证修复结果
            cacheLog.info("正在验证ufw防火墙状态...");
            checkItem.setMessage("正在验证ufw防火墙状态...");

            CommandResult verifyResult = execCommand(session, "ufw status", cacheLog);
            if (verifyResult.isSuccess() &&
                    (verifyResult.getOutput().toLowerCase().contains("inactive") ||
                            verifyResult.getOutput().toLowerCase().contains("disabled"))) {
                cacheLog.info("验证成功: ufw防火墙已关闭");
                checkItem.setMessage("ufw防火墙已成功关闭");
                return true;
            } else {
                cacheLog.warn("警告: ufw防火墙可能未成功关闭，请手动检查");
                checkItem.setMessage("警告: ufw防火墙可能未成功关闭，请手动检查");
                return false;
            }
        }

        // 状态不明确，返回失败
        cacheLog.warn("无法确定ufw防火墙状态，修复失败");
        checkItem.setMessage("无法确定ufw防火墙状态，修复失败");
        return false;
    }

    /**
     * 修复iptables防火墙
     */
    protected boolean fixIptables(ClientSession session, CheckItem checkItem, CheckLogger cacheLog)
            throws InterruptedException {
        // 清空所有iptables规则
        cacheLog.info("正在清空iptables规则...");
        checkItem.setMessage("正在清空iptables规则...");

        // 设置默认策略为ACCEPT
        String[] chains = { "INPUT", "FORWARD", "OUTPUT" };
        for (String chain : chains) {
            CommandResult policyResult = execCommand(session, "iptables -P " + chain + " ACCEPT", cacheLog);
            if (!policyResult.isSuccess()) {
                cacheLog.error("设置iptables %s链默认策略失败: %s", chain, policyResult.getErrorOrOutput());
                checkItem.setMessage("设置iptables " + chain + "链默认策略失败: " + policyResult.getErrorOrOutput());
                // 继续尝试其他链
            }
        }

        // 清空所有规则
        CommandResult flushResult = execCommand(session, "iptables -F", cacheLog);
        if (!flushResult.isSuccess()) {
            cacheLog.error("清空iptables规则失败: %s", flushResult.getErrorOrOutput());
            checkItem.setMessage("清空iptables规则失败: " + flushResult.getErrorOrOutput());
            return false;
        }

        // 清空所有自定义链
        CommandResult chainResult = execCommand(session, "iptables -X", cacheLog);
        if (!chainResult.isSuccess()) {
            cacheLog.error("清空iptables自定义链失败: %s", chainResult.getErrorOrOutput());
            checkItem.setMessage("清空iptables自定义链失败: " + chainResult.getErrorOrOutput());
            // 不直接返回失败，因为可能没有自定义链
        }

        // 验证iptables规则已清空
        cacheLog.info("正在验证iptables规则...");
        checkItem.setMessage("正在验证iptables规则...");

        CommandResult verifyResult = execCommand(session, "iptables -L", cacheLog);
        if (verifyResult.isSuccess() &&
                verifyResult.getOutput().contains("Chain INPUT (policy ACCEPT)") &&
                verifyResult.getOutput().contains("Chain FORWARD (policy ACCEPT)") &&
                verifyResult.getOutput().contains("Chain OUTPUT (policy ACCEPT)") &&
                !verifyResult.getOutput().contains("REJECT") &&
                !verifyResult.getOutput().contains("DROP")) {

            cacheLog.info("验证成功: iptables规则已清空");
            checkItem.setMessage("iptables防火墙规则已成功清空");
            return true;
        } else {
            cacheLog.warn("警告: iptables规则可能未成功清空，请手动检查");
            checkItem.setMessage("警告: iptables规则可能未成功清空，请手动检查");
            return false;
        }
    }

    /**
     * 执行SSH命令
     */
    protected CommandResult execCommand(ClientSession session, String command, CheckLogger cacheLog) {
        if (session == null) {
            log.error("SSH会话为空，无法执行命令");
            cacheLog.error("SSH会话为空，无法执行命令");
            return new CommandResult("", "SSH会话为空", -1);
        }

        try {
            log.debug("执行命令: {}", command);
            cacheLog.debug("执行命令: %s", command);

            // 使用SshConnectionPoolManager执行命令
            if (sshConnectionPoolManager != null) {
                return sshConnectionPoolManager.execCommand(session, command);
            } else {
                cacheLog.error("SSH连接池管理器未注入，无法执行命令");
                return new CommandResult("", "SSH连接池管理器未注入", -1);
            }
        } catch (Exception e) {
            log.error("执行命令失败: {}", e.getMessage(), e);
            cacheLog.error("执行命令失败: %s", e.getMessage());
            return new CommandResult("", e.getMessage(), -1);
        }
    }
}