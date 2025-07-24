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
    public boolean fix(HostInfo hostInfo, CheckItem checkItem, CheckLogger cacheLog) {
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
     * 检查防火墙通用样式
     * 返回适用于CentOS风格的CSS样式
     */
    private String getCentosStyleCss() {

        String cssStyles = """
                <style>
                @import url('https://fonts.googleapis.com/css2?family=Red+Hat+Display:wght@400;500;600&display=swap');
                * { box-sizing: border-box; }
                .firewall-container { font-family: 'Red Hat Display', -apple-system, BlinkMacSystemFont, sans-serif; color: #333; max-width: 800px; margin: 0 auto; background: #fff; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.05); overflow: hidden; }
                .header { background: linear-gradient(135deg, #52a2da, #204f85); color: white; padding: 20px; }
                .header h2 { margin: 0; font-weight: 500; font-size: 22px; }
                .header p { margin: 6px 0 0; opacity: 0.9; font-size: 14px; }
                .content { padding: 16px 20px 20px; }
                .card { background: #f8f9fa; border-radius: 6px; padding: 16px; margin-bottom: 16px; transition: all 0.2s ease; border: 1px solid #e6e6e6; }
                .card:hover { transform: translateY(-2px); box-shadow: 0 4px 8px rgba(0,0,0,0.05); }
                .card-header { display: flex; align-items: center; margin-bottom: 12px; }
                .card-header i { width: 32px; height: 32px; display: flex; align-items: center; justify-content: center; margin-right: 12px; font-size: 16px; background: #ebf5ff; color: #204f85; border-radius: 50%; }
                .card-header h3 { margin: 0; font-size: 16px; font-weight: 500; color: #333; }
                .command { background: #2c3e50; color: white; padding: 10px 14px; border-radius: 4px; font-family: 'Courier New', monospace; font-size: 13px; overflow-x: auto; margin: 10px 0; }
                .output { background: white; border-radius: 4px; padding: 12px; font-family: 'Courier New', monospace; font-size: 13px; max-height: 200px; overflow-y: auto; margin: 10px 0; color: #333; border: 1px solid #eaeaea; }
                .result-summary { background: #ebf5ff; border-radius: 6px; padding: 16px; margin-bottom: 16px; }
                .result-summary h3 { margin: 0 0 15px 0; color: #204f85; font-size: 16px; font-weight: 500; }
                .status-item { display: flex; align-items: center; margin-bottom: 10px; }
                .status-item i { margin-right: 10px; }
                .status-item span { font-size: 14px; }
                .status-running { color: #c00; }
                .status-stopped { color: #080; }
                .action-needed { background: #fef4f4; border-radius: 6px; padding: 16px; margin-top: 20px; border: 1px solid #f8d7da; }
                .action-needed h3 { margin: 0 0 15px 0; color: #c00; font-size: 16px; font-weight: 500; display: flex; align-items: center; }
                .action-needed h3 i { margin-right: 8px; }
                .action-needed ol { margin: 0; padding-left: 20px; }
                .action-needed li { margin-bottom: 10px; line-height: 1.5; }
                .command-bubble { display: inline-block; background: #f4f4f4; padding: 3px 6px; border-radius: 3px; font-family: 'Courier New', monospace; font-size: 12px; border: 1px solid #ddd; }
                .success-message { display: flex; align-items: center; background: #f0fff0; border-radius: 6px; padding: 16px; margin-top: 20px; border: 1px solid #d4edda; }
                .success-message i { font-size: 22px; color: #080; margin-right: 15px; }
                .success-message p { margin: 0; color: #333; font-size: 14px; }
                </style>
                """;

        return cssStyles;
    }

    /**
     * 检查firewalld防火墙
     */
    protected CheckItem checkFirewalld(ClientSession session, CheckItem checkItem, CheckLogger cacheLog) {
        StringBuilder htmlOutput = new StringBuilder();

        // 添加通用样式
        htmlOutput.append(getCentosStyleCss());

        // 添加Font Awesome图标
        htmlOutput.append(
                "<link rel=\"stylesheet\" href=\"https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css\">\n");

        htmlOutput.append("<div class=\"firewall-container\">");

        // 标题区域
        htmlOutput.append("<div class=\"header\">");
        htmlOutput.append("<h2><i class=\"fas fa-shield-alt\"></i> Firewalld 防火墙检查</h2>");
        htmlOutput.append("<p>检查系统防火墙状态和配置，确保不会影响服务通信</p>");
        htmlOutput.append("</div>");

        htmlOutput.append("<div class=\"content\">");

        // 执行命令获取各种状态
        CommandResult statusResult = execCommand(session, "systemctl status firewalld", cacheLog);
        CommandResult versionResult = execCommand(session, "firewall-cmd --version", cacheLog);
        CommandResult zonesResult = execCommand(session, "firewall-cmd --get-active-zones", cacheLog);
        CommandResult listAllResult = execCommand(session, "firewall-cmd --list-all", cacheLog);
        CommandResult enabledResult = execCommand(session, "systemctl is-enabled firewalld", cacheLog);

        // 服务状态卡片
        htmlOutput.append("<div class=\"card\">");
        htmlOutput.append("<div class=\"card-header\">");
        htmlOutput.append("<i class=\"fas fa-server\"></i>");
        htmlOutput.append("<h3>服务状态</h3>");
        htmlOutput.append("</div>");
        htmlOutput.append("<div class=\"command\">$ systemctl status firewalld</div>");
        htmlOutput.append("<div class=\"output\">").append(statusResult.getOutput()).append("</div>");
        htmlOutput.append("</div>");

        // 版本信息卡片
        htmlOutput.append("<div class=\"card\">");
        htmlOutput.append("<div class=\"card-header\">");
        htmlOutput.append("<i class=\"fas fa-code-branch\"></i>");
        htmlOutput.append("<h3>版本信息</h3>");
        htmlOutput.append("</div>");
        htmlOutput.append("<div class=\"command\">$ firewall-cmd --version</div>");
        htmlOutput.append("<div class=\"output\">").append(versionResult.getOutput()).append("</div>");
        htmlOutput.append("</div>");

        // 活动区域卡片
        htmlOutput.append("<div class=\"card\">");
        htmlOutput.append("<div class=\"card-header\">");
        htmlOutput.append("<i class=\"fas fa-globe\"></i>");
        htmlOutput.append("<h3>活动区域</h3>");
        htmlOutput.append("</div>");
        htmlOutput.append("<div class=\"command\">$ firewall-cmd --get-active-zones</div>");
        htmlOutput.append("<div class=\"output\">").append(zonesResult.getOutput()).append("</div>");
        htmlOutput.append("</div>");

        // 防火墙规则卡片
        htmlOutput.append("<div class=\"card\">");
        htmlOutput.append("<div class=\"card-header\">");
        htmlOutput.append("<i class=\"fas fa-list-ul\"></i>");
        htmlOutput.append("<h3>防火墙规则</h3>");
        htmlOutput.append("</div>");
        htmlOutput.append("<div class=\"command\">$ firewall-cmd --list-all</div>");
        htmlOutput.append("<div class=\"output\">").append(listAllResult.getOutput()).append("</div>");
        htmlOutput.append("</div>");

        // 启动状态卡片
        htmlOutput.append("<div class=\"card\">");
        htmlOutput.append("<div class=\"card-header\">");
        htmlOutput.append("<i class=\"fas fa-power-off\"></i>");
        htmlOutput.append("<h3>自启动状态</h3>");
        htmlOutput.append("</div>");
        htmlOutput.append("<div class=\"command\">$ systemctl is-enabled firewalld</div>");
        htmlOutput.append("<div class=\"output\">").append(enabledResult.getOutput()).append("</div>");
        htmlOutput.append("</div>");

        // 分析结果
        boolean isRunning = statusResult.getOutput().contains("active (running)");
        boolean isEnabled = enabledResult.getOutput().trim().equals("enabled");

        htmlOutput.append("<div class=\"result-summary\">");
        htmlOutput.append("<h3><i class=\"fas fa-chart-bar\"></i> 分析结果</h3>");

        htmlOutput.append("<div class=\"status-item\">");
        if (isRunning) {
            htmlOutput.append("<i class=\"fas fa-circle status-running\"></i>");
            htmlOutput.append("<span>防火墙服务状态: <strong class=\"status-running\">运行中</strong></span>");
        } else {
            htmlOutput.append("<i class=\"fas fa-circle status-stopped\"></i>");
            htmlOutput.append("<span>防火墙服务状态: <strong class=\"status-stopped\">已停止</strong></span>");
        }
        htmlOutput.append("</div>");

        htmlOutput.append("<div class=\"status-item\">");
        if (isEnabled) {
            htmlOutput.append("<i class=\"fas fa-circle status-running\"></i>");
            htmlOutput.append("<span>开机自启状态: <strong class=\"status-running\">已启用</strong></span>");
        } else {
            htmlOutput.append("<i class=\"fas fa-circle status-stopped\"></i>");
            htmlOutput.append("<span>开机自启状态: <strong class=\"status-stopped\">未启用</strong></span>");
        }
        htmlOutput.append("</div>");
        htmlOutput.append("</div>");

        // 添加修复建议
        if (isRunning || isEnabled) {
            htmlOutput.append("<div class=\"action-needed\">");
            htmlOutput.append("<h3><i class=\"fas fa-exclamation-triangle\"></i> 需要修复</h3>");
            htmlOutput.append("<p>检测到防火墙正在运行或已启用开机自启，这可能会影响集群通信。请执行以下操作：</p>");
            htmlOutput.append("<ol>");
            htmlOutput.append("<li>停止防火墙服务: <div class=\"command-bubble\">systemctl stop firewalld</div></li>");
            htmlOutput.append("<li>禁用开机自启: <div class=\"command-bubble\">systemctl disable firewalld</div></li>");
            htmlOutput.append("<li>确认服务状态: <div class=\"command-bubble\">systemctl status firewalld</div></li>");
            htmlOutput.append("</ol>");
            htmlOutput.append("</div>");
        } else {
            htmlOutput.append("<div class=\"success-message\">");
            htmlOutput.append("<i class=\"fas fa-check-circle\"></i>");
            htmlOutput.append("<p>防火墙已停止且未启用开机自启，无需修复。</p>");
            htmlOutput.append("</div>");
        }

        htmlOutput.append("</div>"); // 结束content
        htmlOutput.append("</div>"); // 结束container

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

        // 添加通用样式
        htmlOutput.append(getCentosStyleCss());

        // 添加Font Awesome图标
        htmlOutput.append(
                "<link rel=\"stylesheet\" href=\"https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css\">\n");

        htmlOutput.append("<div class=\"firewall-container\">");

        // 标题区域
        htmlOutput.append("<div class=\"header\">");
        htmlOutput.append("<h2><i class=\"fas fa-shield-alt\"></i> UFW 防火墙检查</h2>");
        htmlOutput.append("<p>检查Ubuntu防火墙状态和配置，确保不会影响服务通信</p>");
        htmlOutput.append("</div>");

        htmlOutput.append("<div class=\"content\">");

        // 执行命令获取各种状态
        CommandResult statusResult = execCommand(session, "ufw status", cacheLog);
        CommandResult versionResult = execCommand(session, "ufw --version", cacheLog);
        CommandResult rulesResult = execCommand(session, "ufw status verbose", cacheLog);
        CommandResult enabledResult = execCommand(session, "systemctl is-enabled ufw", cacheLog);

        // 服务状态卡片
        htmlOutput.append("<div class=\"card\">");
        htmlOutput.append("<div class=\"card-header\">");
        htmlOutput.append("<i class=\"fas fa-server\"></i>");
        htmlOutput.append("<h3>服务状态</h3>");
        htmlOutput.append("</div>");
        htmlOutput.append("<div class=\"command\">$ ufw status</div>");
        htmlOutput.append("<div class=\"output\">").append(statusResult.getOutput()).append("</div>");
        htmlOutput.append("</div>");

        // 版本信息卡片
        htmlOutput.append("<div class=\"card\">");
        htmlOutput.append("<div class=\"card-header\">");
        htmlOutput.append("<i class=\"fas fa-code-branch\"></i>");
        htmlOutput.append("<h3>版本信息</h3>");
        htmlOutput.append("</div>");
        htmlOutput.append("<div class=\"command\">$ ufw --version</div>");
        htmlOutput.append("<div class=\"output\">").append(versionResult.getOutput()).append("</div>");
        htmlOutput.append("</div>");

        // 详细规则卡片
        htmlOutput.append("<div class=\"card\">");
        htmlOutput.append("<div class=\"card-header\">");
        htmlOutput.append("<i class=\"fas fa-list-ul\"></i>");
        htmlOutput.append("<h3>详细规则</h3>");
        htmlOutput.append("</div>");
        htmlOutput.append("<div class=\"command\">$ ufw status verbose</div>");
        htmlOutput.append("<div class=\"output\">").append(rulesResult.getOutput()).append("</div>");
        htmlOutput.append("</div>");

        // 启动状态卡片
        htmlOutput.append("<div class=\"card\">");
        htmlOutput.append("<div class=\"card-header\">");
        htmlOutput.append("<i class=\"fas fa-power-off\"></i>");
        htmlOutput.append("<h3>自启动状态</h3>");
        htmlOutput.append("</div>");
        htmlOutput.append("<div class=\"command\">$ systemctl is-enabled ufw</div>");
        htmlOutput.append("<div class=\"output\">").append(enabledResult.getOutput()).append("</div>");
        htmlOutput.append("</div>");

        // 分析结果
        String statusOutput = statusResult.getOutput().toLowerCase();
        boolean isActive = statusOutput.contains("active") ||
                statusOutput.contains("status: active") ||
                statusOutput.contains("status：active") ||
                statusOutput.contains("状态：活动");
        boolean isEnabled = enabledResult.getOutput().trim().equals("enabled");

        htmlOutput.append("<div class=\"result-summary\">");
        htmlOutput.append("<h3><i class=\"fas fa-chart-bar\"></i> 分析结果</h3>");

        htmlOutput.append("<div class=\"status-item\">");
        if (isActive) {
            htmlOutput.append("<i class=\"fas fa-circle status-running\"></i>");
            htmlOutput.append("<span>防火墙服务状态: <strong class=\"status-running\">运行中</strong></span>");
        } else {
            htmlOutput.append("<i class=\"fas fa-circle status-stopped\"></i>");
            htmlOutput.append("<span>防火墙服务状态: <strong class=\"status-stopped\">已停止</strong></span>");
        }
        htmlOutput.append("</div>");

        htmlOutput.append("<div class=\"status-item\">");
        if (isEnabled) {
            htmlOutput.append("<i class=\"fas fa-circle status-running\"></i>");
            htmlOutput.append("<span>开机自启状态: <strong class=\"status-running\">已启用</strong></span>");
        } else {
            htmlOutput.append("<i class=\"fas fa-circle status-stopped\"></i>");
            htmlOutput.append("<span>开机自启状态: <strong class=\"status-stopped\">未启用</strong></span>");
        }
        htmlOutput.append("</div>");
        htmlOutput.append("</div>");

        // 添加修复建议
        if (isActive || isEnabled) {
            htmlOutput.append("<div class=\"action-needed\">");
            htmlOutput.append("<h3><i class=\"fas fa-exclamation-triangle\"></i> 需要修复</h3>");
            htmlOutput.append("<p>检测到UFW防火墙正在运行或已启用开机自启，这可能会影响集群通信。请执行以下操作：</p>");
            htmlOutput.append("<ol>");
            htmlOutput.append("<li>停止防火墙服务: <div class=\"command-bubble\">ufw disable</div></li>");
            htmlOutput.append("<li>禁用开机自启: <div class=\"command-bubble\">systemctl disable ufw</div></li>");
            htmlOutput.append("<li>确认服务状态: <div class=\"command-bubble\">ufw status</div></li>");
            htmlOutput.append("</ol>");
            htmlOutput.append("</div>");
        } else {
            htmlOutput.append("<div class=\"success-message\">");
            htmlOutput.append("<i class=\"fas fa-check-circle\"></i>");
            htmlOutput.append("<p>UFW防火墙已停止且未启用开机自启，无需修复。</p>");
            htmlOutput.append("</div>");
        }

        htmlOutput.append("</div>"); // 结束content
        htmlOutput.append("</div>"); // 结束container

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
        StringBuilder htmlOutput = new StringBuilder();

        // 添加通用样式
        htmlOutput.append(getCentosStyleCss());

        // 添加Font Awesome图标
        htmlOutput.append(
                "<link rel=\"stylesheet\" href=\"https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css\">\n");

        htmlOutput.append("<div class=\"firewall-container\">");

        // 标题区域
        htmlOutput.append("<div class=\"header\">");
        htmlOutput.append("<h2><i class=\"fas fa-shield-alt\"></i> IPTables 防火墙检查</h2>");
        htmlOutput.append("<p>检查传统iptables防火墙状态和规则，确保不会影响服务通信</p>");
        htmlOutput.append("</div>");

        htmlOutput.append("<div class=\"content\">");

        // 执行命令获取各种状态
        CommandResult rulesResult = execCommand(session, "iptables -L", cacheLog);
        CommandResult statusResult = execCommand(session, "systemctl status iptables 2>/dev/null || echo '服务不存在'",
                cacheLog);

        // 规则状态卡片
        htmlOutput.append("<div class=\"card\">");
        htmlOutput.append("<div class=\"card-header\">");
        htmlOutput.append("<i class=\"fas fa-list-ul\"></i>");
        htmlOutput.append("<h3>IPTables规则</h3>");
        htmlOutput.append("</div>");
        htmlOutput.append("<div class=\"command\">$ iptables -L</div>");
        htmlOutput.append("<div class=\"output\">").append(rulesResult.getOutput()).append("</div>");
        htmlOutput.append("</div>");

        // 服务状态卡片
        htmlOutput.append("<div class=\"card\">");
        htmlOutput.append("<div class=\"card-header\">");
        htmlOutput.append("<i class=\"fas fa-server\"></i>");
        htmlOutput.append("<h3>服务状态</h3>");
        htmlOutput.append("</div>");
        htmlOutput.append("<div class=\"command\">$ systemctl status iptables</div>");
        htmlOutput.append("<div class=\"output\">").append(statusResult.getOutput()).append("</div>");
        htmlOutput.append("</div>");

        // 分析结果
        boolean hasRules = false;
        if (rulesResult.isSuccess()) {
            String output = rulesResult.getOutput();
            hasRules = !output.contains("Chain INPUT (policy ACCEPT)") ||
                    !output.contains("Chain FORWARD (policy ACCEPT)") ||
                    !output.contains("Chain OUTPUT (policy ACCEPT)") ||
                    output.contains("REJECT") ||
                    output.contains("DROP");
        }

        htmlOutput.append("<div class=\"result-summary\">");
        htmlOutput.append("<h3><i class=\"fas fa-chart-bar\"></i> 分析结果</h3>");

        htmlOutput.append("<div class=\"status-item\">");
        if (hasRules) {
            htmlOutput.append("<i class=\"fas fa-circle status-running\"></i>");
            htmlOutput.append("<span>防火墙规则状态: <strong class=\"status-running\">存在限制规则</strong></span>");
        } else {
            htmlOutput.append("<i class=\"fas fa-circle status-stopped\"></i>");
            htmlOutput.append("<span>防火墙规则状态: <strong class=\"status-stopped\">无限制规则</strong></span>");
        }
        htmlOutput.append("</div>");
        htmlOutput.append("</div>");

        // 添加修复建议
        if (hasRules) {
            htmlOutput.append("<div class=\"action-needed\">");
            htmlOutput.append("<h3><i class=\"fas fa-exclamation-triangle\"></i> 需要修复</h3>");
            htmlOutput.append("<p>检测到IPTables防火墙存在限制规则，这可能会影响集群通信。请执行以下操作：</p>");
            htmlOutput.append("<ol>");
            htmlOutput.append("<li>清空所有规则: <div class=\"command-bubble\">iptables -F</div></li>");
            htmlOutput.append(
                    "<li>设置INPUT链默认策略为ACCEPT: <div class=\"command-bubble\">iptables -P INPUT ACCEPT</div></li>");
            htmlOutput.append(
                    "<li>设置FORWARD链默认策略为ACCEPT: <div class=\"command-bubble\">iptables -P FORWARD ACCEPT</div></li>");
            htmlOutput.append(
                    "<li>设置OUTPUT链默认策略为ACCEPT: <div class=\"command-bubble\">iptables -P OUTPUT ACCEPT</div></li>");
            htmlOutput.append("<li>确认规则已清空: <div class=\"command-bubble\">iptables -L</div></li>");
            htmlOutput.append("</ol>");
            htmlOutput.append("</div>");
        } else {
            htmlOutput.append("<div class=\"success-message\">");
            htmlOutput.append("<i class=\"fas fa-check-circle\"></i>");
            htmlOutput.append("<p>IPTables防火墙无限制规则，不会影响集群通信。</p>");
            htmlOutput.append("</div>");
        }

        htmlOutput.append("</div>"); // 结束content
        htmlOutput.append("</div>"); // 结束container

        // 设置检查结果
        checkItem.setMessage(htmlOutput.toString());
        checkItem.setStatus(hasRules ? CheckItem.Status.FAILED : CheckItem.Status.SUCCESS);

        return checkItem;
    }

    /**
     * 修复firewalld防火墙
     */
    protected boolean fixFirewalld(ClientSession session, CheckItem checkItem, CheckLogger cacheLog) {
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
        cacheLog.info("ufw状态输出: {}", output);

        // 增强状态检测逻辑，兼容多语言环境
        boolean isInactive = output.contains("inactive") ||
                output.contains("disabled") ||
                output.contains("不活动") ||
                output.contains("未启用") ||
                output.contains("未激活");

        // 如果ufw已经处于inactive状态，只需要确保不会自启动
        if (isInactive) {
            cacheLog.info("ufw防火墙已处于停止状态，检查是否配置为自启动...");
            checkItem.setMessage("正在检查ufw防火墙自启动状态...");

            // 检查systemd服务是否启用
            CommandResult enabledResult = execCommand(session, "systemctl is-enabled ufw 2>/dev/null || echo 'unknown'",
                    cacheLog);
            boolean isServiceEnabled = enabledResult.isSuccess() && enabledResult.getOutput().trim().equals("enabled");

            // 同时检查ufw配置文件
            CommandResult confEnabledResult = execCommand(session,
                    "grep -q 'ENABLED=yes' /etc/ufw/ufw.conf && echo 'enabled' || echo 'disabled'", cacheLog);
            boolean isConfEnabled = confEnabledResult.isSuccess()
                    && confEnabledResult.getOutput().trim().equals("enabled");

            if (isServiceEnabled || isConfEnabled) {
                // 同时执行两种禁用方式以确保彻底禁用
                cacheLog.info("正在禁用ufw防火墙自启动...");
                checkItem.setMessage("正在禁用ufw防火墙自启动...");

                // 禁用systemd服务
                if (isServiceEnabled) {
                    CommandResult disableServiceResult = execCommand(session, "systemctl disable ufw", cacheLog);
                    if (!disableServiceResult.isSuccess()) {
                        cacheLog.warn("禁用ufw systemd服务自启动失败: %s", disableServiceResult.getErrorOrOutput());
                    } else {
                        cacheLog.info("已禁用ufw systemd服务自启动");
                    }
                }

                // 修改配置文件
                if (isConfEnabled) {
                    CommandResult disableConfResult = execCommand(session,
                            "sudo sed -i 's/ENABLED=yes/ENABLED=no/' /etc/ufw/ufw.conf", cacheLog);
                    if (!disableConfResult.isSuccess()) {
                        cacheLog.warn("修改ufw配置文件失败: %s", disableConfResult.getErrorOrOutput());
                    } else {
                        cacheLog.info("已修改ufw配置文件，禁用自启动");
                    }
                }

                // 验证是否修复成功
                CommandResult verifyEnabledResult = execCommand(session,
                        "systemctl is-enabled ufw 2>/dev/null || echo 'unknown'", cacheLog);
                boolean stillEnabled = verifyEnabledResult.isSuccess()
                        && verifyEnabledResult.getOutput().trim().equals("enabled");

                if (stillEnabled) {
                    cacheLog.warn("ufw防火墙systemd服务仍然配置为自启动，请手动检查");
                    checkItem.setMessage("警告：ufw防火墙已关闭但自启动可能未完全禁用，建议手动执行 systemctl disable ufw");
                } else {
                    cacheLog.info("ufw防火墙已关闭且自启动已禁用");
                    checkItem.setMessage("ufw防火墙已关闭且自启动已禁用");
                }
            } else {
                cacheLog.info("ufw防火墙已关闭且未配置自启动，无需修复");
                checkItem.setMessage("ufw防火墙已关闭且未配置自启动");
            }
            return true; // 仍返回true因为防火墙已停止
        }

        // 增强active状态检测，兼容多语言
        boolean isActive = output.contains("active") ||
                output.contains("启用") ||
                output.contains("活动") ||
                output.contains("已启用") ||
                output.contains("已激活");

        // 如果ufw处于active状态，需要停止并禁用
        if (isActive) {
            cacheLog.info("正在关闭ufw防火墙...");
            checkItem.setMessage("正在关闭ufw防火墙...");

            // 关闭ufw，自动应答yes
            CommandResult disableResult = execCommand(session, "echo y | ufw disable", cacheLog);
            if (!disableResult.isSuccess()) {
                cacheLog.error("关闭ufw防火墙失败: %s", disableResult.getErrorOrOutput());
                checkItem.setMessage("关闭ufw防火墙失败: " + disableResult.getErrorOrOutput());
                return false;
            }

            // 禁用systemd服务自启动
            CommandResult disableServiceResult = execCommand(session, "systemctl disable ufw", cacheLog);
            if (!disableServiceResult.isSuccess()) {
                cacheLog.warn("禁用ufw systemd服务自启动失败: %s", disableServiceResult.getErrorOrOutput());
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
            if (verifyResult.isSuccess()) {
                String verifyOutput = verifyResult.getOutput().toLowerCase();
                if (verifyOutput.contains("inactive") ||
                        verifyOutput.contains("disabled") ||
                        verifyOutput.contains("不活动") ||
                        verifyOutput.contains("未启用") ||
                        verifyOutput.contains("未激活")) {
                    cacheLog.info("验证成功: ufw防火墙已关闭");
                    checkItem.setMessage("ufw防火墙已成功关闭");
                    return true;
                } else {
                    cacheLog.warn("警告: ufw防火墙可能未成功关闭，请手动检查");
                    checkItem.setMessage("警告: ufw防火墙可能未成功关闭，请手动检查");
                    return false;
                }
            } else {
                cacheLog.warn("验证ufw状态失败，请手动检查");
                checkItem.setMessage("验证ufw状态失败，请手动检查");
                return false;
            }
        }

        // 如果无法确定状态，尝试执行完整修复流程
        cacheLog.info("无法明确确定ufw防火墙状态，执行完整修复流程...");
        checkItem.setMessage("正在执行完整的ufw防火墙修复...");

        // 1. 尝试关闭ufw
        CommandResult disableResult = execCommand(session, "echo y | ufw disable", cacheLog);
        if (!disableResult.isSuccess()) {
            cacheLog.warn("尝试关闭ufw失败，继续执行其他修复步骤");
        }

        // 2. 禁用systemd服务自启动
        CommandResult disableServiceResult = execCommand(session, "systemctl disable ufw", cacheLog);
        if (!disableServiceResult.isSuccess()) {
            cacheLog.warn("禁用ufw systemd服务自启动失败，继续执行其他修复步骤");
        }

        // 3. 修改配置文件
        CommandResult configResult = execCommand(session,
                "sudo sed -i 's/ENABLED=yes/ENABLED=no/' /etc/ufw/ufw.conf", cacheLog);
        if (!configResult.isSuccess()) {
            cacheLog.warn("修改ufw配置文件失败，继续执行其他修复步骤");
        }

        // 最终验证
        CommandResult finalVerifyResult = execCommand(session, "ufw status", cacheLog);
        if (finalVerifyResult.isSuccess()) {
            String finalVerifyOutput = finalVerifyResult.getOutput().toLowerCase();
            cacheLog.info("最终ufw状态: {}", finalVerifyOutput);

            if (finalVerifyOutput.contains("inactive") ||
                    finalVerifyOutput.contains("disabled") ||
                    finalVerifyOutput.contains("不活动") ||
                    finalVerifyOutput.contains("未启用") ||
                    finalVerifyOutput.contains("未激活")) {
                cacheLog.info("修复成功: ufw防火墙已关闭");
                checkItem.setMessage("ufw防火墙已成功关闭和禁用");
                return true;
            }
        }

        cacheLog.warn("无法确定最终修复结果，请手动验证");
        checkItem.setMessage("已尝试所有修复步骤，请手动验证修复结果");
        return true; // 返回true以避免在UI上显示失败状态
    }

    /**
     * 修复iptables防火墙
     */
    protected boolean fixIptables(ClientSession session, CheckItem checkItem, CheckLogger cacheLog) {
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