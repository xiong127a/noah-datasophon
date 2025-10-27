package com.datasophon.api.controller.v1;

import com.datasophon.api.annotation.ApiVersion;
import com.datasophon.api.annotation.ClusterId;
import com.datasophon.api.checker.util.CheckLogWriter;
import com.datasophon.api.dto.Result;
import com.datasophon.api.service.EnvironmentCheckService;
import com.datasophon.common.dto.environment.EnvironmentCheckRequest;
import com.datasophon.common.dto.environment.RepairCheckItemRequest;
import com.datasophon.common.dto.environment.SkipCheckItemRequest;
import com.datasophon.common.vo.environment.EnvironmentCheckStatusVO;
import com.datasophon.common.vo.environment.EnvironmentValidationResult;
import com.datasophon.common.vo.environment.RepairResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 环境检查控制器
 * 提供环境检查的HTTP接口
 * 
 * @author 任相鹏
 * @date 2025-01-23
 */
@Slf4j
@ApiVersion(path = "environment-check")
public class EnvironmentCheckController {
    
    private final EnvironmentCheckService environmentCheckService;
    
    @Autowired
    private CheckLogWriter checkLogWriter;
    
    @Autowired
    private com.datasophon.api.service.ClusterInfoService clusterInfoService;
    
    @Autowired
    private com.datasophon.api.service.ParcelRepositoryService parcelRepositoryService;
    
    // 构造器日志，确认控制器是否被实例化
    public EnvironmentCheckController(EnvironmentCheckService environmentCheckService) {
        this.environmentCheckService = environmentCheckService;
        log.info("====== EnvironmentCheckController 已初始化 ======");
        log.info("====== API路径应为: /ddh/api/v1/environment-check ======");
    }
    
    /**
     * 启动环境检查
     */
    @PostMapping("/start")
    public Result<String> startCheck(
            @RequestBody EnvironmentCheckRequest request,
            @ClusterId Long clusterId) {
        
        log.info("====== [环境检查控制器] 收到启动请求 ======");
        log.info("====== 集群ID: {}, 主机列表: {} ======", clusterId, request.getHostIps());
        log.info("====== 连接参数: {} ======", request.getConnectionParams());
        
        log.info("启动环境检查: clusterId={}, 主机数量={}", 
                clusterId, request.getHostIps().size());
        
        try {
            // 设置集群ID
            request.setClusterId(clusterId);
            
            // 参数校验
            if (request.getHostIps() == null || request.getHostIps().isEmpty()) {
                return Result.error("主机列表不能为空");
            }
            
            if (request.getConnectionParams() == null) {
                return Result.error("连接参数不能为空");
            }
            
            // 启动检查
            var taskId = environmentCheckService.startEnvironmentCheck(request);
            
            return new Result<>(200, "环境检查任务已启动，请通过SSE接收实时状态更新", taskId);
            
        } catch (Exception e) {
            log.error("启动环境检查失败: clusterId={}, error={}", 
                    clusterId, e.getMessage(), e);
            return Result.error("启动环境检查失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取检查状态
     */
    @GetMapping("/status")
    public Result<List<EnvironmentCheckStatusVO>> getStatus(@ClusterId Long clusterId) {
        
        try {
            var statuses = environmentCheckService.getCheckStatus(clusterId);
            return Result.success(statuses);
            
        } catch (Exception e) {
            log.error("获取检查状态失败: clusterId={}, error={}", clusterId, e.getMessage(), e);
            return Result.error("获取检查状态失败: " + e.getMessage());
        }
    }
    
    /**
     * 跳过检查项
     */
    @PostMapping("/skip")
    public Result<String> skipItem(
            @RequestBody SkipCheckItemRequest request,
            @ClusterId Long clusterId) {
        
        log.info("跳过检查项: clusterId={}, hostIp={}, checkItemKey={}", 
                clusterId, request.getHostIp(), request.getCheckItemKey());
        
        try {
            environmentCheckService.skipCheckItem(
                    clusterId, 
                    request.getHostIp(), 
                    request.getCheckItemKey());
            
            return Result.success("检查项已跳过");
            
        } catch (Exception e) {
            log.error("跳过检查项失败: {}", e.getMessage(), e);
            return Result.error("跳过检查项失败: " + e.getMessage());
        }
    }
    
    /**
     * 修复检查项
     */
    @PostMapping("/repair")
    public Result<RepairResult> repairItem(
            @RequestBody RepairCheckItemRequest request,
            @ClusterId Long clusterId) {
        
        log.info("修复检查项: clusterId={}, hostIp={}, checkItemKey={}", 
                clusterId, request.getHostIp(), request.getCheckItemKey());
        
        try {
            var result = environmentCheckService.repairCheckItem(
                    clusterId,
                    request.getHostIp(),
                    request.getCheckItemKey(),
                    request.getRepairParams());
            
            return Result.success(result);
            
        } catch (Exception e) {
            log.error("修复检查项失败: {}", e.getMessage(), e);
            return Result.error("修复检查项失败: " + e.getMessage());
        }
    }
    
    /**
     * 暂停检查
     */
    @PostMapping("/pause")
    public Result<String> pauseCheck(@ClusterId Long clusterId) {
        
        log.info("暂停环境检查: clusterId={}", clusterId);
        
        try {
            environmentCheckService.pauseCheck(clusterId);
            return Result.success("检查已暂停");
            
        } catch (Exception e) {
            log.error("暂停检查失败: {}", e.getMessage(), e);
            return Result.error("暂停检查失败: " + e.getMessage());
        }
    }
    
    /**
     * 恢复检查
     */
    @PostMapping("/resume")
    public Result<String> resumeCheck(@ClusterId Long clusterId) {
        
        log.info("恢复环境检查: clusterId={}", clusterId);
        
        try {
            environmentCheckService.resumeCheck(clusterId);
            return Result.success("检查已恢复");
            
        } catch (Exception e) {
            log.error("恢复检查失败: {}", e.getMessage(), e);
            return Result.error("恢复检查失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取检查和修复日志
     */
    @GetMapping("/logs/{hostIp}/{checkKey}")
    public Result<Map<String, String>> getLogs(
            @PathVariable String hostIp,
            @PathVariable String checkKey,
            @ClusterId Long clusterId) {
        
        log.info("获取日志: clusterId={}, hostIp={}, checkKey={}", clusterId, hostIp, checkKey);
        
        try {
            var checkLog = checkLogWriter.readCheckLog(clusterId, hostIp, checkKey);
            var repairLog = checkLogWriter.readRepairLog(clusterId, hostIp, checkKey);
            
            var logs = new HashMap<String, String>();
            logs.put("checkLog", checkLog != null ? checkLog : "暂无检查日志");
            logs.put("repairLog", repairLog != null ? repairLog : "暂无修复日志");
            
            return Result.success(logs);
            
        } catch (Exception e) {
            log.error("获取日志失败: {}", e.getMessage(), e);
            return Result.error("获取日志失败: " + e.getMessage());
        }
    }
    
    /**
     * 验证环境检查是否完成
     * 用于判断是否可以进入下一步
     */
    @GetMapping("/validation")
    public Result<EnvironmentValidationResult> validateForNextStep(
            @ClusterId Long clusterId) {
        
        log.info("验证环境检查是否完成: clusterId={}", clusterId);
        
        try {
            var result = environmentCheckService.validateForNextStep(clusterId);
            return Result.success(result);
        } catch (Exception e) {
            log.error("验证环境检查失败: clusterId={}, error={}", 
                clusterId, e.getMessage(), e);
            return Result.error("验证失败: " + e.getMessage());
        }
    }
    
    /**
     * 清理环境检查数据
     * 用户点击"上一步"时调用，清理当前步骤的缓存数据
     */
    @PostMapping("/cleanup")
    public Result<String> cleanupCheckData(@ClusterId Long clusterId) {
        
        log.info("清理环境检查数据: clusterId={}", clusterId);
        
        try {
            environmentCheckService.cleanupCheckData(clusterId);
            return Result.success("检查数据已清理");
        } catch (Exception e) {
            log.error("清理检查数据失败: clusterId={}, error={}", 
                clusterId, e.getMessage(), e);
            return Result.error("清理失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取存储库中的JDK文件列表
     * 用于环境修复时让用户选择JDK版本
     */
    @GetMapping("/jdk-files")
    public Result<List<String>> getJdkFiles(@ClusterId Long clusterId) {
        
        log.info("获取JDK文件列表: clusterId={}", clusterId);
        
        try {
            // 获取集群关联的存储库
            var cluster = clusterInfoService.getById(clusterId);
            if (cluster == null) {
                return Result.error("集群不存在");
            }
            
            Long repositoryId = cluster.getRepositoryId();
            if (repositoryId == null) {
                return Result.error("集群未关联存储库");
            }
            
            // 获取存储库中的JDK文件列表
            var jdkFiles = parcelRepositoryService.listJdkFiles(repositoryId);
            
            log.info("找到 {} 个JDK文件", jdkFiles.size());
            return Result.success(jdkFiles);
            
        } catch (Exception e) {
            log.error("获取JDK文件列表失败: clusterId={}, error={}", 
                clusterId, e.getMessage(), e);
            return Result.error("获取失败: " + e.getMessage());
        }
    }
}

