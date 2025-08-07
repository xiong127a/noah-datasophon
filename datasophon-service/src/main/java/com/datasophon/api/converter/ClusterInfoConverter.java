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
import com.datasophon.common.dto.ClusterInfoDTO;
import com.datasophon.common.model.ClusterInfoDO;
import com.datasophon.common.vo.ClusterInfoVO;
import com.datasophon.dao.entity.ClusterInfoEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

/**
 * 集群信息转换器
 * 负责ClusterInfoEntity、ClusterInfoDTO、ClusterInfoVO之间的转换
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-01
 */
@Mapper(componentModel = "spring", uses = { UserInfoConverter.class })
public interface ClusterInfoConverter extends BaseConverter<ClusterInfoEntity, ClusterInfoDTO, ClusterInfoVO> {

    /**
     * Entity转换为VO时，添加状态文本映射
     */
    @Mapping(target = "clusterStateText", source = "clusterState", qualifiedByName = "mapEntityClusterStateText")
    @Override
    ClusterInfoVO entityToVo(ClusterInfoEntity entity);

    /**
     * DTO转换为VO时，添加状态文本映射
     */
    @Mapping(target = "clusterStateText", source = "clusterState", qualifiedByName = "mapClusterStateText")
    @Override
    ClusterInfoVO dtoToVo(ClusterInfoDTO dto);

    /**
     * 映射集群状态文本（用于DTO）
     */
    @Named("mapClusterStateText")
    default String mapClusterStateText(Integer clusterState) {
        if (clusterState == null)
            return null;
        // 这里需要根据实际的ClusterState枚举值来映射
        return switch (clusterState) {
            case 1 -> "待配置";
            case 2 -> "正在运行";
            case 3 -> "停止";
            case 4 -> "删除中";
            case 5 -> "已删除";
            default -> "未知状态";
        };
    }

    /**
     * 映射集群状态文本（用于Entity）
     */
    @Named("mapEntityClusterStateText")
    default String mapEntityClusterStateText(com.datasophon.dao.enums.ClusterState clusterState) {
        if (clusterState == null)
            return null;
        return clusterState.getDesc();
    }

    /**
     * Entity 转换为 DO（业务对象）
     * 用于业务逻辑层处理
     */
    @Mapping(target = "clusterState", source = "clusterState", qualifiedByName = "mapClusterStateToInteger")
    ClusterInfoDO entityToDo(ClusterInfoEntity entity);

    /**
     * 将枚举的ClusterState转换为Integer
     */
    @Named("mapClusterStateToInteger")
    default Integer mapClusterStateToInteger(com.datasophon.dao.enums.ClusterState clusterState) {
        return clusterState != null ? clusterState.getValue() : null;
    }
}