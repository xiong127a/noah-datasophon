package com.datasophon.common.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 集群类型枚举
 * 
 * @author DataSophon Team
 */
public enum ClusterType {
    
    /**
     * 物理/虚拟机集群模式
     */
    PVM("PVM", "物理/虚拟机集群"),
    
    /**
     * Kubernetes集群模式
     */
    KUBERNETES("Kubernetes", "Kubernetes集群");
    
    private final String code;
    @Getter
    private final String description;
    
    ClusterType(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    @JsonValue
    public String getCode() {
        return code;
    }

    /**
     * 根据代码获取枚举
     */
    public static ClusterType fromCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }
        
        for (ClusterType type : ClusterType.values()) {
            if (type.code.equalsIgnoreCase(code.trim())) {
                return type;
            }
        }
        
        // 兼容老的命名方式
        String normalizedCode = code.trim().toLowerCase();
        return switch (normalizedCode) {
            case "pvm" -> PVM;
            case "kubernetes", "k8s" -> KUBERNETES;
            default -> throw new IllegalArgumentException("不支持的集群类型: " + code);
        };
    }
    
    /**
     * 是否为PVM类型
     */
    public boolean isPvm() {
        return this == PVM;
    }
    
    /**
     * 是否为Kubernetes类型
     */
    public boolean isKubernetes() {
        return this == KUBERNETES;
    }
    
    @Override
    public String toString() {
        return code;
    }
}