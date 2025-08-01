package com.datasophon.common.vo;

import java.util.Date;

/**
 * 自动伸缩任务视图对象
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-01
 */
public record AutoScaleTaskVO(
        Long id,
        String taskName,
        Integer clusterId,
        String clusterName,
        Integer serviceId,
        String serviceName,
        String scaleType,
        String scaleTypeDesc,
        String scalePolicy,
        String scalePolicyDesc,
        Integer minReplicas,
        Integer maxReplicas,
        String cronExpression,
        String cronExpressionDesc,
        Boolean enabled,
        String enabledDesc,
        String description,
        Date createdAt,
        Date updatedAt,
        String createdAtFormatted,
        String updatedAtFormatted) {
}