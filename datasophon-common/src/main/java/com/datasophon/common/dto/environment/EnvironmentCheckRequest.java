package com.datasophon.common.dto.environment;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 环境检查请求DTO
 * 
 * @author 任相鹏
 * @date 2025-01-23
 */
@Data
public class EnvironmentCheckRequest {
    
    /**
     * 集群ID
     */
    private Long clusterId;
    
    /**
     * 要检查的主机IP列表
     */
    private List<String> hostIps;
    
    /**
     * SSH连接参数
     * - sshUser: SSH用户名
     * - sshPort: SSH端口
     * - sshPassword: SSH密码
     */
    private Map<String, Object> connectionParams;
}

