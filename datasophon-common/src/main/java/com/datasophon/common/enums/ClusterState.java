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

import com.fasterxml.jackson.annotation.JsonValue;
import com.mybatisflex.annotation.EnumValue;
import lombok.Getter;

import java.util.Arrays;

/**
 * 集群状态
 *
 * @author 63588
 * @since 2022/8/13
 **/
@Getter
public enum ClusterState {
    /**
     * 删除中 - 集群已删除，可以删除
     */
    DELETING(5, "删除中"),
    /**
     * 停止 - 集群已停止，可以删除
     */
    STOP(4, "停止"),

    /**
     * 正在运行 - 配置完成，可以进入集群管理
     */
    RUNNING(3, "正在运行"),
    
    /**
     * 待配置 - 集群刚创建，需要开始配置
     */
    NEED_CONFIG(1, "待配置");

    @Getter
    @EnumValue
    private final int value;

    private final String desc;
    /**
     * 构造函数
     *
     * @param value 枚举值
     * @param desc  枚举描述
     */
    ClusterState(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    @JsonValue
    public String getDesc() {
        return desc;
    }

    public static ClusterState of(int value) {
        return Arrays.stream(values()).filter(state -> state.getValue() == value).findAny().orElse(null);
    }

    /**
     * 是否可以进入集群管理
     */
    public boolean canEnterCluster() {
        return this == RUNNING;
    }
    
    /**
     * 是否需要继续配置
     */
    public boolean needsContinueConfig() {
        return this == NEED_CONFIG;
    }
    
    /**
     * 是否正在配置中
     */
    public boolean isConfiguring() {
        return false; // 已删除CONFIGURING状态
    }
    
    /**
     * 是否未开始配置
     */
    public boolean isUnconfigured() {
        return this == NEED_CONFIG;
    }
    
    /**
     * 是否已停止
     */
    public boolean isStopped() {
        return this == STOP;
    }
    
    /**
     * 是否正在运行
     */
    public boolean isRunning() {
        return this == RUNNING;
    }
    
    /**
     * 是否可以删除
     * 只有停止状态的集群才可以删除
     */
    public boolean canDelete() {
        return this == STOP;
    }
    
    /**
     * 是否可以停止
     * 只有正在运行的集群才可以停止
     */
    public boolean canStop() {
        return this == RUNNING;
    }
    
    /**
     * 是否可以启动
     * 只有停止状态的集群才可以启动
     */
    public boolean canStart() {
        return this == STOP;
    }

    @Override
    public String toString() {
        return this.desc;
    }
}
