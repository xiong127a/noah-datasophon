package com.datasophon.api.checker;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * 主机检查上下文
 * 封装检查所需的所有参数
 * 
 * @author 任相鹏
 * @date 2025-01-23
 */
@Data
@Builder
public class HostCheckContext {
    
    /**
     * 集群ID
     */
    private Long clusterId;
    
    /**
     * 主机IP
     */
    private String hostIp;
    
    /**
     * 主机名
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
     * 其他连接参数
     */
    private Map<String, Object> connectionParams;
    
    /**
     * 检查器配置
     */
    private Map<String, Object> checkerConfig;
}

