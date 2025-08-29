package com.datasophon.api.hostvalidation.service;

import com.datasophon.common.dto.HostValidationRequestDTO;
import com.datasophon.common.enums.CheckType;
import com.datasophon.common.vo.HostValidationStatusVO;

import java.util.List;

/**
 * 主机校验服务接口
 * 提供主机校验、修复和状态管理功能
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-28
 */
public interface HostValidationService {
    
    /**
     * 启动主机校验
     * 
     * @param request 校验请求
     */
    void startValidation(HostValidationRequestDTO request);
    
    /**
     * 获取校验状态
     * 
     * @param clusterId 集群ID
     * @return 主机校验状态列表
     */
    List<HostValidationStatusVO> getValidationStatus(Long clusterId);
    
    /**
     * 修复失败的检查项
     * 
     * @param clusterId 集群ID
     * @param hostIps 需要修复的主机IP列表
     */
    void repairFailedChecks(Long clusterId, List<String> hostIps);
    
    /**
     * 停止校验任务
     * 
     * @param clusterId 集群ID
     */
    void stopValidation(Long clusterId);

    /**
     * 暂停主机校验
     *
     * @param clusterId 集群ID
     * @param hostIp 主机IP，为空则暂停所有主机
     */
    void pauseValidation(Long clusterId, String hostIp);

    /**
     * 继续主机校验
     *
     * @param clusterId 集群ID
     * @param hostIp 主机IP，为空则继续所有主机
     */
    void resumeValidation(Long clusterId, String hostIp);

    /**
     * 重新检查指定项目
     *
     * @param clusterId 集群ID
     * @param hostIp 主机IP
     * @param checkType 检查类型
     */
    void recheckItem(Long clusterId, String hostIp, CheckType checkType);

    /**
     * 启动主机修复
     *
     * @param clusterId 集群ID
     * @param hostIp 主机IP
     * @param checkType 检查类型
     */
    void startRepair(Long clusterId, String hostIp, CheckType checkType);
    
    // ==================== 调度器专用方法 ====================
    
    /**
     * 执行主机校验 - 供调度器调用
     * 此方法由db-scheduler调度执行，不直接暴露给外部API
     * 
     * @param request 校验请求
     */
    void executeValidation(HostValidationRequestDTO request);
    
    /**
     * 执行主机修复 - 供调度器调用 
     * 此方法由db-scheduler调度执行，不直接暴露给外部API
     *
     * @param clusterId 集群ID
     * @param hostIp 主机IP
     * @param checkType 检查类型
     */
    void executeRepair(Long clusterId, String hostIp, CheckType checkType);
}
