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

import com.datasophon.dao.entity.ClusterRack;
import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 集群机架数据访问对象
 * 提供集群机架的数据库操作
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
@Mapper
public interface ClusterRackMapper extends BaseMapper<ClusterRack> {

    /**
     * 根据集群ID查询机架
     */
    default List<ClusterRack> selectByClusterId(Integer clusterId) {
        QueryWrapper query = QueryWrapper.create()
                .where(ClusterRack::getClusterId).eq(clusterId);
        return this.selectListByQuery(query);
    }

    /**
     * 检查集群ID和机架名是否存在
     */
    default boolean existsByClusterIdAndRack(Integer clusterId, String rack) {
        QueryWrapper query = QueryWrapper.create()
                .where(ClusterRack::getClusterId).eq(clusterId)
                .and(ClusterRack::getRack).eq(rack);
        return this.selectOneByQuery(query) != null;
    }
}
