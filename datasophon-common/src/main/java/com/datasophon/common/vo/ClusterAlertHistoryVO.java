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
import java.util.Date;

/**
 * 集群告警历史视图对象
 * 用于前端展示
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
public record ClusterAlertHistoryVO(
        Integer id,
        String alertGroupName,
        String alertTargetName,
        String alertInfo,
        String alertAdvice,
        String hostname,
        Integer alertLevel,
        String alertLevelText,
        Integer isEnabled,
        String isEnabledText,
        Integer serviceRoleInstanceId,
        Integer serviceInstanceId,
        Date createTime,
        String createTimeFormatted,
        Date updateTime,
        String updateTimeFormatted,
        Long clusterId) implements Serializable {
}