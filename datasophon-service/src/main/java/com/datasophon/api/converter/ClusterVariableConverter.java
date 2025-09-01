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
import com.datasophon.common.dto.ClusterVariableDTO;
import com.datasophon.common.utils.FormatterUtils;
import com.datasophon.common.vo.ClusterVariableVO;
import com.datasophon.dao.entity.ClusterVariableEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

/**
 * 集群变量转换器
 * 负责Entity、DTO和VO之间的转换
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
@Mapper(componentModel = "spring", uses = FormatterUtils.class)
public interface ClusterVariableConverter
        extends BaseConverter<ClusterVariableEntity, ClusterVariableDTO, ClusterVariableVO> {

    @Override
    @Named("entityToDto")
    ClusterVariableDTO entityToDto(ClusterVariableEntity entity);

    @Override
    @Named("dtoToEntity")
    ClusterVariableEntity dtoToEntity(ClusterVariableDTO dto);

    @Override
    @Named("entityToVo")
    @Mapping(target = "createTimeFormatted", source = "createTime", qualifiedByName = "formatDateTime")
    @Mapping(target = "updateTimeFormatted", source = "updateTime", qualifiedByName = "formatDateTime")
    ClusterVariableVO entityToVo(ClusterVariableEntity entity);

    @Override
    @Named("dtoToVo")
    @Mapping(target = "createTimeFormatted", source = "createTime", qualifiedByName = "formatDateTime")
    @Mapping(target = "updateTimeFormatted", source = "updateTime", qualifiedByName = "formatDateTime")
    ClusterVariableVO dtoToVo(ClusterVariableDTO dto);
}