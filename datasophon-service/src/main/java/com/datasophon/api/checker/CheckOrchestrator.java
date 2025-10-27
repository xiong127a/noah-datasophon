package com.datasophon.api.checker;

import com.datasophon.common.enums.CheckItemStatus;
import com.datasophon.common.enums.HostCheckStatus;
import com.datasophon.common.vo.environment.CheckItemStatusVO;
import com.datasophon.common.vo.environment.EnvironmentCheckStatusVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * 检查编排器
 * 负责并发检查多台主机，每台主机内检查项按优先级顺序执行
 * 使用JDK 21虚拟线程优化并发性能
 * 
 * @author 任相鹏
 * @date 2025-01-23
 */
@Slf4j
@Component
public class CheckOrchestrator {
    
    @Value("${datasophon.checker.validation-flow.max-concurrent-hosts:10}")
    private int maxConcurrentHosts;
    
    @Autowired
    private List<EnvironmentCheckItem> checkItems;
    
    // 使用虚拟线程池（JDK 21特性）
    private final ExecutorService virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
    
    // 限制并发主机数的信号量
    private Semaphore hostConcurrencyLimit;
    
    /**
     * 检查所有主机
     * 
     * @param hostIps 主机IP列表
     * @param connectionParams SSH连接参数
     * @param clusterId 集群ID
     * @param stateManager 状态管理器（用于实时更新状态）
     * @return 检查结果
     */
    public CompletableFuture<List<EnvironmentCheckStatusVO>> checkAllHosts(
            List<String> hostIps,
            Map<String, Object> connectionParams,
            Long clusterId,
            CheckStateManager stateManager) {
        
        log.info("开始并发检查 {} 台主机的环境，集群ID: {}", hostIps.size(), clusterId);
        
        // 初始化信号量
        if (hostConcurrencyLimit == null) {
            hostConcurrencyLimit = new Semaphore(maxConcurrentHosts);
        }
        
        // 并发检查所有主机
        var futures = hostIps.stream()
                .map(hostIp -> checkSingleHost(hostIp, connectionParams, clusterId, stateManager))
                .toList();
        
        // 等待所有主机检查完成
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList()));
    }
    
    /**
     * 检查单台主机
     * 
     * @param hostIp 主机IP
     * @param connectionParams 连接参数
     * @param clusterId 集群ID
     * @param stateManager 状态管理器
     * @return 主机检查结果
     */
    private CompletableFuture<EnvironmentCheckStatusVO> checkSingleHost(
            String hostIp,
            Map<String, Object> connectionParams,
            Long clusterId,
            CheckStateManager stateManager) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 获取信号量，控制并发数
                hostConcurrencyLimit.acquire();
                log.info("开始检查主机: {}", hostIp);
                
                // 构建检查上下文
                var context = buildContext(hostIp, connectionParams, clusterId);
                
                // 初始化主机状态
                var statusVO = initHostStatus(hostIp);
                stateManager.updateHostStatus(clusterId, hostIp, statusVO);
                
                // 获取启用的检查项并按优先级排序
                var enabledCheckers = checkItems.stream()
                        .filter(EnvironmentCheckItem::isEnabled)
                        .sorted(Comparator.comparingInt(EnvironmentCheckItem::getPriority))
                        .toList();
                
                log.info("主机 {} 共有 {} 个检查项", hostIp, enabledCheckers.size());
                
                // 顺序执行检查项
                var checkItemResults = new ArrayList<CheckItemStatusVO>();
                for (var checker : enabledCheckers) {
                    var itemResult = executeCheckItem(checker, context, clusterId, hostIp, stateManager);
                    checkItemResults.add(itemResult);
                    
                    // TODO: 如果SSH连接失败，快速失败，跳过后续检查
                }
                
                // 计算主机整体状态
                statusVO.setCheckItems(checkItemResults);
                statusVO.setCompletedItems(checkItemResults.size());
                statusVO.setSuccessItems((int) checkItemResults.stream()
                        .filter(item -> item.getStatus() == CheckItemStatus.SUCCESS)
                        .count());
                statusVO.setFailedItems((int) checkItemResults.stream()
                        .filter(item -> item.getStatus() == CheckItemStatus.FAILED)
                        .count());
                statusVO.setSkippedItems((int) checkItemResults.stream()
                        .filter(item -> item.getStatus() == CheckItemStatus.SKIPPED)
                        .count());
                statusVO.setEndTime(System.currentTimeMillis());
                
                // 确定整体状态
                statusVO.setOverallStatus(determineOverallStatus(statusVO));
                
                // 收集主机信息（主机名和hosts文件）用于后续全局检查
                collectHostInfo(context, clusterId, hostIp, stateManager);
                
                stateManager.updateHostStatus(clusterId, hostIp, statusVO);
                log.info("主机 {} 检查完成，整体状态: {}", hostIp, statusVO.getOverallStatus());
                
                return statusVO;
                
            } catch (InterruptedException e) {
                log.error("主机 {} 检查被中断", hostIp, e);
                Thread.currentThread().interrupt();
                return createErrorStatus(hostIp, "检查被中断");
            } catch (Exception e) {
                log.error("主机 {} 检查异常", hostIp, e);
                return createErrorStatus(hostIp, e.getMessage());
            } finally {
                hostConcurrencyLimit.release();
            }
        }, virtualThreadExecutor);
    }
    
    /**
     * 执行单个检查项
     */
    private CheckItemStatusVO executeCheckItem(
            EnvironmentCheckItem checker,
            HostCheckContext context,
            Long clusterId,
            String hostIp,
            CheckStateManager stateManager) {
        
        var itemVO = CheckItemStatusVO.builder()
                .checkKey(checker.getCheckKey())
                .displayName(checker.getDisplayName())
                .priority(checker.getPriority())
                .status(CheckItemStatus.RUNNING)
                .startTime(System.currentTimeMillis())
                .build();
        
        // 更新状态为运行中
        stateManager.updateCheckItemStatus(clusterId, hostIp, checker.getCheckKey(), itemVO);
        
        try {
            // 执行检查
            var result = checker.execute(context);
            
            // 填充结果
            itemVO.setStatus(result.getStatus());
            itemVO.setMessage(result.getMessage());
            itemVO.setRecommendation(result.getRecommendation());
            itemVO.setCanSkip(result.getCanSkip() != null ? result.getCanSkip() : false);
            itemVO.setCanRepair(result.getCanRepair() != null ? result.getCanRepair() : false);
            itemVO.setCheckResult(result.getDetails());
            itemVO.setEndTime(System.currentTimeMillis());
            
        } catch (Exception e) {
            log.error("检查项 {} 执行异常: {}", checker.getCheckKey(), e.getMessage(), e);
            itemVO.setStatus(CheckItemStatus.FAILED);
            itemVO.setMessage("检查异常: " + e.getMessage());
            itemVO.setCanSkip(false);
            itemVO.setCanRepair(false);
            itemVO.setEndTime(System.currentTimeMillis());
        }
        
        // 更新最终状态
        stateManager.updateCheckItemStatus(clusterId, hostIp, checker.getCheckKey(), itemVO);
        
        return itemVO;
    }
    
    /**
     * 构建检查上下文
     */
    private HostCheckContext buildContext(String hostIp, Map<String, Object> connectionParams, Long clusterId) {
        return HostCheckContext.builder()
                .clusterId(clusterId)
                .hostIp(hostIp)
                .sshUser((String) connectionParams.get("sshUser"))
                .sshPort(Integer.parseInt((String) connectionParams.get("sshPort")))
                .sshPassword((String) connectionParams.get("sshPassword"))
                .connectionParams(connectionParams)
                .build();
    }
    
    /**
     * 初始化主机状态
     */
    private EnvironmentCheckStatusVO initHostStatus(String hostIp) {
        return EnvironmentCheckStatusVO.builder()
                .hostIp(hostIp)
                .hostname(hostIp)
                .overallStatus(HostCheckStatus.RUNNING)
                .checkItems(new ArrayList<>())
                .totalItems(checkItems.size())
                .completedItems(0)
                .successItems(0)
                .failedItems(0)
                .skippedItems(0)
                .startTime(System.currentTimeMillis())
                .build();
    }
    
    /**
     * 确定主机整体状态
     */
    private HostCheckStatus determineOverallStatus(EnvironmentCheckStatusVO statusVO) {
        if (statusVO.getFailedItems() > 0 && statusVO.getSkippedItems() == 0) {
            return HostCheckStatus.FAILED;
        } else if (statusVO.getFailedItems() > 0 && statusVO.getSkippedItems() > 0) {
            return HostCheckStatus.PARTIAL_SUCCESS;
        } else if (statusVO.getSuccessItems() + statusVO.getSkippedItems() == statusVO.getTotalItems()) {
            return HostCheckStatus.SUCCESS;
        } else {
            return HostCheckStatus.RUNNING;
        }
    }
    
    /**
     * 创建错误状态
     */
    private EnvironmentCheckStatusVO createErrorStatus(String hostIp, String errorMessage) {
        return EnvironmentCheckStatusVO.builder()
                .hostIp(hostIp)
                .hostname(hostIp)
                .overallStatus(HostCheckStatus.FAILED)
                .errorMessage(errorMessage)
                .checkItems(new ArrayList<>())
                .totalItems(0)
                .completedItems(0)
                .successItems(0)
                .failedItems(0)
                .skippedItems(0)
                .startTime(System.currentTimeMillis())
                .endTime(System.currentTimeMillis())
                .build();
    }
    
    /**
     * 收集主机信息（主机名和hosts文件内容）
     */
    private void collectHostInfo(HostCheckContext context, Long clusterId, String hostIp, 
                                 CheckStateManager stateManager) {
        try {
            log.info("开始收集主机 {} 的信息（主机名和hosts文件）", hostIp);
            
            var pluginContext = toPluginContext(context);
            
            // 获取主机名
            var hostnameResult = sshService.executeCommand(pluginContext, "hostname");
            String hostname = hostnameResult.isSuccess() ? hostnameResult.output().trim() : null;
            
            // 获取hosts文件内容
            var hostsResult = sshService.executeCommand(pluginContext, "cat /etc/hosts");
            String hostsContent = hostsResult.isSuccess() ? hostsResult.output() : null;
            
            if (hostname != null || hostsContent != null) {
                // 存储到缓存
                Map<String, Object> hostInfo = new HashMap<>();
                hostInfo.put("hostIp", hostIp);
                if (hostname != null) {
                    hostInfo.put("hostname", hostname);
                }
                if (hostsContent != null) {
                    hostInfo.put("hostsContent", hostsContent);
                }
                hostInfo.put("timestamp", System.currentTimeMillis());
                
                stateManager.storeHostInfo(clusterId, hostIp, hostInfo);
                log.info("成功收集主机 {} 的信息: hostname={}", hostIp, hostname);
            }
            
        } catch (Exception e) {
            log.warn("收集主机 {} 信息失败（不影响检查）: {}", hostIp, e.getMessage());
        }
    }
}

