package com.datasophon.common.vo;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serial;
import java.io.Serializable;

/**
 * Kubernetes命名空间视图对象
 * 用于Controller层响应前端
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record K8sNamespaceVO(
        String name,
        String phase,
        String creationTime,
        Integer resourceVersion,
        String displayName, // 显示名称
        String phaseText, // 状态显示文本
        Boolean isActive, // 是否活跃
        String statusColor // 状态颜色
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 从DTO创建VO
     */
    public static K8sNamespaceVO from(String name, String phase, String creationTime, Integer resourceVersion) {
        String displayName = name != null ? name : "未知命名空间";
        String phaseText = getPhaseText(phase);
        Boolean isActive = "Active".equals(phase);
        String statusColor = getStatusColor(phase);

        return new K8sNamespaceVO(name, phase, creationTime, resourceVersion,
                displayName, phaseText, isActive, statusColor);
    }

    /**
     * 获取状态显示文本
     */
    private static String getPhaseText(String phase) {
        if (phase == null) {
            return "未知";
        }
        if ("Active".equals(phase)) {
            return "活跃";
        } else if ("Terminating".equals(phase)) {
            return "终止中";
        } else {
            return phase;
        }
    }

    /**
     * 获取状态颜色
     */
    private static String getStatusColor(String phase) {
        if (phase == null) {
            return "gray";
        }
        if ("Active".equals(phase)) {
            return "green";
        } else if ("Terminating".equals(phase)) {
            return "orange";
        } else {
            return "red";
        }
    }
}