package com.datasophon.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serial;
import java.io.Serializable;

/**
 * Kubernetes命名空间数据传输对象
 * 用于Service层数据传输
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record K8sNamespaceDTO(
        String name,
        String phase,
        String creationTime,
        Integer resourceVersion,
        BasicResourceStats basicStats) implements Serializable {

    /**
     * 基础资源统计
     */
    public record BasicResourceStats(
            Integer podCount,
            Integer serviceCount) implements Serializable {
        
        @Serial
        private static final long serialVersionUID = 1L;
    }

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 创建基础K8sNamespaceDTO（不含统计）
     */
    public static K8sNamespaceDTO of(String name, String phase, String creationTime, Integer resourceVersion) {
        return new K8sNamespaceDTO(name, phase, creationTime, resourceVersion, null);
    }

    /**
     * 创建包含统计的K8sNamespaceDTO
     */
    public static K8sNamespaceDTO withStats(String name, String phase, String creationTime, 
                                           Integer resourceVersion, Integer podCount, Integer serviceCount) {
        BasicResourceStats stats = new BasicResourceStats(podCount, serviceCount);
        return new K8sNamespaceDTO(name, phase, creationTime, resourceVersion, stats);
    }

    /**
     * 检查命名空间是否活跃
     */
    public boolean isActive() {
        return "Active".equals(phase);
    }

    /**
     * 获取显示名称
     */
    public String getDisplayName() {
        return name != null ? name : "未知命名空间";
    }
}