package com.datasophon.api.service.impl;

import com.datasophon.api.agent.AgentDistributionContext;
import com.datasophon.api.agent.AgentDistributionStep;
import com.datasophon.api.agent.AgentStateManager;
import com.datasophon.api.agent.steps.*;
import com.datasophon.api.agent.util.AgentLogWriter;
import com.datasophon.api.load.ConfigBean;
import com.datasophon.api.repository.RepositoryDownloaderFactory;
import com.datasophon.api.service.AgentDistributionService;
import com.datasophon.api.service.ParcelRepositoryService;
import com.datasophon.common.Constants;
import com.datasophon.common.dto.ParcelRepositoryDTO;
import com.datasophon.common.vo.agent.AgentDistributionStatusVO;
import com.datasophon.dao.entity.ClusterInfoEntity;
import com.datasophon.dao.mapper.ClusterInfoMapper;
import com.datasophon.plugins.api.factory.SshConnectionServiceFactory;
import com.datasophon.plugins.api.service.SshConnectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Agent分发服务实现
 * 
 * @author DataSophon Team
 * @date 2025-01-24
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentDistributionServiceImpl implements AgentDistributionService {
    
    private final AgentStateManager stateManager;
    private final AgentLogWriter logWriter;
    private final ClusterInfoMapper clusterInfoMapper;
    private final ParcelRepositoryService repositoryService;
    private final ConfigBean configBean;
    private final RepositoryDownloaderFactory downloaderFactory;
    
    // SSH连接服务
    private final SshConnectionService sshService = 
            SshConnectionServiceFactory.getInstance().getDefaultSshConnectionService();
    
    // 异步执行线程池
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    
    @Override
    public String startDistribution(Long clusterId, List<String> hostIps, Map<String, Object> connectionParams) {
        log.info("开始Agent分发: 集群={}, 主机数量={}", clusterId, hostIps.size());
        
        // 获取集群信息
        ClusterInfoEntity cluster = clusterInfoMapper.selectOneById(clusterId);
        if (cluster == null) {
            throw new RuntimeException("集群不存在: " + clusterId);
        }
        
        // 获取存储库配置
        String agentPackageUrl = getAgentPackageUrl(cluster);
        boolean isLocalRepository = isLocalRepository(agentPackageUrl);
        
        log.info("Agent包来源: {}, 类型: {}", agentPackageUrl, 
                isLocalRepository ? "本地" : "HTTP");
        
        // 提取SSH连接参数
        String sshUser = (String) connectionParams.get("sshUser");
        Integer sshPort = Integer.parseInt(String.valueOf(connectionParams.get("sshPort")));
        String sshPassword = (String) connectionParams.get("sshPassword");
        
        // 为每个主机初始化状态并创建异步分发任务
        for (String hostIp : hostIps) {
            String hostname = hostIp; // 默认使用IP作为主机名
            if (connectionParams.containsKey("hostnames")) {
                @SuppressWarnings("unchecked")
                Map<String, String> hostnameMap = (Map<String, String>) connectionParams.get("hostnames");
                hostname = hostnameMap.getOrDefault(hostIp, hostIp);
            }
            
            // 初始化主机状态
            stateManager.initHostStatus(clusterId, hostIp, hostname);
            
            // 清理旧日志
            logWriter.clearLog(clusterId, hostIp);
            
            // 启动异步分发任务
            String finalHostname = hostname;
            CompletableFuture.runAsync(() -> {
                distributeToHost(clusterId, hostIp, finalHostname, sshUser, sshPort, sshPassword,
                        agentPackageUrl, isLocalRepository, cluster.getClusterFrame());
            }, executorService);
        }
        
        return "Agent分发任务已启动";
    }
    
    /**
     * 分发Agent到单个主机
     */
    private void distributeToHost(Long clusterId, String hostIp, String hostname,
                                   String sshUser, Integer sshPort, String sshPassword,
                                   String agentPackageUrl, boolean isLocalRepository,
                                   String clusterFrame) {
        try {
            // 检查是否已取消
            if (stateManager.isCancelled(clusterId)) {
                log.info("集群分发已取消: {}, 主机: {}", clusterId, hostIp);
                stateManager.updateHostStatus(clusterId, hostIp, "FAILED", 0,
                        "", "分发已取消");
                return;
            }
            
            log.info("开始分发Agent到主机: {}", hostIp);
            stateManager.updateHostStatus(clusterId, hostIp, "RUNNING", 0,
                    "准备中", "开始分发Agent");
            
            logWriter.logStart(clusterId, hostIp, "开始分发Agent到主机: " + hostIp);
            
            // 构建分发上下文
            AgentDistributionContext context = AgentDistributionContext.builder()
                    .clusterId(clusterId)
                    .hostIp(hostIp)
                    .hostname(hostname)
                    .sshUser(sshUser)
                    .sshPort(sshPort)
                    .sshPassword(sshPassword)
                    .agentPackageUrl(agentPackageUrl)
                    .isLocalRepository(isLocalRepository)
                    .logWriter(logWriter)
                    .remoteInstallPath(Constants.INSTALL_PATH)
                    .build();
            
            // 创建分发步骤列表
            List<AgentDistributionStep> steps = Arrays.asList(
                    new DownloadAgentStep(downloaderFactory),
                    new UploadAgentStep(sshService),
                    new VerifyMd5Step(sshService),
                    new DecompressAgentStep(sshService),
                    new StartAgentStep(sshService, configBean, clusterFrame)
            );
            
            int totalSteps = steps.size();
            int completedSteps = 0;
            
            // 依次执行每个步骤
            for (AgentDistributionStep step : steps) {
                // 检查是否已取消
                if (stateManager.isCancelled(clusterId)) {
                    log.info("集群分发已取消: {}, 主机: {}", clusterId, hostIp);
                    stateManager.updateHostStatus(clusterId, hostIp, "FAILED", 
                            (completedSteps * 100) / totalSteps,
                            step.getStepName(), "分发已取消");
                    return;
                }
                
                String stepName = step.getStepName();
                log.info("执行步骤 [{}/{}]: {} - 主机: {}", 
                        completedSteps + 1, totalSteps, stepName, hostIp);
                
                // 更新状态
                int progress = (completedSteps * 100) / totalSteps;
                stateManager.updateHostStatus(clusterId, hostIp, "RUNNING", 
                        progress, stepName, "执行中: " + stepName);
                
                try {
                    // 执行步骤
                    step.execute(context);
                    completedSteps++;
                    
                    log.info("步骤完成 [{}/{}]: {} - 主机: {}", 
                            completedSteps, totalSteps, stepName, hostIp);
                    
                } catch (Exception e) {
                    log.error("步骤执行失败: {} - 主机: {}, 错误: {}", 
                            stepName, hostIp, e.getMessage(), e);
                    
                    // 标记为失败
                    stateManager.updateHostStatus(clusterId, hostIp, "FAILED",
                            (completedSteps * 100) / totalSteps,
                            stepName, "失败: " + e.getMessage());
                    
                    logWriter.logError(clusterId, hostIp, "error",
                            "Agent分发失败: " + e.getMessage(), null);
                    return;
                }
            }
            
            // 所有步骤完成，标记为成功
            stateManager.updateHostStatus(clusterId, hostIp, "SUCCESS", 100,
                    "完成", "Agent分发成功");
            
            logWriter.logSuccess(clusterId, hostIp, "complete",
                    "Agent分发成功完成", null);
            log.info("Agent分发成功完成: {}", hostIp);
            
        } catch (Exception e) {
            log.error("Agent分发异常: 主机={}, 错误={}", hostIp, e.getMessage(), e);
            stateManager.updateHostStatus(clusterId, hostIp, "FAILED", 0,
                    "", "分发异常: " + e.getMessage());
            
            logWriter.logError(clusterId, hostIp, "error",
                    "Agent分发异常: " + e.getMessage(), null);
        }
    }
    
    @Override
    public List<AgentDistributionStatusVO> getDistributionStatus(Long clusterId) {
        return stateManager.getClusterStatus(clusterId);
    }
    
    @Override
    public void cancelDistribution(Long clusterId) {
        log.info("取消Agent分发: 集群={}", clusterId);
        stateManager.cancelCluster(clusterId);
    }
    
    /**
     * 获取Agent包URL
     * 从集群配置的存储库获取Agent包路径
     */
    private String getAgentPackageUrl(ClusterInfoEntity cluster) {
        Long repositoryId = cluster.getRepositoryId();
        
        // 集群必须配置存储库ID
        if (repositoryId == null) {
            String errorMsg = String.format("集群 %s 未配置存储库ID，无法获取Agent包路径", cluster.getId());
            log.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }
        
        ParcelRepositoryDTO repository = repositoryService.getById(repositoryId);
        
        // 存储库记录必须存在
        if (repository == null) {
            String errorMsg = String.format("存储库ID %s 不存在，无法获取Agent包路径", repositoryId);
            log.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }
        
        String repoUrl = repository.getRepoUrl();
        // 确保URL末尾没有斜杠
        String baseUrl = repoUrl.endsWith("/") ? repoUrl.substring(0, repoUrl.length() - 1) : repoUrl;
        String agentPackagePath = baseUrl + "/" + Constants.WORKER_PACKAGE_NAME;
        
        log.info("从存储库获取Agent包路径: type={}, url={}", 
                repository.getRepoType(), agentPackagePath);
        
        return agentPackagePath;
    }
    
    /**
     * 判断是否为本地存储库
     */
    private boolean isLocalRepository(String url) {
        // 不以http://或https://开头的都认为是本地路径
        return !url.startsWith("http://") && !url.startsWith("https://");
    }
}

