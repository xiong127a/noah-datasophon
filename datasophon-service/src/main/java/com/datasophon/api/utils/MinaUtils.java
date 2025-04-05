package com.datasophon.api.utils;

import com.datasophon.common.Constants;
import com.datasophon.common.model.HostInfo;
import lombok.Getter;
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
     * 命令执行结果类
     * 包含命令执行的状态码、输出信息和错误信息
     */
    @Getter
    public static class CommandResult {
        /** 命令内容 */
        private final String command;

        /** 命令执行的退出码 */
        private final int exitCode;

        /** 命令的标准输出内容 */
        private final String output;

        /** 命令的错误输出内容 */
        private final String error;

        /** 命令是否执行成功 */
        private final boolean success;

        /**
         * 构造命令执行结果
         * 
         * @param command  执行的命令
         * @param exitCode 退出码
         * @param output   标准输出
         * @param error    错误输出
         */
        public CommandResult(String command, int exitCode, String output, String error) {
            this.command = command;
            this.exitCode = exitCode;
            this.output = output;
            this.error = error;
            this.success = exitCode == 0;
        }

        /**
         * 创建成功的命令结果
         */
        public static CommandResult success(String command, String output) {
            return new CommandResult(command, 0, output, "");
        }

        /**
         * 创建失败的命令结果
         */
        public static CommandResult failed(String command, int exitCode, String error) {
            return new CommandResult(command, exitCode, "", error);
        }

        /**
         * 创建异常的命令结果
         */
        public static CommandResult exception(String command, String errorMessage) {
            return new CommandResult(command, -1, "", errorMessage);
        }

        /**
         * 获取兼容的旧格式输出
         * 如果成功则返回output，如果失败则返回带EXIT_CODE前缀的错误信息
         */
        public String getLegacyOutput() {
            if (success) {
                return output;
            } else {
                return "EXIT_CODE_" + exitCode + ": " + (error.isEmpty() ? "执行错误" : error);
            }
        }

        @Override
        public String toString() {
            return "CommandResult{" +
                    "command='" + command + '\'' +
                    ", exitCode=" + exitCode +
                    ", success=" + success +
                    ", output='"
                    + (output != null ? (output.length() > 50 ? output.substring(0, 50) + "..." : output) : "null")
                    + '\'' +
                    ", error='"
                    + (error != null ? (error.length() > 50 ? error.substring(0, 50) + "..." : error) : "null") + '\'' +
                    '}';
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
            return CommandResult.exception(command, "SSH会话为空");
        }

        // 检测并处理Windows命令
        if (command.trim().startsWith("cmd ")) {
            return execWindowsCmdWithResult(session, command);
        }

        session.resetAuthTimeout();
        LOG.info("执行命令: {}", command);
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
                return CommandResult.failed(command, 124, "命令执行超时");
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
            return CommandResult.success(command, outResult);
        } catch (IOException e) {
            LOG.error("执行命令异常: {} - {}", command, e.getMessage());
            return CommandResult.exception(command, e.getMessage());
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
     * @return 命令执行结果对象
     */
    public static CommandResult execWindowsCmdWithResult(ClientSession session, String command) {
        if (session == null) {
            LOG.error("SSH会话为空，无法执行Windows命令: {}", command);
            return CommandResult.exception(command, "SSH会话为空");
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
                return CommandResult.failed(command, 124, "命令执行超时");
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
                        return CommandResult.success(command, "Windows"); // 简单返回Windows标识
                    } else if (command.contains("uname -m") || command.contains("uname -p")) {
                        // 尝试获取CPU架构
                        return execWindowsCmdWithResult(session,
                                "powershell -command \"$env:PROCESSOR_ARCHITECTURE\"");
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
                        CommandResult ipconfig = execWindowsCmdWithResult(session, dnsCmd);
                        if (ipconfig.isSuccess()) {
                            return ipconfig;
                        } else {
                            // 简单提取DNS部分（实际应用中可能需要更复杂的解析）
                            return CommandResult.success(command, "8.8.8.8");
                        }
                    }
                    // 其他/etc文件，返回空值
                    return CommandResult.success(command, "");
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
                    return CommandResult.failed(command, exitStatus, "命令不支持: " + errResult);
                }

                return new CommandResult(command, exitStatus, outResult, errResult);
            }

            // 检查输出是否为空
            if (outResult == null || outResult.trim().isEmpty()) {
                // 如果标准输出为空，尝试使用错误输出
                if (errResult != null && !errResult.trim().isEmpty()) {
                    LOG.warn("Windows命令标准输出为空，使用错误输出: {}", errResult);
                    return new CommandResult(command, exitStatus, "", errResult);
                }
                LOG.warn("Windows命令无输出");
                return CommandResult.success(command, "");
            }

            LOG.info("Windows命令执行结果: {}", outResult);
            return CommandResult.success(command, outResult);
        } catch (IOException e) {
            LOG.error("执行Windows命令异常: {} - {}", command, e.getMessage());
            return CommandResult.exception(command, e.getMessage());
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

            // 尝试使用chcp命令作为备选方案，但避免使用execCmdWithResultObject来防止递归调用
            ChannelExec chcpChannel = session.createExecChannel("cmd /c chcp");
            ByteArrayOutputStream chcpOut = new ByteArrayOutputStream();
            chcpChannel.setOut(chcpOut);
            chcpChannel.open();
            chcpChannel.waitFor(EnumSet.of(ClientChannelEvent.CLOSED), TimeUnit.SECONDS.toMillis(10000));
            chcpChannel.close();

            result = chcpOut.toString();
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
            // 常用编码列表，按优先级排序
            String[] commonEncodings = {
                    "UTF-8", "GBK", "GB2312", "Big5", "Cp1252", "Cp850", "Cp437", "ISO-8859-1"
            };

            // 先尝试UTF-8解码，如果没有问题直接返回
            try {
                String utf8Result = new String(bytes, StandardCharsets.UTF_8);
                // 简单检查是否有明显的乱码
                if (!containsInvalidUTF8Chars(utf8Result)) {
                    return utf8Result;
                }
            } catch (Exception e) {
                // UTF-8解码失败，继续尝试其他编码
                LOG.debug("UTF-8解码失败，尝试其他编码");
            }

            // 根据代码页确定编码名称
            String encoding;
            switch (codepage) {
                case "936": // 简体中文GBK
                    encoding = "GBK";
                    break;
                case "950": // 繁体中文Big5
                    encoding = "Big5";
                    break;
                case "437": // 美国英语
                    encoding = "Cp437";
                    break;
                case "850": // 多语言拉丁语-1
                    encoding = "Cp850";
                    break;
                case "65001": // UTF-8
                    encoding = "UTF-8";
                    break;
                case "1252": // 西欧
                    encoding = "Cp1252";
                    break;
                case "932": // 日语
                    encoding = "Shift_JIS";
                    break;
                case "949": // 韩语
                    encoding = "Cp949";
                    break;
                default:
                    // 尝试使用代码页作为编码名称，但先检查是否支持
                    encoding = "Cp" + codepage;
                    try {
                        if (!isEncodingSupported(encoding)) {
                            LOG.warn("不支持的编码: {}，使用UTF-8替代", encoding);
                            encoding = "UTF-8"; // 默认回退到UTF-8
                        }
                    } catch (Exception e) {
                        LOG.warn("编码检查异常: {}，使用UTF-8替代", e.getMessage());
                        encoding = "UTF-8";
                    }
            }

            // 尝试使用确定的编码
            try {
                String result = new String(bytes, encoding);
                // 检查结果是否包含替换字符()，如果有，尝试其他编码
                if (containsReplacementChar(result)) {
                    LOG.warn("使用编码 {} 解码存在问题，尝试其他编码", encoding);
                    return tryMultipleEncodings(bytes, commonEncodings);
                }
                return result;
            } catch (Exception e) {
                LOG.warn("使用编码 {} 解码失败: {}，尝试其他编码", encoding, e.getMessage());
                return tryMultipleEncodings(bytes, commonEncodings);
            }
        } catch (Exception e) {
            LOG.warn("转换Windows输出编码失败: {}，尝试使用系统默认编码解码", e.getMessage());
            // 出错时使用系统默认编码
            return new String(bytes);
        }
    }

    /**
     * 检查字符串是否包含UTF-8解码错误的标志
     */
    private static boolean containsInvalidUTF8Chars(String str) {
        // 检查常见的无效UTF-8解码结果特征
        return str.contains("") || str.contains("\uFFFD");
    }

    /**
     * 检查字符串是否包含替换字符()
     */
    private static boolean containsReplacementChar(String str) {
        return str.contains("") || str.contains("\uFFFD");
    }

    /**
     * 检查指定编码是否被Java支持
     */
    private static boolean isEncodingSupported(String encoding) {
        try {
            return java.nio.charset.Charset.isSupported(encoding);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 尝试多种编码解码字节数组
     */
    private static String tryMultipleEncodings(byte[] bytes, String[] encodings) {
        for (String encoding : encodings) {
            try {
                String result = new String(bytes, encoding);
                if (!containsReplacementChar(result)) {
                    LOG.info("使用编码 {} 成功解码", encoding);
                    return result;
                }
            } catch (Exception e) {
                // 忽略此编码的错误，继续尝试
            }
        }

        // 如果所有编码都失败，返回系统默认编码的结果
        LOG.warn("所有编码尝试均失败，使用系统默认编码");
        return new String(bytes);
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
        CommandResult result = execCmdWithResultObject(session, command);
        if (result.isSuccess()) {
            return result.getOutput();
        } else {
            throw new IOException(
                    "Command execution failed with exit code " + result.getExitCode() + ": " + result.getError());
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
                hostInfo.setOsErrorMsg("连接异常堆栈: " + stackInfo.toString());
            }
            return null;
        }
        LOG.info(sshIp + " 密码连接成功");
        return session;
    }

    /** 设置免密登录 */
    public static boolean setupPasswordlessLogin(ClientSession session, String sshUser, String sshPassword) {
        try {
            // 检查必要的信息
            if (session == null) {
                LOG.error("SSH会话为空，无法设置免密登录");
                return false;
            }

            // 获取用户主目录
            String userHome = System.getProperty("user.home");
            String sshDir = userHome + File.separator + ".ssh";
            String publicKeyFile = sshDir + File.separator + "id_ed25519.pub";
            String privateKeyFile = sshDir + File.separator + "id_ed25519";

            // 1. 检查本地.ssh目录和密钥是否存在
            File sshDirFile = new File(sshDir);
            if (!sshDirFile.exists()) {
                LOG.info("创建本地.ssh目录: {}", sshDir);
                sshDirFile.mkdirs();
                CommandResult chmodResult = execCmdWithResultObject(session, "chmod 700 " + sshDir);
                LOG.info("设置本地.ssh目录权限结果: {}", chmodResult.isSuccess() ? "成功" : "失败");
            }

            // 2. 生成SSH密钥对
            boolean keysExist = new File(publicKeyFile).exists() && new File(privateKeyFile).exists();
            if (!keysExist) {
                LOG.info("生成SSH密钥对");
                generateSshKeyPair(sshDir);
            }

            if (!new File(publicKeyFile).exists()) {
                LOG.error("公钥文件不存在: {}", publicKeyFile);
                return false;
            }

            try {
                // 1. 确保远程.ssh目录存在且权限正确
                CommandResult mkdirResult = execCmdWithResultObject(session, "mkdir -p ~/.ssh && chmod 700 ~/.ssh");
                LOG.info("创建远程.ssh目录结果: {}", mkdirResult.isSuccess() ? "成功" : "失败: " + mkdirResult.getError());

                // 2. 检查远程authorized_keys文件
                CommandResult touchResult = execCmdWithResultObject(session,
                        "touch ~/.ssh/authorized_keys && chmod 600 ~/.ssh/authorized_keys");
                LOG.info("创建/设置authorized_keys文件权限结果: {}",
                        touchResult.isSuccess() ? "成功" : "失败: " + touchResult.getError());

                // 3. 读取本地公钥
                String publicKey = readPublicKeyFile(publicKeyFile);
                if (publicKey == null || publicKey.isEmpty()) {
                    LOG.error("无法读取公钥内容: {}", publicKeyFile);
                    return false;
                }

                // 4. 将公钥写入临时文件
                String tempFile = createTempPublicKeyFile(publicKey);
                if (tempFile == null) {
                    LOG.error("创建临时公钥文件失败");
                    return false;
                }

                // 5. 上传公钥到远程服务器并添加到authorized_keys
                String remoteKeyPath = "/tmp/id_ed25519.pub." + System.currentTimeMillis();
                boolean uploadResult = uploadFile(session, remoteKeyPath, tempFile);
                if (!uploadResult) {
                    LOG.error("上传公钥到远程服务器失败");
                    new File(tempFile).delete();
                    return false;
                }

                String addKeyCmd = String.format(
                        "cat %s >> ~/.ssh/authorized_keys && sort -u ~/.ssh/authorized_keys -o ~/.ssh/authorized_keys",
                        remoteKeyPath);
                CommandResult addKeyResult = execCmdWithResultObject(session, addKeyCmd);
                LOG.info("添加公钥到authorized_keys结果: {}",
                        addKeyResult.isSuccess() ? "成功" : "失败: " + addKeyResult.getError());

                // 6. 清理临时文件
                CommandResult cleanupResult = execCmdWithResultObject(session, "rm -f " + remoteKeyPath);
                if (!cleanupResult.isSuccess()) {
                    LOG.warn("清理远程临时文件失败: {}", cleanupResult.getError());
                }
                new File(tempFile).delete();

                // 7. 检查远程SSH配置并尝试修复常见问题
                // 检查SSH配置是否启用公钥认证
                CommandResult grepResult = execCmdWithResultObject(session,
                        "grep -E '^PubkeyAuthentication\\s+yes' /etc/ssh/sshd_config");
                if (!grepResult.isSuccess() || grepResult.getOutput().isEmpty()) {
                    LOG.warn("远程SSH服务器可能未明确启用公钥认证，尝试添加配置");
                    // 尝试添加配置
                    String backupCmd = "cp /etc/ssh/sshd_config /etc/ssh/sshd_config.bak_$(date +%Y%m%d%H%M%S)";
                    CommandResult backupResult = execCmdWithResultObject(session, backupCmd);
                    if (!backupResult.isSuccess()) {
                        LOG.warn("备份SSH配置文件失败: {}", backupResult.getError());
                    }

                    // 添加或修改PubkeyAuthentication设置
                    String fixCmd = "grep -q '^PubkeyAuthentication' /etc/ssh/sshd_config && " +
                            "sed -i 's/^PubkeyAuthentication.*/PubkeyAuthentication yes/' /etc/ssh/sshd_config || " +
                            "echo 'PubkeyAuthentication yes' >> /etc/ssh/sshd_config";
                    CommandResult fixResult = execCmdWithResultObject(session, fixCmd);
                    if (!fixResult.isSuccess()) {
                        LOG.warn("修复PubkeyAuthentication配置失败: {}", fixResult.getError());
                    }

                    // 确保支持Ed25519密钥
                    String checkEd25519Cmd = "grep -q '^HostKey.*ssh_host_ed25519_key' /etc/ssh/sshd_config";
                    CommandResult ed25519Result = execCmdWithResultObject(session, checkEd25519Cmd);

                    if (!ed25519Result.isSuccess() || ed25519Result.getOutput().isEmpty()) {
                        LOG.warn("远程SSH服务器可能未启用Ed25519密钥支持，尝试添加配置");
                        String fixEd25519Cmd = "echo 'HostKey /etc/ssh/ssh_host_ed25519_key' >> /etc/ssh/sshd_config";
                        CommandResult fixEd25519Result = execCmdWithResultObject(session, fixEd25519Cmd);
                        if (!fixEd25519Result.isSuccess()) {
                            LOG.warn("添加Ed25519密钥支持失败: {}", fixEd25519Result.getError());
                        }
                    }

                    // 重启SSH服务
                    LOG.info("尝试重启SSH服务以应用新配置");
                    String restartCmd = "systemctl restart sshd || service sshd restart || service ssh restart";
                    CommandResult restartResult = execCmdWithResultObject(session, restartCmd);
                    if (!restartResult.isSuccess()) {
                        LOG.warn("重启SSH服务失败: {}", restartResult.getError());
                    }

                    LOG.info("SSH配置已更新，请在服务器重启后再次尝试免密登录");
                }

                // 检查是否禁用密码认证(可选，通常不推荐自动设置)
                CommandResult passwordAuthResult = execCmdWithResultObject(session,
                        "grep -E '^PasswordAuthentication\\s+no' /etc/ssh/sshd_config");
                if (passwordAuthResult.isSuccess() && !passwordAuthResult.getOutput().isEmpty()) {
                    LOG.warn("远程SSH服务器已禁用密码认证，请确保公钥认证正常工作");
                }

                return true;
            } catch (Exception e) {
                LOG.error("设置免密登录过程发生异常", e);
                return false;
            }
        } catch (Exception e) {
            LOG.error("设置免密登录过程发生严重异常", e);
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
        // 尝试多种方法检测Linux发行版
        String[] commands = {
                "cat /etc/os-release | grep -E '^ID=' | cut -d= -f2",
                "lsb_release -i | cut -f2",
                "cat /etc/redhat-release",
                "cat /etc/issue | head -1"
        };

        for (String cmd : commands) {
            CommandResult result = execCmdWithResultObject(session, cmd);
            if (result.isSuccess() && !result.getOutput().isEmpty()) {
                return result.getOutput().trim();
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
                adaptedCommand = adaptedCommand.replace(" start ", " start ");
                adaptedCommand = adaptedCommand.replace(" stop ", " stop ");
                adaptedCommand = adaptedCommand.replace(" restart ", " restart ");
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

                if (!dirCheckResult.isSuccess() || "not exists".equals(dirCheckResult.getOutput().trim())) {
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
        if (result != null && !result.isSuccess() && adaptedCommand.contains("update-rc.d")
                && adaptedCommand.contains("enable")) {
            String scriptPath = adaptedCommand.replace("update-rc.d", "").replace("enable", "").trim();
            LOG.warn("启用服务失败，尝试添加LSB头信息: {}", scriptPath);

            String fixCmd = "sed -i '1i#!/bin/sh\\n### BEGIN INIT INFO\\n# Provides: " + scriptPath
                    + "\\n# Required-Start: $network $local_fs $remote_fs\\n# Required-Stop: "
                    + "$network $local_fs $remote_fs\\n# Default-Start: 2 3 4 5\\n# Default-Stop: 0 1 6\\n"
                    + "# Short-Description: Datasophon Service\\n# Description: Datasophon Service\\n"
                    + "### END INIT INFO\\n' "
                    + scriptPath;

            CommandResult fixResult = execCmdWithResultObject(session, fixCmd);
            LOG.info("已添加LSB头信息，重试启用服务...");

            // 重试启用服务
            CommandResult retryResult = execCmdWithResultObject(session, adaptedCommand);
            return retryResult.isSuccess() ? retryResult.getOutput()
                    : "EXIT_CODE_" + retryResult.getExitCode() + ": " + retryResult.getError();
        }

        // 如果执行失败，尝试添加sudo再次执行
        if (!result.isSuccess() && !adaptedCommand.startsWith("sudo")) {
            LOG.warn("命令执行失败，尝试使用sudo: {}", adaptedCommand);
            CommandResult sudoResult = execCmdWithResultObject(session, "sudo " + adaptedCommand);
            return sudoResult.isSuccess() ? sudoResult.getOutput()
                    : "EXIT_CODE_" + sudoResult.getExitCode() + ": " + sudoResult.getError();
        }

        return result.isSuccess() ? result.getOutput() : "EXIT_CODE_" + result.getExitCode() + ": " + result.getError();
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
        if (!scriptExistsResult.isSuccess() || !"exists".equals(scriptExistsResult.getOutput().trim())) {
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

        if (systemdExistsResult.isSuccess() && "exists".equals(systemdExistsResult.getOutput().trim())) {
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
                return "EXIT_CODE_" + exitCode + ": 命令执行失败";
            }
        } catch (Exception e) {
            LOG.error("执行本地命令失败: {}", e.getMessage());
            return "EXIT_CODE_-1: " + e.getMessage();
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
            CommandResult sshResult = execCmdWithResultObject(session, "systemctl status sshd | grep Active");
            if (!sshResult.isSuccess() || !sshResult.getOutput().contains("active")) {
                return new CheckResult(false, "SSH服务未运行");
            }

            // 2. 检查.ssh目录权限
            CommandResult dirResult = execCmdWithResultObject(session, "ls -ld ~/.ssh");
            if (!dirResult.isSuccess() || !dirResult.getOutput().contains("drwx------")) {
                return new CheckResult(false, "SSH目录权限不正确");
            }

            // 3. 检查authorized_keys文件权限
            CommandResult keysResult = execCmdWithResultObject(session, "ls -l ~/.ssh/authorized_keys");
            if (!keysResult.isSuccess() || !keysResult.getOutput().contains("-rw-------")) {
                return new CheckResult(false, "authorized_keys文件权限不正确");
            }

            // 4. 检查SSH配置
            CommandResult configResult = execCmdWithResultObject(session,
                    "grep -E '^PubkeyAuthentication\\s+yes' /etc/ssh/sshd_config");
            if (!configResult.isSuccess() || configResult.getOutput().isEmpty()) {
                return new CheckResult(false, "SSH配置未启用公钥认证");
            }

            // 5. 测试免密登录
            CommandResult testResult = execCmdWithResultObject(session,
                    "ssh -o BatchMode=yes -o StrictHostKeyChecking=no localhost echo OK");
            if (!testResult.isSuccess() || !"OK".equals(testResult.getOutput().trim())) {
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
            CommandResult result = MinaUtils.execCmdWithResultObject(session, "arch");
            System.out.println(result.isSuccess() ? result.getOutput() : "Error: " + result.getError());
        }
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
            CommandResult hostnameResult = execCmdWithResultObject(session, "hostname -f 2>/dev/null || hostname");

            // 如果成功获取到主机名
            if (hostnameResult.isSuccess() && !hostnameResult.getOutput().trim().isEmpty()) {
                String hostname = hostnameResult.getOutput().trim();
                LOG.info("获取到远程主机名: {}", hostname);
                return hostname;
            } else {
                // 如果hostname命令失败，尝试其他方式
                CommandResult hostsResult = execCmdWithResultObject(session,
                        "cat /etc/hosts | grep -v '^#' | grep -v '^127.0.0.1' | grep -v '^::1' | head -1");
                if (hostsResult.isSuccess() && !hostsResult.getOutput().trim().isEmpty()) {
                    // 解析/etc/hosts中的第一个非本地回环条目
                    String[] parts = hostsResult.getOutput().trim().split("\\s+");
                    if (parts.length >= 2) {
                        String hostname = parts[1].trim();
                        LOG.info("从/etc/hosts获取到主机名: {}", hostname);
                        return hostname;
                    }
                }

                // 再尝试一种方法
                CommandResult hostnameFileResult = execCmdWithResultObject(session, "cat /etc/hostname 2>/dev/null");
                if (hostnameFileResult.isSuccess() && !hostnameFileResult.getOutput().trim().isEmpty()) {
                    String hostname = hostnameFileResult.getOutput().trim();
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
            return "ERROR: SSH session is null";
        }

        String command;
        switch (commandType.toLowerCase()) {
            case "cpu":
                command = "powershell -command \"Get-WmiObject -Class Win32_Processor | Select-Object Name, NumberOfCores, NumberOfLogicalProcessors, MaxClockSpeed | Format-List\"";
                break;
            case "memory":
                command = "powershell -command \"Get-WmiObject -Class Win32_OperatingSystem | Select-Object TotalVisibleMemorySize, FreePhysicalMemory | Format-List\"";
                break;
            case "disk":
                command = "powershell -command \"Get-WmiObject -Class Win32_LogicalDisk -Filter 'DriveType=3' | " +
                        "Select-Object DeviceID, Size, FreeSpace | Format-List\"";
                break;
            case "gpu":
                command = "powershell -command \"Get-WmiObject -Class Win32_VideoController | " +
                        "Select-Object Name, AdapterRAM, DriverVersion | Format-List\"";
                break;
            case "network":
                command = "powershell -command \"Get-NetAdapter | " +
                        "Select-Object Name, InterfaceDescription, Status, LinkSpeed | Format-List\"";
                break;
            case "swap":
                command = "powershell -command \"Get-WmiObject -Class Win32_PageFileUsage | " +
                        "Select-Object AllocatedBaseSize, CurrentUsage | Format-List\"";
                break;
            default:
                LOG.error("未知的硬件信息类型: {}", commandType);
                return "ERROR: Unknown hardware info type: " + commandType;
        }

        CommandResult result = execCmdWithResultObject(session, command);

        if (result.isSuccess()) {
            return result.getOutput();
        } else {
            LOG.error("收集Windows {} 信息失败，退出码: {}, 错误: {}",
                    commandType, result.getExitCode(), result.getError());
            return "ERROR: Failed to collect " + commandType + " info: " + result.getError();
        }
    }

    /**
     * 生成SSH密钥对
     * 
     * @param sshDir .ssh目录路径
     * @return 是否生成成功
     */
    private static boolean generateSshKeyPair(String sshDir) {
        try {
            String keyFile = sshDir + File.separator + "id_ed25519";

            // 使用系统ssh-keygen命令生成ED25519密钥对
            ProcessBuilder pb = new ProcessBuilder(
                    "ssh-keygen",
                    "-t", "ed25519",
                    "-f", keyFile,
                    "-N", "", // 空密码
                    "-C", "datasophon-" + System.currentTimeMillis());

            pb.redirectErrorStream(true);
            Process process = pb.start();

            // 读取进程输出
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    LOG.info("ssh-keygen: {}", line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                LOG.error("生成SSH密钥对失败，退出码: {}", exitCode);
                return false;
            }

            // 设置正确的密钥文件权限
            File privateKey = new File(keyFile);
            File publicKey = new File(keyFile + ".pub");

            if (privateKey.exists()) {
                try {
                    Set<PosixFilePermission> privateKeyPermissions = new HashSet<>();
                    privateKeyPermissions.add(PosixFilePermission.OWNER_READ);
                    privateKeyPermissions.add(PosixFilePermission.OWNER_WRITE);
                    Files.setPosixFilePermissions(privateKey.toPath(), privateKeyPermissions);
                    LOG.info("已设置私钥权限为0600");
                } catch (Exception e) {
                    LOG.warn("设置私钥权限失败: {}", e.getMessage());
                }
            }

            if (publicKey.exists()) {
                try {
                    Set<PosixFilePermission> publicKeyPermissions = new HashSet<>();
                    publicKeyPermissions.add(PosixFilePermission.OWNER_READ);
                    publicKeyPermissions.add(PosixFilePermission.OWNER_WRITE);
                    publicKeyPermissions.add(PosixFilePermission.GROUP_READ);
                    publicKeyPermissions.add(PosixFilePermission.OTHERS_READ);
                    Files.setPosixFilePermissions(publicKey.toPath(), publicKeyPermissions);
                    LOG.info("已设置公钥权限为0644");
                } catch (Exception e) {
                    LOG.warn("设置公钥权限失败: {}", e.getMessage());
                }
            }

            LOG.info("SSH密钥对生成成功: {}", keyFile);
            return true;
        } catch (Exception e) {
            LOG.error("生成SSH密钥对失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 读取公钥文件内容
     * 
     * @param publicKeyFile 公钥文件路径
     * @return 公钥内容
     */
    private static String readPublicKeyFile(String publicKeyFile) {
        try {
            return new String(Files.readAllBytes(Paths.get(publicKeyFile)), StandardCharsets.UTF_8).trim();
        } catch (Exception e) {
            LOG.error("读取公钥文件失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 创建临时公钥文件
     * 
     * @param publicKey 公钥内容
     * @return 临时文件路径
     */
    private static String createTempPublicKeyFile(String publicKey) {
        try {
            String tempFile = System.getProperty("java.io.tmpdir") + File.separator +
                    "datasophon_pubkey_" + System.currentTimeMillis() + ".pub";
            Files.write(Paths.get(tempFile), publicKey.getBytes(StandardCharsets.UTF_8));
            LOG.info("创建临时公钥文件: {}", tempFile);
            return tempFile;
        } catch (Exception e) {
            LOG.error("创建临时公钥文件失败: {}", e.getMessage());
            return null;
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
        return result.isSuccess() ? result.getOutput() : "EXIT_CODE_" + result.getExitCode() + ": " + result.getError();
    }
}