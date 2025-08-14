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

package com.datasophon.common.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;

/**
 * 令牌提供者接口
 * 定义令牌管理的核心操作
 */
public interface TokenProvider {

    /**
     * 创建JWT访问令牌
     * 
     * @param authentication 认证对象
     * @return JWT令牌字符串
     */
    String createToken(Authentication authentication);

    /**
     * 创建刷新令牌
     * 
     * @param userId 用户ID
     * @return 刷新令牌字符串
     */
    String createRefreshToken(String userId);

    /**
     * 从令牌中获取认证信息
     * 
     * @param token JWT令牌
     * @return 认证对象
     */
    Authentication getAuthentication(String token);

    /**
     * 验证令牌的有效性
     * 
     * @param token JWT令牌
     * @return 如果令牌有效返回true
     */
    boolean validateToken(String token);

    /**
     * 从请求中解析JWT令牌
     * 
     * @param request HTTP请求
     * @return 令牌字符串，如果没有找到返回null
     */
    String resolveToken(HttpServletRequest request);

    /**
     * 从令牌中获取用户ID
     * 
     * @param token JWT令牌
     * @return 用户ID
     */
    String getUserIdFromToken(String token);

    /**
     * 获取令牌过期时间
     * 
     * @param token JWT令牌
     * @return 过期时间
     */
    LocalDateTime getExpirationDateFromToken(String token);

    /**
     * 从令牌中获取Claims
     * 
     * @param token JWT令牌
     * @return Claims对象
     */
    Claims getClaimsFromToken(String token);
}