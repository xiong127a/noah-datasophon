package com.datasophon.common.dto.host;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Hosts文件同步请求
 * 
 * @author 任相鹏
 * @date 2025-01-27
 */
@Data
public class HostsSyncRequest {
    
    /**
     * 集群ID
     */
    private Long clusterId;
    
    /**
     * 需要同步的主机IP列表
     */
    private List<String> hostIps;
    
    /**
     * SSH连接参数
     */
    private Map<String, Object> connectionParams;
    
    /**
     * Hosts文件内容（由用户编辑）
     */
    private String hostsContent;
}

