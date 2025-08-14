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
import com.datasophon.common.model.PageResult;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

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
     * Entity转换为DTO时，枚举转换为Integer
     */
    @Mapping(target = "hostCommandId", source = "id")
    @Mapping(target = "commandState", source = "commandState", qualifiedByName = "commandStateToInteger")
    @Mapping(target = "serviceRoleType", source = "serviceRoleType", qualifiedByName = "roleTypeToInteger")
    @Override
    ClusterServiceCommandHostCommandDTO entityToDto(ClusterServiceCommandHostCommandEntity entity);

    /**
     * DTO转换为Entity时，Integer转换为枚举
     */
    @Mapping(target = "id", source = "hostCommandId")
    @Mapping(target = "commandState", source = "commandState", qualifiedByName = "integerToCommandState")
    @Mapping(target = "serviceRoleType", source = "serviceRoleType", qualifiedByName = "integerToRoleType")
    @Override
    ClusterServiceCommandHostCommandEntity dtoToEntity(ClusterServiceCommandHostCommandDTO dto);

    /**
     * Entity转换为VO时，添加格式化字段和枚举文本
     */
    @Mapping(target = "hostCommandId", source = "id")
    @Mapping(target = "commandState", source = "commandState", qualifiedByName = "commandStateToInteger")
    @Mapping(target = "commandStateText", source = "commandState", qualifiedByName = "mapCommandStateText")
    @Mapping(target = "serviceRoleType", source = "serviceRoleType", qualifiedByName = "roleTypeToInteger")
    @Mapping(target = "serviceRoleTypeText", source = "serviceRoleType", qualifiedByName = "mapRoleTypeText")
    @Mapping(target = "createTimeFormatted", source = "createTime", qualifiedByName = "formatDateTime")
    @Override
    ClusterServiceCommandHostCommandVO entityToVo(ClusterServiceCommandHostCommandEntity entity);

    /**
     * DTO转换为VO时，添加格式化字段和枚举文本
     */
    @Mapping(target = "commandStateText", source = "commandState", qualifiedByName = "mapIntegerCommandStateText")
    @Mapping(target = "serviceRoleTypeText", source = "serviceRoleType", qualifiedByName = "mapIntegerRoleTypeText")
    @Mapping(target = "createTimeFormatted", source = "createTime", qualifiedByName = "formatDateTime")
    @Override
    ClusterServiceCommandHostCommandVO dtoToVo(ClusterServiceCommandHostCommandDTO dto);

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
        if (value == null)
            return null;
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
     * RoleType枚举映射为文本
     */
    @Named("mapRoleTypeText")
    default String mapRoleTypeText(RoleType roleType) {
        if (roleType == null)
            return null;
        return switch (roleType) {
            case MASTER -> "master";
            case WORKER -> "worker";
            case CLIENT -> "client";
            case SLAVE -> "slave";
        };
    }

    /**
     * Integer类型RoleType映射为文本
     */
    @Named("mapIntegerRoleTypeText")
    default String mapIntegerRoleTypeText(Integer roleType) {
        if (roleType == null)
            return null;
        return switch (roleType) {
            case 1 -> "master";
            case 2 -> "worker";
            case 3 -> "client";
            case 4 -> "slave";
            default -> "unknown";
        };
    }

    /**
     * PageResult<Entity> 转换为 PageResult<VO>
     */
    @Named("pageResultToPageResultVO")
    default PageResult<ClusterServiceCommandHostCommandVO> pageResultToPageResultVO(
            PageResult<ClusterServiceCommandHostCommandEntity> pageResult) {
        if (pageResult == null) {
            return null;
        }

        return PageResult.of(
                entityListToVoList(pageResult.getRecords()),
                pageResult.getTotal(),
                pageResult.getPage(),
                pageResult.getSize());
    }
}