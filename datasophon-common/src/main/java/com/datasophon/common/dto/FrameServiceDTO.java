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

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serial;
import java.io.Serializable;

/**
 * 集群框架版本服务表数据传输对象
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record FrameServiceDTO(
        Integer id,
        Integer frameId,
        String serviceName,
        String label,
        String serviceVersion,
        String serviceDesc,
        String packageName,
        String dependencies,
        String serviceJson,
        String serviceJsonMd5,
        String serviceConfig,
        String frameCode,
        String configFileJson,
        String configFileJsonMd5,
        String decompressPackageName,
        Integer sortNum,
        Boolean installed, // 运行时计算字段
        Boolean isRequired // 运行时计算字段
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 创建基础FrameServiceDTO，不包含运行时计算字段
     */
    public static FrameServiceDTO of(Integer id, Integer frameId, String serviceName, String serviceVersion) {
        return new FrameServiceDTO(id, frameId, serviceName, null, serviceVersion, null, null, null,
                null, null, null, null, null, null, null, null, null, null);
    }

    /**
     * 创建包含安装状态的FrameServiceDTO
     */
    public static FrameServiceDTO withInstallStatus(Integer id, Integer frameId, String serviceName,
            String serviceVersion, Boolean installed, Boolean isRequired) {
        return new FrameServiceDTO(id, frameId, serviceName, null, serviceVersion, null, null, null,
                null, null, null, null, null, null, null, null, installed, isRequired);
    }

    /**
     * 设置安装状态 - 返回新的DTO实例
     */
    public FrameServiceDTO withInstalled(Boolean installed) {
        return new FrameServiceDTO(id, frameId, serviceName, label, serviceVersion, serviceDesc,
                packageName, dependencies, serviceJson, serviceJsonMd5, serviceConfig, frameCode,
                configFileJson, configFileJsonMd5, decompressPackageName, sortNum, installed, isRequired);
    }

    /**
     * 设置必需状态 - 返回新的DTO实例
     */
    public FrameServiceDTO withRequired(Boolean isRequired) {
        return new FrameServiceDTO(id, frameId, serviceName, label, serviceVersion, serviceDesc,
                packageName, dependencies, serviceJson, serviceJsonMd5, serviceConfig, frameCode,
                configFileJson, configFileJsonMd5, decompressPackageName, sortNum, installed, isRequired);
    }
}