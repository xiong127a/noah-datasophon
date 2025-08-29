package com.datasophon.api.hostvalidation.service.impl;

import com.datasophon.api.hostvalidation.executor.HostValidationExecutor;
import com.datasophon.api.hostvalidation.manager.HostValidationStateManager;
import com.datasophon.api.hostvalidation.scheduler.HostValidationSchedulerService;
import com.datasophon.api.hostvalidation.service.HostValidationService;
import com.datasophon.common.dto.HostValidationRequestDTO;
import com.datasophon.common.enums.CheckType;
import com.datasophon.common.enums.ValidationStatus;
import com.datasophon.common.vo.HostValidationStatusVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 主机校验服务实现
 * 重新设计的清晰架构：只负责业务编排，不处理具体执行
 * 
 * 职责：
 * 1. 业务流程编排和任务调度
 * 2. 状态查询和管理
 * 3. 对外API接口
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HostValidationServiceImpl implements HostValidationService {
    
    private final HostValidationStateManager stateManager;
    private final HostValidationSchedulerService schedulerService;
    private final HostValidationExecutor executor;
    
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
        // 委托给执行器处理
        executor.executeValidation(request);
    }
    
    @Override
    public void executeRepair(Long clusterId, String hostIp, CheckType checkType) {
        // 委托给执行器处理
        executor.executeRepair(clusterId, hostIp, checkType);
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
        // 委托给执行器处理
        executor.recheckItem(clusterId, hostIp, checkType);
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
}