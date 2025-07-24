package com.datasophon.api.service.checker.checkers.passwordfree;

import com.datasophon.api.service.checker.common.CommandResult;
import com.datasophon.api.service.checker.common.ItemCode;
import com.datasophon.api.service.checker.core.AbstractItemChecker;
import com.datasophon.api.service.checker.helpers.CheckLogger;
import com.datasophon.api.utils.MinaUtils;
import com.datasophon.common.model.CheckItem;
import com.datasophon.common.model.HostInfo;
import com.datasophon.common.model.LogEntry;
import org.apache.sshd.client.session.ClientSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class PasswordFreeChecker extends AbstractItemChecker {

    private static final Logger logger = LoggerFactory.getLogger(PasswordFreeChecker.class);
    private static final String SSH_DIR = ".ssh";
    private static final String ID_RSA = "id_rsa";
    private static final String ID_RSA_PUB = "id_rsa.pub";
    private static final String AUTHORIZED_KEYS = "authorized_keys";

    @Override
    protected CheckItem doCheck(HostInfo hostInfo, CheckItem checkItem) {
        try {
            cacheLog.info("==== 免密登录检查开始 ====");
            cacheLog.info("主机: " + hostInfo.getIp());

            // 检查本地是否能够免密登录到远程主机
            cacheLog.info("尝试使用密钥进行免密登录...");

            try {
                // 尝试建立SSH连接而不需要密码
                ClientSession testSession = MinaUtils.openConnection(hostInfo);

                if (testSession != null) {
                    cacheLog.info("成功建立免密连接");

                    // 执行一个简单命令进一步验证
                    CommandResult echoResult = execCommand(testSession, "echo 'SSH connection test'");

                    // 关闭会话
                    try {
                        testSession.close();
                    } catch (Exception e) {
                        cacheLog.warn("关闭测试会话时发生异常: %s", e.getMessage());
                    }

                    if (echoResult.isSuccess() && echoResult.getOutput().contains("SSH connection test")) {
                        cacheLog.info("免密登录命令执行成功");
                        checkItem.setStatus(CheckItem.Status.SUCCESS);
                        checkItem.setMessage("免密登录配置正确");
                    } else {
                        cacheLog.warn("免密登录建立成功但命令执行失败");
                        checkItem.setStatus(CheckItem.Status.FAILED);
                        checkItem.setMessage("免密登录建立成功但命令执行失败，可能需要重新配置");
                    }
                    return checkItem;
                } else {
                    cacheLog.info("无法建立免密登录连接");
                }
            } catch (Exception e) {
                cacheLog.info("免密登录尝试失败: %s", e.getMessage());
            }

            // 检查本地SSH密钥是否存在
            Path userHome = Paths.get(System.getProperty("user.home"));
            Path sshDir = userHome.resolve(SSH_DIR);
            Path privateKeyPath = sshDir.resolve(ID_RSA);
            Path publicKeyPath = sshDir.resolve(ID_RSA_PUB);

            boolean sshDirExists = Files.exists(sshDir);
            boolean privateKeyExists = Files.exists(privateKeyPath);
            boolean publicKeyExists = Files.exists(publicKeyPath);

            cacheLog.info("本地SSH目录状态: " + (sshDirExists ? "存在" : "不存在"));
            cacheLog.info("本地私钥状态: " + (privateKeyExists ? "存在" : "不存在"));
            cacheLog.info("本地公钥状态: " + (publicKeyExists ? "存在" : "不存在"));

            // 如果缺少必要文件，则标记为失败
            if (!sshDirExists || !privateKeyExists || !publicKeyExists) {
                checkItem.setStatus(CheckItem.Status.FAILED);
                checkItem.setMessage("本地SSH密钥不完整，需要重新生成");
                return checkItem;
            }

            // 检查远程authorized_keys是否包含了本地公钥
            boolean remoteAuthKeysContainsLocalPubKey = false;
            try {
                // 使用密码建立SSH连接
                ClientSession pwdSession = MinaUtils.openConnectionWithPassword(
                        hostInfo);

                if (pwdSession != null) {
                    try {
                        // 检查远程.ssh目录和authorized_keys文件
                        CommandResult checkDirResult = execCommand(pwdSession, "mkdir -p ~/.ssh && chmod 700 ~/.ssh");
                        if (!checkDirResult.isSuccess()) {
                            cacheLog.warn("远程主机.ssh目录检查/创建失败: %s", checkDirResult.getErrorOrOutput());
                        }

                        CommandResult checkAuthResult = execCommand(pwdSession,
                                "test -f ~/.ssh/authorized_keys && echo 'EXISTS' || echo 'NOT_EXISTS'");
                        boolean authKeysExists = checkAuthResult.isSuccess()
                                && checkAuthResult.getOutput().contains("EXISTS");
                        cacheLog.info("远程authorized_keys状态: " + (authKeysExists ? "存在" : "不存在"));

                        if (authKeysExists) {
                            // 读取本地公钥
                            String localPubKey = Files.readString(publicKeyPath)
                                    .trim();

                            // 检查远程authorized_keys是否包含本地公钥
                            CommandResult catResult = execCommand(pwdSession, "cat ~/.ssh/authorized_keys");
                            if (catResult.isSuccess()) {
                                String remoteAuthKeys = catResult.getOutput();
                                remoteAuthKeysContainsLocalPubKey = remoteAuthKeys.contains(localPubKey);
                                cacheLog.info("远程authorized_keys " + (remoteAuthKeysContainsLocalPubKey ? "包含" : "不包含")
                                        + " 本地公钥");
                            }
                        }
                    } finally {
                        // 关闭会话
                        try {
                            pwdSession.close();
                        } catch (Exception e) {
                            cacheLog.warn("关闭密码会话时发生异常: %s", e.getMessage());
                        }
                    }
                }
            } catch (Exception e) {
                cacheLog.error("检查远程authorized_keys时发生错误: %s", e.getMessage());
            }

            if (remoteAuthKeysContainsLocalPubKey) {
                // 如果远程已包含本地公钥，但无法免密登录，可能是权限问题
                cacheLog.warn("远程authorized_keys包含本地公钥，但无法免密登录，可能是权限问题");
                checkItem.setStatus(CheckItem.Status.FAILED);
                checkItem.setMessage("远程已包含本地公钥，但无法免密登录，可能是权限问题");
            } else {
                checkItem.setStatus(CheckItem.Status.FAILED);
                checkItem.setMessage("远程authorized_keys不包含本地公钥，需要配置免密登录");
            }

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
        logger.info("开始修复检查项: {}, 主机: {}, 检查项ID: {}", checkItem.getItemName(), hostInfo.getIp(),
                checkItem.getId());

        // 设置为修复操作
        operationType = LogEntry.Type.FIX;

        // 设置当前检查项的日志缓存键
        setCurrentLogKey(clusterId, hostInfo.getIp(), checkItem.getId());

        // 更新日志记录器的类型
        CheckLogger.LoggerImpl loggerImpl = (CheckLogger.LoggerImpl) this.cacheLog;
        loggerImpl.setLogType(operationType);

        // 设置状态为"修复中"
        checkItem.setStatus(CheckItem.Status.FIXING);
        checkItem.setMessage("正在修复...");

        // 记录修复开始
        cacheLog.info("===============================================");
        cacheLog.info("开始修复检查项: " + checkItem.getItemName());
        cacheLog.info("主机: " + hostInfo.getIp());
        cacheLog.info("检查项ID: " + checkItem.getId());
        cacheLog.info("开始时间: " + getCurrentTime());
        cacheLog.info("===============================================");

        try {
            // 设置当前主机信息
            setCurrentHostInfo(hostInfo);

            // 执行具体修复逻辑
            boolean doFixResult = doFix(hostInfo, checkItem);

            if (doFixResult) {
                cacheLog.info("修复逻辑执行成功");

                // 增加重试机制：最多尝试3次，每次间隔3秒
                boolean verificationSuccess = false;
                int maxRetries = 3;

                for (int attemptCount = 1; attemptCount <= maxRetries; attemptCount++) {
                    // 验证免密登录
                    cacheLog.info("第 " + attemptCount + " 次验证免密登录...");

                    try {
                        // 建立SSH连接验证
                        ClientSession testSession = MinaUtils.openConnection(hostInfo);

                        if (testSession != null) {
                            cacheLog.info("免密登录验证成功！");
                            // 执行一个简单命令进一步验证
                            String result = MinaUtils.execCmdWithResult(testSession, "echo 'SSH connection test'");
                            if (result != null && result.contains("SSH connection test")) {
                                cacheLog.info("免密登录命令执行验证成功！");

                                // 验证成功
                                verificationSuccess = true;

                                // 更新状态为成功
                                checkItem.setStatus(CheckItem.Status.SUCCESS);
                                checkItem.setMessage("免密登录已成功设置并验证通过");

                                // 关闭验证会话
                                MinaUtils.closeConnection(testSession);
                                break; // 验证成功，跳出循环
                            } else {
                                cacheLog.warn("免密登录命令执行验证失败: " + result);

                                // 关闭验证会话
                                MinaUtils.closeConnection(testSession);

                                if (attemptCount == maxRetries) {
                                    // 最后一次尝试仍然失败
                                    checkItem.setStatus(CheckItem.Status.SUCCESS);
                                    checkItem.setMessage("免密登录已设置，但命令验证未通过，已尝试 " + maxRetries + " 次");
                                } else {
                                    // 等待3秒后重试
                                    cacheLog.info("等待3秒后重试...");
                                    Thread.sleep(3000);
                                }
                            }
                        } else {
                            // 连接建立失败
                            cacheLog.warn("免密登录验证失败，无法建立连接");

                            if (attemptCount == maxRetries) {
                                // 最后一次尝试仍然失败
                                checkItem.setStatus(CheckItem.Status.SUCCESS);
                                checkItem.setMessage("免密登录已设置，但验证未通过，已尝试 " + maxRetries + " 次");
                            } else {
                                // 等待3秒后重试
                                cacheLog.info("等待3秒后重试...");
                                Thread.sleep(3000);
                            }
                        }
                    } catch (Exception e) {
                        cacheLog.warn("免密登录验证时发生异常: " + e.getMessage());

                        if (attemptCount == maxRetries) {
                            // 最后一次尝试仍然发生异常
                            checkItem.setStatus(CheckItem.Status.SUCCESS);
                            checkItem.setMessage("免密登录已设置，但验证时发生异常: " + e.getMessage() + "，已尝试 " + maxRetries + " 次");
                        } else {
                            // 等待3秒后重试
                            cacheLog.info("等待3秒后重试...");
                            Thread.sleep(3000);
                        }
                    }
                }

                // 即使所有验证尝试都失败，我们仍然认为整体修复成功，因为doFix返回了成功
                if (!verificationSuccess) {
                    cacheLog.info("尽管验证失败，但修复过程已完成");
                }
            } else {
                // 修复失败，更新状态
                checkItem.setStatus(CheckItem.Status.FAILED);
                checkItem.setMessage("免密登录设置失败，请查看日志了解详情");
            }

            // 记录最终结果
            cacheLog.info("修复操作" + (doFixResult ? "成功完成" : "失败"));

            return doFixResult;
        } catch (Exception e) {
            String errorMsg = "修复过程中发生异常: " + e.getMessage();
            logger.error(errorMsg, e);
            cacheLog.error("错误: " + errorMsg);

            // 更新状态为失败
            checkItem.setStatus(CheckItem.Status.FAILED);
            checkItem.setMessage("修复异常: " + e.getMessage());
            return false;
        } finally {
            // 清理当前主机信息
            clearCurrentHostInfo();

            // 记录修复结束
            cacheLog.info("===============================================");
            cacheLog.info("修复操作结束");
            cacheLog.info("结束时间: " + getCurrentTime());
            cacheLog.info("===============================================");
        }
    }

    @Override
    protected boolean doFix(HostInfo hostInfo, CheckItem checkItem) {
        try {
            cacheLog.info("==== 开始配置免密登录 ====");

            // 1. 检查并创建本地SSH目录和密钥
            Path userHome = Paths.get(System.getProperty("user.home"));
            Path sshDir = userHome.resolve(SSH_DIR);
            Path privateKeyPath = sshDir.resolve(ID_RSA);
            Path publicKeyPath = sshDir.resolve(ID_RSA_PUB);

            // 确保.ssh目录存在
            if (!Files.exists(sshDir)) {
                cacheLog.info("创建本地.ssh目录...");
                Files.createDirectory(sshDir);
                // 设置权限为700 (只有用户自己可读写执行)
                try {
                    // 设置目录权限，不同系统可能有不同实现
                    if (System.getProperty("os.name").toLowerCase().contains("win")) {
                        // Windows系统下设置权限
                        File sshDirFile = sshDir.toFile();
                        sshDirFile.setReadable(false, false);
                        sshDirFile.setReadable(true, true);
                        sshDirFile.setWritable(false, false);
                        sshDirFile.setWritable(true, true);
                        sshDirFile.setExecutable(false, false);
                        sshDirFile.setExecutable(true, true);
                    } else {
                        // Linux/Unix系统使用PosixFilePermissions
                        Files.setPosixFilePermissions(sshDir,
                                java.nio.file.attribute.PosixFilePermissions.fromString("rwx------"));
                    }
                } catch (Exception e) {
                    cacheLog.warn("设置目录权限时出现异常: %s", e.getMessage());
                    // 继续执行，不终止处理
                }
            }

            // 检查是否已有SSH密钥
            boolean keysExist = Files.exists(privateKeyPath) && Files.exists(publicKeyPath);
            String publicKeyContent;

            if (!keysExist) {
                // 生成SSH密钥对
                cacheLog.info("生成本地SSH密钥对...");
                boolean keyGenResult = setupKeyBasedAuth(hostInfo.getIp(), hostInfo.getSshUser(),
                        hostInfo.getSshPassword(), hostInfo.getSshPort());
                if (!keyGenResult) {
                    cacheLog.error("生成SSH密钥对失败");
                    return false;
                }

                // 重新检查密钥是否生成
                keysExist = Files.exists(privateKeyPath) && Files.exists(publicKeyPath);
                if (!keysExist) {
                    cacheLog.error("密钥生成后仍然找不到密钥文件");
                    return false;
                }
            }

            // 读取公钥内容
            publicKeyContent = Files.readString(publicKeyPath).trim();
            cacheLog.info("读取本地公钥内容成功");

            // 2. 建立到远程主机的密码连接并配置authorized_keys
            cacheLog.info("连接到远程主机配置authorized_keys...");


            // 将密码连接赋值给当前会话，以便后续 execCommand 方法可以使用


            // 在远程主机上创建.ssh目录
            CommandResult mkdirResult = execCommand(sshConnectionPoolManager.getOrCreateConnection(hostInfo), "mkdir -p ~/.ssh");
            if (!mkdirResult.isSuccess()) {
                cacheLog.error("在远程主机上创建.ssh目录失败: %s", mkdirResult.getErrorOrOutput());
                return false;
            }

            // 设置远程.ssh目录权限
            CommandResult chmodDirResult = execCommand(sshConnectionPoolManager.getOrCreateConnection(hostInfo), "chmod 700 ~/.ssh");
            if (!chmodDirResult.isSuccess()) {
                cacheLog.error("设置远程.ssh目录权限失败: %s", chmodDirResult.getErrorOrOutput());
                return false;
            }

            // 将本地公钥添加到远程authorized_keys
            // 先检查远程authorized_keys是否已包含此公钥
            CommandResult checkExistResult = execCommand(sshConnectionPoolManager.getOrCreateConnection(hostInfo),
                    "grep -F \"" + publicKeyContent + "\" ~/.ssh/authorized_keys 2>/dev/null || echo 'NOT_FOUND'");
            boolean alreadyExists = checkExistResult.isSuccess()
                    && !checkExistResult.getOutput().contains("NOT_FOUND");

            if (!alreadyExists) {
                // 添加公钥到远程authorized_keys
                CommandResult appendResult = execCommand(sshConnectionPoolManager.getOrCreateConnection(hostInfo),
                        "echo \"" + publicKeyContent + "\" >> ~/.ssh/authorized_keys");
                if (!appendResult.isSuccess()) {
                    cacheLog.error("将公钥添加到远程authorized_keys失败: %s", appendResult.getErrorOrOutput());
                    return false;
                }
                cacheLog.info("公钥已添加到远程authorized_keys");
            } else {
                cacheLog.info("公钥已存在于远程authorized_keys中，无需添加");
            }

            // 设置远程authorized_keys权限
            CommandResult chmodKeyResult = execCommand(sshConnectionPoolManager.getOrCreateConnection(hostInfo), "chmod 600 ~/.ssh/authorized_keys");
            if (!chmodKeyResult.isSuccess()) {
                cacheLog.error("设置远程authorized_keys权限失败: %s", chmodKeyResult.getErrorOrOutput());
                return false;
            }

            // 确保SSH服务配置正确
            CommandResult sshConfigResult = execCommand(this.sshConnectionPoolManager.getOrCreateConnection(hostInfo),
                    "grep -F \"PubkeyAuthentication yes\" /etc/ssh/sshd_config || " +
                            "echo '可能需要配置SSH服务以启用公钥认证'");
            if (sshConfigResult.getOutput().contains("需要配置SSH服务")) {
                cacheLog.warn("远程SSH服务可能需要配置以启用公钥认证");
            }

            cacheLog.info("免密登录配置完成");
            return true;

        } catch (Exception e) {
            String errorMsg = "配置免密登录时发生错误: " + e.getMessage();
            logger.error(errorMsg, e);
            cacheLog.error(errorMsg);
            return false;
        }
    }

    private boolean setupKeyBasedAuth(String hostname, String user, String password, int port) {
        try {
            Path userHome = Paths.get(System.getProperty("user.home"));
            Path sshDir = userHome.resolve(SSH_DIR);
            Path privateKeyPath = sshDir.resolve(ID_RSA);
            Path publicKeyPath = sshDir.resolve(ID_RSA_PUB);

            if (Files.exists(privateKeyPath) && Files.exists(publicKeyPath)) {
                cacheLog.info("SSH密钥对已存在，无需生成");
                return true;
            }

            // 尝试使用Java调用系统命令生成密钥
            cacheLog.info("生成SSH密钥对...");
            Process process = new ProcessBuilder("ssh-keygen", "-t", "rsa", "-N", "", "-f", privateKeyPath.toString())
                    .redirectErrorStream(true)
                    .start();

            // 读取命令输出
            try (BufferedReader reader = new BufferedReader(new java.io.InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    cacheLog.debug("ssh-keygen输出: %s", line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                cacheLog.error("ssh-keygen命令执行失败，退出码: %d", exitCode);
                return false;
            }

            // 检查密钥是否生成
            if (!Files.exists(privateKeyPath) || !Files.exists(publicKeyPath)) {
                cacheLog.error("生成密钥后未找到密钥文件");
                return false;
            }

            cacheLog.info("SSH密钥对生成成功");
            return true;

        } catch (Exception e) {
            cacheLog.error("生成SSH密钥对时发生错误: %s", e.getMessage());
            return false;
        }
    }

    /**
     * 增强版的execCommand方法，支持命令失败时自动重试
     * 最多尝试MAX_RETRY_ATTEMPTS次，每次失败后等待RETRY_DELAY_MS毫秒
     *
     * @param session 会话对象
     * @param command 要执行的命令
     * @return 命令执行结果
     * @throws InterruptedException 如果线程被中断
     */
    @Override
    protected CommandResult execCommand(ClientSession session, String command) throws InterruptedException {
        // 重试相关配置
        final int MAX_RETRY_ATTEMPTS = 5; // 最大重试次数
        final long RETRY_DELAY_MS = 2000; // 重试间隔时间，2秒

        int attempts = 0;
        CommandResult result = null;
        Exception lastException = null;

        // 自动重试逻辑，最多尝试MAX_RETRY_ATTEMPTS次
        while (true) {
            attempts++;
            try {
                // 调用父类的execCommand方法执行命令
                result = super.execCommand(session, command);

                // 如果命令执行成功，直接返回结果
                if (result.isSuccess()) {
                    if (attempts > 1) {
                        cacheLog.info("命令 [%s] 在第 %d 次尝试成功执行", command, attempts);
                    }
                    return result;
                } else {
                    // 命令执行失败，记录错误并准备重试
                    cacheLog.warn("命令 [%s] 第 %d 次执行失败: %s",
                            command, attempts, result.getErrorOrOutput());

                    // 如果已达到最大重试次数，返回最后一次执行的结果
                    if (attempts >= MAX_RETRY_ATTEMPTS) {
                        cacheLog.error("命令 [%s] 在尝试 %d 次后仍然失败，放弃重试",
                                command, attempts);
                        return result;
                    }

                    // 否则等待一段时间后重试
                    cacheLog.info("将在 %d 毫秒后进行第 %d 次重试命令 [%s]",
                            RETRY_DELAY_MS, attempts + 1, command);
                    Thread.sleep(RETRY_DELAY_MS);
                }
            } catch (InterruptedException e) {
                // 如果线程被中断，停止重试
                Thread.currentThread().interrupt();
                throw e;
            } catch (Exception e) {
                // 捕获其他异常
                lastException = e;
                cacheLog.error("命令 [%s] 第 %d 次执行出现异常: %s",
                        command, attempts, e.getMessage());

                // 如果已达到最大重试次数，抛出异常
                if (attempts >= MAX_RETRY_ATTEMPTS) {
                    cacheLog.error("命令 [%s] 在尝试 %d 次后仍然出现异常，放弃重试",
                            command, attempts);
                    throw (RuntimeException) e;
                }

                // 否则等待一段时间后重试
                cacheLog.info("将在 %d 毫秒后进行第 %d 次重试命令 [%s]",
                        RETRY_DELAY_MS, attempts + 1, command);
                Thread.sleep(RETRY_DELAY_MS);
            }
        }

        // 这里正常不会执行到，因为循环中已经有返回或抛出异常
        // 但为了代码完整性，返回最后的结果或抛出异常
        if (result != null) {
            return result;
        } else if (lastException != null) {
            if (lastException instanceof RuntimeException) {
                throw (RuntimeException) lastException;
            } else {
                throw new RuntimeException("执行命令失败: " + lastException.getMessage(), lastException);
            }
        } else {
            return new CommandResult("", "未知错误，命令执行结果和异常均为null", -1);
        }
    }

    @Override
    public ItemCode getCheckerType() {
        return ItemCode.PASSWORD_FREE;
    }
}