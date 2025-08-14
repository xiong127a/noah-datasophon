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

package com.datasophon.dao.entity;

import com.datasophon.dao.entity.base.BaseEntity;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Table;
import com.datasophon.common.enums.CommandState;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.Serial;

/**
 * 集群服务命令主机实体类
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-13
 */
@Table("t_ddh_cluster_service_command_host")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class ClusterServiceCommandHostEntity extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;


    /**
     * 主机
     */
    private String hostname;
    /**
     * 命令状态 1：正在运行2：成功3：失败
     */
    private CommandState commandState;

    @Column(ignore = true)
    private Integer commandStateCode;
    /**
     * 命令进度
     */
    private Long commandProgress;
    /**
     * 操作指令id（逻辑外键，关联t_ddh_cluster_service_command表的id）
     */
    private Long commandId;



}
