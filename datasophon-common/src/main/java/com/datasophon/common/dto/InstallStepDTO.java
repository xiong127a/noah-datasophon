package com.datasophon.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serial;
import java.io.Serializable;

/**
 * 安装步骤数据传输对象
 * 用于Service层数据传输
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record InstallStepDTO(
        Integer id,
        String stepName,
        String stepDesc,
        String installType) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 创建基础InstallStepDTO
     */
    public static InstallStepDTO of(Integer id, String stepName, String stepDesc, String installType) {
        return new InstallStepDTO(id, stepName, stepDesc, installType);
    }

    /**
     * 获取安装类型整数值
     */
    public Integer getInstallTypeValue() {
        if (installType == null || installType.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(installType);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 检查是否为有效的安装步骤
     */
    public boolean isValid() {
        return stepName != null && !stepName.trim().isEmpty() &&
                installType != null && !installType.trim().isEmpty();
    }

    /**
     * 获取步骤显示名称
     */
    public String getDisplayStepName() {
        return stepName != null ? stepName : "未知步骤";
    }

    /**
     * 获取步骤描述显示文本
     */
    public String getDisplayStepDesc() {
        return stepDesc != null && !stepDesc.trim().isEmpty() ? stepDesc : "暂无描述";
    }
}