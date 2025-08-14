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
import java.util.List;

/**
 * 集群框架表视图对象
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record FrameInfoVO(
        Long id,
        String frameName,
        String frameCode,
        String frameVersion,
        Object frameServiceList, // 使用Object避免循环依赖
        String displayName) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 获取格式化的显示名称
     */
    public String getDisplayName() {
        if (displayName != null) {
            return displayName;
        }
        return frameName != null && frameVersion != null
                ? frameName + " " + frameVersion
                : frameName;
    }

    /**
     * 创建基础FrameInfoVO，不包含服务列表
     */
    public static FrameInfoVO of(Long id, String frameName, String frameCode, String frameVersion) {
        String displayName = frameName != null && frameVersion != null
                ? frameName + " " + frameVersion
                : frameName;
        return new FrameInfoVO(id, frameName, frameCode, frameVersion, null, displayName);
    }

    /**
     * 创建完整FrameInfoVO，包含服务列表
     */
    public static FrameInfoVO withServices(Long id, String frameName, String frameCode, String frameVersion,
            Object frameServiceList) {
        String displayName = frameName != null && frameVersion != null
                ? frameName + " " + frameVersion
                : frameName;
        return new FrameInfoVO(id, frameName, frameCode, frameVersion, frameServiceList, displayName);
    }
}