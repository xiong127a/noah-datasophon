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
import java.util.List;

/**
 * 通知组数据传输对象
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-01
 */
public record NoticeGroupDTO(
        Long id,
        Long clusterId,
        String noticeGroupName,
        LocalDateTime createTime,
        List<Long> userIds) {

    /**
     * 创建新的NoticeGroupDTO
     */
    public static NoticeGroupDTO create(String noticeGroupName, Long clusterId, List<Long> userIds) {
        return new NoticeGroupDTO(
                null,
                clusterId,
                noticeGroupName,
                LocalDateTime.now(),
                userIds);
    }

    /**
     * 更新用户ID列表
     */
    public NoticeGroupDTO withUserIds(List<Long> userIds) {
        return new NoticeGroupDTO(
                this.id,
                this.clusterId,
                this.noticeGroupName,
                this.createTime,
                userIds);
    }

    /**
     * 设置集群ID
     */
    public NoticeGroupDTO withClusterId(Long clusterId) {
        return new NoticeGroupDTO(
                this.id,
                clusterId,
                this.noticeGroupName,
                this.createTime,
                this.userIds);
    }
}