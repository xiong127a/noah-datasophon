package com.datasophon.api.service;

import com.datasophon.common.dto.host.BatchHostnameChangeRequest;
import com.datasophon.common.dto.host.HostsSyncRequest;

import java.util.Map;

/**
 * 主机管理服务接口
 * 
 * @author 任相鹏
 * @date 2025-01-27
 */
public interface HostManagementService {
    
    /**
     * 预览主机名变更（在实际应用前）
     * 
     * @param request 变更请求
     * @return 预览结果（IP -> 新主机名映射）
     */
    Map<String, String> previewHostnameChanges(BatchHostnameChangeRequest request);
    
    /**
     * 批量修改主机名（异步执行，通过SSE推送进度）
     * 
     * @param request 包含集群ID、主机列表、主机名前缀、后缀格式等
     * @return 任务ID
     */
    String batchChangeHostnames(BatchHostnameChangeRequest request);
    
    /**
     * 同步hosts文件到所有主机（异步执行，通过SSE推送进度）
     * 
     * @param request 包含集群ID、主机IP列表、连接参数
     * @return 任务ID
     */
    String syncHostsFile(HostsSyncRequest request);
    
    /**
     * 获取主机名配置（前缀推荐、格式选项）
     * 
     * @param clusterId 集群ID
     * @return 主机名配置
     */
    Map<String, Object> getHostnameConfig(Long clusterId);
}

