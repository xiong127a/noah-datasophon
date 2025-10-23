package com.datasophon.common.vo.environment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 修复结果VO
 * 
 * @author 任相鹏
 * @date 2025-01-23
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepairResult {
    
    /**
     * 修复是否成功
     */
    private Boolean success;
    
    /**
     * 修复消息
     */
    private String message;
    
    /**
     * 修复详情
     */
    private String details;
    
    /**
     * 修复后的检查项状态
     */
    private CheckItemStatusVO updatedStatus;
}

