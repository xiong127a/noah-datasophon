package com.datasophon.api.utils;

import com.datasophon.common.Constants;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ChannelExec;
import org.apache.sshd.client.channel.ClientChannelEvent;
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
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class MinaUtils {
    
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(MinaUtils.class);
    
    /** 打开远程会话 */
    public static ClientSession openConnection(String sshHost, Integer sshPort, String sshUser) {
        SshClient sshClient = SshClient.setUpDefaultClient();
        sshClient.start();
        ClientSession session = null;
        String privateKeyPath = System.getProperty("user.home") + Constants.ID_RSA;
        try {
            String privateKeyContent = new String(Files.readAllBytes(Paths.get(privateKeyPath)));
            session = sshClient.connect(sshUser, sshHost, sshPort).verify().getClientSession();
            session.addPublicKeyIdentity(getKeyPairFromString(privateKeyContent));
            if (session.auth().verify().isFailure()) {
                LOG.info("验证失败");
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
        session.resetAuthTimeout();
        LOG.info("exe cmd: {}", command);
        // 命令返回的结果
        ChannelExec ce = null;
        // 返回结果流
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // 错误信息
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        try {
            ce = session.createExecChannel(command);
            ce.setOut(out);
            ce.setErr(err);
            // 执行并等待
            ce.open();
            Set<ClientChannelEvent> events =
                    ce.waitFor(
                            EnumSet.of(ClientChannelEvent.CLOSED),
                            TimeUnit.SECONDS.toMillis(100000));
            // 检查请求是否超时
            if (events.contains(ClientChannelEvent.TIMEOUT)) {
                throw new Exception("mina 连接超时");
            }
            int exitStatus = ce.getExitStatus();
            LOG.info("mina result {}", exitStatus);
            if (exitStatus == 1) {
                return "failed";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (ce.isClosed()) {
                try {
                    ce.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        LOG.info("exe cmd return : {}", out);
        return out.toString().trim();
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
            throw new RuntimeException(e);
        }
        LOG.info(sshHost + " 密码连接成功");
        return session;
    }

    /** 设置免密登录 */
    public static boolean setupPasswordlessLogin(ClientSession session, String sshUser, String sshPassword) {
        try {
            String homeDir = System.getProperty("user.home");
            String sshDir = homeDir + "/.ssh";
            String privateKeyPath = sshDir + "/id_rsa";
            String publicKeyPath = sshDir + "/id_rsa.pub";
            
            LOG.info("SSH目录: {}", sshDir);
            LOG.info("私钥路径: {}", privateKeyPath);
            LOG.info("公钥路径: {}", publicKeyPath);

            // 创建本地.ssh目录
            File sshDirFile = new File(sshDir);
            if (!sshDirFile.exists()) {
                LOG.info("创建本地.ssh目录: {}", sshDir);
                sshDirFile.mkdirs();
                execCmdWithResult(session, "chmod 700 " + sshDir);
            }

            // 生成密钥对(如果不存在)
            File privateKeyFile = new File(privateKeyPath);
            File publicKeyFile = new File(publicKeyPath);
            
            if (!privateKeyFile.exists() || !publicKeyFile.exists()) {
                LOG.info("生成SSH密钥对");
                
                // 使用Java生成RSA密钥对
                KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
                keyGen.initialize(2048);
                KeyPair keyPair = keyGen.generateKeyPair();
                
                RSAPublicKey rsaPublicKey = (RSAPublicKey) keyPair.getPublic();
                RSAPrivateKey rsaPrivateKey = (RSAPrivateKey) keyPair.getPrivate();
                
                // 将密钥转换为OpenSSH格式
                // 私钥格式
                PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(rsaPrivateKey.getEncoded());
                
                // 公钥格式 (SSH-RSA BASE64(publickey) comment)
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] sshrsa = new byte[] {0, 0, 0, 7, 's', 's', 'h', '-', 'r', 's', 'a'};
                byte[] e = rsaPublicKey.getPublicExponent().toByteArray();
                byte[] n = rsaPublicKey.getModulus().toByteArray();
                
                if (e[0] == 0) {
                    byte[] tmp = new byte[e.length - 1];
                    System.arraycopy(e, 1, tmp, 0, tmp.length);
                    e = tmp;
                }
                
                if (n[0] == 0) {
                    byte[] tmp = new byte[n.length - 1];
                    System.arraycopy(n, 1, tmp, 0, tmp.length);
                    n = tmp;
                }
                
                writeInt(baos, sshrsa.length);
                baos.write(sshrsa);
                writeInt(baos, e.length);
                baos.write(e);
                writeInt(baos, n.length);
                baos.write(n);
                
                String publicKeyContent = "ssh-rsa " + 
                    Base64.getEncoder().encodeToString(baos.toByteArray()) + 
                    " " + System.getProperty("user.name") + "@" + InetAddress.getLocalHost().getHostName();
                
                // 保存私钥(PEM格式)
                String privateKeyContent = 
                    "-----BEGIN PRIVATE KEY-----\n" +
                    Base64.getEncoder().encodeToString(pkcs8EncodedKeySpec.getEncoded()) +
                    "\n-----END PRIVATE KEY-----\n";
                
                // 写入文件
                try (FileWriter fw = new FileWriter(privateKeyFile)) {
                    fw.write(privateKeyContent);
                }
                
                try (FileWriter fw = new FileWriter(publicKeyFile)) {
                    fw.write(publicKeyContent);
                }
                
                // 设置权限
                execCmdWithResult(session, "chmod 600 " + privateKeyPath);
                execCmdWithResult(session, "chmod 644 " + publicKeyPath);
                
                LOG.info("密钥对生成完成");
            }

            // 确保公钥文件存在
            if (!publicKeyFile.exists()) {
                LOG.error("公钥文件不存在: {}", publicKeyPath);
                return false;
            }

            // 读取公钥内容
            String publicKey = new String(Files.readAllBytes(Paths.get(publicKeyPath)));
            LOG.info("读取到的公钥内容: {}", publicKey);

            // 将公钥添加到远程authorized_keys
            // 1. 创建远程.ssh目录
            execCmdWithResult(session, "mkdir -p ~/.ssh");
            execCmdWithResult(session, "chmod 700 ~/.ssh");
            
            // 2. 将公钥追加到authorized_keys
            String remoteCmd = String.format(
                "echo '%s' >> ~/.ssh/authorized_keys && chmod 600 ~/.ssh/authorized_keys",
                publicKey.trim()
            );
            LOG.info("添加公钥到远程主机");
            String result = execCmdWithResult(session, remoteCmd);
            LOG.info("添加公钥到远程主机结果: {}", result);

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