package com.datasophon.api.websocket;

import com.datasophon.api.service.ClusterServiceCommandHostCommandService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 日志WebSocket处理器
 * 实现实时日志推送功能
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-15
 */
@Component
public class LogWebSocketHandler implements WebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(LogWebSocketHandler.class);
    
    private final ClusterServiceCommandHostCommandService hostCommandService;
    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService executor;
    
    // 存储WebSocket会话信息
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, LogStreamInfo> logStreams = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    public LogWebSocketHandler(ClusterServiceCommandHostCommandService hostCommandService) {
        this.hostCommandService = hostCommandService;
        this.objectMapper = new ObjectMapper();
        this.executor = Executors.newScheduledThreadPool(10);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        
        // 获取认证信息
        Object auth = session.getAttributes().get("authentication");
        if (auth != null) {
            logger.info("WebSocket连接建立: sessionId={}, user={}", sessionId, auth.toString());
        } else {
            logger.warn("WebSocket连接建立但缺少认证信息: sessionId={}", sessionId);
            // 理论上不应该到这里，因为拦截器已经验证过
            session.close();
            return;
        }
        
        sessions.put(sessionId, session);
        
        // 发送连接成功消息
        sendMessage(session, new LogMessage("connection", "WebSocket连接已建立", "INFO"));
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        String payload = message.getPayload().toString();
        String sessionId = session.getId();
        
        try {
            // 解析客户端消息
            @SuppressWarnings("unchecked")
            Map<String, Object> messageData = objectMapper.readValue(payload, Map.class);
            String action = (String) messageData.get("action");
            
            logger.debug("收到WebSocket消息: sessionId={}, action={}", sessionId, action);
            
            switch (action) {
                case "startLog":
                    startLogStream(session, messageData);
                    break;
                case "stopLog":
                    stopLogStream(session);
                    break;
                case "ping":
                    sendMessage(session, new LogMessage("pong", "服务器响应", "INFO"));
                    break;
                default:
                    sendMessage(session, new LogMessage("error", "未知操作: " + action, "ERROR"));
            }
        } catch (Exception e) {
            logger.error("处理WebSocket消息时出错: sessionId={}", sessionId, e);
            sendMessage(session, new LogMessage("error", "消息处理失败: " + e.getMessage(), "ERROR"));
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String sessionId = session.getId();
        logger.error("WebSocket传输错误: sessionId={}", sessionId, exception);
        cleanup(sessionId);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        String sessionId = session.getId();
        logger.info("WebSocket连接关闭: sessionId={}, status={}", sessionId, closeStatus);
        cleanup(sessionId);
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    /**
     * 开始日志流
     */
    private void startLogStream(WebSocketSession session, Map<String, Object> messageData) {
        String sessionId = session.getId();
        
        try {
            Long clusterId = Long.valueOf(messageData.get("clusterId").toString());
            Long hostCommandId = Long.valueOf(messageData.get("hostCommandId").toString());
            
            LogStreamInfo streamInfo = new LogStreamInfo(clusterId, hostCommandId);
            logStreams.put(sessionId, streamInfo);
            
            // 启动定时任务推送日志
            ScheduledFuture<?> task = executor.scheduleWithFixedDelay(
                () -> pushLogUpdates(session, streamInfo),
                0, 2, TimeUnit.SECONDS
            );
            scheduledTasks.put(sessionId, task);
            
            logger.info("开始日志流: sessionId={}, clusterId={}, hostCommandId={}", 
                       sessionId, clusterId, hostCommandId);
            sendMessage(session, new LogMessage("started", "日志流已启动", "INFO"));
            
        } catch (Exception e) {
            logger.error("启动日志流失败: sessionId={}", sessionId, e);
            sendMessage(session, new LogMessage("error", "启动日志流失败: " + e.getMessage(), "ERROR"));
        }
    }

    /**
     * 停止日志流
     */
    private void stopLogStream(WebSocketSession session) {
        String sessionId = session.getId();
        
        ScheduledFuture<?> task = scheduledTasks.remove(sessionId);
        if (task != null) {
            task.cancel(false);
        }
        
        logStreams.remove(sessionId);
        logger.info("停止日志流: sessionId={}", sessionId);
        sendMessage(session, new LogMessage("stopped", "日志流已停止", "INFO"));
    }

    /**
     * 推送日志更新
     */
    private void pushLogUpdates(WebSocketSession session, LogStreamInfo streamInfo) {
        try {
            String newLogContent = hostCommandService.getHostCommandLog(
                streamInfo.getClusterId(), streamInfo.getHostCommandId());
            
            // 检查日志内容是否有变化
            if (!newLogContent.equals(streamInfo.getLastContent())) {
                streamInfo.setLastContent(newLogContent);
                
                // 发送完整日志内容（后续可优化为增量推送）
                sendMessage(session, new LogMessage("log", newLogContent, "INFO"));
            }
        } catch (Exception e) {
            logger.error("推送日志更新失败: sessionId={}", session.getId(), e);
            sendMessage(session, new LogMessage("error", "获取日志失败: " + e.getMessage(), "ERROR"));
        }
    }

    /**
     * 发送消息到客户端
     */
    private void sendMessage(WebSocketSession session, LogMessage message) {
        try {
            if (session.isOpen()) {
                String json = objectMapper.writeValueAsString(message);
                session.sendMessage(new TextMessage(json));
            }
        } catch (IOException e) {
            logger.error("发送WebSocket消息失败: sessionId={}", session.getId(), e);
        }
    }

    /**
     * 清理会话资源
     */
    private void cleanup(String sessionId) {
        sessions.remove(sessionId);
        logStreams.remove(sessionId);
        
        ScheduledFuture<?> task = scheduledTasks.remove(sessionId);
        if (task != null) {
            task.cancel(false);
        }
        
        logger.debug("清理WebSocket会话资源: sessionId={}", sessionId);
    }

    /**
     * 日志消息类
     */
    public static class LogMessage {
        private String type;
        private String data;
        private String level;
        private long timestamp;

        public LogMessage(String type, String data, String level) {
            this.type = type;
            this.data = data;
            this.level = level;
            this.timestamp = System.currentTimeMillis();
        }

        // Getters and Setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getData() { return data; }
        public void setData(String data) { this.data = data; }
        public String getLevel() { return level; }
        public void setLevel(String level) { this.level = level; }
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }

    /**
     * 日志流信息类
     */
    private static class LogStreamInfo {
        private final Long clusterId;
        private final Long hostCommandId;
        private String lastContent = "";

        public LogStreamInfo(Long clusterId, Long hostCommandId) {
            this.clusterId = clusterId;
            this.hostCommandId = hostCommandId;
        }

        public Long getClusterId() { return clusterId; }
        public Long getHostCommandId() { return hostCommandId; }
        public String getLastContent() { return lastContent; }
        public void setLastContent(String lastContent) { this.lastContent = lastContent; }
    }
}
