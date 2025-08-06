package com.datasophon.api.workflow.impl;

import com.datasophon.api.workflow.SingleHostCheckWorkflow;
import com.datasophon.api.workflow.activity.HostCheckActivities;
import com.datasophon.api.workflow.model.*;
import com.datasophon.common.model.OsInfo;
import com.datasophon.plugins.api.model.CheckResult;
import com.datasophon.plugins.api.model.CheckConfiguration;
import com.datasophon.plugins.api.model.HostCheckContext;
import com.datasophon.common.model.HostInfo;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Async;
import io.temporal.workflow.Promise;
import io.temporal.workflow.Workflow;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 单个主机检查工作流实现类
 * 
 * @author DataSophon Team
 */
@Slf4j
public class SingleHostCheckWorkflowImpl implements SingleHostCheckWorkflow {
    
    // 工作流状态
    private WorkflowStatus currentStatus = WorkflowStatus.PENDING;

    // 进度跟踪
    private CheckProgress progress = CheckProgress.builder()
            .currentStatus(WorkflowStatus.PENDING)
            .startTime(LocalDateTime.now())
            .build();
    
    // 配置活动选项
    private final ActivityOptions defaultActivityOptions = ActivityOptions.newBuilder()
            .setScheduleToCloseTimeout(Duration.ofMinutes(10))
            .setRetryOptions(RetryOptions.newBuilder()
                    .setMaximumAttempts(3)
                    .setBackoffCoefficient(2.0)
                    .setInitialInterval(Duration.ofSeconds(1))
                    .setMaximumInterval(Duration.ofMinutes(1))
                    .build())
            .build();
    
    // 活动存根
    private final HostCheckActivities activities = Workflow.newActivityStub(
            HostCheckActivities.class, defaultActivityOptions);
    
    @Override
    public HostCheckResult executeHostCheck(HostCheckRequest request) {
        log.info("开始执行单个主机检查工作流: {} -> {}", 
                request.getRequestId(), request.getHostInfo().getIp());
        
        // 初始化工作流状态
        currentStatus = WorkflowStatus.RUNNING;
        progress = CheckProgress.builder()
                .requestId(request.getRequestId())
                .currentStatus(WorkflowStatus.RUNNING)
                .startTime(LocalDateTime.now())
                .currentHost(request.getHostInfo().getIp())
                .build();
        
        try {
            // 阶段1：健康检查
            if (!executeHealthCheck(request.getHostInfo())) {
                return createFailedResult(request, "主机健康检查失败");
            }
            
            // 阶段2：收集主机信息
            HostInfo hostInfo = activities.collectHostInfo(request.getHostInfo());
            progress.setCurrentHost(hostInfo.getIp());
            
            // 阶段3：检测操作系统
            OsInfo osInfo = activities.detectOperatingSystem(hostInfo);
            
            // 阶段4：创建SSH会话
            if (!activities.createSshSession(hostInfo)) {
                return createFailedResult(request, "SSH会话创建失败");
            }
            
            try {
                // 阶段5：发现可用插件
                List<String> pluginIds = request.getPluginIds();
                if (pluginIds == null || pluginIds.isEmpty()) {
                    pluginIds = activities.discoverPlugins(osInfo, null);
                }
                
                // 阶段6：执行检查策略
                List<CheckResult> results = executeCheckStrategy(request, hostInfo, osInfo, pluginIds);
                
                // 阶段7：聚合结果
                String summary = activities.aggregateResults(results);
                
                // 阶段8：发送通知（异步）
                Async.procedure(activities::sendNotification, hostInfo, results);
                
                // 阶段9：保存结果
                activities.saveCheckResults(hostInfo, results);
                
                // 构建最终结果
                HostCheckResult result = buildHostCheckResult(request, hostInfo, osInfo, results, summary);
                
                currentStatus = result.isAllSuccess() ? WorkflowStatus.COMPLETED : WorkflowStatus.PARTIAL_SUCCESS;
                progress.setCurrentStatus(currentStatus);
                
                log.info("单个主机检查工作流执行完成: {} -> {}, 状态: {}", 
                        request.getRequestId(), hostInfo.getIp(), currentStatus);
                
                return result;
                
            } finally {
                // 清理SSH会话
                activities.closeSshSession(hostInfo);
            }
            
        } catch (Exception e) {
            log.error("单个主机检查工作流执行失败: {} -> {}", 
                    request.getRequestId(), request.getHostInfo().getIp(), e);
                    
            currentStatus = WorkflowStatus.FAILED;
            progress.setCurrentStatus(WorkflowStatus.FAILED);
            progress.setErrorMessage(e.getMessage());
            
            return createFailedResult(request, "工作流执行异常: " + e.getMessage());
        }
    }
    
    @Override
    public void pauseCheck() {
        log.info("暂停检查工作流");

        currentStatus = WorkflowStatus.PAUSED;
        progress.setCurrentStatus(WorkflowStatus.PAUSED);
    }
    
    @Override
    public void resumeCheck() {
        log.info("恢复检查工作流");

        currentStatus = WorkflowStatus.RUNNING;
        progress.setCurrentStatus(WorkflowStatus.RUNNING);
    }
    
    @Override
    public void stopCheck() {
        log.info("停止检查工作流");
        currentStatus = WorkflowStatus.CANCELLED;
        progress.setCurrentStatus(WorkflowStatus.CANCELLED);
    }
    
    @Override
    public CheckProgress getProgress() {
        progress.setCurrentTime(LocalDateTime.now());
        return progress;
    }
    
    @Override
    public WorkflowStatus getStatus() {
        return currentStatus;
    }
    
    /**
     * 执行健康检查
     */
    private boolean executeHealthCheck(HostInfo hostInfo) {
        try {
            return activities.healthCheck(hostInfo);
        } catch (Exception e) {
            log.error("健康检查失败: {}", hostInfo.getIp(), e);
            return false;
        }
    }
    
    /**
     * 根据策略执行检查
     */
    private List<CheckResult> executeCheckStrategy(HostCheckRequest request, HostInfo hostInfo, 
                                                  OsInfo osInfo, List<String> pluginIds) {
        
        HostCheckContext context = HostCheckContext.builder()
                .requestId(request.getRequestId())
                .hostInfo(hostInfo)
                .osInfo(osInfo)
                .timeout(request.getTimeoutMs())
                .failFast(request.isFailFast())
                .configuration(CheckConfiguration.builder()
                        .timeoutMs(request.getTimeoutMs())
                        .retryCount(request.getRetryCount())
                        .failFast(request.isFailFast())
                        .concurrency(request.getConcurrency())
                        .build())
                .build();

        return switch (request.getStrategy()) {
            case SERIAL -> executeSerial(pluginIds, context);
            case PARALLEL -> executeParallel(pluginIds, context);
            case PRIORITY_BASED -> executePriorityBased(pluginIds, context);
            case PIPELINE -> executePipeline(pluginIds, context);
            case FAST_CHECK -> executeFastCheck(pluginIds, context);
            case COMPREHENSIVE_CHECK -> executeComprehensive(pluginIds, context);
            default -> executeParallel(pluginIds, context);
        };
    }
    
    /**
     * 串行执行插件
     */
    private List<CheckResult> executeSerial(List<String> pluginIds, HostCheckContext context) {
        log.info("串行执行插件检查，插件数量: {}", pluginIds.size());
        return activities.executePlugins(pluginIds, context);
    }
    
    /**
     * 并行执行插件
     */
    private List<CheckResult> executeParallel(List<String> pluginIds, HostCheckContext context) {
        log.info("并行执行插件检查，插件数量: {}", pluginIds.size());
        
        List<Promise<CheckResult>> promises = pluginIds.stream()
                .map(pluginId -> Async.function(activities::executePlugin, pluginId, context))
                .toList();
        
        return promises.stream()
                .map(Promise::get)
                .collect(Collectors.toList());
    }
    
    /**
     * 优先级执行插件
     */
    private List<CheckResult> executePriorityBased(List<String> pluginIds, HostCheckContext context) {
        log.info("优先级执行插件检查，插件数量: {}", pluginIds.size());
        // 这里应该根据插件优先级排序后串行执行
        return activities.executePlugins(pluginIds, context);
    }
    
    /**
     * 管道执行插件
     */
    private List<CheckResult> executePipeline(List<String> pluginIds, HostCheckContext context) {
        log.info("管道执行插件检查，插件数量: {}", pluginIds.size());
        
        List<CheckResult> results = new ArrayList<>();
        
        for (String pluginId : pluginIds) {
            CheckResult result = activities.executePlugin(pluginId, context);
            results.add(result);
            
            // 将当前结果添加到上下文，供下个插件使用
            context.addSharedData(pluginId + "_result", result);
            
            // 如果快速失败且当前检查失败，则停止
            if (context.isFailFast() && result.isFailed()) {
                break;
            }
        }
        
        return results;
    }
    
    /**
     * 快速检查
     */
    private List<CheckResult> executeFastCheck(List<String> pluginIds, HostCheckContext context) {
        log.info("快速检查模式，只执行核心插件");
        
        // 快速检查只执行最重要的几个插件
        List<String> corePlugins = pluginIds.stream()
                .filter(id -> id.contains("cpu") || id.contains("memory") || id.contains("disk"))
                .limit(3)
                .collect(Collectors.toList());
        
        return executeParallel(corePlugins, context);
    }
    
    /**
     * 全面检查
     */
    private List<CheckResult> executeComprehensive(List<String> pluginIds, HostCheckContext context) {
        log.info("全面检查模式，执行所有可用插件");
        return executeParallel(pluginIds, context);
    }
    
    /**
     * 构建主机检查结果
     */
    private HostCheckResult buildHostCheckResult(HostCheckRequest request, HostInfo hostInfo, 
                                               OsInfo osInfo, List<CheckResult> results, String summary) {
        
        LocalDateTime endTime = LocalDateTime.now();
        long executionTime = java.time.Duration.between(
                java.time.Instant.ofEpochMilli(request.getStartTime()), 
                java.time.Instant.now()).toMillis();
        
        long successCount = results.stream().mapToLong(r -> r.isSuccess() ? 1 : 0).sum();
        long failedCount = results.stream().mapToLong(r -> r.isFailed() ? 1 : 0).sum();
        long errorCount = results.stream().mapToLong(r -> r.hasError() ? 1 : 0).sum();
        long skippedCount = results.stream().mapToLong(r -> r.getStatus().name().equals("SKIPPED") ? 1 : 0).sum();
        
        return HostCheckResult.builder()
                .requestId(request.getRequestId())
                .hostInfo(hostInfo)
                .osInfo(osInfo)
                .checkResults(results)
                .status(failedCount == 0 && errorCount == 0 ? WorkflowStatus.COMPLETED : WorkflowStatus.PARTIAL_SUCCESS)
                .startTime(java.time.LocalDateTime.ofInstant(
                        java.time.Instant.ofEpochMilli(request.getStartTime()),
                        java.time.ZoneId.systemDefault()))
                .endTime(endTime)
                .totalExecutionTimeMs(executionTime)
                .successCount((int) successCount)
                .failedCount((int) failedCount)
                .errorCount((int) errorCount)
                .skippedCount((int) skippedCount)
                .totalCount(results.size())
                .summary(summary)
                .build();
    }
    
    /**
     * 创建失败结果
     */
    private HostCheckResult createFailedResult(HostCheckRequest request, String errorMessage) {
        return HostCheckResult.builder()
                .requestId(request.getRequestId())
                .hostInfo(request.getHostInfo())
                .status(WorkflowStatus.FAILED)
                .startTime(LocalDateTime.ofInstant(
                        java.time.Instant.ofEpochMilli(request.getStartTime()),
                        java.time.ZoneId.systemDefault()))
                .endTime(LocalDateTime.now())
                .summary(errorMessage)
                .checkResults(new ArrayList<>())
                .totalCount(0)
                .successCount(0)
                .failedCount(1)
                .errorCount(0)
                .skippedCount(0)
                .build();
    }
}