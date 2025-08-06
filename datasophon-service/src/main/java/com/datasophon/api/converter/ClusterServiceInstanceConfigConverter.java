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
import com.datasophon.common.dto.ClusterServiceInstanceConfigDTO;
import com.datasophon.common.vo.ClusterServiceInstanceConfigVO;
import com.datasophon.dao.entity.ClusterServiceInstanceConfigEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * 集群服务实例配置转换器
 * 继承BaseConverter，提供Entity、DTO、VO之间的转换
 * 使用MapStruct注解优化，避免IDE"未使用"警告
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-06
 */
@Mapper(componentModel = "spring")
public interface ClusterServiceInstanceConfigConverter 
    extends BaseConverter<ClusterServiceInstanceConfigEntity, ClusterServiceInstanceConfigDTO, ClusterServiceInstanceConfigVO> {
    
    /**
     * Entity转DTO
     * 处理Date到LocalDateTime的转换
     */
    @Override
    @Mapping(target = "createTime", source = "createTime", qualifiedByName = "dateToLocalDateTime")
    @Mapping(target = "updateTime", source = "updateTime", qualifiedByName = "dateToLocalDateTime")
    ClusterServiceInstanceConfigDTO entityToDto(ClusterServiceInstanceConfigEntity entity);
    
    /**
     * DTO转Entity
     * 处理LocalDateTime到Date的转换
     */
    @Override
    @Mapping(target = "createTime", source = "createTime", qualifiedByName = "localDateTimeToDate")
    @Mapping(target = "updateTime", source = "updateTime", qualifiedByName = "localDateTimeToDate")
    ClusterServiceInstanceConfigEntity dtoToEntity(ClusterServiceInstanceConfigDTO dto);
    
    /**
     * DTO转VO - 使用静态工厂方法
     */
    @Override
    default ClusterServiceInstanceConfigVO dtoToVo(ClusterServiceInstanceConfigDTO dto) {
        return ClusterServiceInstanceConfigVO.fromDTO(dto);
    }
    
    /**
     * Entity转VO - 通过DTO中转
     */
    @Override
    default ClusterServiceInstanceConfigVO entityToVo(ClusterServiceInstanceConfigEntity entity) {
        return dtoToVo(entityToDto(entity));
    }
    
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
}