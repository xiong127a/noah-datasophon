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

package com.datasophon.common.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 集群用户视图对象
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClusterUserVO {

    /**
     * 用户ID
     */
    private Integer id;

    /**
     * 集群ID
     */
    private Integer clusterId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 主组名称
     */
    private String mainGroup;

    /**
     * 其他组名称（逗号分隔）
     */
    private String otherGroups;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 格式化的创建时间
     */
    private String createTimeFormatted;
}