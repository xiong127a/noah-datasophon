package com.datasophon.api.service.checker.impl;

import com.datasophon.api.service.checker.AbstractItemChecker;
import com.datasophon.api.service.checker.CommandResult;
import com.datasophon.common.model.CheckItem;
import com.datasophon.common.model.HostInfo;
import com.datasophon.common.model.ItemCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class FirewallChecker extends AbstractItemChecker {
    
    private static final Logger logger = LoggerFactory.getLogger(FirewallChecker.class);

    
    @Override
    protected CheckItem doCheck(HostInfo hostInfo, CheckItem checkItem) throws InterruptedException {
        try {
            cacheLog.info("==== 防火墙检查开始 ====");
            cacheLog.info("检查系统防火墙状态...");
            
            // 更新状态为正在检查防火墙状态
            setCheckItemMessage(hostInfo, checkItem, "正在检查防火墙状态...");
            
            // 检查防火墙状态
            cacheLog.info("执行检查命令: systemctl status firewalld");
            CommandResult result = execCommand(session, "systemctl status firewalld");
            
            // 根据退出状态码判断防火墙状态
            switch (result.getExitCode()) {
                case 0:
                    // 服务正在运行
                    cacheLog.info("防火墙状态: 正在运行");
                    checkItem.setStatus(CheckItem.Status.FAILED);
                    setCheckItemMessage(hostInfo, checkItem, "防火墙正在运行，建议关闭");
                    break;
                    
                case 3:
                    // 服务已停止
                    cacheLog.info("防火墙状态: 已停止");
                    checkItem.setStatus(CheckItem.Status.SUCCESS);
                    setCheckItemMessage(hostInfo, checkItem, "防火墙已关闭");
                    break;
                    
                case 4:
                    // 服务不存在
                    cacheLog.info("防火墙状态: 服务不存在");
                    checkItem.setStatus(CheckItem.Status.SUCCESS);
                    setCheckItemMessage(hostInfo, checkItem, "防火墙服务未安装");
                    break;
                    
                default:
                    // 其他状态，可能是命令执行出错
                    cacheLog.warn("获取防火墙状态失败，退出状态码: %d", result.getExitCode());
                    checkItem.setStatus(CheckItem.Status.FAILED);
                    setCheckItemMessage(hostInfo, checkItem, "获取防火墙状态失败: " + result.getErrorOrOutput());
                    break;
            }
            
            // 如果防火墙正在运行，获取更多配置信息
            if (result.getExitCode() == 0) {
                // 更新状态为正在获取防火墙配置
                setCheckItemMessage(hostInfo, checkItem, "正在获取防火墙配置信息...");
                
                try {
                    CommandResult firewallConfig = execCommand(session, "firewall-cmd --list-all");
                    if (firewallConfig.isSuccess()) {
                        cacheLog.info("当前防火墙配置信息:");
                        cacheLog.info(firewallConfig.getOutput());
                    }
                } catch (Exception e) {
                    cacheLog.warn("获取防火墙配置信息失败: %s", e.getMessage());
                }
            }
            
        } catch (Exception e) {
            String errorMsg = "防火墙检查失败: " + e.getMessage();
            logger.error(errorMsg, e);
            cacheLog.error("错误: %s", errorMsg);
            checkItem.setStatus(CheckItem.Status.FAILED);
            setCheckItemMessage(hostInfo, checkItem, errorMsg);
        } finally {
            cacheLog.info("==== 防火墙检查完成 ====");
        }
        return checkItem;
    }
    
    @Override
    protected boolean doFix(HostInfo hostInfo, CheckItem checkItem) {
        try {
            cacheLog.info("==== 开始修复防火墙配置 ====");
            
            // 更新状态为正在检查防火墙状态
            setCheckItemMessage(hostInfo, checkItem, "正在检查防火墙当前状态...");
            
            // 先检查防火墙状态
            cacheLog.info("检查防火墙当前状态...");
            CommandResult statusResult = execCommand(session, "systemctl status firewalld");
            
            // 如果服务不存在，直接返回成功
            if (statusResult.getExitCode() == 4) {
                cacheLog.info("防火墙服务未安装，无需修复");
                setCheckItemMessage(hostInfo, checkItem, "防火墙服务未安装，无需修复");
                return true;
            }
            
            // 如果服务已停止，检查是否已禁用自启动
            if (statusResult.getExitCode() == 3) {
                // 更新状态为正在检查自启动状态
                setCheckItemMessage(hostInfo, checkItem, "正在检查防火墙自启动状态...");
                
                CommandResult isEnabledResult = execCommand(session, "systemctl is-enabled firewalld");
                if (isEnabledResult.isSuccess() && isEnabledResult.getOutput().trim().equals("disabled")) {
                    cacheLog.info("防火墙已关闭且已禁用自启动，无需修复");
                    setCheckItemMessage(hostInfo, checkItem, "防火墙已关闭且已禁用自启动");
                    return true;
                }
            }
            
            // 更新状态为正在停止防火墙服务
            setCheckItemMessage(hostInfo, checkItem, "正在停止防火墙服务...");
            
            // 停止并禁用防火墙
            cacheLog.info("正在停止防火墙服务...");
            CommandResult stopResult = execCommand(session, "systemctl stop firewalld");
            if (!stopResult.isSuccess()) {
                cacheLog.error("停止防火墙服务失败: %s", stopResult.getErrorOrOutput());
                setCheckItemMessage(hostInfo, checkItem, "停止防火墙服务失败: " + stopResult.getErrorOrOutput());
                return false;
            }
            cacheLog.info("停止防火墙服务完成");
            
            // 更新状态为正在禁用防火墙自启动
            setCheckItemMessage(hostInfo, checkItem, "正在禁用防火墙自启动...");
            
            cacheLog.info("正在禁用防火墙自启动...");
            CommandResult disableResult = execCommand(session, "systemctl disable firewalld");
            if (!disableResult.isSuccess()) {
                cacheLog.error("禁用防火墙自启动失败: %s", disableResult.getErrorOrOutput());
                setCheckItemMessage(hostInfo, checkItem, "禁用防火墙自启动失败: " + disableResult.getErrorOrOutput());
                return false;
            }
            cacheLog.info("禁用防火墙自启动完成");
            
            // 更新状态为正在验证防火墙状态
            setCheckItemMessage(hostInfo, checkItem, "正在验证防火墙状态...");
            
            // 再次检查确认防火墙已关闭
            cacheLog.info("验证防火墙状态...");
            CommandResult verifyResult = execCommand(session, "systemctl status firewalld");
            if (verifyResult.getExitCode() != 3) {
                cacheLog.warn("警告: 防火墙服务可能未成功关闭，请手动检查");
                setCheckItemMessage(hostInfo, checkItem, "警告: 防火墙服务可能未成功关闭，请手动检查");
                return false;
            }
            
            cacheLog.info("验证成功: 防火墙已关闭");
            setCheckItemMessage(hostInfo, checkItem, "防火墙已成功关闭并禁用自启动");
            cacheLog.info("==== 防火墙配置修复完成 ====");
            return true;
            
        } catch (Exception e) {
            String errorMsg = "防火墙配置修复失败: " + e.getMessage();
            logger.error(errorMsg);
            cacheLog.error("错误: " + errorMsg);
            setCheckItemMessage(hostInfo, checkItem, "防火墙配置修复失败: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public ItemCode getCheckerType() {
        return ItemCode.FIREWALL;
    }
} 