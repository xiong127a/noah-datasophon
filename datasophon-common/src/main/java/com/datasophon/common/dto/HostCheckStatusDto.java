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

import com.datasophon.common.model.HostInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 主机检查状态数据传输对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HostCheckStatusDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主机信息列表
     */
    private List<HostInfo> hosts;

    /**
     * 检查是否完成
     */
    private boolean completed;

    /**
     * 总主机数
     */
    private int totalHosts;

    /**
     * 检查完成的主机数
     */
    private int completedHosts;

    /**
     * 检查失败的主机数
     */
    private int failedHosts;

    /**
     * 检查成功的主机数
     */
    private int successHosts;
}