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
import com.datasophon.common.dto.ClusterServiceInstanceDTO;
import com.datasophon.common.utils.FormatterUtils;
import com.datasophon.common.vo.ClusterServiceInstanceVO;
import com.datasophon.dao.entity.ClusterServiceInstanceEntity;
import com.datasophon.common.enums.NeedRestart;
import com.datasophon.common.enums.ServiceState;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

/**
 * 集群服务实例转换器
 * 负责ClusterServiceInstanceEntity、ClusterServiceInstanceDTO、ClusterServiceInstanceVO之间的转换
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-01
 */
@Mapper(componentModel = "spring", uses = { FormatterUtils.class })
public interface ClusterServiceInstanceConverter
        extends BaseConverter<ClusterServiceInstanceEntity, ClusterServiceInstanceDTO, ClusterServiceInstanceVO> {

    /**
     * Entity转换为DTO时，枚举转换为Integer
     */
    @Mapping(target = "serviceState", source = "serviceState", qualifiedByName = "serviceStateToInteger")
    @Mapping(target = "needRestart", source = "needRestart", qualifiedByName = "needRestartToInteger")
    @Override
    ClusterServiceInstanceDTO entityToDto(ClusterServiceInstanceEntity entity);

    /**
     * DTO转换为Entity时，Integer转换为枚举
     */
    @Mapping(target = "serviceState", source = "serviceState", qualifiedByName = "integerToServiceState")
    @Mapping(target = "needRestart", source = "needRestart", qualifiedByName = "integerToNeedRestart")
    @Override
    ClusterServiceInstanceEntity dtoToEntity(ClusterServiceInstanceDTO dto);

    /**
     * Entity转换为VO时，添加格式化字段
     */
    @Mapping(target = "serviceState", source = "serviceState", qualifiedByName = "serviceStateToInteger")
    @Mapping(target = "serviceStateText", source = "serviceState", qualifiedByName = "mapServiceStateText")
    @Mapping(target = "needRestart", source = "needRestart", qualifiedByName = "needRestartToInteger")
    @Mapping(target = "needRestartText", source = "needRestart", qualifiedByName = "mapNeedRestartText")
    @Mapping(target = "updateTimeFormatted", source = "updateTime", qualifiedByName = "formatDateTime")
    @Mapping(target = "createTimeFormatted", source = "createTime", qualifiedByName = "formatDateTime")
    @Override
    ClusterServiceInstanceVO entityToVo(ClusterServiceInstanceEntity entity);

    /**
     * DTO转换为VO时，添加格式化字段
     */
    @Mapping(target = "serviceStateText", source = "serviceState", qualifiedByName = "mapIntegerServiceStateText")
    @Mapping(target = "needRestartText", source = "needRestart", qualifiedByName = "mapIntegerNeedRestartText")
    @Mapping(target = "updateTimeFormatted", source = "updateTime", qualifiedByName = "formatDateTime")
    @Mapping(target = "createTimeFormatted", source = "createTime", qualifiedByName = "formatDateTime")
    @Override
    ClusterServiceInstanceVO dtoToVo(ClusterServiceInstanceDTO dto);

    /**
     * ServiceState枚举转换为Integer
     */
    @Named("serviceStateToInteger")
    default Integer serviceStateToInteger(ServiceState serviceState) {
        if (serviceState == null) {
            return null;
        }
        return serviceState.getValue();
    }

    /**
     * Integer转换为ServiceState枚举
     */
    @Named("integerToServiceState")
    default ServiceState integerToServiceState(Integer serviceState) {
        if (serviceState == null) {
            return null;
        }
        if (serviceState == 1) {
            return ServiceState.WAIT_INSTALL;
        } else if (serviceState == 2) {
            return ServiceState.RUNNING;
        } else if (serviceState == 3) {
            return ServiceState.EXISTS_ALARM;
        } else if (serviceState == 4) {
            return ServiceState.EXISTS_EXCEPTION;
        } else {
            return ServiceState.WAIT_INSTALL;
        }
    }

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
     * 映射服务状态文本（用于Entity的ServiceState枚举）
     */
    @Named("mapServiceStateText")
    default String mapServiceStateText(ServiceState serviceState) {
        if (serviceState == null) {
            return null;
        }
        return serviceState.getDesc();
    }

    /**
     * 映射服务状态文本（用于DTO的Integer类型）
     */
    @Named("mapIntegerServiceStateText")
    default String mapIntegerServiceStateText(Integer serviceState) {
        if (serviceState == null) {
            return null;
        }
        if (serviceState == 1) {
            return "待安装";
        } else if (serviceState == 2) {
            return "正常";
        } else if (serviceState == 3) {
            return "存在告警";
        } else if (serviceState == 4) {
            return "存在异常";
        } else {
            return "未知状态";
        }
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