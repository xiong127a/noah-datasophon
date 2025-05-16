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

import java.io.Serializable;
import java.util.Date;

import lombok.Data;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;

/**
 * 配置版本详情表
 *
 * @author datasophon
 */
@Data
@TableName("t_ddh_config_version_info")
public class ConfigVersionInfoEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 版本号
     */
    @TableId(type = IdType.INPUT)
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
    private Date editTime;

    /**
     * 是否当前使用版本
     */
    private Boolean isCurrent;

    /**
     * 服务代码
     */
    private String serviceCode;
}