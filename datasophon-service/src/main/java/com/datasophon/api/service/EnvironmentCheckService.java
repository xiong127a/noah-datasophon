package com.datasophon.api.service;

import com.datasophon.common.dto.environment.EnvironmentCheckRequest;
import com.datasophon.common.vo.environment.EnvironmentCheckStatusVO;
import com.datasophon.common.vo.environment.EnvironmentValidationResult;
import com.datasophon.common.vo.environment.GlobalCheckResult;
import com.datasophon.common.vo.environment.RepairResult;

import java.util.List;
import java.util.Map;

/**
 * 环境检查服务接口
 * 
 * @author 任相鹏
 * @date 2025-01-23
 */
public interface EnvironmentCheckService {
    
    /**
     * 启动环境检查任务
     * 
     * @param request 检查请求
     * @return 任务ID
     */
    String startEnvironmentCheck(EnvironmentCheckRequest request);
    
    /**
     * 获取检查状态快照
     * 
     * @param clusterId 集群ID
     * @return 检查状态列表
     */
    List<EnvironmentCheckStatusVO> getCheckStatus(Long clusterId);
    
    /**
     * 跳过指定检查项
     * 
     * @param clusterId 集群ID
     * @param hostIp 主机IP
     * @param checkItemKey 检查项键名
     */
    void skipCheckItem(Long clusterId, String hostIp, String checkItemKey);
    
    /**
     * 修复失败的检查项
     * 
     * @param clusterId 集群ID
     * @param hostIp 主机IP
     * @param checkItemKey 检查项键名
     * @param repairParams 修复参数
     * @return 修复结果
     */
    RepairResult repairCheckItem(Long clusterId, String hostIp, String checkItemKey, Map<String, Object> repairParams);
    
    /**
     * 暂停检查
     * 
     * @param clusterId 集群ID
     */
    void pauseCheck(Long clusterId);
    
    /**
     * 恢复检查
     * 
     * @param clusterId 集群ID
     */
    void resumeCheck(Long clusterId);
    
    /**
     * 验证环境检查是否完成，是否可以进入下一步
     * 
     * @param clusterId 集群ID
     * @return 验证结果（包含是否可以进入下一步、原因、统计信息）
     */
    EnvironmentValidationResult validateForNextStep(Long clusterId);
    
    /**
     * 清理环境检查数据
     * 用户点击"上一步"时调用，清理当前步骤的缓存数据
     * 
     * @param clusterId 集群ID
     */
    void cleanupCheckData(Long clusterId);
    
    /**
     * 运行全局检查（在所有主机单个检查完成后）
     * 
     * @param clusterId 集群ID
     * @return 全局检查结果列表
     */
    List<GlobalCheckResult> runGlobalChecks(Long clusterId);
    
    /**
     * 获取全局检查结果
     * 
     * @param clusterId 集群ID
     * @return 全局检查结果列表
     */
    List<GlobalCheckResult> getGlobalCheckResults(Long clusterId);
}

