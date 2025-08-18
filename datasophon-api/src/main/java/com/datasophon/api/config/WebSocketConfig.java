package com.datasophon.api.config;

import com.datasophon.api.websocket.LogWebSocketHandler;
import com.datasophon.api.websocket.WebSocketAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket配置类
 * 用于配置实时日志推送
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-15
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final LogWebSocketHandler logWebSocketHandler;
    private final WebSocketAuthInterceptor authInterceptor;

    public WebSocketConfig(LogWebSocketHandler logWebSocketHandler,
                          WebSocketAuthInterceptor authInterceptor) {
        this.logWebSocketHandler = logWebSocketHandler;
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 注册日志WebSocket处理器，添加认证拦截器
        registry.addHandler(logWebSocketHandler, "/api/v1/websocket/log")
                .addInterceptors(authInterceptor)
                .setAllowedOrigins("*"); // 生产环境应该配置具体的允许域名
    }
}
