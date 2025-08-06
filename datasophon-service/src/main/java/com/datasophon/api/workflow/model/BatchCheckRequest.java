package com.datasophon.api.workflow.model;

import com.datasophon.common.model.HostInfo;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 批量主机检查请求模型
 * 
 * @author DataSophon Team
 */
@Data
@Builder
public class BatchCheckRequest {
    
    /**
     * 批量请求ID
     */
    private String batchRequestId;
    
    /**
     * 主机信息列表
     */
    private List<HostInfo> hostInfos;
    
    /**
     * 需要执行的插件ID列表
     */
    private List<String> pluginIds;
    
    /**
     * 执行策略
     */
    private ExecutionStrategy strategy;
    
    /**
     * 批量执行模式
     */
    private BatchExecutionMode batchMode;
    
    /**
     * 超时时间（毫秒）
     */
    @Builder.Default
    private long timeoutMs = 600000; // 10分钟
    
    /**
     * 重试次数
     */
    @Builder.Default
    private int retryCount = 3;
    
    /**
     * 是否快速失败
     */
    @Builder.Default
    private boolean failFast = false;
    
    /**
     * 最大并发主机数量
     */
    @Builder.Default
    private int maxConcurrentHosts = 10;
    
    /**
     * 自定义参数
     */
    private Map<String, Object> customParameters;
    
    /**
     * 批量检查开始时间
     */
    @Builder.Default
    private long startTime = System.currentTimeMillis();
}