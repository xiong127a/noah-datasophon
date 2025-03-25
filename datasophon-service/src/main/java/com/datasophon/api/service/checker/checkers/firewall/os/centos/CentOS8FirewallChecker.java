package com.datasophon.api.service.checker.checkers.firewall.os.centos;

import com.datasophon.api.service.checker.common.CommandResult;
import com.datasophon.api.service.checker.helpers.CheckLogger;
import com.datasophon.common.model.CheckItem;
import com.datasophon.common.model.HostInfo;
import org.apache.sshd.client.session.ClientSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CentOS 8防火墙检查器
 * 专门处理CentOS 8系统的防火墙检查和修复
 * CentOS 8默认使用更新版本的firewalld作为防火墙
 */
public class CentOS8FirewallChecker extends CentOSFirewallChecker {

    private static final Logger log = LoggerFactory.getLogger(CentOS8FirewallChecker.class);

    @Override
    public CheckItem check(HostInfo hostInfo, CheckItem checkItem, CheckLogger cacheLog) throws InterruptedException {
        cacheLog.info("==== CentOS 8防火墙检查开始 ====");

        // 获取SSH会话
        ClientSession session = hostInfo.getExternalSession();
        if (session == null) {
            String errorMsg = "SSH会话为空，无法执行命令";
            log.error(errorMsg);
            cacheLog.error(errorMsg);
            checkItem.setStatus(CheckItem.Status.FAILED);
            checkItem.setMessage(errorMsg);
            return checkItem;
        }

        // 检查Linux发行版本信息
        CommandResult versionResult = execCommand(session, "cat /etc/redhat-release", cacheLog);
        if (versionResult.isSuccess()) {
            cacheLog.info("系统版本信息: %s", versionResult.getOutput().trim());
        }

        // 确定防火墙类型
        FirewallType firewallType = detectFirewallType(session, cacheLog);
        cacheLog.info("检测到CentOS 8防火墙类型: %s", firewallType.name());

        // CentOS 8特有的检查
        checkCentOS8SpecificServices(session, cacheLog);

        // 执行具体的防火墙检查
        if (firewallType == FirewallType.FIREWALLD) {
            // CentOS 8默认使用firewalld
            cacheLog.info("检测到CentOS 8使用firewalld防火墙(默认)");
            return checkFirewalld(session, checkItem, cacheLog);
        } else if (firewallType == FirewallType.IPTABLES) {
            cacheLog.info("检测到CentOS 8使用iptables防火墙(非默认)");
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
        cacheLog.info("==== CentOS 8防火墙修复开始 ====");

        // 获取SSH会话
        ClientSession session = hostInfo.getExternalSession();
        if (session == null) {
            String errorMsg = "SSH会话为空，无法执行命令";
            log.error(errorMsg);
            cacheLog.error(errorMsg);
            checkItem.setMessage(errorMsg);
            return false;
        }

        // 确定防火墙类型
        FirewallType firewallType = detectFirewallType(session, cacheLog);
        cacheLog.info("检测到CentOS 8防火墙类型: %s", firewallType.name());

        // 执行具体的防火墙修复
        if (firewallType == FirewallType.FIREWALLD) {
            // CentOS 8默认使用firewalld
            cacheLog.info("检测到CentOS 8使用firewalld防火墙(默认)，正在修复...");
            return fixFirewalld(session, checkItem, cacheLog);
        } else if (firewallType == FirewallType.IPTABLES) {
            cacheLog.info("检测到CentOS 8使用iptables防火墙(非默认)，正在修复...");

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
     * 检查CentOS 8特有的服务
     */
    private void checkCentOS8SpecificServices(ClientSession session, CheckLogger cacheLog) throws InterruptedException {
        // 调用父类的通用检查
        checkCentOSSpecificServices(session, cacheLog);

        // 检查是否启用了nftables（CentOS 8使用nftables作为底层实现）
        CommandResult nftablesResult = execCommand(session, "which nft 2>/dev/null || echo 'Not Found'", cacheLog);
        if (nftablesResult.isSuccess() && !nftablesResult.getOutput().contains("Not Found")) {
            cacheLog.info("检测到nftables工具，CentOS 8默认使用nftables作为防火墙后端");

            // 检查nftables规则
            CommandResult nftRulesResult = execCommand(session, "nft list tables 2>/dev/null || echo 'No tables'",
                    cacheLog);
            if (nftRulesResult.isSuccess() && !nftRulesResult.getOutput().contains("No tables")) {
                cacheLog.info("nftables表信息: %s", nftRulesResult.getOutput().trim());
            }
        }

        // 检查CentOS 8特有的firewalld版本
        CommandResult firewalldVersionResult = execCommand(session,
                "firewall-cmd --version 2>/dev/null || echo 'Not Found'", cacheLog);
        if (firewalldVersionResult.isSuccess() && !firewalldVersionResult.getOutput().contains("Not Found")) {
            cacheLog.info("firewalld版本: %s", firewalldVersionResult.getOutput().trim());

            // CentOS 8使用高于0.6.x版本的firewalld
            String version = firewalldVersionResult.getOutput().trim();
            if (version.startsWith("0.6") || version.startsWith("0.7") || version.startsWith("0.8")
                    || version.startsWith("0.9") || version.startsWith("1.")) {
                cacheLog.info("确认为CentOS 8标准的firewalld版本");
            } else {
                cacheLog.warn("检测到非标准firewalld版本: %s，可能存在兼容性问题", version);
            }
        }

        // 检查dnf-automatic服务状态（CentOS 8特有）
        CommandResult dnfAutomaticResult = execCommand(session,
                "systemctl status dnf-automatic.timer 2>/dev/null || echo 'Not Found'", cacheLog);
        if (dnfAutomaticResult.isSuccess() && !dnfAutomaticResult.getOutput().contains("Not Found")) {
            cacheLog.info("检测到dnf-automatic服务，该服务可能影响系统更新行为");
        }
    }
}