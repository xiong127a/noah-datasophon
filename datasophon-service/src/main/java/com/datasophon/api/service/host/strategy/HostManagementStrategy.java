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

package com.datasophon.api.service.host.strategy;

import com.datasophon.api.service.host.strategy.model.HostDiscoveryRequest;
import com.datasophon.api.service.host.strategy.model.HostDiscoveryResult;
import com.datasophon.api.service.host.strategy.model.HostListRequest;
import com.datasophon.api.service.host.strategy.model.HostListResult;
import com.datasophon.api.service.host.strategy.model.HostImportRequest;
import com.datasophon.dao.entity.ClusterHostEntity;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * 主机管理策略接口
 * 使用策略模式分离PVM和K8S的主机管理逻辑
 */
public interface HostManagementStrategy {

    /**
     * 获取策略类型
     * @return 策略类型标识
     */
    StrategyType getStrategyType();

    /**
     * 发现主机
     * 根据不同的策略（PVM/K8S）发现可用主机
     * 
     * @param request 主机发现请求
     * @return 发现的主机列表
     */
    HostDiscoveryResult discoverHosts(HostDiscoveryRequest request);

    /**
     * 获取主机列表
     * 用于Step2界面显示，支持分页和筛选
     * 
     * @param request 主机列表请求
     * @return 主机列表结果
     */
    HostListResult getHostList(HostListRequest request);

    /**
     * 导入主机到集群
     * 将用户选择的主机正式导入到集群中
     * 
     * @param request 主机导入请求
     */
    void importHosts(HostImportRequest request);

    /**
     * 刷新主机信息
     * 重新获取主机的最新状态和硬件信息
     * 
     * @param clusterId 集群ID
     * @param connectionParams 连接参数
     * @return 刷新后的主机列表
     */
    List<ClusterHostEntity> refreshHosts(Long clusterId, Map<String, Object> connectionParams);

    /**
     * 检查连接状态
     * 验证是否能正常连接到目标环境
     * 
     * @param connectionParams 连接参数
     * @return 连接状态信息
     */
    Map<String, Object> checkConnection(Map<String, Object> connectionParams);

    /**
     * 执行主机环境检查
     * 对主机进行环境校验，确保满足部署要求
     * 
     * @param clusterId 集群ID
     * @param hostnames 主机名列表
     * @param connectionParams 连接参数
     * @return 检查结果
     */
    Map<String, Object> performHostCheck(Long clusterId, List<String> hostnames, 
                                       Map<String, Object> connectionParams);

    /**
     * 获取主机检查状态
     * 查询主机环境检查的进度和结果
     * 
     * @param clusterId 集群ID
     * @return 检查状态
     */
    Map<String, Object> getHostCheckStatus(Long clusterId);

    /**
     * 清理资源
     * 清理策略相关的临时资源和缓存
     * 
     * @param clusterId 集群ID
     */
    void cleanup(Long clusterId);

    /**
     * 校验是否可以进入下一步（Step2 -> Step3等）
     * 不同策略可有不同校验规则；返回校验细节，并在通过时触发进度保存
     *
     * @param clusterId 集群ID
     * @return 校验结果Map，包含：valid(boolean), message(String), totalHosts, unmanagedHosts, readyHosts 等
     */
    Map<String, Object> validateForNextStep(Long clusterId);

    /**
     * 策略类型枚举
     */
    @Getter
    enum StrategyType {
        PVM("PVM", "传统虚拟机模式"),
        KUBERNETES("Kubernetes", "Kubernetes容器模式");

        private final String code;
        private final String description;

        StrategyType(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public static StrategyType fromCode(String code) {
            for (StrategyType type : values()) {
                if (type.code.equalsIgnoreCase(code)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("未知的策略类型: " + code);
        }
    }
}