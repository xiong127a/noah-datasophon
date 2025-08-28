/*
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
 */

package com.datasophon.kubernetes.util;

import com.datasophon.common.Constants;
import com.datasophon.common.enums.UserEnum;
import com.datasophon.plugins.api.factory.SshConnectionServiceFactory;
import com.datasophon.plugins.api.service.SshConnectionService;
import com.datasophon.plugins.api.model.HostCheckContext;
import com.datasophon.plugins.api.model.CommandResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * Kubernetes SSH/SFTP utility class for remote operations.
 * 
 * 重构后的实现：通过SSH插件适配器提供SSH功能，完全隔离SSH库依赖
 * 设计原则：
 * 1. 所有SSH操作通过SSH插件适配器实现
 * 2. 保持与原有接口的兼容性
 * 3. 移除直接SSH库依赖
 * 4. 提供更好的错误处理和日志记录
 * 
 * @author DataSophon Team
 */
@Slf4j
public class KubernetesMinaUtils {

    private static final String DEFAULT_SSH_USER = "root";
    private static final int DEFAULT_SSH_PORT = 22;
    private static final String DEFAULT_SSH_PASSWORD = "defaultPassword"; // TODO: 从配置中获取
    
    /**
     * 获取SSH连接服务实例
     */
    private static SshConnectionService getSshConnectionService() {
        return SshConnectionServiceFactory.getInstance().getDefaultSshConnectionService();
    }
    
    /**
     * 构建HostCheckContext对象
     */
    private static HostCheckContext buildHostCheckContext(String hostname) {
        return HostCheckContext.builder()
                .hostIp(hostname)
                .sshPort(DEFAULT_SSH_PORT)
                .sshUser(DEFAULT_SSH_USER)
                .sshPassword(DEFAULT_SSH_PASSWORD)
                .build();
    }

    /**
     * Executes a command on a remote host and returns the result.
     *
     * @param hostname The hostname or IP address of the remote host
     * @param command  The command to execute
     * @return The command output as string, null if execution fails, or
     *         Constants.FAILED if exit status is not 0
     */
    public static String execCmdWithResult(String hostname, String command) {
        if (StringUtils.isAnyBlank(hostname, command)) {
            log.error("【Kubernetes SSH工具】主机名和命令不能为空");
            return null;
        }

        try {
            log.debug("【Kubernetes SSH工具】在主机 {} 上执行命令: {}", hostname, command);
            
            SshConnectionService sshService = getSshConnectionService();
            if (sshService == null) {
                log.error("【Kubernetes SSH工具】SSH连接服务不可用");
                return null;
            }
            
            HostCheckContext context = buildHostCheckContext(hostname);
            CommandResult result = sshService.executeCommand(context, command);
            
            if (result != null && result.isSuccess()) {
                String output = result.output().trim();
                log.debug("【Kubernetes SSH工具】命令执行成功: {}, 输出: {}", 
                        hostname, output.length() > 100 ? output.substring(0, 100) + "..." : output);
                return output;
            } else {
                String errorMsg = result != null ? result.error() : "未知错误";
                log.warn("【Kubernetes SSH工具】命令执行失败: {} -> {}, 错误: {}", 
                        hostname, command, errorMsg);
                return Constants.FAILED;
            }
            
        } catch (Exception e) {
            log.error("【Kubernetes SSH工具】命令执行异常: {} -> {}, 错误: {}", 
                    hostname, command, e.getMessage(), e);
            return null;
        }
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
            log.error("【Kubernetes SSH工具】参数不能为空: hostname={}, remotePath={}, inputFile={}", 
                    hostname, remotePath, inputFile);
            return false;
        }

        File uploadFile = new File(inputFile);
        if (!uploadFile.exists() || !uploadFile.isFile()) {
            log.error("【Kubernetes SSH工具】本地文件不存在或不是文件: {}", inputFile);
            return false;
        }

        log.info("【Kubernetes SSH工具】开始上传文件: {} -> {}:{}/{}", 
                inputFile, hostname, remotePath, uploadFile.getName());
        
        try {
            SshConnectionService sshService = getSshConnectionService();
            if (sshService == null) {
                log.error("【Kubernetes SSH工具】SSH连接服务不可用");
                return false;
            }
            
            HostCheckContext context = buildHostCheckContext(hostname);
            
            // 构建完整的远程文件路径（目录 + 文件名）
            String fullRemotePath = remotePath.endsWith("/") ? 
                    remotePath + uploadFile.getName() : remotePath + "/" + uploadFile.getName();
            
            boolean success = sshService.uploadFile(context, inputFile, fullRemotePath);
            
            if (success) {
                log.info("【Kubernetes SSH工具】文件上传成功: {} -> {}:{}", 
                        uploadFile.getName(), hostname, fullRemotePath);
            } else {
                log.error("【Kubernetes SSH工具】文件上传失败: {} -> {}:{}", 
                        inputFile, hostname, fullRemotePath);
            }
            
            return success;
            
        } catch (Exception e) {
            log.error("【Kubernetes SSH工具】文件上传异常: {} -> {}:{}, 错误: {}", 
                    inputFile, hostname, remotePath, e.getMessage(), e);
            return false;
        }
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
            log.error("【Kubernetes SSH工具】参数不能为空: hostname={}, remotePath={}, fileName={}, inputStream={}", 
                    hostname, remotePath, fileName, inputStream != null ? "非空" : "空");
            return false;
        }

        log.info("【Kubernetes SSH工具】开始上传文件流: {} -> {}:{}/{}", 
                "InputStream", hostname, remotePath, fileName);
        
        try {
            SshConnectionService sshService = getSshConnectionService();
            if (sshService == null) {
                log.error("【Kubernetes SSH工具】SSH连接服务不可用");
                return false;
            }
            
            HostCheckContext context = buildHostCheckContext(hostname);
            
            // 构建完整的远程文件路径（目录 + 文件名）
            String fullRemotePath = remotePath.endsWith("/") ? 
                    remotePath + fileName : remotePath + "/" + fileName;
            
            boolean success = sshService.uploadFileFromStream(context, inputStream, fullRemotePath);
            
            if (success) {
                log.info("【Kubernetes SSH工具】文件流上传成功: {} -> {}:{}", 
                        fileName, hostname, fullRemotePath);
            } else {
                log.error("【Kubernetes SSH工具】文件流上传失败: {} -> {}:{}", 
                        fileName, hostname, fullRemotePath);
            }
            
            return success;
            
        } catch (Exception e) {
            log.error("【Kubernetes SSH工具】文件流上传异常: {} -> {}:{}, 错误: {}", 
                    fileName, hostname, remotePath, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Creates a directory on a remote host.
     *
     * @param hostname The hostname or IP address of the remote host
     * @param path     The directory path to create
     */
    public static void createDir(String hostname, String path) {
        if (StringUtils.isAnyBlank(hostname, path)) {
            log.error("【Kubernetes SSH工具】参数不能为空: hostname={}, path={}", hostname, path);
            return;
        }

        log.info("【Kubernetes SSH工具】开始创建目录: {}:{}", hostname, path);
        
        try {
            SshConnectionService sshService = getSshConnectionService();
            if (sshService == null) {
                log.error("【Kubernetes SSH工具】SSH连接服务不可用");
                return;
            }
            
            HostCheckContext context = buildHostCheckContext(hostname);
            boolean success = sshService.createDirectory(context, path);
            
            if (success) {
                log.info("【Kubernetes SSH工具】目录创建成功: {}:{}", hostname, path);
            } else {
                log.error("【Kubernetes SSH工具】目录创建失败: {}:{}", hostname, path);
            }
            
        } catch (Exception e) {
            log.error("【Kubernetes SSH工具】目录创建异常: {}:{}, 错误: {}", 
                    hostname, path, e.getMessage(), e);
        }
    }

    /**
     * Creates an empty file on a remote host.
     *
     * @param hostname The hostname or IP address of the remote host
     * @param path     The file path to create
     */
    public static void createFile(String hostname, String path) {
        if (StringUtils.isAnyBlank(hostname, path)) {
            log.error("【Kubernetes SSH工具】参数不能为空: hostname={}, path={}", hostname, path);
            return;
        }

        log.info("【Kubernetes SSH工具】开始创建文件: {}:{}", hostname, path);
        
        try {
            SshConnectionService sshService = getSshConnectionService();
            if (sshService == null) {
                log.error("【Kubernetes SSH工具】SSH连接服务不可用");
                return;
            }
            
            HostCheckContext context = buildHostCheckContext(hostname);
            
            // 首先检查文件是否已经存在
            if (sshService.checkPathExists(context, path)) {
                log.debug("【Kubernetes SSH工具】文件已存在: {}:{}", hostname, path);
                return;
            }
            
            boolean success = sshService.createFile(context, path);
            
            if (success) {
                log.info("【Kubernetes SSH工具】文件创建成功: {}:{}", hostname, path);
            } else {
                log.error("【Kubernetes SSH工具】文件创建失败: {}:{}", hostname, path);
            }
            
        } catch (Exception e) {
            log.error("【Kubernetes SSH工具】文件创建异常: {}:{}, 错误: {}", 
                    hostname, path, e.getMessage(), e);
        }
    }

    /**
     * Deletes a file on a remote host.
     *
     * @param hostname The hostname or IP address of the remote host
     * @param path     The file path to delete
     */
    public static void deleteFile(String hostname, String path) {
        if (StringUtils.isAnyBlank(hostname, path)) {
            log.error("【Kubernetes SSH工具】参数不能为空: hostname={}, path={}", hostname, path);
            return;
        }

        log.info("【Kubernetes SSH工具】开始删除文件: {}:{}", hostname, path);
        
        try {
            SshConnectionService sshService = getSshConnectionService();
            if (sshService == null) {
                log.error("【Kubernetes SSH工具】SSH连接服务不可用");
                return;
            }
            
            HostCheckContext context = buildHostCheckContext(hostname);
            
            // 首先检查文件是否存在
            if (!sshService.checkPathExists(context, path)) {
                log.debug("【Kubernetes SSH工具】文件不存在，无需删除: {}:{}", hostname, path);
                return;
            }
            
            boolean success = sshService.deleteFile(context, path);
            
            if (success) {
                log.info("【Kubernetes SSH工具】文件删除成功: {}:{}", hostname, path);
            } else {
                log.error("【Kubernetes SSH工具】文件删除失败: {}:{}", hostname, path);
            }
            
        } catch (Exception e) {
            log.error("【Kubernetes SSH工具】文件删除异常: {}:{}, 错误: {}", 
                    hostname, path, e.getMessage(), e);
        }
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
            log.error("【Kubernetes SSH工具】参数不能为空: hostname={}, path={}", hostname, path);
            return false;
        }

        log.debug("【Kubernetes SSH工具】检查路径是否存在: {}:{}", hostname, path);
        
        try {
            SshConnectionService sshService = getSshConnectionService();
            if (sshService == null) {
                log.error("【Kubernetes SSH工具】SSH连接服务不可用");
                return false;
            }
            
            HostCheckContext context = buildHostCheckContext(hostname);
            boolean exists = sshService.checkPathExists(context, path);
            
            log.debug("【Kubernetes SSH工具】路径 {}:{} 存在: {}", hostname, path, exists);
            return exists;
            
        } catch (Exception e) {
            log.error("【Kubernetes SSH工具】路径检查异常: {}:{}, 错误: {}", 
                    hostname, path, e.getMessage(), e);
            return false;
        }
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
            throw new IllegalArgumentException("【Kubernetes SSH工具】参数不能为空: hostname, user, group");
        }

        Integer userId = UserEnum.getUserIdByUsername(user);
        Integer groupId = UserEnum.getGroupIdByGroupName(group);

        if (userId == null || groupId == null) {
            throw new IllegalArgumentException("【Kubernetes SSH工具】未找到用户或组的ID配置");
        }

        log.info("【Kubernetes SSH工具】开始创建用户和组: {}@{} (uid={}, gid={})", 
                user, hostname, userId, groupId);
        
        try {
            String command = String.format(
                    "if ! getent group %s > /dev/null; then groupadd -g %d %s; fi && " +
                            "if ! getent passwd %s > /dev/null; then useradd -m -u %d -g %d %s; fi",
                    group, groupId, group, user, userId, groupId, user);

            String result = execCmdWithResult(hostname, command);
            boolean success = result != null && !Constants.FAILED.equals(result);

            if (success) {
                log.info("【Kubernetes SSH工具】用户和组创建/验证成功: {} 和 {} 在 {}", user, group, hostname);
            } else {
                log.error("【Kubernetes SSH工具】用户和组创建失败: {} 和 {} 在 {}", user, group, hostname);
            }
            
        } catch (Exception e) {
            log.error("【Kubernetes SSH工具】用户和组创建异常: {}@{}, 错误: {}", 
                    user, hostname, e.getMessage(), e);
            throw new RuntimeException("用户和组创建失败", e);
        }
    }


}
