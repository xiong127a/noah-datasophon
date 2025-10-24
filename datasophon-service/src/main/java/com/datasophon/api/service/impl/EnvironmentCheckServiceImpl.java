package com.datasophon.api.service.impl;

import com.datasophon.api.checker.CheckOrchestrator;
import com.datasophon.api.checker.CheckStateManager;
import com.datasophon.api.checker.EnvironmentCheckItem;
import com.datasophon.api.checker.HostCheckContext;
import com.datasophon.api.checker.util.CheckLogWriter;
import com.datasophon.api.service.EnvironmentCheckService;
import com.datasophon.common.dto.environment.EnvironmentCheckRequest;
import com.datasophon.common.vo.environment.EnvironmentCheckStatusVO;
import com.datasophon.common.vo.environment.RepairResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

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
    
    @Autowired
    private CheckLogWriter checkLogWriter;
    
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
        stateManager.markItemSkipped(clusterId, hostIp, checkItemKey);
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
            
            return repairResult;
            
        } catch (Exception e) {
            log.error("修复检查项失败: {}", e.getMessage(), e);
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
}

