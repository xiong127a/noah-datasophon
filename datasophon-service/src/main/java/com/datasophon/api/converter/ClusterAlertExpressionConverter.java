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
import com.datasophon.common.dto.ClusterAlertExpressionDTO;
import com.datasophon.common.utils.FormatterUtils;
import com.datasophon.common.vo.ClusterAlertExpressionVO;
import com.datasophon.dao.entity.ClusterAlertExpressionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * 集群告警表达式转换器
 * 负责ClusterAlertExpression Entity、DTO、VO之间的转换
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
@Mapper(componentModel = "spring", uses = FormatterUtils.class)
public interface ClusterAlertExpressionConverter extends
        BaseConverter<ClusterAlertExpressionEntity, ClusterAlertExpressionDTO, ClusterAlertExpressionVO> {

    /**
     * Entity转换为VO时，添加格式化字段
     */
    @Mapping(target = "createTimeFormatted", source = "createTime", qualifiedByName = "formatDateTime")
    @Mapping(target = "updateTimeFormatted", source = "updateTime", qualifiedByName = "formatDateTime")
    @Override
    ClusterAlertExpressionVO entityToVo(ClusterAlertExpressionEntity entity);

    /**
     * DTO转换为VO时，添加格式化字段
     */
    @Mapping(target = "createTimeFormatted", source = "createTime", qualifiedByName = "formatDateTime")
    @Mapping(target = "updateTimeFormatted", source = "updateTime", qualifiedByName = "formatDateTime")
    @Override
    ClusterAlertExpressionVO dtoToVo(ClusterAlertExpressionDTO dto);
}