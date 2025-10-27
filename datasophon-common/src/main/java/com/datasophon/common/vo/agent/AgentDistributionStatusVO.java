package com.datasophon.common.vo.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Agent分发状态VO
 * 
 * @author DataSophon Team
 * @date 2025-01-24
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentDistributionStatusVO {
    
    /**
     * 主机IP
     */
    private String hostIp;
    
    /**
     * 主机名
     */
    private String hostname;
    
    /**
     * 分发状态: PENDING, RUNNING, SUCCESS, FAILED
     */
    private String status;
    
    /**
     * 进度百分比 (0-100)
     */
    private Integer progress;
    
    /**
     * 当前执行的步骤名称
     */
    private String currentStep;
    
    /**
     * 状态消息（成功或失败信息）
     */
    private String message;
    
    /**
     * 开始时间（时间戳）
     */
    private Long startTime;
    
    /**
     * 结束时间（时间戳）
     */
    private Long endTime;
}

