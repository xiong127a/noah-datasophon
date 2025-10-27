package com.datasophon.common.vo.environment;

import com.datasophon.common.enums.CheckItemStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 全局检查结果VO
 * 用于表示跨主机的全局检查结果
 * 
 * @author 任相鹏
 * @date 2025-01-27
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlobalCheckResult {
    
    /**
     * 检查项键名
     */
    private String checkKey;
    
    /**
     * 显示名称
     */
    private String displayName;
    
    /**
     * 检查状态
     */
    private CheckItemStatus status;
    
    /**
     * 检查消息
     */
    private String message;
    
    /**
     * 建议信息
     */
    private String recommendation;
    
    /**
     * 详细信息（JSON格式存储）
     * 例如：冲突的主机名列表、hosts文件差异等
     */
    private Map<String, Object> details;
    
    /**
     * 检查时间戳
     */
    private Long timestamp;
}

