package com.datasophon.common.vo.host;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 主机操作进度VO（用于SSE推送）
 * 
 * @author 任相鹏
 * @date 2025-01-27
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HostOperationProgressVO {
    
    /**
     * 主机IP
     */
    private String hostIp;
    
    /**
     * 操作状态：pending, processing, success, failed
     */
    private String status;
    
    /**
     * 操作消息
     */
    private String message;
    
    /**
     * 原主机名（仅用于主机名修改）
     */
    private String oldHostname;
    
    /**
     * 新主机名（仅用于主机名修改）
     */
    private String newHostname;
    
    /**
     * 错误信息（如果失败）
     */
    private String error;
    
    /**
     * 时间戳
     */
    private Long timestamp;
}

