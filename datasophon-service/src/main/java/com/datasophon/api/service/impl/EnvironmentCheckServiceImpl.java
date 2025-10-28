package com.datasophon.api.service.impl;

import com.datasophon.api.checker.CheckOrchestrator;
import com.datasophon.api.checker.CheckStateManager;
import com.datasophon.api.checker.EnvironmentCheckItem;
import com.datasophon.api.checker.GlobalCheckItem;
import com.datasophon.api.checker.HostCheckContext;
import com.datasophon.api.checker.util.CheckLogWriter;
import com.datasophon.api.event.RepairCompleteEvent;
import com.datasophon.api.service.EnvironmentCheckService;
import com.datasophon.common.dto.environment.EnvironmentCheckRequest;
import com.datasophon.common.enums.CheckItemStatus;
import com.datasophon.common.vo.environment.EnvironmentCheckStatusVO;
import com.datasophon.common.vo.environment.EnvironmentValidationResult;
import com.datasophon.common.vo.environment.GlobalCheckResult;
import com.datasophon.common.vo.environment.RepairResult;
import com.datasophon.common.vo.environment.ValidationSummary;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 环境检查服务实现
 * 
 * @author 任相鹏
 * @date 2025-01-23
 */
@Slf4j
@Service
public class EnvironmentCheckServiceImpl implements EnvironmentCheckService {
    
    @Autowired
    private CheckOrchestrator checkOrchestrator;
    
    @Autowired
    private CheckStateManager stateManager;
    
    @Autowired
    private List<EnvironmentCheckItem> checkItems;
    
    @Autowired(required = false)
    private List<GlobalCheckItem> globalCheckItems = new ArrayList<>();
    
    @Autowired
    private CheckLogWriter checkLogWriter;
    
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    // 存储全局检查结果：clusterId -> List<GlobalCheckResult>
    private final Map<Long, List<GlobalCheckResult>> globalCheckResultsCache = new java.util.concurrent.ConcurrentHashMap<>();
    
    @Override
    public String startEnvironmentCheck(EnvironmentCheckRequest request) {
        var clusterId = request.getClusterId();
        var hostIps = request.getHostIps();
        var connectionParams = request.getConnectionParams();
        
        log.info("====== [环境检查服务] 开始执行 ======");
        log.info("====== 集群ID: {}, 主机列表: {} ======", clusterId, hostIps);
        
        log.info("启动环境检查: 集群={}, 主机数={}", clusterId, hostIps.size());
        
        // 保存连接参数，供后续修复使用
        stateManager.saveConnectionParams(clusterId, connectionParams);
        
        // 生成任务ID
        var taskId = UUID.randomUUID().toString();
        
        // 异步执行检查（不阻塞）
        checkOrchestrator.checkAllHosts(hostIps, connectionParams, clusterId, stateManager)
                .whenComplete((results, error) -> {
                    if (error != null) {
                        log.error("环境检查失败: 集群={}, 错误={}", clusterId, error.getMessage(), error);
                    } else {
                        log.info("环境检查完成: 集群={}, 主机数={}", clusterId, results.size());
                        
                        // 所有主机检查完成后，自动执行全局检查
                        log.info("所有主机检查完成，开始自动执行全局检查...");
                        try {
                            runGlobalChecks(clusterId);
                            log.info("全局检查自动执行完成");
                        } catch (Exception e) {
                            log.error("自动执行全局检查失败: {}", e.getMessage(), e);
                        }
                    }
                });
        
        return taskId;
    }
    
    @Override
    public List<EnvironmentCheckStatusVO> getCheckStatus(Long clusterId) {
        return stateManager.getClusterStatus(clusterId);
    }
    
    @Override
    public void skipCheckItem(Long clusterId, String hostIp, String checkItemKey) {
        log.info("跳过检查项: 集群={}, 主机={}, 检查项={}", clusterId, hostIp, checkItemKey);
        // markItemSkipped会自动发布状态变更事件，触发SSE推送
        stateManager.markItemSkipped(clusterId, hostIp, checkItemKey);
        log.info("跳过成功: clusterId={}, hostIp={}, checkItemKey={}", clusterId, hostIp, checkItemKey);
    }
    
    @Override
    public RepairResult repairCheckItem(Long clusterId, String hostIp, String checkItemKey, Map<String, Object> repairParams) {
        log.info("修复检查项: 集群={}, 主机={}, 检查项={}", clusterId, hostIp, checkItemKey);
        
        // 清空旧的修复日志（每次修复前清空，只保留最新一次）
        checkLogWriter.clearRepairLogs(clusterId, hostIp, checkItemKey);
        
        try {
            // 查找对应的检查器
            var checker = checkItems.stream()
                    .filter(item -> item.getCheckKey().equals(checkItemKey))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("未找到检查项: " + checkItemKey));
            
            // 获取主机状态，构建上下文
            var hostStatus = stateManager.getHostStatus(clusterId, hostIp);
            if (hostStatus == null) {
                return RepairResult.builder()
                        .success(false)
                        .message("未找到主机状态")
                        .build();
            }
            
            // 从状态管理器获取连接参数
            var connectionParams = stateManager.getConnectionParams(clusterId);
            if (connectionParams == null) {
                return RepairResult.builder()
                        .success(false)
                        .message("未找到集群连接参数，请重新执行环境检查")
                        .build();
            }
            
            // 构建完整的检查上下文（包含SSH认证信息）
            var context = HostCheckContext.builder()
                    .clusterId(clusterId)
                    .hostIp(hostIp)
                    .sshUser((String) connectionParams.get("sshUser"))
                    .sshPort(Integer.parseInt((String) connectionParams.get("sshPort")))
                    .sshPassword((String) connectionParams.get("sshPassword"))
                    .connectionParams(connectionParams)
                    .build();
            
            // 执行修复
            var repairResult = checker.repair(context, repairParams);
            
            // 如果修复成功，重新执行检查
            if (repairResult.getSuccess() != null && repairResult.getSuccess()) {
                log.info("修复成功，重新执行检查: {}", checkItemKey);
                var checkResult = checker.execute(context);
                
                // 更新检查状态
                var itemVO = hostStatus.getCheckItems().stream()
                        .filter(item -> item.getCheckKey().equals(checkItemKey))
                        .findFirst()
                        .orElse(null);
                
                if (itemVO != null) {
                    itemVO.setStatus(checkResult.getStatus());
                    itemVO.setMessage(checkResult.getMessage());
                    repairResult.setUpdatedStatus(itemVO);
                    stateManager.updateCheckItemStatus(clusterId, hostIp, checkItemKey, itemVO);
                }
            }
            
            // 发布修复完成事件（由SSE控制器监听并推送）
            eventPublisher.publishEvent(new RepairCompleteEvent(
                    this, clusterId, hostIp, checkItemKey,
                    repairResult.getSuccess() != null && repairResult.getSuccess(),
                    repairResult.getMessage() != null ? repairResult.getMessage() : "修复完成"));
            
            return repairResult;
            
        } catch (Exception e) {
            log.error("修复检查项失败: {}", e.getMessage(), e);
            
            // 发布修复失败事件
            eventPublisher.publishEvent(new RepairCompleteEvent(
                    this, clusterId, hostIp, checkItemKey, false, "修复失败: " + e.getMessage()));
            
            return RepairResult.builder()
                    .success(false)
                    .message("修复失败: " + e.getMessage())
                    .build();
        }
    }
    
    @Override
    public void pauseCheck(Long clusterId) {
        log.info("暂停环境检查: 集群={}", clusterId);
        stateManager.pauseCheck(clusterId);
    }
    
    @Override
    public void resumeCheck(Long clusterId) {
        log.info("恢复环境检查: 集群={}", clusterId);
        stateManager.resumeCheck(clusterId);
    }
    
    @Override
    public EnvironmentValidationResult validateForNextStep(Long clusterId) {
        log.info("验证环境检查是否完成: 集群={}", clusterId);
        
        // 获取所有主机的检查状态
        List<EnvironmentCheckStatusVO> statuses = getCheckStatus(clusterId);
        
        if (statuses == null || statuses.isEmpty()) {
            return EnvironmentValidationResult.builder()
                    .canProceed(false)
                    .reason("未找到环境检查状态")
                    .summary(ValidationSummary.builder()
                            .totalHosts(0)
                            .completedHosts(0)
                            .totalItems(0)
                            .successItems(0)
                            .failedItems(0)
                            .skippedItems(0)
                            .failedHostIps(List.of())
                            .build())
                    .build();
        }
        
        // 统计信息
        int totalHosts = statuses.size();
        int totalItems = 0;
        int successItems = 0;
        int failedItems = 0;
        int skippedItems = 0;
        
        // 业务规则：所有主机的所有检查项都必须是SUCCESS或SKIPPED
        boolean canProceed = true;
        List<String> failedHostIps = statuses.stream()
                .filter(host -> {
                    // 修复：使用枚举比较而不是字符串比较
                    boolean hasFailed = host.getCheckItems().stream()
                            .anyMatch(item -> item.getStatus() == CheckItemStatus.FAILED);
                    return hasFailed;
                })
                .map(EnvironmentCheckStatusVO::getHostIp)
                .collect(Collectors.toList());
        
        // 如果有失败的主机，则不能进入下一步
        if (!failedHostIps.isEmpty()) {
            canProceed = false;
        }
        
        // 计算统计数据
        for (EnvironmentCheckStatusVO host : statuses) {
            totalItems += host.getTotalItems();
            successItems += host.getSuccessItems();
            failedItems += host.getFailedItems();
            skippedItems += host.getSkippedItems();
        }
        
        // 已完成的主机数（所有检查项都是SUCCESS或SKIPPED）
        int completedHosts = (int) statuses.stream()
                .filter(host -> host.getCheckItems().stream()
                        // 修复：使用枚举比较而不是字符串比较
                        .allMatch(item -> item.getStatus() == CheckItemStatus.SUCCESS || 
                                        item.getStatus() == CheckItemStatus.SKIPPED))
                .count();
        
        // 构建验证结果
        ValidationSummary summary = ValidationSummary.builder()
                .totalHosts(totalHosts)
                .completedHosts(completedHosts)
                .totalItems(totalItems)
                .successItems(successItems)
                .failedItems(failedItems)
                .skippedItems(skippedItems)
                .failedHostIps(failedHostIps)
                .build();
        
        String reason = null;
        if (!canProceed) {
            if (!failedHostIps.isEmpty()) {
                reason = String.format("还有 %d 台主机存在失败的检查项: %s", 
                        failedHostIps.size(), 
                        String.join(", ", failedHostIps));
            } else {
                reason = "存在未完成的检查项";
            }
        }
        
        EnvironmentValidationResult result = EnvironmentValidationResult.builder()
                .canProceed(canProceed)
                .reason(reason)
                .summary(summary)
                .build();
        
        log.info("验证结果: canProceed={}, reason={}, 完成主机={}/{}, 成功项={}, 失败项={}, 跳过项={}", 
                canProceed, reason, completedHosts, totalHosts, successItems, failedItems, skippedItems);
        
        return result;
    }
    
    @Override
    public void cleanupCheckData(Long clusterId) {
        log.info("清理环境检查数据: 集群={}", clusterId);
        
        // 清理状态管理器中的所有数据
        stateManager.clearClusterState(clusterId);
        
        // 清理全局检查结果缓存
        globalCheckResultsCache.remove(clusterId);
        
        log.info("环境检查数据已清理: 集群={}", clusterId);
    }
    
    @Override
    public List<GlobalCheckResult> runGlobalChecks(Long clusterId) {
        log.info("开始运行全局检查: 集群={}", clusterId);
        
        try {
            // 1. 获取所有主机信息（从单主机检查结果中）
            List<GlobalCheckItem.HostInfo> hosts = collectHostInfoFromCheckResults(clusterId);
            
            if (hosts.isEmpty()) {
                log.warn("没有找到主机信息，跳过全局检查: 集群={}", clusterId);
                return Collections.emptyList();
            }
            
            // 2. 获取连接参数
            Map<String, Object> connectionParams = stateManager.getConnectionParams(clusterId);
            if (connectionParams == null) {
                log.warn("没有找到连接参数，跳过全局检查: 集群={}", clusterId);
                return Collections.emptyList();
            }
            
            // 3. 按优先级执行所有全局检查器
            List<GlobalCheckResult> results = globalCheckItems.stream()
                    .filter(GlobalCheckItem::isEnabled)
                    .sorted(Comparator.comparingInt(GlobalCheckItem::getPriority))
                    .map(checker -> {
                        try {
                            log.info("执行全局检查: checkKey={}, displayName={}", 
                                    checker.getCheckKey(), checker.getDisplayName());
                            return checker.execute(hosts, clusterId, connectionParams);
                        } catch (Exception e) {
                            log.error("全局检查执行失败: checkKey={}, error={}", 
                                    checker.getCheckKey(), e.getMessage(), e);
                            return GlobalCheckResult.builder()
                                    .checkKey(checker.getCheckKey())
                                    .displayName(checker.getDisplayName())
                                    .status(CheckItemStatus.FAILED)
                                    .message("检查执行失败: " + e.getMessage())
                                    .recommendation("请检查系统状态")
                                    .details(Map.of("error", e.getMessage()))
                                    .timestamp(System.currentTimeMillis())
                                    .build();
                        }
                    })
                    .collect(Collectors.toList());
            
            // 4. 保存全局检查结果到缓存
            globalCheckResultsCache.put(clusterId, results);
            
            log.info("全局检查完成: 集群={}, 检查项数={}", clusterId, results.size());
            return results;
            
        } catch (Exception e) {
            log.error("运行全局检查失败: 集群={}, error={}", clusterId, e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    @Override
    public List<GlobalCheckResult> getGlobalCheckResults(Long clusterId) {
        log.info("获取全局检查结果: 集群={}", clusterId);
        return globalCheckResultsCache.getOrDefault(clusterId, Collections.emptyList());
    }
    
    /**
     * 从检查结果中收集主机信息
     */
    private List<GlobalCheckItem.HostInfo> collectHostInfoFromCheckResults(Long clusterId) {
        List<EnvironmentCheckStatusVO> statuses = getCheckStatus(clusterId);
        Map<String, Map<String, Object>> allHostInfo = stateManager.getAllHostInfo(clusterId);
        
        return statuses.stream()
                .map(status -> {
                    String ip = status.getHostIp();
                    Map<String, Object> info = allHostInfo.get(ip);
                    
                    // 从缓存中获取主机名和其他信息
                    String hostname = status.getHostname();
                    Map<String, Object> additionalInfo = new HashMap<>();
                    
                    if (info != null) {
                        // 更新主机名
                        if (info.containsKey("hostname")) {
                            hostname = (String) info.get("hostname");
                        }
                        // 存储所有额外信息（包括hostsContent等）
                        additionalInfo.putAll(info);
                    }
                    
                    return new GlobalCheckItem.HostInfo(ip, hostname, additionalInfo);
                })
                .collect(Collectors.toList());
    }
}

