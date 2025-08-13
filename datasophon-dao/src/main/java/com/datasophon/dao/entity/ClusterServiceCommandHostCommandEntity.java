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
import com.datasophon.common.enums.RoleType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.util.Date;

/**
 * 集群服务命令主机命令实体类
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-13
 */
@Table("t_ddh_cluster_service_command_host_command")
@Data
@EqualsAndHashCode(callSuper = true)
public class ClusterServiceCommandHostCommandEntity extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主机命令标识（原主键字段，保留作为业务标识）
     */
    private String hostCommandId;
    /**
     * 指令名称
     */
    private String commandName;
    /**
     * 指令状态 1、正在运行2：成功3：失败
     */
    private CommandState commandState;

    @Column(ignore = true)
    private Integer commandStateCode;
    /**
     * 指令进度
     */
    private Integer commandProgress;
    /**
     * 主机id
     */
    private String commandHostId;

    private String commandId;

    private String hostname;
    /**
     * 服务角色名称
     */
    private String serviceRoleName;

    private RoleType serviceRoleType;

    private String resultMsg;



    private Integer commandType;

}
