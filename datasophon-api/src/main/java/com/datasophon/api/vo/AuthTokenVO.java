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

package com.datasophon.api.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Date;

/**
 * 认证令牌视图对象
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-01
 */
public record AuthTokenVO(
        Long id,
        Integer userId,
        String username,
        String tokenType,
        String clientIp,
        String userAgent,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date issuedAt,
        String issuedAtFormatted,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date expiresAt,
        String expiresAtFormatted,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date lastAccessTime,
        String lastAccessTimeFormatted,
        Boolean isRevoked,
        String status,
        String revokedReason,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date createdAt,
        String createdAtFormatted,
        Long remainingTimeSeconds,
        String remainingTimeFormatted) {
}