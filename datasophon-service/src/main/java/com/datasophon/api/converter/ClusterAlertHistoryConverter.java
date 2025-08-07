/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.datasophon.api.converter;

import com.datasophon.common.converter.BaseConverter;
import com.datasophon.common.dto.ClusterAlertHistoryDTO;
import com.datasophon.common.utils.FormatterUtils;
import com.datasophon.common.vo.ClusterAlertHistoryVO;
import com.datasophon.dao.entity.ClusterAlertHistory;
import com.datasophon.common.enums.AlertLevel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

/**
 * 集群告警历史转换器
 * 负责ClusterAlertHistory Entity、DTO、VO之间的转换
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
@Mapper(componentModel = "spring", uses = FormatterUtils.class)
public interface ClusterAlertHistoryConverter extends
        BaseConverter<ClusterAlertHistory, ClusterAlertHistoryDTO, ClusterAlertHistoryVO> {

    /**
     * Entity转换为DTO时，AlertLevel枚举转Integer
     */
    @Mapping(target = "alertLevel", source = "alertLevel", qualifiedByName = "alertLevelToInteger")
    @Override
    ClusterAlertHistoryDTO entityToDto(ClusterAlertHistory entity);

    /**
     * DTO转换为Entity时，Integer转AlertLevel枚举
     */
    @Mapping(target = "alertLevel", source = "alertLevel", qualifiedByName = "integerToAlertLevel")
    @Override
    ClusterAlertHistory dtoToEntity(ClusterAlertHistoryDTO dto);

    /**
     * Entity转换为VO时，添加格式化字段和文本字段
     */
    @Mapping(target = "alertLevel", source = "alertLevel", qualifiedByName = "alertLevelToInteger")
    @Mapping(target = "alertLevelText", source = "alertLevel", qualifiedByName = "mapEntityAlertLevelText")
    @Mapping(target = "isEnabledText", source = "isEnabled", qualifiedByName = "mapIsEnabledText")
    @Mapping(target = "createTimeFormatted", source = "createTime", qualifiedByName = "formatDateTime")
    @Mapping(target = "updateTimeFormatted", source = "updateTime", qualifiedByName = "formatDateTime")
    @Override
    ClusterAlertHistoryVO entityToVo(ClusterAlertHistory entity);

    /**
     * DTO转换为VO时，添加格式化字段和文本字段
     */
    @Mapping(target = "alertLevelText", source = "alertLevel", qualifiedByName = "mapAlertLevelText")
    @Mapping(target = "isEnabledText", source = "isEnabled", qualifiedByName = "mapIsEnabledText")
    @Mapping(target = "createTimeFormatted", source = "createTime", qualifiedByName = "formatDateTime")
    @Mapping(target = "updateTimeFormatted", source = "updateTime", qualifiedByName = "formatDateTime")
    @Override
    ClusterAlertHistoryVO dtoToVo(ClusterAlertHistoryDTO dto);

    /**
     * AlertLevel枚举转Integer
     */
    @Named("alertLevelToInteger")
    default Integer alertLevelToInteger(AlertLevel alertLevel) {
        return alertLevel != null ? alertLevel.getValue() : null;
    }

    /**
     * Integer转AlertLevel枚举
     */
    @Named("integerToAlertLevel")
    default AlertLevel integerToAlertLevel(Integer value) {
        if (value == null) {
            return null;
        }
        return switch (value) {
            case 1 -> AlertLevel.WARN;
            case 2 -> AlertLevel.EXCEPTION;
            default -> null;
        };
    }

    /**
     * AlertLevel枚举转文本（用于Entity直接转VO）
     */
    @Named("mapEntityAlertLevelText")
    default String mapEntityAlertLevelText(AlertLevel alertLevel) {
        if (alertLevel == null) {
            return "未知";
        }
        return switch (alertLevel) {
            case WARN -> "警告";
            case EXCEPTION -> "异常";
        };
    }

    /**
     * Integer转文本（用于DTO转VO）
     */
    @Named("mapAlertLevelText")
    default String mapAlertLevelText(Integer alertLevel) {
        if (alertLevel == null) {
            return "未知";
        }
        return switch (alertLevel) {
            case 1 -> "警告";
            case 2 -> "异常";
            default -> "未知";
        };
    }

    /**
     * isEnabled字段转文本
     */
    @Named("mapIsEnabledText")
    default String mapIsEnabledText(Integer isEnabled) {
        if (isEnabled == null) {
            return "未知";
        }
        return switch (isEnabled) {
            case 1 -> "未处理";
            case 2 -> "已处理";
            default -> "未知状态";
        };
    }
}