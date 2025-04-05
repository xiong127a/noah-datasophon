package com.datasophon.api.service.checker.checkers.firewall;

import com.datasophon.api.service.checker.checkers.firewall.factory.FirewallCheckerFactory;
import com.datasophon.api.service.checker.common.CommandResult;
import com.datasophon.api.service.checker.common.ItemCode;
import com.datasophon.api.service.checker.core.AbstractItemChecker;
import com.datasophon.api.service.checker.helpers.CheckLogger;
import com.datasophon.common.enums.OsDistribution;
import com.datasophon.common.model.CheckItem;
import com.datasophon.common.model.HostInfo;
import com.datasophon.common.model.OsInfo;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 防火墙检查器
 * 负责检查和修复主机防火墙配置
 */
@Component
public class FirewallChecker extends AbstractItemChecker {

    private static final Logger log = LoggerFactory.getLogger(FirewallChecker.class);

    @Override
    protected CheckItem doCheck(HostInfo hostInfo, CheckItem checkItem) {
        CheckLogger cacheLog = this.cacheLog;

        try {
            log.info("开始检查主机 {} 的防火墙状态", hostInfo.getIp());
            cacheLog.info("开始检查防火墙状态...");

            // 更新检查项状态
            checkItem.setMessage("正在检查防火墙状态...");

            // 检查会话是否准备就绪
            if (session == null) {
                // 检查hostInfo中是否有可用的会话
                if (!hostInfo.isSessionReady()) {
                    String errorMsg = "SSH会话未就绪，无法执行防火墙检查: " + hostInfo.getIp();
                    log.error(errorMsg);
                    cacheLog.error(errorMsg);
                    checkItem.setStatus(CheckItem.Status.FAILED);
                    checkItem.setMessage(errorMsg);
                    return checkItem;
                }
                // 使用hostInfo的会话
                session = hostInfo.getExternalSession();
            }

            // 获取操作系统信息
            OsInfo osInfo;
            try {
                osInfo = getOsInfo(hostInfo);
                if (osInfo == null || !osInfo.isValid()) {
                    String errorMsg = "无法获取操作系统信息，防火墙检查失败";
                    log.error(errorMsg);
                    cacheLog.error(errorMsg);
                    checkItem.setStatus(CheckItem.Status.FAILED);
                    checkItem.setMessage(errorMsg);
                    return checkItem;
                }
                hostInfo.setExternalSession(session);
            } catch (InterruptedException e) {
                String errorMsg = "获取操作系统信息过程被中断";
                log.error(errorMsg, e);
                cacheLog.error(errorMsg);
                checkItem.setStatus(CheckItem.Status.FAILED);
                checkItem.setMessage(errorMsg);
                return checkItem;
            }

            log.info("主机 {} 操作系统: {}", hostInfo.getIp(), osInfo.getFullName());
            cacheLog.info("操作系统信息: {}", osInfo.getFullName());

            // 通过工厂获取对应的防火墙检查器策略
            FirewallCheckerStrategy strategy = FirewallCheckerFactory.getChecker(osInfo);

            // 执行检查
            CheckItem result = strategy.check(hostInfo, checkItem, cacheLog);

            // 确保状态已更新，但不能盲目强制设置
            if (result.getStatus() == CheckItem.Status.CHECKING) {
                log.warn("防火墙检查完成但状态仍未更新，进行额外检查");
                cacheLog.warn("防火墙检查完成但状态未更新，进行额外检查");

                // 尝试进行额外的防火墙检查
                try {
                    // 根据不同操作系统执行不同的检查命令
                    String checkCommand = "";

                    switch (osInfo.getOsDistribution()) {
                        case CENTOS:
                        case REDHAT:
                        case KYLIN:
                            // 检查firewalld状态
                            checkCommand = "systemctl is-active firewalld || echo 'inactive'";
                            break;
                        case UBUNTU:
                        case DEBIAN:
                            // 检查ufw状态
                            checkCommand = "ufw status | grep Status || echo 'inactive'";
                            break;
                        default:
                            // 通用检查，尝试iptables
                            checkCommand = "iptables -L | grep -E 'Chain|REJECT|DROP' || echo 'No rules'";
                            break;
                    }

                    // 执行额外检查命令
                    if (!checkCommand.isEmpty()) {
                        CommandResult statusResult = execCommand(session, checkCommand);
                        String output = statusResult.getOutput().trim();

                        if (statusResult.isSuccess()) {
                            // 分析输出确定防火墙状态
                            boolean firewallRunning = false;

                            switch (osInfo.getOsDistribution()) {
                                case CENTOS:
                                case REDHAT:
                                case KYLIN:
                                    firewallRunning = output.contains("active");
                                    break;
                                case UBUNTU:
                                case DEBIAN:
                                    firewallRunning = output.contains("active");
                                    break;
                                default:
                                    firewallRunning = output.contains("REJECT") || output.contains("DROP");
                                    break;
                            }

                            if (firewallRunning) {
                                log.warn("额外检查发现防火墙服务处于活动状态");
                                cacheLog.warn("额外检查发现防火墙服务处于活动状态");
                                result.setStatus(CheckItem.Status.FAILED);
                                result.setMessage("防火墙正在运行，建议关闭");
                            } else {
                                log.info("额外检查发现防火墙服务未运行");
                                cacheLog.info("额外检查发现防火墙服务未运行");
                                result.setStatus(CheckItem.Status.SUCCESS);
                                result.setMessage("防火墙未运行或无限制规则");
                            }
                        } else {
                            // 命令执行失败
                            log.warn("额外防火墙检查命令执行失败");
                            cacheLog.warn("额外防火墙检查命令执行失败，无法确定防火墙状态");
                            result.setStatus(CheckItem.Status.FAILED);
                            result.setMessage("无法确定防火墙状态，检查失败: " + statusResult.getError());
                        }
                    } else {
                        // 无法确定检查命令
                        log.warn("无法为当前操作系统确定适合的防火墙检查命令");
                        cacheLog.warn("无法确定适合的防火墙检查命令");
                        result.setStatus(CheckItem.Status.FAILED);
                        result.setMessage("无法为当前操作系统确定适合的防火墙检查命令");
                    }
                } catch (Exception e) {
                    log.error("执行额外防火墙检查时出错: ", e);
                    cacheLog.error("执行额外防火墙检查时出错: {}", e.getMessage());
                    result.setStatus(CheckItem.Status.FAILED);
                    result.setMessage("防火墙检查执行过程中发生异常: " + e.getMessage());
                }
            }

            return result;

        } catch (Exception e) {
            String errorMsg = "检查防火墙时发生异常: " + e.getMessage();
            log.error(errorMsg, e);
            cacheLog.error(errorMsg);
            cacheLog.error(e.getMessage());

            checkItem.setStatus(CheckItem.Status.FAILED);
            checkItem.setMessage("检查防火墙失败: " + (StringUtils.isBlank(e.getMessage()) ? "未知错误" : e.getMessage()));
            return checkItem;
        } finally {
            log.info("完成主机 {} 的防火墙检查", hostInfo.getIp());
            cacheLog.info("防火墙检查完成");
        }
    }

    @Override
    protected boolean doFix(HostInfo hostInfo, CheckItem checkItem) {
        CheckLogger cacheLog = this.cacheLog;

        try {
            log.info("开始修复主机 {} 的防火墙配置", hostInfo.getIp());
            cacheLog.info("开始修复防火墙配置...");

            // 更新修复项状态
            checkItem.setMessage("正在修复防火墙配置...");

            // 检查会话是否准备就绪
            if (session == null) {
                // 检查hostInfo中是否有可用的会话
                if (!hostInfo.isSessionReady()) {
                    String errorMsg = "SSH会话未就绪，无法执行防火墙修复: " + hostInfo.getIp();
                    log.error(errorMsg);
                    cacheLog.error(errorMsg);
                    checkItem.setStatus(CheckItem.Status.FAILED);
                    checkItem.setMessage(errorMsg);
                    return false;
                }
                // 使用hostInfo的会话
                session = hostInfo.getExternalSession();
            }

            // 获取操作系统信息
            OsInfo osInfo;
            try {
                osInfo = getOsInfo(hostInfo);
                if (osInfo == null || !osInfo.isValid()) {
                    String errorMsg = "无法获取操作系统信息，防火墙修复失败";
                    log.error(errorMsg);
                    cacheLog.error(errorMsg);
                    checkItem.setMessage(errorMsg);
                    return false;
                }
            } catch (InterruptedException e) {
                String errorMsg = "获取操作系统信息过程被中断";
                log.error(errorMsg, e);
                cacheLog.error(errorMsg);
                checkItem.setMessage(errorMsg);
                return false;
            }

            log.info("主机 {} 操作系统: {}", hostInfo.getIp(), osInfo.getFullName());
            cacheLog.info("操作系统信息: {}", osInfo.getFullName());

            // 通过工厂获取对应的防火墙检查器策略
            FirewallCheckerStrategy strategy = FirewallCheckerFactory.getChecker(osInfo);

            // 执行修复
            boolean result = false;
            try {
                result = strategy.fix(hostInfo, checkItem, cacheLog);
            } catch (InterruptedException e) {
                String errorMsg = "修复防火墙被中断: " + e.getMessage();
                log.error(errorMsg, e);
                cacheLog.error(errorMsg);
                checkItem.setMessage(errorMsg);
                return false;
            }

            // 记录修复结果
            if (result) {
                log.info("主机 {} 防火墙修复成功", hostInfo.getIp());
                cacheLog.info("防火墙修复成功");
            } else {
                log.warn("主机 {} 防火墙修复失败", hostInfo.getIp());
                cacheLog.warn("防火墙修复失败");
            }

            return result;

        } catch (Exception e) {
            String errorMsg = "修复防火墙时发生异常: " + e.getMessage();
            log.error(errorMsg, e);
            cacheLog.error(errorMsg);
            cacheLog.error(e.getMessage());

            checkItem.setMessage("修复防火墙失败: " + (StringUtils.isBlank(e.getMessage()) ? "未知错误" : e.getMessage()));
            return false;
        } finally {
            log.info("完成主机 {} 的防火墙修复", hostInfo.getIp());
            cacheLog.info("防火墙修复完成");
        }
    }

    @Override
    public ItemCode getCheckerType() {
        return ItemCode.FIREWALL;
    }
}