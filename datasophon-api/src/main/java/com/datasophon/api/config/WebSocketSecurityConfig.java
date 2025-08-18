package com.datasophon.api.config;

import com.datasophon.common.security.TokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket安全配置
 * 使用现代化的Spring配置方式，避免deprecated API
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-18
 */
@Configuration
public class WebSocketSecurityConfig implements WebSocketMessageBrokerConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketSecurityConfig.class);
    
    private final TokenProvider tokenProvider;

    public WebSocketSecurityConfig(TokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                
                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    logger.info("WebSocket STOMP连接请求");
                    
                    // 从连接头中获取认证信息
                    String authToken = accessor.getFirstNativeHeader("Authorization");
                    if (StringUtils.hasText(authToken) && authToken.startsWith("Bearer ")) {
                        String token = authToken.substring(7);
                        logger.debug("从连接头获取到token");
                        
                        if (authenticateWithToken(token, accessor)) {
                            return message;
                        }
                    }
                    
                    // 如果连接头没有token，尝试从查询参数获取
                    String tokenParam = accessor.getFirstNativeHeader("token");
                    if (StringUtils.hasText(tokenParam)) {
                        logger.debug("从连接参数获取到token");
                        
                        if (authenticateWithToken(tokenParam, accessor)) {
                            return message;
                        }
                    }
                    
                    logger.warn("WebSocket STOMP连接认证失败：缺少有效的认证信息");
                    throw new SecurityException("Authentication required");
                }
                
                return message;
            }
            
            /**
             * 使用token进行认证
             */
            private boolean authenticateWithToken(String token, StompHeaderAccessor accessor) {
                try {
                    if (tokenProvider.validateToken(token)) {
                        Authentication auth = tokenProvider.getAuthentication(token);
                        accessor.setUser(auth);
                        logger.info("WebSocket STOMP认证成功: user={}", auth.getName());
                        return true;
                    }
                } catch (Exception e) {
                    logger.error("WebSocket token认证失败", e);
                }
                return false;
            }
        });
    }
}
