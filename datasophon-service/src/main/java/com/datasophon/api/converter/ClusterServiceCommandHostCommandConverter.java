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
import com.datasophon.common.dto.ClusterServiceCommandHostCommandDTO;
import com.datasophon.common.utils.FormatterUtils;
import com.datasophon.common.vo.ClusterServiceCommandHostCommandVO;
import com.datasophon.dao.entity.ClusterServiceCommandHostCommandEntity;
import com.datasophon.common.enums.CommandState;
import com.datasophon.common.enums.RoleType;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

/**
 * 集群服务命令主机命令转换器
 * 负责ClusterServiceCommandHostCommandEntity、DTO、VO之间的转换
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
@Mapper(componentModel = "spring", uses = FormatterUtils.class)
public interface ClusterServiceCommandHostCommandConverter extends
        BaseConverter<ClusterServiceCommandHostCommandEntity, ClusterServiceCommandHostCommandDTO, ClusterServiceCommandHostCommandVO> {

    /**
     * Entity转换为DTO时，处理id映射和枚举转换
     */
    @Mapping(target = "hostCommandId", source = "id")
    @Mapping(target = "commandState", source = "commandState", qualifiedByName = "commandStateToInteger")
    @Mapping(target = "commandStateText", source = "commandState", qualifiedByName = "mapCommandStateText")
    @Mapping(target = "serviceRoleType", source = "serviceRoleType", qualifiedByName = "roleTypeToInteger")
    @Mapping(target = "serviceRoleTypeText", source = "serviceRoleType", qualifiedByName = "mapRoleTypeText")
    @Mapping(target = "createTimeFormatted", source = "createTime", qualifiedByName = "formatDateTime")
    @Mapping(target = "updateTimeFormatted", source = "updateTime", qualifiedByName = "formatDateTime")
    @Override
    @Named("entityToDto")
    ClusterServiceCommandHostCommandDTO entityToDto(ClusterServiceCommandHostCommandEntity entity);

    /**
     * DTO转换为Entity时，处理id映射和枚举转换
     */
    @Mapping(target = "id", source = "hostCommandId")
    @Mapping(target = "commandState", source = "commandState", qualifiedByName = "integerToCommandState")
    @Mapping(target = "serviceRoleType", source = "serviceRoleType", qualifiedByName = "integerToRoleType")
    @Override
    @Named("dtoToEntity")
    ClusterServiceCommandHostCommandEntity dtoToEntity(ClusterServiceCommandHostCommandDTO dto);

    /**
     * Entity转换为VO时，处理id映射和格式化
     */
    @Mapping(target = "hostCommandId", source = "id")
    @Mapping(target = "commandState", source = "commandState", qualifiedByName = "commandStateToInteger")
    @Mapping(target = "commandStateText", source = "commandState", qualifiedByName = "mapCommandStateText")
    @Mapping(target = "serviceRoleType", source = "serviceRoleType", qualifiedByName = "roleTypeToInteger")
    @Mapping(target = "serviceRoleTypeText", source = "serviceRoleType", qualifiedByName = "mapRoleTypeText")
    @Mapping(target = "createTimeFormatted", source = "createTime", qualifiedByName = "formatDateTime")
    @Mapping(target = "updateTimeFormatted", source = "updateTime", qualifiedByName = "formatDateTime")
    @Override
    @Named("entityToVo")
    ClusterServiceCommandHostCommandVO entityToVo(ClusterServiceCommandHostCommandEntity entity);

    /**
     * DTO转换为VO时，直接映射（DTO已包含格式化字段）
     */
    @Override
    @Named("dtoToVo")
    ClusterServiceCommandHostCommandVO dtoToVo(ClusterServiceCommandHostCommandDTO dto);

    /**
     * 重写列表转换方法
     */
    @Override
    @IterableMapping(qualifiedByName = "entityToDto")
    List<ClusterServiceCommandHostCommandDTO> entityListToDtoList(List<ClusterServiceCommandHostCommandEntity> entityList);

    @Override
    @IterableMapping(qualifiedByName = "dtoToEntity")
    List<ClusterServiceCommandHostCommandEntity> dtoListToEntityList(List<ClusterServiceCommandHostCommandDTO> dtoList);

    @Override
    @IterableMapping(qualifiedByName = "entityToVo")
    List<ClusterServiceCommandHostCommandVO> entityListToVoList(List<ClusterServiceCommandHostCommandEntity> entityList);

    @Override
    @IterableMapping(qualifiedByName = "dtoToVo")
    List<ClusterServiceCommandHostCommandVO> dtoListToVoList(List<ClusterServiceCommandHostCommandDTO> dtoList);

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
     * RoleType枚举转换为Integer
     */
    @Named("roleTypeToInteger")
    default Integer roleTypeToInteger(RoleType roleType) {
        return roleType != null ? roleType.getValue() : null;
    }

    /**
     * Integer转换为RoleType枚举
     */
    @Named("integerToRoleType")
    default RoleType integerToRoleType(Integer value) {
        if (value == null) return null;
        return switch (value) {
            case 1 -> RoleType.MASTER;
            case 2 -> RoleType.WORKER;
            case 3 -> RoleType.CLIENT;
            case 4 -> RoleType.SLAVE;
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

    /**
     * RoleType枚举映射为文本
     */
    @Named("mapRoleTypeText")
    default String mapRoleTypeText(RoleType roleType) {
        if (roleType == null) return null;
        return switch (roleType) {
            case MASTER -> "master";
            case WORKER -> "worker";
            case CLIENT -> "client";
            case SLAVE -> "slave";
        };
    }
}