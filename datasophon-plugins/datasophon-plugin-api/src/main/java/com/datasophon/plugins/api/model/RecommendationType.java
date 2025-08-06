package com.datasophon.plugins.api.model;

/**
 * 建议类型枚举
 * 
 * @author DataSophon Team
 */
public enum RecommendationType {
    
    /**
     * 配置修改
     */
    CONFIG_CHANGE("config_change", "配置修改"),
    
    /**
     * 软件安装
     */
    SOFTWARE_INSTALL("software_install", "软件安装"),
    
    /**
     * 服务重启
     */
    SERVICE_RESTART("service_restart", "服务重启"),
    
    /**
     * 系统优化
     */
    SYSTEM_OPTIMIZATION("system_optimization", "系统优化"),
    
    /**
     * 资源清理
     */
    RESOURCE_CLEANUP("resource_cleanup", "资源清理"),
    
    /**
     * 安全加固
     */
    SECURITY_HARDENING("security_hardening", "安全加固"),
    
    /**
     * 监控告警
     */
    MONITORING_ALERT("monitoring_alert", "监控告警");
    
    private final String code;
    private final String description;
    
    RecommendationType(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
}