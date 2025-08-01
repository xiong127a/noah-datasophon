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

package com.datasophon.api.service;

import com.datasophon.dao.entity.AuthTokenEntity;
import com.datasophon.dao.entity.UserInfoEntity;
import com.datasophon.common.dto.AuthTokenDTO;
import com.datasophon.common.model.PageResult;
import com.mybatisflex.core.service.IService;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Date;
import java.util.List;

/**
 * JWT认证令牌服务接口
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-01
 */
public interface AuthTokenService extends IService<AuthTokenEntity> {

    /**
     * 创建新的认证令牌
     * 
     * @param user         用户信息
     * @param token        JWT令牌
     * @param refreshToken 刷新令牌
     * @param request      HTTP请求
     * @param expiresAt    过期时间
     * @return 认证令牌DTO
     */
    AuthTokenDTO createToken(UserInfoEntity user, String token, String refreshToken,
            HttpServletRequest request, Date expiresAt);

    /**
     * 根据令牌获取认证信息
     * 
     * @param token JWT令牌
     * @return 认证令牌DTO
     */
    AuthTokenDTO getByToken(String token);

    /**
     * 更新令牌访问时间
     * 
     * @param tokenId 令牌ID
     * @return 是否成功更新
     */
    boolean updateAccessTime(Long tokenId);

    /**
     * 撤销令牌
     * 
     * @param tokenId 令牌ID
     * @param reason  撤销原因
     * @return 是否成功撤销
     */
    boolean revokeToken(Long tokenId, String reason);

    /**
     * 撤销用户的所有令牌
     * 
     * @param userId 用户ID
     * @param reason 撤销原因
     */
    void revokeAllUserTokens(Integer userId, String reason);

    /**
     * 清理过期的令牌
     * 
     * @return 清理的令牌数量
     */
    int cleanupExpiredTokens();

    /**
     * 创建刷新令牌
     * 
     * @param userId 用户ID
     * @return 刷新令牌
     */
    String createRefreshToken(String userId);

    /**
     * 通过刷新令牌获取新的访问令牌
     * 
     * @param refreshToken 刷新令牌
     * @param request      HTTP请求
     * @return 新的访问令牌
     */
    String refreshAccessToken(String refreshToken, HttpServletRequest request);

    /**
     * 获取用户的令牌分页列表
     * 
     * @param userId   用户ID
     * @param page     页码
     * @param pageSize 页面大小
     * @return 令牌分页结果
     */
    PageResult<AuthTokenDTO> getUserTokenList(Integer userId, Integer page, Integer pageSize);

    /**
     * 获取用户的有效令牌列表
     * 
     * @param userId 用户ID
     * @return 有效令牌列表
     */
    List<AuthTokenDTO> getValidTokensByUserId(Integer userId);

    /**
     * 根据ID获取令牌
     * 
     * @param id 令牌ID
     * @return 认证令牌DTO
     */
    AuthTokenDTO getAuthTokenById(Long id);

    /**
     * 校验令牌是否有效
     * 
     * @param token JWT令牌
     * @return 是否有效
     */
    boolean validateToken(String token);
}