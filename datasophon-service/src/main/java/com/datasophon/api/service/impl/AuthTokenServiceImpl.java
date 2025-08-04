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

package com.datasophon.api.service.impl;

import cn.hutool.core.util.StrUtil;
import com.datasophon.api.converter.AuthTokenConverter;
import com.datasophon.api.security.PersistentTokenManager;
import com.datasophon.api.service.AuthTokenService;
import com.datasophon.api.service.UserInfoService;
import com.datasophon.api.utils.HttpUtils;
import com.datasophon.common.dto.AuthTokenDTO;
import com.datasophon.common.model.PageResult;
import com.datasophon.common.utils.CollectionUtils;
import com.datasophon.dao.entity.AuthTokenEntity;
import com.datasophon.dao.entity.UserInfoEntity;
import com.datasophon.dao.mapper.AuthTokenMapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * JWT认证令牌服务实现类
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-01
 */
@Service("authTokenService")
public class AuthTokenServiceImpl extends ServiceImpl<AuthTokenMapper, AuthTokenEntity> implements AuthTokenService {

    private static final Logger logger = LoggerFactory.getLogger(AuthTokenServiceImpl.class);

    /**
     * 每个用户最多可同时拥有的有效令牌数量
     */
    private static final int MAX_TOKENS_PER_USER = 5;

    @Autowired
    private PersistentTokenManager tokenProvider;

    @Autowired
    private UserInfoService userInfoService;

    @Autowired
    private AuthTokenConverter authTokenConverter;

    /**
     * 创建新的认证令牌
     *
     * @param user         用户信息
     * @param token        JWT令牌
     * @param refreshToken 刷新令牌
     * @param request      HTTP请求
     * @param expiresAt    过期时间
     * @return 认证令牌实体
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AuthTokenDTO createToken(UserInfoEntity user, String token, String refreshToken,
            HttpServletRequest request, Date expiresAt) {
        // 清理超出限制的旧令牌
        this.getMapper().cleanupOldTokens(user.getId(), MAX_TOKENS_PER_USER);

        // 获取客户端信息
        String ip = HttpUtils.getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");

        // 创建新的令牌实体
        AuthTokenEntity authToken = new AuthTokenEntity();
        authToken.setUserId(user.getId());
        authToken.setToken(token);
        authToken.setRefreshToken(refreshToken);
        authToken.setTokenType("Bearer");
        authToken.setClientIp(ip);
        authToken.setUserAgent(userAgent);
        authToken.setIssuedAt(new Date());
        authToken.setExpiresAt(expiresAt);
        authToken.setLastAccessTime(new Date());
        authToken.setIsRevoked(false);
        authToken.setCreatedAt(new Date());
        authToken.setUpdatedAt(new Date());

        // 保存到数据库
        this.save(authToken);
        logger.debug("Created auth token for user: {}, IP: {}", user.getId(), ip);
        return authTokenConverter.entityToDto(authToken);
    }

    /**
     * 根据令牌获取认证信息
     *
     * @param token JWT令牌
     * @return 认证令牌DTO
     */
    @Override
    public AuthTokenDTO getByToken(String token) {
        if (StrUtil.isBlank(token)) {
            return null;
        }

        // 移除Bearer前缀
        String actualToken = token;
        if (token.startsWith("Bearer ")) {
            actualToken = token.substring(7);
        }

        AuthTokenEntity entity = this.getMapper().findByToken(actualToken);
        return entity != null ? authTokenConverter.entityToDto(entity) : null;
    }

    /**
     * 更新令牌访问时间
     *
     * @param tokenId 令牌ID
     * @return 是否成功更新
     */
    @Override
    public boolean updateAccessTime(Long tokenId) {
        return this.getMapper().updateLastAccessTime(tokenId, new Date());
    }

    /**
     * 撤销令牌
     *
     * @param tokenId 令牌ID
     * @param reason  撤销原因
     * @return 是否成功撤销
     */
    @Override
    public boolean revokeToken(Long tokenId, String reason) {
        return this.getMapper().revokeToken(tokenId, reason);
    }

    /**
     * 撤销用户的所有令牌
     *
     * @param userId 用户ID
     * @param reason 撤销原因
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revokeAllUserTokens(Integer userId, String reason) {
        List<AuthTokenEntity> tokens = this.getMapper().findValidTokensByUserId(userId);
        tokens.forEach(token -> this.getMapper().revokeToken(token.getId(), reason));
        logger.debug("Revoked all tokens for user: {}, reason: {}", userId, reason);
    }

    /**
     * 清理过期的令牌
     *
     * @return 清理的令牌数量
     */
    @Override
    public int cleanupExpiredTokens() {
        // 删除过期超过7天的令牌
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -7);
        Date cutoffDate = cal.getTime();

        int count = this.getMapper().deleteExpiredTokens(cutoffDate);
        logger.debug("Cleaned up {} expired tokens", count);
        return count;
    }

    /**
     * 创建刷新令牌
     *
     * @param userId 用户ID
     * @return 刷新令牌
     */
    @Override
    public String createRefreshToken(String userId) {
        // 直接调用JwtTokenProvider的方法
        return tokenProvider.createRefreshToken(userId);
    }

    /**
     * 通过刷新令牌获取新的访问令牌
     *
     * @param refreshToken 刷新令牌
     * @param request      HTTP请求
     * @return 新的访问令牌
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String refreshAccessToken(String refreshToken, HttpServletRequest request) {
        try {
            // 验证刷新令牌
            if (!tokenProvider.validateToken(refreshToken)) {
                logger.error("Invalid refresh token");
                return null;
            }

            // 获取用户ID或用户名
            String userIdentifier = tokenProvider.getUserIdFromToken(refreshToken);
            if (StrUtil.isBlank(userIdentifier)) {
                logger.error("Refresh token has no user identifier");
                return null;
            }

            UserInfoEntity user;
            try {
                // 尝试将标识符解析为整数ID
                Integer userId = Integer.parseInt(userIdentifier);
                user = userInfoService.getById(userId);
            } catch (NumberFormatException e) {
                // 如果不是整数，则尝试作为用户名处理
                user = userInfoService.getUserEntityByUsername(userIdentifier);
            }

            if (user == null) {
                logger.error("User not found for identifier: {}", userIdentifier);
                return null;
            }

            // 创建认证对象
            Collection<GrantedAuthority> authorities = Collections
                    .singletonList(new SimpleGrantedAuthority("ROLE_USER"));
            User principal = new User(user.getUsername(), "", authorities);
            Authentication authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);

            // 生成新的访问令牌
            String accessToken = tokenProvider.createToken(authentication, request);

            // 获取过期时间
            Date expiresAt = tokenProvider.getExpirationDateFromToken(accessToken);

            // 记录令牌到数据库
            createToken(user, accessToken, refreshToken, request, expiresAt);

            return accessToken;
        } catch (Exception e) {
            logger.error("Failed to refresh access token", e);
            return null;
        }
    }

    @Override
    public PageResult<AuthTokenDTO> getUserTokenList(Integer userId, Integer page, Integer pageSize) {
        // 查询用户的令牌列表
        List<AuthTokenEntity> tokenList = getMapper().selectByUserIdWithPagination(userId, (page - 1) * pageSize,
                pageSize);

        long total = getMapper().countByUserId(userId);

        if (CollectionUtils.isEmpty(tokenList)) {
            return PageResult.empty(page, pageSize);
        }

        List<AuthTokenDTO> dtoList = tokenList.stream()
                .map(authTokenConverter::entityToDto)
                .toList();

        return PageResult.of(dtoList, total, page, pageSize);
    }

    @Override
    public AuthTokenDTO getAuthTokenById(Long id) {
        AuthTokenEntity entity = this.getById(id);
        return entity != null ? authTokenConverter.entityToDto(entity) : null;
    }

    @Override
    public boolean validateToken(String token) {
        if (StrUtil.isBlank(token)) {
            return false;
        }

        AuthTokenDTO tokenDTO = getByToken(token);
        return tokenDTO != null && tokenDTO.isValid();
    }

    @Override
    public List<AuthTokenDTO> getValidTokensByUserId(Integer userId) {
        if (userId == null) {
            return List.of();
        }

        try {
            // 查询用户的有效令牌（未撤销且未过期）
            List<AuthTokenEntity> validTokens = getMapper().findValidTokensByUserId(userId);

            return authTokenConverter.entityListToDtoList(validTokens);
        } catch (Exception e) {
            logger.error("获取用户有效令牌失败: userId={}", userId, e);
            return List.of();
        }
    }

    @Override
    public int cleanupExcessiveTokens(Integer userId, int maxTokens) {
        if (userId == null || maxTokens <= 0) {
            return 0;
        }

        try {
            // 获取用户当前有效令牌
            List<AuthTokenDTO> validTokens = getValidTokensByUserId(userId);

            if (validTokens.size() <= maxTokens) {
                logger.debug("用户 {} 的令牌数量 {} 未超过限制 {}", userId, validTokens.size(), maxTokens);
                return 0; // 无需清理
            }

            // 计算需要清理的数量
            int deleteCount = validTokens.size() - maxTokens;
            logger.info("用户 {} 的令牌数量 {} 超过限制 {}，需要清理 {} 个",
                    userId, validTokens.size(), maxTokens, deleteCount);

            // 按发布时间排序，删除最旧的令牌（保留最新的maxTokens个）
            List<Long> tokenIdsToDelete = validTokens.stream()
                    .sorted(Comparator.comparing(AuthTokenDTO::issuedAt)) // 按发布时间升序
                    .limit(deleteCount)
                    .map(AuthTokenDTO::id)
                    .toList();

            // 批量撤销过期令牌
            int deletedCount = 0;
            for (Long tokenId : tokenIdsToDelete) {
                if (revokeToken(tokenId, "超出最大令牌数量限制，自动清理")) {
                    deletedCount++;
                }
            }

            logger.info("用户 {} 已清理 {} 个过期令牌", userId, deletedCount);
            return deletedCount;

        } catch (Exception e) {
            logger.error("清理用户过多令牌失败: userId={}, maxTokens={}", userId, maxTokens, e);
            return 0;
        }
    }

}