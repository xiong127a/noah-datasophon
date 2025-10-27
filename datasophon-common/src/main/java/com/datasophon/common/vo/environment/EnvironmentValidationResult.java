package com.datasophon.common.vo.environment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 环境检查验证结果
 * 用于判断是否可以进入下一步
 * 
 * @author 任相鹏
 * @date 2025-01-27
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnvironmentValidationResult implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 是否可以进入下一步
     * true: 所有检查项都成功或已跳过
     * false: 仍有失败的检查项
     */
    private Boolean canProceed;
    
    /**
     * 如果不能进入下一步,说明原因
     */
    private String reason;
    
    /**
     * 总体统计信息
     */
    private ValidationSummary summary;
}

