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

package com.datasophon.dao.mapper;

import com.datasophon.dao.entity.ClusterYarnQueue;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryChain;

import java.util.List;

/**
 * 
 * 
 * @author gaodayu
 * @email gaodayu2022@163.com
 * @date 2022-07-13 19:34:14
 */
@Mapper
public interface ClusterYarnQueueMapper extends BaseMapper<ClusterYarnQueue> {

    /**
     * 分页查询集群Yarn队列
     */
    default Page<ClusterYarnQueue> selectPageByClusterId(Page<ClusterYarnQueue> page,
            @Param("clusterId") Integer clusterId) {
        QueryChain<ClusterYarnQueue> query = QueryChain.of(ClusterYarnQueue.class)
                .where(ClusterYarnQueue::getClusterId).eq(clusterId)
                .orderBy(ClusterYarnQueue::getCreateTime).desc();
        return query.page(page);
    }

    /**
     * 根据队列名称查询是否存在
     */
    default boolean existsByQueueName(@Param("queueName") String queueName) {
        return QueryChain.of(ClusterYarnQueue.class)
                .where(ClusterYarnQueue::getQueueName).eq(queueName)
                .exists();
    }

    /**
     * 根据集群ID查询所有队列
     */
    default List<ClusterYarnQueue> selectByClusterId(@Param("clusterId") Integer clusterId) {
        return QueryChain.of(ClusterYarnQueue.class)
                .where(ClusterYarnQueue::getClusterId).eq(clusterId)
                .list();
    }

    /**
     * 根据集群ID和队列名称查询队列
     */
    default ClusterYarnQueue selectByClusterIdAndQueueName(@Param("clusterId") Integer clusterId,
            @Param("queueName") String queueName) {
        return QueryChain.of(ClusterYarnQueue.class)
                .where(ClusterYarnQueue::getQueueName).eq(queueName)
                .and(ClusterYarnQueue::getClusterId).eq(clusterId)
                .one();
    }
}
