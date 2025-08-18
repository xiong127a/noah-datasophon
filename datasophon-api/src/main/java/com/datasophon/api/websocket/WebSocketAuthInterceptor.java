package com.datasophon.api.websocket;

import com.datasophon.common.security.TokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.List;
import java.util.Map;

/**
 * WebSocket认证拦截器
 * 在WebSocket握手阶段进行JWT认证
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-18
 */
@Component
public class WebSocketAuthInterceptor implements HandshakeInterceptor {
    
    private static final Logger logger = LoggerFactory.getLogger(WebSocketAuthInterceptor.class);
    
    private final TokenProvider tokenProvider;
    
    public WebSocketAuthInterceptor(TokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }
    
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        try {
            // 从请求头获取Authorization
            List<String> authHeaders = request.getHeaders().get(HttpHeaders.AUTHORIZATION);
            
            if (authHeaders == null || authHeaders.isEmpty()) {
                // 如果header中没有，尝试从查询参数获取token（WebSocket可能无法设置header）
                String query = request.getURI().getQuery();
                if (query != null && query.contains("token=")) {
                    String token = extractTokenFromQuery(query);
                    if (token != null && tokenProvider.validateToken(token)) {
                        Authentication auth = tokenProvider.getAuthentication(token);
                        attributes.put("authentication", auth);
                        logger.debug("WebSocket认证成功(从查询参数): {}", auth.getName());
                        return true;
                    }
                }
                logger.warn("WebSocket连接缺少认证信息");
                return false;
            }
            
            String bearerToken = authHeaders.get(0);
            if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
                String token = bearerToken.substring(7);
                
                if (tokenProvider.validateToken(token)) {
                    Authentication auth = tokenProvider.getAuthentication(token);
                    // 将认证信息存储到WebSocket会话属性中
                    attributes.put("authentication", auth);
                    logger.debug("WebSocket认证成功: {}", auth.getName());
                    return true;
                }
            }
            
            logger.warn("WebSocket连接认证失败：无效的token");
            return false;
            
        } catch (Exception e) {
            logger.error("WebSocket认证过程出错", e);
            return false;
        }
    }
    
    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        if (exception != null) {
            logger.error("WebSocket握手后发生异常", exception);
        }
    }
    
    /**
     * 从查询字符串中提取token参数
     */
    private String extractTokenFromQuery(String query) {
        String[] params = query.split("&");
        for (String param : params) {
            String[] keyValue = param.split("=");
            if (keyValue.length == 2 && "token".equals(keyValue[0])) {
                return keyValue[1];
            }
        }
        return null;
    }
}
