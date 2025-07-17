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

package com.datasophon.common.enums;

/**
 * 操作范围枚举
 * 用于定义队列管理器操作的作用范围
 */
public enum ScopeCode {
    /** 所有组件 */
    ALL("all"),
    /** 仅队列 */
    QUEUE("queue"),
    /** 仅定时任务 */
    SCHEDULER("scheduler");

    private final String code;

    ScopeCode(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    /**
     * 根据编码获取枚举值
     * @param code 编码
     * @return 枚举值，默认返回ALL
     */
    public static ScopeCode of(String code) {
        if (code == null) {
            return ALL;
        }
        
        for (ScopeCode scopeCode : values()) {
            if (scopeCode.code.equals(code)) {
                return scopeCode;
            }
        }
        
        return ALL;
    }
} 