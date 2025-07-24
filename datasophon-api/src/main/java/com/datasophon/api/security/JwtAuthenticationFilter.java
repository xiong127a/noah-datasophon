/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.datasophon.api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * JWT认证过滤器
 * 用于处理JWT令牌的认证
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    // 不需要验证令牌的路径
    private static final List<String> AUTH_WHITELIST = Arrays.asList(
            "/api/login",
            "/api/register",
            "/api/refresh-token",
            "/swagger-ui",
            "/v3/api-docs");


    private JwtTokenProvider tokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }


    /**
     * 过滤请求，验证JWT令牌
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        // 对于白名单路径，跳过令牌验证
        if (shouldSkipAuthentication(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String jwt = tokenProvider.resolveToken(request);

            if (StringUtils.hasText(jwt)) {
                if (tokenProvider.validateToken(jwt)) {
                    Authentication auth = tokenProvider.getAuthentication(jwt);
                    SecurityContextHolder.getContext().setAuthentication(auth);

                    if (logger.isDebugEnabled()) {
                        logger.debug("已设置认证: {}, URI: {}",
                                auth.getName(), request.getRequestURI());
                    }
                } else {
                    logger.debug("无效的JWT令牌");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("{\"error\":\"无效的令牌\",\"status\":401}");
                    return;
                }
            } else {
                logger.debug("未找到JWT令牌, URI: {}", request.getRequestURI());
            }
        } catch (Exception e) {
            logger.error("无法设置用户认证: {}", e.getMessage());
            SecurityContextHolder.clearContext();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\":\"认证失败\",\"status\":401}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 检查是否应该跳过认证
     * 
     * @param path 请求路径
     * @return 如果应该跳过则返回true
     */
    private boolean shouldSkipAuthentication(String path) {
        return AUTH_WHITELIST.stream()
                .anyMatch(path::startsWith);
    }
}