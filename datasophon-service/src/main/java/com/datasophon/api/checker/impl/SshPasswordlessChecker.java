package com.datasophon.api.checker.impl;

import com.datasophon.api.checker.CheckResult;
import com.datasophon.api.checker.EnvironmentCheckItem;
import com.datasophon.api.checker.HostCheckContext;
import com.datasophon.api.checker.util.CheckLogWriter;
import com.datasophon.api.config.CheckerProperties;
import com.datasophon.common.vo.environment.RepairResult;
import com.datasophon.plugins.api.factory.SshConnectionServiceFactory;
import com.datasophon.plugins.api.service.SshConnectionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * SSH免密登录检查器
 * 检查并修复SSH免密登录配置
 * 
 * @author 任相鹏
 * @date 2025-01-27
 */
@Slf4j
@Component
public class SshPasswordlessChecker implements EnvironmentCheckItem {
    
    @Autowired
    private CheckerProperties checkerProperties;
    
    @Autowired
    private CheckLogWriter checkLogWriter;
    
    private SshConnectionService sshService;
    
    /**
     * 获取SSH服务（延迟加载）
     */
    private SshConnectionService getSshService() {
        if (sshService == null) {
            sshService = SshConnectionServiceFactory.getInstance().getDefaultSshConnectionService();
        }
        return sshService;
    }
    
    /**
     * 转换为插件API的HostCheckContext
     */
    private com.datasophon.plugins.api.model.HostCheckContext toPluginContext(HostCheckContext context) {
        return com.datasophon.plugins.api.model.HostCheckContext.builder()
                .hostIp(context.getHostIp())
                .sshUser(context.getSshUser())
                .sshPassword(context.getSshPassword())
                .sshPort(context.getSshPort())
                .build();
    }
    
    @Override
    public String getCheckKey() {
        return "ssh-passwordless";
    }
    
    @Override
    public String getDisplayName() {
        return "SSH免密登录检查";
    }
    
    @Override
    public int getPriority() {
        return checkerProperties.getSshPasswordless().getPriority();
    }
    
    @Override
    public boolean isEnabled() {
        return checkerProperties.getSshPasswordless().isEnabled();
    }
    
    @Override
    public CheckResult execute(HostCheckContext context) {
        log.info("检查SSH免密登录: host={}", context.getHostIp());
        
        try {
            // 尝试使用密钥进行无密码登录
            // 如果能够成功执行命令，说明免密登录已配置
            var testResult = getSshService().testConnection(toPluginContext(context));
            
            if (testResult.isSuccess()) {
                log.info("SSH免密登录检查通过: host={}", context.getHostIp());
                return CheckResult.success("SSH免密登录已配置");
            } else {
                log.warn("SSH免密登录未配置: host={}, error={}", context.getHostIp(), testResult.error());
                return CheckResult.failure(
                    "SSH免密登录未配置",
                    "需要配置SSH密钥认证。可以点击修复按钮自动配置",
                    false,
                    true
                );
            }
            
        } catch (Exception e) {
            log.error("SSH免密登录检查失败: host={}, error={}", context.getHostIp(), e.getMessage(), e);
            return CheckResult.failure(
                "SSH免密登录检查失败: " + e.getMessage(),
                "请检查SSH服务是否正常运行，网络是否畅通",
                false,
                false
            );
        }
    }
    
    @Override
    public RepairResult repair(HostCheckContext context, Map<String, Object> params) {
        log.info("开始修复SSH免密登录: host={}", context.getHostIp());
        
        var config = checkerProperties.getSshPasswordless();
        
        try {
            // 步骤1：检查本地密钥对是否存在
            Path publicKeyPath = Paths.get(expandHome(config.getPublicKeyPath()));
            Path privateKeyPath = Paths.get(expandHome(config.getPrivateKeyPath()));
            
            if (!Files.exists(privateKeyPath) || !Files.exists(publicKeyPath)) {
                log.info("本地SSH密钥对不存在，开始生成");
                
                if (config.isAutoGenerateKey()) {
                    boolean generated = generateSshKeyPair(privateKeyPath, config);
                    if (!generated) {
                        return RepairResult.builder()
                                .success(false)
                                .message("生成SSH密钥对失败")
                                .build();
                    }
                } else {
                    return RepairResult.builder()
                            .success(false)
                            .message("SSH密钥对不存在，且未启用自动生成")
                            .build();
                }
            }
            
            // 步骤2：读取公钥内容
            String publicKey = Files.readString(publicKeyPath, StandardCharsets.UTF_8).trim();
            log.info("读取公钥成功，长度: {}", publicKey.length());
            
            // 步骤3：使用密码SSH登录到目标主机
            // 步骤4：将公钥追加到目标主机的 ~/.ssh/authorized_keys
            String setupScript = String.format(
                "#!/bin/bash\n" +
                "mkdir -p ~/.ssh\n" +
                "chmod 700 ~/.ssh\n" +
                "touch ~/.ssh/authorized_keys\n" +
                "chmod 600 ~/.ssh/authorized_keys\n" +
                "# 检查公钥是否已存在\n" +
                "if ! grep -q '%s' ~/.ssh/authorized_keys 2>/dev/null; then\n" +
                "  echo '%s' >> ~/.ssh/authorized_keys\n" +
                "  echo 'Public key added successfully'\n" +
                "else\n" +
                "  echo 'Public key already exists'\n" +
                "fi",
                publicKey.split(" ")[publicKey.split(" ").length - 1], // 使用公钥末尾的注释部分进行匹配
                publicKey
            );
            
            var result = getSshService().executeCommand(toPluginContext(context), setupScript, 30);
            
            if (!result.isSuccess()) {
                log.error("配置SSH免密登录失败: host={}, error={}", context.getHostIp(), result.error());
                return RepairResult.builder()
                        .success(false)
                        .message("配置失败: " + result.error())
                        .build();
            }
            
            log.info("SSH免密登录配置成功: host={}, output={}", context.getHostIp(), result.output());
            
            // 步骤5：验证免密登录是否成功
            var verifyResult = getSshService().testConnection(toPluginContext(context));
            if (verifyResult.isSuccess()) {
                return RepairResult.builder()
                        .success(true)
                        .message("SSH免密登录配置成功")
                        .build();
            } else {
                return RepairResult.builder()
                        .success(false)
                        .message("验证失败，免密登录仍不可用")
                        .build();
            }
            
        } catch (Exception e) {
            log.error("修复SSH免密登录失败: host={}, error={}", context.getHostIp(), e.getMessage(), e);
            return RepairResult.builder()
                    .success(false)
                    .message("修复失败: " + e.getMessage())
                    .build();
        }
    }
    
    /**
     * 生成SSH密钥对
     */
    private boolean generateSshKeyPair(Path privateKeyPath, CheckerProperties.SshPasswordlessConfig config) {
        try {
            // 确保.ssh目录存在
            Path sshDir = privateKeyPath.getParent();
            if (!Files.exists(sshDir)) {
                Files.createDirectories(sshDir);
                // 设置目录权限为700 (仅限所有者)
                File sshDirFile = sshDir.toFile();
                sshDirFile.setReadable(false, false);
                sshDirFile.setWritable(false, false);
                sshDirFile.setExecutable(false, false);
                sshDirFile.setReadable(true, true);
                sshDirFile.setWritable(true, true);
                sshDirFile.setExecutable(true, true);
            }
            
            // 使用ssh-keygen生成密钥对
            String command = String.format(
                "ssh-keygen -t %s -b %d -N '' -f %s -C 'datasophon-generated'",
                config.getKeyType(),
                config.getKeyBits(),
                privateKeyPath.toString()
            );
            
            Process process = Runtime.getRuntime().exec(command);
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                log.info("SSH密钥对生成成功: {}", privateKeyPath);
                
                // 设置私钥权限为600
                File privateKeyFile = privateKeyPath.toFile();
                privateKeyFile.setReadable(false, false);
                privateKeyFile.setWritable(false, false);
                privateKeyFile.setReadable(true, true);
                privateKeyFile.setWritable(true, true);
                
                return true;
            } else {
                log.error("SSH密钥对生成失败: exitCode={}", exitCode);
                return false;
            }
            
        } catch (IOException | InterruptedException e) {
            log.error("生成SSH密钥对异常", e);
            return false;
        }
    }
    
    /**
     * 展开路径中的~为用户主目录
     */
    private String expandHome(String path) {
        if (path.startsWith("~/")) {
            return System.getProperty("user.home") + path.substring(1);
        }
        return path;
    }
}

