package com.datasophon.api.service;

import com.datasophon.common.vo.agent.AgentDistributionStatusVO;

import java.util.List;
import java.util.Map;

/**
 * Agent分发服务接口
 * 
 * @author DataSophon Team
 * @date 2025-01-24
 */
public interface AgentDistributionService {
    
    /**
     * 开始分发Agent到目标主机
     * 
     * @param clusterId 集群ID
     * @param hostIps 目标主机IP列表
     * @param connectionParams SSH连接参数（包含sshUser, sshPort, sshPassword等）
     * @return 分发任务ID
     */
    String startDistribution(Long clusterId, List<String> hostIps, Map<String, Object> connectionParams);
    
    /**
     * 获取Agent分发状态
     * 
     * @param clusterId 集群ID
     * @return 所有主机的分发状态列表
     */
    List<AgentDistributionStatusVO> getDistributionStatus(Long clusterId);
    
    /**
     * 取消Agent分发
     * 
     * @param clusterId 集群ID
     */
    void cancelDistribution(Long clusterId);
}

