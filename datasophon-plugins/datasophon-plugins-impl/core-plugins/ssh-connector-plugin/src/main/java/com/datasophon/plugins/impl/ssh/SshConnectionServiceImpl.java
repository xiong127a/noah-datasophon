package com.datasophon.plugins.impl.ssh;

import com.datasophon.plugins.api.model.HostCheckContext;
import com.datasophon.plugins.api.model.CommandResult;
import com.datasophon.plugins.api.service.SshConnectionService;
import com.datasophon.plugins.ssh.service.SshConnectionPoolManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

/**
 * SSH连接服务实现
 * 
 * 基于Apache SSHJ + Commons Pool2的高性能SSH连接服务
 * 完全隔离SSH库依赖，提供统一的命令执行接口
 * 
 * @author DataSophon Team
 */
@Service
@Slf4j
public class SshConnectionServiceImpl implements SshConnectionService {
    
    // SSH连接池管理器
    private SshConnectionPoolManager poolManager;
    
    @PostConstruct
    public void init() {
        try {
            log.info("【SSH连接服务】初始化SSH连接服务...");
            poolManager = new SshConnectionPoolManager();
            poolManager.init();
            log.info("【SSH连接服务】SSH连接服务初始化完成");
        } catch (Exception e) {
            log.error("【SSH连接服务】初始化失败", e);
            throw new RuntimeException("SSH连接服务初始化失败", e);
        }
    }
    
    @PreDestroy
    public void cleanup() {
        try {
            log.info("【SSH连接服务】清理SSH连接服务...");
            if (poolManager != null) {
                poolManager.destroy();
            }
            log.info("【SSH连接服务】SSH连接服务清理完成");
        } catch (Exception e) {
            log.error("【SSH连接服务】清理失败", e);
        }
    }
    
    @Override
    public CommandResult executeCommand(HostCheckContext context, String command) {
        return executeCommand(context, command, 30); // 默认30秒超时
    }
    
    @Override
    public CommandResult executeCommand(HostCheckContext context, String command, long timeoutSeconds) {
        log.debug("【SSH连接服务】执行命令: {}@{}:{} -> {}, 超时: {}s", 
                context.getSshUser(), context.getHostIp(), context.getSshPort(), command, timeoutSeconds);
        
        try {
            if (poolManager == null) {
                throw new RuntimeException("SSH连接池管理器未初始化");
            }
            
            // 使用连接池管理器执行命令
            String output = poolManager.executeCommand(
                    context.getHostIp(), 
                    context.getSshPort(), 
                    context.getSshUser(), 
                    context.getSshPassword(), 
                    command);
            
            log.debug("【SSH连接服务】命令执行成功: {} -> {} chars", command, 
                    output != null ? output.length() : 0);
            
            return new CommandResult(command, 0, output != null ? output : "", "");
            
        } catch (Exception e) {
            log.error("【SSH连接服务】命令执行失败: {}@{}:{} -> {}, 错误: {}", 
                    context.getSshUser(), context.getHostIp(), context.getSshPort(), command, e.getMessage(), e);
            
            return new CommandResult(command, -1, "", 
                    e.getMessage() != null ? e.getMessage() : "命令执行异常");
        }
    }
    
    @Override
    public List<CommandResult> executeBatchCommands(HostCheckContext context, List<String> commands) {
        log.info("【SSH连接服务】批量执行命令: {}@{}:{}, 命令数量: {}", 
                context.getSshUser(), context.getHostIp(), context.getSshPort(), commands.size());
        
        List<CommandResult> results = new ArrayList<>();
        
        for (String command : commands) {
            try {
                CommandResult result = executeCommand(context, command);
                results.add(result);
                
                // 如果命令失败，记录但继续执行后续命令
                if (!result.isSuccess()) {
                    log.warn("【SSH连接服务】批量命令执行失败: {} -> 错误: {}", command, result.error());
                }
            } catch (Exception e) {
                log.error("【SSH连接服务】批量命令执行异常: {} -> 错误: {}", command, e.getMessage(), e);
                results.add(new CommandResult(command, -1, "", 
                        e.getMessage() != null ? e.getMessage() : "批量命令执行异常"));
            }
        }
        
        return results;
    }
    
    @Override
    public CommandResult testConnection(HostCheckContext context) {
        log.debug("【SSH连接服务】测试SSH连接: {}@{}:{}", 
                context.getSshUser(), context.getHostIp(), context.getSshPort());
        
        try {
            // 执行简单的测试命令
            CommandResult result = executeCommand(context, "echo 'connection_test'", 10);
            
            if (result.isSuccess() && result.output().contains("connection_test")) {
                return new CommandResult("connection_test", 0, 
                        "SSH连接测试成功", "");
            } else {
                return new CommandResult("connection_test", -1, "", 
                        "SSH连接测试失败，输出异常: " + result.output());
            }
            
        } catch (Exception e) {
            log.error("【SSH连接服务】连接测试异常: {}@{}:{}, 错误: {}", 
                    context.getSshUser(), context.getHostIp(), context.getSshPort(), e.getMessage(), e);
            
            return new CommandResult("connection_test", -1, "", 
                    "SSH连接测试异常: " + e.getMessage());
        }
    }
    
    @Override
    public Map<String, Object> getConnectionPoolStats(HostCheckContext context) {
        log.debug("【SSH连接服务】获取连接池统计: {}@{}:{}", 
                context.getSshUser(), context.getHostIp(), context.getSshPort());
        
        try {
            if (poolManager != null) {
                return poolManager.getPoolStats();
            } else {
                return Map.of("error", "SSH连接池管理器未初始化");
            }
        } catch (Exception e) {
            log.error("【SSH连接服务】获取连接池统计失败", e);
            return Map.of("error", e.getMessage());
        }
    }
    
    @Override
    public boolean isConnectionPoolHealthy(HostCheckContext context) {
        try {
            Map<String, Object> stats = getConnectionPoolStats(context);
            return !stats.containsKey("error");
        } catch (Exception e) {
            log.warn("【SSH连接服务】连接池健康检查失败", e);
            return false;
        }
    }
    
    @Override
    public void closeConnectionPool(HostCheckContext context) {
        log.info("【SSH连接服务】关闭连接池: {}@{}:{}", 
                context.getSshUser(), context.getHostIp(), context.getSshPort());
        
        try {
            if (poolManager != null) {
                poolManager.closePool(context.getHostIp(), context.getSshPort(), context.getSshUser());
            }
        } catch (Exception e) {
            log.error("【SSH连接服务】关闭连接池失败", e);
        }
    }
    
    @Override
    public Map<String, Object> getGlobalPoolStats() {
        log.debug("【SSH连接服务】获取全局连接池统计");
        
        try {
            if (poolManager != null) {
                return poolManager.getPoolStats();
            } else {
                return Map.of("error", "SSH连接池管理器未初始化");
            }
        } catch (Exception e) {
            log.error("【SSH连接服务】获取全局统计失败", e);
            return Map.of("error", e.getMessage());
        }
    }
    
    // ==================== 文件传输方法实现 ====================
    
    @Override
    public boolean uploadFile(HostCheckContext context, String localFilePath, String remoteFilePath) {
        if (context == null || localFilePath == null || remoteFilePath == null) {
            log.error("【SSH连接服务】上传文件参数不能为空");
            return false;
        }
        
        log.info("【SSH连接服务】上传文件: {}@{}:{} {} -> {}", 
                context.getSshUser(), context.getHostIp(), context.getSshPort(), localFilePath, remoteFilePath);
        
        try {
            if (poolManager == null) {
                log.error("【SSH连接服务】连接池管理器未初始化");
                return false;
            }
            
            return poolManager.executeWithConnection(
                context.getHostIp(), context.getSshPort(), 
                context.getSshUser(), context.getSshPassword(),
                sshClient -> {
                    try (var sftpClient = sshClient.newSFTPClient()) {
                        // 确保远程目录存在
                        ensureRemoteDirectoryExists(sftpClient, getParentPath(remoteFilePath));
                        
                        // 上传文件
                        sftpClient.put(localFilePath, remoteFilePath);
                        
                        log.info("【SSH连接服务】文件上传成功: {} -> {}:{}", 
                                localFilePath, context.getHostIp(), remoteFilePath);
                        return true;
                    }
                }
            );
            
        } catch (Exception e) {
            log.error("【SSH连接服务】文件上传失败: {}@{}:{} {} -> {}, 错误: {}", 
                    context.getSshUser(), context.getHostIp(), context.getSshPort(), 
                    localFilePath, remoteFilePath, e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public boolean uploadFileFromStream(HostCheckContext context, java.io.InputStream inputStream, String remoteFilePath) {
        if (context == null || inputStream == null || remoteFilePath == null) {
            log.error("【SSH连接服务】上传文件流参数不能为空");
            return false;
        }
        
        log.info("【SSH连接服务】上传文件流: {}@{}:{} -> {}", 
                context.getSshUser(), context.getHostIp(), context.getSshPort(), remoteFilePath);
        
        try {
            if (poolManager == null) {
                log.error("【SSH连接服务】连接池管理器未初始化");
                return false;
            }
            
            return poolManager.executeWithConnection(
                context.getHostIp(), context.getSshPort(), 
                context.getSshUser(), context.getSshPassword(),
                sshClient -> {
                    try (var sftpClient = sshClient.newSFTPClient()) {
                        // 确保远程目录存在
                        ensureRemoteDirectoryExists(sftpClient, getParentPath(remoteFilePath));
                        
                        // 上传文件流
                        try (var remoteFile = sftpClient.open(remoteFilePath, 
                                java.util.EnumSet.of(net.schmizz.sshj.sftp.OpenMode.WRITE, 
                                                   net.schmizz.sshj.sftp.OpenMode.CREAT, 
                                                   net.schmizz.sshj.sftp.OpenMode.TRUNC))) {
                            
                            byte[] buffer = new byte[8192];
                            int bytesRead;
                            long totalBytes = 0;
                            
                            while ((bytesRead = inputStream.read(buffer)) != -1) {
                                remoteFile.write(totalBytes, buffer, 0, bytesRead);
                                totalBytes += bytesRead;
                            }
                            
                            log.info("【SSH连接服务】文件流上传成功: {}:{}, 总大小: {} bytes", 
                                    context.getHostIp(), remoteFilePath, totalBytes);
                        }
                        
                        return true;
                    }
                }
            );
            
        } catch (Exception e) {
            log.error("【SSH连接服务】文件流上传失败: {}@{}:{} -> {}, 错误: {}", 
                    context.getSshUser(), context.getHostIp(), context.getSshPort(), 
                    remoteFilePath, e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public boolean downloadFile(HostCheckContext context, String remoteFilePath, String localFilePath) {
        if (context == null || remoteFilePath == null || localFilePath == null) {
            log.error("【SSH连接服务】下载文件参数不能为空");
            return false;
        }
        
        log.info("【SSH连接服务】下载文件: {}@{}:{} {} -> {}", 
                context.getSshUser(), context.getHostIp(), context.getSshPort(), remoteFilePath, localFilePath);
        
        try {
            if (poolManager == null) {
                log.error("【SSH连接服务】连接池管理器未初始化");
                return false;
            }
            
            return poolManager.executeWithConnection(
                context.getHostIp(), context.getSshPort(), 
                context.getSshUser(), context.getSshPassword(),
                sshClient -> {
                    try (var sftpClient = sshClient.newSFTPClient()) {
                        // 确保本地目录存在
                        java.io.File localFile = new java.io.File(localFilePath);
                        localFile.getParentFile().mkdirs();
                        
                        // 下载文件
                        sftpClient.get(remoteFilePath, localFilePath);
                        
                        log.info("【SSH连接服务】文件下载成功: {}:{} -> {}", 
                                context.getHostIp(), remoteFilePath, localFilePath);
                        return true;
                    }
                }
            );
            
        } catch (Exception e) {
            log.error("【SSH连接服务】文件下载失败: {}@{}:{} {} -> {}, 错误: {}", 
                    context.getSshUser(), context.getHostIp(), context.getSshPort(), 
                    remoteFilePath, localFilePath, e.getMessage(), e);
        return false;
        }
    }
    
    @Override
    public boolean createDirectory(HostCheckContext context, String remotePath) {
        if (context == null || remotePath == null) {
            log.error("【SSH连接服务】创建目录参数不能为空");
            return false;
        }
        
        log.info("【SSH连接服务】创建目录: {}@{}:{} -> {}", 
                context.getSshUser(), context.getHostIp(), context.getSshPort(), remotePath);
        
        try {
            if (poolManager == null) {
                log.error("【SSH连接服务】连接池管理器未初始化");
                return false;
            }
            
            return poolManager.executeWithConnection(
                context.getHostIp(), context.getSshPort(), 
                context.getSshUser(), context.getSshPassword(),
                sshClient -> {
                    try (var sftpClient = sshClient.newSFTPClient()) {
                        ensureRemoteDirectoryExists(sftpClient, remotePath);
                        
                        log.info("【SSH连接服务】目录创建成功: {}:{}", context.getHostIp(), remotePath);
                        return true;
                    }
                }
            );
            
        } catch (Exception e) {
            log.error("【SSH连接服务】目录创建失败: {}@{}:{} -> {}, 错误: {}", 
                    context.getSshUser(), context.getHostIp(), context.getSshPort(), 
                    remotePath, e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public boolean deleteFile(HostCheckContext context, String remoteFilePath) {
        if (context == null || remoteFilePath == null) {
            log.error("【SSH连接服务】删除文件参数不能为空");
            return false;
        }
        
        log.info("【SSH连接服务】删除文件: {}@{}:{} -> {}", 
                context.getSshUser(), context.getHostIp(), context.getSshPort(), remoteFilePath);
        
        try {
            if (poolManager == null) {
                log.error("【SSH连接服务】连接池管理器未初始化");
                return false;
            }
            
            return poolManager.executeWithConnection(
                context.getHostIp(), context.getSshPort(), 
                context.getSshUser(), context.getSshPassword(),
                sshClient -> {
                    try (var sftpClient = sshClient.newSFTPClient()) {
                        // 检查文件是否存在
                        if (!checkRemotePathExists(sftpClient, remoteFilePath)) {
                            log.warn("【SSH连接服务】文件不存在，无法删除: {}:{}", context.getHostIp(), remoteFilePath);
                            return false;
                        }
                        
                        // 删除文件
                        sftpClient.rm(remoteFilePath);
                        
                        log.info("【SSH连接服务】文件删除成功: {}:{}", context.getHostIp(), remoteFilePath);
                        return true;
                    }
                }
            );
            
        } catch (Exception e) {
            log.error("【SSH连接服务】文件删除失败: {}@{}:{} -> {}, 错误: {}", 
                    context.getSshUser(), context.getHostIp(), context.getSshPort(), 
                    remoteFilePath, e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public boolean checkPathExists(HostCheckContext context, String remotePath) {
        if (context == null || remotePath == null) {
            log.error("【SSH连接服务】路径检查参数不能为空");
            return false;
        }
        
        log.debug("【SSH连接服务】检查路径存在: {}@{}:{} -> {}", 
                context.getSshUser(), context.getHostIp(), context.getSshPort(), remotePath);
        
        try {
            if (poolManager == null) {
                log.error("【SSH连接服务】连接池管理器未初始化");
                return false;
            }
            
            return poolManager.executeWithConnection(
                context.getHostIp(), context.getSshPort(), 
                context.getSshUser(), context.getSshPassword(),
                sshClient -> {
                    try (var sftpClient = sshClient.newSFTPClient()) {
                        boolean exists = checkRemotePathExists(sftpClient, remotePath);
                        
                        log.debug("【SSH连接服务】路径检查: {}:{} -> {}", context.getHostIp(), remotePath, exists);
                        return exists;
                    }
                }
            );
            
        } catch (Exception e) {
            log.error("【SSH连接服务】路径检查失败: {}@{}:{} -> {}, 错误: {}", 
                    context.getSshUser(), context.getHostIp(), context.getSshPort(), 
                    remotePath, e.getMessage(), e);
            return false;
        }
    }
    
        @Override
    public boolean createFile(HostCheckContext context, String remoteFilePath) {
        if (context == null || remoteFilePath == null) {
            log.error("【SSH连接服务】创建文件参数不能为空");
            return false;
        }
        
        log.info("【SSH连接服务】创建文件: {}@{}:{} -> {}", 
                context.getSshUser(), context.getHostIp(), context.getSshPort(), remoteFilePath);
        
        try {
            if (poolManager == null) {
                log.error("【SSH连接服务】连接池管理器未初始化");
                return false;
            }
            
            return poolManager.executeWithConnection(
                context.getHostIp(), context.getSshPort(), 
                context.getSshUser(), context.getSshPassword(),
                sshClient -> {
                    try (var sftpClient = sshClient.newSFTPClient()) {
                        // 确保远程目录存在
                        ensureRemoteDirectoryExists(sftpClient, getParentPath(remoteFilePath));
                        
                        // 创建空文件
                        try (var remoteFile = sftpClient.open(remoteFilePath, 
                                java.util.EnumSet.of(net.schmizz.sshj.sftp.OpenMode.WRITE, 
                                                   net.schmizz.sshj.sftp.OpenMode.CREAT, 
                                                   net.schmizz.sshj.sftp.OpenMode.TRUNC))) {
                            // 不写入任何内容，创建空文件
                        }
                        
                        log.info("【SSH连接服务】空文件创建成功: {}:{}", context.getHostIp(), remoteFilePath);
                        return true;
                    }
                }
            );
            
            } catch (Exception e) {
            log.error("【SSH连接服务】文件创建失败: {}@{}:{} -> {}, 错误: {}", 
                    context.getSshUser(), context.getHostIp(), context.getSshPort(), 
                    remoteFilePath, e.getMessage(), e);
            return false;
        }
    }
    
    // ==================== 私有辅助方法 ====================
    
    /**
     * 确保远程目录存在
     */
    private void ensureRemoteDirectoryExists(net.schmizz.sshj.sftp.SFTPClient sftpClient, String remoteDirPath) throws java.io.IOException {
        if (remoteDirPath == null || remoteDirPath.trim().isEmpty() || "/".equals(remoteDirPath)) {
            return;
        }
        
        try {
            if (!checkRemotePathExists(sftpClient, remoteDirPath)) {
                // 递归创建父目录
                ensureRemoteDirectoryExists(sftpClient, getParentPath(remoteDirPath));
                
                // 创建当前目录
                sftpClient.mkdir(remoteDirPath);
                log.debug("【SSH连接服务】创建远程目录: {}", remoteDirPath);
            }
        } catch (Exception e) {
            log.error("【SSH连接服务】创建远程目录失败: {}", remoteDirPath, e);
            throw new java.io.IOException("创建远程目录失败: " + remoteDirPath, e);
        }
    }
    
    /**
     * 检查远程路径是否存在
     */
    private boolean checkRemotePathExists(net.schmizz.sshj.sftp.SFTPClient sftpClient, String remotePath) {
        try {
            sftpClient.statExistence(remotePath);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 获取路径的父目录
     */
    private String getParentPath(String path) {
        if (path == null || path.trim().isEmpty()) {
            return "/";
        }
        
        path = path.replace("\\", "/");
        int lastSlash = path.lastIndexOf('/');
        
        if (lastSlash <= 0) {
            return "/";
        }
        
        return path.substring(0, lastSlash);
    }
}