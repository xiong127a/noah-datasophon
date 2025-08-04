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
import com.datasophon.common.dto.ClusterUserGroupDTO;
import com.datasophon.common.vo.ClusterUserGroupVO;
import com.datasophon.dao.entity.ClusterUserGroup;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

/**
 * 集群用户组关联转换器
 * 负责ClusterUserGroup Entity、DTO、VO之间的转换
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
@Mapper(componentModel = "spring")
public interface ClusterUserGroupConverter extends
        BaseConverter<ClusterUserGroup, ClusterUserGroupDTO, ClusterUserGroupVO> {

    /**
     * Entity转换为VO时，添加用户组类型描述
     */
    @Mapping(target = "userGroupTypeText", source = "userGroupType", qualifiedByName = "mapUserGroupTypeText")
    @Override
    ClusterUserGroupVO entityToVo(ClusterUserGroup entity);

    /**
     * DTO转换为VO时，添加用户组类型描述
     */
    @Mapping(target = "userGroupTypeText", source = "userGroupType", qualifiedByName = "mapUserGroupTypeText")
    @Override
    ClusterUserGroupVO dtoToVo(ClusterUserGroupDTO dto);

    /**
     * 将用户组类型数字转换为文本描述
     *
     * @param userGroupType 用户组类型
     * @return 类型描述
     */
    @Named("mapUserGroupTypeText")
    default String mapUserGroupTypeText(Integer userGroupType) {
        if (userGroupType == null) {
            return "未知";
        }
        return switch (userGroupType) {
            case 1 -> "主用户组";
            case 2 -> "其他用户组";
            default -> "未知类型";
        };
    }
}