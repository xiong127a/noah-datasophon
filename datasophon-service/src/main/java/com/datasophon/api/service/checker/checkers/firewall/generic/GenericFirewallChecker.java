package com.datasophon.api.service.checker.checkers.firewall.generic;

import com.datasophon.api.service.checker.checkers.firewall.FirewallCheckerStrategy;
import com.datasophon.api.service.checker.common.CommandResult;
import com.datasophon.api.service.checker.helpers.CheckLogger;
import com.datasophon.common.model.CheckItem;
import com.datasophon.common.model.HostInfo;
import org.apache.sshd.client.session.ClientSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 通用防火墙检查器
 * 提供基本的防火墙检查和修复功能
 */
public class GenericFirewallChecker implements FirewallCheckerStrategy {

    private static final Logger log = LoggerFactory.getLogger(GenericFirewallChecker.class);

    /**
     * 防火墙类型枚举
     */
    protected enum FirewallType {
        FIREWALLD, // CentOS 7/8, RHEL 7/8, Kylin
        UFW, // Ubuntu
        IPTABLES, // 传统防火墙
        NONE // 未检测到防火墙
    }

    @Override
    public CheckItem check(HostInfo hostInfo, CheckItem checkItem, CheckLogger cacheLog) throws InterruptedException {
        cacheLog.info("==== 通用防火墙检查开始 ====");

        checkItem.setMessage("正在检查防火墙状态...");

        // 获取SSH会话
        ClientSession session = hostInfo.getExternalSession();
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
        ClientSession session = hostInfo.getExternalSession();
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
    protected FirewallType detectFirewallType(ClientSession session, CheckLogger cacheLog) throws InterruptedException {
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
        cacheLog.info("执行检查命令: systemctl status firewalld");
        CommandResult result = execCommand(session, "systemctl status firewalld", cacheLog);

        switch (result.getExitCode()) {
            case 0:
                // 服务正在运行
                cacheLog.info("firewalld状态: 正在运行");

                // 获取防火墙详细配置
                CommandResult zoneResult = execCommand(session, "firewall-cmd --get-active-zones", cacheLog);
                if (zoneResult.isSuccess()) {
                    cacheLog.info("当前活动区域信息:");
                    cacheLog.info(zoneResult.getOutput());
                }

                CommandResult configResult = execCommand(session, "firewall-cmd --list-all", cacheLog);
                if (configResult.isSuccess()) {
                    cacheLog.info("当前防火墙配置:");
                    cacheLog.info(configResult.getOutput());
                }

                checkItem.setStatus(CheckItem.Status.FAILED);
                checkItem.setMessage("firewalld防火墙正在运行，建议关闭");
                break;

            case 3:
                // 服务已停止
                cacheLog.info("firewalld状态: 已停止");

                // 检查自启动状态
                CommandResult enabledResult = execCommand(session,
                        "systemctl is-enabled firewalld 2>/dev/null || echo 'Unknown'", cacheLog);
                if (enabledResult.isSuccess() && enabledResult.getOutput().trim().equals("enabled")) {
                    cacheLog.info("firewalld自启动状态: 已启用");
                    checkItem.setStatus(CheckItem.Status.FAILED);
                    checkItem.setMessage("firewalld防火墙已配置为自启动，建议禁用");
                } else {
                    checkItem.setStatus(CheckItem.Status.SUCCESS);
                    checkItem.setMessage("firewalld防火墙已关闭");
                }
                break;

            case 4:
                // 服务不存在
                cacheLog.info("firewalld状态: 服务不存在");
                checkItem.setStatus(CheckItem.Status.SUCCESS);
                checkItem.setMessage("firewalld防火墙服务未安装");
                break;

            default:
                // 其他状态，可能是命令执行出错
                cacheLog.warn("获取firewalld防火墙状态失败，退出状态码: %d", result.getExitCode());
                checkItem.setStatus(CheckItem.Status.FAILED);
                checkItem.setMessage("获取firewalld防火墙状态失败: " + result.getErrorOrOutput());
                break;
        }

        return checkItem;
    }

    /**
     * 检查ufw防火墙
     */
    protected CheckItem checkUfw(ClientSession session, CheckItem checkItem, CheckLogger cacheLog)
            throws InterruptedException {
        cacheLog.info("执行检查命令: ufw status");
        CommandResult result = execCommand(session, "ufw status", cacheLog);

        if (result.isSuccess()) {
            String output = result.getOutput().toLowerCase();

            if (output.contains("inactive") || output.contains("disabled")) {
                // 检查是否设置为自启动
                CommandResult enabledResult = execCommand(session,
                        "grep -q 'ENABLED=yes' /etc/ufw/ufw.conf && echo 'enabled' || echo 'disabled'", cacheLog);
                if (enabledResult.isSuccess() && enabledResult.getOutput().trim().equals("enabled")) {
                    cacheLog.info("ufw状态: 已停止但设置为自启动");
                    checkItem.setStatus(CheckItem.Status.FAILED);
                    checkItem.setMessage("ufw防火墙已配置为自启动，建议禁用");
                } else {
                    cacheLog.info("ufw状态: 已停止且未设置自启动");
                    checkItem.setStatus(CheckItem.Status.SUCCESS);
                    checkItem.setMessage("ufw防火墙已关闭");
                }
            } else if (output.contains("active") || output.contains("enabled")) {
                cacheLog.info("ufw状态: 正在运行");

                // 获取ufw规则
                cacheLog.info("当前ufw规则:");
                cacheLog.info(output);

                checkItem.setStatus(CheckItem.Status.FAILED);
                checkItem.setMessage("ufw防火墙正在运行，建议关闭");
            } else {
                // 状态不明
                cacheLog.warn("无法确定ufw状态: %s", output);
                checkItem.setStatus(CheckItem.Status.FAILED);
                checkItem.setMessage("无法确定ufw防火墙状态，请手动检查: " + output);
            }
        } else {
            // 命令执行失败，可能是ufw未安装
            cacheLog.info("ufw命令执行失败，可能未安装: %s", result.getErrorOrOutput());
            checkItem.setStatus(CheckItem.Status.SUCCESS);
            checkItem.setMessage("ufw防火墙未安装");
        }

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
            boolean hasRules = false;

            // 检查是否有非默认规则
            if (!output.contains("Chain INPUT (policy ACCEPT)") ||
                    !output.contains("Chain FORWARD (policy ACCEPT)") ||
                    !output.contains("Chain OUTPUT (policy ACCEPT)") ||
                    output.contains("REJECT") ||
                    output.contains("DROP")) {
                hasRules = true;
            }

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
    protected boolean fixUfw(ClientSession session, CheckItem checkItem, CheckLogger cacheLog)
            throws InterruptedException {
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
     * 执行命令
     *
     * @param session  SSH会话
     * @param command  要执行的命令
     * @param cacheLog 日志记录器
     * @return 命令执行结果
     */
    protected CommandResult execCommand(ClientSession session, String command, CheckLogger cacheLog)
            throws InterruptedException {
        if (session == null || !session.isOpen()) {
            String errorMsg = "SSH会话为空或已关闭，无法执行命令";
            log.error(errorMsg);
            cacheLog.error(errorMsg);
            return new CommandResult("", errorMsg, -1);
        }

        try {
            // 记录执行的命令
            cacheLog.debug("执行命令: %s", command);

            // 创建输出流
            java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
            java.io.ByteArrayOutputStream errorStream = new java.io.ByteArrayOutputStream();

            // 创建通道
            org.apache.sshd.client.channel.ClientChannel channel = session.createExecChannel(command);
            channel.setOut(outputStream);
            channel.setErr(errorStream);

            // 打开通道
            channel.open().verify(30, java.util.concurrent.TimeUnit.SECONDS);

            // 等待命令完成
            channel.waitFor(java.util.EnumSet.of(
                    org.apache.sshd.client.channel.ClientChannelEvent.CLOSED), 30000);

            // 获取退出状态
            Integer exitStatus = channel.getExitStatus();
            String output = outputStream.toString();
            String error = errorStream.toString();

            // 关闭通道
            channel.close();

            // 创建结果
            CommandResult result = new CommandResult(output, error, exitStatus != null ? exitStatus : -1);

            // 记录执行结果
            if (result.isSuccess()) {
                cacheLog.debug("命令执行成功，退出状态码: %d", result.getExitCode());
            } else {
                cacheLog.debug("命令执行失败，退出状态码: %d, 错误信息: %s", result.getExitCode(), result.getError());
            }

            return result;

        } catch (Exception e) {
            String errorMsg = "执行命令异常: " + e.getMessage();
            log.error(errorMsg, e);
            cacheLog.error(errorMsg);
            return new CommandResult("", errorMsg, -1);
        }
    }
}