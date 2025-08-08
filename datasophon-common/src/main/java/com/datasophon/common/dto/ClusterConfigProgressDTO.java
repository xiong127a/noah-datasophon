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

package com.datasophon.common.dto;

import com.datasophon.common.enums.ConfigStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 集群配置进度数据传输对象
 * 用于Service层数据传输
 *
 * @author DataSophon Team
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClusterConfigProgressDTO implements Serializable {
    
    @Serial
    private static final long serialVersionUID = 1L;
    
    /**
     * 主键ID
     */
    private Long id;
    
    /**
     * 集群ID
     */
    private Integer clusterId;
    
    /**
     * 配置状态
     */
    private ConfigStatus configStatus;
    
    /**
     * 已完成步骤：0-未开始，1-安装主机，2-环境校验，3-Agent分发，4-选择服务，5-分配Master，6-分配Worker，7-服务配置，8-全部完成
     */
    private Integer completedStep;
    
    /**
     * 步骤数据映射（key: stepNumber, value: stepData JSON）
     */
    private Map<Integer, String> stepDataMap;
    
    /**
     * 配置开始时间
     */
    private LocalDateTime startedTime;
    
    /**
     * 配置完成时间
     */
    private LocalDateTime completedTime;
    
    /**
     * 最后步骤操作时间
     */
    private LocalDateTime lastStepTime;
    
    /**
     * 创建人
     */
    private String createdBy;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdTime;
    
    /**
     * 最后更新人
     */
    private String updatedBy;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedTime;
    
    // ========== 业务计算字段 ==========
    
    /**
     * 配置进度百分比
     */
    private Integer progressPercentage;
    
    /**
     * 下一个要执行的步骤
     */
    private Integer nextStep;
    
    /**
     * 当前步骤描述
     */
    private String currentStepDescription;
    
    /**
     * 是否可以进入集群管理
     */
    private Boolean canEnterCluster;
    
    /**
     * 是否需要继续配置
     */
    private Boolean needsContinueConfig;
    
    // ========== 静态工厂方法 ==========
    
    /**
     * 创建新的配置进度
     *
     * @param clusterId 集群ID
     * @param createdBy 创建人
     * @return ClusterConfigProgressDTO
     */
    public static ClusterConfigProgressDTO create(Integer clusterId, String createdBy) {
        ClusterConfigProgressDTO dto = new ClusterConfigProgressDTO();
        dto.setClusterId(clusterId);
        dto.setConfigStatus(ConfigStatus.UNCONFIGURED);
        dto.setCompletedStep(0);
        dto.setCreatedBy(createdBy);
        dto.setCreatedTime(LocalDateTime.now());
        return dto;
    }
    
    /**
     * 创建配置中状态的进度
     *
     * @param clusterId 集群ID
     * @param completedStep 已完成步骤
     * @param createdBy 创建人
     * @return ClusterConfigProgressDTO
     */
    public static ClusterConfigProgressDTO configuring(Integer clusterId, Integer completedStep, String createdBy) {
        ClusterConfigProgressDTO dto = create(clusterId, createdBy);
        dto.setConfigStatus(ConfigStatus.CONFIGURING);
        dto.setCompletedStep(completedStep);
        dto.setStartedTime(LocalDateTime.now());
        return dto;
    }
    
    /**
     * 创建配置完成状态的进度
     *
     * @param clusterId 集群ID
     * @param createdBy 创建人
     * @return ClusterConfigProgressDTO
     */
    public static ClusterConfigProgressDTO completed(Integer clusterId, String createdBy) {
        ClusterConfigProgressDTO dto = create(clusterId, createdBy);
        dto.setConfigStatus(ConfigStatus.COMPLETED);
        dto.setCompletedStep(8);
        dto.setStartedTime(LocalDateTime.now());
        dto.setCompletedTime(LocalDateTime.now());
        return dto;
    }
    
    // ========== 业务方法 ==========
    
    /**
     * 获取指定步骤的配置数据
     *
     * @param stepNumber 步骤号
     * @return 步骤配置数据的JSON字符串
     */
    public String getStepData(int stepNumber) {
        return stepDataMap != null ? stepDataMap.get(stepNumber) : null;
    }
    
    /**
     * 设置指定步骤的配置数据
     *
     * @param stepNumber 步骤号
     * @param data 配置数据的JSON字符串
     */
    public void setStepData(int stepNumber, String data) {
        if (stepDataMap == null) {
            stepDataMap = java.util.HashMap.newHashMap(8);
        }
        stepDataMap.put(stepNumber, data);
    }
    
    /**
     * 计算并更新业务字段
     */
    public void calculateBusinessFields() {
        // 计算进度百分比
        this.progressPercentage = completedStep != null ? (completedStep * 100) / 8 : 0;
        
        // 计算下一步骤
        if (completedStep == null || completedStep == 0) {
            this.nextStep = 1;
        } else if (completedStep >= 8) {
            this.nextStep = -1; // 已完成
        } else {
            this.nextStep = completedStep + 1;
        }
        
        // 设置当前步骤描述
        this.currentStepDescription = switch (this.nextStep) {
            case 1 -> "安装主机";
            case 2 -> "主机环境校验";
            case 3 -> "主机Agent分发";
            case 4 -> "选择服务";
            case 5 -> "分配服务Master角色";
            case 6 -> "分配服务Worker与Client角色";
            case 7 -> "服务配置";
            case 8 -> "安装并启动服务";
            case -1 -> "配置完成";
            default -> "未知步骤";
        };
        
        // 计算业务状态
        this.canEnterCluster = configStatus != null && configStatus.canEnterCluster();
        this.needsContinueConfig = configStatus != null && configStatus.needsContinueConfig();
    }
}