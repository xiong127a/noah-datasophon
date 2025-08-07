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
import com.datasophon.common.dto.ClusterServiceRoleInstanceDTO;
import com.datasophon.common.utils.FormatterUtils;
import com.datasophon.common.vo.ClusterServiceRoleInstanceVO;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;
import com.datasophon.common.enums.NeedRestart;
import com.datasophon.common.enums.RoleType;
import com.datasophon.common.enums.ServiceRoleState;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

/**
 * 集群服务角色实例转换器
 * 负责ClusterServiceRoleInstanceEntity、ClusterServiceRoleInstanceDTO、ClusterServiceRoleInstanceVO之间的转换
 * 处理ServiceRoleState、RoleType、NeedRestart三个枚举的转换
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-01
 */
@Mapper(componentModel = "spring", uses = { FormatterUtils.class })
public interface ClusterServiceRoleInstanceConverter extends BaseConverter<ClusterServiceRoleInstanceEntity, ClusterServiceRoleInstanceDTO, ClusterServiceRoleInstanceVO> {

    /**
     * Entity转换为DTO时，枚举转换为Integer
     */
    @Mapping(target = "serviceRoleState", source = "serviceRoleState", qualifiedByName = "serviceRoleStateToInteger")
    @Mapping(target = "roleType", source = "roleType", qualifiedByName = "roleTypeToInteger")
    @Mapping(target = "needRestart", source = "needRestart", qualifiedByName = "needRestartToInteger")
    @Override
    ClusterServiceRoleInstanceDTO entityToDto(ClusterServiceRoleInstanceEntity entity);

    /**
     * DTO转换为Entity时，Integer转换为枚举
     */
    @Mapping(target = "serviceRoleState", source = "serviceRoleState", qualifiedByName = "integerToServiceRoleState")
    @Mapping(target = "roleType", source = "roleType", qualifiedByName = "integerToRoleType")
    @Mapping(target = "needRestart", source = "needRestart", qualifiedByName = "integerToNeedRestart")
    @Override
    ClusterServiceRoleInstanceEntity dtoToEntity(ClusterServiceRoleInstanceDTO dto);

    /**
     * Entity转换为VO时，添加格式化字段
     */
    @Mapping(target = "serviceRoleState", source = "serviceRoleState", qualifiedByName = "serviceRoleStateToInteger")
    @Mapping(target = "serviceRoleStateText", source = "serviceRoleState", qualifiedByName = "mapServiceRoleStateText")
    @Mapping(target = "roleType", source = "roleType", qualifiedByName = "roleTypeToInteger")
    @Mapping(target = "roleTypeText", source = "roleType", qualifiedByName = "mapRoleTypeText")
    @Mapping(target = "needRestart", source = "needRestart", qualifiedByName = "needRestartToInteger")
    @Mapping(target = "needRestartText", source = "needRestart", qualifiedByName = "mapNeedRestartText")
    @Mapping(target = "updateTimeFormatted", source = "updateTime", qualifiedByName = "formatDateTime")
    @Mapping(target = "createTimeFormatted", source = "createTime", qualifiedByName = "formatDateTime")
    @Override
    ClusterServiceRoleInstanceVO entityToVo(ClusterServiceRoleInstanceEntity entity);

    /**
     * DTO转换为VO时，添加格式化字段
     */
    @Mapping(target = "serviceRoleStateText", source = "serviceRoleState", qualifiedByName = "mapIntegerServiceRoleStateText")
    @Mapping(target = "roleTypeText", source = "roleType", qualifiedByName = "mapIntegerRoleTypeText")
    @Mapping(target = "needRestartText", source = "needRestart", qualifiedByName = "mapIntegerNeedRestartText")
    @Mapping(target = "updateTimeFormatted", source = "updateTime", qualifiedByName = "formatDateTime")
    @Mapping(target = "createTimeFormatted", source = "createTime", qualifiedByName = "formatDateTime")
    @Override
    ClusterServiceRoleInstanceVO dtoToVo(ClusterServiceRoleInstanceDTO dto);

    // ================== ServiceRoleState 转换方法 ==================

    /**
     * ServiceRoleState枚举转换为Integer
     */
    @Named("serviceRoleStateToInteger")
    default Integer serviceRoleStateToInteger(ServiceRoleState serviceRoleState) {
        if (serviceRoleState == null) {
            return null;
        }
        return serviceRoleState.getValue();
    }

    /**
     * Integer转换为ServiceRoleState枚举
     */
    @Named("integerToServiceRoleState")
    default ServiceRoleState integerToServiceRoleState(Integer serviceRoleState) {
        if (serviceRoleState == null) {
            return null;
        }
        if (serviceRoleState == 1) {
            return ServiceRoleState.RUNNING;
        } else if (serviceRoleState == 2) {
            return ServiceRoleState.STOP;
        } else if (serviceRoleState == 3) {
            return ServiceRoleState.EXISTS_ALARM;
        } else if (serviceRoleState == 4) {
            return ServiceRoleState.DECOMMISSIONING;
        } else if (serviceRoleState == 5) {
            return ServiceRoleState.DECOMMISSIONED;
        } else {
            return ServiceRoleState.STOP;
        }
    }

    /**
     * 映射服务角色状态文本（用于Entity的ServiceRoleState枚举）
     */
    @Named("mapServiceRoleStateText")
    default String mapServiceRoleStateText(ServiceRoleState serviceRoleState) {
        if (serviceRoleState == null) {
            return null;
        }
        return serviceRoleState.getDesc();
    }

    /**
     * 映射服务角色状态文本（用于DTO的Integer类型）
     */
    @Named("mapIntegerServiceRoleStateText")
    default String mapIntegerServiceRoleStateText(Integer serviceRoleState) {
        if (serviceRoleState == null) {
            return null;
        }
        if (serviceRoleState == 1) {
            return "正在运行";
        } else if (serviceRoleState == 2) {
            return "停止";
        } else if (serviceRoleState == 3) {
            return "存在告警";
        } else if (serviceRoleState == 4) {
            return "退役中";
        } else if (serviceRoleState == 5) {
            return "已退役";
        } else {
            return "未知状态";
        }
    }

    // ================== RoleType 转换方法 ==================

    /**
     * RoleType枚举转换为Integer
     */
    @Named("roleTypeToInteger")
    default Integer roleTypeToInteger(RoleType roleType) {
        if (roleType == null) {
            return null;
        }
        return roleType.getValue();
    }

    /**
     * Integer转换为RoleType枚举
     */
    @Named("integerToRoleType")
    default RoleType integerToRoleType(Integer roleType) {
        if (roleType == null) {
            return null;
        }
        if (roleType == 1) {
            return RoleType.MASTER;
        } else if (roleType == 2) {
            return RoleType.WORKER;
        } else if (roleType == 3) {
            return RoleType.CLIENT;
        } else if (roleType == 4) {
            return RoleType.SLAVE;
        } else {
            return RoleType.WORKER;
        }
    }

    /**
     * 映射角色类型文本（用于Entity的RoleType枚举）
     */
    @Named("mapRoleTypeText")
    default String mapRoleTypeText(RoleType roleType) {
        if (roleType == null) {
            return null;
        }
        return roleType.getDesc();
    }

    /**
     * 映射角色类型文本（用于DTO的Integer类型）
     */
    @Named("mapIntegerRoleTypeText")
    default String mapIntegerRoleTypeText(Integer roleType) {
        if (roleType == null) {
            return null;
        }
        if (roleType == 1) {
            return "master";
        } else if (roleType == 2) {
            return "worker";
        } else if (roleType == 3) {
            return "client";
        } else if (roleType == 4) {
            return "slave";
        } else {
            return "未知类型";
        }
    }

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