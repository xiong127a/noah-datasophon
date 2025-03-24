package com.datasophon.api.service.checker.checkers.firewall;

import com.datasophon.api.service.checker.common.CommandResult;
import com.datasophon.api.service.checker.common.OsInfo;
import com.datasophon.common.model.CheckItem;
import com.datasophon.common.model.HostInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CentOS防火墙检查器
 * 针对CentOS系统的防火墙检查和修复
 * CentOS 7及以上使用firewalld作为默认防火墙
 * CentOS 6及以下使用iptables作为默认防火墙
 */
public class CentOSFirewallChecker extends FirewallChecker {

    private static final Logger logger = LoggerFactory.getLogger(CentOSFirewallChecker.class);

    /**
     * 检测CentOS系统防火墙状态
     */
    public CheckItem doCheckCentOS(HostInfo hostInfo, CheckItem checkItem, OsInfo osInfo) throws InterruptedException {
        // 根据CentOS版本选择检查方式
        if (osInfo.isCentOS7() || osInfo.isCentOS8()) {
            // CentOS 7/8 使用firewalld
            cacheLog.info("CentOS 7/8系统，检查firewalld服务");
            return doCheckFirewalld(hostInfo, checkItem);
        } else {
            // CentOS 6及以下使用iptables
            cacheLog.info("CentOS 6或更早版本，检查iptables服务");
            return doCheckIptables(hostInfo, checkItem);
        }
    }

    /**
     * 修复CentOS系统防火墙
     */
    public boolean doFixCentOS(HostInfo hostInfo, CheckItem checkItem, OsInfo osInfo) throws InterruptedException {
        // 根据CentOS版本选择修复方式
        if (osInfo.isCentOS7() || osInfo.isCentOS8()) {
            // CentOS 7/8 使用firewalld
            cacheLog.info("CentOS 7/8系统，修复firewalld服务");
            return doFixFirewalld(hostInfo, checkItem);
        } else {
            // CentOS 6及以下使用iptables
            cacheLog.info("CentOS 6或更早版本，修复iptables服务");
            return doFixIptables(hostInfo, checkItem);
        }
    }

    /**
     * 检查firewalld服务
     */
    private CheckItem doCheckFirewalld(HostInfo hostInfo, CheckItem checkItem) throws InterruptedException {
        cacheLog.info("执行检查命令: systemctl status firewalld");
        CommandResult result = execCommand(session, "systemctl status firewalld");

        switch (result.getExitCode()) {
            case 0:
                // 服务正在运行
                cacheLog.info("firewalld状态: 正在运行");

                // 检查是否有转发规则（CentOS特定检查）
                CommandResult forwardResult = execCommand(session, "firewall-cmd --direct --get-all-rules");
                if (forwardResult.isSuccess() && !forwardResult.getOutput().trim().isEmpty()) {
                    cacheLog.info("检测到自定义转发规则");
                }

                checkItem.setStatus(CheckItem.Status.FAILED);
                setCheckItemMessage(hostInfo, checkItem, "CentOS系统firewalld防火墙正在运行，建议关闭");
                break;

            case 3:
                // 服务已停止
                cacheLog.info("firewalld状态: 已停止");
                checkItem.setStatus(CheckItem.Status.SUCCESS);
                setCheckItemMessage(hostInfo, checkItem, "CentOS系统firewalld防火墙已关闭");
                break;

            case 4:
                // 服务不存在
                cacheLog.info("firewalld状态: 服务不存在");
                checkItem.setStatus(CheckItem.Status.SUCCESS);
                setCheckItemMessage(hostInfo, checkItem, "CentOS系统firewalld防火墙服务未安装");
                break;

            default:
                // 其他状态，可能是命令执行出错
                cacheLog.warn("获取firewalld防火墙状态失败，退出状态码: %d", result.getExitCode());
                checkItem.setStatus(CheckItem.Status.FAILED);
                setCheckItemMessage(hostInfo, checkItem, "获取CentOS系统firewalld防火墙状态失败: " + result.getErrorOrOutput());
                break;
        }

        return checkItem;
    }

    /**
     * 检查iptables服务（CentOS 6及以下）
     */
    private CheckItem doCheckIptables(HostInfo hostInfo, CheckItem checkItem) throws InterruptedException {
        cacheLog.info("执行检查命令: service iptables status");
        CommandResult result = execCommand(session, "service iptables status");

        if (result.isSuccess()) {
            String output = result.getOutput().toLowerCase();

            if (output.contains("not running") || output.contains("stopped")) {
                // 服务已停止
                cacheLog.info("iptables状态: 已停止");
                checkItem.setStatus(CheckItem.Status.SUCCESS);
                setCheckItemMessage(hostInfo, checkItem, "CentOS系统iptables防火墙已停止");
            } else if (output.contains("running")) {
                // 服务正在运行
                cacheLog.info("iptables状态: 正在运行");

                // 获取iptables规则
                CommandResult rulesResult = execCommand(session, "iptables -L");
                if (rulesResult.isSuccess()) {
                    cacheLog.info("当前iptables规则:");
                    cacheLog.info(rulesResult.getOutput());
                }

                checkItem.setStatus(CheckItem.Status.FAILED);
                setCheckItemMessage(hostInfo, checkItem, "CentOS系统iptables防火墙正在运行，建议关闭");
            } else {
                // 状态不明
                cacheLog.warn("无法确定iptables状态: %s", output);
                checkItem.setStatus(CheckItem.Status.FAILED);
                setCheckItemMessage(hostInfo, checkItem, "无法确定CentOS系统iptables防火墙状态，请手动检查");
            }
        } else {
            // 命令执行失败
            cacheLog.warn("获取iptables状态失败: %s", result.getErrorOrOutput());
            checkItem.setStatus(CheckItem.Status.FAILED);
            setCheckItemMessage(hostInfo, checkItem, "获取CentOS系统iptables防火墙状态失败: " + result.getErrorOrOutput());
        }

        return checkItem;
    }

    /**
     * 修复firewalld服务（CentOS 7/8）
     */
    private boolean doFixFirewalld(HostInfo hostInfo, CheckItem checkItem) throws InterruptedException {
        // 更新状态为正在检查防火墙当前状态
        setCheckItemMessage(hostInfo, checkItem, "正在检查CentOS系统firewalld防火墙当前状态...");

        // 先检查防火墙状态
        cacheLog.info("检查CentOS系统firewalld防火墙当前状态...");
        CommandResult statusResult = execCommand(session, "systemctl status firewalld");

        // 如果服务不存在，直接返回成功
        if (statusResult.getExitCode() == 4) {
            cacheLog.info("CentOS系统firewalld防火墙服务未安装，无需修复");
            setCheckItemMessage(hostInfo, checkItem, "CentOS系统firewalld防火墙服务未安装，无需修复");
            return true;
        }

        // 如果服务已停止，检查是否已禁用自启动
        if (statusResult.getExitCode() == 3) {
            // 更新状态为正在检查自启动状态
            setCheckItemMessage(hostInfo, checkItem, "正在检查CentOS系统firewalld防火墙自启动状态...");

            CommandResult isEnabledResult = execCommand(session, "systemctl is-enabled firewalld");
            if (isEnabledResult.isSuccess() && isEnabledResult.getOutput().trim().equals("disabled")) {
                cacheLog.info("CentOS系统firewalld防火墙已关闭且已禁用自启动，无需修复");
                setCheckItemMessage(hostInfo, checkItem, "CentOS系统firewalld防火墙已关闭且已禁用自启动");
                return true;
            }
        }

        // 更新状态为正在停止防火墙服务
        setCheckItemMessage(hostInfo, checkItem, "正在停止CentOS系统firewalld防火墙服务...");

        // 停止并禁用防火墙
        cacheLog.info("正在停止CentOS系统firewalld防火墙服务...");
        CommandResult stopResult = execCommand(session, "systemctl stop firewalld");
        if (!stopResult.isSuccess()) {
            cacheLog.error("停止CentOS系统firewalld防火墙服务失败: %s", stopResult.getErrorOrOutput());
            setCheckItemMessage(hostInfo, checkItem, "停止CentOS系统firewalld防火墙服务失败: " + stopResult.getErrorOrOutput());
            return false;
        }
        cacheLog.info("停止CentOS系统firewalld防火墙服务完成");

        // 更新状态为正在禁用防火墙自启动
        setCheckItemMessage(hostInfo, checkItem, "正在禁用CentOS系统firewalld防火墙自启动...");

        cacheLog.info("正在禁用CentOS系统firewalld防火墙自启动...");
        CommandResult disableResult = execCommand(session, "systemctl disable firewalld");
        if (!disableResult.isSuccess()) {
            cacheLog.error("禁用CentOS系统firewalld防火墙自启动失败: %s", disableResult.getErrorOrOutput());
            setCheckItemMessage(hostInfo, checkItem,
                    "禁用CentOS系统firewalld防火墙自启动失败: " + disableResult.getErrorOrOutput());
            return false;
        }
        cacheLog.info("禁用CentOS系统firewalld防火墙自启动完成");

        // 更新状态为正在验证防火墙状态
        setCheckItemMessage(hostInfo, checkItem, "正在验证CentOS系统firewalld防火墙状态...");

        // 再次检查确认防火墙已关闭
        cacheLog.info("验证CentOS系统firewalld防火墙状态...");
        CommandResult verifyResult = execCommand(session, "systemctl status firewalld");
        if (verifyResult.getExitCode() != 3) {
            cacheLog.warn("警告: CentOS系统firewalld防火墙服务可能未成功关闭，请手动检查");
            setCheckItemMessage(hostInfo, checkItem, "警告: CentOS系统firewalld防火墙服务可能未成功关闭，请手动检查");
            return false;
        }

        cacheLog.info("验证成功: CentOS系统firewalld防火墙已关闭");
        setCheckItemMessage(hostInfo, checkItem, "CentOS系统firewalld防火墙已成功关闭并禁用自启动");
        return true;
    }

    /**
     * 修复iptables服务（CentOS 6及以下）
     */
    private boolean doFixIptables(HostInfo hostInfo, CheckItem checkItem) throws InterruptedException {
        // 更新状态为正在检查iptables当前状态
        setCheckItemMessage(hostInfo, checkItem, "正在检查CentOS系统iptables防火墙当前状态...");

        // 先检查服务状态
        cacheLog.info("检查CentOS系统iptables防火墙当前状态...");
        CommandResult statusResult = execCommand(session, "service iptables status");

        if (statusResult.isSuccess() &&
                (statusResult.getOutput().toLowerCase().contains("not running") ||
                        statusResult.getOutput().toLowerCase().contains("stopped"))) {
            // 检查自启动状态
            CommandResult chkconfigResult = execCommand(session,
                    "chkconfig --list iptables | grep on || echo 'Not enabled'");
            if (chkconfigResult.isSuccess() && chkconfigResult.getOutput().contains("Not enabled")) {
                cacheLog.info("CentOS系统iptables防火墙已关闭且已禁用自启动，无需修复");
                setCheckItemMessage(hostInfo, checkItem, "CentOS系统iptables防火墙已关闭且已禁用自启动");
                return true;
            }
        }

        // 更新状态为正在停止iptables服务
        setCheckItemMessage(hostInfo, checkItem, "正在停止CentOS系统iptables防火墙服务...");

        // 停止服务
        cacheLog.info("正在停止CentOS系统iptables防火墙服务...");
        CommandResult stopResult = execCommand(session, "service iptables stop");
        if (!stopResult.isSuccess()) {
            cacheLog.error("停止CentOS系统iptables防火墙服务失败: %s", stopResult.getErrorOrOutput());
            setCheckItemMessage(hostInfo, checkItem, "停止CentOS系统iptables防火墙服务失败: " + stopResult.getErrorOrOutput());
            return false;
        }

        // 禁用自启动
        cacheLog.info("正在禁用CentOS系统iptables防火墙自启动...");
        CommandResult disableResult = execCommand(session, "chkconfig iptables off");
        if (!disableResult.isSuccess()) {
            cacheLog.error("禁用CentOS系统iptables防火墙自启动失败: %s", disableResult.getErrorOrOutput());
            setCheckItemMessage(hostInfo, checkItem, "禁用CentOS系统iptables防火墙自启动失败: " + disableResult.getErrorOrOutput());
            return false;
        }

        // 清空iptables规则
        cacheLog.info("正在清空CentOS系统iptables规则...");
        CommandResult flushResult = execCommand(session,
                "iptables -F && iptables -X && iptables -P INPUT ACCEPT && iptables -P FORWARD ACCEPT && iptables -P OUTPUT ACCEPT");
        if (!flushResult.isSuccess()) {
            cacheLog.error("清空CentOS系统iptables规则失败: %s", flushResult.getErrorOrOutput());
            setCheckItemMessage(hostInfo, checkItem, "清空CentOS系统iptables规则失败: " + flushResult.getErrorOrOutput());
            return false;
        }

        // 保存规则
        cacheLog.info("正在保存CentOS系统iptables规则...");
        CommandResult saveResult = execCommand(session, "service iptables save");
        if (!saveResult.isSuccess()) {
            cacheLog.warn("保存CentOS系统iptables规则警告: %s", saveResult.getErrorOrOutput());
        }

        // 更新状态为正在验证iptables状态
        setCheckItemMessage(hostInfo, checkItem, "正在验证CentOS系统iptables防火墙状态...");

        // 验证服务状态
        cacheLog.info("验证CentOS系统iptables防火墙状态...");
        CommandResult verifyResult = execCommand(session, "service iptables status");
        if (verifyResult.isSuccess() &&
                (verifyResult.getOutput().toLowerCase().contains("not running") ||
                        verifyResult.getOutput().toLowerCase().contains("stopped"))) {

            // 验证规则是否清空
            CommandResult rulesResult = execCommand(session, "iptables -L");
            if (rulesResult.isSuccess()) {
                boolean isEmpty = rulesResult.getOutput().contains("Chain INPUT (policy ACCEPT)") &&
                        rulesResult.getOutput().contains("Chain FORWARD (policy ACCEPT)") &&
                        rulesResult.getOutput().contains("Chain OUTPUT (policy ACCEPT)");

                if (isEmpty) {
                    cacheLog.info("验证成功: CentOS系统iptables防火墙已关闭且规则已清空");
                    setCheckItemMessage(hostInfo, checkItem, "CentOS系统iptables防火墙已成功关闭并禁用自启动");
                    return true;
                } else {
                    cacheLog.warn("警告: CentOS系统iptables规则可能未完全清空，请手动检查");
                    setCheckItemMessage(hostInfo, checkItem, "警告: CentOS系统iptables规则可能未完全清空，请手动检查");
                    return false;
                }
            } else {
                // 无法验证规则
                cacheLog.info("验证成功: CentOS系统iptables防火墙已关闭");
                setCheckItemMessage(hostInfo, checkItem, "CentOS系统iptables防火墙已成功关闭并禁用自启动");
                return true;
            }
        } else {
            cacheLog.warn("警告: CentOS系统iptables防火墙服务可能未成功关闭，请手动检查");
            setCheckItemMessage(hostInfo, checkItem, "警告: CentOS系统iptables防火墙服务可能未成功关闭，请手动检查");
            return false;
        }
    }
}