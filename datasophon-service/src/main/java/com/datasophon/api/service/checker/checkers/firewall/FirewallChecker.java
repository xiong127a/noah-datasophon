package com.datasophon.api.service.checker.checkers.firewall;

import com.datasophon.api.service.checker.core.AbstractItemChecker;
import com.datasophon.api.service.checker.common.CommandResult;
import com.datasophon.api.service.checker.common.OsInfo;
import com.datasophon.common.model.CheckItem;
import com.datasophon.common.model.HostInfo;
import com.datasophon.common.model.ItemCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 防火墙检查器
 * 支持多个Linux发行版的防火墙检查和修复，包括：
 * - CentOS/RHEL 7/8: firewalld
 * - Ubuntu 22.04/24.04: ufw
 * - Kylin V4/V10: firewalld
 * - 其他支持systemd的发行版: firewalld
 * - 传统发行版: iptables
 */
@Component
public class FirewallChecker extends AbstractItemChecker {

    private static final Logger logger = LoggerFactory.getLogger(FirewallChecker.class);

    // 防火墙类型枚举
    private enum FirewallType {
        FIREWALLD, // CentOS 7/8, RHEL 7/8, Kylin
        UFW, // Ubuntu
        IPTABLES, // 传统防火墙
        NONE // 未检测到防火墙
    }

    @Override
    protected CheckItem doCheck(HostInfo hostInfo, CheckItem checkItem) throws InterruptedException {
        try {
            cacheLog.info("==== 防火墙检查开始 ====");

            // 更新状态为正在检测操作系统类型
            setCheckItemMessage(hostInfo, checkItem, "正在检测操作系统类型...");

            // 获取操作系统信息
            OsInfo osInfo = getOsInfo(hostInfo);
            cacheLog.info("检测到操作系统: %s, 版本: %s", osInfo.getFullName(), osInfo.getVersionId());

            // 确定防火墙类型
            FirewallType firewallType = detectFirewallType(osInfo);
            cacheLog.info("检测到防火墙类型: %s", firewallType.name());

            // 更新状态为正在检查防火墙状态
            setCheckItemMessage(hostInfo, checkItem, "正在检查防火墙状态...");

            // 根据不同的防火墙类型执行相应的检查
            switch (firewallType) {
                case FIREWALLD:
                    return checkFirewalld(hostInfo, checkItem);
                case UFW:
                    return checkUfw(hostInfo, checkItem);
                case IPTABLES:
                    return checkIptables(hostInfo, checkItem);
                case NONE:
                    // 没有检测到防火墙，直接返回成功
                    cacheLog.info("未检测到防火墙服务");
                    checkItem.setStatus(CheckItem.Status.SUCCESS);
                    setCheckItemMessage(hostInfo, checkItem, "未检测到防火墙服务");
                    return checkItem;
                default:
                    cacheLog.warn("未知的防火墙类型");
                    checkItem.setStatus(CheckItem.Status.FAILED);
                    setCheckItemMessage(hostInfo, checkItem, "未知的防火墙类型，请手动检查");
                    return checkItem;
            }

        } catch (Exception e) {
            String errorMsg = "防火墙检查失败: " + e.getMessage();
            logger.error(errorMsg, e);
            cacheLog.error("错误: %s", errorMsg);
            checkItem.setStatus(CheckItem.Status.FAILED);
            setCheckItemMessage(hostInfo, checkItem, errorMsg);
            return checkItem;
        } finally {
            cacheLog.info("==== 防火墙检查完成 ====");
        }
    }

    /**
     * 检测系统使用的防火墙类型
     */
    private FirewallType detectFirewallType(OsInfo osInfo) throws InterruptedException {
        // 默认根据操作系统类型推断防火墙类型
        if (osInfo.isDistribution(LinuxDistribution.UBUNTU)) {
            // Ubuntu 默认使用UFW
            CommandResult ufwResult = execCommand(session, "which ufw 2>/dev/null || echo 'Not Found'");
            if (ufwResult.isSuccess() && !ufwResult.getOutput().contains("Not Found")) {
                return FirewallType.UFW;
            }
        } else if (osInfo.isDistribution(LinuxDistribution.CENTOS) ||
                osInfo.isDistribution(LinuxDistribution.REDHAT) ||
                osInfo.isDistribution(LinuxDistribution.KYLIN)) {
            // CentOS/RHEL/Kylin 7及以上默认使用firewalld
            if (osInfo.usesSystemd()) {
                CommandResult firewalldResult = execCommand(session,
                        "systemctl status firewalld 2>/dev/null || echo 'Not Found'");
                if (!firewalldResult.getOutput().contains("Not Found")) {
                    return FirewallType.FIREWALLD;
                }
            }
        }

        // 检查是否安装了firewalld（通用方法）
        CommandResult firewalldResult = execCommand(session, "which firewall-cmd 2>/dev/null || echo 'Not Found'");
        if (firewalldResult.isSuccess() && !firewalldResult.getOutput().contains("Not Found")) {
            return FirewallType.FIREWALLD;
        }

        // 检查是否安装了UFW（通用方法）
        CommandResult ufwResult = execCommand(session, "which ufw 2>/dev/null || echo 'Not Found'");
        if (ufwResult.isSuccess() && !ufwResult.getOutput().contains("Not Found")) {
            return FirewallType.UFW;
        }

        // 检查是否使用传统的iptables
        CommandResult iptablesResult = execCommand(session, "which iptables 2>/dev/null || echo 'Not Found'");
        if (iptablesResult.isSuccess() && !iptablesResult.getOutput().contains("Not Found")) {
            // 检查iptables服务状态
            CommandResult iptablesServiceResult = execCommand(session,
                    "systemctl status iptables 2>/dev/null || service iptables status 2>/dev/null || echo 'Not Found'");
            if (!iptablesServiceResult.getOutput().contains("Not Found")) {
                return FirewallType.IPTABLES;
            }

            // 检查iptables规则是否存在
            CommandResult iptablesRulesResult = execCommand(session, "iptables -L 2>/dev/null");
            if (iptablesRulesResult.isSuccess()) {
                // 如果能够列出iptables规则，但不是默认的空规则，则认为iptables在使用中
                if (!iptablesRulesResult.getOutput().contains("Chain INPUT (policy ACCEPT)") ||
                        !iptablesRulesResult.getOutput().contains("Chain FORWARD (policy ACCEPT)") ||
                        !iptablesRulesResult.getOutput().contains("Chain OUTPUT (policy ACCEPT)")) {
                    return FirewallType.IPTABLES;
                }
            }
        }

        // 没有检测到任何防火墙
        return FirewallType.NONE;
    }

    /**
     * 检查firewalld防火墙
     */
    private CheckItem checkFirewalld(HostInfo hostInfo, CheckItem checkItem) throws InterruptedException {
        cacheLog.info("执行检查命令: systemctl status firewalld");
        CommandResult result = execCommand(session, "systemctl status firewalld");

        // 根据退出状态码判断防火墙状态
        switch (result.getExitCode()) {
            case 0:
                // 服务正在运行
                cacheLog.info("firewalld状态: 正在运行");
                checkItem.setStatus(CheckItem.Status.FAILED);
                setCheckItemMessage(hostInfo, checkItem, "firewalld防火墙正在运行，建议关闭");

                // 获取更多配置信息
                try {
                    CommandResult firewallConfig = execCommand(session, "firewall-cmd --list-all");
                    if (firewallConfig.isSuccess()) {
                        cacheLog.info("当前防火墙配置信息:");
                        cacheLog.info(firewallConfig.getOutput());
                    }
                } catch (Exception e) {
                    cacheLog.warn("获取防火墙配置信息失败: %s", e.getMessage());
                }
                break;

            case 3:
                // 服务已停止
                cacheLog.info("firewalld状态: 已停止");
                checkItem.setStatus(CheckItem.Status.SUCCESS);
                setCheckItemMessage(hostInfo, checkItem, "firewalld防火墙已关闭");
                break;

            case 4:
                // 服务不存在
                cacheLog.info("firewalld状态: 服务不存在");
                checkItem.setStatus(CheckItem.Status.SUCCESS);
                setCheckItemMessage(hostInfo, checkItem, "firewalld防火墙服务未安装");
                break;

            default:
                // 其他状态，可能是命令执行出错
                cacheLog.warn("获取firewalld防火墙状态失败，退出状态码: %d", result.getExitCode());
                checkItem.setStatus(CheckItem.Status.FAILED);
                setCheckItemMessage(hostInfo, checkItem, "获取firewalld防火墙状态失败: " + result.getErrorOrOutput());
                break;
        }

        return checkItem;
    }

    /**
     * 检查UFW防火墙
     */
    private CheckItem checkUfw(HostInfo hostInfo, CheckItem checkItem) throws InterruptedException {
        cacheLog.info("执行检查命令: ufw status");
        CommandResult result = execCommand(session, "ufw status");

        if (result.isSuccess()) {
            String output = result.getOutput().toLowerCase();

            if (output.contains("inactive") || output.contains("已禁用")) {
                // UFW已禁用
                cacheLog.info("UFW状态: 已禁用");
                checkItem.setStatus(CheckItem.Status.SUCCESS);
                setCheckItemMessage(hostInfo, checkItem, "UFW防火墙已禁用");
            } else if (output.contains("active") || output.contains("已启用")) {
                // UFW已启用
                cacheLog.info("UFW状态: 已启用");
                checkItem.setStatus(CheckItem.Status.FAILED);
                setCheckItemMessage(hostInfo, checkItem, "UFW防火墙已启用，建议关闭");

                // 获取更多配置信息
                cacheLog.info("当前UFW配置信息:");
                cacheLog.info(output);
            } else {
                // 状态不明
                cacheLog.warn("无法确定UFW状态: %s", output);
                checkItem.setStatus(CheckItem.Status.FAILED);
                setCheckItemMessage(hostInfo, checkItem, "无法确定UFW防火墙状态，请手动检查");
            }
        } else {
            // 命令执行失败
            cacheLog.warn("获取UFW状态失败: %s", result.getErrorOrOutput());
            checkItem.setStatus(CheckItem.Status.FAILED);
            setCheckItemMessage(hostInfo, checkItem, "获取UFW防火墙状态失败: " + result.getErrorOrOutput());
        }

        return checkItem;
    }

    /**
     * 检查iptables防火墙
     */
    private CheckItem checkIptables(HostInfo hostInfo, CheckItem checkItem) throws InterruptedException {
        cacheLog.info("执行检查命令: systemctl status iptables || service iptables status");

        // 尝试通过systemd检查服务状态
        CommandResult systemdResult = execCommand(session, "systemctl status iptables 2>/dev/null || echo 'Not Found'");
        if (systemdResult.isSuccess() && !systemdResult.getOutput().contains("Not Found")) {
            if (systemdResult.getExitCode() == 0) {
                // 服务正在运行
                cacheLog.info("iptables服务状态: 正在运行");
                checkItem.setStatus(CheckItem.Status.FAILED);
                setCheckItemMessage(hostInfo, checkItem, "iptables防火墙服务正在运行，建议关闭");
                return checkItem;
            } else if (systemdResult.getExitCode() == 3) {
                // 服务已停止
                cacheLog.info("iptables服务状态: 已停止");
                checkItem.setStatus(CheckItem.Status.SUCCESS);
                setCheckItemMessage(hostInfo, checkItem, "iptables防火墙服务已停止");
                return checkItem;
            }
        }

        // 尝试通过传统服务管理检查状态
        CommandResult serviceResult = execCommand(session, "service iptables status 2>/dev/null || echo 'Not Found'");
        if (serviceResult.isSuccess() && !serviceResult.getOutput().contains("Not Found")) {
            if (serviceResult.getOutput().toLowerCase().contains("running")) {
                // 服务正在运行
                cacheLog.info("iptables服务状态: 正在运行");
                checkItem.setStatus(CheckItem.Status.FAILED);
                setCheckItemMessage(hostInfo, checkItem, "iptables防火墙服务正在运行，建议关闭");
                return checkItem;
            } else if (serviceResult.getOutput().toLowerCase().contains("stopped") ||
                    serviceResult.getOutput().toLowerCase().contains("not running")) {
                // 服务已停止
                cacheLog.info("iptables服务状态: 已停止");
                checkItem.setStatus(CheckItem.Status.SUCCESS);
                setCheckItemMessage(hostInfo, checkItem, "iptables防火墙服务已停止");
                return checkItem;
            }
        }

        // 如果无法通过服务状态判断，则检查iptables规则
        cacheLog.info("无法确定iptables服务状态，检查iptables规则...");
        CommandResult rulesResult = execCommand(session, "iptables -L");

        if (rulesResult.isSuccess()) {
            // 检查是否有非默认规则
            boolean hasRules = false;
            String output = rulesResult.getOutput();

            // 如果某个链的策略不是ACCEPT，或者有自定义规则，则认为有规则
            if (!output.contains("Chain INPUT (policy ACCEPT)") ||
                    !output.contains("Chain FORWARD (policy ACCEPT)") ||
                    !output.contains("Chain OUTPUT (policy ACCEPT)") ||
                    output.contains("REJECT") ||
                    output.contains("DROP")) {
                hasRules = true;
            }

            // 检查规则数量
            int ruleCount = 0;
            for (String line : output.split("\n")) {
                // 跳过表头和空行
                if (line.trim().isEmpty() || line.startsWith("Chain") || line.startsWith("target")) {
                    continue;
                }
                ruleCount++;
            }

            if (hasRules || ruleCount > 0) {
                cacheLog.info("检测到iptables有自定义规则");
                checkItem.setStatus(CheckItem.Status.FAILED);
                setCheckItemMessage(hostInfo, checkItem, "iptables防火墙配置了规则，建议清除");
            } else {
                cacheLog.info("iptables没有配置规则");
                checkItem.setStatus(CheckItem.Status.SUCCESS);
                setCheckItemMessage(hostInfo, checkItem, "iptables防火墙未配置规则");
            }
        } else {
            // iptables命令执行失败
            cacheLog.warn("执行iptables -L命令失败: %s", rulesResult.getErrorOrOutput());
            checkItem.setStatus(CheckItem.Status.FAILED);
            setCheckItemMessage(hostInfo, checkItem, "无法获取iptables规则，请手动检查");
        }

        return checkItem;
    }

    @Override
    protected boolean doFix(HostInfo hostInfo, CheckItem checkItem) {
        try {
            cacheLog.info("==== 开始修复防火墙配置 ====");

            // 更新状态为正在检测操作系统类型
            setCheckItemMessage(hostInfo, checkItem, "正在检测操作系统类型...");

            // 获取操作系统信息
            OsInfo osInfo = getOsInfo(hostInfo);
            cacheLog.info("检测到操作系统: %s, 版本: %s", osInfo.getFullName(), osInfo.getVersionId());

            // 确定防火墙类型
            FirewallType firewallType = detectFirewallType(osInfo);
            cacheLog.info("检测到防火墙类型: %s", firewallType.name());

            // 更新状态为正在修复防火墙配置
            setCheckItemMessage(hostInfo, checkItem, "正在修复防火墙配置...");

            // 根据不同的防火墙类型执行相应的修复
            switch (firewallType) {
                case FIREWALLD:
                    return fixFirewalld(hostInfo, checkItem);
                case UFW:
                    return fixUfw(hostInfo, checkItem);
                case IPTABLES:
                    return fixIptables(hostInfo, checkItem);
                case NONE:
                    // 没有检测到防火墙，直接返回成功
                    cacheLog.info("未检测到防火墙服务，无需修复");
                    setCheckItemMessage(hostInfo, checkItem, "未检测到防火墙服务，无需修复");
                    return true;
                default:
                    cacheLog.warn("未知的防火墙类型，无法修复");
                    setCheckItemMessage(hostInfo, checkItem, "未知的防火墙类型，请手动检查");
                    return false;
            }

        } catch (Exception e) {
            String errorMsg = "防火墙配置修复失败: " + e.getMessage();
            logger.error(errorMsg);
            cacheLog.error("错误: " + errorMsg);
            setCheckItemMessage(hostInfo, checkItem, "防火墙配置修复失败: " + e.getMessage());
            return false;
        } finally {
            cacheLog.info("==== 防火墙配置修复完成 ====");
        }
    }

    /**
     * 修复firewalld防火墙
     */
    private boolean fixFirewalld(HostInfo hostInfo, CheckItem checkItem) throws InterruptedException {
        // 更新状态为正在检查防火墙当前状态
        setCheckItemMessage(hostInfo, checkItem, "正在检查firewalld防火墙当前状态...");

        // 先检查防火墙状态
        cacheLog.info("检查firewalld防火墙当前状态...");
        CommandResult statusResult = execCommand(session, "systemctl status firewalld");

        // 如果服务不存在，直接返回成功
        if (statusResult.getExitCode() == 4) {
            cacheLog.info("firewalld防火墙服务未安装，无需修复");
            setCheckItemMessage(hostInfo, checkItem, "firewalld防火墙服务未安装，无需修复");
            return true;
        }

        // 如果服务已停止，检查是否已禁用自启动
        if (statusResult.getExitCode() == 3) {
            // 更新状态为正在检查自启动状态
            setCheckItemMessage(hostInfo, checkItem, "正在检查firewalld防火墙自启动状态...");

            CommandResult isEnabledResult = execCommand(session, "systemctl is-enabled firewalld");
            if (isEnabledResult.isSuccess() && isEnabledResult.getOutput().trim().equals("disabled")) {
                cacheLog.info("firewalld防火墙已关闭且已禁用自启动，无需修复");
                setCheckItemMessage(hostInfo, checkItem, "firewalld防火墙已关闭且已禁用自启动");
                return true;
            }
        }

        // 更新状态为正在停止防火墙服务
        setCheckItemMessage(hostInfo, checkItem, "正在停止firewalld防火墙服务...");

        // 停止并禁用防火墙
        cacheLog.info("正在停止firewalld防火墙服务...");
        CommandResult stopResult = execCommand(session, "systemctl stop firewalld");
        if (!stopResult.isSuccess()) {
            cacheLog.error("停止firewalld防火墙服务失败: %s", stopResult.getErrorOrOutput());
            setCheckItemMessage(hostInfo, checkItem, "停止firewalld防火墙服务失败: " + stopResult.getErrorOrOutput());
            return false;
        }
        cacheLog.info("停止firewalld防火墙服务完成");

        // 更新状态为正在禁用防火墙自启动
        setCheckItemMessage(hostInfo, checkItem, "正在禁用firewalld防火墙自启动...");

        cacheLog.info("正在禁用firewalld防火墙自启动...");
        CommandResult disableResult = execCommand(session, "systemctl disable firewalld");
        if (!disableResult.isSuccess()) {
            cacheLog.error("禁用firewalld防火墙自启动失败: %s", disableResult.getErrorOrOutput());
            setCheckItemMessage(hostInfo, checkItem, "禁用firewalld防火墙自启动失败: " + disableResult.getErrorOrOutput());
            return false;
        }
        cacheLog.info("禁用firewalld防火墙自启动完成");

        // 更新状态为正在验证防火墙状态
        setCheckItemMessage(hostInfo, checkItem, "正在验证firewalld防火墙状态...");

        // 再次检查确认防火墙已关闭
        cacheLog.info("验证firewalld防火墙状态...");
        CommandResult verifyResult = execCommand(session, "systemctl status firewalld");
        if (verifyResult.getExitCode() != 3) {
            cacheLog.warn("警告: firewalld防火墙服务可能未成功关闭，请手动检查");
            setCheckItemMessage(hostInfo, checkItem, "警告: firewalld防火墙服务可能未成功关闭，请手动检查");
            return false;
        }

        cacheLog.info("验证成功: firewalld防火墙已关闭");
        setCheckItemMessage(hostInfo, checkItem, "firewalld防火墙已成功关闭并禁用自启动");
        return true;
    }

    /**
     * 修复UFW防火墙
     */
    private boolean fixUfw(HostInfo hostInfo, CheckItem checkItem) throws InterruptedException {
        // 更新状态为正在检查UFW当前状态
        setCheckItemMessage(hostInfo, checkItem, "正在检查UFW防火墙当前状态...");

        // 检查UFW状态
        cacheLog.info("检查UFW防火墙当前状态...");
        CommandResult statusResult = execCommand(session, "ufw status");

        if (statusResult.isSuccess() &&
                (statusResult.getOutput().toLowerCase().contains("inactive") ||
                        statusResult.getOutput().toLowerCase().contains("已禁用"))) {
            // UFW已禁用，检查自启动状态
            CommandResult enabledResult = execCommand(session,
                    "grep -q \"^ENABLED=yes\" /etc/ufw/ufw.conf && echo \"enabled\" || echo \"disabled\"");
            if (enabledResult.isSuccess() && enabledResult.getOutput().trim().equals("disabled")) {
                cacheLog.info("UFW防火墙已禁用且未设置自启动，无需修复");
                setCheckItemMessage(hostInfo, checkItem, "UFW防火墙已禁用且未设置自启动");
                return true;
            }
        }

        // 更新状态为正在禁用UFW防火墙
        setCheckItemMessage(hostInfo, checkItem, "正在禁用UFW防火墙...");

        // 禁用UFW
        cacheLog.info("正在禁用UFW防火墙...");
        CommandResult disableResult = execCommand(session, "ufw disable");
        if (!disableResult.isSuccess()) {
            cacheLog.error("禁用UFW防火墙失败: %s", disableResult.getErrorOrOutput());
            setCheckItemMessage(hostInfo, checkItem, "禁用UFW防火墙失败: " + disableResult.getErrorOrOutput());
            return false;
        }

        // 禁用UFW自启动
        cacheLog.info("正在禁用UFW防火墙自启动...");
        CommandResult disableAutoResult = execCommand(session,
                "sed -i 's/^ENABLED=yes/ENABLED=no/g' /etc/ufw/ufw.conf");
        if (!disableAutoResult.isSuccess()) {
            cacheLog.error("禁用UFW防火墙自启动失败: %s", disableAutoResult.getErrorOrOutput());
            setCheckItemMessage(hostInfo, checkItem, "禁用UFW防火墙自启动失败: " + disableAutoResult.getErrorOrOutput());
            return false;
        }

        // 更新状态为正在验证UFW状态
        setCheckItemMessage(hostInfo, checkItem, "正在验证UFW防火墙状态...");

        // 再次检查确认UFW已禁用
        cacheLog.info("验证UFW防火墙状态...");
        CommandResult verifyResult = execCommand(session, "ufw status");
        if (verifyResult.isSuccess() &&
                (verifyResult.getOutput().toLowerCase().contains("inactive") ||
                        verifyResult.getOutput().toLowerCase().contains("已禁用"))) {
            cacheLog.info("验证成功: UFW防火墙已禁用");
            setCheckItemMessage(hostInfo, checkItem, "UFW防火墙已成功禁用");
            return true;
        } else {
            cacheLog.warn("警告: UFW防火墙可能未成功禁用，请手动检查");
            setCheckItemMessage(hostInfo, checkItem, "警告: UFW防火墙可能未成功禁用，请手动检查");
            return false;
        }
    }

    /**
     * 修复iptables防火墙
     */
    private boolean fixIptables(HostInfo hostInfo, CheckItem checkItem) throws InterruptedException {
        // 更新状态为正在检查iptables当前状态
        setCheckItemMessage(hostInfo, checkItem, "正在检查iptables防火墙当前状态...");

        // 检查iptables服务状态
        boolean serviceRunning = false;
        boolean systemdService = false;

        // 检查systemd服务
        cacheLog.info("检查iptables服务状态（systemd）...");
        CommandResult systemdResult = execCommand(session, "systemctl status iptables 2>/dev/null || echo 'Not Found'");
        if (systemdResult.isSuccess() && !systemdResult.getOutput().contains("Not Found")) {
            systemdService = true;
            if (systemdResult.getExitCode() == 0) {
                serviceRunning = true;
            }
        }

        // 检查传统服务
        if (!systemdService) {
            cacheLog.info("检查iptables服务状态（传统服务）...");
            CommandResult serviceResult = execCommand(session,
                    "service iptables status 2>/dev/null || echo 'Not Found'");
            if (serviceResult.isSuccess() && !serviceResult.getOutput().contains("Not Found")) {
                if (serviceResult.getOutput().toLowerCase().contains("running")) {
                    serviceRunning = true;
                }
            }
        }

        // 如果服务在运行，停止服务
        if (serviceRunning) {
            // 更新状态为正在停止iptables服务
            setCheckItemMessage(hostInfo, checkItem, "正在停止iptables服务...");

            cacheLog.info("正在停止iptables服务...");
            if (systemdService) {
                CommandResult stopResult = execCommand(session, "systemctl stop iptables");
                if (!stopResult.isSuccess()) {
                    cacheLog.error("停止iptables服务失败: %s", stopResult.getErrorOrOutput());
                    setCheckItemMessage(hostInfo, checkItem, "停止iptables服务失败: " + stopResult.getErrorOrOutput());
                    return false;
                }

                // 禁用自启动
                cacheLog.info("正在禁用iptables服务自启动...");
                CommandResult disableResult = execCommand(session, "systemctl disable iptables");
                if (!disableResult.isSuccess()) {
                    cacheLog.error("禁用iptables服务自启动失败: %s", disableResult.getErrorOrOutput());
                    setCheckItemMessage(hostInfo, checkItem, "禁用iptables服务自启动失败: " + disableResult.getErrorOrOutput());
                    return false;
                }
            } else {
                CommandResult stopResult = execCommand(session, "service iptables stop");
                if (!stopResult.isSuccess()) {
                    cacheLog.error("停止iptables服务失败: %s", stopResult.getErrorOrOutput());
                    setCheckItemMessage(hostInfo, checkItem, "停止iptables服务失败: " + stopResult.getErrorOrOutput());
                    return false;
                }

                // 禁用自启动（多种方式尝试）
                cacheLog.info("正在禁用iptables服务自启动...");
                execCommand(session, "chkconfig iptables off 2>/dev/null || true");
                execCommand(session, "update-rc.d iptables disable 2>/dev/null || true");
            }
        }

        // 清空iptables规则
        setCheckItemMessage(hostInfo, checkItem, "正在清空iptables规则...");

        cacheLog.info("正在清空iptables规则...");
        CommandResult flushResult = execCommand(session,
                "iptables -F && iptables -X && iptables -t nat -F && iptables -t nat -X && iptables -t mangle -F && iptables -t mangle -X && iptables -P INPUT ACCEPT && iptables -P FORWARD ACCEPT && iptables -P OUTPUT ACCEPT");
        if (!flushResult.isSuccess()) {
            cacheLog.error("清空iptables规则失败: %s", flushResult.getErrorOrOutput());
            setCheckItemMessage(hostInfo, checkItem, "清空iptables规则失败: " + flushResult.getErrorOrOutput());
            return false;
        }

        // 保存规则（多种方式尝试）
        cacheLog.info("正在保存iptables规则...");
        execCommand(session, "service iptables save 2>/dev/null || true");
        execCommand(session, "iptables-save > /etc/sysconfig/iptables 2>/dev/null || true");
        execCommand(session, "netfilter-persistent save 2>/dev/null || true");

        // 更新状态为正在验证iptables状态
        setCheckItemMessage(hostInfo, checkItem, "正在验证iptables规则...");

        // 验证iptables规则
        cacheLog.info("验证iptables规则...");
        CommandResult verifyResult = execCommand(session, "iptables -L");
        if (verifyResult.isSuccess()) {
            // 检查是否是默认规则
            boolean isDefault = verifyResult.getOutput().contains("Chain INPUT (policy ACCEPT)") &&
                    verifyResult.getOutput().contains("Chain FORWARD (policy ACCEPT)") &&
                    verifyResult.getOutput().contains("Chain OUTPUT (policy ACCEPT)");

            // 检查规则数量
            int ruleCount = 0;
            for (String line : verifyResult.getOutput().split("\n")) {
                // 跳过表头和空行
                if (line.trim().isEmpty() || line.startsWith("Chain") || line.startsWith("target")) {
                    continue;
                }
                ruleCount++;
            }

            if (isDefault && ruleCount == 0) {
                cacheLog.info("验证成功: iptables规则已清空");
                setCheckItemMessage(hostInfo, checkItem, "iptables防火墙已成功禁用和清空规则");
                return true;
            } else {
                cacheLog.warn("警告: iptables规则可能未完全清空，请手动检查");
                setCheckItemMessage(hostInfo, checkItem, "警告: iptables规则可能未完全清空，请手动检查");
                return false;
            }
        } else {
            cacheLog.warn("无法验证iptables规则状态: %s", verifyResult.getErrorOrOutput());
            setCheckItemMessage(hostInfo, checkItem, "无法验证iptables规则状态，请手动检查");
            return false;
        }
    }

    @Override
    public ItemCode getCheckerType() {
        return ItemCode.FIREWALL;
    }
}