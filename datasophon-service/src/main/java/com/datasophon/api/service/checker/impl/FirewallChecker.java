package com.datasophon.api.service.checker.impl;

import com.datasophon.api.service.checker.AbstractItemChecker;
import com.datasophon.common.model.CheckItem;
import com.datasophon.common.model.HostInfo;
import com.datasophon.common.model.ItemCode;
import org.apache.sshd.client.session.ClientSession;
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
            
            // 检查防火墙状态
            cacheLog.info("执行检查命令: systemctl status firewalld");
            String result = execCommand(session, "systemctl status firewalld");
            
            // 检查命令执行结果中是否包含 "Active: active"
            boolean isFirewalldRunning = result.contains("Active: active");
            cacheLog.info("防火墙运行状态分析: " + (isFirewalldRunning ? "正在运行" : "未运行"));
            
            if (!isFirewalldRunning) {
                cacheLog.info("防火墙检查结果: 已关闭，符合要求");
                checkItem.setStatus(CheckItem.Status.SUCCESS);
                checkItem.setMessage("防火墙已关闭");
                return checkItem;
            }
            
            // 如果防火墙正在运行，尝试获取更多信息
            cacheLog.info("防火墙状态: 正在运行，不符合要求");
            cacheLog.info("获取防火墙详细配置信息...");
            
            try {
                String firewallConfig = execCommand(session, "firewall-cmd --list-all");
                cacheLog.info("防火墙配置信息获取成功");
            } catch (Exception e) {
                cacheLog.error("获取防火墙配置信息失败: " + e.getMessage());
            }
            
            cacheLog.info("防火墙检查结果: 防火墙正在运行，建议关闭");
            checkItem.setStatus(CheckItem.Status.FAILED);
            checkItem.setMessage("防火墙正在运行，建议关闭");
        } catch (Exception e) {
            String errorMsg = "防火墙检查失败: " + e.getMessage();
            logger.error(errorMsg);
            cacheLog.error("错误: " + errorMsg);
            checkItem.setStatus(CheckItem.Status.FAILED);
            checkItem.setMessage("防火墙检查失败: " + e.getMessage());
        } finally {
            cacheLog.info("==== 防火墙检查完成 ====");
        }
        return checkItem;
    }
    
    @Override
    protected boolean doFix(HostInfo hostInfo, CheckItem checkItem) {
        try {
            cacheLog.info("==== 开始修复防火墙配置 ====");
            
            // 先检查防火墙状态
            cacheLog.info("检查防火墙当前状态...");
            String statusResult = execCommand(session, "systemctl status firewalld");
            boolean isFirewalldRunning = statusResult.contains("Active: active");
            cacheLog.info("防火墙当前状态: " + (isFirewalldRunning ? "运行中" : "已关闭"));
            
            if (isFirewalldRunning) {
                // 停止并禁用防火墙
                cacheLog.info("正在停止防火墙服务...");
                String stopResult = execCommand(session, "systemctl stop firewalld");
                cacheLog.info("停止防火墙服务完成");
                
                cacheLog.info("正在禁用防火墙自启动...");
                String disableResult = execCommand(session, "systemctl disable firewalld");
                cacheLog.info("禁用防火墙自启动完成");
                
                // 再次检查确认防火墙已关闭
                cacheLog.info("验证防火墙状态...");
                String verifyResult = execCommand(session, "systemctl status firewalld");
                boolean stillRunning = verifyResult.contains("Active: active");
                
                if (stillRunning) {
                    cacheLog.warn("警告: 防火墙服务可能未成功关闭，请手动检查");
                } else {
                    cacheLog.info("验证成功: 防火墙已关闭");
                }
            } else {
                cacheLog.info("防火墙已经处于关闭状态，无需修复");
            }
            
            cacheLog.info("==== 防火墙配置修复完成 ====");
            return true;
        } catch (Exception e) {
            String errorMsg = "防火墙配置修复失败: " + e.getMessage();
            logger.error(errorMsg);
            cacheLog.error("错误: " + errorMsg);
            return false;
        }
    }
    
    @Override
    protected ItemCode getCheckerType() {
        return ItemCode.FIREWALL;
    }
} 