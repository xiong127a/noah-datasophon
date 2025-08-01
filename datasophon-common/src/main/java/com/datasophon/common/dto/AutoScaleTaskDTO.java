package com.datasophon.common.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Date;

/**
 * 自动伸缩任务数据传输对象
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-01
 */
public record AutoScaleTaskDTO(
        Long id,

        @NotBlank(message = "任务名称不能为空") String taskName,

        @NotNull(message = "集群ID不能为空") Integer clusterId,

        @NotNull(message = "服务ID不能为空") Integer serviceId,

        @NotBlank(message = "服务名称不能为空") String serviceName,

        @NotBlank(message = "伸缩类型不能为空") String scaleType,

        String scalePolicy,

        @Min(value = 1, message = "最小副本数不能小于1") Integer minReplicas,

        @Max(value = 100, message = "最大副本数不能超过100") Integer maxReplicas,

        String cronExpression,

        Boolean enabled,

        String description,

        Date createdAt,

        Date updatedAt) {
}