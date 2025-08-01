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

package com.datasophon.api.service;

import com.datasophon.dao.entity.ClusterQueueCapacity;
import com.datasophon.dao.model.ClusterQueueCapacityList;

import java.util.List;

/**
 * 集群队列容量服务
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2024-12-19
 */
public interface ClusterQueueCapacityService {

    boolean refreshToYarn(Integer clusterId) throws Exception;

    void createDefaultQueue(Integer clusterId);

    ClusterQueueCapacityList listCapacityQueue(Integer clusterId);

    // 标准CRUD方法
    ClusterQueueCapacity getById(Integer id);

    ClusterQueueCapacity save(ClusterQueueCapacity entity);

    ClusterQueueCapacity updateById(ClusterQueueCapacity entity);

    boolean removeByIds(List<Integer> ids);

    List<ClusterQueueCapacity> getAllQueueCapacities();
}
