package com.datasophon.plugins.api.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 检查结果模型
 * 统一的插件执行结果封装
 * 
 * @author DataSophon Team
 */
@Data
@Builder
public class CheckResult {
    
    /**
     * 插件ID
     */
    private String pluginId;
    
    /**
     * 插件版本
     */
    private String pluginVersion;
    
    /**
     * 主机IP
     */
    private String hostIp;
    
    /**
     * 检查状态
     */
    private CheckStatus status;
    
    /**
     * 严重程度
     */
    private Severity severity;
    
    /**
     * 检查消息
     */
    private String message;
    
    /**
     * 详细信息
     */
    private Map<String, Object> details;
    
    /**
     * 检查建议
     */
    private List<CheckRecommendation> recommendations;
    
    /**
     * 执行时间（毫秒）
     */
    private long executionTimeMs;
    
    /**
     * 异常信息
     */
    private String exceptionMessage;
    
    /**
     * 检查时间
     */
    @Builder.Default
    private LocalDateTime checkTime = LocalDateTime.now();
    
    /**
     * 检查项代码
     */
    private String itemCode;
    
    /**
     * 是否成功
     */
    public boolean isSuccess() {
        return status == CheckStatus.SUCCESS;
    }
    
    /**
     * 是否失败
     */
    public boolean isFailed() {
        return status == CheckStatus.FAILED;
    }
    
    /**
     * 是否有错误
     */
    public boolean hasError() {
        return status == CheckStatus.ERROR;
    }
    
    /**
     * 是否是致命问题
     */
    public boolean isCritical() {
        return severity == Severity.CRITICAL;
    }
    
    /**
     * 创建成功结果
     */
    public static CheckResult success(String pluginId, String message) {
        return CheckResult.builder()
                .pluginId(pluginId)
                .status(CheckStatus.SUCCESS)
                .severity(Severity.INFO)
                .message(message)
                .build();
    }
    
    /**
     * 创建失败结果
     */
    public static CheckResult failed(String pluginId, String message, Severity severity) {
        return CheckResult.builder()
                .pluginId(pluginId)
                .status(CheckStatus.FAILED)
                .severity(severity)
                .message(message)
                .build();
    }
    
    /**
     * 创建错误结果
     */
    public static CheckResult error(String pluginId, String message, Exception e) {
        return CheckResult.builder()
                .pluginId(pluginId)
                .status(CheckStatus.ERROR)
                .severity(Severity.CRITICAL)
                .message(message)
                .exceptionMessage(e.getMessage())
                .build();
    }
}