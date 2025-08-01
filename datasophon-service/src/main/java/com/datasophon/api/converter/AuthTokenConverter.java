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

import com.datasophon.common.dto.AuthTokenDTO;
import com.datasophon.api.vo.AuthTokenVO;
import com.datasophon.dao.entity.AuthTokenEntity;
import com.datasophon.common.converter.BaseConverter;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

/**
 * AuthToken对象转换器
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-01
 */
@Mapper(componentModel = "spring")
public interface AuthTokenConverter extends BaseConverter<AuthTokenEntity, AuthTokenDTO, AuthTokenVO> {

    @Override
    AuthTokenDTO entityToDto(AuthTokenEntity entity);

    @Override
    AuthTokenEntity dtoToEntity(AuthTokenDTO dto);

    @Override
    @Mapping(target = "username", ignore = true)
    @Mapping(target = "status", source = "isRevoked", qualifiedByName = "formatStatus")
    @Mapping(target = "issuedAtFormatted", source = "issuedAt", qualifiedByName = "formatDateTime")
    @Mapping(target = "expiresAtFormatted", source = "expiresAt", qualifiedByName = "formatDateTime")
    @Mapping(target = "lastAccessTimeFormatted", source = "lastAccessTime", qualifiedByName = "formatDateTime")
    @Mapping(target = "createdAtFormatted", source = "createdAt", qualifiedByName = "formatDateTime")
    @Mapping(target = "remainingTimeSeconds", source = "expiresAt", qualifiedByName = "calculateRemainingSeconds")
    @Mapping(target = "remainingTimeFormatted", source = "expiresAt", qualifiedByName = "formatRemainingTime")
    AuthTokenVO dtoToVo(AuthTokenDTO dto);

    @Override
    List<AuthTokenDTO> entityListToDtoList(List<AuthTokenEntity> entityList);

    @Override
    List<AuthTokenVO> dtoListToVoList(List<AuthTokenDTO> dtoList);

    @Override
    @Mapping(target = "username", ignore = true)
    @Mapping(target = "status", source = "isRevoked", qualifiedByName = "formatStatus")
    @Mapping(target = "issuedAtFormatted", source = "issuedAt", qualifiedByName = "formatDateTime")
    @Mapping(target = "expiresAtFormatted", source = "expiresAt", qualifiedByName = "formatDateTime")
    @Mapping(target = "lastAccessTimeFormatted", source = "lastAccessTime", qualifiedByName = "formatDateTime")
    @Mapping(target = "createdAtFormatted", source = "createdAt", qualifiedByName = "formatDateTime")
    @Mapping(target = "remainingTimeSeconds", source = "expiresAt", qualifiedByName = "calculateRemainingSeconds")
    @Mapping(target = "remainingTimeFormatted", source = "expiresAt", qualifiedByName = "formatRemainingTime")
    AuthTokenVO entityToVo(AuthTokenEntity entity);

    @Override
    List<AuthTokenVO> entityListToVoList(List<AuthTokenEntity> entityList);

    @Override
    void updateEntityFromDto(AuthTokenDTO dto, AuthTokenEntity entity);

    /**
     * 创建包含用户名的VO
     */
    @Mapping(target = "status", source = "dto.isRevoked", qualifiedByName = "formatStatus")
    @Mapping(target = "issuedAtFormatted", source = "dto.issuedAt", qualifiedByName = "formatDateTime")
    @Mapping(target = "expiresAtFormatted", source = "dto.expiresAt", qualifiedByName = "formatDateTime")
    @Mapping(target = "lastAccessTimeFormatted", source = "dto.lastAccessTime", qualifiedByName = "formatDateTime")
    @Mapping(target = "createdAtFormatted", source = "dto.createdAt", qualifiedByName = "formatDateTime")
    @Mapping(target = "remainingTimeSeconds", source = "dto.expiresAt", qualifiedByName = "calculateRemainingSeconds")
    @Mapping(target = "remainingTimeFormatted", source = "dto.expiresAt", qualifiedByName = "formatRemainingTime")
    AuthTokenVO dtoToVoWithUsername(AuthTokenDTO dto, String username);

    /**
     * 格式化令牌状态（特定业务逻辑）
     */
    @Named("formatStatus")
    default String formatStatus(Boolean isRevoked) {
        if (Boolean.TRUE.equals(isRevoked)) {
            return "已撤销";
        }
        return "有效";
    }
}