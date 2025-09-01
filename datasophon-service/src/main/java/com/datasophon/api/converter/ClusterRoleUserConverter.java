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
import com.datasophon.common.dto.ClusterRoleUserDTO;
import com.datasophon.common.utils.FormatterUtils;
import com.datasophon.common.vo.ClusterRoleUserVO;
import com.datasophon.dao.entity.ClusterRoleUserEntity;
import com.datasophon.common.enums.UserType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

/**
 * 集群角色用户转换器
 * 负责ClusterRoleUserEntity、ClusterRoleUserDTO、ClusterRoleUserVO之间的转换
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-01
 */
@Mapper(componentModel = "spring", uses = { UserInfoConverter.class, FormatterUtils.class })
public interface ClusterRoleUserConverter
        extends BaseConverter<ClusterRoleUserEntity, ClusterRoleUserDTO, ClusterRoleUserVO> {

    /**
     * Entity转换为DTO时，UserType枚举转换为Integer
     */
    @Mapping(target = "userType", source = "userType", qualifiedByName = "userTypeToInteger")
    @Override
    ClusterRoleUserDTO entityToDto(ClusterRoleUserEntity entity);

    /**
     * DTO转换为Entity时，Integer转换为UserType枚举
     */
    @Mapping(target = "userType", source = "userType", qualifiedByName = "integerToUserType")
    @Override
    ClusterRoleUserEntity dtoToEntity(ClusterRoleUserDTO dto);

    /**
     * Entity转换为VO时，添加用户类型文本映射
     */
    @Mapping(target = "userType", source = "userType", qualifiedByName = "userTypeToInteger")
    @Mapping(target = "userTypeText", source = "userType", qualifiedByName = "mapUserTypeText")
    @Mapping(target = "createTimeFormatted", source = "createTime", qualifiedByName = "formatDateTime")
    @Mapping(target = "updateTimeFormatted", source = "updateTime", qualifiedByName = "formatDateTime")
    @Override
    ClusterRoleUserVO entityToVo(ClusterRoleUserEntity entity);

    /**
     * DTO转换为VO时，添加用户类型文本映射
     */
    @Mapping(target = "userTypeText", source = "userType", qualifiedByName = "mapIntegerUserTypeText")
    @Mapping(target = "createTimeFormatted", source = "createTime", qualifiedByName = "formatDateTime")
    @Mapping(target = "updateTimeFormatted", source = "updateTime", qualifiedByName = "formatDateTime")
    @Override
    ClusterRoleUserVO dtoToVo(ClusterRoleUserDTO dto);

    /**
     * UserType枚举转换为Integer
     */
    @Named("userTypeToInteger")
    default Integer userTypeToInteger(UserType userType) {
        if (userType == null) {
            return null;
        }
        return userType.getValue();
    }

    /**
     * Integer转换为UserType枚举
     */
    @Named("integerToUserType")
    default UserType integerToUserType(Integer userType) {
        if (userType == null) {
            return null;
        }
        return userType == 1 ? UserType.ADMIN : UserType.NORMAL;
    }

    /**
     * 映射用户类型文本（用于Entity的UserType枚举）
     */
    @Named("mapUserTypeText")
    default String mapUserTypeText(UserType userType) {
        if (userType == null) {
            return null;
        }
        return userType.getDesc();
    }

    /**
     * 映射用户类型文本（用于DTO的Integer类型）
     */
    @Named("mapIntegerUserTypeText")
    default String mapIntegerUserTypeText(Integer userType) {
        if (userType == null) {
            return null;
        }
        if (userType == 1) {
            return "管理员";
        } else if (userType == 2) {
            return "普通用户";
        } else {
            return "未知类型";
        }
    }
}