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
 * 检查项类型枚举（按执行优先级排序）
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-28
 */
@Getter
public enum CheckType {
    SSH_PASSWORDLESS("ssh-passwordless", "SSH免密检查", 1, true),
    SSH_CONNECTION("ssh-connection", "SSH连接检查", 2, true),
    SYSTEM_INFO("system-info", "系统信息收集", 3, false),
    JAVA_ENV("java-env", "Java环境检查", 4, true),
    FIREWALL("firewall", "防火墙检查", 5, true),
    SELINUX("selinux", "SELinux检查", 6, true),
    SERVICES("services", "服务检查", 7, true),
    HOSTS_FILE("hosts-file", "Hosts文件检查", 8, true),
    FILE_HANDLE_LIMIT("file-handle-limit", "文件句柄限制检查", 9, true),
    TIME_SYNC("time-sync", "时间同步检查", 10, true),
    
    // 兼容旧名称
    SSH_CONNECTIVITY("ssh-connectivity", "SSH连接检查", 2, true),
    OS_INFO_COLLECTION("os-info-collection", "操作系统信息收集", 3, false),
    HARDWARE_INFO_COLLECTION("hardware-info-collection", "硬件信息收集", 3, false),
    CPU_CHECK("cpu-check", "CPU检查", 3, false),
    MEMORY_CHECK("memory-check", "内存检查", 3, false),
    DISK_SPACE_CHECK("disk-space-check", "磁盘空间检查", 3, false),
    JAVA_ENVIRONMENT_CHECK("java-environment-check", "Java环境检查", 4, true),
    FILE_HANDLE_LIMIT_CHECK("file-handle-limit-check", "文件句柄限制检查", 9, true),
    TIME_SYNC_CHECK("time-sync-check", "时间同步检查", 10, true),
    USER_GROUP_CHECK("user-group-check", "用户组检查", 7, true),
    FIREWALL_CHECK("firewall-check", "防火墙检查", 5, true),
    SELINUX_CHECK("selinux-check", "SELinux检查", 6, true),
    HOSTS_FILE_CHECK("hosts-file-check", "Hosts文件检查", 8, true);
    
    private final String code;
    private final String displayName;
    private final int priority;
    private final boolean canRepair;
    
    CheckType(String code, String displayName, int priority, boolean canRepair) {
        this.code = code;
        this.displayName = displayName;
        this.priority = priority;
        this.canRepair = canRepair;
    }
    
    /**
     * 根据代码获取检查类型
     */
    public static CheckType fromCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }
        
        for (CheckType type : values()) {
            if (type.code.equalsIgnoreCase(code.trim())) {
                return type;
            }
        }
        
        return null;
    }
    
    /**
     * 是否为前置检查（SSH相关）
     */
    public boolean isPrerequisite() {
        return this == SSH_PASSWORDLESS || this == SSH_CONNECTIVITY;
    }
    
    /**
     * 是否为信息收集类检查
     */
    public boolean isInfoCollection() {
        return this == OS_INFO_COLLECTION || this == HARDWARE_INFO_COLLECTION;
    }
    
    /**
     * 是否为环境检查
     */
    public boolean isEnvironmentCheck() {
        return priority >= 5 && priority <= 11;
    }
    
    /**
     * 是否为系统配置检查
     */
    public boolean isSystemConfigCheck() {
        return priority >= 12;
    }
}
