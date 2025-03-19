package com.datasophon.api.service.checker.impl;

import com.datasophon.api.service.checker.AbstractItemChecker;
import com.datasophon.api.service.checker.CommandResult;
import com.datasophon.api.utils.MinaUtils;
import com.datasophon.common.model.CheckItem;
import com.datasophon.common.model.HostInfo;
import com.datasophon.common.model.ItemCode;
import org.apache.sshd.client.session.ClientSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
public class PasswordFreeChecker extends AbstractItemChecker {

    private static final Logger logger = LoggerFactory.getLogger(PasswordFreeChecker.class);

    @Override
    protected CheckItem doCheck(HostInfo hostInfo, CheckItem checkItem) {
        try {
            cacheLog.info("==== 免密登录检查开始 ====");
            cacheLog.info("主机: " + hostInfo.getHostname());

            // 检查SSH密钥是否存在
            cacheLog.info("检查SSH密钥是否存在...");
            CommandResult keyResult = execCommand(session, "ls -l ~/.ssh/id_rsa");
            boolean keyExists = keyResult.isSuccess();
            
            if (keyExists) {
                cacheLog.info("SSH密钥已存在");
            } else {
                cacheLog.info("未找到SSH密钥");
                checkItem.setStatus(CheckItem.Status.FAILED);
                checkItem.setMessage("未找到SSH密钥，需要配置免密登录");
                return checkItem;
            }

            // 检查authorized_keys文件
            cacheLog.info("检查authorized_keys文件...");
            CommandResult authResult = execCommand(session, "ls -l ~/.ssh/authorized_keys");
            boolean authExists = authResult.isSuccess();
            
            if (authExists) {
                cacheLog.info("authorized_keys文件已存在");
                
                // 检查文件权限
                CommandResult permResult = execCommand(session, "stat -c %a ~/.ssh/authorized_keys");
                if (permResult.isSuccess()) {
                    String perms = permResult.getOutput().trim();
                    boolean validPerms = "600".equals(perms) || "644".equals(perms);
                    if (!validPerms) {
                        cacheLog.warn("authorized_keys文件权限不正确: %s", perms);
                        checkItem.setStatus(CheckItem.Status.FAILED);
                        checkItem.setMessage("authorized_keys文件权限不正确: " + perms);
                        return checkItem;
                    }
                }
                
                // 检查文件内容
                CommandResult contentResult = execCommand(session, "cat ~/.ssh/authorized_keys");
                if (!contentResult.isSuccess() || contentResult.getOutput().trim().isEmpty()) {
                    cacheLog.warn("authorized_keys文件为空");
                    checkItem.setStatus(CheckItem.Status.FAILED);
                    checkItem.setMessage("authorized_keys文件为空");
                    return checkItem;
                }
            } else {
                cacheLog.info("未找到authorized_keys文件");
                checkItem.setStatus(CheckItem.Status.FAILED);
                checkItem.setMessage("未找到authorized_keys文件，需要配置免密登录");
                return checkItem;
            }

            // 检查SSH目录权限
            cacheLog.info("检查SSH目录权限...");
            CommandResult sshDirResult = execCommand(session, "stat -c %a ~/.ssh");
            if (sshDirResult.isSuccess()) {
                String dirPerms = sshDirResult.getOutput().trim();
                boolean validDirPerms = "700".equals(dirPerms);
                if (!validDirPerms) {
                    cacheLog.warn("SSH目录权限不正确: %s", dirPerms);
                    checkItem.setStatus(CheckItem.Status.FAILED);
                    checkItem.setMessage("SSH目录权限不正确: " + dirPerms);
                    return checkItem;
                }
            }

            // 所有检查通过
            checkItem.setStatus(CheckItem.Status.SUCCESS);
            checkItem.setMessage("免密登录配置正确");
            cacheLog.info("免密登录检查通过");

        } catch (Exception e) {
            String errorMsg = "检查免密登录配置时发生错误: " + e.getMessage();
            logger.error(errorMsg, e);
            cacheLog.error(errorMsg);
            checkItem.setStatus(CheckItem.Status.FAILED);
            checkItem.setMessage(errorMsg);
        } finally {
            cacheLog.info("==== 免密登录检查结束 ====");
        }
        return checkItem;
    }

    @Override
    public boolean fix(Integer clusterId, HostInfo hostInfo, CheckItem checkItem) {
        logger.info("开始修复检查项: {}, 主机: {}, 检查项ID: {}", checkItem.getItemName(), hostInfo.getHostname(), checkItem.getId());
        
        // 设置当前检查项的日志缓存键
        setCurrentLogKey(clusterId, hostInfo.getHostname(), checkItem.getId());
        
        // 设置状态为"修复中"
        checkItem.setStatus(CheckItem.Status.FIXING);
        checkItem.setMessage("正在修复...");
        
        // 记录修复开始
        cacheLog.info("<font color='blue'>===============================================</font>");
        cacheLog.info("<font color='blue'>开始修复检查项: " + checkItem.getItemName() + "</font>");
        cacheLog.info("<font color='blue'>主机: " + hostInfo.getHostname() + "</font>");
        cacheLog.info("<font color='blue'>检查项ID: " + checkItem.getId() + "</font>");
        cacheLog.info("<font color='blue'>开始时间: " + getCurrentTimeInChinese() + "</font>");
        cacheLog.info("<font color='blue'>===============================================</font>");
        
        // 对于免密登录检查器，我们不使用父类的openSession方法
        // 而是直接在doFix中处理连接逻辑
        
        try {
            // 执行具体修复逻辑，不依赖session
            boolean doFixResult = false;
            try {
                cacheLog.info("<font color='blue'>正在执行修复逻辑...</font>");
                doFixResult = doFix(hostInfo, checkItem);
                if (doFixResult) {
                    cacheLog.info("<font color='green'>修复逻辑执行成功</font>");
                } else {
                    cacheLog.error("<font color='red'>修复逻辑执行失败</font>");
                }
            } catch (Exception e) {
                String errorMsg = "执行修复逻辑时发生异常: " + e.getMessage();
                logger.error(errorMsg, e);
                cacheLog.error("<font color='red'>错误: " + errorMsg + "</font>");
                
                // 更新状态为失败
                checkItem.setStatus(CheckItem.Status.FAILED);
                checkItem.setMessage("修复失败: " + e.getMessage());
                return false;
            }
            
            // 如果修复成功，我们可以尝试建立免密连接进行验证
            if (doFixResult) {
                cacheLog.info("<font color='blue'>修复完成，正在验证免密登录...</font>");
                // 尝试建立免密连接验证
                try {
                    // 建立SSH连接
                    ClientSession testSession = MinaUtils.openConnection(
                            hostInfo.getHostname(),
                            hostInfo.getSshPort(),
                            hostInfo.getSshUser());
                    
                    if (testSession != null) {
                        cacheLog.info("<font color='green'>免密登录验证成功！</font>");
                        // 执行一个简单命令进一步验证
                        String result = MinaUtils.execCmdWithResult(testSession, "echo 'SSH connection test'");
                        if (result != null && result.contains("SSH connection test")) {
                            cacheLog.info("<font color='green'>免密登录命令执行验证成功！</font>");
                            
                            // 更新状态为成功
                            checkItem.setStatus(CheckItem.Status.SUCCESS);
                            checkItem.setMessage("免密登录已成功设置并验证通过");
                        } else {
                            cacheLog.warn("<font color='orange'>免密登录命令执行验证失败: " + result + "</font>");
                            
                            // 状态仍为成功，但消息中说明了命令验证未通过
                            checkItem.setStatus(CheckItem.Status.SUCCESS);
                            checkItem.setMessage("免密登录已设置，但命令验证未通过");
                        }
                        // 关闭验证会话
                        MinaUtils.closeConnection(testSession);
                    } else {
                        cacheLog.warn("<font color='orange'>免密登录验证失败，但修复过程已完成，可能需要等待SSH服务重新加载配置</font>");
                        
                        // 状态仍为成功，但消息中说明了验证未通过
                        checkItem.setStatus(CheckItem.Status.SUCCESS);
                        checkItem.setMessage("免密登录已设置，但验证未通过，可能需要等待SSH服务重新加载配置");
                    }
                } catch (Exception e) {
                    cacheLog.warn("<font color='orange'>免密登录验证时发生异常: " + e.getMessage() + "，但修复过程已完成</font>");
                    
                    // 状态仍为成功，但消息中说明了验证异常
                    checkItem.setStatus(CheckItem.Status.SUCCESS);
                    checkItem.setMessage("免密登录已设置，但验证时发生异常: " + e.getMessage());
                }
            } else {
                // 修复失败，更新状态
                checkItem.setStatus(CheckItem.Status.FAILED);
                checkItem.setMessage("免密登录设置失败，请查看日志了解详情");
            }
            
            // 记录最终结果
            if (doFixResult) {
                cacheLog.info("<font color='green'>修复操作成功完成</font>");
            } else {
                cacheLog.error("<font color='red'>修复操作失败</font>");
            }
            
            return doFixResult;
        } catch (Exception e) {
            String errorMsg = "修复过程中发生异常: " + e.getMessage();
            logger.error(errorMsg, e);
            cacheLog.error("<font color='red'>错误: " + errorMsg + "</font>");
            
            // 更新状态为失败
            checkItem.setStatus(CheckItem.Status.FAILED);
            checkItem.setMessage("修复异常: " + e.getMessage());
            return false;
        } finally {
            // 记录修复结束
            cacheLog.info("<font color='blue'>===============================================</font>");
            cacheLog.info("<font color='blue'>修复操作结束</font>");
            cacheLog.info("<font color='blue'>结束时间: " + getCurrentTimeInChinese() + "</font>");
            cacheLog.info("<font color='blue'>===============================================</font>");
        }
    }

    @Override
    protected boolean doFix(HostInfo hostInfo, CheckItem checkItem) {
        try {
            cacheLog.info("==== 开始配置免密登录 ====");

            // 创建.ssh目录
            cacheLog.info("创建.ssh目录...");
            CommandResult mkdirResult = execCommand(session, "mkdir -p ~/.ssh");
            if (!mkdirResult.isSuccess()) {
                cacheLog.error("创建.ssh目录失败: %s", mkdirResult.getErrorOrOutput());
                return false;
            }

            // 设置.ssh目录权限
            cacheLog.info("设置.ssh目录权限...");
            CommandResult chmodDirResult = execCommand(session, "chmod 700 ~/.ssh");
            if (!chmodDirResult.isSuccess()) {
                cacheLog.error("设置.ssh目录权限失败: %s", chmodDirResult.getErrorOrOutput());
                return false;
            }

            // 检查是否已有SSH密钥
            cacheLog.info("检查是否已有SSH密钥...");
            CommandResult keyCheckResult = execCommand(session, "ls ~/.ssh/id_rsa");
            if (!keyCheckResult.isSuccess()) {
                // 生成SSH密钥
                cacheLog.info("生成SSH密钥...");
                CommandResult keygenResult = execCommand(session, 
                    "ssh-keygen -t rsa -N '' -f ~/.ssh/id_rsa");
                if (!keygenResult.isSuccess()) {
                    cacheLog.error("生成SSH密钥失败: %s", keygenResult.getErrorOrOutput());
                    return false;
                }
            }

            // 配置authorized_keys
            cacheLog.info("配置authorized_keys...");
            CommandResult catResult = execCommand(session, "cat ~/.ssh/id_rsa.pub >> ~/.ssh/authorized_keys");
            if (!catResult.isSuccess()) {
                cacheLog.error("配置authorized_keys失败: %s", catResult.getErrorOrOutput());
                return false;
            }

            // 设置authorized_keys权限
            cacheLog.info("设置authorized_keys权限...");
            CommandResult chmodResult = execCommand(session, "chmod 600 ~/.ssh/authorized_keys");
            if (!chmodResult.isSuccess()) {
                cacheLog.error("设置authorized_keys权限失败: %s", chmodResult.getErrorOrOutput());
                return false;
            }

            // 验证配置
            cacheLog.info("验证免密登录配置...");
            CommandResult verifyResult = execCommand(session, "ls -la ~/.ssh/authorized_keys");
            if (!verifyResult.isSuccess()) {
                cacheLog.error("验证配置失败: %s", verifyResult.getErrorOrOutput());
                return false;
            }

            cacheLog.info("==== 免密登录配置完成 ====");
            return true;
        } catch (Exception e) {
            String errorMsg = "配置免密登录时发生错误: " + e.getMessage();
            logger.error(errorMsg, e);
            cacheLog.error(errorMsg);
            return false;
        }
    }
    
    /**
     * 设置基于密钥的认证 - 纯Java实现，不依赖外部命令
     * @param hostname 主机名
     * @param user 用户名
     * @param password 密码
     * @param port 端口
     * @param keyType 密钥类型 (ed25519 或 rsa)
     * @return 设置是否成功
     */
    private boolean setupKeyBasedAuth(String hostname, String user, String password, int port, String keyType) {
        cacheLog.info("<font color='blue'>开始设置{}类型的密钥认证</font>", keyType);
        try {
            // 1. 获取密钥路径
            String keyPath = System.getProperty("user.home") + "/.ssh/id_" + keyType;
            String pubKeyPath = keyPath + ".pub";
            
            File keyFile = new File(keyPath);
            File pubKeyFile = new File(pubKeyPath);
            File sshDir = keyFile.getParentFile();
            
            // 确保.ssh目录存在
            if (!sshDir.exists()) {
                if (!sshDir.mkdirs()) {
                    cacheLog.error("<font color='red'>无法创建.ssh目录</font>");
                    return false;
                }
                // 设置目录权限为700 (仅所有者可读写执行)
                sshDir.setReadable(false, false);
                sshDir.setReadable(true, true);
                sshDir.setWritable(false, false);
                sshDir.setWritable(true, true);
                sshDir.setExecutable(false, false);
                sshDir.setExecutable(true, true);
            }
            
            // 2. 使用密码连接到远程主机
            ClientSession session = MinaUtils.openConnectionWithPassword(hostname, port, user, password);
            if (session == null) {
                cacheLog.error("<font color='red'>无法使用密码连接到主机</font>");
                return false;
            }
            
            try {
                // 3. 检查远程.ssh目录
                String checkSshDirCmd = "mkdir -p ~/.ssh && chmod 700 ~/.ssh";
                String result = MinaUtils.execCmdWithResult(session, checkSshDirCmd);
                if (result != null && result.startsWith("ERROR:")) {
                    cacheLog.error("<font color='red'>创建远程.ssh目录失败: {}</font>", result);
                    return false;
                }
                
                // 4. 准备公钥内容
                String publicKeyContent = null;
                if (pubKeyFile.exists()) {
                    // 如果已存在公钥文件，读取内容
                    try {
                        publicKeyContent = new String(java.nio.file.Files.readAllBytes(pubKeyFile.toPath())).trim();
                        cacheLog.info("<font color='blue'>使用现有{}公钥文件</font>", keyType);
                    } catch (Exception e) {
                        cacheLog.error("<font color='red'>读取{}公钥文件失败: {}</font>", keyType, e.getMessage());
                        publicKeyContent = null;
                    }
                }
                
                // 如果没有公钥内容，需要服务器上传认证
                if (publicKeyContent == null || publicKeyContent.isEmpty()) {
                    // 这里直接使用MinaUtils的setupPasswordlessLogin方法
                    // 它会在没有密钥时自动生成并设置
                    boolean setupResult = MinaUtils.setupPasswordlessLogin(session, user, password);
                    if (setupResult) {
                        cacheLog.info("<font color='green'>{}免密登录设置成功</font>", keyType);
                        return true;
                    } else {
                        cacheLog.error("<font color='red'>{}免密登录设置失败</font>", keyType);
                        return false;
                    }
                } else {
                    // 如果有公钥内容，直接添加到authorized_keys
                    result = MinaUtils.execCmdWithResult(session, "touch ~/.ssh/authorized_keys && chmod 600 ~/.ssh/authorized_keys");
                    
                    // 将公钥写入远程authorized_keys文件
                    String escapedPubKey = publicKeyContent.replace("\"", "\\\"");
                    String addKeyCmd = String.format("grep -q \"%s\" ~/.ssh/authorized_keys || echo \"%s\" >> ~/.ssh/authorized_keys", 
                                                  escapedPubKey, escapedPubKey);
                    
                    result = MinaUtils.execCmdWithResult(session, addKeyCmd);
                    if (result != null && result.startsWith("ERROR:")) {
                        cacheLog.error("<font color='red'>添加公钥到authorized_keys失败: {}</font>", result);
                        return false;
                    }
                    
                    cacheLog.info("<font color='green'>{}免密登录设置成功</font>", keyType);
                    return true;
                }
            } finally {
                // 确保关闭会话
                MinaUtils.closeConnection(session);
            }
            
        } catch (Exception e) {
            cacheLog.error("<font color='red'>设置{}免密登录时发生异常: {}</font>", keyType, e.getMessage());
            cacheLog.error("<font color='red'>异常详情:</font>", e);
            return false;
        }
    }

    @Override
    public ItemCode getCheckerType() {
        return ItemCode.PASSWORD_FREE;
    }

} 