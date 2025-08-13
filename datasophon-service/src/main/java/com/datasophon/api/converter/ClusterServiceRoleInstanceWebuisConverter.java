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
import com.datasophon.common.dto.ClusterServiceRoleInstanceWebuisDTO;
import com.datasophon.common.vo.ClusterServiceRoleInstanceWebuisVO;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceWebuisEntity;
import org.mapstruct.Mapper;

/**
 * 集群服务角色实例WebUI转换器
 * 继承BaseConverter，提供Entity、DTO、VO之间的转换
 * 使用MapStruct注解优化，避免IDE"未使用"警告
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-06
 */
@Mapper(componentModel = "spring")
public interface ClusterServiceRoleInstanceWebuisConverter 
    extends BaseConverter<ClusterServiceRoleInstanceWebuisEntity, ClusterServiceRoleInstanceWebuisDTO, ClusterServiceRoleInstanceWebuisVO> {
    
    /**
     * Entity转DTO - 标准转换
     */
    @Override
    ClusterServiceRoleInstanceWebuisDTO entityToDto(ClusterServiceRoleInstanceWebuisEntity entity);
    
    /**
     * DTO转Entity - 标准转换
     */
    @Override
    ClusterServiceRoleInstanceWebuisEntity dtoToEntity(ClusterServiceRoleInstanceWebuisDTO dto);
    
    /**
     * DTO转VO - 使用静态工厂方法
     */
    @Override
    default ClusterServiceRoleInstanceWebuisVO dtoToVo(ClusterServiceRoleInstanceWebuisDTO dto) {
        return ClusterServiceRoleInstanceWebuisVO.fromDTO(dto);
    }
    
    /**
     * Entity转VO - 通过DTO中转
     */
    @Override
    default ClusterServiceRoleInstanceWebuisVO entityToVo(ClusterServiceRoleInstanceWebuisEntity entity) {
        return dtoToVo(entityToDto(entity));
    }
}