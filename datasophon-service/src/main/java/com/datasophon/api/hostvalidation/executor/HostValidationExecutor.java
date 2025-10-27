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

package com.datasophon.api.hostvalidation.executor;

import com.datasophon.api.hostvalidation.manager.HostValidationStateManager;
import com.datasophon.common.dto.HostValidationRequestDTO;
import com.datasophon.common.enums.CheckType;
import com.datasophon.common.enums.ValidationStatus;
import com.datasophon.plugins.api.HostValidator;
import com.datasophon.plugins.api.HostRepairer;
import com.datasophon.plugins.api.model.CheckResult;
import com.datasophon.plugins.api.model.HostCheckContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 主机校验执行器 - 基于官方pf4j-spring标准
 * 专门负责校验和修复的具体执行逻辑
 * <p>
 * 职责：
 * 1. 插件发现和调用（通过Spring自动注入）
 * 2. 校验结果处理
 * 3. 状态更新
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-28
 */
@Slf4j
@Component
@DependsOn("extensionsInjector")
public class HostValidationExecutor {
    
    private final HostValidationStateManager stateManager;
    
    // 官方pf4j-spring方式：直接通过Spring自动注入所有插件扩展
    @Autowired
    private List<HostValidator> hostValidators;
    
    @Autowired
    private List<HostRepairer> hostRepairers;
    
    public HostValidationExecutor(HostValidationStateManager stateManager) {
        this.stateManager = stateManager;
    }
    
    /**
     * 执行主机校验
     */
    public void executeValidation(HostValidationRequestDTO request) {
        Long clusterId = request.clusterId();
        
        try {
            log.info("开始执行主机校验: clusterId={}, 主机数量={}", clusterId, request.hostIps().size());
            
            // 1. 获取校验插件（官方pf4j-spring自动注入）
            if (hostValidators.isEmpty()) {
                log.warn("未找到校验插件: clusterId={}", clusterId);
                stateManager.completeValidationSession(clusterId);
                return;
            }
            
            // 2. 并发校验所有主机
            request.hostIps().parallelStream().forEach(hostIp -> 
                validateSingleHost(request, hostIp, hostValidators)
            );
            
            log.info("主机校验执行完成: clusterId={}", clusterId);
            stateManager.completeValidationSession(clusterId);
            
        } catch (Exception e) {
            log.error("主机校验执行异常: clusterId={}, error={}", clusterId, e.getMessage(), e);
            stateManager.completeValidationSession(clusterId);
        }
    }
    
    /**
     * 执行主机修复
     */
    public void executeRepair(Long clusterId, String hostIp, CheckType checkType) {
        try {
            log.info("开始执行主机修复: clusterId={}, hostIp={}, checkType={}", clusterId, hostIp, checkType);
            
            // 获取修复插件（官方pf4j-spring自动注入）
            if (hostRepairers.isEmpty()) {
                log.warn("未找到修复插件: clusterId={}, hostIp={}, checkType={}", clusterId, hostIp, checkType);
                stateManager.updateCheckItemStatus(clusterId, hostIp, checkType, 
                    ValidationStatus.FAILED, "未找到可用的修复插件", Map.of());
                return;
            }

            // 寻找支持该修复类型的插件
            HostRepairer targetPlugin = hostRepairers.stream()
                .filter(plugin -> plugin.getSupportedRepairTypes().contains(checkType))
                .findFirst()
                .orElse(null);
                
            if (targetPlugin == null) {
                log.warn("未找到支持修复类型{}的插件: clusterId={}, hostIp={}", checkType, clusterId, hostIp);
                stateManager.updateCheckItemStatus(clusterId, hostIp, checkType, 
                    ValidationStatus.FAILED, "未找到支持该修复类型的插件", Map.of());
                return;
            }

            // 构建修复上下文
            HostCheckContext context = HostCheckContext.builder()
                .hostIp(hostIp)
                // 注意：这里需要从状态管理器或其他地方获取SSH连接信息
                .build();

            // 执行修复
            stateManager.updateCheckItemStatus(clusterId, hostIp, checkType, 
                ValidationStatus.REPAIRING, "正在执行修复...", Map.of());
                
            CheckResult repairResult = targetPlugin.executeRepair(context, checkType, Map.of()).get();
            
            // 更新修复结果
            stateManager.updateCheckItemStatus(clusterId, hostIp, checkType, 
                repairResult.isSuccess() ? ValidationStatus.SUCCESS : ValidationStatus.FAILED,
                repairResult.getMessage(),
                repairResult.getData()
            );
            
            log.info("主机修复执行完成: clusterId={}, hostIp={}, checkType={}, result={}", 
                clusterId, hostIp, checkType, repairResult.getStatus());
                
        } catch (Exception e) {
            log.error("主机修复执行异常: clusterId={}, hostIp={}, checkType={}, error={}", 
                clusterId, hostIp, checkType, e.getMessage(), e);
        }
    }
    
    /**
     * 校验单个主机 - 插件化处理
     */
    private void validateSingleHost(HostValidationRequestDTO request, String hostIp, List<HostValidator> plugins) {
        Long clusterId = request.clusterId();
        
        try {
            log.debug("开始校验主机: clusterId={}, hostIp={}", clusterId, hostIp);
            
            HostCheckContext context = HostCheckContext.builder()
                .clusterId(clusterId)
                .hostIp(hostIp)
                .sshPort(request.sshPort())
                .sshUser(request.sshUser())
                .sshPassword(request.sshPassword())
                .privateKeyPath(request.privateKeyPath())
                .build();
            
            // 按优先级顺序执行插件
            for (HostValidator plugin : plugins) {
                // 遍历插件支持的检查类型
                for (CheckType checkType : plugin.getSupportedCheckTypes()) {
                    try {
                        if (!plugin.canExecute(context, checkType)) {
                            log.debug("插件检查项不适用: plugin={}, hostIp={}, checkType={}", 
                                    plugin.getClass().getSimpleName(), hostIp, checkType);
                            continue;
                        }
                    
                        CheckResult result = plugin.executeCheck(context, checkType).get(); // 同步等待结果
                    
                    // 更新检查结果
                    stateManager.updateCheckItemStatus(clusterId, hostIp, result.getCheckType(), 
                        result.isSuccess() ? ValidationStatus.SUCCESS : ValidationStatus.FAILED,
                        result.getMessage(), result.getData());
                    
                    log.debug("插件检查完成: plugin={}, hostIp={}, checkType={}, success={}", 
                        plugin.getClass().getSimpleName(), hostIp, result.getCheckType(), result.isSuccess());
                    
                    } catch (Exception e) {
                        log.error("插件检查异常: plugin={}, hostIp={}, checkType={}, error={}", 
                                plugin.getClass().getSimpleName(), hostIp, checkType, e.getMessage(), e);
                        // 检查失败时更新状态
                        stateManager.updateCheckItemStatus(clusterId, hostIp, checkType, 
                            ValidationStatus.FAILED, "检查异常: " + e.getMessage(), Map.of());
                    }
                }
            }
            
            log.debug("主机校验完成: clusterId={}, hostIp={}", clusterId, hostIp);
            
        } catch (Exception e) {
            log.error("主机校验异常: clusterId={}, hostIp={}, error={}", clusterId, hostIp, e.getMessage(), e);
        }
    }
    
    /**
     * 重新检查指定项
     */
    public void recheckItem(Long clusterId, String hostIp, CheckType checkType) {
        try {
            log.info("重新检查项: clusterId={}, hostIp={}, checkType={}", clusterId, hostIp, checkType);
            
            // 获取校验插件（官方pf4j-spring自动注入）
            HostValidator targetPlugin = hostValidators.stream()
                .filter(plugin -> plugin.getSupportedCheckTypes().contains(checkType))
                .findFirst()
                .orElse(null);
                
            if (targetPlugin == null) {
                log.warn("未找到支持检查类型{}的插件: clusterId={}, hostIp={}", checkType, clusterId, hostIp);
                stateManager.updateCheckItemStatus(clusterId, hostIp, checkType, 
                    ValidationStatus.FAILED, "未找到支持该检查类型的插件", Map.of());
                return;
            }
            
            // 构建上下文 - 需要从状态管理器获取SSH连接信息
            HostCheckContext context = HostCheckContext.builder()
                .clusterId(clusterId)
                .hostIp(hostIp)
                .build();
            
            // 更新为检查中状态
            stateManager.updateCheckItemStatus(clusterId, hostIp, checkType, 
                ValidationStatus.CHECKING, "重新检查中...", Map.of());
                
            CheckResult result = targetPlugin.executeCheck(context, checkType).get(); // 同步等待结果
            stateManager.updateCheckItemStatus(clusterId, hostIp, checkType, 
                result.isSuccess() ? ValidationStatus.SUCCESS : ValidationStatus.FAILED,
                result.getMessage(), result.getData());
            
            log.info("重新检查完成: clusterId={}, hostIp={}, checkType={}, success={}", 
                clusterId, hostIp, checkType, result.isSuccess());
                
        } catch (Exception e) {
            log.error("重新检查异常: clusterId={}, hostIp={}, checkType={}, error={}", 
                clusterId, hostIp, checkType, e.getMessage(), e);
        }
    }
    

}
