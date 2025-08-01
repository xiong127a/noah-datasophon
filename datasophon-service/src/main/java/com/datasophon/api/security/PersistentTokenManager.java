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

import com.datasophon.api.service.AuthTokenService;
import com.datasophon.common.security.JwtTokenProviderBase;
import com.datasophon.dao.entity.AuthTokenEntity;
import com.datasophon.dao.entity.UserInfoEntity;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 持久化令牌管理器
 * 扩展基础JWT令牌提供者，添加令牌持久化和管理功能
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-01
 */
@Component("tokenProvider")
public class PersistentTokenManager extends JwtTokenProviderBase {

    private static final Logger logger = LoggerFactory.getLogger(PersistentTokenManager.class);

    @Value("${datasophon.security.max-tokens-per-user:5}")
    private int maxTokensPerUser;

    @Autowired
    private AuthTokenService authTokenService;

    /**
     * 创建令牌并保存到数据库
     * 
     * @param authentication 认证对象
     * @param request        HTTP请求
     * @return JWT令牌
     */
    public String createToken(Authentication authentication, HttpServletRequest request) {
        String token = super.createToken(authentication);

        // 从认证对象中获取用户名
        Object principal = authentication.getPrincipal();
        String username = null;
        if (principal instanceof User) {
            username = ((User) principal).getUsername();
        } else if (principal instanceof String) {
            username = (String) principal;
        }

        if (username == null) {
            logger.error("无法从认证对象中获取用户名");
            return token;
        }

        // 生成刷新令牌
        String refreshToken = createRefreshToken(username);

        // 获取令牌过期时间
        Date validity = getExpirationDateFromToken(token);

        // 保存令牌到数据库
        try {
            // 根据用户名查询用户信息 - 暂时简化处理
            // TODO: 需要根据实际的UserInfoService方法签名调整
            logger.debug("尝试保存令牌到数据库，用户: {}", username);

            // 简化处理：直接创建基本的User entity
            UserInfoEntity user = new UserInfoEntity();
            user.setUsername(username);

            createTokenRecord(user, token, refreshToken, request, validity);
        } catch (Exception e) {
            logger.error("保存令牌到数据库失败", e);
        }

        return token;
    }

    /**
     * 在数据库中创建令牌记录
     * 
     * @param user         用户信息
     * @param token        访问令牌
     * @param refreshToken 刷新令牌
     * @param request      HTTP请求
     * @param expiresAt    过期时间
     * @return 令牌实体
     */
    private AuthTokenEntity createTokenRecord(UserInfoEntity user, String token, String refreshToken,
            HttpServletRequest request, Date expiresAt) {
        try {
            // 清理用户过多的令牌，保持在限制数量内
            int cleanupCount = authTokenService.cleanupExcessiveTokens(user.getId(), maxTokensPerUser);
            if (cleanupCount > 0) {
                logger.info("用户 {} 自动清理了 {} 个过期令牌", user.getUsername(), cleanupCount);
            }

            // 创建新令牌记录 - 使用Service层
            AuthTokenEntity authToken = new AuthTokenEntity();
            authToken.setUserId(user.getId());
            authToken.setToken(token);
            authToken.setRefreshToken(refreshToken);
            authToken.setTokenType("Bearer");
            authToken.setExpiresAt(expiresAt);

            Date now = new Date();
            authToken.setIssuedAt(now);
            authToken.setLastAccessTime(now);
            authToken.setCreatedAt(now);
            authToken.setUpdatedAt(now);
            authToken.setIsRevoked(false);

            // 记录客户端信息
            if (request != null) {
                authToken.setClientIp(request.getRemoteAddr());
                authToken.setUserAgent(request.getHeader("User-Agent"));
            }

            // 通过Service层保存
            return authTokenService.save(authToken) ? authToken : null;

        } catch (Exception e) {
            logger.error("保存令牌记录失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 验证令牌，包括检查数据库中的状态
     * 
     * @param token JWT令牌
     * @return 如果令牌有效返回true
     */
    @Override
    public boolean validateToken(String token) {
        if (!super.validateToken(token)) {
            return false;
        }

        // 通过Service层检查数据库中的令牌状态 - 暂时简化处理
        try {
            Object tokenDto = authTokenService.getByToken(token);
            if (tokenDto == null) {
                logger.error("JWT令牌在数据库中不存在");
                return false;
            }

            // TODO: 需要根据实际的AuthTokenDTO结构调整
            logger.debug("令牌验证通过: {}", token);

            // 通过Service层更新最后访问时间 - 暂时注释
            // authTokenService.updateAccessTime(tokenDto.getId());
        } catch (Exception e) {
            logger.error("验证令牌状态失败", e);
            return false;
        }

        return true;
    }

}