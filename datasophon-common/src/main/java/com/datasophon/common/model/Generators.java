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

import java.io.Serializable;
import java.util.List;

import lombok.Data;

/**
 * 配置文件详情
 */
@Data
public class Generators implements Serializable {

    /**
     * 文件名
     */
    private String filename;

    /**
     * 文件数据类型
     */
    private String configFormat;

    /**
     * 输出文件夹
     */
    private String outputDirectory;

    /**
     * 配置参数名称列表
     */
    private List<String> includeParams;

    /**
     * 自定义配置模板名称
     */
    private String templateName;
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Generators generators = (Generators) o;
        if (generators.getFilename().equals(filename)) {
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return filename.hashCode();
    }

}
