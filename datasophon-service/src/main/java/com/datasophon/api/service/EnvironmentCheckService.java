package com.datasophon.api.service;

import com.datasophon.common.dto.environment.EnvironmentCheckRequest;
import com.datasophon.common.vo.environment.EnvironmentCheckStatusVO;
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
}

