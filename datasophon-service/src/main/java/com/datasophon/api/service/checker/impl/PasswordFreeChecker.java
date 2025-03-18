package com.datasophon.api.service.checker.impl;

import com.datasophon.api.service.checker.AbstractItemChecker;
import com.datasophon.api.utils.MinaUtils;
import com.datasophon.common.model.CheckItem;
import com.datasophon.common.model.HostInfo;
import com.datasophon.common.model.ItemCode;
import org.apache.sshd.client.session.ClientSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

@Component
public class PasswordFreeChecker extends AbstractItemChecker {

    private static final Logger logger = LoggerFactory.getLogger(PasswordFreeChecker.class);

    @Override
    protected CheckItem doCheck(HostInfo hostInfo, CheckItem checkItem) throws InterruptedException {
        cacheLog.info("开始执行免密检查...");
        
        // 检查session是否为null
        if (session == null) {
            cacheLog.error("SSH会话为空，无法执行免密检查");
            checkItem.setStatus(CheckItem.Status.FAILED);
            checkItem.setMessage("SSH连接失败，请检查主机连接信息");
            return checkItem;
        }

        try {
            // 尝试执行一个简单的命令来验证SSH连接
            String result = execCommand(session, "echo 'SSH connection test'");
            
            if (result.startsWith("ERROR:")) {
                cacheLog.error("SSH命令执行失败: {}", result);
                checkItem.setStatus(CheckItem.Status.FAILED);
                checkItem.setMessage("SSH连接测试失败: " + result);
                return checkItem;
            }

            // 检查是否能成功执行命令
            if (!result.contains("SSH connection test")) {
                cacheLog.error("SSH命令执行结果异常: {}", result);
                checkItem.setStatus(CheckItem.Status.FAILED);
                checkItem.setMessage("SSH连接测试返回异常结果");
                return checkItem;
            }

            // 执行更多的免密检查
            // 检查用户权限
            result = execCommand(session, "id");
            if (result.startsWith("ERROR:")) {
                cacheLog.error("用户权限检查失败: {}", result);
                checkItem.setStatus(CheckItem.Status.FAILED);
                checkItem.setMessage("用户权限检查失败: " + result);
                return checkItem;
            }

            // 检查sudo权限
            result = execCommand(session, "sudo -n true 2>&1");
            if (!result.startsWith("ERROR:") && !result.contains("password")) {
                cacheLog.info("用户具有sudo权限");
            } else {
                cacheLog.warn("用户可能没有sudo权限，但不影响基本操作");
            }

            // 所有检查都通过
            cacheLog.info("免密检查通过");
            checkItem.setStatus(CheckItem.Status.SUCCESS);
            checkItem.setMessage("检查通过");
            
        } catch (InterruptedException e) {
            cacheLog.warn("免密检查被中断");
            throw e;
        } catch (Exception e) {
            cacheLog.error("执行免密检查时发生异常: {}", e.getMessage());
            checkItem.setStatus(CheckItem.Status.FAILED);
            checkItem.setMessage("免密检查异常: " + e.getMessage());
        }

        // 最后确认状态
        if (checkItem.getStatus() == null || checkItem.getStatus() == CheckItem.Status.CHECKING) {
            cacheLog.error("检查完成但状态未正确设置，强制设置为失败");
            checkItem.setStatus(CheckItem.Status.FAILED);
            checkItem.setMessage("检查状态异常");
        }

        return checkItem;
    }

    @Override
    protected boolean doFix(HostInfo hostInfo, CheckItem checkItem) {
        // 免密检查失败通常需要手动修复
        cacheLog.info("免密检查需要手动修复，请确保：");
        cacheLog.info("1. SSH用户名和密码正确");
        cacheLog.info("2. SSH端口号正确");
        cacheLog.info("3. 目标主机SSH服务正常运行");
        cacheLog.info("4. 网络连接正常");
        return false;
    }

    @Override
    protected ItemCode getCheckerType() {
        return ItemCode.PASSWORD_FREE;
    }

} 