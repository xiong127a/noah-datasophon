package com.datasophon.api.utils;


import com.datasophon.common.model.HostInfo;
import com.datasophon.plugins.api.model.CommandResult;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ChannelExec;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.keyverifier.AcceptAllServerKeyVerifier;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.sftp.client.SftpClientFactory;
import org.apache.sshd.sftp.client.fs.SftpFileSystem;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class MinaUtils {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(MinaUtils.class);

    /** 打开远程会话 */
    public static ClientSession openConnection(HostInfo hostInfo) throws IOException {
        String sshIp = hostInfo.getIp();
        Integer sshPort = hostInfo.getSshPort();
        String sshUser = hostInfo.getSshUser();
        ClientSession session;
        try (SshClient sshClient = SshClient.setUpDefaultClient()) {

            // 配置自动接受未知主机密钥
            sshClient.setServerKeyVerifier(AcceptAllServerKeyVerifier.INSTANCE);

            sshClient.start();

            // 获取两种格式的私钥路径
            String privateKeyPathRSA = System.getProperty("user.home") + "/.ssh/id_rsa";
            String privateKeyPathED25519 = System.getProperty("user.home") + "/.ssh/id_ed25519";

            try {
                // 创建会话连接
                session = sshClient.connect(sshUser, sshIp, sshPort).verify().getClientSession();
                boolean authAdded = false;

                // 尝试使用ED25519密钥（先尝试更安全的密钥）
                File ed25519KeyFile = new File(privateKeyPathED25519);
                if (ed25519KeyFile.exists()) {
                    try {
                        String privateKeyContent = new String(Files.readAllBytes(Paths.get(privateKeyPathED25519)));
                        session.addPublicKeyIdentity(getKeyPairFromString(privateKeyContent));
                        LOG.info("已添加ED25519密钥认证");
                        authAdded = true;
                    } catch (Exception e) {
                        LOG.warn("ED25519密钥加载失败: {}", e.getMessage());
                    }
                }

                // 尝试使用RSA密钥
                File rsaKeyFile = new File(privateKeyPathRSA);
                if (rsaKeyFile.exists()) {
                    try {
                        String privateKeyContent = new String(Files.readAllBytes(Paths.get(privateKeyPathRSA)));
                        session.addPublicKeyIdentity(getKeyPairFromString(privateKeyContent));
                        LOG.info("已添加RSA密钥认证");
                        authAdded = true;
                    } catch (Exception e) {
                        LOG.warn("RSA密钥加载失败: {}", e.getMessage());
                    }
                }

                // 如果没有添加任何认证方式，则失败
                if (!authAdded) {
                    LOG.error("没有可用的SSH密钥");
                    return null;
                }

                // 执行认证
                if (session.auth().verify().isFailure()) {
                    LOG.error("SSH密钥认证失败");
                    return null;
                }
            } catch (IOException e) {
                LOG.error("免密登录失败: {}", e.getMessage());
                return null;
            } catch (Exception e) {
                LOG.error("连接异常: {}", e.getMessage());
                return null;
            }
        }

        LOG.info("{} 连接成功", sshIp);
        return session;
    }

    /** 关闭远程会话 */
    public static void closeConnection(ClientSession session) {
        if (session == null) {
            return;
        }

        try {
            if (session.isOpen()) {
                session.close();
            }
        } catch (IOException e) {
            // 记录异常但不抛出，避免影响调用方
            LOG.warn("关闭SSH会话时发生异常: {}", e.getMessage());
        }
    }

    /** 获取密钥对 */
    static KeyPair getKeyPairFromString(String pk) {
        final KeyPairGenerator rsa;
        try {
            rsa = KeyPairGenerator.getInstance("RSA");
            final KeyPair keyPair = rsa.generateKeyPair();
            final ByteArrayOutputStream stream = new ByteArrayOutputStream();
            stream.write(pk.getBytes());
            final ObjectOutputStream o = new ObjectOutputStream(stream);
            o.writeObject(keyPair);
            return keyPair;
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 同步执行,需要获取执行完的结果
     *
     * @param session 连接
     * @param command 命令
     * @return 结果对象，包含退出码、输出和错误信息
     */
    public static CommandResult execCmdWithResultObject(ClientSession session, String command) {
        if (session == null) {
            LOG.error("SSH会话为空，无法执行命令: {}", command);
            return new CommandResult(command, -1, "", "SSH会话为空");
        }

        session.resetAuthTimeout();
        LOG.info("执行命令: {}", command);
        // 命令返回的结果
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        try (ChannelExec ce = session.createExecChannel(command)) {
            try {
                ce.setOut(out);
                ce.setErr(err);
                // 打开通道并执行命令
                ce.open();

                // 等待命令执行完成或超时
                Set<ClientChannelEvent> events = ce.waitFor(EnumSet.of(ClientChannelEvent.CLOSED),
                        TimeUnit.SECONDS.toMillis(100000));

                if (events.contains(ClientChannelEvent.TIMEOUT)) {
                    LOG.error("命令执行超时: {}", command);
                    return new CommandResult(command, 124,"命令执行超时", "");
                }

                int exitStatus = ce.getExitStatus();
                LOG.info("命令退出状态: {}", exitStatus);

                String outResult = out.toString();
                String errResult = err.toString();

                if (exitStatus != 0) {
                    // 处理常见的错误并尝试替代解决方案
                    if (command.contains("chkconfig") && exitStatus == 127) {
                        LOG.warn("chkconfig命令不存在，尝试使用systemctl替代...");
                        String serviceName = command.substring(command.lastIndexOf(" ") + 1);
                        return execCmdWithResultObject(session, "systemctl enable " + serviceName);
                    } else if (command.contains("\\cp") && exitStatus == 1) {
                        LOG.warn("复制文件失败，尝试使用sudo...");
                        return execCmdWithResultObject(session, "sudo " + command);
                    } else if (command.contains("service") && command.contains("restart")) {
                        LOG.warn("service命令启动服务失败，尝试使用systemctl...");
                        String serviceName = command.substring(command.indexOf("service ") + 8, command.lastIndexOf(" "));
                        LOG.info("尝试使用systemctl重启服务: {}", serviceName);
                        return execCmdWithResultObject(session, "systemctl restart " + serviceName);
                    }

                    LOG.error("命令执行失败: {} - 错误信息: {}, 退出码: {}", command, errResult, exitStatus);
                    return new CommandResult(command, exitStatus, outResult, errResult);
                }

                LOG.info("命令执行结果: {}", outResult);
                return new CommandResult(command, 0, outResult, "");
            } catch (IOException e) {
                LOG.error("执行命令异常: {} - {}", command, e.getMessage());
                return new CommandResult(command, -1, "", e.getMessage());
            }
        } catch (IOException e) {
            LOG.error("关闭命令通道异常", e);
        }
        return null;
    }

    /**
     * 同步执行命令，超时时间自定义
     */
    public static CommandResult execCmdWithResultObject(ClientSession session, String command, long timeoutSeconds) {
        if (session == null) {
            LOG.error("SSH会话为空，无法执行命令: {}", command);
            return new CommandResult(command, -1, "", "SSH会话为空");
        }

        session.resetAuthTimeout();
        LOG.info("执行命令(超时{}秒): {}", timeoutSeconds, command);
        // 命令返回的结果
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        try (ChannelExec ce = session.createExecChannel(command)) {
            try {
                ce.setOut(out);
                ce.setErr(err);
                // 打开通道并执行命令
                ce.open();

                // 等待命令执行完成或超时
                Set<ClientChannelEvent> events = ce.waitFor(EnumSet.of(ClientChannelEvent.CLOSED),
                        TimeUnit.SECONDS.toMillis(timeoutSeconds));

                if (events.contains(ClientChannelEvent.TIMEOUT)) {
                    LOG.error("命令执行超时: {}", command);
                    return new CommandResult(command, 124, "", "命令执行超时");
                }

                int exitStatus = ce.getExitStatus();
                LOG.info("命令退出状态: {}", exitStatus);

                String outResult = out.toString();
                String errResult = err.toString();

                if (exitStatus != 0) {
                    LOG.error("命令执行失败: {} - 错误信息: {}, 退出码: {}", command, errResult, exitStatus);
                    return new CommandResult(command, exitStatus, outResult, errResult);
                }

                LOG.info("命令执行结果: {}", outResult);
                return new CommandResult(command, 0, outResult, "");
            } catch (IOException e) {
                LOG.error("执行命令异常: {} - {}", command, e.getMessage());
                return new CommandResult(command, -1, "", e.getMessage());
            }
        } catch (IOException e) {
            LOG.error("关闭命令通道异常", e);
        }
        return null;
    }

    public static String executeCommandAndGetResult(ClientSession session, String command) throws IOException {
        CommandResult result = execCmdWithResultObject(session, command);
        if (result.isSuccess()) {
            return result.output();
        } else {
            throw new IOException(
                    "Command execution failed with exit code " + result.exitCode() + ": " + result.error());
        }
    }

    /**
     * 上传文件,相同路径ui覆盖
     *
     * @param session    连接
     * @param remotePath 远程目录地址
     * @param inputFile  文件 File
     */
    public static boolean uploadFile(ClientSession session, String remotePath, String inputFile) {
        File uploadFile = new File(inputFile);
        InputStream input;
        SftpFileSystem sftp;
        try {
            sftp = SftpClientFactory.instance().createSftpFileSystem(session);
            Path path = sftp.getDefaultDir().resolve(remotePath);
            if (!Files.exists(path)) {
                LOG.info("create pathHome {} ", path);
                Files.createDirectories(path);
            }
            input = Files.newInputStream(uploadFile.toPath());
            Path file = path.resolve(uploadFile.getName());
            if (Files.exists(file)) {
                LOG.info("delete file  {}", file);
                Files.deleteIfExists(file);
            }
            Files.copy(input, file);
            LOG.info("file copy success");
            return true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /** 使用密码进行连接 */
    public static ClientSession openConnectionWithPassword(HostInfo hostInfo) throws IOException {
        if (hostInfo == null) {
            LOG.error("主机信息为空，无法建立连接");
            return null;
        }

        String sshIp = hostInfo.getIp();
        Integer sshPort = hostInfo.getSshPort();
        String sshUser = hostInfo.getSshUser();
        String sshPassword = hostInfo.getSshPassword();

        if (sshIp == null || sshPort == null || sshUser == null || sshPassword == null) {
            LOG.error("SSH连接信息不完整: IP={}, 端口={}, 用户名={}, 密码={}",
                    sshIp, sshPort, sshUser, sshPassword == null ? "null" : "******");
            hostInfo.setSshErrorMsg("SSH连接信息不完整");
            hostInfo.setErrorMessage("SSH连接失败: 连接信息不完整");
            return null;
        }

        ClientSession session;
        try (SshClient sshClient = SshClient.setUpDefaultClient()) {

            // 设置连接超时为10秒，减少对慢主机的等待时间
            long connectTimeout = 10000; // 10秒

            // 使用正确的字符串常量设置连接超时
            sshClient.getProperties().put("ssh.connectTimeout", String.valueOf(connectTimeout));
            sshClient.setServerKeyVerifier(AcceptAllServerKeyVerifier.INSTANCE);

            sshClient.start();
            try {
                session = sshClient.connect(sshUser, sshIp, sshPort).verify().getClientSession();
                session.addPasswordIdentity(sshPassword);
                if (session.auth().verify().isFailure()) {
                    String errorMsg = "用户名或密码验证失败";
                    LOG.error("{}: {}", sshIp, errorMsg);
                    // 设置具体的错误信息
                    hostInfo.setSshErrorMsg(errorMsg);
                    hostInfo.setErrorMessage("SSH连接失败: " + errorMsg);
                    return null;
                }
            } catch (IOException e) {
                String errorMsg = "密码连接失败: " + e.getMessage();
                LOG.error("{}: {}", sshIp, errorMsg, e);
                // 设置具体的错误信息
                hostInfo.setSshErrorMsg(errorMsg);
                hostInfo.setErrorMessage("SSH连接失败: " + e.getMessage());
                // 保存异常类型信息
                if (e.getMessage() != null) {
                    if (e.getMessage().contains("Auth fail") || e.getMessage().contains("authentication failed")) {
                        hostInfo.setSshErrorMsg("用户名或密码错误: " + e.getMessage());
                    } else if (e.getMessage().contains("Connection refused")) {
                        hostInfo.setSshErrorMsg("SSH服务未启动或端口未开放: " + e.getMessage());
                    } else if (e.getMessage().contains("connect timed out")) {
                        hostInfo.setSshErrorMsg("连接超时，网络不通或防火墙阻止: " + e.getMessage());
                    } else if (e.getMessage().contains("UnknownHostException")) {
                        hostInfo.setSshErrorMsg("无法解析主机名: " + e.getMessage());
                    } else if (e.getMessage().contains("No route to host")) {
                        hostInfo.setSshErrorMsg("无法访问主机: " + e.getMessage());
                    }
                }
                return null;
            } catch (Exception e) {
                String errorMsg = "连接异常: " + e.getMessage();
                LOG.error("{}: {}", sshIp, errorMsg, e);
                // 设置具体的错误信息
                hostInfo.setSshErrorMsg(errorMsg);
                hostInfo.setErrorMessage("SSH连接失败: " + e.getMessage());
                // 保存完整的异常堆栈信息
                if (e.getStackTrace() != null && e.getStackTrace().length > 0) {
                    StringBuilder stackInfo = new StringBuilder();
                    for (int i = 0; i < Math.min(3, e.getStackTrace().length); i++) {
                        stackInfo.append(e.getStackTrace()[i].toString()).append("\n");
                    }
                    hostInfo.setOsErrorMsg("连接异常堆栈: " + stackInfo);
                }
                return null;
            }
        }
        LOG.info("{} 密码连接成功", sshIp);
        return session;
    }

    /**
     * 检测Linux发行版类型
     * 
     * @param session SSH会话
     * @return 发行版信息，如"CentOS 7"、"Ubuntu 20.04"等
     */
    public static String detectLinuxDistro(ClientSession session) {
        // 尝试多种方法检测Linux发行版
        String[] commands = {
                "cat /etc/os-release | grep -E '^ID=' | cut -d= -f2",
                "lsb_release -i | cut -f2",
                "cat /etc/redhat-release",
                "cat /etc/issue | head -1"
        };

        for (String cmd : commands) {
            CommandResult result = execCmdWithResultObject(session, cmd);
            if (result.isSuccess() && !result.output().isEmpty()) {
                return result.output().trim();
            }
        }

        // 如果无法检测，默认返回centos
        return "centos";
    }

    /**
     * 根据Linux发行版调整命令
     * 
     * @param session SSH会话
     * @param command 原始命令
     * @return 调整后的命令
     */
    public static String adaptCommandToDistro(ClientSession session, String command) {
        // 检测系统类型
        String distro = detectLinuxDistro(session).toLowerCase();
        String adaptedCommand = command;

        // 如果是服务管理命令，根据发行版调整
        if (command.contains("service")
                && (command.contains("start") || command.contains("stop") || command.contains("restart"))) {
            if (distro.contains("ubuntu") || distro.contains("debian")) {
                // 对于Debian/Ubuntu, 先尝试systemctl, 如果不存在再用service命令
                adaptedCommand = command.replace("service", "systemctl");
            }
        } else if (command.contains("chkconfig")) {
            if (distro.contains("ubuntu") || distro.contains("debian")) {
                // 替换chkconfig命令
                if (command.contains("--add")) {
                    adaptedCommand = command.replace("chkconfig --add", "update-rc.d") + " defaults";
                } else if (command.contains("on")) {
                    adaptedCommand = command.replace("chkconfig", "update-rc.d");
                    adaptedCommand = adaptedCommand.replace("on", "enable");
                } else if (command.contains("off")) {
                    adaptedCommand = command.replace("chkconfig", "update-rc.d");
                    adaptedCommand = adaptedCommand.replace("off", "disable");
                }
            } else if (distro.contains("centos") || distro.contains("redhat")) {
                // 检查目标目录是否存在
                String checkDir = "[ -d /etc/rc.d/init.d/ ] && echo 'exists' || echo 'not exists'";
                CommandResult dirCheckResult = execCmdWithResultObject(session, checkDir);

                if (!dirCheckResult.isSuccess() || "not exists".equals(dirCheckResult.output().trim())) {
                    // 如果目录不存在，调整为systemctl命令
                    if (command.contains("on")) {
                        adaptedCommand = command.replace("chkconfig", "systemctl enable");
                        adaptedCommand = adaptedCommand.replace("on", "");
                    } else if (command.contains("off")) {
                        adaptedCommand = command.replace("chkconfig", "systemctl disable");
                        adaptedCommand = adaptedCommand.replace("off", "");
                    }
                }
            }
        }

        return adaptedCommand;
    }

    /**
     * 安全执行命令，自动适应不同Linux发行版
     * 
     * @param session SSH会话
     * @param command 要执行的命令
     * @return 命令执行结果
     */
    public static String safeExecCommand(ClientSession session, String command) {
        // 记录原始命令
        LOG.info("执行命令: {}", command);

        // 首先获取调整后的命令
        String adaptedCommand = adaptCommandToDistro(session, command);
        if (!adaptedCommand.equals(command)) {
            LOG.info("已根据系统类型调整命令: {}", adaptedCommand);
        }

        // 执行调整后的命令
        CommandResult result = execCmdWithResultObject(session, adaptedCommand);

        // 如果是启用服务失败，尝试修复LSB头信息后再重试
        if (!result.isSuccess() && adaptedCommand.contains("update-rc.d") && adaptedCommand.contains("enable")) {
            String scriptPath = adaptedCommand.replace("update-rc.d", "").replace("enable", "").trim();
            LOG.warn("启用服务失败，尝试添加LSB头信息: {}", scriptPath);

            String fixCmd = "sed -i '1i#!/bin/sh\\n### BEGIN INIT INFO\\n# Provides: " + scriptPath
                    + "\\n# Required-Start: $network $local_fs $remote_fs\\n# Required-Stop: "
                    + "$network $local_fs $remote_fs\\n# Default-Start: 2 3 4 5\\n# Default-Stop: 0 1 6\\n"
                    + "# Short-Description: Datasophon Service\\n# Description: Datasophon Service\\n"
                    + "### END INIT INFO\\n' "
                    + scriptPath;

            execCmdWithResultObject(session, fixCmd);
            LOG.info("已添加LSB头信息，重试启用服务...");

            // 重试启用服务
            return execCmdWithResult(session, adaptedCommand);
        }

        // 如果执行失败，尝试添加sudo再次执行
        if (!result.isSuccess() && !adaptedCommand.startsWith("sudo")) {
            LOG.warn("命令执行失败，尝试使用sudo: {}", adaptedCommand);
            CommandResult sudoResult = execCmdWithResultObject(session, "sudo " + adaptedCommand);
            return sudoResult.isSuccess() ? sudoResult.output()
                    : "EXIT_CODE_" + sudoResult.exitCode() + ": " + sudoResult.error();
        }

        return result.isSuccess() ? result.output() : "EXIT_CODE_" + result.exitCode() + ": " + result.error();
    }

    /**
     * 为Ubuntu/Debian系统创建systemd服务单元文件
     * 
     * @param session     SSH会话
     * @param scriptPath  脚本路径
     * @param installPath 安装路径
     * @return 是否创建成功
     */
    public static boolean createSystemdServiceForDebian(ClientSession session, String scriptPath, String installPath) {
        LOG.info("为Ubuntu/Debian创建systemd服务单元文件");

        // 1. 检查脚本是否存在
        String checkScript = "[ -f " + scriptPath + " ] && echo 'exists' || echo 'not exists'";
        CommandResult scriptExistsResult = execCmdWithResultObject(session, checkScript);
        if (!scriptExistsResult.isSuccess() || !"exists".equals(scriptExistsResult.output().trim())) {
            LOG.error("找不到启动脚本: {}", scriptPath);
            return false;
        }

        // 2. 确保脚本有执行权限
        String chmodCmd = "chmod 755 " + scriptPath;
        execCmdWithResultObject(session, chmodCmd);

        // 3. 为systemd创建服务文件
        String systemdDir = "/etc/systemd/system";
        String checkSystemd = "[ -d " + systemdDir + " ] && echo 'exists' || echo 'not exists'";
        CommandResult systemdExistsResult = execCmdWithResultObject(session, checkSystemd);

        if (systemdExistsResult.isSuccess() && "exists".equals(systemdExistsResult.output().trim())) {
            LOG.info("创建systemd服务单元文件");

            // 创建服务单元文件内容
            String serviceContent = "[Unit]\n"
                    + "Description=Datasophon Worker Service\n"
                    + "After=network.target\n\n"
                    + "[Service]\n"
                    + "Type=forking\n"
                    + "Environment=\"JAVA_HOME=" + installPath + "/datasophon-worker/jdk/current\"\n"
                    + "ExecStart=" + scriptPath + " start worker\n"
                    + "ExecStop=" + scriptPath + " stop worker\n"
                    + "ExecReload=" + scriptPath + " restart worker\n"
                    + "KillMode=process\n"
                    + "Restart=on-failure\n\n"
                    + "[Install]\n"
                    + "WantedBy=multi-user.target\n";

            // 将内容写入临时文件
            String tempFile = "/tmp/datasophon-worker.service." + System.currentTimeMillis();
            try (FileWriter fw = new FileWriter(tempFile)) {
                fw.write(serviceContent);
            } catch (IOException e) {
                LOG.error("创建临时服务文件失败", e);
                return false;
            }

            // 上传服务文件
            boolean uploadResult = uploadFile(session, "/tmp/", tempFile);
            if (!uploadResult) {
                LOG.error("上传服务文件失败");
                return false;
            }

            // 移动到系统目录
            String moveCmd = "sudo mv /tmp/" + new File(tempFile).getName() + " " + systemdDir
                    + "/datasophon-worker.service";
            execCmdWithResultObject(session, moveCmd);

            // 重新加载systemd
            execCmdWithResultObject(session, "sudo systemctl daemon-reload");
            execCmdWithResultObject(session, "sudo systemctl enable datasophon-worker.service");

            new File(tempFile).delete();
            LOG.info("已创建systemd服务单元文件");
            return true;
        } else {
            LOG.warn("systemd目录不存在，无法创建服务单元文件");
            return false;
        }
    }

    /**
     * 同步执行命令并获取结果，兼容旧版接口
     *
     * @param session 连接会话
     * @param command 要执行的命令
     * @return 结果字符串，对于失败的命令会在结果前加上"EXIT_CODE_XXX:"前缀
     */
    public static String execCmdWithResult(ClientSession session, String command) {
        CommandResult result = execCmdWithResultObject(session, command);
        return result.isSuccess() ? result.output() : "EXIT_CODE_" + result.exitCode() + ": " + result.error();
    }

    /**
     * 执行命令
     * 
     * @param session SSH会话
     * @param command 要执行的命令
     * @return 命令执行结果
     */
    public static CommandResult execCommand(ClientSession session, String command) {
        if (session == null) {
            LOG.error("会话为空，无法执行命令");
            return new CommandResult(command, -1, "", "SSH会话为空");
        }

        if (!session.isOpen()) {
            LOG.error("会话已关闭，无法执行命令");
            return new CommandResult("", -1, "", "SSH会话已关闭");
        }

        // 获取当前线程名称，用于日志
        String currentThreadName = Thread.currentThread().getName();
        String hostAddress;
        try {
            // 尝试从会话中提取远程地址信息
            hostAddress = session.getIoSession().getRemoteAddress().toString();
            // 简化地址信息，通常是/IP:端口格式
            if (hostAddress.startsWith("/")) {
                hostAddress = hostAddress.substring(1);
            }
            if (hostAddress.contains(":")) {
                hostAddress = hostAddress.substring(0, hostAddress.indexOf(":"));
            }
        } catch (Exception e) {
            // 忽略异常，使用默认值
            hostAddress = "unknown";
        }

        LOG.info("执行命令: {}, 主机: {}, 线程: {}", command, hostAddress, currentThreadName);

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ByteArrayOutputStream errorStream = new ByteArrayOutputStream();

            ChannelExec channel = session.createExecChannel(command);
            channel.setOut(outputStream);
            channel.setErr(errorStream);
            channel.open().verify(30000); // 30秒超时

            Set<ClientChannelEvent> events = channel.waitFor(EnumSet.of(ClientChannelEvent.CLOSED), 30000); // 30秒超时

            // 检查是否在超时时间内关闭
            if (!events.contains(ClientChannelEvent.CLOSED)) {
                LOG.warn("命令执行超时，强制关闭通道: {}", command);
                channel.close(true);
            }

            int exitStatus = channel.getExitStatus();
            String output = outputStream.toString(StandardCharsets.UTF_8);
            String error = errorStream.toString(StandardCharsets.UTF_8);

            if (exitStatus != 0) {
                LOG.error("命令执行失败 [exit={}]: {}\n错误信息: {}", exitStatus, command, error);
                return new CommandResult("", exitStatus, output, error);
            }

            return new CommandResult("", exitStatus, output, error);
        } catch (Exception e) {
            LOG.error("执行命令时异常 {}: {}", command, e.getMessage());
            return new CommandResult("", -1, "", "执行异常: " + e.getMessage());
        }
    }

    /**
     * 检查SSH连接是否有效
     *
     * @param session 会话连接
     * @return 是否有效
     */
    public static boolean isSessionValid(ClientSession session) {
        if (session == null) {
            return false;
        }
        try {
            // 使用简单的命令来验证连接可用性，设置3秒超时
            CommandResult result = execCmdWithResultObject(session, "echo 'connection_test'", 3);
            return result.isSuccess() && "connection_test".equals(result.output().trim());
        } catch (Exception e) {
            LOG.warn("SSH连接验证失败: {}", e.getMessage());
            return false;
        }
    }
}