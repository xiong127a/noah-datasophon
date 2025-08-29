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
import com.datasophon.plugins.manager.PluginManager;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 主机校验服务实现
 * 纯插件化架构：主程序只负责插件发现、调用和结果汇总
 * 
 * 设计原则：
 * 1. 最小化业务逻辑，所有具体逻辑交给插件处理
 * 2. 只负责插件的生命周期管理和结果聚合
 * 3. 状态管理通过插件接口统一处理
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
    
    // 活跃的校验任务跟踪
    private final Map<Long, String> activeValidationTasks = new ConcurrentHashMap<>();
    
    @Override
    public void startValidation(HostValidationRequestDTO request) {
        Long clusterId = request.clusterId();
        
        log.info("启动主机校验: clusterId={}, 主机数量={}", clusterId, request.hostIps().size());
        
        // 1. 创建校验会话
        stateManager.createValidationSession(clusterId, request);
        
        // 2. 通过db-scheduler调度执行校验任务
        String taskId = schedulerService.scheduleHostValidationNow(request);
        activeValidationTasks.put(clusterId, taskId);
        
        log.info("主机校验任务已调度: clusterId={}, taskId={}", clusterId, taskId);
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
        
        // 通过db-scheduler调度修复任务
        for (String hostIp : hostIps) {
            List<CheckType> failedCheckTypes = stateManager.getFailedCheckTypes(clusterId, hostIp);
            for (CheckType checkType : failedCheckTypes) {
                schedulerService.scheduleHostRepairNow(clusterId, hostIp, checkType);
            }
        }
    }
    
    @Override
    public void stopValidation(Long clusterId) {
        String taskId = activeValidationTasks.get(clusterId);
        if (taskId != null) {
            // 通过db-scheduler取消任务
            schedulerService.cancelTask(taskId);
            activeValidationTasks.remove(clusterId);
            log.info("停止主机校验任务: clusterId={}, taskId={}", clusterId, taskId);
        }
    }
    
    @Override
    public void executeValidation(HostValidationRequestDTO request) {
        Long clusterId = request.clusterId();
        
        try {
            log.info("开始执行主机校验: clusterId={}, 主机数量={}", clusterId, request.hostIps().size());
            
            // 1. 获取校验插件
            List<HostValidationPlugin> plugins = getAvailableValidationPlugins();
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
     * 校验单个主机 - 插件化处理
     */
    private void validateSingleHost(HostValidationRequestDTO request, String hostIp, 
                                   List<HostValidationPlugin> plugins) {
        Long clusterId = request.clusterId();
        
        try {
            HostCheckContext context = createHostCheckContext(request, hostIp);
            
            // 按优先级执行所有插件的检查项
            plugins.stream()
                .flatMap(plugin -> plugin.getSupportedCheckTypes().stream()
                    .map(checkType -> new Object[]{plugin, checkType}))
                .forEach(pair -> {
                    HostValidationPlugin plugin = (HostValidationPlugin) pair[0];
                    CheckType checkType = (CheckType) pair[1];
                    executePluginCheck(clusterId, hostIp, context, plugin, checkType);
                });
                
        } catch (Exception e) {
            log.error("主机校验异常: clusterId={}, hostIp={}, error={}", 
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
    private void handleCheckResult(Long clusterId, String hostIp, CheckType checkType, CheckResult result) {
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
                checkType.getDisplayName(), result.isSuccess() ? "通过" : "失败", message);
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
    
    @Override
    public void executeRepair(Long clusterId, String hostIp, CheckType checkType) {
        log.info("开始执行修复: clusterId={}, hostIp={}, checkType={}", clusterId, hostIp, checkType);
        
        try {
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
            CheckResult result = repairFuture.get(); // 同步等待结果
            
            handleRepairResult(clusterId, hostIp, checkType, result);
            
        } catch (Exception e) {
            log.error("修复执行异常: clusterId={}, hostIp={}, checkType={}, error={}", 
                    clusterId, hostIp, checkType, e.getMessage(), e);
            stateManager.updateCheckItemStatus(clusterId, hostIp, checkType, 
                ValidationStatus.FAILED, "修复执行异常: " + e.getMessage(), Map.of());
        }
    }
    
    // ==================== 工具方法 ====================
    
    /**
     * 获取所有可用的校验插件
     */
    private List<HostValidationPlugin> getAvailableValidationPlugins() {
        // 确保插件管理器已初始化
        if (!pluginManager.isInitialized()) {
            pluginManager.initializePlugins();
        }
        
        List<HostValidationPlugin> plugins = pluginManager.getPf4jManager().getExtensions(HostValidationPlugin.class);
        
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
        try {
            log.info("重新检查: clusterId={}, hostIp={}, checkType={}", clusterId, hostIp, checkType);
            
            // 更新状态为检查中
            stateManager.updateCheckItemStatus(clusterId, hostIp, checkType, 
                ValidationStatus.CHECKING, "重新检查中...", Map.of());
            
            // 获取校验请求信息
            HostValidationRequestDTO request = getValidationRequest(clusterId);
            if (request == null) {
                log.error("无法获取校验请求信息: clusterId={}", clusterId);
                stateManager.updateCheckItemStatus(clusterId, hostIp, checkType, 
                    ValidationStatus.FAILED, "无法获取校验请求信息", Map.of());
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
    }

    @Override
    public void startRepair(Long clusterId, String hostIp, CheckType checkType) {
        try {
            String taskId = schedulerService.scheduleHostRepairNow(clusterId, hostIp, checkType);
            log.info("启动修复任务: clusterId={}, hostIp={}, checkType={}, taskId={}", 
                    clusterId, hostIp, checkType, taskId);
        } catch (Exception e) {
            log.error("启动修复任务失败: clusterId={}, hostIp={}, checkType={}, error={}", 
                    clusterId, hostIp, checkType, e.getMessage(), e);
            
            // 更新状态为修复失败
            stateManager.updateCheckItemStatus(clusterId, hostIp, checkType, 
                ValidationStatus.FAILED, "启动修复任务失败: " + e.getMessage(), Map.of());
        }
    }



    /**
     * 处理修复结果
     */
    private void handleRepairResult(Long clusterId, String hostIp, CheckType checkType, CheckResult result) {
        ValidationStatus status = result.isSuccess() ? ValidationStatus.SUCCESS : ValidationStatus.FAILED;
        String message = result.getMessage() != null ? result.getMessage() : 
                        (result.isSuccess() ? "修复成功" : "修复失败");
        
        // 更新检查项状态
        stateManager.updateCheckItemStatus(clusterId, hostIp, checkType, status, message, 
                result.getData() != null ? result.getData() : Map.of());
        
        // 添加日志
        String logMessage = String.format("修复项 [%s] %s: %s", 
                checkType.getDisplayName(), result.isSuccess() ? "成功" : "失败", message);
        stateManager.addLog(clusterId, hostIp, logMessage);
        
        log.info("修复项完成: clusterId={}, hostIp={}, checkType={}, success={}, message={}", 
                clusterId, hostIp, checkType, result.isSuccess(), message);
    }

    /**
     * 获取可用的修复插件
     */
    private List<HostRepairPlugin> getAvailableRepairPlugins() {
        // 确保插件管理器已初始化
        if (!pluginManager.isInitialized()) {
            pluginManager.initializePlugins();
        }
        
        List<HostRepairPlugin> plugins = pluginManager.getPf4jManager().getExtensions(HostRepairPlugin.class);
        
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
                .map(HostValidationStateManager.HostValidationSession::getRequest)
                .orElse(null);
    }
}