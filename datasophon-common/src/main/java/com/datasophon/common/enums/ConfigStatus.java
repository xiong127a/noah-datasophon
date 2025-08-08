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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.mybatisflex.annotation.EnumValue;
import lombok.Getter;

import java.util.Arrays;

/**
 * 集群配置状态枚举
 * 用于跟踪集群配置过程的状态
 *
 * @author DataSophon Team
 */
@Getter
public enum ConfigStatus {
    
    /**
     * 未配置 - 集群刚创建，尚未开始配置
     */
    UNCONFIGURED("UNCONFIGURED", "未配置"),
    
    /**
     * 配置中 - 集群正在配置过程中
     */
    CONFIGURING("CONFIGURING", "配置中"),
    
    /**
     * 配置完成 - 集群配置完成，可以开始部署或运行
     */
    COMPLETED("COMPLETED", "配置完成");

    /**
     * 数据库存储值
     */
    @EnumValue
    @JsonValue
    private final String code;
    
    /**
     * 显示描述
     */
    private final String desc;
    
    /**
     * 构造函数
     *
     * @param code 状态代码
     * @param desc 状态描述
     */
    ConfigStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
    
    /**
     * 根据代码获取枚举实例
     *
     * @param code 状态代码
     * @return ConfigStatus枚举实例，如果找不到则返回null
     */
    @JsonCreator
    public static ConfigStatus fromCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }
        return Arrays.stream(values())
                .filter(status -> status.getCode().equalsIgnoreCase(code.trim()))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * 检查是否为最终状态（配置完成）
     *
     * @return true如果是最终状态
     */
    public boolean isFinal() {
        return this == COMPLETED;
    }
    
    /**
     * 检查是否为活跃状态（配置中）
     *
     * @return true如果是活跃状态
     */
    public boolean isActive() {
        return this == CONFIGURING;
    }
    
    /**
     * 检查是否为初始状态（未配置）
     *
     * @return true如果是初始状态
     */
    public boolean isInitial() {
        return this == UNCONFIGURED;
    }
    
    /**
     * 获取下一个状态
     *
     * @return 下一个配置状态，如果已是最终状态则返回当前状态
     */
    public ConfigStatus nextStatus() {
        return switch (this) {
            case UNCONFIGURED -> CONFIGURING;
            case CONFIGURING -> COMPLETED;
            case COMPLETED -> COMPLETED; // 已完成，保持不变
        };
    }
    
    /**
     * 可以进入集群管理界面
     *
     * @return true如果可以进入集群管理
     */
    public boolean canEnterCluster() {
        return this == COMPLETED;
    }
    
    /**
     * 需要继续配置
     *
     * @return true如果需要继续配置
     */
    public boolean needsContinueConfig() {
        return this == UNCONFIGURED || this == CONFIGURING;
    }
    
    @Override
    public String toString() {
        return this.desc;
    }
}