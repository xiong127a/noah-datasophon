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
import com.datasophon.plugins.api.HostCheckerPlugin;
import com.datasophon.plugins.api.model.CheckResult;
import com.datasophon.plugins.api.model.HostCheckContext;
import com.datasophon.plugins.manager.PluginManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 主机校验执行器
 * 专门负责校验和修复的具体执行逻辑
 * 
 * 职责：
 * 1. 插件发现和调用
 * 2. 校验结果处理
 * 3. 状态更新
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-29
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HostValidationExecutor {
    
    private final HostValidationStateManager stateManager;
    private final PluginManager pluginManager;
    
    /**
     * 执行主机校验
     */
    public void executeValidation(HostValidationRequestDTO request) {
        Long clusterId = request.clusterId();
        
        try {
            log.info("开始执行主机校验: clusterId={}, 主机数量={}", clusterId, request.hostIps().size());
            
            // 1. 获取校验插件
            List<HostCheckerPlugin> plugins = getAvailableValidationPlugins();
            if (plugins.isEmpty()) {
                log.warn("未找到校验插件: clusterId={}", clusterId);
                stateManager.completeValidationSession(clusterId);
                return;
            }
            
            // 2. 并发校验所有主机
            request.hostIps().parallelStream().forEach(hostIp -> 
                validateSingleHost(request, hostIp, plugins)
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
            
            // 暂时更新状态为修复中，具体修复逻辑待实现
            stateManager.updateCheckItemStatus(clusterId, hostIp, checkType, 
                ValidationStatus.REPAIRING, "修复功能正在开发中...", Map.of());
            
            log.info("主机修复功能正在开发中: clusterId={}, hostIp={}, checkType={}", 
                clusterId, hostIp, checkType);
                
        } catch (Exception e) {
            log.error("主机修复执行异常: clusterId={}, hostIp={}, checkType={}, error={}", 
                clusterId, hostIp, checkType, e.getMessage(), e);
        }
    }
    
    /**
     * 校验单个主机 - 插件化处理
     */
    private void validateSingleHost(HostValidationRequestDTO request, String hostIp, List<HostCheckerPlugin> plugins) {
        Long clusterId = request.clusterId();
        
        try {
            log.debug("开始校验主机: clusterId={}, hostIp={}", clusterId, hostIp);
            
            HostCheckContext context = HostCheckContext.builder()
                .clusterId(clusterId.toString())
                .hostIp(hostIp)
                .sshPort(request.sshPort())
                .sshUser(request.sshUser())
                .sshPassword(request.sshPassword())
                .privateKeyPath(request.privateKeyPath())
                .build();
            
            // 按优先级顺序执行插件
            for (HostCheckerPlugin plugin : plugins) {
                try {
                    if (!plugin.canExecute(context)) {
                        log.debug("插件不适用: plugin={}, hostIp={}", plugin.getClass().getSimpleName(), hostIp);
                        continue;
                    }
                    
                    CheckResult result = plugin.executeCheck(context).get(); // 同步等待结果
                    
                    // 更新检查结果
                    stateManager.updateCheckItemStatus(clusterId, hostIp, result.getCheckType(), 
                        result.isSuccess() ? ValidationStatus.SUCCESS : ValidationStatus.FAILED,
                        result.getMessage(), result.getData());
                    
                    log.debug("插件检查完成: plugin={}, hostIp={}, checkType={}, success={}", 
                        plugin.getClass().getSimpleName(), hostIp, result.getCheckType(), result.isSuccess());
                    
                } catch (Exception e) {
                    log.error("插件执行异常: plugin={}, hostIp={}, error={}", 
                        plugin.getClass().getSimpleName(), hostIp, e.getMessage(), e);
                    
                    // 记录插件执行失败 - 使用通用错误类型
                    stateManager.updateCheckItemStatus(clusterId, hostIp, CheckType.SYSTEM_INFO, 
                        ValidationStatus.FAILED, "插件执行异常: " + e.getMessage(), Map.of());
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
            
            List<HostCheckerPlugin> plugins = getAvailableValidationPlugins();
            HostCheckerPlugin targetPlugin = plugins.stream()
                .findFirst() // 暂时使用第一个可用插件
                .orElse(null);
                
            if (targetPlugin == null) {
                log.warn("未找到支持检查的插件: clusterId={}, hostIp={}, checkType={}", clusterId, hostIp, checkType);
                return;
            }
            
            // 构建上下文 - 需要从状态管理器获取SSH连接信息
            HostCheckContext context = HostCheckContext.builder()
                .clusterId(clusterId.toString())
                .hostIp(hostIp)
                .build();
                
            CheckResult result = targetPlugin.executeCheck(context).get(); // 同步等待结果
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
    
    /**
     * 获取可用的校验插件
     */
    private List<HostCheckerPlugin> getAvailableValidationPlugins() {
        return pluginManager.getPf4jManager().getExtensions(HostCheckerPlugin.class)
            .stream()
            .sorted(Comparator.comparingInt(HostCheckerPlugin::getPriority))
            .toList();
    }
}
