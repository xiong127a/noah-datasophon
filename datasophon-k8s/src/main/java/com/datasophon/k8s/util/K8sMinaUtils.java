/*
 *
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.datasophon.k8s.util;

import com.datasophon.common.Constants;
import com.datasophon.common.enums.UserEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ChannelExec;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.sftp.client.fs.SftpFileSystem;
import org.apache.sshd.sftp.client.fs.SftpFileSystemProvider;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
public class K8sMinaUtils {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(K8sMinaUtils.class);

    /**
     * 打开远程会话
     */
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
            throw new RuntimeException(e);
        }
        LOG.info(sshHost + " 连接成功");
        return session;
    }

    /**
     * 关闭远程会话
     */
    public static void closeConnection(ClientSession session) {
        try {
            session.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取密钥对
     */
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
     * @param command 命令
     * @return 结果
     */
    public static String execCmdWithResult(String hostname, String command) {
        return SshSftpUtil.withClientSession(hostname, session -> {
            session.resetAuthTimeout();
            LOG.info("exe cmd: {}", command);

            // 命令返回的结果
            // 返回结果流
            try (ByteArrayOutputStream out = new ByteArrayOutputStream();
                 ByteArrayOutputStream err = new ByteArrayOutputStream();
                 ChannelExec ce = session.createExecChannel(command)) {

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
                    return Constants.FAILED;
                }

                LOG.info("exe cmd return : {}", out);
                return out.toString().trim();

            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        });
    }

    /**
     * 上传文件,相同路径ui覆盖
     *
     * @param remotePath 远程目录地址
     * @param inputFile  文件 File
     */
    public static boolean uploadFile(String hostname, String remotePath, String inputFile) {
        return SshSftpUtil.withSftpFileSystem(hostname, sftp -> {
            File uploadFile = new File(inputFile);
            try (InputStream input = Files.newInputStream(uploadFile.toPath())) {
                Path path = sftp.getDefaultDir().resolve(remotePath);
                if (!Files.exists(path)) {
                    LOG.info("create pathHome {} ", path);
                    Files.createDirectories(path);
                }

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
        });
    }

    public static boolean uploadFile(String hostname, String remotePath, InputStream inputStream, String fileName) {
        return SshSftpUtil.withSftpFileSystem(hostname, sftp -> {
            try {
                Path path = sftp.getDefaultDir().resolve(remotePath);
                if (!Files.exists(path)) {
                    LOG.info("create pathHome {} ", path);
                    Files.createDirectories(path);
                }

                Path file = path.resolve(fileName);
                if (Files.exists(file)) {
                    LOG.info("delete file  {}", file);
                    Files.deleteIfExists(file);
                }

                // Upload directly from InputStream
                try (OutputStream outputStream = Files.newOutputStream(file)) {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                }

                LOG.info("file upload success");
                return true;

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * 下载文件
     * @param hostname
     * @param remotePath
     * @param fileName
     * @return
     */
    public static InputStream downloadFile(String hostname, String remotePath, String fileName) {
        return SshSftpUtil.withSftpFileSystem(hostname, sftp -> {
            try {
                // 拼接远程文件的完整路径
                Path remoteFile = sftp.getDefaultDir().resolve(remotePath).resolve(fileName);

                // 检查远程文件是否存在
                if (!Files.exists(remoteFile)) {
                    LOG.error("Remote file does not exist: {}", remoteFile);
                    return null;  // 返回null表示文件不存在
                }

                // 返回远程文件的输入流
                return Files.newInputStream(remoteFile);

            } catch (IOException e) {
                LOG.error("File download failed", e);
                throw new RuntimeException(e);
            }
        });
    }


    /**
     * 创建目录
     */
    public static boolean createDir(String hostname, String path) throws IOException {
        return SshSftpUtil.withSftpFileSystem(hostname, sftp -> {
            Path remoteRoot = sftp.getDefaultDir().resolve(path);
            if (!Files.exists(remoteRoot)) {
                Files.createDirectories(remoteRoot);
            }
            return true;
        });
    }

    public static boolean createFile(String hostname, String path) {
        return SshSftpUtil.withSftpFileSystem(hostname, sftp -> {
            Path remoteFile = sftp.getPath(path);
            try {
                if (!Files.exists(remoteFile)) {
                    Files.createFile(remoteFile);
                }
                return true;
            } catch (IOException e) {
                log.error("Failed to create file at {}: {}", path, e.getMessage());
                return false;
            }
        });
    }


    public static boolean deleteFile(String hostname, String path) {
        return SshSftpUtil.withSftpFileSystem(hostname, sftp -> {
            try {
                Path remoteFile = sftp.getPath(path);
                if (Files.exists(remoteFile) && Files.isRegularFile(remoteFile)) {
                    Files.delete(remoteFile);
                    return true;
                }
            } catch (IOException e) {
                log.error("Failed to delete file at {}: {}", path, e.getMessage());
            }
            return false;
        });
    }


    public static boolean writeUtf8String(String hostname, String content, String remoteFilePath) {
        return SshSftpUtil.withSftpFileSystem(hostname, sftp -> {
            try {
                Path remoteFile = sftp.getPath(remoteFilePath);
                Path parentDir = remoteFile.getParent();

                if (parentDir != null && !Files.exists(parentDir)) {
                    Files.createDirectories(parentDir);
                }

                Files.write(remoteFile, content.replace("\r\n", "\n").getBytes(StandardCharsets.UTF_8));
                return true;
            } catch (IOException e) {
                log.error("Failed to write content to file at {}: {}", remoteFilePath, e.getMessage());
                return false;
            }
        });
    }

    public static boolean deleteDirectory(SftpFileSystem sftp, String path) {
        try {
            Path remoteDir = sftp.getPath(path);
            if (Files.exists(remoteDir) && Files.isDirectory(remoteDir)) {
                Files.walkFileTree(remoteDir, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    }
                });
                return true;
            }
        } catch (IOException e) {
            log.error("Failed to delete directory at {}: {}", path, e.getMessage());
        }
        return false;
    }

    public static boolean checkPathExists(String hostname, String path) {
        return SshSftpUtil.withSftpFileSystem(hostname, sftp -> {
            try {
                Path remoteRoot = sftp.getDefaultDir().resolve(path);
                return Files.exists(remoteRoot);
            } catch (Exception e) {
                log.error("Failed to check path existence at {}: {}", path, e.getMessage());
                return false;
            }
        });
    }

    public static void checkParentPath(String hostname, String path) {
        SshSftpUtil.withSftpFileSystem(hostname, sftp -> {
            try {
                Path remotePath = sftp.getDefaultDir().resolve(path);
                Path parentPath = remotePath.getParent();

                if (!checkPathExists(hostname, parentPath.toString())) {
                    createDir(hostname, parentPath.toString());
                    execCmdWithResult(hostname, "chmod 777 " + parentPath);
                }
            } catch (Exception e) {
                log.error("Failed to check or create path at {}: {}", path, e.getMessage());
            }
            return null;
        });
    }

    public static boolean isDirectory(String hostname, String path) {
        return SshSftpUtil.withSftpFileSystem(hostname, sftp -> {
            try {
                Path remoteRoot = sftp.getDefaultDir().resolve(path);
                return Files.isDirectory(remoteRoot);
            } catch (Exception e) {
                log.error("Failed to check if path is a directory at {}: {}", path, e.getMessage());
                return false;
            }
        });
    }

    public static void checkSession(ClientSession session) {
        if (session == null || !session.isOpen()) {
            throw new RuntimeException("SSH session is not open or has been closed.");
        }
    }

    /**
     * 读取文件最后几行 <br>
     * 相当于Linux系统中的tail命令 读取大小限制是2GB
     *
     * @param filename 文件名
     * @param charset  文件编码格式,传null默认使用defaultCharset
     * @param rows     读取行数
     * @throws IOException IOException
     */
    public static String readLastRows(String filename, Charset charset, int rows) throws IOException {
        charset = charset == null ? Charset.defaultCharset() : charset;
        byte[] lineSeparator = System.getProperty("line.separator").getBytes();
        try (RandomAccessFile rf = new RandomAccessFile(filename, "r")) {
            // 每次读取的字节数要和系统换行符大小一致
            byte[] c = new byte[lineSeparator.length];
            // 在获取到指定行数和读完文档之前,从文档末尾向前移动指针,遍历文档每一个字节
            for (long pointer = rf.length(), lineSeparatorNum = 0; pointer >= 0 && lineSeparatorNum < rows; ) {
                // 移动指针
                rf.seek(pointer--);
                // 读取数据
                int readLength = rf.read(c);
                if (readLength != -1 && Arrays.equals(lineSeparator, c)) {
                    lineSeparatorNum++;
                }
                // 扫描完依然没有找到足够的行数,将指针归0
                if (pointer == -1 && lineSeparatorNum < rows) {
                    rf.seek(0);
                }
            }
            byte[] tempbytes = new byte[(int) (rf.length() - rf.getFilePointer())];
            rf.readFully(tempbytes);
            return new String(tempbytes, charset);
        }
    }

    public static String readLastRows(String hostname, String remoteFilePath, Charset charset, int rows) {
        return SshSftpUtil.withSftpFileSystem(hostname, sftp -> {
            //charset = charset == null ? Charset.defaultCharset() : charset;

            String linuxLineSeparator = "\n";
            byte[] lineSeparator = linuxLineSeparator.getBytes(charset);

            SftpFileSystemProvider provider = (SftpFileSystemProvider) sftp.provider();
            Path filePath = sftp.getPath(remoteFilePath);

            BasicFileAttributes attrs = provider.readAttributes(filePath, BasicFileAttributes.class);
            long fileSize = attrs.size();

            final int bufferSize = 8192;
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            int lineSeparatorCount = 0;
            long position = fileSize;

            try (SeekableByteChannel channel = provider.newByteChannel(filePath, EnumSet.of(StandardOpenOption.READ))) {
                while (position > 0 && lineSeparatorCount < rows) {
                    long start = Math.max(position - bufferSize, 0);
                    ByteBuffer buffer = ByteBuffer.allocate((int) (position - start));
                    channel.position(start);
                    channel.read(buffer);
                    buffer.flip();

                    for (int i = buffer.limit() - 1; i >= 0; i--) {
                        byte b = buffer.get(i);
                        byteArrayOutputStream.write(b);
                        if (b == lineSeparator[lineSeparator.length - 1] && checkLineSeparator(buffer, lineSeparator, i)) {
                            lineSeparatorCount++;
                            if (lineSeparatorCount >= rows) break;
                        }
                    }
                    position = start;
                }
            }

            byteArrayOutputStream.flush();
            byte[] byteArray = byteArrayOutputStream.toByteArray();

            return new StringBuilder(new String(byteArray, charset)).reverse().toString();
        });
    }

    private static boolean checkLineSeparator(ByteBuffer buffer, byte[] lineSeparator, int index) {
        // Simple check for line separator
        for (int i = 0; i < lineSeparator.length; i++) {
            if (index - i < 0 || buffer.get(index - i) != lineSeparator[lineSeparator.length - 1 - i]) {
                return false;
            }
        }
        return true;
    }

    public static void createUserAndGroup(String hostname, String user, String group) throws IOException {
        Integer userId = UserEnum.getUserIdByUsername(user);
        Integer groupId = UserEnum.getGroupIdByGroupName(group);

        if (userId == null || groupId == null) {
            throw new IllegalArgumentException("User or group ID not found.");
        }

        SshSftpUtil.withSftpFileSystem(hostname, sftp -> {
            String command = String.format(
                    "if ! getent group %s > /dev/null; then groupadd -g %d %s; fi && " +
                            "if ! getent passwd %s > /dev/null; then useradd -m -u %d -g %d %s; fi",
                    group, groupId, group, user, userId, groupId, user
            );

            execCmdWithResult(hostname, command);

            return true;
        });
    }

}
