package com.datasophon.api.controller.websocket;

import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.api.service.ClusterServiceCommandHostCommandService;
import com.datasophon.api.service.ClusterServiceCommandService;
import com.datasophon.api.service.LogTailService;
import com.datasophon.common.command.GetLogCommand;

import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.utils.PropertyUtils;
import com.datasophon.dao.entity.ClusterInfoEntity;
import com.datasophon.dao.entity.ClusterServiceCommandHostCommandEntity;
import com.datasophon.dao.mapper.ClusterServiceCommandHostCommandMapper;
import com.datasophon.kubernetes.util.KubernetesMinaUtils;
import com.datasophon.api.master.ActorUtils;
import org.apache.pekko.actor.ActorSelection;
import org.apache.pekko.pattern.Patterns;
import org.apache.pekko.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.nio.charset.Charset;
import java.security.Principal;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executors;

/**
 * 日志WebSocket控制器
 * 使用STOMP协议和注解方式实现实时日志推送
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-18
 */
@Controller
public class LogWebSocketController {

    private static final Logger logger = LoggerFactory.getLogger(LogWebSocketController.class);
    private static final String AKKA_TCP_PREFIX = "pekko.tcp://datasophon@";
    private static final int DEFAULT_LOG_TIMEOUT_SECONDS = 30;
    
    // 非K8s模式的定时任务管理
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();
    // 缓存最后的日志内容，避免重复推送
    private final Map<String, String> lastLogContent = new ConcurrentHashMap<>();
    
    private final ClusterInfoService clusterInfoService;
    private final ClusterServiceCommandHostCommandService hostCommandService;
    private final ClusterServiceCommandService commandService;
    private final SimpMessageSendingOperations messagingTemplate;
    private final LogTailService logTailService;

    public LogWebSocketController(ClusterInfoService clusterInfoService,
                                 ClusterServiceCommandHostCommandService hostCommandService,
                                 ClusterServiceCommandService commandService,
                                 SimpMessageSendingOperations messagingTemplate,
                                 LogTailService logTailService) {
        this.clusterInfoService = clusterInfoService;
        this.hostCommandService = hostCommandService;
        this.commandService = commandService;
        this.messagingTemplate = messagingTemplate;
        this.logTailService = logTailService;
    }

    /**
     * 启动日志流 - 超简单实现
     * 客户端发送消息到 /app/logs/start
     */
    @MessageMapping("/logs/start")
    public void startLogStream(@Payload LogStartRequest request, Principal principal) {
        var username = principal.getName();
        String sessionKey = username + ":" + request.clusterId() + ":" + request.hostCommandId();
        
        logger.info("启动日志流: user={}, clusterId={}, hostCommandId={}", 
                   username, request.clusterId(), request.hostCommandId());
        
        try {
            // 获取日志文件信息（复用原有逻辑）
            LogFileInfo logInfo = getLogFileInfo(request.clusterId(), request.hostCommandId());
            if (logInfo == null) {
                messagingTemplate.convertAndSendToUser(username, "/queue/logs", 
                    new LogMessage("error", "无法获取日志文件信息", "ERROR"));
                return;
            }
            
            // 检查是否为K8s模式
            boolean isKubernetes = logInfo.clusterInfo().getDepType() != null && 
                                  logInfo.clusterInfo().getDepType().isKubernetes();
            
            if (isKubernetes) {
                // K8s模式：使用增量读取（优化版）
                logger.info("K8s模式 - 启动增量日志跟踪: {}", logInfo.fullPath());
                
                // 先发送历史日志（复用原有readLastRows逻辑）
                String historyLog = KubernetesMinaUtils.readLastRows(
                    logInfo.fullPath(), 
                    Charset.defaultCharset(), 
                    PropertyUtils.getInt("rows")
                );
                
                if (!historyLog.isEmpty()) {
                    messagingTemplate.convertAndSendToUser(username, "/queue/logs", 
                        new LogMessage("history", historyLog, "INFO")); // 标记为历史日志
                }
                
                // 启动实时跟踪（使用Commons IO Tailer）
                LogTailService.LogSession session = new LogTailService.LogSession(
                    sessionKey, 
                    username, 
                    logInfo.fullPath(),
                    // 新内容回调 - 推送增量内容
                    newContent -> messagingTemplate.convertAndSendToUser(username, "/queue/logs", 
                        new LogMessage("increment", newContent, "INFO")) // 标记为增量日志
                );
                
                logTailService.startTailing(session);
                
            } else {
                // 非K8s模式：优化的Actor轮询（高频率，智能缓存）
                logger.info("非K8s模式 - 启动优化Actor日志获取: host={}", logInfo.hostCommand().getHostname());
                
                // 启动高频轮询，1秒一次，带智能去重
                ScheduledFuture<?> task = scheduler.scheduleWithFixedDelay(() -> {
                    try {
                        String newLogContent = getActorLogContent(logInfo);
                        String lastContent = lastLogContent.get(sessionKey);
                        
                        // 智能去重：只有内容真正变化才推送
                        if (!newLogContent.equals(lastContent)) {
                            lastLogContent.put(sessionKey, newLogContent);
                            if (!newLogContent.isEmpty() && !"can not find log file".equals(newLogContent)) {
                                // 非K8s模式发送完整日志内容，标记为full
                                messagingTemplate.convertAndSendToUser(username, "/queue/logs", 
                                    new LogMessage("full", newLogContent, "INFO"));
                            }
                        }
                    } catch (Exception e) {
                        logger.error("获取Actor日志失败", e);
                        messagingTemplate.convertAndSendToUser(username, "/queue/logs", 
                            new LogMessage("error", "获取日志失败: " + e.getMessage(), "ERROR"));
                    }
                }, 0, 1, TimeUnit.SECONDS); // 优化：1秒轮询，比原来的2秒更实时
                
                scheduledTasks.put(sessionKey, task);
            }
            
            // 发送启动成功消息
            messagingTemplate.convertAndSendToUser(username, "/queue/logs", 
                new LogMessage("started", "日志流已启动", "INFO"));
            
        } catch (Exception e) {
            logger.error("启动日志流失败: user={}", username, e);
            messagingTemplate.convertAndSendToUser(username, "/queue/logs", 
                new LogMessage("error", "启动日志流失败: " + e.getMessage(), "ERROR"));
        }
    }

    /**
     * 停止日志流 - 超简单实现
     */
    @MessageMapping("/logs/stop")
    public void stopLogStream(@Payload LogStopRequest request, Principal principal) {
        var username = principal.getName();
        String sessionKey = username + ":" + request.clusterId() + ":" + request.hostCommandId();
        
        logger.info("停止日志流: user={}, sessionKey={}", username, sessionKey);
        
        // 停止K8s模式的Tailer跟踪
        logTailService.stopTailing(sessionKey);
        
        // 停止非K8s模式的定时任务
        ScheduledFuture<?> task = scheduledTasks.remove(sessionKey);
        if (task != null) {
            task.cancel(false);
            logger.info("停止非K8s模式定时任务: sessionKey={}", sessionKey);
        }
        
        // 清理缓存的日志内容
        lastLogContent.remove(sessionKey);
        
        // 发送停止确认消息
        messagingTemplate.convertAndSendToUser(username, "/queue/logs", 
            new LogMessage("stopped", "日志流已停止", "INFO"));
    }

    /**
     * 心跳处理
     */
    @MessageMapping("/logs/ping")
    public void handlePing(Principal principal) {
        messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/logs", 
            new LogMessage("pong", "服务器响应", "INFO"));
    }

    /**
     * 用户订阅日志主题时的处理
     */
    @SubscribeMapping("/queue/logs")
    public LogMessage onSubscribe(Principal principal) {
        logger.info("用户订阅日志主题: user={}", principal.getName());
        return new LogMessage("connection", "WebSocket连接已建立", "INFO");
    }
    
    /**
     * 获取日志文件路径和集群信息 - 完全复用原有逻辑
     */
    private LogFileInfo getLogFileInfo(Long clusterId, Long hostCommandId) {
        try {
            ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);
            // 通过service层获取hostCommand
            ClusterServiceCommandHostCommandEntity hostCommand = null;
            try {
                // 暂时直接调用mapper，后续可以优化为service方法
                var mapper = hostCommandService.getMapper();
                if (mapper instanceof ClusterServiceCommandHostCommandMapper) {
                    hostCommand = ((ClusterServiceCommandHostCommandMapper) mapper).selectByHostCommandId(hostCommandId);
                }
            } catch (Exception e) {
                logger.error("获取hostCommand失败", e);
            }

            if (hostCommand == null || clusterInfo == null) {
                return null;
            }

            var commandDto = commandService.getCommandById(hostCommand.getCommandId());
            if (commandDto == null) {
                return null;
            }

            // 完全按照原有逻辑获取服务名称
            String serviceName = commandDto.serviceName();
            if (serviceName == null || serviceName.trim().isEmpty()) {
                serviceName = commandDto.commandName();
                logger.warn("Command ID {} serviceName is null, using commandName: {}", hostCommand.getCommandId(), serviceName);
            }
            String serviceRoleName = hostCommand.getServiceRoleName();
            String logFile = String.format("%s/%s/%s.log", "logs", serviceName, serviceRoleName);
            
            // K8s模式需要完整路径
            String fullPath = clusterInfo.getDepType() != null && clusterInfo.getDepType().isKubernetes() 
                ? System.getProperty("user.dir") + "/" + logFile
                : logFile;

            return new LogFileInfo(clusterInfo, hostCommand, fullPath, logFile);
            
        } catch (Exception e) {
            logger.error("获取日志文件信息失败", e);
            return null;
        }
    }
    
    /**
     * 非K8s模式：通过Actor获取日志内容（保持兼容）
     */
    private String getActorLogContent(LogFileInfo logInfo) throws Exception {
        GetLogCommand command = new GetLogCommand();
        command.setLogFile(logInfo.relativePath());
        command.setDecompressPackageName("datasophon-worker");
        
        logger.debug("通过Actor获取历史日志: host={}, file={}", 
                    logInfo.hostCommand().getHostname(), logInfo.relativePath());
        
        Timeout timeout = new Timeout(Duration.create(DEFAULT_LOG_TIMEOUT_SECONDS, TimeUnit.SECONDS));
        ActorSelection configActor = ActorUtils.actorSystem
                .actorSelection(AKKA_TCP_PREFIX + logInfo.hostCommand().getHostname() + ":2552/user/worker/logActor");
        
        Future<Object> logFuture = Patterns.ask(configActor, command, timeout);
        ExecResult logResult = (ExecResult) Await.result(logFuture, timeout.duration());
        
        if (Objects.nonNull(logResult) && logResult.getExecResult()) {
            return logResult.getExecOut();
        }
        return "";
    }
    

    
    // JDK21 record - 日志文件信息
    record LogFileInfo(ClusterInfoEntity clusterInfo, ClusterServiceCommandHostCommandEntity hostCommand, 
                      String fullPath, String relativePath) {}

    // JDK21 static record - 解决Jackson序列化问题
    public static record LogStartRequest(Long clusterId, Long hostCommandId) {}
    public static record LogStopRequest(Long clusterId, Long hostCommandId) {}
    
    // JDK21 static record简化响应类
    public static record LogMessage(String type, String data, String level, long timestamp) {
        public LogMessage(String type, String data, String level) {
            this(type, data, level, System.currentTimeMillis());
        }
    }
}
