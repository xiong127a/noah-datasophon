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

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Table;

/**
 * 框架服务实体类
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-13
 */
@Table("t_ddh_frame_service")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class FrameServiceEntity extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /*
      主键
     */

    /**
     * 框架id
     */
    private Long frameId;
    /**
     * 服务名称
     */
    private String serviceName;

    private String label;
    /**
     * 服务版本
     */
    private String serviceVersion;
    /**
     * 服务描述
     */
    private String serviceDesc;

    /**
     * 压缩包名称
     */
    private String packageName;

    /**
     * 依赖角色
     */
    private String dependencies;

    /**
     * 角色所有配置json，对应各角色定义的service_ddl.json文件
     */
    private String serviceJson;

    private String serviceJsonMd5;

    /**
     * 所有配置参数详情数组
     */
    private String serviceConfig;

    private String frameCode;

    /**
     * 配置文件详情jsonObject -> 该文件包含参数详情jsonArray
     */
    private String configFileJson;

    private String configFileJsonMd5;

    /**
     * 解压文件名
     */
    private String decompressPackageName;

    @Column(ignore = true)
    private Boolean installed;

    private Integer sortNum;

    @Column(ignore = true)
    private Boolean isRequired;

}
