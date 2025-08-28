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
}