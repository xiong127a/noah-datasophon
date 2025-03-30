package com.datasophon.api.utils;

import com.datasophon.common.Constants;
import com.datasophon.common.model.HostInfo;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ChannelExec;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.keyverifier.AcceptAllServerKeyVerifier;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.sftp.client.SftpClientFactory;
import org.apache.sshd.sftp.client.fs.SftpFileSystem;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class MinaUtils {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(MinaUtils.class);

    /** 打开远程会话 */
    public static ClientSession openConnection(HostInfo hostInfo) {
        String sshIp = hostInfo.getIp();
        Integer sshPort = hostInfo.getSshPort();
        String sshUser = hostInfo.getSshUser();
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

        LOG.info(sshIp + " 连接成功");
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

        // 检测并处理Windows命令
        if (command.trim().startsWith("cmd ")) {
            return execWindowsCmdWithResult(session, command);
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
            Set<ClientChannelEvent> events = ce.waitFor(EnumSet.of(ClientChannelEvent.CLOSED),
                    TimeUnit.SECONDS.toMillis(100000));

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
                } else if (command.contains("\\cp") && exitStatus == 1) {
                    LOG.warn("复制文件失败，尝试使用sudo...");
                    return execCmdWithResult(session, "sudo " + command);
                } else if (command.contains("service") && command.contains("restart")) {
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

    /**
     * 针对Windows系统的命令执行，解决中文编码问题
     * 
     * @param session SSH会话
     * @param command 要执行的命令
     * @return 解码后的命令执行结果
     */
    public static String execWindowsCmdWithResult(ClientSession session, String command) {
        if (session == null) {
            LOG.error("SSH会话为空，无法执行Windows命令: {}", command);
            return null;
        }

        session.resetAuthTimeout();
        LOG.info("执行Windows命令: {}", command);

        // 先执行代码页设置为UTF-8，解决中文乱码问题
        try {
            // 设置控制台代码页为UTF-8
            ChannelExec chcpChannel = session.createExecChannel("chcp 65001");
            chcpChannel.setOut(new ByteArrayOutputStream());
            chcpChannel.open();
            chcpChannel.waitFor(EnumSet.of(ClientChannelEvent.CLOSED), TimeUnit.SECONDS.toMillis(5000));
            chcpChannel.close();
            LOG.info("已设置Windows控制台代码页为UTF-8 (65001)");
        } catch (Exception e) {
            LOG.warn("设置Windows代码页失败: {}, 继续尝试执行命令", e.getMessage());
        }

        // 获取Windows系统的代码页
        String codepage = getWindowsCodePage(session);
        LOG.info("Windows系统代码页: {}", codepage);

        // 尝试使用PowerShell并强制UTF-8输出
        if (!command.contains("powershell") && !command.contains("wmic")) {
            // 对于简单命令，尝试用PowerShell包装以获得更好的编码处理
            if (command.startsWith("cmd /c")) {
                command = command.replace("cmd /c", "powershell -command");
            } else {
                command = "powershell -command \"" + command.replace("\"", "\\\"") + "\"";
            }
            // 强制PowerShell输出UTF-8
            command = command + " | Out-String -Width 4096";
            LOG.info("转换为PowerShell命令: {}", command);
        }

        // 执行原始命令
        ChannelExec ce = null;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        try {
            ce = session.createExecChannel(command);
            ce.setOut(out);
            ce.setErr(err);
            ce.open();

            // 等待命令执行完成
            Set<ClientChannelEvent> events = ce.waitFor(EnumSet.of(ClientChannelEvent.CLOSED),
                    TimeUnit.SECONDS.toMillis(100000));

            if (events.contains(ClientChannelEvent.TIMEOUT)) {
                LOG.error("Windows命令执行超时: {}", command);
                return "ERROR: Command timed out";
            }

            int exitStatus = ce.getExitStatus();
            LOG.info("Windows命令执行结果状态: {}", exitStatus);

            // 获取输出并进行编码转换
            String outResult = convertWindowsOutput(out.toByteArray(), codepage);
            String errResult = convertWindowsOutput(err.toByteArray(), codepage);

            // 如果出错了，但这是预期中的错误（比如命令不存在），提供友好的替代值
            if (exitStatus != 0) {
                // 如果是命令不存在的错误，根据不同命令提供合理的默认值
                if (command.contains("hostname -f") || command.contains("hostname -s")) {
                    // 获取短主机名失败时，直接通过hostname获取
                    LOG.warn("Windows不支持hostname -f或-s参数，尝试直接执行hostname");
                    return execWindowsCmdWithResult(session, "powershell -command \"hostname\"");
                } else if (command.contains("uname")) {
                    // Windows没有uname命令
                    LOG.warn("Windows不支持uname命令，尝试获取系统信息");
                    if (command.contains("uname -a")) {
                        return "Windows"; // 简单返回Windows标识
                    } else if (command.contains("uname -m") || command.contains("uname -p")) {
                        // 尝试获取CPU架构
                        return execWindowsCmdWithResult(session, "powershell -command \"$env:PROCESSOR_ARCHITECTURE\"");
                    }
                } else if (command.contains("cat /etc")) {
                    // Windows没有cat和/etc目录
                    LOG.warn("Windows不支持cat /etc相关命令");
                    if (command.contains("/etc/hosts")) {
                        // 尝试直接读取hosts文件
                        return execWindowsCmdWithResult(session,
                                "powershell -command \"Get-Content C:\\Windows\\System32\\drivers\\etc\\hosts\"");
                    } else if (command.contains("/etc/resolv.conf")) {
                        // DNS信息通过其他方式获取
                        LOG.warn("Windows无法读取/etc/resolv.conf，尝试使用PowerShell获取DNS信息");
                        String dnsCmd = "powershell -command \"Get-DnsClientServerAddress | Select-Object ServerAddresses | Format-List\"";
                        String ipconfig = execWindowsCmdWithResult(session, dnsCmd);
                        // 简单提取DNS部分（实际应用中可能需要更复杂的解析）
                        return ipconfig != null ? ipconfig : "8.8.8.8";
                    }
                    // 其他/etc文件，返回空值
                    return "";
                } else if (command.contains("lspci")) {
                    // Windows没有lspci命令
                    LOG.warn("Windows不支持lspci命令，尝试使用其他方式获取GPU信息");
                    return execWindowsCmdWithResult(session,
                            "powershell -command \"Get-WmiObject Win32_VideoController | Select-Object Name\"");
                } else if (errResult.contains("不是内部或外部命令") ||
                        errResult.contains("不可识别的命令") ||
                        errResult.contains("command not found")) {
                    // 通用命令不存在处理
                    LOG.warn("Windows命令不存在: {}, 错误: {}", command, errResult);
                    return "命令不支持";
                }

                LOG.error("Windows命令执行失败: {} - 错误信息: {}", command, errResult);
                // 确保错误信息不包含乱码
                if (errResult.trim().isEmpty()) {
                    return "执行错误: 未知错误";
                }
                return "执行错误: " + errResult;
            }

            // 检查输出是否为空
            if (outResult == null || outResult.trim().isEmpty()) {
                // 如果标准输出为空，尝试使用错误输出
                if (errResult != null && !errResult.trim().isEmpty()) {
                    LOG.warn("Windows命令标准输出为空，使用错误输出: {}", errResult);
                    return errResult;
                }
                LOG.warn("Windows命令无输出");
                return "";
            }

            LOG.info("Windows命令执行结果: {}", outResult);
            return outResult;
        } catch (IOException e) {
            LOG.error("执行Windows命令异常: {} - {}", command, e.getMessage());
            return "执行异常: " + e.getMessage();
        } finally {
            try {
                if (ce != null) {
                    ce.close();
                }
            } catch (IOException e) {
                LOG.error("关闭Windows命令通道异常", e);
            }
        }
    }

    /**
     * 获取Windows系统的代码页
     * 
     * @param session SSH会话
     * @return 代码页编号，如果获取失败则返回默认值
     */
    private static String getWindowsCodePage(ClientSession session) {
        try {
            // 使用PowerShell获取当前代码页，更可靠
            ChannelExec ce = session.createExecChannel("powershell -command \"[Console]::OutputEncoding.CodePage\"");
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ce.setOut(out);
            ce.open();

            // 等待命令执行完成
            ce.waitFor(EnumSet.of(ClientChannelEvent.CLOSED), TimeUnit.SECONDS.toMillis(10000));
            ce.close();

            String result = out.toString().trim();
            LOG.info("Windows代码页信息: {}", result);

            // 如果能获取到代码页数字
            if (result.matches("\\d+")) {
                return result;
            }

            // 尝试使用chcp命令作为备选方案
            ce = session.createExecChannel("cmd /c chcp");
            out = new ByteArrayOutputStream();
            ce.setOut(out);
            ce.open();
            ce.waitFor(EnumSet.of(ClientChannelEvent.CLOSED), TimeUnit.SECONDS.toMillis(10000));
            ce.close();

            result = out.toString();
            if (result.contains("活动代码页:") || result.contains("Active code page:")) {
                String[] parts = result.split("[:,：]");
                if (parts.length >= 2) {
                    return parts[1].trim();
                }
            }

            // 如果无法解析，返回默认代码页（UTF-8）
            return "65001";
        } catch (Exception e) {
            LOG.warn("获取Windows代码页失败: {}", e.getMessage());
            // 默认返回UTF-8代码页
            return "65001";
        }
    }

    /**
     * 根据代码页转换Windows命令输出的编码
     * 
     * @param bytes    原始输出字节
     * @param codepage Windows代码页
     * @return 转换后的字符串
     */
    private static String convertWindowsOutput(byte[] bytes, String codepage) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }

        try {
            String encoding;
            // 先尝试使用UTF-8解码
            try {
                String utf8Result = new String(bytes, "UTF-8");
                // 如果解码后没有明显的乱码字符，直接返回
                if (!utf8Result.contains("")) {
                    return utf8Result;
                }
            } catch (Exception e) {
                // UTF-8解码失败，继续尝试其他编码
            }

            // 根据代码页选择正确的编码
            switch (codepage) {
                case "936": // 简体中文GBK
                    encoding = "GBK";
                    break;
                case "950": // 繁体中文Big5
                    encoding = "Big5";
                    break;
                case "437": // 美国英语
                case "850": // 多语言拉丁语-1
                    encoding = "Cp" + codepage;
                    break;
                case "65001": // UTF-8
                    encoding = "UTF-8";
                    break;
                default:
                    // 尝试使用代码页作为编码名称
                    encoding = "Cp" + codepage;
            }

            // 使用指定编码解码字节
            String result = new String(bytes, encoding);

            // 检查结果是否包含替换字符()，如果有，尝试其他编码
            if (result.contains("")) {
                LOG.warn("使用编码 {} 解码存在问题，尝试其他编码", encoding);

                // 尝试按优先级尝试常见编码
                String[] fallbackEncodings = { "GBK", "UTF-8", "Cp1252", "Cp850", "ISO-8859-1" };
                for (String fallbackEncoding : fallbackEncodings) {
                    if (!fallbackEncoding.equals(encoding)) {
                        try {
                            String fallbackResult = new String(bytes, fallbackEncoding);
                            if (!fallbackResult.contains("")) {
                                LOG.info("使用备选编码 {} 成功解码", fallbackEncoding);
                                return fallbackResult;
                            }
                        } catch (Exception e) {
                            // 忽略此编码的错误，继续尝试
                        }
                    }
                }
            }

            return result;
        } catch (Exception e) {
            LOG.warn("转换Windows输出编码失败: {}，尝试使用系统默认编码解码", e.getMessage());
            // 出错时使用系统默认编码
            return new String(bytes);
        }
    }

    /**
     * 从ipconfig输出中提取DNS服务器信息
     * 
     * @param ipconfig ipconfig /all的输出结果
     * @return DNS服务器列表，每行一个
     */
    private static String extractDNSFromIpconfig(String ipconfig) {
        if (ipconfig == null || ipconfig.isEmpty()) {
            return "";
        }

        StringBuilder dns = new StringBuilder();
        String[] lines = ipconfig.split("[\r\n]+");
        boolean inDnsSection = false;

        for (String line : lines) {
            // 检测是否包含DNS服务器相关文本
            if (line.contains("DNS Server") || line.contains("DNS 服务器")) {
                inDnsSection = true;
                // 尝试提取同一行中的IP地址
                int colonIndex = line.indexOf(':');
                if (colonIndex > 0 && colonIndex < line.length() - 1) {
                    String ipPart = line.substring(colonIndex + 1).trim();
                    if (ipPart.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
                        dns.append(ipPart).append("\n");
                    }
                }
            }
            // 如果在DNS部分，提取缩进的IP地址行
            else if (inDnsSection && line.trim().matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
                dns.append(line.trim()).append("\n");
            }
            // 如果遇到新的章节（包含冒号但不是IP地址），结束DNS部分
            else if (inDnsSection && line.contains(":") && !line.contains("DNS")) {
                inDnsSection = false;
            }
        }

        // 如果没有找到DNS，返回谷歌公共DNS作为备用
        if (dns.length() == 0) {
            return "8.8.8.8\n";
        }

        return dns.toString();
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
            Set<ClientChannelEvent> events = channelExec.waitFor(EnumSet.of(ClientChannelEvent.CLOSED),
                    TimeUnit.SECONDS.toMillis(100000));

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
     * @param session    连接
     * @param remotePath 远程目录地址
     * @param inputFile  文件 File
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
    public static ClientSession openConnectionWithPassword(HostInfo hostInfo) {
        String sshIp = hostInfo.getIp();
        int sshPort = hostInfo.getSshPort();
        String sshUser = hostInfo.getSshUser();
        String sshPassword = hostInfo.getSshPassword();
        SshClient sshClient = SshClient.setUpDefaultClient();

        // 配置自动接受未知主机密钥
        sshClient.setServerKeyVerifier(AcceptAllServerKeyVerifier.INSTANCE);

        sshClient.start();
        ClientSession session = null;
        try {
            session = sshClient.connect(sshUser, sshIp, sshPort).verify().getClientSession();
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
        LOG.info(sshIp + " 密码连接成功");
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
                            });

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
                            });

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
                    remoteKeyPath);
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
     * 
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
     * 
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
     * 
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
        } else if (command.startsWith("service")) {
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
     * 
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
            String fixCmd = "sudo sed -i '2i### BEGIN INIT INFO\\n# Provides:          datasophon-worker\\n# Required-Start:    $remote_fs $syslog\\n# Required-Stop:     $remote_fs $syslog\\n# Default-Start:     2 3 4 5\\n# Default-Stop:      0 1 6\\n# Short-Description: Datasophon Worker Service\\n# Description:       Datasophon Worker Service for Big Data Platform\\n### END INIT INFO' "
                    + scriptPath;

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
            String moveCmd = "sudo mv /tmp/" + new File(tempFile).getName() + " " + systemdDir
                    + "/datasophon-worker.service";
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
     * 
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

    /**
     * 检查SSH免密配置状态
     * 
     * @param session SSH会话
     * @return 检查结果，包含是否成功和详细消息
     */
    public static CheckResult checkPasswordlessStatus(ClientSession session) {
        try {
            // 1. 检查SSH服务状态
            String result = execCmdWithResult(session, "systemctl status sshd | grep Active");
            if (result == null || !result.contains("active")) {
                return new CheckResult(false, "SSH服务未运行");
            }

            // 2. 检查.ssh目录权限
            result = execCmdWithResult(session, "ls -ld ~/.ssh");
            if (result == null || !result.contains("drwx------")) {
                return new CheckResult(false, "SSH目录权限不正确");
            }

            // 3. 检查authorized_keys文件权限
            result = execCmdWithResult(session, "ls -l ~/.ssh/authorized_keys");
            if (result == null || !result.contains("-rw-------")) {
                return new CheckResult(false, "authorized_keys文件权限不正确");
            }

            // 4. 检查SSH配置
            result = execCmdWithResult(session, "grep -E '^PubkeyAuthentication\\s+yes' /etc/ssh/sshd_config");
            if (result == null || result.isEmpty()) {
                return new CheckResult(false, "SSH配置未启用公钥认证");
            }

            // 5. 测试免密登录
            result = execCmdWithResult(session, "ssh -o BatchMode=yes -o StrictHostKeyChecking=no localhost echo OK");
            if (result == null || !"OK".equals(result.trim())) {
                return new CheckResult(false, "免密登录测试失败");
            }

            return new CheckResult(true, "免密登录配置正确");
        } catch (Exception e) {
            LOG.error("免密检查过程发生异常", e);
            return new CheckResult(false, "免密检查过程发生异常: " + e.getMessage());
        }
    }

    /**
     * 检查结果类
     */
    public static class CheckResult {
        private final boolean success;
        private final String message;

        public CheckResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        ClientSession session = MinaUtils.openConnection(new HostInfo("localhost", 22, "liuxin"));
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

    /**
     * 获取远程主机的实际主机名
     *
     * @param session SSH会话
     * @return 实际主机名，如果获取失败则返回null
     */
    public static String getRemoteHostname(ClientSession session) {
        if (session == null) {
            LOG.error("SSH会话为空，无法获取远程主机名");
            return null;
        }

        try {
            // 尝试获取主机名
            String hostname = execCmdWithResult(session, "hostname -f 2>/dev/null || hostname");

            // 如果成功获取到主机名
            if (hostname != null && !hostname.startsWith("ERROR:") && !hostname.trim().isEmpty()) {
                hostname = hostname.trim();
                LOG.info("获取到远程主机名: {}", hostname);
                return hostname;
            } else {
                // 如果hostname命令失败，尝试其他方式
                String hostsEntry = execCmdWithResult(session,
                        "cat /etc/hosts | grep -v '^#' | grep -v '^127.0.0.1' | grep -v '^::1' | head -1");
                if (hostsEntry != null && !hostsEntry.startsWith("ERROR:") && !hostsEntry.trim().isEmpty()) {
                    // 解析/etc/hosts中的第一个非本地回环条目
                    String[] parts = hostsEntry.trim().split("\\s+");
                    if (parts.length >= 2) {
                        hostname = parts[1].trim();
                        LOG.info("从/etc/hosts获取到主机名: {}", hostname);
                        return hostname;
                    }
                }

                // 再尝试一种方法
                hostname = execCmdWithResult(session, "cat /etc/hostname 2>/dev/null");
                if (hostname != null && !hostname.startsWith("ERROR:") && !hostname.trim().isEmpty()) {
                    hostname = hostname.trim();
                    LOG.info("从/etc/hostname获取到主机名: {}", hostname);
                    return hostname;
                }
            }

            LOG.warn("无法获取远程主机名，所有尝试均失败");
            return null;
        } catch (Exception e) {
            LOG.error("获取远程主机名时发生异常: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 针对Windows系统的硬件信息收集，处理编码问题并返回格式化结果
     * 
     * @param session     SSH会话
     * @param commandType 命令类型：可以是"disk"、"memory"、"gpu"、"cpu"、"swap"
     * @return 命令执行结果，已经过编码处理和格式化
     */
    public static String collectWindowsHardwareInfo(ClientSession session, String commandType) {
        if (session == null) {
            LOG.error("SSH会话为空，无法收集Windows硬件信息");
            return null;
        }

        session.resetAuthTimeout();
        String command = "";

        // 根据不同类型选择合适的命令 - 统一使用PowerShell以获得更好的编码处理
        switch (commandType.toLowerCase()) {
            case "disk":
                // 使用PowerShell获取磁盘信息，强制UTF-8输出
                command = "powershell -command \"[Console]::OutputEncoding = [System.Text.Encoding]::UTF8; Get-WmiObject Win32_LogicalDisk | Select-Object DeviceID, Size, FreeSpace | Format-Table -AutoSize | Out-String -Width 4096\"";
                break;
            case "memory":
                // 使用PowerShell获取内存信息，强制UTF-8输出
                command = "powershell -command \"[Console]::OutputEncoding = [System.Text.Encoding]::UTF8; $os = Get-WmiObject -Class Win32_OperatingSystem; Write-Output ('TotalVisibleMemorySize=' + $os.TotalVisibleMemorySize); Write-Output ('FreePhysicalMemory=' + $os.FreePhysicalMemory)\"";
                break;
            case "gpu":
                // 获取GPU信息，强制UTF-8输出
                command = "powershell -command \"[Console]::OutputEncoding = [System.Text.Encoding]::UTF8; Get-WmiObject Win32_VideoController | Select-Object Name, AdapterRAM, DriverVersion | Format-List | Out-String -Width 4096\"";
                break;
            case "cpu":
                // 获取CPU详细信息，强制UTF-8输出
                command = "powershell -command \"[Console]::OutputEncoding = [System.Text.Encoding]::UTF8; Get-WmiObject Win32_Processor | Select-Object Name, NumberOfCores, NumberOfLogicalProcessors, MaxClockSpeed | Format-List | Out-String -Width 4096\"";
                break;
            case "swap":
                // 获取交换空间信息，强制UTF-8输出
                command = "powershell -command \"[Console]::OutputEncoding = [System.Text.Encoding]::UTF8; Get-WmiObject -Class Win32_PageFileUsage | Select-Object AllocatedBaseSize, CurrentUsage, PeakUsage | Format-List | Out-String -Width 4096\"";
                break;
            default:
                LOG.error("未知的硬件信息类型: {}", commandType);
                return "ERROR: 未知的硬件信息类型";
        }

        LOG.info("执行Windows硬件信息收集命令: {} ({})", commandType, command);

        // 使用改进的execWindowsCmdWithResult方法执行命令
        String result = execWindowsCmdWithResult(session, command);

        // 对结果进行验证
        if (result == null || result.isEmpty() || result.startsWith("ERROR:") || result.startsWith("执行错误:")) {
            LOG.warn("Windows {} 信息收集失败，尝试使用备选命令", commandType);

            // 使用备选命令
            switch (commandType.toLowerCase()) {
                case "disk":
                    return execWindowsCmdWithResult(session,
                            "powershell -command \"[Console]::OutputEncoding = [System.Text.Encoding]::UTF8; Get-PSDrive -PSProvider FileSystem | Select-Object Name, Used, Free | Format-Table -AutoSize | Out-String\"");
                case "memory":
                    return execWindowsCmdWithResult(session,
                            "powershell -command \"[Console]::OutputEncoding = [System.Text.Encoding]::UTF8; Get-CimInstance -ClassName Win32_OperatingSystem | Select-Object TotalVisibleMemorySize, FreePhysicalMemory | Format-List | Out-String\"");
                case "gpu":
                    return execWindowsCmdWithResult(session,
                            "powershell -command \"[Console]::OutputEncoding = [System.Text.Encoding]::UTF8; Get-CimInstance -ClassName Win32_VideoController | Select-Object Name | Format-List | Out-String\"");
                case "cpu":
                    return execWindowsCmdWithResult(session,
                            "powershell -command \"[Console]::OutputEncoding = [System.Text.Encoding]::UTF8; Get-CimInstance -ClassName Win32_Processor | Select-Object Name, NumberOfCores, NumberOfLogicalProcessors | Format-List | Out-String\"");
                case "swap":
                    return execWindowsCmdWithResult(session,
                            "powershell -command \"[Console]::OutputEncoding = [System.Text.Encoding]::UTF8; Get-CimInstance -ClassName Win32_PageFileUsage | Select-Object AllocatedBaseSize, CurrentUsage | Format-List | Out-String\"");
                default:
                    break;
            }
        }

        return result;
    }
}