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

import com.datasophon.dao.entity.ClusterZkEntity;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;

import java.util.List;

/**
 * 集群ZooKeeper映射器
 * 按照架构重构规范，迁移QueryChain到DAO层
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-01
 */
@Mapper
public interface ClusterZkMapper extends BaseMapper<ClusterZkEntity> {

    /**
     * 获取指定集群的最大myid值
     *
     * @param clusterId 集群ID
     * @return 最大myid值
     */
    default Integer getMaxMyId(@Param("clusterId") Long clusterId) {
        QueryWrapper query = QueryWrapper.create()
                .select("MAX(myid)")
                .from("t_ddh_cluster_zk")
                .where(ClusterZkEntity::getClusterId).eq(clusterId);

        // 使用selectObjectByQuery查询单个聚合结果
        Object result = this.selectObjectByQuery(query);
        return result != null ? (Integer) result : null;
    }

    /**
     * 根据集群ID查询所有ZK服务器
     *
     * @param clusterId 集群ID
     * @return ZK服务器列表
     */
    default List<ClusterZkEntity> selectByClusterId(@Param("clusterId") Long clusterId) {
        QueryWrapper query = QueryWrapper.create()
                .where(ClusterZkEntity::getClusterId).eq(clusterId);
        return this.selectListByQuery(query);
    }
}
