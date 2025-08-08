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

package com.datasophon.common.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 集群配置进度视图对象
 * 用于前端展示
 *
 * @author DataSophon Team
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClusterConfigProgressVO implements Serializable {
    
    @Serial
    private static final long serialVersionUID = 1L;
    
    /**
     * 集群ID
     */
    private Integer clusterId;
    
    /**
     * 集群名称
     */
    private String clusterName;
    
    /**
     * 配置状态代码
     */
    private String configStatusCode;
    
    /**
     * 配置状态描述
     */
    private String configStatusDesc;
    
    /**
     * 已完成步骤
     */
    private Integer completedStep;
    
    /**
     * 下一个要执行的步骤
     */
    private Integer nextStep;
    
    /**
     * 当前步骤描述
     */
    private String currentStepDescription;
    
    /**
     * 配置进度百分比
     */
    private Integer progressPercentage;
    
    /**
     * 步骤列表
     */
    private List<StepInfo> steps;
    
    /**
     * 是否可以进入集群管理
     */
    private Boolean canEnterCluster;
    
    /**
     * 是否需要继续配置
     */
    private Boolean needsContinueConfig;
    
    /**
     * 配置开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startedTime;
    
    /**
     * 配置完成时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime completedTime;
    
    /**
     * 最后步骤操作时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastStepTime;
    
    /**
     * 配置耗时（分钟）
     */
    private Long configurationDurationMinutes;
    
    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;
    
    /**
     * 步骤信息
     */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class StepInfo implements Serializable {
        
        @Serial
        private static final long serialVersionUID = 1L;
        
        /**
         * 步骤号
         */
        private Integer stepNumber;
        
        /**
         * 步骤名称
         */
        private String stepName;
        
        /**
         * 步骤描述
         */
        private String stepDescription;
        
        /**
         * 步骤状态：pending-未开始，processing-进行中，completed-已完成，failed-失败
         */
        private String stepStatus;
        
        /**
         * 是否为当前步骤
         */
        private Boolean isCurrent;
        
        /**
         * 是否已完成
         */
        private Boolean isCompleted;
        
        /**
         * 是否可以访问
         */
        private Boolean canAccess;
        
        /**
         * 步骤图标
         */
        private String stepIcon;
        
        /**
         * 创建步骤信息
         *
         * @param stepNumber 步骤号
         * @param stepName 步骤名称
         * @param stepDescription 步骤描述
         * @return StepInfo
         */
        public static StepInfo of(Integer stepNumber, String stepName, String stepDescription) {
            StepInfo stepInfo = new StepInfo();
            stepInfo.setStepNumber(stepNumber);
            stepInfo.setStepName(stepName);
            stepInfo.setStepDescription(stepDescription);
            return stepInfo;
        }
    }
    
    /**
     * 创建默认的8步配置进度VO
     *
     * @param clusterId 集群ID
     * @param clusterName 集群名称
     * @return ClusterConfigProgressVO
     */
    public static ClusterConfigProgressVO createDefault(Integer clusterId, String clusterName) {
        ClusterConfigProgressVO vo = new ClusterConfigProgressVO();
        vo.setClusterId(clusterId);
        vo.setClusterName(clusterName);
        vo.setConfigStatusCode("UNCONFIGURED");
        vo.setConfigStatusDesc("未配置");
        vo.setCompletedStep(0);
        vo.setNextStep(1);
        vo.setCurrentStepDescription("安装主机");
        vo.setProgressPercentage(0);
        vo.setCanEnterCluster(false);
        vo.setNeedsContinueConfig(true);
        
        // 创建步骤列表
        vo.setSteps(List.of(
            StepInfo.of(1, "安装主机", "配置主机列表和SSH连接"),
            StepInfo.of(2, "主机环境校验", "检查主机环境和依赖"),
            StepInfo.of(3, "主机Agent分发", "分发和启动主机Agent"),
            StepInfo.of(4, "选择服务", "选择要安装的大数据服务"),
            StepInfo.of(5, "分配服务Master角色", "配置服务的Master节点"),
            StepInfo.of(6, "分配服务Worker与Client角色", "配置服务的Worker和Client节点"),
            StepInfo.of(7, "服务配置", "配置服务参数和依赖关系"),
            StepInfo.of(8, "安装并启动服务", "执行服务安装和启动")
        ));
        
        return vo;
    }
    
    /**
     * 更新步骤状态
     *
     * @param completedStep 已完成步骤
     */
    public void updateStepStatus(Integer completedStep) {
        if (steps == null || completedStep == null) {
            return;
        }
        
        for (StepInfo step : steps) {
            int stepNum = step.getStepNumber();
            
            if (stepNum <= completedStep) {
                // 已完成的步骤
                step.setStepStatus("completed");
                step.setIsCompleted(true);
                step.setIsCurrent(false);
                step.setCanAccess(true);
                step.setStepIcon("check-circle");
            } else if (stepNum == completedStep + 1) {
                // 当前要执行的步骤
                step.setStepStatus("processing");
                step.setIsCompleted(false);
                step.setIsCurrent(true);
                step.setCanAccess(true);
                step.setStepIcon("play-circle");
            } else {
                // 未开始的步骤
                step.setStepStatus("pending");
                step.setIsCompleted(false);
                step.setIsCurrent(false);
                step.setCanAccess(false);
                step.setStepIcon("clock-circle");
            }
        }
    }
    
    /**
     * 计算配置耗时
     */
    public void calculateDuration() {
        if (startedTime != null && completedTime != null) {
            this.configurationDurationMinutes = java.time.Duration.between(startedTime, completedTime).toMinutes();
        } else if (startedTime != null && lastStepTime != null) {
            this.configurationDurationMinutes = java.time.Duration.between(startedTime, lastStepTime).toMinutes();
        }
    }
}