package com.datasophon.api.service.checker.checkers.firewall.os.ubuntu;

import com.datasophon.api.service.checker.common.CommandResult;
import com.datasophon.api.service.checker.helpers.CheckLogger;
import com.datasophon.common.model.CheckItem;
import com.datasophon.common.model.HostInfo;
import org.apache.sshd.client.session.ClientSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ubuntu 22.04防火墙检查器
 * 专门处理Ubuntu 22.04系统的防火墙检查和修复
 */
public class Ubuntu22FirewallChecker extends UbuntuFirewallChecker {

    private static final Logger log = LoggerFactory.getLogger(Ubuntu22FirewallChecker.class);

    @Override
    public CheckItem check(HostInfo hostInfo, CheckItem checkItem, CheckLogger cacheLog) throws InterruptedException {
        cacheLog.info("==== Ubuntu 22.04防火墙检查开始 ====");

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

        // Ubuntu 22.04特有的检查
        checkUbuntu22SpecificServices(session, cacheLog);

        // 确定防火墙类型
        FirewallType firewallType = detectFirewallType(session, cacheLog);
        cacheLog.info("检测到Ubuntu 22.04防火墙类型: %s", firewallType.name());

        // 执行具体的防火墙检查
        if (firewallType == FirewallType.UFW) {
            // Ubuntu 22.04默认使用ufw
            cacheLog.info("检测到Ubuntu 22.04使用ufw防火墙(默认)");
            return checkUfw(session, checkItem, cacheLog);
        } else if (firewallType == FirewallType.FIREWALLD) {
            cacheLog.info("检测到Ubuntu 22.04使用firewalld防火墙(非默认)");
            return checkFirewalld(session, checkItem, cacheLog);
        } else if (firewallType == FirewallType.IPTABLES) {
            cacheLog.info("检测到Ubuntu 22.04使用iptables防火墙");
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
        cacheLog.info("==== Ubuntu 22.04防火墙修复开始 ====");

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
        cacheLog.info("检测到Ubuntu 22.04防火墙类型: %s", firewallType.name());

        // 执行具体的防火墙修复
        if (firewallType == FirewallType.UFW) {
            // Ubuntu 22.04默认使用ufw
            cacheLog.info("检测到Ubuntu 22.04使用ufw防火墙(默认)，正在修复...");
            return fixUfw(session, checkItem, cacheLog);
        } else if (firewallType == FirewallType.FIREWALLD) {
            cacheLog.info("检测到Ubuntu 22.04使用firewalld防火墙(非默认)，正在修复...");
            return fixFirewalld(session, checkItem, cacheLog);
        } else if (firewallType == FirewallType.IPTABLES) {
            cacheLog.info("检测到Ubuntu 22.04使用iptables防火墙，正在修复...");

            // 检查netfilter-persistent服务（Ubuntu特有）
            CommandResult netfilterResult = execCommand(session,
                    "systemctl status netfilter-persistent 2>/dev/null || echo 'Not Found'", cacheLog);
            if (netfilterResult.isSuccess() && !netfilterResult.getOutput().contains("Not Found")) {
                // 停止并禁用netfilter-persistent服务
                cacheLog.info("正在停止netfilter-persistent服务...");
                execCommand(session, "systemctl stop netfilter-persistent", cacheLog);

                cacheLog.info("正在禁用netfilter-persistent服务自启动...");
                execCommand(session, "systemctl disable netfilter-persistent", cacheLog);
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
     * 检查Ubuntu 22.04特有的服务
     */
    private void checkUbuntu22SpecificServices(ClientSession session, CheckLogger cacheLog)
            throws InterruptedException {
        // 调用父类方法
        checkUbuntuSpecificServices(session, cacheLog);

        // 检查是否启用了nftables（Ubuntu 22.04可能默认安装）
        CommandResult nftablesResult = execCommand(session, "which nft 2>/dev/null || echo 'Not Found'", cacheLog);
        if (nftablesResult.isSuccess() && !nftablesResult.getOutput().contains("Not Found")) {
            cacheLog.info("检测到nftables工具，可能用于实现底层防火墙规则");

            // 检查nftables规则
            CommandResult nftRulesResult = execCommand(session, "nft list tables 2>/dev/null || echo 'No tables'",
                    cacheLog);
            if (nftRulesResult.isSuccess() && !nftRulesResult.getOutput().contains("No tables")) {
                cacheLog.info("nftables表信息: %s", nftRulesResult.getOutput().trim());
            }
        }

        // 检查是否安装了ufw防火墙
        CommandResult ufwVersionResult = execCommand(session, "ufw --version 2>/dev/null || echo 'Not Found'",
                cacheLog);
        if (ufwVersionResult.isSuccess() && !ufwVersionResult.getOutput().contains("Not Found")) {
            cacheLog.info("ufw版本信息: %s", ufwVersionResult.getOutput().trim());
        }

        // 检查Ubuntu 22.04特有的服务 - systemd-networkd
        CommandResult networkdResult = execCommand(session,
                "systemctl status systemd-networkd 2>/dev/null || echo 'Not Found'", cacheLog);
        if (networkdResult.isSuccess() && !networkdResult.getOutput().contains("Not Found")) {
            if (networkdResult.getExitCode() == 0) {
                cacheLog.info("检测到systemd-networkd服务正在运行，此服务管理网络配置");
            }
        }

        // 检查Ubuntu 22.04特有的安全特性 - secureboot
        CommandResult securebootResult = execCommand(session, "mokutil --sb-state 2>/dev/null || echo 'Not Found'",
                cacheLog);
        if (securebootResult.isSuccess() && !securebootResult.getOutput().contains("Not Found")) {
            cacheLog.info("Secure Boot状态: %s", securebootResult.getOutput().trim());
        }
    }
}