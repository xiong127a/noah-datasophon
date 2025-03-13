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
    protected CheckItem doCheck( HostInfo hostInfo, CheckItem checkItem) {
        try {
            // 检查防火墙状态
            String result = execCommand(session, "systemctl status firewalld");
            boolean isFirewalldRunning = result.contains("Active: active");
            
            if (!isFirewalldRunning) {
                checkItem.setStatus(CheckItem.Status.SUCCESS);
                checkItem.setMessage("防火墙已关闭");
                return checkItem;
            }
            
            checkItem.setStatus(CheckItem.Status.FAILED);
            checkItem.setMessage("防火墙正在运行，建议关闭");
        } catch (Exception e) {
            logger.error("防火墙检查失败: {}", e.getMessage());
            checkItem.setStatus(CheckItem.Status.FAILED);
            checkItem.setMessage("防火墙检查失败: " + e.getMessage());
        }
        return checkItem;
    }
    
    @Override
    protected boolean doFix(HostInfo hostInfo, CheckItem checkItem) {
        try {
            // 停止并禁用防火墙
            execCommand(session, "systemctl stop firewalld");
            execCommand(session, "systemctl disable firewalld");
            return true;
        } catch (Exception e) {
            logger.error("防火墙配置修复失败: {}", e.getMessage());
            return false;
        }
    }
    
    @Override
    protected ItemCode getCheckerType() {
        return ItemCode.FIREWALL;
    }
} 