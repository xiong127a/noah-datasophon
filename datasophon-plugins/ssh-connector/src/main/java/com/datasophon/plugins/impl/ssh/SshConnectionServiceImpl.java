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

package com.datasophon.plugins.impl.ssh;

import com.datasophon.plugins.api.model.CommandResult;
import com.datasophon.plugins.api.model.HostCheckContext;
import com.datasophon.plugins.api.service.SshConnectionService;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.sftp.SFTPClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * SSH连接服务实现类
 * 基于Apache SSHJ + Commons Pool2实现高性能SSH连接服务
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-10-22
 */
public class SshConnectionServiceImpl implements SshConnectionService {
    
    private static final Logger logger = LoggerFactory.getLogger(SshConnectionServiceImpl.class);
    private static final int DEFAULT_TIMEOUT_SECONDS = 60;
    private static final int BUFFER_SIZE = 8192;
    
    public SshConnectionServiceImpl() {
        logger.info("SSH连接服务初始化完成");
    }
    
    @Override
    public CommandResult executeCommand(HostCheckContext context, String command) {
        return executeCommand(context, command, DEFAULT_TIMEOUT_SECONDS);
    }
    
    @Override
    public CommandResult executeCommand(HostCheckContext context, String command, long timeoutSeconds) {
        var config = buildSshConfig(context);
        SSHClient client = null;
        
        try {
            client = SshConnectionPool.borrowClient(config);
            return executeCommandInternal(client, command, timeoutSeconds);
            
        } catch (Exception e) {
            logger.error("执行SSH命令失败: host={}, command={}, error={}", 
                        context.getHostIp(), command, e.getMessage(), e);
            
            // 如果连接失败，销毁该连接
            if (client != null) {
                SshConnectionPool.invalidateClient(config, client);
                client = null;
            }
            
            return new CommandResult(command, -1, "", "执行失败: " + e.getMessage());
            
        } finally {
            // 归还连接
            if (client != null) {
                SshConnectionPool.returnClient(config, client);
            }
        }
    }
    
    /**
     * 内部执行命令方法
     */
    private CommandResult executeCommandInternal(SSHClient client, String command, long timeoutSeconds) 
            throws IOException {
        
        try (var session = client.startSession()) {
            session.allocateDefaultPTY(); // 分配伪终端
            
            var cmd = session.exec(command);
            
            // 读取输出
            var output = readStream(cmd.getInputStream());
            var error = readStream(cmd.getErrorStream());
            
            // 等待命令执行完成
            cmd.join(timeoutSeconds, TimeUnit.SECONDS);
            
            var exitStatus = cmd.getExitStatus();
            
            logger.debug("命令执行完成: command={}, exitCode={}, output length={}, error length={}", 
                        command, exitStatus, output.length(), error.length());
            
            return new CommandResult(command, exitStatus != null ? exitStatus : -1, output, error);
        }
    }
    
    /**
     * 读取流内容
     */
    private String readStream(InputStream inputStream) throws IOException {
        try (var bos = new ByteArrayOutputStream()) {
            var buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                bos.write(buffer, 0, bytesRead);
            }
            return bos.toString(StandardCharsets.UTF_8);
        }
    }
    
    @Override
    public List<CommandResult> executeBatchCommands(HostCheckContext context, List<String> commands) {
        var results = new ArrayList<CommandResult>();
        
        for (var command : commands) {
            results.add(executeCommand(context, command));
        }
        
        return results;
    }
    
    @Override
    public CommandResult testConnection(HostCheckContext context) {
        return executeCommand(context, "echo 'SSH connection test successful'", 10);
    }
    
    @Override
    public Map<String, Object> getConnectionPoolStats(HostCheckContext context) {
        var config = buildSshConfig(context);
        return SshConnectionPool.getPoolStats(config.getPoolKey());
    }
    
    @Override
    public boolean isConnectionPoolHealthy(HostCheckContext context) {
        var stats = getConnectionPoolStats(context);
        return !stats.containsKey("error");
    }
    
    @Override
    public void closeConnectionPool(HostCheckContext context) {
        var config = buildSshConfig(context);
        SshConnectionPool.closePool(config.getPoolKey());
    }
    
    @Override
    public Map<String, Object> getGlobalPoolStats() {
        return SshConnectionPool.getGlobalStats();
    }
    
    // ==================== 文件传输接口实现 ====================
    
    @Override
    public boolean uploadFile(HostCheckContext context, String localFilePath, String remoteFilePath) {
        var config = buildSshConfig(context);
        SSHClient client = null;
        
        try {
            client = SshConnectionPool.borrowClient(config);
            
            try (var sftp = client.newSFTPClient()) {
                // 确保远程目录存在
                var remoteDir = getParentPath(remoteFilePath);
                if (remoteDir != null) {
                    createDirectoryInternal(sftp, remoteDir);
                }
                
                // 上传文件
                sftp.put(localFilePath, remoteFilePath);
                logger.info("文件上传成功: {} -> {}", localFilePath, remoteFilePath);
                return true;
            }
            
        } catch (Exception e) {
            logger.error("文件上传失败: local={}, remote={}, error={}", 
                        localFilePath, remoteFilePath, e.getMessage(), e);
            
            if (client != null) {
                SshConnectionPool.invalidateClient(config, client);
                client = null;
            }
            
            return false;
            
        } finally {
            if (client != null) {
                SshConnectionPool.returnClient(config, client);
            }
        }
    }
    
    @Override
    public boolean uploadFileFromStream(HostCheckContext context, InputStream inputStream, String remoteFilePath) {
        var config = buildSshConfig(context);
        SSHClient client = null;
        
        try {
            client = SshConnectionPool.borrowClient(config);
            
            try (var sftp = client.newSFTPClient()) {
                // 确保远程目录存在
                var remoteDir = getParentPath(remoteFilePath);
                if (remoteDir != null) {
                    createDirectoryInternal(sftp, remoteDir);
                }
                
                // 使用 RemoteFile.write() 方法直接写入数据
                try (var remoteFile = sftp.open(remoteFilePath, 
                        java.util.EnumSet.of(
                            net.schmizz.sshj.sftp.OpenMode.WRITE,
                            net.schmizz.sshj.sftp.OpenMode.CREAT,
                            net.schmizz.sshj.sftp.OpenMode.TRUNC
                        ))) {
                    
                    var buffer = new byte[BUFFER_SIZE];
                    int bytesRead;
                    long fileOffset = 0;
                    
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        remoteFile.write(fileOffset, buffer, 0, bytesRead);
                        fileOffset += bytesRead;
                    }
                }
                
                logger.info("文件流上传成功: {}", remoteFilePath);
                return true;
            }
            
        } catch (Exception e) {
            logger.error("文件流上传失败: remote={}, error={}", remoteFilePath, e.getMessage(), e);
            
            if (client != null) {
                SshConnectionPool.invalidateClient(config, client);
                client = null;
            }
            
            return false;
            
        } finally {
            if (client != null) {
                SshConnectionPool.returnClient(config, client);
            }
        }
    }
    
    @Override
    public boolean uploadFileFromStream(HostCheckContext context, InputStream inputStream, 
                                       String remoteFilePath, long totalBytes, UploadProgressCallback progressCallback) {
        var config = buildSshConfig(context);
        SSHClient client = null;
        
        try {
            client = SshConnectionPool.borrowClient(config);
            
            try (var sftp = client.newSFTPClient()) {
                // 确保远程目录存在
                var remoteDir = getParentPath(remoteFilePath);
                if (remoteDir != null) {
                    createDirectoryInternal(sftp, remoteDir);
                }
                
                // 使用 RemoteFile.write() 方法直接写入数据（带进度回调）
                try (var remoteFile = sftp.open(remoteFilePath, 
                        java.util.EnumSet.of(
                            net.schmizz.sshj.sftp.OpenMode.WRITE,
                            net.schmizz.sshj.sftp.OpenMode.CREAT,
                            net.schmizz.sshj.sftp.OpenMode.TRUNC
                        ))) {
                    
                    var buffer = new byte[BUFFER_SIZE];
                    int bytesRead;
                    long uploadedBytes = 0;
                    long lastReportedBytes = 0;
                    long lastReportTime = System.currentTimeMillis();
                    
                    // 初始进度
                    if (progressCallback != null) {
                        progressCallback.onProgress(0, totalBytes, 0);
                    }
                    
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        remoteFile.write(uploadedBytes, buffer, 0, bytesRead);
                        uploadedBytes += bytesRead;
                        
                        // 每上传至少512KB或每500ms报告一次进度
                        long currentTime = System.currentTimeMillis();
                        if (progressCallback != null && 
                            (uploadedBytes - lastReportedBytes >= 512 * 1024 || 
                             currentTime - lastReportTime >= 500)) {
                            
                            int progress = totalBytes > 0 ? (int) (uploadedBytes * 100 / totalBytes) : 0;
                            progressCallback.onProgress(uploadedBytes, totalBytes, progress);
                            lastReportedBytes = uploadedBytes;
                            lastReportTime = currentTime;
                        }
                    }
                    
                    // 最终进度（确保100%）
                    if (progressCallback != null) {
                        progressCallback.onProgress(uploadedBytes, totalBytes, 100);
                    }
                }
                
                logger.info("文件流上传成功: {}, 大小: {} bytes", remoteFilePath, totalBytes);
                return true;
            }
            
        } catch (Exception e) {
            logger.error("文件流上传失败: remote={}, error={}", remoteFilePath, e.getMessage(), e);
            
            if (client != null) {
                SshConnectionPool.invalidateClient(config, client);
                client = null;
            }
            
            return false;
            
        } finally {
            if (client != null) {
                SshConnectionPool.returnClient(config, client);
            }
        }
    }
    
    @Override
    public boolean downloadFile(HostCheckContext context, String remoteFilePath, String localFilePath) {
        var config = buildSshConfig(context);
        SSHClient client = null;
        
        try {
            client = SshConnectionPool.borrowClient(config);
            
            try (var sftp = client.newSFTPClient()) {
                // 确保本地目录存在
                var localDir = Paths.get(localFilePath).getParent();
                if (localDir != null) {
                    Files.createDirectories(localDir);
                }
                
                // 下载文件
                sftp.get(remoteFilePath, localFilePath);
                logger.info("文件下载成功: {} -> {}", remoteFilePath, localFilePath);
                return true;
            }
            
        } catch (Exception e) {
            logger.error("文件下载失败: remote={}, local={}, error={}", 
                        remoteFilePath, localFilePath, e.getMessage(), e);
            
            if (client != null) {
                SshConnectionPool.invalidateClient(config, client);
                client = null;
            }
            
            return false;
            
        } finally {
            if (client != null) {
                SshConnectionPool.returnClient(config, client);
            }
        }
    }
    
    @Override
    public boolean createDirectory(HostCheckContext context, String remotePath) {
        var config = buildSshConfig(context);
        SSHClient client = null;
        
        try {
            client = SshConnectionPool.borrowClient(config);
            
            try (var sftp = client.newSFTPClient()) {
                createDirectoryInternal(sftp, remotePath);
                logger.info("目录创建成功: {}", remotePath);
                return true;
            }
            
        } catch (Exception e) {
            logger.error("目录创建失败: path={}, error={}", remotePath, e.getMessage(), e);
            
            if (client != null) {
                SshConnectionPool.invalidateClient(config, client);
                client = null;
            }
            
            return false;
            
        } finally {
            if (client != null) {
                SshConnectionPool.returnClient(config, client);
            }
        }
    }
    
    /**
     * 递归创建目录
     */
    private void createDirectoryInternal(SFTPClient sftp, String path) throws IOException {
        if (path == null || path.isEmpty() || "/".equals(path)) {
            return;
        }
        
        try {
            sftp.stat(path);
            // 目录已存在
        } catch (IOException e) {
            // 目录不存在，尝试创建
            var parent = getParentPath(path);
            if (parent != null) {
                createDirectoryInternal(sftp, parent);
            }
            
            sftp.mkdir(path);
        }
    }
    
    @Override
    public boolean deleteFile(HostCheckContext context, String remoteFilePath) {
        var config = buildSshConfig(context);
        SSHClient client = null;
        
        try {
            client = SshConnectionPool.borrowClient(config);
            
            try (var sftp = client.newSFTPClient()) {
                sftp.rm(remoteFilePath);
                logger.info("文件删除成功: {}", remoteFilePath);
                return true;
            }
            
        } catch (Exception e) {
            logger.error("文件删除失败: path={}, error={}", remoteFilePath, e.getMessage(), e);
            
            if (client != null) {
                SshConnectionPool.invalidateClient(config, client);
                client = null;
            }
            
            return false;
            
        } finally {
            if (client != null) {
                SshConnectionPool.returnClient(config, client);
            }
        }
    }
    
    @Override
    public boolean checkPathExists(HostCheckContext context, String remotePath) {
        var config = buildSshConfig(context);
        SSHClient client = null;
        
        try {
            client = SshConnectionPool.borrowClient(config);
            
            try (var sftp = client.newSFTPClient()) {
                sftp.stat(remotePath);
                return true;
            } catch (IOException e) {
                return false;
            }
            
        } catch (Exception e) {
            logger.error("检查路径失败: path={}, error={}", remotePath, e.getMessage(), e);
            
            if (client != null) {
                SshConnectionPool.invalidateClient(config, client);
                client = null;
            }
            
            return false;
            
        } finally {
            if (client != null) {
                SshConnectionPool.returnClient(config, client);
            }
        }
    }
    
    @Override
    public boolean createFile(HostCheckContext context, String remoteFilePath) {
        var config = buildSshConfig(context);
        SSHClient client = null;
        
        try {
            client = SshConnectionPool.borrowClient(config);
            
            try (var sftp = client.newSFTPClient()) {
                // 确保父目录存在
                var parent = getParentPath(remoteFilePath);
                if (parent != null) {
                    createDirectoryInternal(sftp, parent);
                }
                
                // 创建空文件
                try (var remoteFile = sftp.open(remoteFilePath, 
                        java.util.EnumSet.of(
                            net.schmizz.sshj.sftp.OpenMode.WRITE,
                            net.schmizz.sshj.sftp.OpenMode.CREAT,
                            net.schmizz.sshj.sftp.OpenMode.TRUNC
                        ))) {
                    // 文件已创建
                }
                
                logger.info("空文件创建成功: {}", remoteFilePath);
                return true;
            }
            
        } catch (Exception e) {
            logger.error("空文件创建失败: path={}, error={}", remoteFilePath, e.getMessage(), e);
            
            if (client != null) {
                SshConnectionPool.invalidateClient(config, client);
                client = null;
            }
            
            return false;
            
        } finally {
            if (client != null) {
                SshConnectionPool.returnClient(config, client);
            }
        }
    }
    
    // ==================== 私有辅助方法 ====================
    
    /**
     * 构建SSH配置
     */
    private SshConnectionPool.SshConfig buildSshConfig(HostCheckContext context) {
        return new SshConnectionPool.SshConfig(
            context.getHostIp(),
            context.getSshPort() != null ? context.getSshPort() : 22,
            context.getSshUser(),
            context.getSshPassword(),
            context.getPrivateKey(),
            context.getConnectionTimeout() != null ? context.getConnectionTimeout() : 30000,
            context.getCommandTimeout() != null ? context.getCommandTimeout() : 60000
        );
    }
    
    /**
     * 获取父路径
     */
    private String getParentPath(String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        
        var index = path.lastIndexOf('/');
        if (index <= 0) {
            return null;
        }
        
        return path.substring(0, index);
    }
}

