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
import com.datasophon.common.dto.FrameServiceDTO;
import com.datasophon.common.vo.FrameServiceVO;
import com.datasophon.dao.entity.FrameServiceEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

/**
 * 集群框架版本服务转换器
 * 负责FrameServiceEntity、FrameServiceDTO、FrameServiceVO之间的转换
 * 特别处理installed和isRequired运行时计算字段
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
@Mapper(componentModel = "spring",
        unmappedSourcePolicy = ReportingPolicy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface FrameServiceConverter extends BaseConverter<FrameServiceEntity, FrameServiceDTO, FrameServiceVO> {

    /**
     * Entity转换为VO时，生成显示相关字段
     */
    @Mapping(target = "displayName", source = ".", qualifiedByName = "generateDisplayName")
    @Mapping(target = "statusText", source = "installed", qualifiedByName = "generateStatusText")
    @Mapping(target = "installStatusText", source = ".", qualifiedByName = "generateInstallStatusText")
    @Override
    FrameServiceVO entityToVo(FrameServiceEntity entity);

    /**
     * DTO转换为VO时，生成显示相关字段
     */
    @Mapping(target = "displayName", source = ".", qualifiedByName = "generateDisplayNameFromDto")
    @Mapping(target = "statusText", source = "installed", qualifiedByName = "generateStatusText")
    @Mapping(target = "installStatusText", source = ".", qualifiedByName = "generateInstallStatusTextFromDto")
    @Override
    FrameServiceVO dtoToVo(FrameServiceDTO dto);

    /**
     * 从Entity生成显示名称
     */
    @Named("generateDisplayName")
    default String generateDisplayName(FrameServiceEntity entity) {
        if (entity == null) {
            return null;
        }
        return entity.getServiceName() != null && entity.getServiceVersion() != null
                ? entity.getServiceName() + " " + entity.getServiceVersion()
                : entity.getServiceName();
    }

    /**
     * 从DTO生成显示名称
     */
    @Named("generateDisplayNameFromDto")
    default String generateDisplayNameFromDto(FrameServiceDTO dto) {
        if (dto == null) {
            return null;
        }
        return dto.serviceName() != null && dto.serviceVersion() != null
                ? dto.serviceName() + " " + dto.serviceVersion()
                : dto.serviceName();
    }

    /**
     * 生成状态文本
     */
    @Named("generateStatusText")
    default String generateStatusText(Boolean installed) {
        return Boolean.TRUE.equals(installed) ? "已安装" : "未安装";
    }

    /**
     * 从Entity生成安装状态文本
     */
    @Named("generateInstallStatusText")
    default String generateInstallStatusText(FrameServiceEntity entity) {
        if (entity == null) {
            return "未知状态";
        }
        return generateInstallStatusTextByFlags(entity.getInstalled(), entity.getIsRequired());
    }

    /**
     * 从DTO生成安装状态文本
     */
    @Named("generateInstallStatusTextFromDto")
    default String generateInstallStatusTextFromDto(FrameServiceDTO dto) {
        if (dto == null) {
            return "未知状态";
        }
        return generateInstallStatusTextByFlags(dto.installed(), dto.isRequired());
    }

    /**
     * 根据安装和必需标志生成状态文本
     */
    default String generateInstallStatusTextByFlags(Boolean installed, Boolean isRequired) {
        if (Boolean.TRUE.equals(isRequired)) {
            return Boolean.TRUE.equals(installed) ? "必选组件-已安装" : "必选组件-未安装";
        } else {
            return Boolean.TRUE.equals(installed) ? "可选组件-已安装" : "可选组件-未安装";
        }
    }
}