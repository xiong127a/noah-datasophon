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

import com.mybatisflex.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 主机管理状态枚举
 * 定义主机在集群中的管理状态
 * 
 * @author 任相鹏
 * @email 635887935@qq.com 
 * @date 2025-01-31
 */
@Getter
public enum ManagementStatus {

    /**
     * 受管 - 主机已正式纳入集群管理（对应原ManagementStatus.MANAGED值1）
     */
    MANAGED(1, "受管"),
    
    /**
     * 未受管 - 主机已发现但未纳入管理（对应原MANAGED.NO值2）
     */
    UNMANAGED(2, "未受管"),
    
    /**
     * 配置中 - 主机正在进行配置，暂时不计入受管统计
     */
    CONFIGURING(3, "配置中");

    @EnumValue
    private final int value;

    private final String desc;

    ManagementStatus(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    @JsonValue
    public String getDesc() {
        return desc;
    }

    /**
     * 判断是否为受管状态（用于统计受管主机数量）
     * 注意：配置中状态不计入受管统计
     */
    public boolean isManaged() {
        return this == MANAGED;
    }

    /**
     * 判断是否可以进行配置操作
     */
    public boolean canConfigure() {
        return this == UNMANAGED || this == CONFIGURING;
    }

    /**
     * 判断是否正在配置中
     */
    public boolean isConfiguring() {
        return this == CONFIGURING;
    }

    /**
     * 根据数值获取枚举
     */
    public static ManagementStatus fromValue(int value) {
        for (ManagementStatus status : values()) {
            if (status.getValue() == value) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的管理状态值: " + value);
    }

    @Override
    public String toString() {
        return this.desc;
    }
}
