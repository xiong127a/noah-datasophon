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

import com.datasophon.api.service.host.strategy.impl.KubernetesHostStrategy;
import com.datasophon.api.service.host.strategy.impl.PvmHostStrategy;
import com.datasophon.common.enums.ClusterType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

/**
 * 主机管理策略工厂
 * 使用工厂模式根据集群类型创建相应的主机管理策略
 */
@Slf4j
@Component
public class HostManagementStrategyFactory {

    @Autowired
    private KubernetesHostStrategy kubernetesHostStrategy;

    @Autowired
    private PvmHostStrategy pvmHostStrategy;

    // 策略注册表
    private final Map<HostManagementStrategy.StrategyType, HostManagementStrategy> strategies = new HashMap<>();

    /**
     * 初始化策略注册表
     */
    @PostConstruct
    private void initStrategies() {
        strategies.put(HostManagementStrategy.StrategyType.KUBERNETES, kubernetesHostStrategy);
        strategies.put(HostManagementStrategy.StrategyType.PVM, pvmHostStrategy);
        
        log.info("主机管理策略工厂初始化完成，注册策略数量: {}", strategies.size());
        strategies.forEach((type, strategy) -> {
            log.debug("注册策略: {} -> {}", type.getCode(), strategy.getClass().getSimpleName());
        });
    }

    /**
     * 根据策略类型获取策略实例
     *
     * @param strategyType 策略类型
     * @return 策略实例
     * @throws IllegalArgumentException 如果策略类型不支持
     */
    public HostManagementStrategy getStrategy(HostManagementStrategy.StrategyType strategyType) {
        HostManagementStrategy strategy = strategies.get(strategyType);
        if (strategy == null) {
            throw new IllegalArgumentException("不支持的主机管理策略类型: " + strategyType);
        }
        
        log.debug("获取主机管理策略: {} -> {}", strategyType.getCode(), strategy.getClass().getSimpleName());
        return strategy;
    }

    /**
     * 根据字符串代码获取策略实例
     *
     * @param strategyCode 策略代码（如 "Kubernetes", "PVM"）
     * @return 策略实例
     * @throws IllegalArgumentException 如果策略代码不支持
     */
    public HostManagementStrategy getStrategy(String strategyCode) {
        if (strategyCode == null || strategyCode.trim().isEmpty()) {
            throw new IllegalArgumentException("策略代码不能为空");
        }
        
        try {
            HostManagementStrategy.StrategyType strategyType = 
                    HostManagementStrategy.StrategyType.fromCode(strategyCode);
            return getStrategy(strategyType);
        } catch (IllegalArgumentException e) {
            log.error("无效的策略代码: {}", strategyCode);
            throw e;
        }
    }

    /**
     * 根据集群部署类型获取策略实例
     * 这是最常用的方法，直接根据集群的depType字段获取策略
     *
     * @param depType 部署类型（从集群信息中获取）
     * @return 策略实例
     */
    public HostManagementStrategy getStrategyByDepType(String depType) {
        if (depType == null || depType.trim().isEmpty()) {
            // 默认使用PVM策略
            log.warn("部署类型为空，使用默认PVM策略");
            return getStrategy(HostManagementStrategy.StrategyType.PVM);
        }
        
        try {
            ClusterType clusterType = ClusterType.fromCode(depType);
            return getStrategyByClusterType(clusterType);
        } catch (IllegalArgumentException e) {
            log.warn("未知的部署类型: {}，使用默认PVM策略", depType);
            return getStrategy(HostManagementStrategy.StrategyType.PVM);
        }
    }
    
    /**
     * 根据集群类型枚举获取策略
     * @param clusterType 集群类型枚举
     * @return 策略实例
     */
    public HostManagementStrategy getStrategyByClusterType(ClusterType clusterType) {
        if (clusterType == null) {
            log.warn("集群类型为null，使用默认PVM策略");
            return getStrategy(HostManagementStrategy.StrategyType.PVM);
        }
        
        if (clusterType.isKubernetes()) {
            return getStrategy(HostManagementStrategy.StrategyType.KUBERNETES);
        } else if (clusterType.isPvm()) {
            return getStrategy(HostManagementStrategy.StrategyType.PVM);
        } else {
            log.warn("未知的集群类型: {}，使用默认PVM策略", clusterType);
            return getStrategy(HostManagementStrategy.StrategyType.PVM);
        }
    }

    /**
     * 检查是否支持指定的策略类型
     *
     * @param strategyType 策略类型
     * @return 是否支持
     */
    public boolean isStrategySupported(HostManagementStrategy.StrategyType strategyType) {
        return strategies.containsKey(strategyType);
    }

    /**
     * 检查是否支持指定的策略代码
     *
     * @param strategyCode 策略代码
     * @return 是否支持
     */
    public boolean isStrategySupported(String strategyCode) {
        try {
            HostManagementStrategy.StrategyType strategyType = 
                    HostManagementStrategy.StrategyType.fromCode(strategyCode);
            return isStrategySupported(strategyType);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 获取所有支持的策略类型
     *
     * @return 支持的策略类型列表
     */
    public Map<HostManagementStrategy.StrategyType, HostManagementStrategy> getAllStrategies() {
        return new HashMap<>(strategies);
    }



    /**
     * 根据集群ID和depType获取策略
     * 提供更完整的上下文信息用于日志和调试
     *
     * @param clusterId 集群ID
     * @param depType 部署类型
     * @return 策略实例
     */
    public HostManagementStrategy getStrategyWithContext(Long clusterId, String depType) {
        log.debug("为集群{}获取主机管理策略，部署类型: {}", clusterId, depType);
        
        HostManagementStrategy strategy = getStrategyByDepType(depType);
        
        log.info("集群{}使用主机管理策略: {} ({})", 
                clusterId, 
                strategy.getStrategyType().getCode(),
                strategy.getClass().getSimpleName());
        
        return strategy;
    }
}