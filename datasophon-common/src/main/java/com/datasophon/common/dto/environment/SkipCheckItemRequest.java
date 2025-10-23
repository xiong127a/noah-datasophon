package com.datasophon.common.dto.environment;

import lombok.Data;

/**
 * 跳过检查项请求DTO
 * 
 * @author 任相鹏
 * @date 2025-01-23
 */
@Data
public class SkipCheckItemRequest {
    
    /**
     * 主机IP
     */
    private String hostIp;
    
    /**
     * 检查项键名（如 "cpu", "memory", "java"）
     */
    private String checkItemKey;
    
    /**
     * 跳过原因
     */
    private String reason;
}

