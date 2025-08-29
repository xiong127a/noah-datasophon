package com.datasophon.api.hostvalidation.service.impl;

import com.datasophon.api.hostvalidation.manager.HostValidationStateManager;
import com.datasophon.api.hostvalidation.scheduler.HostValidationSchedulerService;
import com.datasophon.api.hostvalidation.service.HostValidationService;
import com.datasophon.common.dto.HostValidationRequestDTO;
import com.datasophon.common.enums.CheckType;
import com.datasophon.common.enums.ValidationStatus;
import com.datasophon.common.vo.HostValidationStatusVO;
import com.datasophon.plugins.api.HostValidationPlugin;
import com.datasophon.plugins.api.HostRepairPlugin;
import com.datasophon.plugins.api.model.CheckResult;
import com.datasophon.plugins.api.model.HostCheckContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pf4j.PluginManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 主机校验服务实现
 * 基于插件化架构，主程序只负责调用和编排插件
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-28
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HostValidationServiceImpl implements HostValidationService {
    
    private final HostValidationStateManager stateManager;
    private final PluginManager pluginManager;
    private final HostValidationSchedulerService schedulerService;
    
    @Value("${datasophon.host-validation.scheduler.enabled:true}")
    private boolean schedulerEnabled;
    
    @Value("${datasophon.host-validation.auto-cleanup.enabled:true}")
    private boolean autoCleanupEnabled;
    
    @Value("${datasophon.host-validation.auto-cleanup.delay-minutes:30}")
    private int autoCleanupDelayMinutes;
    
    // 校验任务执行器
    private final ExecutorService validationExecutor = Executors.newVirtualThreadPerTaskExecutor();
    
    // 活跃的校验任务
    private final Map<Long, CompletableFuture<Void>> activeValidations = new ConcurrentHashMap<>();
    
    @Override
    public void startValidation(HostValidationRequestDTO request) {
        Long clusterId = request.clusterId();
        
        log.info("启动主机校验: clusterId={}, 主机数量={}", clusterId, request.hostIps().size());
        
        // 创建校验会话
        stateManager.createValidationSession(clusterId, request);
        
        // 异步执行校验任务
        CompletableFuture<Void> validationTask = CompletableFuture.runAsync(
            () -> executeValidation(request), validationExecutor
        );
        
        // 记录活跃任务
        activeValidations.put(clusterId, validationTask);
        
        // 任务完成后清理
        validationTask.whenComplete((result, throwable) -> {
            activeValidations.remove(clusterId);
            stateManager.completeValidationSession(clusterId);
            
            if (throwable != null) {
                log.error("主机校验任务异常: clusterId={}, error={}", clusterId, throwable.getMessage(), throwable);
            } else {
                log.info("主机校验任务完成: clusterId={}", clusterId);
                
                // 如果启用了自动清理，调度清理任务
                if (autoCleanupEnabled && schedulerEnabled) {
                    try {
                        String cleanupTaskId = schedulerService.scheduleHostCleanup(clusterId, autoCleanupDelayMinutes);
                        log.info("已调度自动清理任务: clusterId={}, taskId={}, delayMinutes={}", 
                                clusterId, cleanupTaskId, autoCleanupDelayMinutes);
                    } catch (Exception e) {
                        log.warn("调度自动清理任务失败: clusterId={}, error={}", clusterId, e.getMessage());
                    }
                }
            }
        });
    }
    
    @Override
    public List<HostValidationStatusVO> getValidationStatus(Long clusterId) {
        return stateManager.getValidationSession(clusterId)
            .map(session -> List.copyOf(session.getHostStatuses().values()))
            .orElse(List.of());
    }
    
    @Override
    public void repairFailedChecks(Long clusterId, List<String> hostIps) {
        log.info("启动主机修复: clusterId={}, 主机数量={}", clusterId, hostIps.size());
        
        // 异步执行修复任务
        CompletableFuture.runAsync(() -> executeRepair(clusterId, hostIps), validationExecutor);
    }
    
    @Override
    public void stopValidation(Long clusterId) {
        CompletableFuture<Void> task = activeValidations.get(clusterId);
        if (task != null) {
            task.cancel(true);
            activeValidations.remove(clusterId);
            log.info("停止主机校验任务: clusterId={}", clusterId);
        }
    }
    
    /**
     * 执行主机校验
     * 主程序只负责编排，具体检查逻辑都在插件中
     */
    private void executeValidation(HostValidationRequestDTO request) {
        Long clusterId = request.clusterId();
        
        try {
            // 获取所有可用的校验插件
            List<HostValidationPlugin> validationPlugins = getAvailableValidationPlugins();
            
            if (validationPlugins.isEmpty()) {
                log.warn("没有找到可用的主机校验插件: clusterId={}", clusterId);
                return;
            }
            
            // 对每个主机执行校验
            for (String hostIp : request.hostIps()) {
                if (Thread.currentThread().isInterrupted()) {
                    log.info("校验任务被中断: clusterId={}", clusterId);
                    return;
                }
                
                log.info("开始校验主机: clusterId={}, hostIp={}", clusterId, hostIp);
                validateSingleHost(request, hostIp, validationPlugins);
            }
            
        } catch (Exception e) {
            log.error("主机校验执行异常: clusterId={}, error={}", clusterId, e.getMessage(), e);
        }
    }
    
    /**
     * 校验单个主机
     * 按优先级顺序调用插件，每个插件执行其支持的所有检查项
     */
    private void validateSingleHost(HostValidationRequestDTO request, String hostIp, 
                                   List<HostValidationPlugin> validationPlugins) {
        Long clusterId = request.clusterId();
        
        try {
            // 创建主机检查上下文
            HostCheckContext context = createHostCheckContext(request, hostIp);
            
            // 按优先级顺序执行插件检查
            for (HostValidationPlugin plugin : validationPlugins) {
                if (Thread.currentThread().isInterrupted()) {
                    return;
                }
                
                // 执行插件支持的所有检查项
                for (String checkType : plugin.getSupportedCheckTypes()) {
                    if (Thread.currentThread().isInterrupted()) {
                        return;
                    }
                    
                    executePluginCheck(clusterId, hostIp, context, plugin, checkType);
                }
            }
            
        } catch (Exception e) {
            log.error("单主机校验异常: clusterId={}, hostIp={}, error={}", 
                    clusterId, hostIp, e.getMessage(), e);
            
            stateManager.addLog(clusterId, hostIp, "校验异常: " + e.getMessage());
        }
    }
    
    /**
     * 执行单个插件的特定检查项
     * 主程序只负责调用插件和处理结果
     */
    private void executePluginCheck(Long clusterId, String hostIp, HostCheckContext context, 
                                   HostValidationPlugin plugin, CheckType checkType) {
        String pluginId = plugin.getPluginId();
        
        log.debug("执行插件检查: clusterId={}, hostIp={}, plugin={}, checkType={}", 
                clusterId, hostIp, pluginId, checkType);
        
        try {
            // 检查插件是否可以执行此检查项
            if (!plugin.canExecute(context, checkType)) {
                log.debug("插件跳过检查: clusterId={}, hostIp={}, plugin={}, checkType={}, reason=canExecute返回false", 
                        clusterId, hostIp, pluginId, checkType);
                return;
            }
            
            // 更新状态为检查中
            stateManager.updateCheckItemStatus(clusterId, hostIp, checkType, 
                    ValidationStatus.CHECKING, "正在检查...", Map.of());
            
            // 调用插件执行检查
            CompletableFuture<CheckResult> checkFuture = plugin.executeCheck(context, checkType);
            
            // 处理检查结果
            checkFuture.whenComplete((result, throwable) -> {
                if (throwable != null) {
                    // 插件执行异常
                    log.error("插件检查异常: clusterId={}, hostIp={}, plugin={}, checkType={}, error={}", 
                            clusterId, hostIp, pluginId, checkType, throwable.getMessage(), throwable);
                    
                    stateManager.updateCheckItemStatus(clusterId, hostIp, checkType, 
                            ValidationStatus.FAILED, "检查异常: " + throwable.getMessage(), Map.of());
                } else if (result != null) {
                    // 处理检查结果
                    handleCheckResult(clusterId, hostIp, checkType, result);
                } else {
                    // 结果为空
                    log.warn("插件检查结果为空: clusterId={}, hostIp={}, plugin={}, checkType={}", 
                            clusterId, hostIp, pluginId, checkType);
                    
                    stateManager.updateCheckItemStatus(clusterId, hostIp, checkType, 
                            ValidationStatus.FAILED, "检查结果为空", Map.of());
                }
            });
            
        } catch (Exception e) {
            log.error("插件检查调用异常: clusterId={}, hostIp={}, plugin={}, checkType={}, error={}", 
                    clusterId, hostIp, pluginId, checkType, e.getMessage(), e);
            
            stateManager.updateCheckItemStatus(clusterId, hostIp, checkType, 
                    ValidationStatus.FAILED, "调用异常: " + e.getMessage(), Map.of());
        }
    }
    
    /**
     * 处理插件检查结果
     */
    private void handleCheckResult(Long clusterId, String hostIp, String checkType, CheckResult result) {
        ValidationStatus status = result.isSuccess() ? ValidationStatus.SUCCESS : ValidationStatus.FAILED;
        String message = result.getMessage() != null ? result.getMessage() : 
                        (result.isSuccess() ? "检查通过" : "检查失败");
        
        // 处理错误信息
        if (!result.isSuccess() && result.getError() != null) {
            message = message + ": " + result.getError();
        }
        
        // 更新检查项状态
        stateManager.updateCheckItemStatus(clusterId, hostIp, checkType, status, message, 
                result.getData() != null ? result.getData() : Map.of());
        
        // 添加日志
        String logMessage = String.format("检查项 [%s] %s: %s", 
                checkType, result.isSuccess() ? "通过" : "失败", message);
        stateManager.addLog(clusterId, hostIp, logMessage);
        
        // 特殊处理：如果是主机名收集成功，更新主机信息
        if (result.isSuccess() && result.hasData("hostname")) {
            String hostname = result.getData("hostname", String.class);
            if (hostname != null && !hostname.isEmpty()) {
                stateManager.updateHostInfo(clusterId, hostIp, hostname);
            }
        }
        
        log.debug("检查项完成: clusterId={}, hostIp={}, checkType={}, success={}, message={}", 
                clusterId, hostIp, checkType, result.isSuccess(), message);
    }
    
    /**
     * 执行修复
     */
    private void executeRepair(Long clusterId, List<String> hostIps) {
        log.info("开始执行修复: clusterId={}, hostIps={}", clusterId, hostIps);
        
        try {
            // 获取可用的修复插件
            List<HostRepairPlugin> repairPlugins = getAvailableRepairPlugins();
            
            if (repairPlugins.isEmpty()) {
                log.warn("没有找到可用的主机修复插件: clusterId={}", clusterId);
                return;
            }
            
            // 获取校验请求信息
            HostValidationRequestDTO request = getValidationRequest(clusterId);
            if (request == null) {
                log.error("无法获取校验请求信息: clusterId={}", clusterId);
                return;
            }
            
            // 对每个主机执行修复
            for (String hostIp : hostIps) {
                if (Thread.currentThread().isInterrupted()) {
                    log.info("修复任务被中断: clusterId={}", clusterId);
                    return;
                }
                
                repairSingleHost(clusterId, hostIp, request, repairPlugins);
            }
            
        } catch (Exception e) {
            log.error("修复执行异常: clusterId={}, error={}", clusterId, e.getMessage(), e);
        }
    }
    
    // ==================== 工具方法 ====================
    
    /**
     * 获取所有可用的校验插件
     */
    private List<HostValidationPlugin> getAvailableValidationPlugins() {
        List<HostValidationPlugin> plugins = pluginManager.getExtensions(HostValidationPlugin.class);
        
        // 按优先级排序（数字越小优先级越高）
        plugins.sort(Comparator.comparing(HostValidationPlugin::getPriority));
        
        // 过滤健康的插件
        List<HostValidationPlugin> healthyPlugins = plugins.stream()
            .filter(plugin -> {
                try {
                    return plugin.isHealthy();
                } catch (Exception e) {
                    log.warn("插件健康检查异常: plugin={}, error={}", 
                            plugin.getPluginId(), e.getMessage());
                    return false;
                }
            })
            .toList();
        
        log.info("发现可用的校验插件: {}", 
                healthyPlugins.stream().map(HostValidationPlugin::getPluginId).toList());
        
        return healthyPlugins;
    }
    
    /**
     * 创建主机检查上下文
     */
    private HostCheckContext createHostCheckContext(HostValidationRequestDTO request, String hostIp) {
        HostCheckContext context = HostCheckContext.builder()
            .clusterId(request.clusterId().toString())
            .hostIp(hostIp)
            .sshPort(request.sshPort())
            .sshUser(request.sshUser())
            .sshPassword(request.sshPassword())
            .privateKey(loadPrivateKeyContent(request.privateKeyPath()))
            .privateKeyPath(request.privateKeyPath())
            .connectionTimeout(30000)
            .commandTimeout(60000)
            .retryCount(3)
            .retryInterval(5000)
            .verboseLogging(false)
            .build();
        
        // 验证上下文有效性
        if (!context.isValid()) {
            log.warn("创建的主机检查上下文无效: hostIp={}, authType={}", 
                    hostIp, context.getAuthType());
        }
        
        log.debug("创建主机检查上下文: hostIp={}, authType={}, valid={}", 
                hostIp, context.getAuthType(), context.isValid());
        
        return context;
    }
    
    /**
     * 加载私钥内容
     */
    private String loadPrivateKeyContent(String privateKeyPath) {
        if (privateKeyPath == null || privateKeyPath.isEmpty()) {
            return null;
        }
        
        try {
            return java.nio.file.Files.readString(java.nio.file.Paths.get(privateKeyPath));
        } catch (Exception e) {
            log.warn("加载私钥文件失败: path={}, error={}", privateKeyPath, e.getMessage());
            return null;
        }
    }

    @Override
    public void pauseValidation(Long clusterId, String hostIp) {
        stateManager.pauseHost(clusterId, hostIp);
        log.info("暂停校验: clusterId={}, hostIp={}", clusterId, hostIp);
    }

    @Override
    public void resumeValidation(Long clusterId, String hostIp) {
        stateManager.resumeHost(clusterId, hostIp);
        log.info("继续校验: clusterId={}, hostIp={}", clusterId, hostIp);
    }

    @Override
    public void recheckItem(Long clusterId, String hostIp, CheckType checkType) {
        virtualThreadExecutor.submit(() -> {
            try {
                log.info("重新检查: clusterId={}, hostIp={}, checkType={}", clusterId, hostIp, checkType);
                
                // 获取校验请求信息
                HostValidationRequestDTO request = getValidationRequest(clusterId);
                if (request == null) {
                    log.error("无法获取校验请求信息: clusterId={}", clusterId);
                    return;
                }
                
                HostCheckContext context = createHostCheckContext(request, hostIp);
                
                // 获取校验插件
                List<HostValidationPlugin> validationPlugins = getAvailableValidationPlugins();
                HostValidationPlugin validationPlugin = validationPlugins.stream()
                    .filter(plugin -> plugin.getSupportedCheckTypes().contains(checkType))
                    .filter(plugin -> plugin.canExecute(context, checkType))
                    .findFirst()
                    .orElse(null);
                
                if (validationPlugin == null) {
                    stateManager.updateCheckItemStatus(clusterId, hostIp, checkType, 
                        ValidationStatus.FAILED, "没有可用的校验插件", Map.of());
                    return;
                }
                
                // 执行检查
                executePluginCheck(clusterId, hostIp, context, validationPlugin, checkType);
                
            } catch (Exception e) {
                log.error("重新检查失败: clusterId={}, hostIp={}, checkType={}, error={}", 
                        clusterId, hostIp, checkType, e.getMessage(), e);
                stateManager.updateCheckItemStatus(clusterId, hostIp, checkType, 
                    ValidationStatus.FAILED, "重新检查失败: " + e.getMessage(), Map.of());
            }
        });
    }

    @Override
    public void startRepair(Long clusterId, String hostIp, CheckType checkType) {
        // 如果启用了调度器，使用调度器执行修复任务
        if (schedulerEnabled) {
            try {
                String taskId = schedulerService.scheduleHostRepairNow(clusterId, hostIp, checkType);
                log.info("通过调度器启动修复任务: clusterId={}, hostIp={}, checkType={}, taskId={}", 
                        clusterId, hostIp, checkType, taskId);
                return;
            } catch (Exception e) {
                log.warn("调度器修复任务失败，使用线程池执行: clusterId={}, hostIp={}, checkType={}, error={}", 
                        clusterId, hostIp, checkType, e.getMessage());
            }
        }
        
        // 回退到线程池执行
        virtualThreadExecutor.submit(() -> {
            try {
                log.info("开始修复: clusterId={}, hostIp={}, checkType={}", clusterId, hostIp, checkType);
                
                // 更新状态为修复中
                stateManager.updateCheckItemStatus(clusterId, hostIp, checkType, 
                    ValidationStatus.REPAIRING, "开始修复...", Map.of());
                
                // 获取校验请求信息
                HostValidationRequestDTO request = getValidationRequest(clusterId);
                if (request == null) {
                    stateManager.updateCheckItemStatus(clusterId, hostIp, checkType, 
                        ValidationStatus.FAILED, "无法获取校验请求信息", Map.of());
                    return;
                }
                
                HostCheckContext context = createHostCheckContext(request, hostIp);
                
                // 获取修复插件
                List<HostRepairPlugin> repairPlugins = getAvailableRepairPlugins();
                HostRepairPlugin repairPlugin = repairPlugins.stream()
                    .filter(plugin -> plugin.canRepair(context, checkType))
                    .findFirst()
                    .orElse(null);
                
                if (repairPlugin == null) {
                    stateManager.updateCheckItemStatus(clusterId, hostIp, checkType, 
                        ValidationStatus.FAILED, "没有可用的修复插件", Map.of());
                    return;
                }
                
                // 执行修复
                CompletableFuture<CheckResult> repairFuture = repairPlugin.executeRepair(context, checkType, Map.of());
                repairFuture.whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        log.error("修复插件执行异常: clusterId={}, hostIp={}, checkType={}, error={}", 
                                clusterId, hostIp, checkType, throwable.getMessage(), throwable);
                        stateManager.updateCheckItemStatus(clusterId, hostIp, checkType, 
                            ValidationStatus.FAILED, "修复插件执行异常: " + throwable.getMessage(), Map.of());
                    } else if (result != null) {
                        handleRepairResult(clusterId, hostIp, checkType, result);
                    }
                });
                
            } catch (Exception e) {
                log.error("修复执行失败: clusterId={}, hostIp={}, checkType={}, error={}", 
                        clusterId, hostIp, checkType, e.getMessage(), e);
                stateManager.updateCheckItemStatus(clusterId, hostIp, checkType, 
                    ValidationStatus.FAILED, "修复执行失败: " + e.getMessage(), Map.of());
            }
        });
    }

    /**
     * 修复单个主机
     */
    private void repairSingleHost(Long clusterId, String hostIp, HostValidationRequestDTO request, 
                                 List<HostRepairPlugin> repairPlugins) {
        try {
            log.info("开始修复主机: clusterId={}, hostIp={}", clusterId, hostIp);
            
            HostCheckContext context = createHostCheckContext(request, hostIp);
            
            // 获取该主机的失败检查项
            List<String> failedCheckTypes = stateManager.getFailedCheckTypes(clusterId, hostIp);
            
            for (String checkType : failedCheckTypes) {
                if (Thread.currentThread().isInterrupted()) {
                    return;
                }
                
                // 找到支持修复此检查项的插件
                HostRepairPlugin repairPlugin = repairPlugins.stream()
                    .filter(plugin -> plugin.canRepair(context, checkType))
                    .findFirst()
                    .orElse(null);
                
                if (repairPlugin != null) {
                    executePluginRepair(clusterId, hostIp, context, repairPlugin, checkType);
                } else {
                    log.warn("没有找到支持修复的插件: clusterId={}, hostIp={}, checkType={}", 
                            clusterId, hostIp, checkType);
                }
            }
            
        } catch (Exception e) {
            log.error("单主机修复异常: clusterId={}, hostIp={}, error={}", 
                    clusterId, hostIp, e.getMessage(), e);
        }
    }

    /**
     * 执行插件修复
     */
    private void executePluginRepair(Long clusterId, String hostIp, HostCheckContext context, 
                                   HostRepairPlugin plugin, String checkType) {
        try {
            // 更新状态为修复中
            stateManager.updateCheckItemStatus(clusterId, hostIp, checkType, 
                ValidationStatus.REPAIRING, "正在修复...", Map.of());
            
            // 调用插件执行修复
            CompletableFuture<CheckResult> repairFuture = plugin.executeRepair(context, checkType, Map.of());
            
            // 处理修复结果
            repairFuture.whenComplete((result, throwable) -> {
                if (throwable != null) {
                    log.error("插件修复异常: clusterId={}, hostIp={}, checkType={}, error={}", 
                            clusterId, hostIp, checkType, throwable.getMessage(), throwable);
                    stateManager.updateCheckItemStatus(clusterId, hostIp, checkType, 
                        ValidationStatus.FAILED, "修复异常: " + throwable.getMessage(), Map.of());
                } else if (result != null) {
                    handleRepairResult(clusterId, hostIp, checkType, result);
                }
            });
            
        } catch (Exception e) {
            log.error("插件修复调用异常: clusterId={}, hostIp={}, checkType={}, error={}", 
                    clusterId, hostIp, checkType, e.getMessage(), e);
            stateManager.updateCheckItemStatus(clusterId, hostIp, checkType, 
                ValidationStatus.FAILED, "修复调用异常: " + e.getMessage(), Map.of());
        }
    }

    /**
     * 处理修复结果
     */
    private void handleRepairResult(Long clusterId, String hostIp, String checkType, CheckResult result) {
        ValidationStatus status = result.isSuccess() ? ValidationStatus.SUCCESS : ValidationStatus.FAILED;
        String message = result.getMessage() != null ? result.getMessage() : 
                        (result.isSuccess() ? "修复成功" : "修复失败");
        
        // 更新检查项状态
        stateManager.updateCheckItemStatus(clusterId, hostIp, checkType, status, message, 
                result.getData() != null ? result.getData() : Map.of());
        
        // 添加日志
        String logMessage = String.format("修复项 [%s] %s: %s", 
                checkType, result.isSuccess() ? "成功" : "失败", message);
        stateManager.addLog(clusterId, hostIp, logMessage);
        
        log.info("修复项完成: clusterId={}, hostIp={}, checkType={}, success={}, message={}", 
                clusterId, hostIp, checkType, result.isSuccess(), message);
    }

    /**
     * 获取可用的修复插件
     */
    private List<HostRepairPlugin> getAvailableRepairPlugins() {
        List<HostRepairPlugin> plugins = pluginManager.getExtensions(HostRepairPlugin.class);
        
        // 过滤健康的插件
        List<HostRepairPlugin> healthyPlugins = plugins.stream()
            .filter(plugin -> {
                try {
                    return plugin.isHealthy();
                } catch (Exception e) {
                    log.warn("修复插件健康检查异常: plugin={}, error={}", 
                            plugin.getPluginId(), e.getMessage());
                    return false;
                }
            })
            .toList();
        
        log.debug("发现可用的修复插件: {}", 
                healthyPlugins.stream().map(HostRepairPlugin::getPluginId).toList());
        
        return healthyPlugins;
    }

    /**
     * 获取校验请求信息
     */
    private HostValidationRequestDTO getValidationRequest(Long clusterId) {
        return stateManager.getValidationSession(clusterId)
                .map(session -> session.getRequest())
                .orElse(null);
    }
}