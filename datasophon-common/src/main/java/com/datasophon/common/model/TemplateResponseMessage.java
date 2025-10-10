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

import lombok.Data;

import java.io.Serializable;

/**
 * 模板响应消息
 * Master通过此消息回复Worker的模板请求
 */
@Data
public class TemplateResponseMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 响应状态：true-成功，false-失败
     */
    private boolean success;

    /**
     * 错误消息，当success=false时提供
     */
    private String errorMessage;

    /**
     * 模板内容，当responseType=CONTENT时提供
     */
    private String templateContent;


}