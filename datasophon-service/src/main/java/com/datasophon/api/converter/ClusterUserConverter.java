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
import com.datasophon.dao.entity.ClusterUser;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * 集群用户转换器
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-01
 */
@Mapper(componentModel = "spring", uses = FormatterUtils.class)
public interface ClusterUserConverter extends BaseConverter<ClusterUser, ClusterUserDTO, ClusterUserVO> {

    @Override
    ClusterUserDTO entityToDto(ClusterUser entity);

    @Override
    ClusterUser dtoToEntity(ClusterUserDTO dto);

    @Override
    @Mapping(target = "createTimeFormatted", source = "createTime", qualifiedByName = "formatDateTime")
    ClusterUserVO entityToVo(ClusterUser entity);

    @Override
    @Mapping(target = "createTimeFormatted", source = "createTime", qualifiedByName = "formatDateTime")
    ClusterUserVO dtoToVo(ClusterUserDTO dto);
}