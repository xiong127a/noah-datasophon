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

import com.datasophon.common.dto.NoticeGroupDTO;
import com.datasophon.api.vo.NoticeGroupVO;
import com.datasophon.api.vo.UserInfoVO;
import com.datasophon.dao.entity.NoticeGroupEntity;
import com.datasophon.common.converter.BaseConverter;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * NoticeGroup对象转换器
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-01
 */
@Mapper(componentModel = "spring")
public interface NoticeGroupConverter extends BaseConverter<NoticeGroupEntity, NoticeGroupDTO, NoticeGroupVO> {

    @Override
    @Mapping(target = "userIds", ignore = true)
    NoticeGroupDTO entityToDto(NoticeGroupEntity entity);

    @Override
    @Mapping(target = "userIds", ignore = true)
    NoticeGroupEntity dtoToEntity(NoticeGroupDTO dto);

    @Override
    @Mapping(target = "users", ignore = true)
    @Mapping(target = "userCount", ignore = true)
    @Mapping(target = "userCountFormatted", source = "userCount", qualifiedByName = "formatUserCount")
    @Mapping(target = "createTimeFormatted", source = "createTime", qualifiedByName = "formatDateTime")
    NoticeGroupVO dtoToVo(NoticeGroupDTO dto);

    @Override
    List<NoticeGroupDTO> entityListToDtoList(List<NoticeGroupEntity> entityList);

    @Override
    List<NoticeGroupVO> dtoListToVoList(List<NoticeGroupDTO> dtoList);

    @Override
    @Mapping(target = "users", ignore = true)
    @Mapping(target = "userCount", ignore = true)
    @Mapping(target = "userCountFormatted", source = "userCount", qualifiedByName = "formatUserCount")
    @Mapping(target = "createTimeFormatted", source = "createTime", qualifiedByName = "formatDateTime")
    NoticeGroupVO entityToVo(NoticeGroupEntity entity);

    @Override
    List<NoticeGroupVO> entityListToVoList(List<NoticeGroupEntity> entityList);

    @Override
    @Mapping(target = "userIds", ignore = true)
    void updateEntityFromDto(NoticeGroupDTO dto, NoticeGroupEntity entity);

    /**
     * 创建包含用户信息的VO
     */
    @Mapping(target = "userCountFormatted", source = "users", qualifiedByName = "formatUserCountFromList")
    @Mapping(target = "userCount", source = "users", qualifiedByName = "calculateUserCount")
    @Mapping(target = "createTimeFormatted", source = "createTime", qualifiedByName = "formatDateTime")
    NoticeGroupVO dtoToVoWithUsers(NoticeGroupDTO dto, List<UserInfoVO> users);

    @Named("formatUserCount")
    default String formatUserCount(Integer userCount) {
        if (userCount == null) {
            return "0个用户";
        }
        return switch (userCount) {
            case 0 -> "0个用户";
            case 1 -> "1个用户";
            default -> userCount + "个用户";
        };
    }

    @Named("formatUserCountFromList")
    default String formatUserCountFromList(List<UserInfoVO> users) {
        int count = users != null ? users.size() : 0;
        return formatUserCount(count);
    }

    @Named("calculateUserCount")
    default Integer calculateUserCount(List<UserInfoVO> users) {
        return users != null ? users.size() : 0;
    }

    @Named("formatDateTime")
    default String formatDateTime(Date date) {
        if (date == null) {
            return null;
        }
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return formatter.format(date);
    }
}