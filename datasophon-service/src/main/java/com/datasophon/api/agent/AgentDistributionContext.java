package com.datasophon.api.agent;

import com.datasophon.api.agent.util.AgentLogWriter;
import lombok.Builder;
import lombok.Data;

/**
 * Agent分发上下文
 * 包含Agent分发过程中所需的所有参数和状态信息
 */
@Data
@Builder
public class AgentDistributionContext {
    
    /**
     * 集群ID
     */
    private Long clusterId;
    
    /**
     * 目标主机IP
     */
    private String hostIp;
    
    /**
     * 目标主机名
     */
    private String hostname;
    
    /**
     * SSH用户名
     */
    private String sshUser;
    
    /**
     * SSH端口
     */
    private Integer sshPort;
    
    /**
     * SSH密码
     */
    private String sshPassword;
    
    /**
     * Agent包URL或本地路径
     */
    private String agentPackageUrl;
    
    /**
     * 是否为本地存储库
     */
    private boolean isLocalRepository;
    
    /**
     * 日志写入器（用于记录分发过程日志）
     */
    private AgentLogWriter logWriter;
    
    /**
     * Master本地Agent包路径
     */
    private String localPackagePath;
    
    /**
     * 目标主机Agent安装路径
     */
    private String remoteInstallPath;
}

