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

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 配置步骤数据传输对象
 * 用于各个配置步骤的数据传输
 *
 * @author DataSophon Team
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StepDataDTO implements Serializable {
    
    @Serial
    private static final long serialVersionUID = 1L;
    
    /**
     * 集群ID
     */
    private Long clusterId;
    
    /**
     * 步骤号 (1-8)
     */
    private Integer stepNumber;
    
    /**
     * 步骤名称
     */
    private String stepName;
    
    /**
     * 步骤数据（JSON格式）
     */
    private String stepData;
    
    /**
     * 步骤状态：pending-未开始，processing-进行中，completed-已完成，failed-失败
     */
    private String stepStatus;
    
    /**
     * 操作人
     */
    private String operatedBy;
    
    /**
     * 操作时间
     */
    private LocalDateTime operatedTime;
    
    // ========== 各步骤具体数据结构 ==========
    
    /**
     * Step1 - 安装主机数据
     */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Step1Data implements Serializable {
        
        @Serial
        private static final long serialVersionUID = 1L;
        
        /**
         * 主机列表
         */
        private List<String> hostList;
        
        /**
         * SSH用户名
         */
        private String sshUser;
        
        /**
         * SSH端口
         */
        private Integer sshPort;
        
        /**
         * SSH密码
         */
        private String sshPassword;
        
        /**
         * 集群类型：Kubernetes, PVM
         */
        private String clusterType;
        
        /**
         * Kubernetes配置（如果是K8S集群）
         */
        private String kubeConfig;
        
        /**
         * Kubernetes命名空间
         */
        private String namespace;
    }
    
    /**
     * Step2 - 主机环境校验数据
     */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Step2Data implements Serializable {
        
        @Serial
        private static final long serialVersionUID = 1L;
        
        /**
         * 校验结果列表
         */
        private List<HostCheckResult> checkResults;
        
        /**
         * 校验通过的主机数量
         */
        private Integer passedHostCount;
        
        /**
         * 校验失败的主机数量
         */
        private Integer failedHostCount;
        
        /**
         * 主机检查结果
         */
        @Data
        public static class HostCheckResult implements Serializable {
            
            @Serial
            private static final long serialVersionUID = 1L;
            
            private String hostname;
            private String ip;
            private Boolean checkPassed;
            private String checkMessage;
            private Map<String, Object> systemInfo;
        }
    }
    
    /**
     * Step3 - 主机Agent分发数据
     */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Step3Data implements Serializable {
        
        @Serial
        private static final long serialVersionUID = 1L;
        
        /**
         * Agent分发状态列表
         */
        private List<AgentDeployStatus> agentStatuses;
        
        /**
         * 分发成功的主机数量
         */
        private Integer successHostCount;
        
        /**
         * 分发失败的主机数量
         */
        private Integer failedHostCount;
        
        /**
         * Agent部署状态
         */
        @Data
        public static class AgentDeployStatus implements Serializable {
            
            @Serial
            private static final long serialVersionUID = 1L;
            
            private String hostname;
            private String ip;
            private String deployStatus; // SUCCESS, FAILED, DEPLOYING
            private String deployMessage;
            private LocalDateTime deployTime;
        }
    }
    
    /**
     * Step4 - 选择服务数据
     */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Step4Data implements Serializable {
        
        @Serial
        private static final long serialVersionUID = 1L;
        
        /**
         * 选择的服务列表
         */
        private List<SelectedService> selectedServices;
        
        /**
         * 框架信息
         */
        private String frameId;
        private String frameName;
        private String frameVersion;
        
        /**
         * 选择的服务
         */
        @Data
        public static class SelectedService implements Serializable {
            
            @Serial
            private static final long serialVersionUID = 1L;
            
            private String serviceName;
            private String serviceVersion;
            private List<String> dependencies;
            private Map<String, Object> serviceConfig;
        }
    }
    
    /**
     * Step5 - 分配服务Master角色数据
     */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Step5Data implements Serializable {
        
        @Serial
        private static final long serialVersionUID = 1L;
        
        /**
         * Master角色分配
         */
        private List<RoleAssignment> masterAssignments;
        
        /**
         * 角色分配
         */
        @Data
        public static class RoleAssignment implements Serializable {
            
            @Serial
            private static final long serialVersionUID = 1L;
            
            private String serviceName;
            private String roleName;
            private List<String> assignedHosts;
        }
    }
    
    /**
     * Step6 - 分配服务Worker与Client角色数据
     */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Step6Data implements Serializable {
        
        @Serial
        private static final long serialVersionUID = 1L;
        
        /**
         * Worker和Client角色分配
         */
        private List<Step5Data.RoleAssignment> workerClientAssignments;
    }
    
    /**
     * Step7 - 服务配置数据
     */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Step7Data implements Serializable {
        
        @Serial
        private static final long serialVersionUID = 1L;
        
        /**
         * 服务配置列表
         */
        private List<ServiceConfiguration> serviceConfigurations;
        
        /**
         * 服务配置
         */
        @Data
        public static class ServiceConfiguration implements Serializable {
            
            @Serial
            private static final long serialVersionUID = 1L;
            
            private String serviceName;
            private Map<String, Object> configParameters;
            private List<String> configFiles;
        }
    }
    
    /**
     * Step8 - 安装并启动服务数据
     */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Step8Data implements Serializable {
        
        @Serial
        private static final long serialVersionUID = 1L;
        
        /**
         * 服务安装状态列表
         */
        private List<ServiceInstallStatus> installStatuses;
        
        /**
         * 安装成功的服务数量
         */
        private Integer successServiceCount;
        
        /**
         * 安装失败的服务数量
         */
        private Integer failedServiceCount;
        
        /**
         * 服务安装状态
         */
        @Data
        public static class ServiceInstallStatus implements Serializable {
            
            @Serial
            private static final long serialVersionUID = 1L;
            
            private String serviceName;
            private String installStatus; // SUCCESS, FAILED, INSTALLING
            private String installMessage;
            private LocalDateTime installStartTime;
            private LocalDateTime installEndTime;
            private List<String> installedHosts;
        }
    }
    
    // ========== 静态工厂方法 ==========
    
    /**
     * 创建步骤数据DTO
     *
     * @param clusterId 集群ID
     * @param stepNumber 步骤号
     * @param stepName 步骤名称
     * @param stepData 步骤数据JSON
     * @param operatedBy 操作人
     * @return StepDataDTO
     */
    public static StepDataDTO of(Long clusterId, Integer stepNumber, String stepName,
                                String stepData, String operatedBy) {
        StepDataDTO dto = new StepDataDTO();
        dto.setClusterId(clusterId);
        dto.setStepNumber(stepNumber);
        dto.setStepName(stepName);
        dto.setStepData(stepData);
        dto.setStepStatus("processing");
        dto.setOperatedBy(operatedBy);
        dto.setOperatedTime(LocalDateTime.now());
        return dto;
    }
    
    /**
     * 创建已完成的步骤数据
     *
     * @param clusterId 集群ID
     * @param stepNumber 步骤号
     * @param stepName 步骤名称
     * @param stepData 步骤数据JSON
     * @param operatedBy 操作人
     * @return StepDataDTO
     */
    public static StepDataDTO completed(Long clusterId, Integer stepNumber, String stepName,
                                       String stepData, String operatedBy) {
        StepDataDTO dto = of(clusterId, stepNumber, stepName, stepData, operatedBy);
        dto.setStepStatus("completed");
        return dto;
    }
}