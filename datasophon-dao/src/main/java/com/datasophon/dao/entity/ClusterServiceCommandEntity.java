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

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import com.datasophon.common.enums.CommandState;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Table("t_ddh_cluster_service_command")
@Data
public class ClusterServiceCommandEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @Id
    private String commandId;
    /**
     * 创建人
     */
    private String createBy;
    /**
     * 创建时间
     */
    private Date createTime;
    /**
     * 命令名称
     */
    private String commandName;
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
     * 集群id
     */
    private Integer clusterId;
    /**
     * 服务名称
     */
    private String serviceName;
    /**
     * 命令类型
     */
    private Integer commandType;

    @Column(ignore = true)
    private String durationTime;

    private Date endTime;

    private Integer serviceInstanceId;

}
