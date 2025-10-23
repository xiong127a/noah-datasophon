package com.datasophon.common.vo.environment;

import com.datasophon.common.enums.CheckItemStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 检查项状态VO
 * 
 * @author 任相鹏
 * @date 2025-01-23
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckItemStatusVO {
    
    /**
     * 检查项键名（如 "cpu", "memory", "java"）
     */
    private String checkKey;
    
    /**
     * 显示名称（如 "CPU核心数检查"）
     */
    private String displayName;
    
    /**
     * 优先级（数字越小优先级越高）
     */
    private Integer priority;
    
    /**
     * 检查状态
     */
    private CheckItemStatus status;
    
    /**
     * 详细信息
     */
    private String message;
    
    /**
     * 修复建议（失败时显示）
     */
    private String recommendation;
    
    /**
     * 是否允许跳过
     */
    private Boolean canSkip;
    
    /**
     * 是否可以修复
     */
    private Boolean canRepair;
    
    /**
     * 检查详情
     * 例如：{"actual": 2, "required": 4} 表示实际2核，要求4核
     */
    private Map<String, Object> checkResult;
    
    /**
     * 检查开始时间
     */
    private Long startTime;
    
    /**
     * 检查结束时间
     */
    private Long endTime;
}

