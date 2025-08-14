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

package com.datasophon.dao.entity;

import com.datasophon.dao.entity.base.BaseEntity;
import java.io.Serial;
import java.time.LocalDateTime;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import com.mybatisflex.annotation.Table;

/**
 * JWT认证令牌实体类
 * 用于存储用户登录后生成的JWT令牌及相关信息
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-13
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Table("t_ddh_auth_token")
public class AuthTokenEntity extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /*
      主键ID
     */


    /**
     * 关联的用户ID
     */
    private Long userId;

    /**
     * JWT访问令牌
     */
    private String token;

    /**
     * 刷新令牌，用于获取新的访问令牌
     */
    private String refreshToken;

    /**
     * 令牌类型，通常为Bearer
     */
    private String tokenType;

    /**
     * 客户端IP地址
     */
    private String clientIp;

    /**
     * 客户端浏览器信息
     */
    private String userAgent;

    /**
     * 令牌颁发时间
     */
    private LocalDateTime issuedAt;

    /**
     * 令牌过期时间
     */
    private LocalDateTime expiresAt;

    /**
     * 最后访问时间
     */
    private LocalDateTime lastAccessTime;

    /**
     * 是否已被撤销
     * false - 有效
     * true - 已撤销
     */
    private Boolean isRevoked;

    /**
     * 撤销原因
     */
    private String revokedReason;




}