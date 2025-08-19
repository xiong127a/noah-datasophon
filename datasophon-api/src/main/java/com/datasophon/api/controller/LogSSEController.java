package com.datasophon.api.controller;

import com.datasophon.api.annotation.ApiVersion;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.api.service.ClusterServiceCommandHostCommandService;
import com.datasophon.api.service.LogTailService;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.dao.entity.ClusterInfoEntity;
import com.datasophon.dao.entity.ClusterServiceCommandHostCommandEntity;
import com.datasophon.api.master.ActorUtils;
import org.apache.pekko.pattern.Patterns;
import org.apache.pekko.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;

import java.io.IOException;
import java.security.Principal;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executors;

/**
 * SSE日志流控制器
 * 使用Server-Sent Events实现轻量级实时日志推送
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-19
 */
@ApiVersion(path = "logs")
public class LogSSEController {

    private static final Logger logger = LoggerFactory.getLogger(LogSSEController.class);
    private static final String AKKA_TCP_PREFIX = "pekko.tcp://datasophon@";
    private static final int DEFAULT_LOG_TIMEOUT_SECONDS = 30;
    private static final long SSE_TIMEOUT_MS = 30 * 60 * 1000; // 30分钟超时
    
    // 非K8s模式的定时任务管理
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();
    private final Map<String, String> lastLogContent = new ConcurrentHashMap<>();
    // SSE连接管理
    private final Map<String, SseEmitter> activeConnections = new ConcurrentHashMap<>();
    
    private final ClusterInfoService clusterInfoService;
    private final ClusterServiceCommandHostCommandService hostCommandService;
    private final LogTailService logTailService;

    public LogSSEController(ClusterInfoService clusterInfoService,
                           ClusterServiceCommandHostCommandService hostCommandService,
                           LogTailService logTailService) {
        this.clusterInfoService = clusterInfoService;
        this.hostCommandService = hostCommandService;
        this.logTailService = logTailService;
    }

    /**
     * 建立SSE连接，开始推送日志
     * GET /ddh/api/v1/logs/stream?clusterId=xxx&hostCommandId=xxx&token=xxx
     * 
     * 由于EventSource API不支持自定义header，token通过URL参数传递
     * Spring Security会自动处理认证（JwtTokenProviderBase已扩展支持URL参数token）
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamLogs(@RequestParam String clusterId,
                                @RequestParam String hostCommandId,
                                Principal principal) {
        
        var username = principal.getName();
        String sessionKey = username + ":" + clusterId + ":" + hostCommandId;
        
        logger.info("建立SSE日志连接: user={}, clusterId={}, hostCommandId={}", 
                   username, clusterId, hostCommandId);
        
        // 创建SSE连接
        SseEmitter sseEmitter = new SseEmitter(SSE_TIMEOUT_MS);
        activeConnections.put(sessionKey, sseEmitter);
        
        // 连接关闭时清理资源
        sseEmitter.onCompletion(() -> cleanup(sessionKey));
        sseEmitter.onTimeout(() -> cleanup(sessionKey));
        sseEmitter.onError(throwable -> {
            logger.error("SSE连接异常: {}", sessionKey, throwable);
            cleanup(sessionKey);
        });
        
        try {
            // 发送连接成功消息
            sseEmitter.send(SseEmitter.event()
                .name("connection")
                .data("连接建立成功"));
            
            // 获取日志文件信息
            LogFileInfo logInfo = getLogFileInfo(clusterId, hostCommandId);
            if (logInfo == null) {
                sseEmitter.send(SseEmitter.event()
                    .name("error") 
                    .data("无法获取日志文件信息"));
                return sseEmitter;
            }
            
            // 检查是否为K8s模式
            boolean isKubernetes = logInfo.clusterInfo().getDepType() != null && 
                                  logInfo.clusterInfo().getDepType().isKubernetes();
            
            if (isKubernetes) {
                // K8s模式：使用Apache Commons IO Tailer
                startK8sLogTailing(sessionKey, logInfo, sseEmitter);
            } else {
                // 非K8s模式：使用Actor轮询
                startActorLogPolling(sessionKey, logInfo, sseEmitter);
            }
            
        } catch (Exception e) {
            logger.error("启动日志流失败: {}", sessionKey, e);
            try {
                sseEmitter.send(SseEmitter.event()
                    .name("error")
                    .data("启动日志流失败: " + e.getMessage()));
            } catch (IOException ex) {
                logger.error("发送错误消息失败", ex);
            }
        }
        
        return sseEmitter;
    }
    
    /**
     * K8s模式：使用Tailer统一处理
     */
    private void startK8sLogTailing(String sessionKey, LogFileInfo logInfo, SseEmitter sseEmitter) {
        logger.info("K8s模式 - 启动SSE日志跟踪: {}", logInfo.fullPath());
        
        LogTailService.LogSession session = new LogTailService.LogSession(
            sessionKey, 
            sessionKey, // username作为标识
            logInfo.fullPath(),
            // SSE发送回调
            newContent -> {
                try {
                    sseEmitter.send(SseEmitter.event()
                        .name("log")
                        .data(newContent));
                } catch (IOException e) {
                    logger.error("SSE发送日志失败: {}", sessionKey, e);
                    cleanup(sessionKey);
                }
            }
        );
        
        logTailService.startTailing(session);
    }
    
    /**
     * 非K8s模式：使用Actor轮询
     */
    private void startActorLogPolling(String sessionKey, LogFileInfo logInfo, SseEmitter sseEmitter) {
        logger.info("非K8s模式 - 启动SSE Actor轮询: host={}", logInfo.hostCommand().getHostname());
        
        ScheduledFuture<?> task = scheduler.scheduleWithFixedDelay(() -> {
            try {
                String newLogContent = getActorLogContent(logInfo);
                String lastContent = lastLogContent.get(sessionKey);
                
                // 智能去重：只有内容真正变化才推送
                if (!newLogContent.equals(lastContent)) {
                    lastLogContent.put(sessionKey, newLogContent);
                    if (!newLogContent.isEmpty() && !"can not find log file".equals(newLogContent)) {
                        sseEmitter.send(SseEmitter.event()
                            .name("log")
                            .data(newLogContent));
                    }
                }
            } catch (Exception e) {
                logger.error("SSE Actor日志获取失败: {}", sessionKey, e);
                try {
                    sseEmitter.send(SseEmitter.event()
                        .name("error")
                        .data("获取日志失败: " + e.getMessage()));
                } catch (IOException ex) {
                    logger.error("发送错误消息失败", ex);
                }
                cleanup(sessionKey);
            }
        }, 0, 1, TimeUnit.SECONDS);
        
        scheduledTasks.put(sessionKey, task);
    }
    
    /**
     * 清理资源
     */
    private void cleanup(String sessionKey) {
        logger.info("清理SSE连接资源: {}", sessionKey);
        
        // 清理SSE连接
        activeConnections.remove(sessionKey);
        
        // 清理定时任务
        ScheduledFuture<?> task = scheduledTasks.remove(sessionKey);
        if (task != null) {
            task.cancel(true);
        }
        
        // 清理日志缓存
        lastLogContent.remove(sessionKey);
        
        // 清理Tailer
        logTailService.stopTailing(sessionKey);
    }
    
    /**
     * 获取日志文件信息（复用原有逻辑）
     */
    private LogFileInfo getLogFileInfo(String clusterId, String hostCommandId) {
        try {
            var clusterInfo = clusterInfoService.getById(Long.parseLong(clusterId));
            if (clusterInfo == null) {
                logger.error("未找到集群信息: clusterId={}", clusterId);
                return null;
            }
            
            var hostCommand = hostCommandService.getById(Long.parseLong(hostCommandId));
            if (hostCommand == null) {
                logger.error("未找到主机命令: hostCommandId={}", hostCommandId);
                return null;
            }
            
            // 获取服务名称和角色名称
            String serviceName = hostCommand.getServiceName();
            String serviceRoleName = hostCommand.getServiceRoleName();
            
            if (serviceName == null || serviceRoleName == null) {
                logger.error("服务名称或角色名称为空: serviceName={}, serviceRoleName={}", serviceName, serviceRoleName);
                return null;
            }
            
            // 增加集群ID层级，实现多集群日志隔离
            String logFile = String.format("%s/%s/%s/%s.log", "logs", clusterId, serviceName, serviceRoleName);
            String relativePath = String.format("%s/%s.log", serviceName, serviceRoleName);
            
            return new LogFileInfo(clusterInfo, hostCommand, logFile, relativePath);
        } catch (Exception e) {
            logger.error("获取日志文件信息失败: clusterId={}, hostCommandId={}", clusterId, hostCommandId, e);
            return null;
        }
    }
    
    /**
     * 非K8s模式：通过Actor获取日志内容
     */
    private String getActorLogContent(LogFileInfo logInfo) throws Exception {
        var command = new com.datasophon.common.command.GetLogCommand();
        command.setLogFile(logInfo.relativePath());
        command.setDecompressPackageName("datasophon-worker");
        
        logger.debug("通过Actor获取历史日志: host={}, file={}", 
                    logInfo.hostCommand().getHostname(), logInfo.relativePath());
        
        var timeout = new Timeout(Duration.create(DEFAULT_LOG_TIMEOUT_SECONDS, TimeUnit.SECONDS));
        var configActor = ActorUtils.actorSystem
                .actorSelection(AKKA_TCP_PREFIX + logInfo.hostCommand().getHostname() + ":2552/user/worker/logActor");
        
        var logFuture = Patterns.ask(configActor, command, timeout);
        var logResult = (ExecResult) Await.result(logFuture, timeout.duration());
        
        if (Objects.nonNull(logResult) && logResult.getExecResult()) {
            return logResult.getExecOut();
        }
        return "";
    }
    
    // JDK21 record - 日志文件信息
    private record LogFileInfo(ClusterInfoEntity clusterInfo, 
                              ClusterServiceCommandHostCommandEntity hostCommand,
                              String fullPath, String relativePath) {}
}
