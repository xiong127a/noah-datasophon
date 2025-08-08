/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.datasophon.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 服务选择类型枚举
 * 用于区分核心服务和自定义服务选择模式
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-20
 */
public enum ServiceType {
    
    /** 核心服务 - 系统推荐的必需大数据服务组件 */
    CORE("core", "核心"),
    
    /** 自定义服务 - 用户可选的额外大数据服务组件 */
    CUSTOM("custom", "自定义");
    
    /** 类型代码 */
    private final String code;
    
    /** 类型描述 */
    @Getter
    private final String description;
    
    ServiceType(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    @JsonValue
    public String getCode() {
        return code;
    }

    /**
     * 根据代码获取枚举值
     * Spring Boot会自动调用此方法进行字符串到枚举的转换
     * 
     * @param code 类型代码
     * @return 对应的枚举值
     * @throws IllegalArgumentException 如果代码不匹配任何枚举值
     */
    @JsonCreator
    public static ServiceType fromCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("服务类型代码不能为空");
        }
        
        for (ServiceType type : ServiceType.values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        
        throw new IllegalArgumentException("不支持的服务类型代码: " + code);
    }
    
    /**
     * 判断是否为核心服务类型
     */
    public boolean isCore() {
        return this == CORE;
    }
    
    /**
     * 判断是否为自定义服务类型
     */
    public boolean isCustom() {
        return this == CUSTOM;
    }
}
