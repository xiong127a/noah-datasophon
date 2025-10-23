package com.datasophon.api.checker;

import com.datasophon.common.enums.CheckItemStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 检查结果
 * 
 * @author 任相鹏
 * @date 2025-01-23
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckResult {
    
    /**
     * 检查是否成功
     */
    private Boolean success;
    
    /**
     * 检查状态
     */
    private CheckItemStatus status;
    
    /**
     * 详细信息
     */
    private String message;
    
    /**
     * 修复建议
     */
    private String recommendation;
    
    /**
     * 检查详情
     */
    private Map<String, Object> details;
    
    /**
     * 是否可以跳过
     */
    private Boolean canSkip;
    
    /**
     * 是否可以修复
     */
    private Boolean canRepair;
    
    /**
     * 创建成功结果
     */
    public static CheckResult success(String message) {
        return CheckResult.builder()
                .success(true)
                .status(CheckItemStatus.SUCCESS)
                .message(message)
                .canSkip(false)
                .canRepair(false)
                .build();
    }
    
    /**
     * 创建失败结果
     */
    public static CheckResult failure(String message, String recommendation, boolean canSkip, boolean canRepair) {
        return CheckResult.builder()
                .success(false)
                .status(CheckItemStatus.FAILED)
                .message(message)
                .recommendation(recommendation)
                .canSkip(canSkip)
                .canRepair(canRepair)
                .build();
    }
}

