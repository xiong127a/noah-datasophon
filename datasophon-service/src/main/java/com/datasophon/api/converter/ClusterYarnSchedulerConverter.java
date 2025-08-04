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
import com.datasophon.common.dto.ClusterYarnSchedulerDTO;
import com.datasophon.common.utils.FormatterUtils;
import com.datasophon.common.vo.ClusterYarnSchedulerVO;
import com.datasophon.dao.entity.ClusterYarnScheduler;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

/**
 * 集群Yarn调度器转换器
 * 负责Entity、DTO和VO之间的转换，处理状态转换
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
@Mapper(componentModel = "spring", uses = FormatterUtils.class)
public interface ClusterYarnSchedulerConverter
        extends BaseConverter<ClusterYarnScheduler, ClusterYarnSchedulerDTO, ClusterYarnSchedulerVO> {

    @Override
    @Named("entityToDto")
    ClusterYarnSchedulerDTO entityToDto(ClusterYarnScheduler entity);

    @Override
    @Named("dtoToEntity")
    ClusterYarnScheduler dtoToEntity(ClusterYarnSchedulerDTO dto);

    @Override
    @Named("entityToVo")
    @Mapping(target = "inUseText", source = "inUse", qualifiedByName = "formatInUse")
    ClusterYarnSchedulerVO entityToVo(ClusterYarnScheduler entity);

    @Override
    @Named("dtoToVo")
    @Mapping(target = "inUseText", source = "inUse", qualifiedByName = "formatInUse")
    ClusterYarnSchedulerVO dtoToVo(ClusterYarnSchedulerDTO dto);

    /**
     * 格式化使用状态文本
     * 1: 使用中, 其他: 未使用
     */
    @Named("formatInUse")
    default String formatInUse(Integer inUse) {
        if (inUse == null) {
            return "未知";
        }
        if (inUse == 1) {
            return "使用中";
        } else {
            return "未使用";
        }
    }
}