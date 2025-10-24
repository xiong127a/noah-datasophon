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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Parcel存储库DTO
 * 
 * @author datasophon
 * @date 2025-10-24
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ParcelRepositoryDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private Long id;

    /**
     * 存储库名称
     */
    private String repoName;

    /**
     * 存储库类型：local/http
     */
    private String repoType;

    /**
     * 存储库地址
     */
    private String repoUrl;

    /**
     * 框架代码（如 DDP-1.2.1）
     */
    private String frameCode;

    /**
     * 描述
     */
    private String description;

    /**
     * 是否默认存储库：0-否，1-是
     */
    private Integer isDefault;

    /**
     * 状态：0-禁用，1-启用
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 判断是否为本地存储库
     */
    public boolean isLocal() {
        return "local".equalsIgnoreCase(this.repoType);
    }

    /**
     * 判断是否为HTTP存储库
     */
    public boolean isHttp() {
        return "http".equalsIgnoreCase(this.repoType);
    }

    /**
     * 判断是否为默认存储库
     */
    public boolean isDefaultRepo() {
        return this.isDefault != null && this.isDefault == 1;
    }

    /**
     * 判断是否启用
     */
    public boolean isEnabled() {
        return this.status != null && this.status == 1;
    }
}

