package com.datasophon.api.converter;

import com.datasophon.common.converter.BaseConverter;
import com.datasophon.common.dto.AutoScaleTaskDTO;
import com.datasophon.common.utils.FormatterUtils;
import com.datasophon.common.vo.AutoScaleTaskVO;
import com.datasophon.dao.entity.AutoScaleTaskEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

/**
 * 自动伸缩任务对象转换器
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-01
 */
@Mapper(componentModel = "spring", uses = FormatterUtils.class)
public interface AutoScaleTaskConverter extends BaseConverter<AutoScaleTaskEntity, AutoScaleTaskDTO, AutoScaleTaskVO> {

    @Override
    @Mapping(target = "id", source = "id")
    @Mapping(target = "taskName", source = "taskName")
    @Mapping(target = "clusterId", source = "clusterId")
    @Mapping(target = "serviceId", source = "serviceId")
    @Mapping(target = "serviceName", source = "serviceName")
    @Mapping(target = "scaleType", source = "scaleType")
    @Mapping(target = "scalePolicy", source = "scalePolicy")
    @Mapping(target = "minReplicas", source = "minReplicas")
    @Mapping(target = "maxReplicas", source = "maxReplicas")
    @Mapping(target = "cronExpression", source = "cronExpression")
    @Mapping(target = "enabled", source = "enabled")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    AutoScaleTaskDTO entityToDto(AutoScaleTaskEntity entity);

    @Override
    @Mapping(target = "id", source = "id")
    @Mapping(target = "taskName", source = "taskName")
    @Mapping(target = "clusterId", source = "clusterId")
    @Mapping(target = "serviceId", source = "serviceId")
    @Mapping(target = "serviceName", source = "serviceName")
    @Mapping(target = "scaleType", source = "scaleType")
    @Mapping(target = "scalePolicy", source = "scalePolicy")
    @Mapping(target = "minReplicas", source = "minReplicas")
    @Mapping(target = "maxReplicas", source = "maxReplicas")
    @Mapping(target = "cronExpression", source = "cronExpression")
    @Mapping(target = "enabled", source = "enabled")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    AutoScaleTaskEntity dtoToEntity(AutoScaleTaskDTO dto);

    @Override
    @Mapping(target = "id", source = "id")
    @Mapping(target = "taskName", source = "taskName")
    @Mapping(target = "clusterId", source = "clusterId")
    @Mapping(target = "clusterName", ignore = true) // 需要在Service层设置
    @Mapping(target = "serviceId", source = "serviceId")
    @Mapping(target = "serviceName", source = "serviceName")
    @Mapping(target = "scaleType", source = "scaleType")
    @Mapping(target = "scaleTypeDesc", source = "scaleType", qualifiedByName = "formatScaleType")
    @Mapping(target = "scalePolicy", source = "scalePolicy")
    @Mapping(target = "scalePolicyDesc", source = "scalePolicy", qualifiedByName = "formatScalePolicy")
    @Mapping(target = "minReplicas", source = "minReplicas")
    @Mapping(target = "maxReplicas", source = "maxReplicas")
    @Mapping(target = "cronExpression", source = "cronExpression")
    @Mapping(target = "cronExpressionDesc", source = "cronExpression", qualifiedByName = "formatCronExpression")
    @Mapping(target = "enabled", source = "enabled")
    @Mapping(target = "enabledDesc", source = "enabled", qualifiedByName = "formatEnabledStatus")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    @Mapping(target = "createdAtFormatted", source = "createdAt", qualifiedByName = "formatDateTime")
    @Mapping(target = "updatedAtFormatted", source = "updatedAt", qualifiedByName = "formatDateTime")
    AutoScaleTaskVO entityToVo(AutoScaleTaskEntity entity);

    @Override
    @Mapping(target = "id", source = "id")
    @Mapping(target = "taskName", source = "taskName")
    @Mapping(target = "clusterId", source = "clusterId")
    @Mapping(target = "clusterName", ignore = true) // 需要在Service层设置
    @Mapping(target = "serviceId", source = "serviceId")
    @Mapping(target = "serviceName", source = "serviceName")
    @Mapping(target = "scaleType", source = "scaleType")
    @Mapping(target = "scaleTypeDesc", source = "scaleType", qualifiedByName = "formatScaleType")
    @Mapping(target = "scalePolicy", source = "scalePolicy")
    @Mapping(target = "scalePolicyDesc", source = "scalePolicy", qualifiedByName = "formatScalePolicy")
    @Mapping(target = "minReplicas", source = "minReplicas")
    @Mapping(target = "maxReplicas", source = "maxReplicas")
    @Mapping(target = "cronExpression", source = "cronExpression")
    @Mapping(target = "cronExpressionDesc", source = "cronExpression", qualifiedByName = "formatCronExpression")
    @Mapping(target = "enabled", source = "enabled")
    @Mapping(target = "enabledDesc", source = "enabled", qualifiedByName = "formatEnabledStatus")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    @Mapping(target = "createdAtFormatted", source = "createdAt", qualifiedByName = "formatDateTime")
    @Mapping(target = "updatedAtFormatted", source = "updatedAt", qualifiedByName = "formatDateTime")
    AutoScaleTaskVO dtoToVo(AutoScaleTaskDTO dto);

    @Named("formatScaleType")
    default String formatScaleType(String scaleType) {
        if (scaleType == null) {
            return "未知";
        }
        return switch (scaleType) {
            case "SCALE_UP" -> "扩容";
            case "SCALE_DOWN" -> "缩容";
            case "AUTO" -> "自动伸缩";
            default -> "未知";
        };
    }

    @Named("formatScalePolicy")
    default String formatScalePolicy(String scalePolicy) {
        if (scalePolicy == null) {
            return "默认策略";
        }
        return switch (scalePolicy) {
            case "CPU_BASED" -> "基于CPU";
            case "MEMORY_BASED" -> "基于内存";
            case "CRON_BASED" -> "基于时间";
            default -> "默认策略";
        };
    }

    @Named("formatCronExpression")
    default String formatCronExpression(String cronExpression) {
        if (cronExpression == null || cronExpression.trim().isEmpty()) {
            return "无定时";
        }
        // 简单的Cron表达式描述，实际应该使用专门的Cron描述库
        if ("0 0 9 * * MON-FRI".equals(cronExpression)) {
            return "工作日上午9点";
        } else if ("0 0 18 * * MON-FRI".equals(cronExpression)) {
            return "工作日下午6点";
        } else {
            return cronExpression;
        }
    }
}