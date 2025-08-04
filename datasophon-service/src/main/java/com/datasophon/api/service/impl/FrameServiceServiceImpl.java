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

package com.datasophon.api.service.impl;

import cn.hutool.core.util.StrUtil;
import com.datasophon.api.converter.FrameServiceConverter;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.api.service.ClusterServiceInstanceService;
import com.datasophon.api.service.FrameServiceService;
import com.datasophon.common.dto.ClusterInfoDTO;
import com.datasophon.common.dto.ClusterServiceInstanceDTO;
import com.datasophon.common.dto.FrameServiceDTO;
import com.datasophon.common.exception.BusinessException;

import com.datasophon.dao.entity.FrameInfoEntity;
import com.datasophon.dao.entity.FrameServiceEntity;
import com.datasophon.dao.enums.ServiceState;
import com.datasophon.dao.mapper.FrameInfoMapper;
import com.datasophon.dao.mapper.FrameServiceMapper;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * 集群框架版本服务表服务实现
 * 继承ServiceImpl提供基础CRUD操作，使用Converter进行对象转换
 * 保持复杂业务逻辑完整性，按照架构重构规范返回DTO对象
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-01
 */
@Slf4j
@Service("frameServiceService")
@RequiredArgsConstructor
public class FrameServiceServiceImpl extends ServiceImpl<FrameServiceMapper, FrameServiceEntity>
        implements FrameServiceService {

    private final FrameServiceConverter frameServiceConverter;
    private final ClusterInfoService clusterInfoService;
    private final FrameInfoMapper frameInfoMapper;
    private final ClusterServiceInstanceService serviceInstanceService;

    private static final List<String> CUSTOM_REQUIRED_SERVICE = List.of(
            "ALERTMANAGER", "GRAFANA", "OPENLDAP", "PROMETHEUS", "RANGER");

    private static final List<String> DATALAKE_REQUIRED_SERVICE = List.of(
            "ALERTMANAGER", "GRAFANA", "OPENLDAP", "PROMETHEUS", "RANGER", "HDFS", "YARN", "HUDI", "HIVE", "ICEBERG",
            "SPARK3", "FLINK");

    @Override
    public List<FrameServiceDTO> getAllFrameService(Integer clusterId) {
        if (clusterId == null) {
            throw new BusinessException("集群ID不能为空");
        }

        ClusterInfoDTO clusterInfo = clusterInfoService.getClusterById(clusterId);
        if (clusterInfo == null) {
            throw new BusinessException("未找到ID为 " + clusterId + " 的集群信息");
        }

        FrameInfoEntity frameInfo = frameInfoMapper.getFrameInfoByFrameCode(clusterInfo.clusterFrame());
        if (frameInfo == null) {
            throw new BusinessException("未找到框架代码为 " + clusterInfo.clusterFrame() + " 的框架信息");
        }

        List<FrameServiceEntity> entities = getMapper().selectByFrameIdOrderBySortNum(frameInfo.getId());
        List<FrameServiceDTO> dtos = frameServiceConverter.entityListToDtoList(entities);

        return setInstalledStatus(clusterId, dtos);
    }

    @Override
    public List<FrameServiceDTO> getAllFrameServiceWithRequired(Integer clusterId, String type) {
        if (clusterId == null) {
            throw new BusinessException("集群ID不能为空");
        }
        if (StrUtil.isBlank(type)) {
            throw new BusinessException("集群类型不能为空");
        }

        ClusterInfoDTO clusterInfo = clusterInfoService.getClusterById(clusterId);
        if (clusterInfo == null) {
            throw new BusinessException("未找到ID为 " + clusterId + " 的集群信息");
        }

        FrameInfoEntity frameInfo = frameInfoMapper.getFrameInfoByFrameCode(clusterInfo.clusterFrame());
        if (frameInfo == null) {
            throw new BusinessException("未找到框架代码为 " + clusterInfo.clusterFrame() + " 的框架信息");
        }

        List<FrameServiceEntity> entities = getMapper().selectByFrameIdOrderBySortNum(frameInfo.getId());
        List<FrameServiceDTO> dtos = frameServiceConverter.entityListToDtoList(entities);

        dtos = setInstalledStatus(clusterId, dtos);
        return setRequiredStatus(dtos, type);
    }

    /**
     * 设置服务的必需状态 - DTO级别操作
     */
    private List<FrameServiceDTO> setRequiredStatus(List<FrameServiceDTO> dtos, String type) {
        List<String> requiredServices = "custom".equals(type) ? CUSTOM_REQUIRED_SERVICE : DATALAKE_REQUIRED_SERVICE;

        return dtos.stream()
                .map(dto -> dto.withRequired(requiredServices.contains(dto.serviceName())))
                .toList();
    }

    /**
     * 设置服务的安装状态 - DTO级别操作
     */
    private List<FrameServiceDTO> setInstalledStatus(Integer clusterId, List<FrameServiceDTO> dtos) {
        return dtos.stream()
                .map(dto -> {
                    ClusterServiceInstanceDTO serviceInstance = serviceInstanceService
                            .getServiceInstanceByClusterIdAndServiceName(clusterId, dto.serviceName());
                    boolean installed = Objects.nonNull(serviceInstance)
                            && !serviceInstance.serviceState().equals(ServiceState.WAIT_INSTALL.getValue());
                    return dto.withInstalled(installed);
                })
                .toList();
    }

    @Override
    public List<FrameServiceDTO> getServiceListByServiceIds(List<Integer> serviceIds) {
        if (serviceIds == null || serviceIds.isEmpty()) {
            return List.of();
        }

        List<FrameServiceEntity> entities = getMapper().selectByIds(serviceIds);
        return frameServiceConverter.entityListToDtoList(entities);
    }

    @Override
    public FrameServiceDTO getServiceByFrameIdAndServiceName(Integer frameId, String serviceName) {
        if (frameId == null) {
            throw new BusinessException("框架ID不能为空");
        }
        if (StrUtil.isBlank(serviceName)) {
            throw new BusinessException("服务名称不能为空");
        }

        FrameServiceEntity entity = getMapper().selectByFrameIdAndServiceName(frameId, serviceName);
        if (entity == null) {
            throw new BusinessException("未找到框架ID为 " + frameId + " 且服务名称为 " + serviceName + " 的服务信息");
        }

        return frameServiceConverter.entityToDto(entity);
    }

    @Override
    public FrameServiceDTO getServiceByFrameCodeAndServiceName(String frameCode, String serviceName) {
        if (StrUtil.isBlank(frameCode)) {
            throw new BusinessException("框架代码不能为空");
        }
        if (StrUtil.isBlank(serviceName)) {
            throw new BusinessException("服务名称不能为空");
        }

        FrameServiceEntity entity = getMapper().selectByFrameCodeAndServiceName(frameCode, serviceName);
        if (entity == null) {
            throw new BusinessException("未找到框架代码为 " + frameCode + " 且服务名称为 " + serviceName + " 的服务信息");
        }

        return frameServiceConverter.entityToDto(entity);
    }

    @Override
    public List<FrameServiceDTO> getAllFrameServiceByFrameCode(String frameCode) {
        if (StrUtil.isBlank(frameCode)) {
            throw new BusinessException("框架代码不能为空");
        }

        List<FrameServiceEntity> entities = getMapper().selectByFrameCode(frameCode);
        return frameServiceConverter.entityListToDtoList(entities);
    }

    @Override
    public List<FrameServiceDTO> listServices(String serviceIds) {
        if (StrUtil.isBlank(serviceIds)) {
            return List.of();
        }

        List<String> ids = Arrays.stream(serviceIds.split(","))
                .filter(StrUtil::isNotBlank)
                .toList();

        if (ids.isEmpty()) {
            return List.of();
        }

        List<FrameServiceEntity> entities = getMapper().selectByStringIds(ids);
        return frameServiceConverter.entityListToDtoList(entities);
    }

    @Override
    public FrameServiceDTO getFrameServiceById(Integer id) {
        if (id == null) {
            throw new BusinessException("服务ID不能为空");
        }

        FrameServiceEntity entity = getById(id);
        if (entity == null) {
            throw new BusinessException("未找到ID为 " + id + " 的服务信息");
        }

        return frameServiceConverter.entityToDto(entity);
    }

    @Override
    public FrameServiceDTO saveFrameService(FrameServiceDTO frameServiceDTO) {
        if (frameServiceDTO == null) {
            throw new BusinessException("服务信息不能为空");
        }

        FrameServiceEntity entity = frameServiceConverter.dtoToEntity(frameServiceDTO);
        boolean result = save(entity);

        if (!result) {
            throw new BusinessException("保存服务信息失败");
        }

        return frameServiceConverter.entityToDto(entity);
    }

    @Override
    public FrameServiceDTO updateFrameService(FrameServiceDTO frameServiceDTO) {
        if (frameServiceDTO == null || frameServiceDTO.id() == null) {
            throw new BusinessException("服务信息或ID不能为空");
        }

        // 检查记录是否存在
        FrameServiceEntity existingEntity = getById(frameServiceDTO.id());
        if (existingEntity == null) {
            throw new BusinessException("未找到ID为 " + frameServiceDTO.id() + " 的服务信息");
        }

        FrameServiceEntity entity = frameServiceConverter.dtoToEntity(frameServiceDTO);
        boolean result = updateById(entity);

        if (!result) {
            throw new BusinessException("更新服务信息失败");
        }

        return frameServiceConverter.entityToDto(entity);
    }

    @Override
    public boolean removeFrameServiceByIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("删除的ID列表不能为空");
        }

        return removeByIds(ids);
    }

    @Override
    public boolean removeFrameServiceById(Integer id) {
        if (id == null) {
            throw new BusinessException("服务ID不能为空");
        }

        return removeById(id);
    }

}
