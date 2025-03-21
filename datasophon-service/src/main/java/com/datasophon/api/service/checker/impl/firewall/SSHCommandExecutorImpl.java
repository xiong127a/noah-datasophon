package com.datasophon.api.service.checker.impl.firewall;

import org.apache.sshd.client.session.ClientSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datasophon.api.service.checker.CommandResult;

/**
 * SSH命令执行器的实现
 * 用于通过SSH执行远程命令
 */
public class SSHCommandExecutorImpl implements FirewallCheckerFactory.SSHCommandExecutor {
    
    private static final Logger logger = LoggerFactory.getLogger(SSHCommandExecutorImpl.class);
    
    private final ClientSession session;
    
    /**
     * 构造函数
     * @param session SSH会话
     */
    public SSHCommandExecutorImpl(ClientSession session) {
        this.session = session;
    }
    
    @Override
    public CommandResult execute(String command) throws Exception {
        if (session == null || !session.isOpen()) {
            logger.error("SSH会话未连接或已关闭");
            return new CommandResult("", "SSH会话未连接或已关闭", -1);
        }
        
        logger.debug("执行SSH命令: {}", command);
        
        try {
            String[] result = SshUtils.execCommand(session, command);
            String output = result[0];
            String error = result[1];
            int exitCode = Integer.parseInt(result[2]);
            
            CommandResult commandResult = new CommandResult(output, error, exitCode);
            
            if (commandResult.isSuccess()) {
                logger.debug("SSH命令执行成功: {}", command);
            } else {
                logger.warn("SSH命令执行失败 [{}]: {}", exitCode, error);
            }
            
            return commandResult;
        } catch (Exception e) {
            logger.error("执行SSH命令时发生异常: {}", e.getMessage(), e);
            return new CommandResult("", "执行命令异常: " + e.getMessage(), -1);
        }
    }
    
    /**
     * SSH工具类
     * 在这里作为占位符，实际应用中应该使用项目中已有的SSH工具类
     */
    private static class SshUtils {
        /**
         * 执行SSH命令
         * @param session SSH会话
         * @param command 要执行的命令
         * @return 结果数组，包含输出、错误和退出码
         */
        public static String[] execCommand(ClientSession session, String command) throws Exception {
            // 这里应该调用实际的SSH命令执行逻辑
            // 但作为示例，我们只返回一个占位符
            throw new UnsupportedOperationException("需要使用项目中已有的SSH执行命令的方法");
        }
    }
} 