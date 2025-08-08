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

import com.datasophon.common.dto.ClusterConfigProgressDTO;
import com.datasophon.common.vo.ClusterConfigProgressVO;
import com.datasophon.dao.entity.ClusterConfigProgressEntity;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 集群配置进度对象转换器
 * 负责Entity、DTO、VO之间的转换
 *
 * @author DataSophon Team
 */
@Component
public class ClusterConfigProgressConverter {
    
    /**
     * Entity转DTO
     *
     * @param entity ClusterConfigProgressEntity
     * @return ClusterConfigProgressDTO
     */
    public ClusterConfigProgressDTO toDTO(ClusterConfigProgressEntity entity) {
        if (entity == null) {
            return null;
        }
        
        ClusterConfigProgressDTO dto = new ClusterConfigProgressDTO();
        dto.setId(entity.getId());
        dto.setClusterId(entity.getClusterId());
        dto.setConfigStatus(entity.getConfigStatus());
        dto.setCompletedStep(entity.getCompletedStep());
        dto.setStartedTime(entity.getStartedTime());
        dto.setCompletedTime(entity.getCompletedTime());
        dto.setLastStepTime(entity.getLastStepTime());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedTime(entity.getCreatedTime());
        dto.setUpdatedBy(entity.getUpdatedBy());
        dto.setUpdatedTime(entity.getUpdatedTime());
        
        // 转换步骤数据映射
        Map<Integer, String> stepDataMap = new HashMap<>();
        for (int i = 1; i <= 8; i++) {
            String stepData = entity.getStepData(i);
            if (stepData != null && !stepData.trim().isEmpty()) {
                stepDataMap.put(i, stepData);
            }
        }
        dto.setStepDataMap(stepDataMap);
        
        // 计算业务字段
        dto.calculateBusinessFields();
        
        return dto;
    }
    
    /**
     * DTO转Entity
     *
     * @param dto ClusterConfigProgressDTO
     * @return ClusterConfigProgressEntity
     */
    public ClusterConfigProgressEntity toEntity(ClusterConfigProgressDTO dto) {
        if (dto == null) {
            return null;
        }
        
        ClusterConfigProgressEntity entity = new ClusterConfigProgressEntity();
        entity.setId(dto.getId());
        entity.setClusterId(dto.getClusterId());
        entity.setConfigStatus(dto.getConfigStatus());
        entity.setCompletedStep(dto.getCompletedStep());
        entity.setStartedTime(dto.getStartedTime());
        entity.setCompletedTime(dto.getCompletedTime());
        entity.setLastStepTime(dto.getLastStepTime());
        entity.setCreatedBy(dto.getCreatedBy());
        entity.setCreatedTime(dto.getCreatedTime());
        entity.setUpdatedBy(dto.getUpdatedBy());
        entity.setUpdatedTime(dto.getUpdatedTime());
        
        // 转换步骤数据
        if (dto.getStepDataMap() != null) {
            for (Map.Entry<Integer, String> entry : dto.getStepDataMap().entrySet()) {
                entity.setStepData(entry.getKey(), entry.getValue());
            }
        }
        
        return entity;
    }
    
    /**
     * DTO转VO
     *
     * @param dto ClusterConfigProgressDTO
     * @return ClusterConfigProgressVO
     */
    public ClusterConfigProgressVO toVO(ClusterConfigProgressDTO dto) {
        return toVO(dto, null);
    }
    
    /**
     * DTO转VO（带集群名称）
     *
     * @param dto ClusterConfigProgressDTO
     * @param clusterName 集群名称
     * @return ClusterConfigProgressVO
     */
    public ClusterConfigProgressVO toVO(ClusterConfigProgressDTO dto, String clusterName) {
        if (dto == null) {
            return null;
        }
        
        ClusterConfigProgressVO vo = new ClusterConfigProgressVO();
        vo.setClusterId(dto.getClusterId());
        vo.setClusterName(clusterName);
        
        // 配置状态
        if (dto.getConfigStatus() != null) {
            vo.setConfigStatusCode(dto.getConfigStatus().getCode());
            vo.setConfigStatusDesc(dto.getConfigStatus().getDesc());
        }
        
        vo.setCompletedStep(dto.getCompletedStep());
        vo.setNextStep(dto.getNextStep());
        vo.setCurrentStepDescription(dto.getCurrentStepDescription());
        vo.setProgressPercentage(dto.getProgressPercentage());
        vo.setCanEnterCluster(dto.getCanEnterCluster());
        vo.setNeedsContinueConfig(dto.getNeedsContinueConfig());
        
        // 时间字段
        vo.setStartedTime(dto.getStartedTime());
        vo.setCompletedTime(dto.getCompletedTime());
        vo.setLastStepTime(dto.getLastStepTime());
        vo.setCreatedTime(dto.getCreatedTime());
        
        // 创建步骤列表
        vo.setSteps(List.of(
            ClusterConfigProgressVO.StepInfo.of(1, "安装主机", "配置主机列表和SSH连接"),
            ClusterConfigProgressVO.StepInfo.of(2, "主机环境校验", "检查主机环境和依赖"),
            ClusterConfigProgressVO.StepInfo.of(3, "主机Agent分发", "分发和启动主机Agent"),
            ClusterConfigProgressVO.StepInfo.of(4, "选择服务", "选择要安装的大数据服务"),
            ClusterConfigProgressVO.StepInfo.of(5, "分配服务Master角色", "配置服务的Master节点"),
            ClusterConfigProgressVO.StepInfo.of(6, "分配服务Worker与Client角色", "配置服务的Worker和Client节点"),
            ClusterConfigProgressVO.StepInfo.of(7, "服务配置", "配置服务参数和依赖关系"),
            ClusterConfigProgressVO.StepInfo.of(8, "安装并启动服务", "执行服务安装和启动")
        ));
        
        // 更新步骤状态
        vo.updateStepStatus(dto.getCompletedStep());
        
        // 计算配置耗时
        vo.calculateDuration();
        
        return vo;
    }
    
    /**
     * Entity列表转DTO列表
     *
     * @param entities Entity列表
     * @return DTO列表
     */
    public List<ClusterConfigProgressDTO> toDTOList(List<ClusterConfigProgressEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return List.of();
        }
        
        return entities.stream()
                .map(this::toDTO)
                .toList();
    }
    
    /**
     * DTO列表转VO列表
     *
     * @param dtos DTO列表
     * @return VO列表
     */
    public List<ClusterConfigProgressVO> toVOList(List<ClusterConfigProgressDTO> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return List.of();
        }
        
        return dtos.stream()
                .map(this::toVO)
                .toList();
    }
    
    /**
     * DTO列表转VO列表（带集群名称映射）
     *
     * @param dtos DTO列表
     * @param clusterNameMap 集群名称映射 (clusterId -> clusterName)
     * @return VO列表
     */
    public List<ClusterConfigProgressVO> toVOList(List<ClusterConfigProgressDTO> dtos, 
                                                 Map<Integer, String> clusterNameMap) {
        if (dtos == null || dtos.isEmpty()) {
            return List.of();
        }
        
        return dtos.stream()
                .map(dto -> {
                    String clusterName = clusterNameMap != null ? 
                            clusterNameMap.get(dto.getClusterId()) : null;
                    return toVO(dto, clusterName);
                })
                .toList();
    }
    
    /**
     * 更新Entity的步骤数据
     *
     * @param entity 要更新的Entity
     * @param stepNumber 步骤号
     * @param stepData 步骤数据JSON
     * @param updatedBy 更新人
     */
    public void updateStepData(ClusterConfigProgressEntity entity, Integer stepNumber, 
                              String stepData, String updatedBy) {
        if (entity == null || stepNumber == null) {
            return;
        }
        
        entity.setStepData(stepNumber, stepData);
        entity.setUpdatedBy(updatedBy);
        entity.setUpdatedTime(java.time.LocalDateTime.now());
        entity.setLastStepTime(java.time.LocalDateTime.now());
    }
    
    /**
     * 标记Entity步骤完成
     *
     * @param entity 要更新的Entity
     * @param stepNumber 完成的步骤号
     * @param updatedBy 更新人
     */
    public void markStepCompleted(ClusterConfigProgressEntity entity, Integer stepNumber, 
                                 String updatedBy) {
        if (entity == null || stepNumber == null) {
            return;
        }
        
        entity.markStepCompleted(stepNumber);
        entity.setUpdatedBy(updatedBy);
        entity.setUpdatedTime(java.time.LocalDateTime.now());
    }
}