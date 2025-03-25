package com.datasophon.api.service.checker.checkers.firewall.os.centos;

import com.datasophon.api.service.checker.common.CommandResult;
import com.datasophon.api.service.checker.helpers.CheckLogger;
import com.datasophon.common.model.CheckItem;
import com.datasophon.common.model.HostInfo;
import org.apache.sshd.client.session.ClientSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CentOS 7防火墙检查器
 * 专门处理CentOS 7系统的防火墙检查和修复
 * CentOS 7默认使用firewalld作为防火墙
 */
public class CentOS7FirewallChecker extends CentOSFirewallChecker {

    private static final Logger log = LoggerFactory.getLogger(CentOS7FirewallChecker.class);

    @Override
    public CheckItem check(HostInfo hostInfo, CheckItem checkItem, CheckLogger cacheLog) throws InterruptedException {
        cacheLog.info("==== CentOS 7防火墙检查开始 ====");

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
        cacheLog.info("检测到CentOS 7防火墙类型: %s", firewallType.name());

        // CentOS 7特有的检查
        checkCentOS7SpecificServices(session, cacheLog);

        // 执行具体的防火墙检查
        if (firewallType == FirewallType.FIREWALLD) {
            // CentOS 7默认使用firewalld
            cacheLog.info("检测到CentOS 7使用firewalld防火墙(默认)");
            return checkFirewalld(session, checkItem, cacheLog);
        } else if (firewallType == FirewallType.IPTABLES) {
            cacheLog.info("检测到CentOS 7使用iptables防火墙(非默认)");
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
        cacheLog.info("==== CentOS 7防火墙修复开始 ====");

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
        cacheLog.info("检测到CentOS 7防火墙类型: %s", firewallType.name());

        // 执行具体的防火墙修复
        if (firewallType == FirewallType.FIREWALLD) {
            // CentOS 7默认使用firewalld
            cacheLog.info("检测到CentOS 7使用firewalld防火墙(默认)，正在修复...");
            return fixFirewalld(session, checkItem, cacheLog);
        } else if (firewallType == FirewallType.IPTABLES) {
            cacheLog.info("检测到CentOS 7使用iptables防火墙(非默认)，正在修复...");

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
     * 检查CentOS 7特有的服务
     */
    private void checkCentOS7SpecificServices(ClientSession session, CheckLogger cacheLog) throws InterruptedException {
        // 调用父类的通用检查
        checkCentOSSpecificServices(session, cacheLog);

        // 检查是否启用了SELinux（可能影响防火墙规则）
        CommandResult selinuxResult = execCommand(session, "getenforce 2>/dev/null || echo 'Unknown'", cacheLog);
        if (selinuxResult.isSuccess() && !selinuxResult.getOutput().trim().equalsIgnoreCase("Disabled")) {
            cacheLog.warn("SELinux状态: %s，可能影响防火墙规则", selinuxResult.getOutput().trim());
        }

        // 检查CentOS 7特有的firewalld版本
        CommandResult firewalldVersionResult = execCommand(session,
                "firewall-cmd --version 2>/dev/null || echo 'Not Found'", cacheLog);
        if (firewalldVersionResult.isSuccess() && !firewalldVersionResult.getOutput().contains("Not Found")) {
            cacheLog.info("firewalld版本: %s", firewalldVersionResult.getOutput().trim());
        }
    }
}