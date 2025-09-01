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

import java.time.LocalDateTime;

/**
 * 告警组数据传输对象
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-14
 */
public record AlertGroupDTO(
        Long id,
        String alertGroupName,
        String alertGroupCategory,
        Long clusterId,
        Integer alertQuotaNum,
        LocalDateTime createTime,
        LocalDateTime updateTime,
        String createBy,
        String updateBy) {

    /**
     * 创建新的AlertGroupDTO
     */
    public static AlertGroupDTO create(String alertGroupName, String alertGroupCategory, Long clusterId) {
        var now = LocalDateTime.now();
        return new AlertGroupDTO(
                null,
                alertGroupName,
                alertGroupCategory,
                clusterId,
                0,
                now,
                now,
                null,
                null);
    }

}