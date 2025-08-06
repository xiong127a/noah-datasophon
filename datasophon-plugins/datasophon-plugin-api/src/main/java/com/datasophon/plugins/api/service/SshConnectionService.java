package com.datasophon.plugins.api.service;

import com.datasophon.plugins.api.model.HostCheckContext;
import com.datasophon.plugins.api.model.CommandResult;
import org.apache.sshd.client.session.ClientSession;

/**
 * SSH连接服务接口
 * 为插件提供统一的SSH连接管理
 * 
 * @author DataSophon Team
 */
public interface SshConnectionService {
    
    /**
     * 获取SSH连接
     * @param context 主机检查上下文
     * @return SSH会话
     * @throws Exception 获取连接失败
     */
    ClientSession borrowConnection(HostCheckContext context) throws Exception;
    
    /**
     * 归还SSH连接
     * @param context 主机检查上下文
     * @param session SSH会话
     */
    void returnConnection(HostCheckContext context, ClientSession session);
    
    /**
     * 执行SSH命令
     * @param context 主机检查上下文
     * @param command 要执行的命令
     * @return 命令执行结果
     */
    default CommandResult executeCommand(HostCheckContext context, String command) {
        ClientSession session = null;
        try {
            session = borrowConnection(context);
            // 这里需要具体的命令执行逻辑
            return new CommandResult("", 0, "", ""); // 临时返回值
        } catch (Exception e) {
            return new CommandResult("", -1, "", e.getMessage());
        } finally {
            if (session != null) {
                returnConnection(context, session);
            }
        }
    }
    
    /**
     * 获取连接池统计信息
     * @param context 主机检查上下文
     * @return 统计信息的JSON字符串
     */
    String getPoolStatistics(HostCheckContext context);
    
    /**
     * 检查SSH连接池是否健康
     * @param context 主机检查上下文
     * @return 是否健康
     */
    boolean isPoolHealthy(HostCheckContext context);
}