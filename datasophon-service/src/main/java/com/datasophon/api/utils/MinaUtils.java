package com.datasophon.api.utils;

import com.datasophon.common.Constants;
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
import java.io.OutputStream;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.nio.file.attribute.PosixFilePermission;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class MinaUtils {
    
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(MinaUtils.class);
    
    /** 打开远程会话 */
    public static ClientSession openConnection(String sshHost, Integer sshPort, String sshUser) {
        SshClient sshClient = SshClient.setUpDefaultClient();
        
        // 配置自动接受未知主机密钥
        sshClient.setServerKeyVerifier(AcceptAllServerKeyVerifier.INSTANCE);
        
        sshClient.start();
        ClientSession session = null;
        
        // 获取两种格式的私钥路径
        String privateKeyPathRSA = System.getProperty("user.home") + "/.ssh/id_rsa";
        String privateKeyPathED25519 = System.getProperty("user.home") + "/.ssh/id_ed25519";
        
        try {
            // 创建会话连接
            session = sshClient.connect(sshUser, sshHost, sshPort).verify().getClientSession();
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
                    LOG.warn("ED25519密钥加载失败: " + e.getMessage());
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
                    LOG.warn("RSA密钥加载失败: " + e.getMessage());
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
            LOG.error("免密登录失败: " + e.getMessage());
            return null;
        } catch (Exception e) {
            LOG.error("连接异常: " + e.getMessage());
            return null;
        }
        
        LOG.info(sshHost + " 连接成功");
        return session;
    }
    
    /** 关闭远程会话 */
    public static void closeConnection(ClientSession session) {
        try {
            session.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
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
     * @return 结果
     */
    public static String execCmdWithResult(ClientSession session, String command) {
        if (session == null) {
            LOG.error("SSH会话为空，无法执行命令: {}", command);
            return null;
        }
        session.resetAuthTimeout();
        LOG.info("exe cmd: {}", command);
        // 命令返回的结果
        ChannelExec ce = null;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        try {
            ce = session.createExecChannel(command);
            ce.setOut(out);
            ce.setErr(err);
            // 打开通道并执行命令
            ce.open();

            // 等待命令执行完成或超时
            Set<ClientChannelEvent> events = ce.waitFor(EnumSet.of(ClientChannelEvent.CLOSED), TimeUnit.SECONDS.toMillis(100000));

            if (events.contains(ClientChannelEvent.TIMEOUT)) {
                LOG.error("命令执行超时: {}", command);
                return "ERROR: Command timed out";
            }

            int exitStatus = ce.getExitStatus();
            LOG.info("mina result {}", exitStatus);
            
            String outResult = out.toString();
            String errResult = err.toString();
            
            if (exitStatus != 0) {
                // 处理常见的错误并尝试替代解决方案
                if (command.contains("chkconfig") && exitStatus == 127) {
                    LOG.warn("chkconfig命令不存在，尝试使用systemctl替代...");
                    String serviceName = command.substring(command.lastIndexOf(" ") + 1);
                    return execCmdWithResult(session, "systemctl enable " + serviceName);
                } 
                else if (command.contains("\\cp") && exitStatus == 1) {
                    LOG.warn("复制文件失败，尝试使用sudo...");
                    return execCmdWithResult(session, "sudo " + command);
                }
                else if (command.contains("service") && command.contains("restart")) {
                    LOG.warn("service命令启动服务失败，尝试使用systemctl...");
                    String serviceName = command.substring(command.indexOf("service ") + 8, command.lastIndexOf(" "));
                    LOG.info("尝试使用systemctl重启服务: {}", serviceName);
                    return execCmdWithResult(session, "systemctl restart " + serviceName);
                }
                
                if (!errResult.isEmpty()) {
                    LOG.error("命令执行失败: {} - 错误信息: {}", command, errResult);
                    return "ERROR: " + errResult;
                }
            }

            LOG.info("exe cmd return : {}", outResult);
            return outResult;
        } catch (IOException e) {
            LOG.error("执行命令异常: {} - {}", command, e.getMessage());
            return "ERROR: " + e.getMessage();
        } finally {
                try {
                if (ce != null) {
                    ce.close();
                }
            } catch (IOException e) {
                LOG.error("关闭命令通道异常", e);
            }
        }
    }
    public static String executeCommandAndGetResult(ClientSession session, String command) throws IOException {
        session.resetAuthTimeout();
        LOG.info("Executing command: {}", command);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
             ChannelExec channelExec = session.createExecChannel(command)) {

            channelExec.setOut(outputStream);
            channelExec.setErr(errorStream);

            // 打开通道并执行命令
            channelExec.open();

            // 等待命令执行完成或超时
            Set<ClientChannelEvent> events = channelExec.waitFor(EnumSet.of(ClientChannelEvent.CLOSED), TimeUnit.SECONDS.toMillis(100000));

            if (events.contains(ClientChannelEvent.TIMEOUT)) {
                throw new IOException("Command execution timed out");
            }

            int exitStatus = channelExec.getExitStatus();
            LOG.info("Command executed with exit status: {}", exitStatus);

            if (exitStatus != 0) {
                String errorOutput = errorStream.toString().trim();
                LOG.error("Command execution failed: {}", errorOutput);
                throw new IOException("Command execution failed with error: " + errorOutput);
            }

            String result = outputStream.toString().trim();
            LOG.info("Command output: {}", result);

            return result;

        } catch (Exception e) {
            LOG.error("Error executing command: {}", e.getMessage());
            throw e;
        }
    }


    /**
     * 上传文件,相同路径ui覆盖
     *
     * @param session 连接
     * @param remotePath 远程目录地址
     * @param inputFile 文件 File
     */
    public static boolean uploadFile(ClientSession session, String remotePath, String inputFile) {
        File uploadFile = new File(inputFile);
        InputStream input = null;
        SftpFileSystem sftp = null;
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
    
    /**
     * 创建目录
     *
     * @param path
     * @return
     */
    public static boolean createDir(ClientSession session, String path) {
        SftpFileSystem sftp = null;
        try {
            sftp = SftpClientFactory.instance().createSftpFileSystem(session);
            Path remoteRoot = sftp.getDefaultDir().resolve(path);
            if (!Files.exists(remoteRoot)) {
                Files.createDirectories(remoteRoot);
                return true;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return false;
    }
    
    /** 使用密码打开远程会话 */
    public static ClientSession openConnectionWithPassword(String sshHost, Integer sshPort, String sshUser, String sshPassword) {
        SshClient sshClient = SshClient.setUpDefaultClient();
        
        // 配置自动接受未知主机密钥
        sshClient.setServerKeyVerifier(AcceptAllServerKeyVerifier.INSTANCE);
        
        sshClient.start();
        ClientSession session = null;
        try {
            session = sshClient.connect(sshUser, sshHost, sshPort).verify().getClientSession();
            session.addPasswordIdentity(sshPassword);
            if (session.auth().verify().isFailure()) {
                LOG.info("密码验证失败");
                return null;
            }
        } catch (IOException e) {
            LOG.error("密码连接失败: " + e.getMessage());
            return null;
        } catch (Exception e) {
            LOG.error("连接异常: " + e.getMessage());
            return null;
        }
        LOG.info(sshHost + " 密码连接成功");
        return session;
    }

    /** 设置免密登录 */
    public static boolean setupPasswordlessLogin(ClientSession session, String sshUser, String sshPassword) {
        try {
            // 先修复现有密钥的权限
            fixSshKeyPermissions();
            
            String homeDir = System.getProperty("user.home");
            String sshDir = homeDir + "/.ssh";
            String privateKeyPathRSA = sshDir + "/id_rsa";
            String publicKeyPathRSA = sshDir + "/id_rsa.pub";
            String privateKeyPathED25519 = sshDir + "/id_ed25519";
            String publicKeyPathED25519 = sshDir + "/id_ed25519.pub";
            
            LOG.info("SSH目录: {}", sshDir);
            LOG.info("RSA私钥路径: {}", privateKeyPathRSA);
            LOG.info("RSA公钥路径: {}", publicKeyPathRSA);
            LOG.info("ED25519私钥路径: {}", privateKeyPathED25519);
            LOG.info("ED25519公钥路径: {}", publicKeyPathED25519);

            // 创建本地.ssh目录
            File sshDirFile = new File(sshDir);
            if (!sshDirFile.exists()) {
                LOG.info("创建本地.ssh目录: {}", sshDir);
                sshDirFile.mkdirs();
                execCmdWithResult(session, "chmod 700 " + sshDir);
            }

            // 生成RSA密钥对(如果不存在)
            File privateKeyFileRSA = new File(privateKeyPathRSA);
            File publicKeyFileRSA = new File(publicKeyPathRSA);
            
            boolean rsaGenerated = false;
            if (!privateKeyFileRSA.exists() || !publicKeyFileRSA.exists()) {
                LOG.info("生成RSA格式SSH密钥对");
                
                // 使用系统ssh-keygen命令生成RSA密钥对
                try {
                    // 确保.ssh目录存在
                    if (!sshDirFile.exists()) {
                        sshDirFile.mkdirs();
                    }
                    
                    // 执行ssh-keygen命令生成RSA密钥
                    Process process = Runtime.getRuntime().exec(
                        new String[] { 
                            "ssh-keygen", 
                            "-t", "rsa", 
                            "-b", "2048", 
                            "-f", privateKeyPathRSA, 
                            "-N", "" // 空密码
                        }
                    );
                    
                    int exitCode = process.waitFor();
                    if (exitCode != 0) {
                        LOG.error("RSA密钥生成失败，退出码: {}", exitCode);
                        // 读取错误输出
                        try (BufferedReader reader = new BufferedReader(
                                new InputStreamReader(process.getErrorStream()))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                LOG.error("ssh-keygen错误: {}", line);
                            }
                        }
                    } else {
                        LOG.info("RSA密钥生成成功");
                        rsaGenerated = true;
                    }
                } catch (Exception e) {
                    LOG.error("生成RSA密钥对失败", e);
                }
            } else {
                LOG.info("RSA密钥对已存在");
                rsaGenerated = true;
            }
            
            // 生成ED25519密钥对(如果不存在)
            File privateKeyFileED25519 = new File(privateKeyPathED25519);
            File publicKeyFileED25519 = new File(publicKeyPathED25519);
            
            boolean ed25519Generated = false;
            if (!privateKeyFileED25519.exists() || !publicKeyFileED25519.exists()) {
                LOG.info("生成ED25519格式SSH密钥对");
                
                // 使用系统ssh-keygen命令生成ED25519密钥对
                try {
                    // 确保.ssh目录存在
                    if (!sshDirFile.exists()) {
                        sshDirFile.mkdirs();
                    }
                    
                    // 执行ssh-keygen命令生成ED25519密钥
                    Process process = Runtime.getRuntime().exec(
                        new String[] { 
                            "ssh-keygen", 
                            "-t", "ed25519", 
                            "-f", privateKeyPathED25519, 
                            "-N", "" // 空密码
                        }
                    );
                    
                    int exitCode = process.waitFor();
                    if (exitCode != 0) {
                        LOG.error("ED25519密钥生成失败，退出码: {}", exitCode);
                        // 读取错误输出
                        try (BufferedReader reader = new BufferedReader(
                                new InputStreamReader(process.getErrorStream()))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                LOG.error("ssh-keygen错误: {}", line);
                            }
                        }
                    } else {
                        LOG.info("ED25519密钥生成成功");
                        ed25519Generated = true;
                    }
                } catch (Exception e) {
                    LOG.error("生成ED25519密钥对失败", e);
                }
            } else {
                LOG.info("ED25519密钥对已存在");
                ed25519Generated = true;
            }
            
            // 如果两种密钥都生成失败，则返回失败
            if (!rsaGenerated && !ed25519Generated) {
                LOG.error("RSA和ED25519密钥都生成失败");
                return false;
            }
            
            // 设置密钥文件权限
            try {
                // 设置.ssh目录权限
                Set<PosixFilePermission> sshDirPermissions = new HashSet<>();
                sshDirPermissions.add(PosixFilePermission.OWNER_READ);
                sshDirPermissions.add(PosixFilePermission.OWNER_WRITE);
                sshDirPermissions.add(PosixFilePermission.OWNER_EXECUTE);
                Files.setPosixFilePermissions(sshDirFile.toPath(), sshDirPermissions);
                
                // 设置RSA密钥权限（如果存在）
                if (privateKeyFileRSA.exists()) {
                    Set<PosixFilePermission> privateKeyPermissions = new HashSet<>();
                    privateKeyPermissions.add(PosixFilePermission.OWNER_READ);
                    privateKeyPermissions.add(PosixFilePermission.OWNER_WRITE);
                    Files.setPosixFilePermissions(privateKeyFileRSA.toPath(), privateKeyPermissions);
                }
                
                if (publicKeyFileRSA.exists()) {
                    Set<PosixFilePermission> publicKeyPermissions = new HashSet<>();
                    publicKeyPermissions.add(PosixFilePermission.OWNER_READ);
                    publicKeyPermissions.add(PosixFilePermission.OWNER_WRITE);
                    publicKeyPermissions.add(PosixFilePermission.GROUP_READ);
                    publicKeyPermissions.add(PosixFilePermission.OTHERS_READ);
                    Files.setPosixFilePermissions(publicKeyFileRSA.toPath(), publicKeyPermissions);
                }
                
                // 设置ED25519密钥权限（如果存在）
                if (privateKeyFileED25519.exists()) {
                    Set<PosixFilePermission> privateKeyPermissions = new HashSet<>();
                    privateKeyPermissions.add(PosixFilePermission.OWNER_READ);
                    privateKeyPermissions.add(PosixFilePermission.OWNER_WRITE);
                    Files.setPosixFilePermissions(privateKeyFileED25519.toPath(), privateKeyPermissions);
                }
                
                if (publicKeyFileED25519.exists()) {
                    Set<PosixFilePermission> publicKeyPermissions = new HashSet<>();
                    publicKeyPermissions.add(PosixFilePermission.OWNER_READ);
                    publicKeyPermissions.add(PosixFilePermission.OWNER_WRITE);
                    publicKeyPermissions.add(PosixFilePermission.GROUP_READ);
                    publicKeyPermissions.add(PosixFilePermission.OTHERS_READ);
                    Files.setPosixFilePermissions(publicKeyFileED25519.toPath(), publicKeyPermissions);
                }
                
                LOG.info("密钥对权限设置完成");
            } catch (Exception e) {
                LOG.error("设置密钥文件权限失败", e);
            }

            // 准备要上传的公钥内容
            StringBuilder publicKeys = new StringBuilder();
            
            // 读取RSA公钥
            if (publicKeyFileRSA.exists()) {
                try {
                    String rsaPublicKey = new String(Files.readAllBytes(Paths.get(publicKeyPathRSA)));
                    LOG.info("读取到的RSA公钥内容: {}", rsaPublicKey);
                    publicKeys.append(rsaPublicKey.trim()).append("\n");
                } catch (Exception e) {
                    LOG.error("读取RSA公钥失败", e);
                }
            }
            
            // 读取ED25519公钥
            if (publicKeyFileED25519.exists()) {
                try {
                    String ed25519PublicKey = new String(Files.readAllBytes(Paths.get(publicKeyPathED25519)));
                    LOG.info("读取到的ED25519公钥内容: {}", ed25519PublicKey);
                    publicKeys.append(ed25519PublicKey.trim()).append("\n");
                } catch (Exception e) {
                    LOG.error("读取ED25519公钥失败", e);
                }
            }
            
            if (publicKeys.length() == 0) {
                LOG.error("没有可用的公钥内容");
                return false;
            }

            // 将公钥添加到远程authorized_keys，使用更明确的步骤和权限设置
            LOG.info("开始配置远程服务器SSH免密登录...");
            LOG.info("将上传RSA和ED25519两种格式的公钥");
            
            // 1. 确保远程.ssh目录存在且权限正确
            String result = execCmdWithResult(session, "mkdir -p ~/.ssh && chmod 700 ~/.ssh");
            LOG.info("创建远程.ssh目录结果: {}", result);
            
            // 2. 检查远程authorized_keys文件
            result = execCmdWithResult(session, "touch ~/.ssh/authorized_keys && chmod 600 ~/.ssh/authorized_keys");
            LOG.info("创建/设置authorized_keys文件权限结果: {}", result);
            
            // 3. 清晰地将公钥写入临时文件，然后使用cat追加到authorized_keys
            String tempFile = "/tmp/temp_pubkey_" + System.currentTimeMillis() + ".pub";
            try (FileWriter fw = new FileWriter(tempFile)) {
                fw.write(publicKeys.toString());
            }
            
            // 4. 上传临时公钥文件到远程服务器
            boolean uploadSuccess = uploadFile(session, "/tmp/", tempFile);
            LOG.info("上传公钥文件结果: {}", uploadSuccess);
            
            if (!uploadSuccess) {
                LOG.error("上传公钥文件失败");
                return false;
            }
            
            // 5. 将公钥添加到authorized_keys，并确保不重复添加
            String remoteKeyPath = "/tmp/" + new File(tempFile).getName();
            String addKeyCmd = String.format(
                "cat %s >> ~/.ssh/authorized_keys && sort -u ~/.ssh/authorized_keys -o ~/.ssh/authorized_keys",
                remoteKeyPath
            );
            result = execCmdWithResult(session, addKeyCmd);
            LOG.info("添加公钥到authorized_keys结果: {}", result);
            
            // 6. 清理临时文件
            execCmdWithResult(session, "rm -f " + remoteKeyPath);
            new File(tempFile).delete();
            
            // 7. 检查远程SSH配置并尝试修复常见问题
            // 检查SSH配置是否启用公钥认证
            result = execCmdWithResult(session, "grep -E '^PubkeyAuthentication\\s+yes' /etc/ssh/sshd_config");
            if (result == null || result.isEmpty()) {
                LOG.warn("远程SSH服务器可能未明确启用公钥认证，尝试添加配置");
                // 尝试添加配置
                String backupCmd = "cp /etc/ssh/sshd_config /etc/ssh/sshd_config.bak_$(date +%Y%m%d%H%M%S)";
                execCmdWithResult(session, backupCmd);
                
                // 添加或修改PubkeyAuthentication设置
                String fixCmd = "grep -q '^PubkeyAuthentication' /etc/ssh/sshd_config && " +
                               "sed -i 's/^PubkeyAuthentication.*/PubkeyAuthentication yes/' /etc/ssh/sshd_config || " +
                               "echo 'PubkeyAuthentication yes' >> /etc/ssh/sshd_config";
                execCmdWithResult(session, fixCmd);
                
                // 确保支持Ed25519密钥
                String checkEd25519Cmd = "grep -q '^HostKey.*ssh_host_ed25519_key' /etc/ssh/sshd_config";
                result = execCmdWithResult(session, checkEd25519Cmd);
                
                if (result == null || result.isEmpty()) {
                    LOG.warn("远程SSH服务器可能未启用Ed25519密钥支持，尝试添加配置");
                    String fixEd25519Cmd = "echo 'HostKey /etc/ssh/ssh_host_ed25519_key' >> /etc/ssh/sshd_config";
                    execCmdWithResult(session, fixEd25519Cmd);
                }
                
                // 重启SSH服务
                LOG.info("尝试重启SSH服务以应用新配置");
                String restartCmd = "systemctl restart sshd || service sshd restart || service ssh restart";
                execCmdWithResult(session, restartCmd);
                
                LOG.info("SSH配置已更新，请在服务器重启后再次尝试免密登录");
            }
            
            // 检查是否禁用密码认证(可选，通常不推荐自动设置)
            result = execCmdWithResult(session, "grep -E '^PasswordAuthentication\\s+no' /etc/ssh/sshd_config");
            if (result != null && !result.isEmpty()) {
                LOG.warn("远程SSH服务器已禁用密码认证，请确保公钥认证正常工作");
            }
            
            LOG.info("SSH免密登录配置完成");

            return true;
        } catch (Exception e) {
            LOG.error("设置免密登录失败", e);
            return false;
        }
    }
    
    // 用于SSH密钥格式的辅助方法
    private static void writeInt(OutputStream out, int v) throws IOException {
        byte[] tmp = new byte[4];
        tmp[0] = (byte) ((v >>> 24) & 0xff);
        tmp[1] = (byte) ((v >>> 16) & 0xff);
        tmp[2] = (byte) ((v >>> 8) & 0xff);
        tmp[3] = (byte) (v & 0xff);
        out.write(tmp);
    }
    
    /**
     * 修复SSH密钥文件权限
     * 将私钥权限设为0600，公钥权限设为0644，.ssh目录权限设为0700
     * @return 是否修复成功
     */
    public static boolean fixSshKeyPermissions() {
        try {
            String homeDir = System.getProperty("user.home");
            String sshDir = homeDir + "/.ssh";
            String privateKeyPathRSA = sshDir + "/id_rsa";
            String publicKeyPathRSA = sshDir + "/id_rsa.pub";
            String privateKeyPathED25519 = sshDir + "/id_ed25519";
            String publicKeyPathED25519 = sshDir + "/id_ed25519.pub";
            
            LOG.info("正在修复SSH密钥权限");
            LOG.info("SSH目录: {}", sshDir);
            LOG.info("RSA私钥路径: {}", privateKeyPathRSA);
            LOG.info("RSA公钥路径: {}", publicKeyPathRSA);
            LOG.info("ED25519私钥路径: {}", privateKeyPathED25519);
            LOG.info("ED25519公钥路径: {}", publicKeyPathED25519);

            File sshDirFile = new File(sshDir);
            File privateKeyFileRSA = new File(privateKeyPathRSA);
            File publicKeyFileRSA = new File(publicKeyPathRSA);
            File privateKeyFileED25519 = new File(privateKeyPathED25519);
            File publicKeyFileED25519 = new File(publicKeyPathED25519);
            
            boolean success = false;
            
            if (!sshDirFile.exists()) {
                LOG.warn("SSH目录不存在: {}", sshDir);
                // 创建SSH目录
                sshDirFile.mkdirs();
                success = true;
            }
            
            // 设置.ssh目录权限
            Set<PosixFilePermission> sshDirPermissions = new HashSet<>();
            sshDirPermissions.add(PosixFilePermission.OWNER_READ);
            sshDirPermissions.add(PosixFilePermission.OWNER_WRITE);
            sshDirPermissions.add(PosixFilePermission.OWNER_EXECUTE);
            Files.setPosixFilePermissions(sshDirFile.toPath(), sshDirPermissions);
            LOG.info("已设置SSH目录权限为0700");
            
            // 设置RSA私钥文件权限
            if (privateKeyFileRSA.exists()) {
                Set<PosixFilePermission> privateKeyPermissions = new HashSet<>();
                privateKeyPermissions.add(PosixFilePermission.OWNER_READ);
                privateKeyPermissions.add(PosixFilePermission.OWNER_WRITE);
                Files.setPosixFilePermissions(privateKeyFileRSA.toPath(), privateKeyPermissions);
                LOG.info("已设置RSA私钥文件权限为0600");
                success = true;
            } else {
                LOG.warn("RSA私钥文件不存在: {}", privateKeyPathRSA);
            }
            
            // 设置RSA公钥文件权限
            if (publicKeyFileRSA.exists()) {
                Set<PosixFilePermission> publicKeyPermissions = new HashSet<>();
                publicKeyPermissions.add(PosixFilePermission.OWNER_READ);
                publicKeyPermissions.add(PosixFilePermission.OWNER_WRITE);
                publicKeyPermissions.add(PosixFilePermission.GROUP_READ);
                publicKeyPermissions.add(PosixFilePermission.OTHERS_READ);
                Files.setPosixFilePermissions(publicKeyFileRSA.toPath(), publicKeyPermissions);
                LOG.info("已设置RSA公钥文件权限为0644");
                success = true;
            } else {
                LOG.warn("RSA公钥文件不存在: {}", publicKeyPathRSA);
            }
            
            // 设置ED25519私钥文件权限
            if (privateKeyFileED25519.exists()) {
                Set<PosixFilePermission> privateKeyPermissions = new HashSet<>();
                privateKeyPermissions.add(PosixFilePermission.OWNER_READ);
                privateKeyPermissions.add(PosixFilePermission.OWNER_WRITE);
                Files.setPosixFilePermissions(privateKeyFileED25519.toPath(), privateKeyPermissions);
                LOG.info("已设置ED25519私钥文件权限为0600");
                success = true;
            } else {
                LOG.warn("ED25519私钥文件不存在: {}", privateKeyPathED25519);
            }
            
            // 设置ED25519公钥文件权限
            if (publicKeyFileED25519.exists()) {
                Set<PosixFilePermission> publicKeyPermissions = new HashSet<>();
                publicKeyPermissions.add(PosixFilePermission.OWNER_READ);
                publicKeyPermissions.add(PosixFilePermission.OWNER_WRITE);
                publicKeyPermissions.add(PosixFilePermission.GROUP_READ);
                publicKeyPermissions.add(PosixFilePermission.OTHERS_READ);
                Files.setPosixFilePermissions(publicKeyFileED25519.toPath(), publicKeyPermissions);
                LOG.info("已设置ED25519公钥文件权限为0644");
                success = true;
            } else {
                LOG.warn("ED25519公钥文件不存在: {}", publicKeyPathED25519);
            }
            
            return success;
        } catch (Exception e) {
            LOG.error("修复SSH密钥权限失败", e);
            return false;
        }
    }
    
    /**
     * 检测Linux发行版类型
     * @param session SSH会话
     * @return 发行版信息，如"CentOS 7"、"Ubuntu 20.04"等
     */
    public static String detectLinuxDistro(ClientSession session) {
        // 尝试多种方法检测发行版
        String[] commands = {
            "cat /etc/os-release | grep -E '^(NAME|VERSION_ID)' | tr '\\n' ' '",
            "cat /etc/redhat-release",
            "cat /etc/issue | head -n 1",
            "uname -a"
        };
        
        for (String cmd : commands) {
            String result = execCmdWithResult(session, cmd);
            if (result != null && !result.startsWith("ERROR:") && !result.isEmpty()) {
                return result.trim();
            }
        }
        
        return "Unknown Linux";
    }
    
    /**
     * 根据Linux发行版调整命令
     * @param session SSH会话
     * @param command 原始命令
     * @return 调整后的命令
     */
    public static String adaptCommandToDistro(ClientSession session, String command) {
        // 缓存的发行版信息
        String distroInfo = detectLinuxDistro(session);
        LOG.info("检测到Linux发行版: {}", distroInfo);
        
        // 根据发行版调整命令
        if (command.startsWith("chkconfig")) {
            if (distroInfo.toLowerCase().contains("ubuntu") || 
                distroInfo.toLowerCase().contains("debian") ||
                distroInfo.toLowerCase().contains("centos 8") ||
                distroInfo.toLowerCase().contains("rhel 8")) {
                
                // 替换为systemctl命令
                if (command.contains("--add")) {
                    String serviceName = command.substring(command.lastIndexOf(" ") + 1);
                    return "systemctl enable " + serviceName;
                }
            }
        } 
        else if (command.startsWith("service")) {
            if (distroInfo.toLowerCase().contains("ubuntu") || 
                distroInfo.toLowerCase().contains("debian") ||
                distroInfo.toLowerCase().contains("centos 8") ||
                distroInfo.toLowerCase().contains("rhel 8")) {
                
                // 替换为systemctl命令
                String[] parts = command.split(" ");
                if (parts.length >= 3) {
                    String serviceName = parts[1];
                    String action = parts[2];
                    return "systemctl " + action + " " + serviceName;
                }
            }
        }
        
        // 默认返回原命令
        return command;
    }
    
    /**
     * 安全执行命令，自动适应不同Linux发行版
     * @param session SSH会话
     * @param command 要执行的命令
     * @return 命令执行结果
     */
    public static String safeExecCommand(ClientSession session, String command) {
        // 首先执行路径替换
        String modifiedCommand = command;
        
        // 替换rc.d路径 (Ubuntu使用/etc/init.d，CentOS使用/etc/rc.d/init.d)
        if (command.contains("/etc/rc.d/init.d/")) {
            // 检查目标目录是否存在
            String checkDir = "[ -d /etc/rc.d/init.d/ ] && echo 'exists' || echo 'not exists'";
            String dirCheck = execCmdWithResult(session, checkDir);
            
            if ("not exists".equals(dirCheck.trim())) {
                LOG.warn("/etc/rc.d/init.d/ 目录不存在，尝试使用 /etc/init.d/");
                modifiedCommand = command.replace("/etc/rc.d/init.d/", "/etc/init.d/");
            }
        }
        
        // 处理chmod命令特殊情况，确保应用了正确的路径
        if (modifiedCommand.contains("chmod") && modifiedCommand.contains("datasophon-worker")) {
            if (modifiedCommand.contains("/etc/rc.d/init.d/") && !command.equals(modifiedCommand)) {
                // 如果已经做了路径替换，确保chmod命令也使用正确的路径
                modifiedCommand = modifiedCommand.replace("/etc/rc.d/init.d/", "/etc/init.d/");
            }
        }
        
        // 调整命令以适应不同发行版
        String adaptedCommand = adaptCommandToDistro(session, modifiedCommand);
        if (!adaptedCommand.equals(modifiedCommand)) {
            LOG.info("命令已适配: {} -> {}", modifiedCommand, adaptedCommand);
        }
        
        // 执行调整后的命令
        String result = execCmdWithResult(session, adaptedCommand);
        
        // 如果是启用服务失败，尝试修复LSB头信息后再重试
        if (result != null && result.startsWith("ERROR:") && 
            (adaptedCommand.contains("systemctl enable") || adaptedCommand.contains("chkconfig --add")) &&
            result.contains("Default-Start contains no runlevels")) {
            
            LOG.warn("服务启动脚本缺少正确的LSB头信息，尝试修复...");
            
            // 获取脚本路径
            String scriptPath = "/etc/init.d/datasophon-worker";
            if (adaptedCommand.contains("/etc/rc.d/init.d/")) {
                scriptPath = "/etc/rc.d/init.d/datasophon-worker";
            }
            
            // 添加正确的LSB头信息
            String fixCmd = "sudo sed -i '2i### BEGIN INIT INFO\\n# Provides:          datasophon-worker\\n# Required-Start:    $remote_fs $syslog\\n# Required-Stop:     $remote_fs $syslog\\n# Default-Start:     2 3 4 5\\n# Default-Stop:      0 1 6\\n# Short-Description: Datasophon Worker Service\\n# Description:       Datasophon Worker Service for Big Data Platform\\n### END INIT INFO' " + scriptPath;
            
            execCmdWithResult(session, fixCmd);
            LOG.info("已添加LSB头信息，重试启用服务...");
            
            // 重试启用服务
            return execCmdWithResult(session, adaptedCommand);
        }
        
        // 如果执行失败，尝试添加sudo再次执行
        if (result != null && result.startsWith("ERROR:") && !adaptedCommand.startsWith("sudo")) {
            LOG.warn("命令执行失败，尝试使用sudo: {}", adaptedCommand);
            return execCmdWithResult(session, "sudo " + adaptedCommand);
        }
        
        return result;
    }
    
    /**
     * 为Ubuntu/Debian系统创建systemd服务单元文件
     * @param session SSH会话
     * @param scriptPath 脚本路径
     * @param installPath 安装路径
     * @return 是否创建成功
     */
    public static boolean createSystemdServiceForDebian(ClientSession session, String scriptPath, String installPath) {
        LOG.info("为Ubuntu/Debian创建systemd服务单元文件");
        
        // 1. 检查脚本是否存在
        String checkScript = "[ -f " + scriptPath + " ] && echo 'exists' || echo 'not exists'";
        String scriptExists = execCmdWithResult(session, checkScript);
        if (!"exists".equals(scriptExists.trim())) {
            LOG.error("找不到启动脚本: {}", scriptPath);
            return false;
        }
        
        // 2. 确保脚本有执行权限
        String chmodCmd = "chmod 755 " + scriptPath;
        execCmdWithResult(session, chmodCmd);
        
        // 3. 为systemd创建服务文件
        String systemdDir = "/etc/systemd/system";
        String checkSystemd = "[ -d " + systemdDir + " ] && echo 'exists' || echo 'not exists'";
        String systemdExists = execCmdWithResult(session, checkSystemd);
        
        if ("exists".equals(systemdExists.trim())) {
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
            String moveCmd = "sudo mv /tmp/" + new File(tempFile).getName() + " " + systemdDir + "/datasophon-worker.service";
            execCmdWithResult(session, moveCmd);
            
            // 重新加载systemd
            execCmdWithResult(session, "sudo systemctl daemon-reload");
            execCmdWithResult(session, "sudo systemctl enable datasophon-worker.service");
            
            new File(tempFile).delete();
            LOG.info("已创建systemd服务单元文件");
            return true;
        } else {
            LOG.warn("systemd目录不存在，无法创建服务单元文件");
            return false;
        }
    }
    
    /**
     * 执行本地命令并返回结果
     * @param command 要执行的命令
     * @return 命令执行结果
     */
    public static String execLocalCmdWithResult(String command) {
        try {
            Process process = Runtime.getRuntime().exec(command);
            StringBuilder output = new StringBuilder();
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                return output.toString().trim();
            } else {
                LOG.error("命令执行失败，退出码: {}", exitCode);
                return "ERROR: " + exitCode;
            }
        } catch (Exception e) {
            LOG.error("执行本地命令失败: {}", e.getMessage());
            return "ERROR: " + e.getMessage();
        }
    }
    
    public static void main(String[] args) throws IOException, InterruptedException {
        ClientSession session = MinaUtils.openConnection("localhost", 22, "liuxin");
        for (int i = 0; i < Constants.TEN; i++) {
            String ls = MinaUtils.execCmdWithResult(session, "arch");
            System.out.println(ls);
        }
        // boolean dir = MinaUtils.createDir(session,"/home/shinow/test/");
        // System.out.println(dir);
        // boolean uploadFile = MinaUtils.uploadFile(session, "/Users/liuxin/opt/test",
        // "/Users/liuxin/Downloads/yarn-default.xml");
        // System.out.println(uploadFile);
    }
}