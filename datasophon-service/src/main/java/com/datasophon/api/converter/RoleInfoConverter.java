/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.datasophon.api.converter;

import com.datasophon.common.converter.BaseConverter;
import com.datasophon.common.dto.RoleInfoDTO;
import com.datasophon.common.vo.RoleInfoVO;
import com.datasophon.dao.entity.RoleInfoEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * 角色信息转换器
 * 继承BaseConverter，提供Entity、DTO、VO之间的转换
 * 使用MapStruct注解优化，避免IDE"未使用"警告
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-06
 */
@Mapper(componentModel = "spring")
public interface RoleInfoConverter extends BaseConverter<RoleInfoEntity, RoleInfoDTO, RoleInfoVO> {
    
    /**
     * Entity转DTO
     * 处理Date到LocalDateTime的转换
     */
    @Override
    @Mapping(target = "createTime", source = "createTime", qualifiedByName = "dateToLocalDateTime")
    RoleInfoDTO entityToDto(RoleInfoEntity entity);
    
    /**
     * DTO转Entity
     * 处理LocalDateTime到Date的转换
     */
    @Override
    @Mapping(target = "createTime", source = "createTime", qualifiedByName = "localDateTimeToDate")
    RoleInfoEntity dtoToEntity(RoleInfoDTO dto);
    
    /**
     * Entity转VO
     * 添加格式化时间和管理员角色标识
     */
    @Override
    @Mapping(target = "createTimeFormatted", source = "createTime", qualifiedByName = "formatDateTimeFromDate")
    @Mapping(target = "updateTimeFormatted", source = "updateTime", qualifiedByName = "formatDateTimeFromDate")
    @Mapping(target = "isAdminRole", source = ".", qualifiedByName = "checkAdminRoleFromEntity")
    @Mapping(target = "status", constant = "ACTIVE")
    RoleInfoVO entityToVo(RoleInfoEntity entity);

    /**
     * DTO转VO
     * 添加格式化时间和管理员角色标识
     */
    @Override
    @Mapping(target = "createTimeFormatted", source = "createTime", qualifiedByName = "formatDateTime")
    @Mapping(target = "updateTimeFormatted", source = "updateTime", qualifiedByName = "formatDateTime")
    @Mapping(target = "isAdminRole", source = ".", qualifiedByName = "checkAdminRole")
    @Mapping(target = "status", constant = "ACTIVE")
    RoleInfoVO dtoToVo(RoleInfoDTO dto);
    
    /**
     * Date转LocalDateTime
     */
    @Named("dateToLocalDateTime")
    default LocalDateTime dateToLocalDateTime(Date date) {
        return date != null ? 
            LocalDateTime.ofInstant(date.toInstant(), java.time.ZoneId.systemDefault()) : null;
    }
    
    /**
     * LocalDateTime转Date
     */
    @Named("localDateTimeToDate")
    default Date localDateTimeToDate(LocalDateTime localDateTime) {
        return localDateTime != null ? 
            Date.from(localDateTime.atZone(java.time.ZoneId.systemDefault()).toInstant()) : null;
    }
    
    /**
     * 格式化时间显示
     */
    @Named("formatDateTime")
    default String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? 
            dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null;
    }
    
    /**
     * 检查是否为管理员角色（从DTO）
     */
    @Named("checkAdminRole")
    default boolean checkAdminRole(RoleInfoDTO dto) {
        return dto.isAdminRole();
    }
    
    /**
     * 检查是否为管理员角色（从Entity）
     */
    @Named("checkAdminRoleFromEntity")
    default boolean checkAdminRoleFromEntity(RoleInfoEntity entity) {
        // 根据角色编码判断是否为管理员角色
        return entity.getRoleCode() != null && "ADMIN".equals(entity.getRoleCode());
    }
    
    /**
     * 格式化时间显示（从Date）
     */
    @Named("formatDateTimeFromDate")
    default String formatDateTimeFromDate(Date date) {
        if (date == null) return null;
        LocalDateTime dateTime = dateToLocalDateTime(date);
        return formatDateTime(dateTime);
    }
}