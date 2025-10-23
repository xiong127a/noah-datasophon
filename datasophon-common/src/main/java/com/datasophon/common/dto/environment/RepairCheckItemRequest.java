package com.datasophon.common.dto.environment;

import lombok.Data;

import java.util.Map;

/**
 * 修复检查项请求DTO
 * 
 * @author 任相鹏
 * @date 2025-01-23
 */
@Data
public class RepairCheckItemRequest {
    
    /**
     * 主机IP
     */
    private String hostIp;
    
    /**
     * 检查项键名（如 "firewall", "selinux"）
     */
    private String checkItemKey;
    
    /**
     * 修复参数
     * 不同检查项可能需要不同的参数
     */
    private Map<String, Object> repairParams;
}

