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
import com.datasophon.common.dto.ClusterUserDTO;
import com.datasophon.common.utils.FormatterUtils;
import com.datasophon.common.vo.ClusterUserVO;
import com.datasophon.dao.entity.ClusterUserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

/**
 * 集群用户转换器
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-01
 */
@Mapper(componentModel = "spring", uses = FormatterUtils.class,
        unmappedSourcePolicy = ReportingPolicy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ClusterUserConverter extends BaseConverter<ClusterUserEntity, ClusterUserDTO, ClusterUserVO> {

    @Override
    ClusterUserDTO entityToDto(ClusterUserEntity entity);

    @Override
    ClusterUserEntity dtoToEntity(ClusterUserDTO dto);

    @Override
    @Mapping(target = "createTimeFormatted", ignore = true)
    ClusterUserVO entityToVo(ClusterUserEntity entity);

    @Override
    @Mapping(target = "createTimeFormatted", source = "createTime", qualifiedByName = "formatDateTime")
    ClusterUserVO dtoToVo(ClusterUserDTO dto);

    @Override
    java.util.List<ClusterUserVO> entityListToVoList(java.util.List<ClusterUserEntity> entityList);

    @Override
    java.util.List<ClusterUserVO> dtoListToVoList(java.util.List<ClusterUserDTO> dtoList);

    @Override
    void updateEntityFromDto(ClusterUserDTO dto, @MappingTarget ClusterUserEntity entity);
}