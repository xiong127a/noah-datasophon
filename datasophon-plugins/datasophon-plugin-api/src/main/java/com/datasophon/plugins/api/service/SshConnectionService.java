package com.datasophon.plugins.api.service;

import com.datasophon.plugins.api.model.HostCheckContext;
import com.datasophon.plugins.api.model.CommandResult;

import java.util.List;
import java.util.Map;

/**
 * SSH连接服务接口
 * 为插件提供统一的SSH连接管理和命令执行
 * 
 * 设计原则：
 * 1. 完全隔离SSH库依赖，不暴露具体的SSH客户端类型
 * 2. 基于Apache SSHJ + Commons Pool2实现高性能连接池
 * 3. 提供简洁的命令执行接口，自动管理连接生命周期
 * 4. 支持连接池监控和管理
 * 
 * @author DataSophon Team
 */
public interface SshConnectionService {
    
    /**
     * 执行SSH命令
     * 
     * @param context 主机检查上下文
     * @param command 要执行的命令
     * @return 命令执行结果
     */
    CommandResult executeCommand(HostCheckContext context, String command);
    
    /**
     * 执行SSH命令（带超时）
     * 
     * @param context 主机检查上下文
     * @param command 要执行的命令
     * @param timeoutSeconds 超时时间（秒）
     * @return 命令执行结果
     */
    CommandResult executeCommand(HostCheckContext context, String command, long timeoutSeconds);
    
    /**
     * 批量执行SSH命令
     * 
     * @param context 主机检查上下文
     * @param commands 要执行的命令列表
     * @return 命令执行结果列表
     */
    List<CommandResult> executeBatchCommands(HostCheckContext context, List<String> commands);
    
    /**
     * 测试SSH连接
     * 
     * @param context 主机检查上下文
     * @return 连接测试结果
     */
    CommandResult testConnection(HostCheckContext context);
    
    /**
     * 获取连接池统计信息
     * 
     * @param context 主机检查上下文
     * @return 连接池统计信息
     */
    Map<String, Object> getConnectionPoolStats(HostCheckContext context);
    
    /**
     * 检查SSH连接池是否健康
     * 
     * @param context 主机检查上下文
     * @return 是否健康
     */
    boolean isConnectionPoolHealthy(HostCheckContext context);
    
    /**
     * 关闭指定主机的连接池
     * 
     * @param context 主机检查上下文
     */
    void closeConnectionPool(HostCheckContext context);
    
    /**
     * 获取全局连接池统计信息
     * 
     * @return 全局统计信息
     */
    Map<String, Object> getGlobalPoolStats();
}