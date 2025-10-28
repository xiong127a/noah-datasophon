package com.datasophon.api.service.impl;

import com.datasophon.api.agent.AgentDistributionContext;
import com.datasophon.api.agent.AgentDistributionStep;
import com.datasophon.api.agent.AgentStateManager;
import com.datasophon.api.agent.steps.*;
import com.datasophon.api.agent.util.AgentLogWriter;
import com.datasophon.api.load.ConfigBean;
import com.datasophon.api.repository.RepositoryDownloaderFactory;
import com.datasophon.api.service.AgentDistributionService;
import com.datasophon.api.service.host.ClusterHostService;
import com.datasophon.api.service.ParcelRepositoryService;
import com.datasophon.common.Constants;
import com.datasophon.common.dto.ParcelRepositoryDTO;
import com.datasophon.common.enums.HostState;
import com.datasophon.common.enums.ManagementStatus;
import com.datasophon.common.vo.agent.AgentDistributionStatusVO;
import com.datasophon.dao.entity.ClusterHostEntity;
import com.datasophon.dao.entity.ClusterInfoEntity;
import com.datasophon.dao.mapper.ClusterHostMapper;
import com.datasophon.dao.mapper.ClusterInfoMapper;
import com.datasophon.plugins.api.factory.SshConnectionServiceFactory;
import com.datasophon.plugins.api.service.SshConnectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
    private final ClusterHostMapper clusterHostMapper;
    private final ClusterHostService clusterHostService;
    private final ParcelRepositoryService repositoryService;
    private final ConfigBean configBean;
    private final RepositoryDownloaderFactory downloaderFactory;
    
    // SSH连接服务（懒加载，第一次使用时才初始化）
    private volatile SshConnectionService sshService;
    
    // 异步执行线程池 - 使用虚拟线程（JDK 21）
    // 虚拟线程非常轻量（KB级别），适合I/O密集型任务（SSH/文件传输）
    // 可以创建数千个虚拟线程而不会耗尽资源
    private final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();
    
    /**
     * 获取SSH连接服务（懒加载模式）
     * 
     * @return SSH连接服务
     */
    private SshConnectionService getSshService() {
        if (sshService == null) {
            synchronized (this) {
                if (sshService == null) {
                    sshService = SshConnectionServiceFactory.getInstance().getDefaultSshConnectionService();
                    log.info("SSH连接服务初始化完成");
                }
            }
        }
        return sshService;
    }
    
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
        
        // ====== 1. 先下载Agent包到管理节点（只下载一次） ======
        String localPackagePath = Constants.MASTER_MANAGE_PACKAGE_PATH + 
                Constants.SLASH + Constants.WORKER_PACKAGE_NAME;
        
        try {
            log.info("【统一下载】开始下载Agent包到管理节点: {} -> {}", 
                    agentPackageUrl, localPackagePath);
            
            // 创建临时上下文用于下载（使用特殊标识，因为是统一下载不针对特定主机）
            AgentDistributionContext downloadContext = AgentDistributionContext.builder()
                    .clusterId(clusterId)
                    .hostIp("management-node")  // 特殊标识：管理节点统一下载
                    .hostname("Management Node")
                    .agentPackageUrl(agentPackageUrl)
                    .isLocalRepository(isLocalRepository)
                    .localPackagePath(localPackagePath)
                    .logWriter(logWriter)
                    .build();
            
            // 执行下载步骤（只执行一次）
            DownloadAgentStep downloadStep = new DownloadAgentStep(downloaderFactory);
            downloadStep.execute(downloadContext);
            
            log.info("【统一下载】Agent包下载完成: {}", localPackagePath);
            
        } catch (Exception e) {
            log.error("【统一下载】Agent包下载失败: {}", e.getMessage(), e);
            
            // 下载失败，所有主机都标记为失败
            for (String hostIp : hostIps) {
                stateManager.initHostStatus(clusterId, hostIp, hostIp);
                stateManager.updateHostStatus(clusterId, hostIp, "FAILED", 0,
                        "下载Agent包", "Agent包下载失败: " + e.getMessage());
            }
            
            throw new RuntimeException("Agent包下载失败: " + e.getMessage(), e);
        }
        
        // 提取SSH连接参数
        String sshUser = (String) connectionParams.get("sshUser");
        Integer sshPort = Integer.parseInt(String.valueOf(connectionParams.get("sshPort")));
        String sshPassword = (String) connectionParams.get("sshPassword");
        
        // ====== 2. 并发分发到各个主机（跳过下载步骤） ======
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
            
            // 启动异步分发任务（从本地上传，跳过下载步骤）
            // 添加30分钟超时控制，防止长时间阻塞
            String finalHostname = hostname;
            CompletableFuture.runAsync(() -> {
                distributeToHost(clusterId, hostIp, finalHostname, sshUser, sshPort, sshPassword,
                        localPackagePath, cluster.getClusterFrame());
            }, executorService)
            .orTimeout(30, TimeUnit.MINUTES)  // 30分钟超时
            .exceptionally(ex -> {
                log.error("Agent分发失败或超时: 集群={}, 主机={}, 错误={}", 
                        clusterId, hostIp, ex.getMessage());
                stateManager.updateHostStatus(clusterId, hostIp, "FAILED", 0, 
                        "", "分发失败: " + (ex instanceof TimeoutException ? "超时(30分钟)" : ex.getMessage()));
                logWriter.logError(clusterId, hostIp, "timeout", 
                        "分发失败: " + ex.getMessage(), null);
                return null;
            });
        }
        
        return "Agent分发任务已启动";
    }
    
    /**
     * 分发Agent到单个主机（从本地已下载的包）
     */
    private void distributeToHost(Long clusterId, String hostIp, String hostname,
                                   String sshUser, Integer sshPort, String sshPassword,
                                   String localPackagePath,
                                   String clusterFrame) {
        try {
            // 检查是否已取消
            if (stateManager.isCancelled(clusterId)) {
                log.info("集群分发已取消: {}, 主机: {}", clusterId, hostIp);
                stateManager.updateHostStatus(clusterId, hostIp, "FAILED", 0,
                        "", "分发已取消");
                return;
            }
            
            log.info("开始分发Agent到主机: {} (从本地: {})", hostIp, localPackagePath);
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
                    .localPackagePath(localPackagePath)
                    .logWriter(logWriter)
                    .remoteInstallPath(Constants.INSTALL_PATH)
                    .build();
            
            // 创建分发步骤列表（跳过下载步骤，直接从本地上传）
            List<AgentDistributionStep> steps = Arrays.asList(
                    new UploadAgentStep(getSshService()),
                    new VerifyMd5Step(getSshService()),
                    new DecompressAgentStep(getSshService()),
                    new StartAgentStep(getSshService(), configBean, clusterFrame),
                    new VerifyWorkerConnectionStep(clusterHostService)  // ✅ 验证Worker连接并收集硬件信息
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
            
            // ========== 保存主机信息到数据库 ==========
            try {
                saveHostToDatabase(clusterId, hostIp, hostname);
                log.info("主机信息已保存到数据库: IP={}, hostname={}", hostIp, hostname);
            } catch (Exception e) {
                log.error("保存主机信息失败: IP={}, 错误={}", hostIp, e.getMessage(), e);
                // 保存失败不影响分发成功状态，仅记录日志
            }
            
        } catch (Exception e) {
            log.error("Agent分发异常: 主机={}, 错误={}", hostIp, e.getMessage(), e);
            stateManager.updateHostStatus(clusterId, hostIp, "FAILED", 0,
                    "", "分发异常: " + e.getMessage());
            
            logWriter.logError(clusterId, hostIp, "error",
                    "Agent分发异常: " + e.getMessage(), null);
        }
    }
    
    /**
     * 保存主机信息到数据库
     */
    private void saveHostToDatabase(Long clusterId, String hostIp, String hostname) {
        // 直接使用Mapper中已有的方法查询
        ClusterHostEntity existingHost = clusterHostMapper.selectByClusterIdAndIp(clusterId, hostIp);
        
        if (existingHost != null) {
            // 主机已存在，更新状态为配置中
            existingHost.setManagementStatus(ManagementStatus.CONFIGURING);
            existingHost.setHostState(HostState.RUNNING);
            existingHost.setHostname(hostname);
            existingHost.setCheckTime(LocalDateTime.now());
            clusterHostMapper.update(existingHost);
            log.info("更新已存在主机信息: IP={}, 状态=配置中", hostIp);
        } else {
            // 新主机，插入数据库，状态设置为配置中
            ClusterHostEntity newHost = ClusterHostEntity.builder()
                    .clusterId(clusterId)
                    .ip(hostIp)
                    .hostname(hostname)
                    .hostState(HostState.RUNNING)
                    .managementStatus(ManagementStatus.CONFIGURING)  // ✅ Agent分发完成后是配置中状态
                    .checkTime(LocalDateTime.now())
                    .rack("/default-rack")  // 默认机架
                    .build();
            
            clusterHostMapper.insert(newHost);
            log.info("新增主机信息到数据库: IP={}, hostname={}, 状态=配置中", hostIp, hostname);
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
        
        // 框架版本号是必需的
        String frameVersion = cluster.getFrameVersion();
        if (frameVersion == null || frameVersion.isEmpty()) {
            String errorMsg = String.format("集群 %s 未配置框架版本号，无法获取Agent包路径", cluster.getId());
            log.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }
        
        // 构建包含框架版本号的路径: baseUrl/frameVersion/datasophon-worker.tar.gz
        String agentPackagePath = baseUrl + "/" + frameVersion + "/" + Constants.WORKER_PACKAGE_NAME;
        
        log.info("从存储库获取Agent包路径: type={}, version={}, url={}", 
                repository.getRepoType(), frameVersion, agentPackagePath);
        
        return agentPackagePath;
    }
    
    /**
     * 判断是否为本地存储库
     */
    private boolean isLocalRepository(String url) {
        // 不以http://或https://开头的都认为是本地路径
        return !url.startsWith("http://") && !url.startsWith("https://");
    }
    
    /**
     * 服务关闭时的清理工作
     * 优雅关闭线程池，等待正在执行的任务完成
     */
    @PreDestroy
    public void shutdown() {
        log.info("正在关闭Agent分发服务...");
        
        executorService.shutdown();
        try {
            // 等待60秒让现有任务完成
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                log.warn("部分Agent分发任务未在60秒内完成，强制关闭线程池");
                List<Runnable> droppedTasks = executorService.shutdownNow();
                log.warn("强制中断了 {} 个未完成的任务", droppedTasks.size());
            } else {
                log.info("所有Agent分发任务已正常完成");
            }
        } catch (InterruptedException e) {
            log.error("等待线程池关闭时被中断", e);
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        log.info("Agent分发服务已关闭");
    }
}

