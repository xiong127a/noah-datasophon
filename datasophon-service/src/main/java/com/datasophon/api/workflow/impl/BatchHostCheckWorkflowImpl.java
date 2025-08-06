package com.datasophon.api.workflow.impl;

import com.datasophon.api.workflow.BatchHostCheckWorkflow;
import com.datasophon.api.workflow.SingleHostCheckWorkflow;

import com.datasophon.api.workflow.model.*;
import com.datasophon.common.model.HostInfo;

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
 * 批量主机检查工作流实现类
 * 
 * @author DataSophon Team
 */
@Slf4j
public class BatchHostCheckWorkflowImpl implements BatchHostCheckWorkflow {
    
    // 工作流状态
    private WorkflowStatus currentStatus = WorkflowStatus.PENDING;

    // 进度跟踪
    private CheckProgress progress = CheckProgress.builder()
            .currentStatus(WorkflowStatus.PENDING)
            .startTime(LocalDateTime.now())
            .build();
    

    

    
    @Override
    public BatchCheckResult executeBatchCheck(BatchCheckRequest request) {
        log.info("开始执行批量主机检查工作流: {}, 主机数量: {}", 
                request.getBatchRequestId(), request.getHostInfos().size());
        
        currentStatus = WorkflowStatus.RUNNING;
        LocalDateTime startTime = LocalDateTime.now();
        
        progress = CheckProgress.builder()
                .requestId(request.getBatchRequestId())
                .currentStatus(WorkflowStatus.RUNNING)
                .startTime(startTime)
                .build();
        
        try {
            List<HostCheckResult> hostResults = switch (request.getBatchMode()) {
                case ALL_PARALLEL -> executeAllParallel(request);
                case BATCH_PARALLEL -> executeBatchParallel(request);
                case SEQUENTIAL -> executeSequential(request);
                case ROLLING -> executeRolling(request);
                default -> executeAllParallel(request);
            };

            // 构建批量结果
            BatchCheckResult result = buildBatchCheckResult(request, hostResults, startTime);
            
            currentStatus = result.isAllHostsSuccess() ? WorkflowStatus.COMPLETED : WorkflowStatus.PARTIAL_SUCCESS;
            progress.setCurrentStatus(currentStatus);
            
            log.info("批量主机检查工作流执行完成: {}, 成功率: {:.1f}%", 
                    request.getBatchRequestId(), result.getHostSuccessRate());
            
            return result;
            
        } catch (Exception e) {
            log.error("批量主机检查工作流执行失败: {}", request.getBatchRequestId(), e);
            
            currentStatus = WorkflowStatus.FAILED;
            progress.setCurrentStatus(WorkflowStatus.FAILED);
            progress.setErrorMessage(e.getMessage());
            
            return BatchCheckResult.builder()
                    .batchRequestId(request.getBatchRequestId())
                    .overallStatus(WorkflowStatus.FAILED)
                    .startTime(startTime)
                    .endTime(LocalDateTime.now())
                    .summary("批量检查执行异常: " + e.getMessage())
                    .build();
        }
    }
    
    @Override
    public void pauseCheck() {
        log.info("暂停批量检查工作流");

        currentStatus = WorkflowStatus.PAUSED;
        progress.setCurrentStatus(WorkflowStatus.PAUSED);
    }
    
    @Override
    public void resumeCheck() {
        log.info("恢复批量检查工作流");

        currentStatus = WorkflowStatus.RUNNING;
        progress.setCurrentStatus(WorkflowStatus.RUNNING);
    }
    
    @Override
    public void stopCheck() {
        log.info("停止批量检查工作流");
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
     * 全并行批量执行
     */
    private List<HostCheckResult> executeAllParallel(BatchCheckRequest request) {
        log.info("全并行执行批量检查，主机数量: {}", request.getHostInfos().size());
        
        List<Promise<HostCheckResult>> promises = request.getHostInfos().stream()
                .map(hostInfo -> {
                    HostCheckRequest singleRequest = HostCheckRequest.builder()
                            .requestId(request.getBatchRequestId() + "_" + hostInfo.getIp())
                            .hostInfo(hostInfo)
                            .pluginIds(request.getPluginIds())
                            .strategy(request.getStrategy())
                            .timeoutMs(request.getTimeoutMs())
                            .retryCount(request.getRetryCount())
                            .failFast(request.isFailFast())
                            .build();
                    
                    // 调用单个主机检查工作流
                    SingleHostCheckWorkflow singleWorkflow = Workflow.newChildWorkflowStub(
                            SingleHostCheckWorkflow.class);
                    return Async.function(singleWorkflow::executeHostCheck, singleRequest);
                })
                .toList();
        
        return promises.stream()
                .map(Promise::get)
                .collect(Collectors.toList());
    }
    
    /**
     * 分批并行执行
     */
    private List<HostCheckResult> executeBatchParallel(BatchCheckRequest request) {
        log.info("分批并行执行批量检查，主机数量: {}, 批大小: {}", 
                request.getHostInfos().size(), request.getMaxConcurrentHosts());
        
        List<HostCheckResult> allResults = new ArrayList<>();
        List<HostInfo> hosts = request.getHostInfos();
        int batchSize = request.getMaxConcurrentHosts();
        
        for (int i = 0; i < hosts.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, hosts.size());
            List<HostInfo> batch = hosts.subList(i, endIndex);
            
            log.info("处理第 {} 批，主机数量: {}", (i / batchSize) + 1, batch.size());
            
            List<Promise<HostCheckResult>> promises = batch.stream()
                    .map(hostInfo -> {
                        HostCheckRequest singleRequest = HostCheckRequest.builder()
                                .requestId(request.getBatchRequestId() + "_" + hostInfo.getIp())
                                .hostInfo(hostInfo)
                                .pluginIds(request.getPluginIds())
                                .strategy(request.getStrategy())
                                .timeoutMs(request.getTimeoutMs())
                                .retryCount(request.getRetryCount())
                                .failFast(request.isFailFast())
                                .build();
                        
                        // 调用单个主机检查工作流
                        SingleHostCheckWorkflow singleWorkflow = Workflow.newChildWorkflowStub(
                                SingleHostCheckWorkflow.class);
                        return Async.function(singleWorkflow::executeHostCheck, singleRequest);
                    })
                    .toList();
            
            List<HostCheckResult> batchResults = promises.stream()
                    .map(Promise::get)
                    .toList();
            
            allResults.addAll(batchResults);
            
            // 更新进度
            progress.setCurrentHost(String.format("批次 %d/%d 完成", 
                    (i / batchSize) + 1, (hosts.size() + batchSize - 1) / batchSize));
        }
        
        return allResults;
    }
    
    /**
     * 串行执行
     */
    private List<HostCheckResult> executeSequential(BatchCheckRequest request) {
        log.info("串行执行批量检查，主机数量: {}", request.getHostInfos().size());
        
        List<HostCheckResult> results = new ArrayList<>();
        
        for (int i = 0; i < request.getHostInfos().size(); i++) {
            HostInfo hostInfo = request.getHostInfos().get(i);
            
            log.info("处理第 {} 台主机: {}", i + 1, hostInfo.getIp());
            
            HostCheckRequest singleRequest = HostCheckRequest.builder()
                    .requestId(request.getBatchRequestId() + "_" + hostInfo.getIp())
                    .hostInfo(hostInfo)
                    .pluginIds(request.getPluginIds())
                    .strategy(request.getStrategy())
                    .timeoutMs(request.getTimeoutMs())
                    .retryCount(request.getRetryCount())
                    .failFast(request.isFailFast())
                    .build();
            
            // 调用单个主机检查工作流
            SingleHostCheckWorkflow singleWorkflow = Workflow.newChildWorkflowStub(
                    SingleHostCheckWorkflow.class);
            HostCheckResult result = singleWorkflow.executeHostCheck(singleRequest);
            results.add(result);
            
            // 更新进度
            progress.setCurrentHost(String.format("%d/%d 完成 - %s", 
                    i + 1, request.getHostInfos().size(), hostInfo.getIp()));
            
            // 如果快速失败且当前主机检查失败
            if (request.isFailFast() && !result.isAllSuccess()) {
                log.warn("快速失败模式，停止后续主机检查");
                break;
            }
        }
        
        return results;
    }
    
    /**
     * 滚动执行
     */
    private List<HostCheckResult> executeRolling(BatchCheckRequest request) {
        log.info("滚动执行批量检查，主机数量: {}", request.getHostInfos().size());
        
        // 滚动执行类似于分批并行，但每批之间有间隔
        List<HostCheckResult> allResults = new ArrayList<>();
        List<HostInfo> hosts = request.getHostInfos();
        int batchSize = Math.max(1, request.getMaxConcurrentHosts() / 2); // 减小批次大小以实现滚动效果
        
        for (int i = 0; i < hosts.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, hosts.size());
            List<HostInfo> batch = hosts.subList(i, endIndex);
            
            log.info("滚动处理第 {} 批，主机数量: {}", (i / batchSize) + 1, batch.size());
            
            List<Promise<HostCheckResult>> promises = batch.stream()
                    .map(hostInfo -> {
                        HostCheckRequest singleRequest = HostCheckRequest.builder()
                                .requestId(request.getBatchRequestId() + "_" + hostInfo.getIp())
                                .hostInfo(hostInfo)
                                .pluginIds(request.getPluginIds())
                                .strategy(request.getStrategy())
                                .timeoutMs(request.getTimeoutMs())
                                .retryCount(request.getRetryCount())
                                .failFast(request.isFailFast())
                                .build();
                        
                        SingleHostCheckWorkflow singleWorkflow = Workflow.newChildWorkflowStub(
                                SingleHostCheckWorkflow.class);
                        return Async.function(singleWorkflow::executeHostCheck, singleRequest);
                    })
                    .toList();
            
            List<HostCheckResult> batchResults = promises.stream()
                    .map(Promise::get)
                    .toList();
            
            allResults.addAll(batchResults);
            
            // 滚动间隔（使用Temporal sleep）
            if (i + batchSize < hosts.size()) {
                Workflow.sleep(Duration.ofSeconds(5)); // 5秒间隔
            }
        }
        
        return allResults;
    }
    
    /**
     * 构建批量检查结果
     */
    private BatchCheckResult buildBatchCheckResult(BatchCheckRequest request, 
                                                 List<HostCheckResult> hostResults, 
                                                 LocalDateTime startTime) {
        
        LocalDateTime endTime = LocalDateTime.now();
        long executionTime = java.time.Duration.between(startTime, endTime).toMillis();
        
        long successCount = hostResults.stream().mapToLong(r -> r.isAllSuccess() ? 1 : 0).sum();
        long failedCount = hostResults.stream().mapToLong(r -> !r.isAllSuccess() ? 1 : 0).sum();
        
        String summary = String.format(
                "批量检查完成: 总计 %d 台主机，成功 %d 台，失败 %d 台，成功率 %.1f%%",
                hostResults.size(), successCount, failedCount,
                hostResults.isEmpty() ? 0.0 : (successCount * 100.0 / hostResults.size()));
        
        return BatchCheckResult.builder()
                .batchRequestId(request.getBatchRequestId())
                .hostResults(hostResults)
                .overallStatus(successCount == hostResults.size() ? WorkflowStatus.COMPLETED : WorkflowStatus.PARTIAL_SUCCESS)
                .startTime(startTime)
                .endTime(endTime)
                .totalExecutionTimeMs(executionTime)
                .totalHostCount(hostResults.size())
                .successHostCount((int) successCount)
                .failedHostCount((int) failedCount)
                .summary(summary)
                .build();
    }
}