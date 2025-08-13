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
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * 配置版本详情表
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
@Table("t_ddh_config_version_info")
public class ConfigVersionInfoEntity extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 版本号（业务主键）
     */
    private Integer version;

    /**
     * 引用类型(SERVICE/ROLE_GROUP)
     */
    private String refType;

    /**
     * 关联对象ID
     */
    private Integer refId;

    /**
     * 版本描述
     */
    private String description;

    /**
     * 编辑者
     */
    private String editor;

    /**
     * 编辑时间
     */
    private LocalDateTime editTime;

    /**
     * 是否当前使用版本
     */
    private Boolean isCurrent;

    /**
     * 服务代码
     */
    private String serviceCode;

    private Integer userId;
}