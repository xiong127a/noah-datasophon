/*
 *
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
 *
 */

package com.datasophon.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 配置文件模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigFile {

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件描述
     */
    private String description;

    /**
     * 文件大小
     */
    private String fileSize;

    /**
     * 文件路径
     */
    private String filePath;

    /**
     * 最后修改时间
     */
    private String lastModified;
}