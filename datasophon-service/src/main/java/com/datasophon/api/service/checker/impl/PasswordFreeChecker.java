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
    protected CheckItem doCheck(HostInfo hostInfo, CheckItem checkItem) {
        try {
            // 检查是否可以免密登录
            String result = execCommand(session, "echo 'SSH连接成功'");

            if (result.contains("SSH连接成功")) {
                checkItem.setStatus(CheckItem.Status.SUCCESS);
                checkItem.setMessage("SSH免密登录配置正确");
            } else {
                checkItem.setStatus(CheckItem.Status.FAILED);
                checkItem.setMessage("SSH免密登录配置失败");
            }
        } catch (Exception e) {
            logger.error("SSH免密登录检查失败: {}", e.getMessage());
            checkItem.setStatus(CheckItem.Status.FAILED);
            checkItem.setMessage("SSH免密登录检查失败: " + e.getMessage());
        }
        return checkItem;
    }

    @Override
    protected boolean doFix(HostInfo hostInfo, CheckItem checkItem) {
        try (ClientSession session = MinaUtils.openConnectionWithPassword(hostInfo.getHostname(), hostInfo.getSshPort(), hostInfo.getSshUser(), hostInfo.getSshPassword())) {
            // 生成SSH密钥对（如果不存在）
            String homeDir = System.getProperty("user.home");
            File sshDir = new File(homeDir, ".ssh");
            File idRsaFile = new File(sshDir, "id_rsa");

            if (!sshDir.exists()) {
                sshDir.mkdirs();
            }

            if (!idRsaFile.exists()) {
                execCommand(session, "ssh-keygen -t rsa -N '' -f ~/.ssh/id_rsa");
            }

            // 将公钥复制到目标主机
            String publicKey = new String(Files.readAllBytes(Paths.get(homeDir, ".ssh", "id_rsa.pub")));

            // 确保目标主机上的.ssh目录存在
            execCommand(session, "mkdir -p ~/.ssh");

            // 将公钥添加到authorized_keys
            String cmd = String.format("grep -q '%s' ~/.ssh/authorized_keys || echo '%s' >> ~/.ssh/authorized_keys",
                    publicKey.trim(), publicKey.trim());
            execCommand(session, cmd);

            // 设置正确的权限
            execCommand(session, "chmod 700 ~/.ssh && chmod 600 ~/.ssh/authorized_keys");

            return true;
        } catch (Exception e) {
            logger.error("SSH免密登录配置修复失败: {}", e.getMessage());
            return false;
        }
    }

    @Override
    protected ItemCode getCheckerType() {
        return ItemCode.PASSWORD_FREE;
    }

} 