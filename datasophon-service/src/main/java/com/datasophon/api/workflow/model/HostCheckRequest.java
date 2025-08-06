package com.datasophon.api.workflow.model;

import com.datasophon.common.model.HostInfo;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 主机检查请求模型
 * 
 * @author DataSophon Team
 */
@Data
@Builder
public class HostCheckRequest {
    
    /**
     * 请求ID
     */
    private String requestId;
    
    /**
     * 主机信息
     */
    private HostInfo hostInfo;
    
    /**
     * 需要执行的插件ID列表
     */
    private List<String> pluginIds;
    
    /**
     * 执行策略
     */
    private ExecutionStrategy strategy;
    
    /**
     * 超时时间（毫秒）
     */
    @Builder.Default
    private long timeoutMs = 300000; // 5分钟
    
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
     * 并发执行数量
     */
    @Builder.Default
    private int concurrency = 4;
    
    /**
     * 自定义参数
     */
    private Map<String, Object> customParameters;
    
    /**
     * 检查开始时间
     */
    @Builder.Default
    private long startTime = System.currentTimeMillis();
}