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

package com.datasophon.common.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 集群告警表达式数据传输对象
 * 用于服务间数据传输
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
public record ClusterAlertExpressionDTO(
        Long id,
        String name,
        String expr,
        String serviceCategory,
        String valueType,
        String isPredefined,
        String state,
        String isDelete,
        LocalDateTime createTime,
        LocalDateTime updateTime) implements Serializable {
}