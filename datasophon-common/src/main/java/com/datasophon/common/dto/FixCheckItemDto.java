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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 修复检查项确认信息数据传输对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FixCheckItemDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 检查项ID
     */
    private Integer itemId;

    /**
     * 检查项名称
     */
    private String itemName;

    /**
     * 修复操作描述
     */
    private String fixDescription;

    /**
     * 是否需要确认
     */
    private boolean needConfirm;

    /**
     * 风险级别 (LOW, MEDIUM, HIGH)
     */
    private String riskLevel;

    /**
     * 确认提示信息
     */
    private String confirmMessage;
}