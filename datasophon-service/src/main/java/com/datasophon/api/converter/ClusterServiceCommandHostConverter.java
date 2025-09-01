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
import com.datasophon.common.dto.ClusterServiceCommandHostDTO;
import com.datasophon.common.utils.FormatterUtils;
import com.datasophon.common.vo.ClusterServiceCommandHostVO;
import com.datasophon.dao.entity.ClusterServiceCommandHostEntity;
import com.datasophon.common.enums.CommandState;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

/**
 * 集群服务命令主机转换器
 * 负责ClusterServiceCommandHostEntity、DTO、VO之间的转换
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
@Mapper(componentModel = "spring", uses = FormatterUtils.class)
public interface ClusterServiceCommandHostConverter extends
        BaseConverter<ClusterServiceCommandHostEntity, ClusterServiceCommandHostDTO, ClusterServiceCommandHostVO> {

    /**
     * Entity转换为DTO时，处理id映射和枚举转换
     */
    @Mapping(target = "commandHostId", source = "id")
    @Mapping(target = "commandState", source = "commandState", qualifiedByName = "commandStateToInteger")
    @Mapping(target = "commandStateText", source = "commandState", qualifiedByName = "mapCommandStateText")
    @Mapping(target = "createTimeFormatted", source = "createTime", qualifiedByName = "formatDateTime")
    @Mapping(target = "updateTimeFormatted", source = "updateTime", qualifiedByName = "formatDateTime")
    @Override
    @Named("entityToDto")
    ClusterServiceCommandHostDTO entityToDto(ClusterServiceCommandHostEntity entity);

    /**
     * DTO转换为Entity时，处理id映射和枚举转换
     */
    @Mapping(target = "id", source = "commandHostId")
    @Mapping(target = "commandState", source = "commandState", qualifiedByName = "integerToCommandState")
    @Override
    @Named("dtoToEntity")
    ClusterServiceCommandHostEntity dtoToEntity(ClusterServiceCommandHostDTO dto);

    /**
     * Entity转换为VO时，处理id映射和格式化
     */
    @Mapping(target = "commandHostId", source = "id")
    @Mapping(target = "commandState", source = "commandState", qualifiedByName = "commandStateToInteger")
    @Mapping(target = "commandStateText", source = "commandState", qualifiedByName = "mapCommandStateText")
    @Mapping(target = "createTimeFormatted", source = "createTime", qualifiedByName = "formatDateTime")
    @Mapping(target = "updateTimeFormatted", source = "updateTime", qualifiedByName = "formatDateTime")
    @Override
    @Named("entityToVo")
    ClusterServiceCommandHostVO entityToVo(ClusterServiceCommandHostEntity entity);

    /**
     * DTO转换为VO时，直接映射（DTO已包含格式化字段）
     */
    @Override
    @Named("dtoToVo")
    ClusterServiceCommandHostVO dtoToVo(ClusterServiceCommandHostDTO dto);

    /**
     * 重写列表转换方法
     */
    @Override
    @IterableMapping(qualifiedByName = "entityToDto")
    List<ClusterServiceCommandHostDTO> entityListToDtoList(List<ClusterServiceCommandHostEntity> entityList);

    @Override
    @IterableMapping(qualifiedByName = "dtoToEntity")
    List<ClusterServiceCommandHostEntity> dtoListToEntityList(List<ClusterServiceCommandHostDTO> dtoList);

    @Override
    @IterableMapping(qualifiedByName = "entityToVo")
    List<ClusterServiceCommandHostVO> entityListToVoList(List<ClusterServiceCommandHostEntity> entityList);

    @Override
    @IterableMapping(qualifiedByName = "dtoToVo")
    List<ClusterServiceCommandHostVO> dtoListToVoList(List<ClusterServiceCommandHostDTO> dtoList);

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
        if (value == null) return null;
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
        if (commandState == null) return null;
        return switch (commandState) {
            case WAIT -> "待运行";
            case RUNNING -> "正在运行";
            case SUCCESS -> "成功";
            case FAILED -> "失败";
            case CANCEL -> "取消";
        };
    }
}