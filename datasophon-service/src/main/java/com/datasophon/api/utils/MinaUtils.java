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

package com.datasophon.api.utils;

import com.datasophon.common.Constants;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ChannelExec;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.sftp.client.SftpClientFactory;
import org.apache.sshd.sftp.client.fs.SftpFileSystem;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.sshd.sftp.client.fs.SftpPath;
import org.slf4j.LoggerFactory;

public class MinaUtils {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(MinaUtils.class);

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
     * @param session 连接
     * @param command 命令
     * @return 结果
     */
    public static String execCmdWithResult(ClientSession session, String command) {
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
                return "failed";
            }

            LOG.info("exe cmd return : {}", out);
            return out.toString().trim();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
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
     * @param session    连接
     * @param remotePath 远程目录地址
     * @param inputFile  文件 File
     */
    public static boolean uploadFile(ClientSession session, String remotePath, String inputFile) {
        File uploadFile = new File(inputFile);

        try (SftpFileSystem sftp = SftpClientFactory.instance().createSftpFileSystem(session);
             InputStream input = Files.newInputStream(uploadFile.toPath())) {

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
    }

    /**
     * 创建目录
     *
     * @param path
     * @return
     */
    public static boolean createDir(ClientSession session, String path) {
        try (SftpFileSystem sftp = SftpClientFactory.instance().createSftpFileSystem(session)) {
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

    public static boolean createFile(ClientSession session, String path) {
        try (SftpFileSystem sftp = SftpClientFactory.instance().createSftpFileSystem(session)) {
            Path remoteFile = sftp.getPath(path);
            if (!Files.exists(remoteFile)) {
                Files.createFile(remoteFile);
                return true;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return false;
    }

    public static boolean deleteFile(ClientSession session, String path) {
        try (SftpFileSystem sftp = SftpClientFactory.instance().createSftpFileSystem(session)) {
            Path remoteFile = sftp.getPath(path);
            if (Files.exists(remoteFile) && Files.isRegularFile(remoteFile)) {
                Files.delete(remoteFile);
                return true;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return false;
    }

    public static boolean writeUtf8String(ClientSession session, String content, String remoteFilePath) {
        try (SftpFileSystem sftp = SftpClientFactory.instance().createSftpFileSystem(session)) {
            Path remoteFile = sftp.getPath(remoteFilePath);
            Path parentDir = remoteFile.getParent();

            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }

            Files.write(remoteFile, content.getBytes(StandardCharsets.UTF_8));
            return true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean deleteDirectory(ClientSession session, String path) {
        try (SftpFileSystem sftp = SftpClientFactory.instance().createSftpFileSystem(session)) {
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
            throw new RuntimeException(e);
        }
        return false;
    }

    public static boolean checkDirExists(ClientSession session, String path) {
        try (SftpFileSystem sftp = SftpClientFactory.instance().createSftpFileSystem(session)) {
            Path remoteRoot = sftp.getDefaultDir().resolve(path);
            return Files.exists(remoteRoot);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean isDirectory(ClientSession session, String path) {
        try (SftpFileSystem sftp = SftpClientFactory.instance().createSftpFileSystem(session)) {
            Path remoteRoot = sftp.getDefaultDir().resolve(path);
            return Files.isDirectory(remoteRoot);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
//        ClientSession session = MinaUtils.openConnection("192.168.1.203", 22, "root");

//        for (int i = 0; i < Constants.TEN; i++) {
//            String ls = MinaUtils.execCmdWithResult(session, "arch");
//            System.out.println(ls);
//        }
//
//        String path = "/opt/test";
//        String filePath = "/opt/test/aaa.txt";
//        String content = "aaa";

//        try (SftpFileSystem sftp = SftpClientFactory.instance().createSftpFileSystem(session)) {
//            Path remoteDir = sftp.getDefaultDir().resolve(path);
//            SftpPath remoteFile = sftp.getPath(filePath);
//
//            if (!Files.exists(remoteDir)) {
//                Files.createDirectories(remoteDir);
//            }
//            if (!Files.exists(remoteFile)) {
//                Files.createFile(remoteFile);
//                execCmdWithResult(session, "chmod 775 " + filePath);
//            }
//
//            Files.write(remoteFile, content.getBytes(StandardCharsets.UTF_8));
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }

//        deleteFile(session, filePath);
//        deleteDirectory(session, path);

//        Class<ClientSession> clientSessionClass = ClientSession.class;
//        boolean implementsAutoCloseable = AutoCloseable.class.isAssignableFrom(clientSessionClass);
//        System.out.println("Implements AutoCloseable: " + implementsAutoCloseable);
//
//        Class<SftpFileSystem> sftpFileSystemClass = SftpFileSystem.class;
//        boolean implementsClose = AutoCloseable.class.isAssignableFrom(sftpFileSystemClass);
//        System.out.println(implementsClose);
//
//        Class<MinaUtils> minaUtilsClass = MinaUtils.class;
//        boolean impClose = AutoCloseable.class.isAssignableFrom(minaUtilsClass);
//        System.out.println(impClose);
    }
}
