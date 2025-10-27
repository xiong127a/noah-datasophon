package com.datasophon.common.vo.environment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 环境检查验证统计信息
 * 
 * @author 任相鹏
 * @date 2025-01-27
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationSummary implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 总主机数
     */
    private Integer totalHosts;
    
    /**
     * 已完成检查的主机数
     */
    private Integer completedHosts;
    
    /**
     * 总检查项数
     */
    private Integer totalItems;
    
    /**
     * 成功的检查项数
     */
    private Integer successItems;
    
    /**
     * 失败的检查项数
     */
    private Integer failedItems;
    
    /**
     * 跳过的检查项数
     */
    private Integer skippedItems;
    
    /**
     * 仍有失败项的主机IP列表
     */
    private List<String> failedHostIps;
}

