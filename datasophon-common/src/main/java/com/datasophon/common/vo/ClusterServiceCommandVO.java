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

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 集群服务命令视图对象
 * 用于前端展示
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
public record ClusterServiceCommandVO(
        Long commandId,
        String createBy,
        LocalDateTime createTime,
        String createTimeFormatted,
        String commandName,
        Integer commandState,
        String commandStateText,
        Integer commandStateCode,
        Long commandProgress,
        Long clusterId,
        String serviceName,
        Integer commandType,
        String commandTypeText,
        String durationTime,
        LocalDateTime endTime,
        String endTimeFormatted,
        Long serviceInstanceId,
        LocalDateTime updateTime,
        String updateTimeFormatted,
        String updateBy) implements Serializable {
}