package com.datasophon.common.vo;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serial;
import java.io.Serializable;

/**
 * 安装步骤视图对象
 * 用于Controller层响应前端
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record InstallStepVO(
        Integer id,
        String stepName,
        String stepDesc,
        String installType,
        String displayStepName, // 步骤显示名称
        String displayStepDesc, // 步骤描述显示文本
        Integer installTypeValue, // 安装类型数值
        String installTypeText, // 安装类型显示文本
        Boolean isValid, // 是否为有效步骤
        Integer stepOrder // 步骤顺序（如果需要排序）
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 创建基础InstallStepVO
     */
    public static InstallStepVO of(Integer id, String stepName, String stepDesc, String installType) {
        String displayStepName = stepName != null ? stepName : "未知步骤";
        String displayStepDesc = stepDesc != null && !stepDesc.trim().isEmpty() ? stepDesc : "暂无描述";

        Integer installTypeValue = null;
        String installTypeText = "未知类型";
        try {
            if (installType != null && !installType.trim().isEmpty()) {
                installTypeValue = Integer.parseInt(installType);
                installTypeText = getInstallTypeDisplayText(installTypeValue);
            }
        } catch (NumberFormatException e) {
            installTypeText = installType;
        }

        Boolean isValid = stepName != null && !stepName.trim().isEmpty() &&
                installType != null && !installType.trim().isEmpty();

        return new InstallStepVO(id, stepName, stepDesc, installType, displayStepName,
                displayStepDesc, installTypeValue, installTypeText, isValid, null);
    }

    /**
     * 创建带步骤顺序的InstallStepVO
     */
    public static InstallStepVO withOrder(Integer id, String stepName, String stepDesc,
            String installType, Integer stepOrder) {
        InstallStepVO base = of(id, stepName, stepDesc, installType);
        return new InstallStepVO(base.id(), base.stepName(), base.stepDesc(), base.installType(),
                base.displayStepName(), base.displayStepDesc(), base.installTypeValue(),
                base.installTypeText(), base.isValid(), stepOrder);
    }

    /**
     * 获取安装类型显示文本
     */
    private static String getInstallTypeDisplayText(Integer installTypeValue) {
        if (installTypeValue == null) {
            return "未知类型";
        }
        if (installTypeValue.equals(1)) {
            return "主机安装";
        } else if (installTypeValue.equals(2)) {
            return "服务安装";
        } else if (installTypeValue.equals(3)) {
            return "配置安装";
        } else {
            return "其他类型";
        }
    }

    /**
     * 获取步骤状态显示文本
     */
    public String getStepStatusText() {
        if (isValid != null && isValid) {
            return "有效";
        }
        return "无效";
    }

    /**
     * 获取完整的步骤信息
     */
    public String getFullStepInfo() {
        return String.format("[%s] %s - %s", installTypeText, displayStepName, displayStepDesc);
    }
}