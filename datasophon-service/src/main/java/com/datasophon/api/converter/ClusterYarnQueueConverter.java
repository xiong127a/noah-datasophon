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
import com.datasophon.common.dto.ClusterYarnQueueDTO;
import com.datasophon.common.utils.FormatterUtils;
import com.datasophon.common.vo.ClusterYarnQueueVO;
import com.datasophon.dao.entity.ClusterYarnQueueEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

/**
 * 集群Yarn队列转换器
 * 负责Entity、DTO和VO之间的转换，处理资源计算和状态转换
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
@Mapper(componentModel = "spring", uses = FormatterUtils.class,
        unmappedSourcePolicy = ReportingPolicy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ClusterYarnQueueConverter
        extends BaseConverter<ClusterYarnQueueEntity, ClusterYarnQueueDTO, ClusterYarnQueueVO> {

    @Override
    @Named("entityToDto")
    @Mapping(target = "minResources", source = ".", qualifiedByName = "formatMinResources")
    @Mapping(target = "maxResources", source = ".", qualifiedByName = "formatMaxResources")
    ClusterYarnQueueDTO entityToDto(ClusterYarnQueueEntity entity);

    @Override
    @Named("dtoToEntity")
    ClusterYarnQueueEntity dtoToEntity(ClusterYarnQueueDTO dto);

    @Override
    @Named("entityToVo")
    @Mapping(target = "minResources", source = ".", qualifiedByName = "formatMinResources")
    @Mapping(target = "maxResources", source = ".", qualifiedByName = "formatMaxResources")
    @Mapping(target = "allowPreemptionText", source = "allowPreemption", qualifiedByName = "formatAllowPreemption")
    @Mapping(target = "createTimeFormatted", source = "createTime", qualifiedByName = "formatDateTime")
    ClusterYarnQueueVO entityToVo(ClusterYarnQueueEntity entity);

    @Override
    @Named("dtoToVo")
    @Mapping(target = "allowPreemptionText", source = "allowPreemption", qualifiedByName = "formatAllowPreemption")
    @Mapping(target = "createTimeFormatted", expression = "java(com.datasophon.common.utils.FormatterUtils.formatDateTime(dto.createTime()))")
    ClusterYarnQueueVO dtoToVo(ClusterYarnQueueDTO dto);

    @Override
    java.util.List<ClusterYarnQueueVO> entityListToVoList(java.util.List<ClusterYarnQueueEntity> entityList);

    @Override
    java.util.List<ClusterYarnQueueVO> dtoListToVoList(java.util.List<ClusterYarnQueueDTO> dtoList);

    /**
     * 格式化最小资源
     */
    @Named("formatMinResources")
    default String formatMinResources(ClusterYarnQueueEntity entity) {
        if (entity.getMinCore() == null || entity.getMinMem() == null) {
            return "";
        }
        return entity.getMinCore() + "Core," + entity.getMinMem() + "GB";
    }

    /**
     * 格式化最大资源
     */
    @Named("formatMaxResources")
    default String formatMaxResources(ClusterYarnQueueEntity entity) {
        if (entity.getMaxCore() == null || entity.getMaxMem() == null) {
            return "";
        }
        return entity.getMaxCore() + "Core," + entity.getMaxMem() + "GB";
    }

    /**
     * 格式化抢占设置文本
     * 1: true, 2: false
     */
    @Named("formatAllowPreemption")
    default String formatAllowPreemption(Integer allowPreemption) {
        if (allowPreemption == null) {
            return "未知";
        }
        return switch (allowPreemption) {
            case 1 -> "允许";
            case 2 -> "不允许";
            default -> "未知";
        };
    }
}