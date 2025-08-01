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

import com.datasophon.dao.entity.ClusterZk;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;

/**
 * 
 *
 */
@Mapper
public interface ClusterZkMapper extends BaseMapper<ClusterZk> {

    /**
     * 获取指定集群的最大myid值
     *
     * @param clusterId 集群ID
     * @return 最大myid值
     */
    default Integer getMaxMyId(@Param("clusterId") Integer clusterId) {
        QueryWrapper query = QueryWrapper.create()
                .select("MAX(myid)")
                .from("t_ddh_cluster_zk")
                .where(ClusterZk::getClusterId).eq(clusterId);

        // 使用selectObjectByQuery查询单个聚合结果
        Object result = this.selectObjectByQuery(query);
        return result != null ? (Integer) result : null;
    }
}
