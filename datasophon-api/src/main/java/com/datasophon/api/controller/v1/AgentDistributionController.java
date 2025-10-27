package com.datasophon.api.controller.v1;

import com.datasophon.api.annotation.ClusterId;
import com.datasophon.api.service.AgentDistributionService;
import com.datasophon.common.Result;
import com.datasophon.common.vo.agent.AgentDistributionStatusVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Agent分发Controller
 * 提供Agent分发的启动、状态查询、取消等操作
 * 
 * @author DataSophon Team
 * @date 2025-01-24
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/agent-distribution")
@RequiredArgsConstructor
@Api(tags = "Agent分发管理")
public class AgentDistributionController {
    
    private final AgentDistributionService agentDistributionService;
    
    /**
     * 启动Agent分发
     * 
     * @param clusterId 集群ID
     * @param request 分发请求参数
     * @return 分发任务ID
     */
    @PostMapping("/start")
    @ApiOperation("启动Agent分发")
    public Result<String> startDistribution(
            @ClusterId Long clusterId,
            @RequestBody AgentDistributionRequest request) {
        
        log.info("启动Agent分发: 集群={}, 主机数量={}", clusterId, request.getHostIps().size());
        
        try {
            String taskId = agentDistributionService.startDistribution(
                    clusterId, 
                    request.getHostIps(), 
                    request.getConnectionParams());
            
            return Result.success(taskId);
            
        } catch (Exception e) {
            log.error("启动Agent分发失败: 集群={}, 错误={}", clusterId, e.getMessage(), e);
            return Result.error("启动Agent分发失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取Agent分发状态
     * 
     * @param clusterId 集群ID
     * @return Agent分发状态列表
     */
    @GetMapping("/status")
    @ApiOperation("获取Agent分发状态")
    public Result<List<AgentDistributionStatusVO>> getDistributionStatus(
            @ClusterId Long clusterId) {
        
        log.debug("获取Agent分发状态: 集群={}", clusterId);
        
        try {
            List<AgentDistributionStatusVO> statusList = agentDistributionService.getDistributionStatus(clusterId);
            return Result.success(statusList);
            
        } catch (Exception e) {
            log.error("获取Agent分发状态失败: 集群={}, 错误={}", clusterId, e.getMessage(), e);
            return Result.error("获取Agent分发状态失败: " + e.getMessage());
        }
    }
    
    /**
     * 取消Agent分发
     * 
     * @param clusterId 集群ID
     * @return 操作结果
     */
    @PostMapping("/cancel")
    @ApiOperation("取消Agent分发")
    public Result<String> cancelDistribution(
            @ClusterId Long clusterId) {
        
        log.info("取消Agent分发: 集群={}", clusterId);
        
        try {
            agentDistributionService.cancelDistribution(clusterId);
            return Result.success("Agent分发已取消");
            
        } catch (Exception e) {
            log.error("取消Agent分发失败: 集群={}, 错误={}", clusterId, e.getMessage(), e);
            return Result.error("取消Agent分发失败: " + e.getMessage());
        }
    }
    
    /**
     * Agent分发请求参数
     */
    @lombok.Data
    public static class AgentDistributionRequest {
        /**
         * 目标主机IP列表
         */
        private List<String> hostIps;
        
        /**
         * SSH连接参数
         */
        private Map<String, Object> connectionParams;
    }
}

