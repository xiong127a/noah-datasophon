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

import com.datasophon.common.enums.ValidationStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 主机校验状态VO（仅内存存储）
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-28
 */
@Data
public class HostValidationStatusVO {
    private String hostIp;
    private String hostname;
    private ValidationStatus overallStatus;
    private List<CheckItemStatusVO> checkItems;
    private List<String> logs;                    // 实时日志
    private LocalDateTime lastUpdateTime;
    private boolean canRepair;                    // 是否可修复
    private boolean paused;                       // 是否暂停
    private boolean cancelled;                    // 是否取消

    public HostValidationStatusVO() {
        this.checkItems = new CopyOnWriteArrayList<>();
        this.logs = new CopyOnWriteArrayList<>();
        this.lastUpdateTime = LocalDateTime.now();
        this.paused = false;
        this.cancelled = false;
    }

    public HostValidationStatusVO(String hostIp, String hostname, ValidationStatus overallStatus) {
        this();
        this.hostIp = hostIp;
        this.hostname = hostname;
        this.overallStatus = overallStatus;
    }
}
