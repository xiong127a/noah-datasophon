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

package com.datasophon.dao.entity;

import com.datasophon.common.enums.ConfigStatus;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 集群配置进度实体类
 * 用于记录和跟踪集群配置过程中的状态和数据
 *
 * @author DataSophon Team
 */
@Data
@Table("t_ddh_cluster_config_progress")
public class ClusterConfigProgressEntity implements Serializable {
    
    @Serial
    private static final long serialVersionUID = 1L;
    
    /**
     * 主键ID
     */
    @Id
    private Long id;
    
    /**
     * 集群ID，关联t_ddh_cluster_info表
     */
    private Integer clusterId;
    
    /**
     * 配置状态：UNCONFIGURED-未配置，CONFIGURING-配置中，COMPLETED-配置完成
     */
    private ConfigStatus configStatus;
    
    /**
     * 已完成步骤：0-未开始，1-安装主机，2-环境校验，3-Agent分发，4-选择服务，5-分配Master，6-分配Worker，7-服务配置，8-全部完成
     */
    private Integer completedStep;
    
    // ========== 各步骤配置数据 ==========
    
    /**
     * Step1数据：安装主机 - 主机列表、SSH配置等（JSON格式）
     */
    private String step1Data;
    
    /**
     * Step2数据：主机环境校验 - 校验结果、环境信息等（JSON格式）
     */
    private String step2Data;
    
    /**
     * Step3数据：主机Agent分发 - Agent状态、分发进度等（JSON格式）
     */
    private String step3Data;
    
    /**
     * Step4数据：选择服务 - 服务列表、框架信息等（JSON格式）
     */
    private String step4Data;
    
    /**
     * Step5数据：分配服务Master角色 - Master节点分配等（JSON格式）
     */
    private String step5Data;
    
    /**
     * Step6数据：分配服务Worker与Client角色 - Worker节点分配等（JSON格式）
     */
    private String step6Data;
    
    /**
     * Step7数据：服务配置 - 服务参数配置等（JSON格式）
     */
    private String step7Data;
    
    /**
     * Step8数据：安装并启动服务 - 安装进度、服务状态等（JSON格式）
     */
    private String step8Data;
    
    // ========== 配置过程元数据 ==========
    
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
    
    // ========== 审计字段 ==========
    
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
    
    // ========== 业务方法 ==========
    
    /**
     * 获取配置进度百分比
     *
     * @return 进度百分比 (0-100)
     */
    public int getProgressPercentage() {
        if (completedStep == null) {
            return 0;
        }
        return (completedStep * 100) / 8; // 总共8个步骤
    }
    
    /**
     * 检查指定步骤是否已完成
     *
     * @param stepNumber 步骤号 (1-8)
     * @return true如果步骤已完成
     */
    public boolean isStepCompleted(int stepNumber) {
        return completedStep != null && completedStep >= stepNumber;
    }
    
    /**
     * 标记指定步骤为完成
     *
     * @param stepNumber 步骤号 (1-8)
     */
    public void markStepCompleted(int stepNumber) {
        if (stepNumber < 1 || stepNumber > 8) {
            throw new IllegalArgumentException("Step number must be between 1 and 8");
        }
        
        // 只能按顺序完成步骤
        if (completedStep != null && stepNumber > completedStep + 1) {
            throw new IllegalStateException("Steps must be completed in order");
        }
        
        completedStep = stepNumber;
        lastStepTime = LocalDateTime.now();
        
        // 更新配置状态
        if (stepNumber == 8) {
            configStatus = ConfigStatus.COMPLETED;
            completedTime = LocalDateTime.now();
        } else if (stepNumber > 0) {
            configStatus = ConfigStatus.CONFIGURING;
            if (startedTime == null) {
                startedTime = LocalDateTime.now();
            }
        }
    }
    
    /**
     * 获取指定步骤的配置数据
     *
     * @param stepNumber 步骤号 (1-8)
     * @return 步骤配置数据的JSON字符串
     */
    public String getStepData(int stepNumber) {
        return switch (stepNumber) {
            case 1 -> step1Data;
            case 2 -> step2Data;
            case 3 -> step3Data;
            case 4 -> step4Data;
            case 5 -> step5Data;
            case 6 -> step6Data;
            case 7 -> step7Data;
            case 8 -> step8Data;
            default -> null;
        };
    }
    
    /**
     * 设置指定步骤的配置数据
     *
     * @param stepNumber 步骤号 (1-8)
     * @param data 配置数据的JSON字符串
     */
    public void setStepData(int stepNumber, String data) {
        switch (stepNumber) {
            case 1 -> step1Data = data;
            case 2 -> step2Data = data;
            case 3 -> step3Data = data;
            case 4 -> step4Data = data;
            case 5 -> step5Data = data;
            case 6 -> step6Data = data;
            case 7 -> step7Data = data;
            case 8 -> step8Data = data;
        }
    }
    
    /**
     * 获取下一个要执行的步骤
     *
     * @return 下一步骤号，如果已完成返回-1
     */
    public int getNextStep() {
        if (completedStep == null || completedStep == 0) {
            return 1;
        }
        if (completedStep >= 8) {
            return -1; // 已完成所有步骤
        }
        return completedStep + 1;
    }
    
    /**
     * 获取当前步骤的描述
     *
     * @return 当前步骤描述
     */
    public String getCurrentStepDescription() {
        int nextStep = getNextStep();
        return switch (nextStep) {
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
    }
    
    /**
     * 检查是否可以进入集群管理
     *
     * @return true如果配置已完成
     */
    public boolean canEnterCluster() {
        return configStatus != null && configStatus.canEnterCluster();
    }
    
    /**
     * 检查是否需要继续配置
     *
     * @return true如果需要继续配置
     */
    public boolean needsContinueConfig() {
        return configStatus != null && configStatus.needsContinueConfig();
    }
}