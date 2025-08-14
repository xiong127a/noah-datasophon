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

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serial;
import java.io.Serializable;

/**
 * 集群框架版本服务表视图对象
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record FrameServiceVO(
        Long id,
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
        Boolean installed,
        Boolean isRequired,
        String displayName, // 显示名称
        String statusText, // 状态文本
        String installStatusText // 安装状态文本
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 获取格式化的显示名称
     */
    public String getDisplayName() {
        if (displayName != null) {
            return displayName;
        }
        return serviceName != null && serviceVersion != null
                ? serviceName + " " + serviceVersion
                : serviceName;
    }

    /**
     * 获取状态文本
     */
    public String getStatusText() {
        if (statusText != null) {
            return statusText;
        }
        return Boolean.TRUE.equals(installed) ? "已安装" : "未安装";
    }

    /**
     * 获取安装状态文本
     */
    public String getInstallStatusText() {
        if (installStatusText != null) {
            return installStatusText;
        }
        if (Boolean.TRUE.equals(isRequired) && Boolean.FALSE.equals(installed)) {
            return "必选组件-未安装";
        } else if (Boolean.TRUE.equals(isRequired) && Boolean.TRUE.equals(installed)) {
            return "必选组件-已安装";
        } else if (Boolean.TRUE.equals(installed)) {
            return "可选组件-已安装";
        } else {
            return "可选组件-未安装";
        }
    }

    /**
     * 创建基础FrameServiceVO
     */
    public static FrameServiceVO of(Long id, String serviceName, String serviceVersion, Boolean installed,
            Boolean isRequired) {
        String displayName = serviceName != null && serviceVersion != null
                ? serviceName + " " + serviceVersion
                : serviceName;
        String statusText = Boolean.TRUE.equals(installed) ? "已安装" : "未安装";
        String installStatusText = getInstallStatusText(installed, isRequired);

        return new FrameServiceVO(id, null, serviceName, null, serviceVersion, null, null, null,
                null, null, null, null, null, null, null, null, installed, isRequired,
                displayName, statusText, installStatusText);
    }

    /**
     * 创建完整FrameServiceVO
     */
    public static FrameServiceVO withAllFields(Long id, Integer frameId, String serviceName, String label,
            String serviceVersion, String serviceDesc, String packageName, String dependencies,
            String serviceJson, String serviceJsonMd5, String serviceConfig, String frameCode,
            String configFileJson, String configFileJsonMd5, String decompressPackageName,
            Integer sortNum, Boolean installed, Boolean isRequired) {

        String displayName = serviceName != null && serviceVersion != null
                ? serviceName + " " + serviceVersion
                : serviceName;
        String statusText = Boolean.TRUE.equals(installed) ? "已安装" : "未安装";
        String installStatusText = getInstallStatusText(installed, isRequired);

        return new FrameServiceVO(id, frameId, serviceName, label, serviceVersion, serviceDesc,
                packageName, dependencies, serviceJson, serviceJsonMd5, serviceConfig, frameCode,
                configFileJson, configFileJsonMd5, decompressPackageName, sortNum,
                installed, isRequired, displayName, statusText, installStatusText);
    }

    private static String getInstallStatusText(Boolean installed, Boolean isRequired) {
        if (Boolean.TRUE.equals(isRequired) && Boolean.FALSE.equals(installed)) {
            return "必选组件-未安装";
        } else if (Boolean.TRUE.equals(isRequired) && Boolean.TRUE.equals(installed)) {
            return "必选组件-已安装";
        } else if (Boolean.TRUE.equals(installed)) {
            return "可选组件-已安装";
        } else {
            return "可选组件-未安装";
        }
    }
}