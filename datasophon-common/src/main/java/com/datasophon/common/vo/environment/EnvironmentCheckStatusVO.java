package com.datasophon.common.vo.environment;

import com.datasophon.common.enums.HostCheckStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 环境检查状态VO（单台主机）
 * 
 * @author 任相鹏
 * @date 2025-01-23
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnvironmentCheckStatusVO {
    
    /**
     * 主机IP
     */
    private String hostIp;
    
    /**
     * 主机名
     */
    private String hostname;
    
    /**
     * 主机整体检查状态
     */
    private HostCheckStatus overallStatus;
    
    /**
     * 检查项列表
     */
    private List<CheckItemStatusVO> checkItems;
    
    /**
     * 总检查项数
     */
    private Integer totalItems;
    
    /**
     * 已完成检查项数
     */
    private Integer completedItems;
    
    /**
     * 成功检查项数
     */
    private Integer successItems;
    
    /**
     * 失败检查项数
     */
    private Integer failedItems;
    
    /**
     * 跳过检查项数
     */
    private Integer skippedItems;
    
    /**
     * 检查开始时间
     */
    private Long startTime;
    
    /**
     * 检查结束时间
     */
    private Long endTime;
    
    /**
     * 错误消息（整体失败时）
     */
    private String errorMessage;
}

