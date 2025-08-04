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
import com.datasophon.common.dto.ClusterServiceInstanceRoleGroupDTO;
import com.datasophon.common.vo.ClusterServiceInstanceRoleGroupVO;
import com.datasophon.dao.entity.ClusterServiceInstanceRoleGroup;
import com.datasophon.dao.enums.NeedRestart;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

/**
 * 集群服务实例角色组转换器
 * 负责ClusterServiceInstanceRoleGroup Entity、DTO、VO之间的转换
 * 处理NeedRestart枚举转换
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-01
 */
@Mapper(componentModel = "spring")
public interface ClusterServiceInstanceRoleGroupConverter extends
        BaseConverter<ClusterServiceInstanceRoleGroup, ClusterServiceInstanceRoleGroupDTO, ClusterServiceInstanceRoleGroupVO> {

    /**
     * Entity转换为DTO时，枚举转换为Integer
     */
    @Mapping(target = "needRestart", source = "needRestart", qualifiedByName = "needRestartToInteger")
    @Override
    ClusterServiceInstanceRoleGroupDTO entityToDto(ClusterServiceInstanceRoleGroup entity);

    /**
     * DTO转换为Entity时，Integer转换为枚举
     */
    @Mapping(target = "needRestart", source = "needRestart", qualifiedByName = "integerToNeedRestart")
    @Override
    ClusterServiceInstanceRoleGroup dtoToEntity(ClusterServiceInstanceRoleGroupDTO dto);

    /**
     * Entity转换为VO时，添加格式化字段
     */
    @Mapping(target = "needRestart", source = "needRestart", qualifiedByName = "needRestartToInteger")
    @Mapping(target = "needRestartText", source = "needRestart", qualifiedByName = "mapNeedRestartText")
    @Override
    ClusterServiceInstanceRoleGroupVO entityToVo(ClusterServiceInstanceRoleGroup entity);

    /**
     * DTO转换为VO时，添加格式化字段
     */
    @Mapping(target = "needRestartText", source = "needRestart", qualifiedByName = "mapIntegerNeedRestartText")
    @Override
    ClusterServiceInstanceRoleGroupVO dtoToVo(ClusterServiceInstanceRoleGroupDTO dto);

    // ================== NeedRestart 转换方法 ==================

    /**
     * NeedRestart枚举转换为Integer
     */
    @Named("needRestartToInteger")
    default Integer needRestartToInteger(NeedRestart needRestart) {
        if (needRestart == null) {
            return null;
        }
        return needRestart.getValue();
    }

    /**
     * Integer转换为NeedRestart枚举
     */
    @Named("integerToNeedRestart")
    default NeedRestart integerToNeedRestart(Integer needRestart) {
        if (needRestart == null) {
            return null;
        }
        return needRestart == 1 ? NeedRestart.NO : NeedRestart.YES;
    }

    /**
     * 映射重启需求文本（用于Entity的NeedRestart枚举）
     */
    @Named("mapNeedRestartText")
    default String mapNeedRestartText(NeedRestart needRestart) {
        if (needRestart == null) {
            return null;
        }
        return needRestart.isDesc() ? "需要重启" : "无需重启";
    }

    /**
     * 映射重启需求文本（用于DTO的Integer类型）
     */
    @Named("mapIntegerNeedRestartText")
    default String mapIntegerNeedRestartText(Integer needRestart) {
        if (needRestart == null) {
            return null;
        }
        return needRestart == 2 ? "需要重启" : "无需重启";
    }
}