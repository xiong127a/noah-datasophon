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

    // 使用BaseConverter默认实现，无需显式重写entityToDto和dtoToEntity

    @Override
    @Mapping(target = "clusterName", ignore = true) // 需要在Service层设置
    @Mapping(target = "scaleTypeDesc", source = "scaleType", qualifiedByName = "formatScaleType")
    @Mapping(target = "scalePolicyDesc", source = "scalePolicy", qualifiedByName = "formatScalePolicy")
    @Mapping(target = "cronExpressionDesc", source = "cronExpression", qualifiedByName = "formatCronExpression")
    @Mapping(target = "enabledDesc", source = "enabled", qualifiedByName = "formatEnabledStatus")
    @Mapping(target = "createTimeFormatted", source = "createTime", qualifiedByName = "formatDateTime")
    @Mapping(target = "updateTimeFormatted", source = "updateTime", qualifiedByName = "formatDateTime")
    AutoScaleTaskVO entityToVo(AutoScaleTaskEntity entity);

    @Override
    @Mapping(target = "clusterName", ignore = true) // 需要在Service层设置
    @Mapping(target = "scaleTypeDesc", source = "scaleType", qualifiedByName = "formatScaleType")
    @Mapping(target = "scalePolicyDesc", source = "scalePolicy", qualifiedByName = "formatScalePolicy")
    @Mapping(target = "cronExpressionDesc", source = "cronExpression", qualifiedByName = "formatCronExpression")
    @Mapping(target = "enabledDesc", source = "enabled", qualifiedByName = "formatEnabledStatus")
    @Mapping(target = "createTimeFormatted", source = "createTime", qualifiedByName = "formatDateTime")
    @Mapping(target = "updateTimeFormatted", source = "updateTime", qualifiedByName = "formatDateTime")
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

    @Named("formatEnabledStatus")
    default String formatEnabledStatus(Boolean enabled) {
        if (enabled == null) {
            return "未知";
        }
        return enabled ? "启用" : "禁用";
    }

    @Named("formatDateTime")
    default String formatDateTime(java.time.LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}