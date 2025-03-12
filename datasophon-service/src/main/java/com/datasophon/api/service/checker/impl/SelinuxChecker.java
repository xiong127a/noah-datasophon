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
public class SelinuxChecker extends AbstractItemChecker {
    
    private static final Logger logger = LoggerFactory.getLogger(SelinuxChecker.class);
    
    @Override
    protected CheckItem doCheck(HostInfo hostInfo, CheckItem checkItem) {
        ClientSession session = hostInfo.getSession();
        try {
            // 检查SELinux状态
            String selinuxStatus = execCommand(session, "getenforce");
            
            if (selinuxStatus.contains("Disabled") || selinuxStatus.contains("Permissive")) {
                checkItem.setStatus(CheckItem.Status.SUCCESS);
                checkItem.setMessage("SELinux已禁用或处于宽容模式");
            } else if (selinuxStatus.contains("Enforcing")) {
                checkItem.setStatus(CheckItem.Status.FAILED);
                checkItem.setMessage("SELinux处于强制模式，建议禁用");
            } else {
                // 可能是命令不存在，尝试检查配置文件
                String configCheck = execCommand(session, "grep -i 'SELINUX=' /etc/selinux/config | grep -i 'disabled'");
                if (!configCheck.isEmpty()) {
                    checkItem.setStatus(CheckItem.Status.SUCCESS);
                    checkItem.setMessage("SELinux配置为禁用状态");
                } else {
                    checkItem.setStatus(CheckItem.Status.FAILED);
                    checkItem.setMessage("SELinux未禁用");
                }
            }
        } catch (Exception e) {
            logger.error("SELinux检查失败: {}", e.getMessage());
            checkItem.setStatus(CheckItem.Status.FAILED);
            checkItem.setMessage("SELinux检查失败: " + e.getMessage());
        }
        return checkItem;
    }
    
    @Override
    protected boolean doFix(HostInfo hostInfo, CheckItem checkItem) {
        ClientSession session = hostInfo.getSession();
        try {
            // 临时设置SELinux为宽容模式
            execCommand(session, "setenforce 0");
            
            // 永久禁用SELinux
            execCommand(session, "sed -i 's/^SELINUX=.*/SELINUX=disabled/g' /etc/selinux/config");
            
            return true;
        } catch (Exception e) {
            logger.error("SELinux配置修复失败: {}", e.getMessage());
            return false;
        }
    }
    
    @Override
    protected ItemCode getCheckerType() {
        return ItemCode.SELINUX;
    }
    
    private String execCommand(ClientSession session, String command) {
        try {
            // TODO: 实现命令执行逻辑
            return "Disabled"; // 临时返回模拟值
        } catch (Exception e) {
            logger.error("执行命令 {} 失败: {}", command, e.getMessage());
            return "ERROR: " + e.getMessage();
        }
    }
} 