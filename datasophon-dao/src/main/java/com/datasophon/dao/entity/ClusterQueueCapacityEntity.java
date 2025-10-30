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
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.Serial;

/**
 * 集群队列容量实体类
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-13
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Table("t_ddh_cluster_queue_capacity")
public class ClusterQueueCapacityEntity extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;



    private Long clusterId;
    
    /**
     * 队列名称
     */
    private String queueName;
    
    /**
     * 父队列名称
     */
    private String parentQueueName;
    
    /**
     * 容量
     */
    private String capacity;
    
    /**
     * 最大容量
     */
    private String maxCapacity;
    
    /**
     * 节点标签
     */
    private String nodeLabel;
    
    /**
     * ACL 用户
     */
    private String aclUsers;

    /**
     * 父队列（兼容旧字段）
     */
    private String parent;

}
