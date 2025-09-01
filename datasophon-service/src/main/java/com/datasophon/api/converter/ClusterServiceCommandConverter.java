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
import com.datasophon.common.dto.ClusterServiceCommandDTO;
import com.datasophon.common.utils.FormatterUtils;
import com.datasophon.common.vo.ClusterServiceCommandVO;
import com.datasophon.dao.entity.ClusterServiceCommandEntity;
import com.datasophon.common.enums.CommandState;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

/**
 * 集群服务命令转换器
 * 负责ClusterServiceCommandEntity、DTO、VO之间的转换
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
@Mapper(componentModel = "spring", uses = FormatterUtils.class)
public interface ClusterServiceCommandConverter extends
        BaseConverter<ClusterServiceCommandEntity, ClusterServiceCommandDTO, ClusterServiceCommandVO> {

    /**
     * Entity转换为DTO时，枚举转换为Integer，include ignore字段
     */
    @Mapping(target = "commandState", source = "commandState", qualifiedByName = "commandStateToInteger")
    @Override
    ClusterServiceCommandDTO entityToDto(ClusterServiceCommandEntity entity);

    /**
     * DTO转换为Entity时，Integer转换为枚举，exclude ignore字段
     */
    @Mapping(target = "commandState", source = "commandState", qualifiedByName = "integerToCommandState")
    @Mapping(target = "commandStateCode", ignore = true)
    @Mapping(target = "durationTime", ignore = true)
    @Override
    ClusterServiceCommandEntity dtoToEntity(ClusterServiceCommandDTO dto);

    /**
     * Entity转换为VO时，添加格式化字段和枚举文本
     */
    @Mapping(target = "commandId", source = "id")
    @Mapping(target = "commandState", source = "commandState", qualifiedByName = "commandStateToInteger")
    @Mapping(target = "commandStateText", source = "commandState", qualifiedByName = "mapCommandStateText")
    @Mapping(target = "commandTypeText", source = "commandType", qualifiedByName = "mapCommandTypeText")
    @Mapping(target = "createTimeFormatted", source = "createTime", qualifiedByName = "formatDateTime")
    @Mapping(target = "updateTimeFormatted", source = "updateTime", qualifiedByName = "formatDateTime")
    @Mapping(target = "endTimeFormatted", source = "endTime", qualifiedByName = "formatDateTime")
    @Override
    ClusterServiceCommandVO entityToVo(ClusterServiceCommandEntity entity);

    /**
     * DTO转换为VO时，添加格式化字段和枚举文本
     */
    @Mapping(target = "commandId", source = "id")
    @Mapping(target = "commandStateText", source = "commandState", qualifiedByName = "mapIntegerCommandStateText")
    @Mapping(target = "commandTypeText", source = "commandType", qualifiedByName = "mapIntegerCommandTypeText")
    @Mapping(target = "createTimeFormatted", source = "createTime", qualifiedByName = "formatDateTime")
    @Mapping(target = "updateTimeFormatted", source = "updateTime", qualifiedByName = "formatDateTime")
    @Mapping(target = "endTimeFormatted", source = "endTime", qualifiedByName = "formatDateTime")
    @Override
    ClusterServiceCommandVO dtoToVo(ClusterServiceCommandDTO dto);

    /**
     * CommandState枚举转换为Integer
     */
    @Named("commandStateToInteger")
    default Integer commandStateToInteger(CommandState commandState) {
        return commandState != null ? commandState.getValue() : null;
    }

    /**
     * Integer转换为CommandState枚举
     */
    @Named("integerToCommandState")
    default CommandState integerToCommandState(Integer value) {
        if (value == null)
            return null;
        return switch (value) {
            case 0 -> CommandState.WAIT;
            case 1 -> CommandState.RUNNING;
            case 2 -> CommandState.SUCCESS;
            case 3 -> CommandState.FAILED;
            case 4 -> CommandState.CANCEL;
            default -> null;
        };
    }

    /**
     * CommandState枚举映射为文本
     */
    @Named("mapCommandStateText")
    default String mapCommandStateText(CommandState commandState) {
        if (commandState == null)
            return null;
        return switch (commandState) {
            case WAIT -> "待运行";
            case RUNNING -> "正在运行";
            case SUCCESS -> "成功";
            case FAILED -> "失败";
            case CANCEL -> "取消";
        };
    }

    /**
     * Integer类型CommandState映射为文本
     */
    @Named("mapIntegerCommandStateText")
    default String mapIntegerCommandStateText(Integer commandState) {
        if (commandState == null)
            return null;
        return switch (commandState) {
            case 0 -> "待运行";
            case 1 -> "正在运行";
            case 2 -> "成功";
            case 3 -> "失败";
            case 4 -> "取消";
            default -> "未知状态";
        };
    }

    /**
     * CommandType枚举映射为文本
     */
    @Named("mapCommandTypeText")
    default String mapCommandTypeText(Integer commandType) {
        if (commandType == null)
            return null;
        return switch (commandType) {
            case 1 -> "安装服务";
            case 2 -> "启动服务";
            case 3 -> "停止服务";
            case 4 -> "重启服务";
            case 5 -> "卸载服务";
            case 6 -> "配置服务";
            default -> "未知操作";
        };
    }

    /**
     * Integer类型CommandType映射为文本
     */
    @Named("mapIntegerCommandTypeText")
    default String mapIntegerCommandTypeText(Integer commandType) {
        if (commandType == null)
            return null;
        return switch (commandType) {
            case 1 -> "安装服务";
            case 2 -> "启动服务";
            case 3 -> "停止服务";
            case 4 -> "重启服务";
            case 5 -> "卸载服务";
            case 6 -> "配置服务";
            default -> "未知操作";
        };
    }
}