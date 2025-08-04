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
        Integer resourceVersion) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 创建基础K8sNamespaceDTO
     */
    public static K8sNamespaceDTO of(String name, String phase, String creationTime, Integer resourceVersion) {
        return new K8sNamespaceDTO(name, phase, creationTime, resourceVersion);
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