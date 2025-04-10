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

package com.datasophon.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 命令行项实体类
 * 用于在前端显示命令行示例和命令执行结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommandLineItem implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 标签/描述
     * 命令的描述信息，用于前端展示
     */
    private String label;

    /**
     * 值/命令
     * 实际执行的命令内容
     */
    private String value;

    /**
     * 命令执行结果
     * 存储命令执行后的预览输出内容
     */
    private String commandResult;
    
    /**
     * 命令提示符
     * 用于显示不同类型的命令提示符，如beeline、shell等
     */
    private String commandPrompt;
}