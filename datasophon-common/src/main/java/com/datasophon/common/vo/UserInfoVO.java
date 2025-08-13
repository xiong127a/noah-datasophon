/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.datasophon.common.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户信息视图对象
 * 用于前端展示，不包含敏感信息
 * 
 * @author DataSophon
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 邮箱 (脱敏处理)
     */
    private String email;

    /**
     * 手机号 (脱敏处理)
     */
    private String phone;

    /**
     * 创建时间 (格式化字符串)
     */
    private String createTime;

    /**
     * 用户类型描述
     */
    private String userTypeDesc;

    /**
     * 个人简介
     */
    private String bio;

    /**
     * 最后登录时间 (格式化字符串)
     */
    private String lastLoginTime;

    /**
     * 用户头像URL
     */
    private String avatar;

    /**
     * 是否在线状态
     */
    private Boolean online;
}