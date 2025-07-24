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

package com.datasophon.kubernetes.util;

import com.datasophon.common.Constants;
import com.datasophon.common.enums.UserEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ChannelExec;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.session.ClientSession;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Kubernetes SSH/SFTP utility class for remote operations.
 * Provides methods for executing commands, file transfer and management on
 * remote hosts.
 */
@Slf4j
public class KubernetesMinaUtils {

    private static final int COMMAND_TIMEOUT_SECONDS = 100000;
    private static final int BUFFER_SIZE = 1024;

    /**
     * Opens a SSH connection to a remote host.
     *
     * @param sshHost The hostname or IP address of the remote host
     * @param sshPort The SSH port number
     * @param sshUser The SSH username
     * @return A ClientSession if connection succeeds, null otherwise
     * @throws IOException If connection or authentication fails
     */
    public static ClientSession openConnection(String sshHost, Integer sshPort, String sshUser) throws IOException {
        if (StringUtils.isAnyBlank(sshHost, sshUser) || sshPort == null) {
            throw new IllegalArgumentException("SSH host, port, and user must not be null or empty");
        }

        ClientSession session;
        try (SshClient sshClient = SshClient.setUpDefaultClient()) {
            sshClient.start();
            String privateKeyPath = System.getProperty("user.home") + Constants.ID_RSA;
            try {
                log.debug("Attempting to connect to {}@{}:{} using private key: {}",
                        sshUser, sshHost, sshPort, privateKeyPath);

                String privateKeyContent = new String(Files.readAllBytes(Paths.get(privateKeyPath)));
                session = sshClient.connect(sshUser, sshHost, sshPort).verify().getClientSession();
                session.addPublicKeyIdentity(getKeyPairFromString(privateKeyContent));

                if (session.auth().verify().isFailure()) {
                    log.error("Authentication failed for {}@{}:{}", sshUser, sshHost, sshPort);
                    return null;
                }
            } catch (IOException e) {
                log.error("Failed to connect to {}@{}:{}: {}", sshUser, sshHost, sshPort, e.getMessage());
                throw new IOException("SSH connection failed", e);
            }
        }
        log.info("Successfully connected to {}@{}:{}", sshUser, sshHost, sshPort);
        return session;
    }

    /**
     * Creates a KeyPair from a private key string.
     * Note: This is a simplified implementation and should be improved for
     * production use.
     *
     * @param privateKeyContent The private key content as string
     * @return A KeyPair object
     * @throws RuntimeException If key generation fails
     */
    static KeyPair getKeyPairFromString(String privateKeyContent) {
        try {
            final KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            final KeyPair keyPair = keyGen.generateKeyPair();
            final ByteArrayOutputStream stream = new ByteArrayOutputStream();
            stream.write(privateKeyContent.getBytes());
            final ObjectOutputStream o = new ObjectOutputStream(stream);
            o.writeObject(keyPair);
            return keyPair;
        } catch (NoSuchAlgorithmException | IOException e) {
            log.error("Failed to generate key pair: {}", e.getMessage());
            throw new RuntimeException("Key pair generation failed", e);
        }
    }

    /**
     * Executes a command on a remote host and returns the result.
     *
     * @param hostname The hostname or IP address of the remote host
     * @param command  The command to execute
     * @return The command output as string, null if execution fails, or
     *         Constants.FAILED if exit status is 1
     */
    public static String execCmdWithResult(String hostname, String command) {
        if (StringUtils.isAnyBlank(hostname, command)) {
            log.error("Hostname and command must not be null or empty");
            return null;
        }

        return SshSftpUtil.withClientSession(hostname, session -> {
            session.resetAuthTimeout();
            log.debug("Executing command on {}: {}", hostname, command);

            try (ByteArrayOutputStream out = new ByteArrayOutputStream();
                    ByteArrayOutputStream err = new ByteArrayOutputStream();
                    ChannelExec ce = session.createExecChannel(command)) {

                ce.setOut(out);
                ce.setErr(err);

                ce.open();
                Set<ClientChannelEvent> events = ce.waitFor(
                        EnumSet.of(ClientChannelEvent.CLOSED),
                        TimeUnit.SECONDS.toMillis(COMMAND_TIMEOUT_SECONDS));

                if (events.contains(ClientChannelEvent.TIMEOUT)) {
                    log.error("Command execution timed out after {} seconds on {}: {}",
                            COMMAND_TIMEOUT_SECONDS, hostname, command);
                    throw new Exception("SSH command execution timed out");
                }

                int exitStatus = ce.getExitStatus();

                // If there's error output, log it at appropriate level
                String errorOutput = err.toString().trim();
                if (!errorOutput.isEmpty()) {
                    log.warn("Command on {} produced error output: {}", hostname, errorOutput);
                }

                if (exitStatus == 0) {
                    String result = out.toString().trim();
                    log.debug("Command execution successful on {}. Output: {}", hostname,
                            result.length() > 100 ? result.substring(0, 100) + "..." : result);
                    return result;
                } else {
                    log.warn("Command on {} failed with exit status {}", hostname, exitStatus);
                    return Constants.FAILED;
                }
            } catch (Exception e) {
                log.error("Failed to execute command on {}: {}", hostname, e.getMessage());
                return null;
            }
        });
    }

    /**
     * Uploads a file to a remote host.
     *
     * @param hostname   The hostname or IP address of the remote host
     * @param remotePath The remote directory path
     * @param inputFile  The local file path to upload
     * @return true if upload succeeds, false otherwise
     */
    public static boolean uploadFile(String hostname, String remotePath, String inputFile) {
        if (StringUtils.isAnyBlank(hostname, remotePath, inputFile)) {
            log.error("Hostname, remote path, and input file must not be null or empty");
            return false;
        }

        return SshSftpUtil.withSftpFileSystem(hostname, sftp -> {
            File uploadFile = new File(inputFile);
            if (!uploadFile.exists() || !uploadFile.isFile()) {
                log.error("Local file does not exist or is not a regular file: {}", inputFile);
                return false;
            }

            try (InputStream input = Files.newInputStream(uploadFile.toPath())) {
                Path path = sftp.getDefaultDir().resolve(remotePath);
                ensureDirectoryExists(path, hostname);

                Path file = path.resolve(uploadFile.getName());
                deleteFileIfExists(file, hostname);

                Files.copy(input, file);
                log.info("Successfully uploaded file {} to {}:{}",
                        uploadFile.getName(), hostname, remotePath);
                return true;
            } catch (IOException e) {
                log.error("Failed to upload file {} to {}:{}: {}",
                        inputFile, hostname, remotePath, e.getMessage());
                return false;
            }
        });
    }

    /**
     * Uploads a file from an input stream to a remote host.
     *
     * @param hostname    The hostname or IP address of the remote host
     * @param remotePath  The remote directory path
     * @param inputStream The input stream containing file data
     * @param fileName    The name to save the file as
     * @return true if upload succeeds, false otherwise
     */
    public static boolean uploadFile(String hostname, String remotePath, InputStream inputStream, String fileName) {
        if (StringUtils.isAnyBlank(hostname, remotePath, fileName) || inputStream == null) {
            log.error("Hostname, remote path, input stream, and file name must not be null or empty");
            return false;
        }

        return SshSftpUtil.withSftpFileSystem(hostname, sftp -> {
            try {
                Path path = sftp.getDefaultDir().resolve(remotePath);
                ensureDirectoryExists(path, hostname);

                Path file = path.resolve(fileName);
                deleteFileIfExists(file, hostname);

                try (OutputStream outputStream = Files.newOutputStream(file)) {
                    byte[] buffer = new byte[BUFFER_SIZE];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                }

                log.info("Successfully uploaded stream as file {} to {}:{}",
                        fileName, hostname, remotePath);
                return true;
            } catch (IOException e) {
                log.error("Failed to upload stream as file {} to {}:{}: {}",
                        fileName, hostname, remotePath, e.getMessage());
                return false;
            }
        });
    }

    /**
     * Creates a directory on a remote host.
     *
     * @param hostname The hostname or IP address of the remote host
     * @param path     The directory path to create
     */
    public static void createDir(String hostname, String path) {
        if (StringUtils.isAnyBlank(hostname, path)) {
             log.error("Hostname and path must not be null or empty for createDir operation");
            return;
        }

        SshSftpUtil.withSftpFileSystem(hostname, sftp -> {
            try {
                Path remoteRoot = sftp.getDefaultDir().resolve(path);
                ensureDirectoryExists(remoteRoot, hostname);
                return true;
            } catch (IOException e) {
                log.error("Failed to create directory at {}:{}: {}", hostname, path, e.getMessage());
                return false;
            }
        });
    }

    /**
     * Creates an empty file on a remote host.
     *
     * @param hostname The hostname or IP address of the remote host
     * @param path     The file path to create
     */
    public static void createFile(String hostname, String path) {
        if (StringUtils.isAnyBlank(hostname, path)) {
            log.error("Hostname and path must not be null or empty");
            return;
        }

        SshSftpUtil.withSftpFileSystem(hostname, sftp -> {
            Path remoteFile = sftp.getPath(path);
            try {
                if (!Files.exists(remoteFile)) {
                    Files.createFile(remoteFile);
                    log.info("Successfully created file at {}:{}", hostname, path);
                } else {
                    log.debug("File already exists at {}:{}", hostname, path);
                }
                return true;
            } catch (IOException e) {
                log.error("Failed to create file at {}:{}: {}", hostname, path, e.getMessage());
                return false;
            }
        });
    }

    /**
     * Deletes a file on a remote host.
     *
     * @param hostname The hostname or IP address of the remote host
     * @param path     The file path to delete
     */
    public static void deleteFile(String hostname, String path) {
        if (StringUtils.isAnyBlank(hostname, path)) {
            log.error("Hostname and path must not be null or empty for deleteFile operation");
            return;
        }

        SshSftpUtil.withSftpFileSystem(hostname, sftp -> {
            try {
                Path remoteFile = sftp.getPath(path);
                if (Files.exists(remoteFile) && Files.isRegularFile(remoteFile)) {
                    Files.delete(remoteFile);
                    log.info("Successfully deleted file at {}:{}", hostname, path);
                    return true;
                } else {
                    log.debug("File does not exist or is not a regular file at {}:{}", hostname, path);
                    return false;
                }
            } catch (IOException e) {
                log.error("Failed to delete file at {}:{}: {}", hostname, path, e.getMessage());
                return false;
            }
        });
    }

    /**
     * Checks if a path exists on a remote host.
     *
     * @param hostname The hostname or IP address of the remote host
     * @param path     The path to check
     * @return true if the path exists, false otherwise
     */
    public static boolean checkPathExists(String hostname, String path) {
        if (StringUtils.isAnyBlank(hostname, path)) {
            log.error("Hostname and path must not be null or empty for checkPathExists operation");
            return false;
        }

        return SshSftpUtil.withSftpFileSystem(hostname, sftp -> {
            try {
                Path remoteRoot = sftp.getDefaultDir().resolve(path);
                boolean exists = Files.exists(remoteRoot);
                log.debug("Path {}:{} exists: {}", hostname, path, exists);
                return exists;
            } catch (Exception e) {
                log.error("Failed to check path existence at {}:{}: {}", hostname, path, e.getMessage());
                return false;
            }
        });
    }

    /**
     * Reads the last N rows from a file.
     * Similar to the Linux 'tail' command.
     *
     * @param filename The file path to read from
     * @param charset  The character set to use, null for platform default
     * @param rows     The number of rows to read
     * @return The last N rows of the file as a string
     * @throws IOException If file reading fails
     */
    public static String readLastRows(String filename, Charset charset, int rows) throws IOException {
        if (StringUtils.isBlank(filename) || rows <= 0) {
            throw new IllegalArgumentException("Filename must not be null or empty and rows must be positive");
        }

        charset = charset == null ? Charset.defaultCharset() : charset;
        byte[] lineSeparator = System.lineSeparator().getBytes();

        try (RandomAccessFile rf = new RandomAccessFile(filename, "r")) {
            // Each read should match the line separator size
            byte[] c = new byte[lineSeparator.length];
            // Navigate from file end until we find the requested number of lines
            for (long pointer = rf.length(), lineSeparatorNum = 0; pointer >= 0 && lineSeparatorNum < rows;) {
                rf.seek(pointer--);
                int readLength = rf.read(c);
                if (readLength != -1 && Arrays.equals(lineSeparator, c)) {
                    lineSeparatorNum++;
                }
                // If we reach the start of file but haven't found enough line separators
                if (pointer == -1 && lineSeparatorNum < rows) {
                    rf.seek(0);
                }
            }
            byte[] tempbytes = new byte[(int) (rf.length() - rf.getFilePointer())];
            rf.readFully(tempbytes);
            return new String(tempbytes, charset);
        } catch (IOException e) {
            log.error("Failed to read last {} rows from file {}: {}", rows, filename, e.getMessage());
            throw e;
        }
    }

    /**
     * Creates a user and group on a remote host if they don't already exist.
     *
     * @param hostname The hostname or IP address of the remote host
     * @param user     The username to create
     * @param group    The group name to create
     * @throws IllegalArgumentException If user or group IDs are not found
     */
    public static void createUserAndGroup(String hostname, String user, String group) {
        if (StringUtils.isAnyBlank(hostname, user, group)) {
            throw new IllegalArgumentException("Hostname, user, and group must not be null or empty");
        }

        Integer userId = UserEnum.getUserIdByUsername(user);
        Integer groupId = UserEnum.getGroupIdByGroupName(group);

        if (userId == null || groupId == null) {
            throw new IllegalArgumentException("User or group ID not found");
        }

        SshSftpUtil.withSftpFileSystem(hostname, sftp -> {
            String command = String.format(
                    "if ! getent group %s > /dev/null; then groupadd -g %d %s; fi && " +
                            "if ! getent passwd %s > /dev/null; then useradd -m -u %d -g %d %s; fi",
                    group, groupId, group, user, userId, groupId, user);

            String result = execCmdWithResult(hostname, command);
            boolean success = result != null && !Constants.FAILED.equals(result);

            if (success) {
                log.info("Successfully created/verified user {} and group {} on {}", user, group, hostname);
            } else {
                log.error("Failed to create user {} and group {} on {}", user, group, hostname);
            }

            return success;
        });
    }

    // Helper methods to reduce code duplication

    /**
     * Ensures a directory exists on the remote host, creating it if necessary.
     *
     * @param path     The path to ensure exists
     * @param hostname The hostname (for logging purposes)
     * @throws IOException If directory creation fails
     */
    private static void ensureDirectoryExists(Path path, String hostname) throws IOException {
        if (!Files.exists(path)) {
            log.debug("Creating directory at {}:{}", hostname, path);
            Files.createDirectories(path);
        }
    }

    /**
     * Deletes a file if it exists on the remote host.
     *
     * @param file     The file path to delete
     * @param hostname The hostname (for logging purposes)
     * @throws IOException If file deletion fails
     */
    private static void deleteFileIfExists(Path file, String hostname) throws IOException {
        if (Files.exists(file)) {
            log.debug("Deleting existing file at {}:{}", hostname, file);
            Files.deleteIfExists(file);
        }
    }
}
