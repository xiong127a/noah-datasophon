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

import com.datasophon.common.dto.FrameServiceRoleDTO;
import com.datasophon.common.vo.FrameServiceRoleVO;
import com.datasophon.dao.entity.FrameServiceRoleEntity;
import com.datasophon.dao.enums.RoleType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

/**
 * 框架服务角色转换器
 * 负责FrameServiceRoleEntity、FrameServiceRoleDTO、FrameServiceRoleVO之间的转换
 * 特别处理RoleType枚举和运行时计算的hosts字段
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
@Mapper(componentModel = "spring")
public interface FrameServiceRoleConverter {

    /**
     * Entity转换为DTO
     */
    @Mapping(target = "serviceRoleType", source = "serviceRoleType", qualifiedByName = "roleTypeToInteger")
    FrameServiceRoleDTO entityToDto(FrameServiceRoleEntity entity);

    /**
     * DTO转换为Entity
     */
    @Mapping(target = "serviceRoleType", source = "serviceRoleType", qualifiedByName = "integerToRoleType")
    FrameServiceRoleEntity dtoToEntity(FrameServiceRoleDTO dto);

    /**
     * DTO转换为VO，添加前端展示优化字段
     */
    @Mapping(target = "serviceRoleTypeText", source = "serviceRoleType", qualifiedByName = "getRoleTypeText")
    @Mapping(target = "cardinalityDescription", source = "cardinality", qualifiedByName = "getCardinalityDescription")
    @Mapping(target = "hostsCount", source = "hosts", qualifiedByName = "getHostsCount")
    @Mapping(target = "hostsSummary", source = "hosts", qualifiedByName = "getHostsSummary")
    @Mapping(target = "hasHosts", source = "hosts", qualifiedByName = "checkHasHosts")
    @Mapping(target = "isMasterRole", source = "serviceRoleType", qualifiedByName = "checkIsMasterRole")
    @Mapping(target = "isWorkerRole", source = "serviceRoleType", qualifiedByName = "checkIsWorkerRole")
    @Mapping(target = "isClientRole", source = "serviceRoleType", qualifiedByName = "checkIsClientRole")
    FrameServiceRoleVO dtoToVo(FrameServiceRoleDTO dto);

    /**
     * VO转换为DTO
     */
    @Mapping(target = "hosts", source = "hosts")
    FrameServiceRoleDTO voToDto(FrameServiceRoleVO vo);

    /**
     * Entity列表转换为DTO列表
     */
    List<FrameServiceRoleDTO> entityListToDtoList(List<FrameServiceRoleEntity> entities);

    /**
     * DTO列表转换为VO列表
     */
    List<FrameServiceRoleVO> dtoListToVoList(List<FrameServiceRoleDTO> dtos);

    /**
     * DTO列表转换为Entity列表
     */
    List<FrameServiceRoleEntity> dtoListToEntityList(List<FrameServiceRoleDTO> dtos);

    /**
     * RoleType枚举转换为Integer
     */
    @Named("roleTypeToInteger")
    default Integer roleTypeToInteger(RoleType roleType) {
        return roleType != null ? roleType.getValue() : null;
    }

    /**
     * Integer转换为RoleType枚举
     */
    @Named("integerToRoleType")
    default RoleType integerToRoleType(Integer value) {
        if (value == null) {
            return null;
        }
        if (value.equals(1)) {
            return RoleType.MASTER;
        } else if (value.equals(2)) {
            return RoleType.WORKER;
        } else if (value.equals(3)) {
            return RoleType.CLIENT;
        } else if (value.equals(4)) {
            return RoleType.SLAVE;
        }
        return null;
    }

    /**
     * 获取角色类型显示文本
     */
    @Named("getRoleTypeText")
    default String getRoleTypeText(Integer serviceRoleType) {
        if (serviceRoleType == null) {
            return "未知";
        }
        if (serviceRoleType.equals(1)) {
            return "主节点";
        } else if (serviceRoleType.equals(2)) {
            return "工作节点";
        } else if (serviceRoleType.equals(3)) {
            return "客户端";
        } else if (serviceRoleType.equals(4)) {
            return "从节点";
        }
        return "未知";
    }

    /**
     * 获取基数描述
     */
    @Named("getCardinalityDescription")
    default String getCardinalityDescription(String cardinality) {
        if (cardinality == null || cardinality.isEmpty()) {
            return "未指定";
        }
        if ("1".equals(cardinality)) {
            return "单实例";
        } else if ("1+".equals(cardinality)) {
            return "一个或多个实例";
        } else if ("0+".equals(cardinality)) {
            return "零个或多个实例";
        }
        return cardinality;
    }

    /**
     * 获取主机数量
     */
    @Named("getHostsCount")
    default Integer getHostsCount(List<String> hosts) {
        return hosts != null ? hosts.size() : 0;
    }

    /**
     * 获取主机摘要
     */
    @Named("getHostsSummary")
    default String getHostsSummary(List<String> hosts) {
        if (hosts == null || hosts.isEmpty()) {
            return "暂无主机";
        }
        if (hosts.size() == 1) {
            return hosts.get(0);
        }
        return hosts.get(0) + " 等 " + hosts.size() + " 台主机";
    }

    /**
     * 检查是否有主机
     */
    @Named("checkHasHosts")
    default Boolean checkHasHosts(List<String> hosts) {
        return hosts != null && !hosts.isEmpty();
    }

    /**
     * 检查是否为Master角色
     */
    @Named("checkIsMasterRole")
    default Boolean checkIsMasterRole(Integer serviceRoleType) {
        return serviceRoleType != null && serviceRoleType.equals(1);
    }

    /**
     * 检查是否为Worker角色
     */
    @Named("checkIsWorkerRole")
    default Boolean checkIsWorkerRole(Integer serviceRoleType) {
        return serviceRoleType != null && serviceRoleType.equals(2);
    }

    /**
     * 检查是否为Client角色
     */
    @Named("checkIsClientRole")
    default Boolean checkIsClientRole(Integer serviceRoleType) {
        return serviceRoleType != null && serviceRoleType.equals(3);
    }
}