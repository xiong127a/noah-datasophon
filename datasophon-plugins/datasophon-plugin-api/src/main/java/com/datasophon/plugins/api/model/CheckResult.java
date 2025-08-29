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

package com.datasophon.plugins.api.model;

import com.datasophon.common.enums.CheckType;
import com.datasophon.common.enums.ValidationStatus;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 检查结果模型
 * 用于表示插件检查的执行结果
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-28
 */
@Data
@Builder
public class CheckResult implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 检查是否成功
     */
    private boolean success;
    
    /**
     * 检查状态
     */
    private ValidationStatus status;
    
    /**
     * 检查类型
     */
    private CheckType checkType;
    
    /**
     * 检查消息
     */
    private String message;
    
    /**
     * 错误信息（检查失败时）
     */
    private String error;
    
    /**
     * 检查开始时间
     */
    private LocalDateTime checkTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
    
    /**
     * 检查耗时（毫秒）
     */
    private Long duration;
    
    /**
     * 是否可修复
     */
    @Builder.Default
    private boolean repairAvailable = false;
    
    /**
     * 检查结果数据
     */
    @Builder.Default
    private Map<String, Object> data = new HashMap<>();
    
    /**
     * 添加结果数据
     */
    public CheckResult data(String key, Object value) {
        if (this.data == null) {
            this.data = new HashMap<>();
        }
        this.data.put(key, value);
        return this;
    }
    
    /**
     * 添加多个结果数据
     */
    public CheckResult data(Map<String, Object> additionalData) {
        if (this.data == null) {
            this.data = new HashMap<>();
        }
        if (additionalData != null) {
            this.data.putAll(additionalData);
        }
        return this;
    }
    
    /**
     * 获取结果数据
     */
    @SuppressWarnings("unchecked")
    public <T> T getData(String key, Class<T> type) {
        if (data == null) {
            return null;
        }
        Object value = data.get(key);
        if (value != null && type.isAssignableFrom(value.getClass())) {
            return (T) value;
        }
        return null;
    }
    
    /**
     * 获取结果数据（带默认值）
     */
    public <T> T getData(String key, Class<T> type, T defaultValue) {
        T value = getData(key, type);
        return value != null ? value : defaultValue;
    }
    
    /**
     * 检查是否有指定的数据键
     */
    public boolean hasData(String key) {
        return data != null && data.containsKey(key);
    }
    
    /**
     * 创建成功的检查结果
     */
    public static CheckResult success(CheckType checkType, String message) {
        return CheckResult.builder()
                .success(true)
                .checkType(checkType)
                .message(message)
                .checkTime(LocalDateTime.now())
                .build();
    }
    
    /**
     * 创建失败的检查结果
     */
    public static CheckResult failure(CheckType checkType, String message, String error) {
        return CheckResult.builder()
                .success(false)
                .checkType(checkType)
                .message(message)
                .error(error)
                .checkTime(LocalDateTime.now())
                .build();
    }
    
    /**
     * 创建失败的检查结果（简化版）
     */
    public static CheckResult failure(CheckType checkType, String error) {
        return failure(checkType, "检查失败", error);
    }
    
    /**
     * 获取检查状态
     */
    public ValidationStatus getStatus() {
        if (status != null) {
            return status;
        }
        return success ? ValidationStatus.SUCCESS : ValidationStatus.FAILED;
    }
    
    /**
     * 获取简短的结果摘要
     */
    public String getSummary() {
        return String.format("[%s] %s: %s", 
                checkType != null ? checkType.getDisplayName() : "unknown",
                success ? "SUCCESS" : "FAILED",
                message != null ? message : (success ? "检查通过" : "检查失败"));
    }
}