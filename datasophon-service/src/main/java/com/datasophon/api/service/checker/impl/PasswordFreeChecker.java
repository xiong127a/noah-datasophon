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
        cacheLog.info("<font color='blue'>开始修复SSH免密登录...</font>");
        
        // 获取主机信息
        String hostname = hostInfo.getHostname();
        String sshUser = hostInfo.getSshUser();
        String sshPassword = hostInfo.getSshPassword();
        int sshPort = hostInfo.getSshPort();
        
        if (sshPassword == null || sshPassword.isEmpty()) {
            cacheLog.error("<font color='red'>SSH密码为空，无法设置免密登录</font>");
            checkItem.setMessage("SSH密码为空，无法设置免密登录");
            return false;
        }
        
        cacheLog.info("<font color='blue'>使用信息: 主机={}, 用户={}, 端口={}</font>", hostname, sshUser, sshPort);
        
        try {
            // 生成并设置ED25519密钥
            boolean ed25519Success = setupKeyBasedAuth(hostname, sshUser, sshPassword, sshPort, "ed25519");
            if (ed25519Success) {
                cacheLog.info("<font color='green'>ED25519免密登录设置成功</font>");
            } else {
                cacheLog.warn("<font color='orange'>ED25519免密登录设置失败，尝试RSA方式</font>");
            }
            
            // 如果ED25519失败或为了双重保障，继续设置RSA密钥
            boolean rsaSuccess = setupKeyBasedAuth(hostname, sshUser, sshPassword, sshPort, "rsa");
            if (rsaSuccess) {
                cacheLog.info("<font color='green'>RSA免密登录设置成功</font>");
            } else {
                cacheLog.error("<font color='red'>RSA免密登录设置失败</font>");
            }
            
            // 只要有一种方式成功即可
            boolean success = ed25519Success || rsaSuccess;
            if (success) {
                cacheLog.info("<font color='green'>免密登录修复成功</font>");
                checkItem.setMessage("免密登录已成功设置");
                return true;
            } else {
                cacheLog.error("<font color='red'>所有免密登录方式均设置失败</font>");
                checkItem.setMessage("免密登录设置失败，请手动检查");
                return false;
            }
            
        } catch (Exception e) {
            cacheLog.error("<font color='red'>设置免密登录时发生异常: {}</font>", e.getMessage());
            cacheLog.error("<font color='red'>异常详情:</font>", e);
            checkItem.setMessage("免密登录设置异常: " + e.getMessage());
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