package com.datasophon.api.service.checker.checkers.firewall.os.ubuntu;

import com.datasophon.api.service.checker.common.CommandResult;
import com.datasophon.api.service.checker.helpers.CheckLogger;
import com.datasophon.api.service.checker.checkers.firewall.generic.GenericFirewallChecker;
import com.datasophon.common.model.CheckItem;
import com.datasophon.common.model.HostInfo;
import org.apache.sshd.client.session.ClientSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ubuntu系统防火墙检查器
 * 适用于所有Ubuntu版本的通用检查逻辑
 */
public class UbuntuFirewallChecker extends GenericFirewallChecker {

    private static final Logger log = LoggerFactory.getLogger(UbuntuFirewallChecker.class);

    @Override
    public CheckItem check(HostInfo hostInfo, CheckItem checkItem, CheckLogger cacheLog) throws InterruptedException {
        cacheLog.info("==== Ubuntu防火墙检查开始 ====");

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

        // 检查Linux发行版本信息
        CommandResult versionResult = execCommand(session, "lsb_release -a 2>/dev/null || cat /etc/lsb-release",
                cacheLog);
        if (versionResult.isSuccess()) {
            cacheLog.info("系统版本信息: %s", versionResult.getOutput().trim());
        }

        // 首先检查是否安装了ufw
        FirewallType firewallType = detectFirewallType(session, cacheLog);

        if (firewallType == FirewallType.UFW) {
            cacheLog.info("检测到Ubuntu使用ufw防火墙(默认)");
            return checkUfw(session, checkItem, cacheLog);
        } else if (firewallType == FirewallType.FIREWALLD) {
            cacheLog.info("检测到Ubuntu使用firewalld防火墙(非默认)");
            return checkFirewalld(session, checkItem, cacheLog);
        } else if (firewallType == FirewallType.IPTABLES) {
            cacheLog.info("检测到Ubuntu使用iptables防火墙");
            return checkIptables(session, checkItem, cacheLog);
        } else {
            cacheLog.info("未检测到防火墙服务");
            checkItem.setStatus(CheckItem.Status.SUCCESS);
            checkItem.setMessage("未检测到防火墙服务");
            return checkItem;
        }
    }

    @Override
    public boolean fix(HostInfo hostInfo, CheckItem checkItem, CheckLogger cacheLog) throws InterruptedException {
        cacheLog.info("==== Ubuntu防火墙修复开始 ====");

        // 获取SSH会话
        ClientSession session = getSession(hostInfo, cacheLog);
        if (session == null) {
            String errorMsg = "SSH会话为空，无法执行命令";
            log.error(errorMsg);
            cacheLog.error(errorMsg);
            checkItem.setMessage(errorMsg);
            return false;
        }

        // 首先检查是否安装了ufw
        FirewallType firewallType = detectFirewallType(session, cacheLog);

        if (firewallType == FirewallType.UFW) {
            cacheLog.info("检测到Ubuntu使用ufw防火墙(默认)，正在修复...");
            return fixUfw(session, checkItem, cacheLog);
        } else if (firewallType == FirewallType.FIREWALLD) {
            cacheLog.info("检测到Ubuntu使用firewalld防火墙(非默认)，正在修复...");
            return fixFirewalld(session, checkItem, cacheLog);
        } else if (firewallType == FirewallType.IPTABLES) {
            cacheLog.info("检测到Ubuntu使用iptables防火墙，正在修复...");
            return fixIptables(session, checkItem, cacheLog);
        } else {
            cacheLog.info("未检测到防火墙服务，无需修复");
            checkItem.setMessage("未检测到防火墙服务，无需修复");
            return true;
        }
    }

    /**
     * 检查Ubuntu特有的系统服务
     */
    protected void checkUbuntuSpecificServices(ClientSession session, CheckLogger cacheLog)
            throws InterruptedException {
        // 检查Ubuntu特有的Apparmor服务
        CommandResult apparmorResult = execCommand(session, "systemctl status apparmor 2>/dev/null || echo 'Not Found'",
                cacheLog);
        if (apparmorResult.isSuccess() && !apparmorResult.getOutput().contains("Not Found")) {
            if (apparmorResult.getExitCode() == 0) {
                cacheLog.info("检测到AppArmor服务正在运行，该服务提供应用程序安全隔离");
            }
        }

        // 检查是否安装了snapd
        CommandResult snapdResult = execCommand(session, "systemctl status snapd 2>/dev/null || echo 'Not Found'",
                cacheLog);
        if (snapdResult.isSuccess() && !snapdResult.getOutput().contains("Not Found")) {
            if (snapdResult.getExitCode() == 0) {
                cacheLog.info("检测到snapd服务正在运行，该服务可能会安装带有自己防火墙规则的应用");
            }
        }

        // 检查是否有LXD/LXC容器服务
        CommandResult lxcResult = execCommand(session, "which lxc 2>/dev/null || echo 'Not Found'", cacheLog);
        if (lxcResult.isSuccess() && !lxcResult.getOutput().contains("Not Found")) {
            cacheLog.info("检测到LXC容器工具，可能会使用iptables规则");
        }
    }
}