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

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * 告警组视图对象
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-14
 */
public record AlertGroupVO(
        Long id,
        String alertGroupName,
        String alertGroupCategory,
        Long clusterId,
        String clusterName,
        Integer alertQuotaNum,
        String alertQuotaNumFormatted,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime createTime,
        String createTimeFormatted,
        LocalDateTime updateTime,
        String updateTimeFormatted,
        String createBy,
        String updateBy) {

    /**
     * 创建简单的AlertGroupVO
     */
    public static AlertGroupVO simple(Long id, String alertGroupName) {
        return new AlertGroupVO(
                id,
                alertGroupName,
                null,
                null,
                null,
                0,
                "0个指标",
                null,
                null,
                null,
                null,
                null,
                null);
    }
}