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

import cn.hutool.extra.servlet.ServletUtil;
import com.datasophon.common.security.JwtTokenProviderBase;
import com.datasophon.dao.entity.AuthTokenEntity;
import com.datasophon.dao.entity.UserInfoEntity;
import com.datasophon.dao.mapper.AuthTokenMapper;
import com.datasophon.api.service.UserInfoService;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.UUID;

/**
 * 持久化令牌管理器
 * 扩展基础JWT令牌提供者，添加令牌持久化和管理功能
 */
@Component("tokenProvider")
public class PersistentTokenManager extends JwtTokenProviderBase {

    private static final Logger logger = LoggerFactory.getLogger(PersistentTokenManager.class);

    @Value("${datasophon.security.max-tokens-per-user:5}")
    private int maxTokensPerUser;

    @Autowired
    private AuthTokenMapper authTokenMapper;

    @Autowired
    private UserInfoService userInfoService;

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
        }

        // 生成刷新令牌
        String refreshToken = createRefreshToken(username);

        // 获取令牌过期时间
        Date validity = getExpirationDateFromToken(token);

        // 保存令牌到数据库
        try {
            // 根据用户名查询用户信息
            UserInfoEntity user = userInfoService.getUserByUsername(username);

            if (user != null) {
                createToken(user, token, refreshToken, request, validity);
            } else {
                logger.error("保存令牌失败：未找到用户信息 [{}]", username);
            }
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
    public AuthTokenEntity createToken(UserInfoEntity user, String token, String refreshToken,
            HttpServletRequest request, Date expiresAt) {
        try {
            // 清理过多的令牌
            int tokenCount = authTokenMapper.countValidTokensByUserId(user.getId());
            if (tokenCount >= maxTokensPerUser) {
                // 删除最旧的令牌
                authTokenMapper.deleteOldestTokens(user.getId(), tokenCount - maxTokensPerUser + 1);
            }
            // 创建新令牌记录
            AuthTokenEntity authToken = new AuthTokenEntity();
            // 设置主键id，使用UUID生成唯一标识符
            authToken.setId(UUID.randomUUID().toString());
            authToken.setUserId(user.getId());
            // 用户名字段在两个实体中不同步，需要从用户实体获取
            // authToken.setUserName(user.getUsername()); // AuthTokenEntity没有这个字段
            authToken.setToken(token);
            authToken.setRefreshToken(refreshToken);
            authToken.setTokenType("Bearer"); // 设置令牌类型
            authToken.setExpiresAt(expiresAt);
            authToken.setIssuedAt(new Date());
            authToken.setLastAccessTime(new Date());
            authToken.setIsRevoked(false);

            // 添加创建和更新时间
            Date now = new Date();
            authToken.setCreatedAt(now);
            authToken.setUpdatedAt(now);

            // 记录客户端信息
            if (request != null) {
                authToken.setClientIp(request.getRemoteAddr());
                authToken.setUserAgent(request.getHeader("User-Agent"));
            }

            authTokenMapper.insert(authToken);
            return authToken;
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

        // 检查数据库中的令牌状态
        AuthTokenEntity tokenEntity = authTokenMapper.getByToken(token);
        if (tokenEntity == null) {
            logger.error("JWT令牌在数据库中不存在");
            return false;
        }

        if (Boolean.TRUE.equals(tokenEntity.getIsRevoked())) {
            logger.error("JWT令牌已被撤销: {}", tokenEntity.getRevokedReason());
            return false;
        }

        // 更新最后访问时间
        updateAccessTime(tokenEntity.getId());

        return true;
    }

    /**
     * 更新令牌的最后访问时间
     * 
     * @param tokenId 令牌ID
     * @return 是否更新成功
     */
    public boolean updateAccessTime(String tokenId) {
        return authTokenMapper.updateAccessTime(tokenId, new Date());
    }

    /**
     * 撤销令牌
     * 
     * @param tokenId 令牌ID
     * @param reason  撤销原因
     * @return 是否成功撤销
     */
    public boolean revokeToken(String tokenId, String reason) {
        return authTokenMapper.revokeToken(tokenId, reason);
    }

    /**
     * 根据令牌获取令牌记录
     * 
     * @param token 令牌字符串
     * @return 令牌实体
     */
    public AuthTokenEntity getByToken(String token) {
        return authTokenMapper.getByToken(token);
    }

    /**
     * 为刷新令牌创建一个新的访问令牌
     * 
     * @param userId  用户ID
     * @param request HTTP请求
     * @return 新的访问令牌
     */
    public String createTokenForRefresh(Integer userId, HttpServletRequest request) {
        Collection<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
        User principal = new User(userId.toString(), "", authorities);
        Authentication authentication = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                principal, null, authorities);
        return createToken(authentication, request);
    }
}