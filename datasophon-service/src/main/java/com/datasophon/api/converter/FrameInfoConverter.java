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
import com.datasophon.common.dto.FrameInfoDTO;
import com.datasophon.common.vo.FrameInfoVO;
import com.datasophon.dao.entity.FrameInfoEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

/**
 * 集群框架信息转换器
 * 负责FrameInfoEntity、FrameInfoDTO、FrameInfoVO之间的转换
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
@Mapper(componentModel = "spring",
        unmappedSourcePolicy = ReportingPolicy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface FrameInfoConverter extends BaseConverter<FrameInfoEntity, FrameInfoDTO, FrameInfoVO> {

    /**
     * DTO转Entity，忽略frameServiceList映射
     */
    @Override
    @Mapping(target = "frameServiceList", ignore = true)
    FrameInfoEntity dtoToEntity(FrameInfoDTO dto);

    /**
     * 更新Entity从DTO，忽略frameServiceList映射
     */
    @Override
    @Mapping(target = "frameServiceList", ignore = true)
    void updateEntityFromDto(FrameInfoDTO dto, @MappingTarget FrameInfoEntity entity);

    /**
     * Entity转换为VO时，生成显示名称
     */
    @Mapping(target = "displayName", source = ".", qualifiedByName = "generateDisplayName")
    @Override
    FrameInfoVO entityToVo(FrameInfoEntity entity);

    /**
     * DTO转换为VO时，生成显示名称
     */
    @Mapping(target = "displayName", source = ".", qualifiedByName = "generateDisplayNameFromDto")
    @Override
    FrameInfoVO dtoToVo(FrameInfoDTO dto);

    /**
     * 从Entity生成显示名称
     */
    @Named("generateDisplayName")
    default String generateDisplayName(FrameInfoEntity entity) {
        if (entity == null) {
            return null;
        }
        return entity.getFrameName() != null && entity.getFrameVersion() != null
                ? entity.getFrameName() + " " + entity.getFrameVersion()
                : entity.getFrameName();
    }

    /**
     * 从DTO生成显示名称
     */
    @Named("generateDisplayNameFromDto")
    default String generateDisplayNameFromDto(FrameInfoDTO dto) {
        if (dto == null) {
            return null;
        }
        return dto.frameName() != null && dto.frameVersion() != null
                ? dto.frameName() + " " + dto.frameVersion()
                : dto.frameName();
    }
}