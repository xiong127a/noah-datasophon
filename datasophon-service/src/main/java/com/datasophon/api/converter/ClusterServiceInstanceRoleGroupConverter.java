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
import com.datasophon.common.enums.NeedRestart;
import com.datasophon.common.utils.FormatterUtils;
import com.datasophon.common.vo.ClusterServiceInstanceRoleGroupVO;
import com.datasophon.dao.entity.ClusterServiceInstanceRoleGroupEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

/**
 * 集群服务实例角色组转换器
 * 负责Entity、DTO、VO之间的转换
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-01
 */
@Mapper(componentModel = "spring", uses = FormatterUtils.class)
public interface ClusterServiceInstanceRoleGroupConverter
        extends
        BaseConverter<ClusterServiceInstanceRoleGroupEntity, ClusterServiceInstanceRoleGroupDTO, ClusterServiceInstanceRoleGroupVO> {

    @Override
    @Mapping(target = "needRestartText", source = "needRestart", qualifiedByName = "formatNeedRestartFromEnum")
    @Mapping(target = "createTimeFormatted", source = "createTime", qualifiedByName = "formatDateTime")
    @Mapping(target = "updateTimeFormatted", source = "updateTime", qualifiedByName = "formatDateTime")
    ClusterServiceInstanceRoleGroupVO entityToVo(ClusterServiceInstanceRoleGroupEntity entity);

    @Override
    @Mapping(target = "needRestartText", source = "needRestart", qualifiedByName = "formatNeedRestartFromInteger")
    @Mapping(target = "createTimeFormatted", source = "createTime", qualifiedByName = "formatDateTime")
    @Mapping(target = "updateTimeFormatted", source = "updateTime", qualifiedByName = "formatDateTime")
    ClusterServiceInstanceRoleGroupVO dtoToVo(ClusterServiceInstanceRoleGroupDTO dto);

    /**
     * 格式化重启需求文本（从枚举）
     */
    @Named("formatNeedRestartFromEnum")
    default String formatNeedRestartFromEnum(NeedRestart needRestart) {
        if (needRestart == null) {
            return "未知";
        }
        return switch (needRestart) {
            case YES -> "需要重启";
            case NO -> "无需重启";
        };
    }

    /**
     * 格式化重启需求文本（从整数）
     */
    @Named("formatNeedRestartFromInteger")
    default String formatNeedRestartFromInteger(Integer needRestart) {
        if (needRestart == null) {
            return "未知";
        }
        return switch (needRestart) {
            case 1 -> "需要重启";
            case 0 -> "无需重启";
            default -> "未知";
        };
    }
}