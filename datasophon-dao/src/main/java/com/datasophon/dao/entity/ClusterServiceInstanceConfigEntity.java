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

import com.mybatisflex.annotation.Table;

/**
 * 集群服务实例配置实体类
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-13
 */
@Table("t_ddh_cluster_service_instance_config")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class ClusterServiceInstanceConfigEntity extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;


    /**
     * 服务角色实例id
     */
    private Integer serviceId;
    /*
      创建时间
     */

    /**
     * 配置json
     */
    private String configJson;
    /*
      更新时间
     */

    /**
     * 配置json md5
     */
    private String configJsonMd5;
    /**
     * 配置json版本
     */
    private Integer configVersion;
    /**
     *
     */
    private Long clusterId;

    private String configFileJson;

    private String configFileJsonMd5;

}
