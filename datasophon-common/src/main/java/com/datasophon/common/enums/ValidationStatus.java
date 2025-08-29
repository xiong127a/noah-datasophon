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

import lombok.Getter;

/**
 * 校验状态枚举
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-28
 */
@Getter
public enum ValidationStatus {
    PENDING("待检查", "pending"),
    CHECKING("检查中", "checking"),
    SUCCESS("检查成功", "success"),
    FAILED("检查失败", "failed"),
    REPAIRING("修复中", "repairing"),
    REPAIRED("修复完成", "repaired"),
    REPAIR_FAILED("修复失败", "repair_failed");
    
    private final String displayName;
    private final String code;
    
    ValidationStatus(String displayName, String code) {
        this.displayName = displayName;
        this.code = code;
    }
    
    /**
     * 根据代码获取状态
     */
    public static ValidationStatus fromCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return PENDING;
        }
        
        for (ValidationStatus status : values()) {
            if (status.code.equalsIgnoreCase(code.trim())) {
                return status;
            }
        }
        
        return PENDING;
    }
    
    /**
     * 是否为成功状态
     */
    public boolean isSuccess() {
        return this == SUCCESS || this == REPAIRED;
    }
    
    /**
     * 是否为失败状态
     */
    public boolean isFailed() {
        return this == FAILED || this == REPAIR_FAILED;
    }
    
    /**
     * 是否为进行中状态
     */
    public boolean isInProgress() {
        return this == CHECKING || this == REPAIRING;
    }
}
