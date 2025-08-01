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

package com.datasophon.common.dto;

import java.util.Date;

/**
 * 认证令牌数据传输对象
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-01
 */
public record AuthTokenDTO(
        Long id,
        Integer userId,
        String token,
        String refreshToken,
        String tokenType,
        String clientIp,
        String userAgent,
        Date issuedAt,
        Date expiresAt,
        Date lastAccessTime,
        Boolean isRevoked,
        String revokedReason,
        Date createdAt,
        Date updatedAt) {

    /**
     * 创建新的认证令牌DTO
     */
    public static AuthTokenDTO create(Integer userId, String token, String refreshToken,
            String clientIp, String userAgent, Date expiresAt) {
        Date now = new Date();
        return new AuthTokenDTO(
                null,
                userId,
                token,
                refreshToken,
                "Bearer",
                clientIp,
                userAgent,
                now,
                expiresAt,
                now,
                false,
                null,
                now,
                now);
    }

    /**
     * 撤销令牌
     */
    public AuthTokenDTO revoke(String reason) {
        return new AuthTokenDTO(
                this.id,
                this.userId,
                this.token,
                this.refreshToken,
                this.tokenType,
                this.clientIp,
                this.userAgent,
                this.issuedAt,
                this.expiresAt,
                this.lastAccessTime,
                true,
                reason,
                this.createdAt,
                new Date());
    }

    /**
     * 更新访问时间
     */
    public AuthTokenDTO updateAccessTime() {
        return new AuthTokenDTO(
                this.id,
                this.userId,
                this.token,
                this.refreshToken,
                this.tokenType,
                this.clientIp,
                this.userAgent,
                this.issuedAt,
                this.expiresAt,
                new Date(),
                this.isRevoked,
                this.revokedReason,
                this.createdAt,
                new Date());
    }

    /**
     * 检查令牌是否过期
     */
    public boolean isExpired() {
        return expiresAt != null && expiresAt.before(new Date());
    }

    /**
     * 检查令牌是否有效
     */
    public boolean isValid() {
        return !Boolean.TRUE.equals(isRevoked) && !isExpired();
    }
}