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

import lombok.Getter;
import java.io.Serializable;

/**
 * 角色信息VO - 视图展示对象
 * 使用JDK21 Record特性，专为前端展示优化
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-06
 */
public record RoleInfoVO(
    Long id,
    String roleName,
    String roleCode,
    String createTimeFormatted,
    String updateTimeFormatted,
    String createBy,
    String updateBy,
    boolean isAdminRole,
    RoleStatus status
) implements Serializable {
    
    /**
     * 角色状态枚举 - JDK21嵌套特性
     */
    @Getter
    public enum RoleStatus {
        ACTIVE("激活"),
        INACTIVE("停用"),
        DELETED("已删除");
        
        private final String description;
        
        RoleStatus(String description) {
            this.description = description;
        }

    }

}