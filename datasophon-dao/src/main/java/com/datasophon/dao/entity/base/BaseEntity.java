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

package com.datasophon.dao.entity.base;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 统一的基础实体类，包含所有审计字段
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-13 16:58:01
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseEntity implements Serializable {
    
    @Serial
    private static final long serialVersionUID = 1L;
    
    /**
     * 主键 - 统一使用Long类型，通过雪花算法生成
     * 序列化为字符串避免前端精度丢失
     */
    @Id(keyType = KeyType.Generator, value = "snowflakeId")
    @Column("id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    
    /**
     * 创建时间
     */
    @Column("create_time")
    private LocalDateTime createTime;
    
    /**
     * 更新时间  
     */
    @Column("update_time")
    private LocalDateTime updateTime;
    
    /**
     * 创建人
     */
    @Column("create_by")
    private String createBy;
    
    /**
     * 更新人
     */
    @Column("update_by") 
    private String updateBy;
}
