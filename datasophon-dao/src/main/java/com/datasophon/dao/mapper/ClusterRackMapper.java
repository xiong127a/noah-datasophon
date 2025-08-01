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
 * 集群机架映射器
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2024-12-19
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

    /**
     * 根据ID查询单个实体
     */
    default ClusterRack selectById(Integer id) {
        return this.selectOneById(id);
    }

    /**
     * 插入实体
     */
    default int insert(ClusterRack entity) {
        return this.insertSelective(entity);
    }

    /**
     * 根据ID更新实体
     */
    default int updateById(ClusterRack entity) {
        return this.update(entity);
    }

    /**
     * 根据ID删除
     */
    default int removeById(Integer id) {
        return this.deleteById(id);
    }

    /**
     * 根据ID列表删除
     */
    default int deleteByIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        return this.deleteBatchByIds(ids);
    }

    /**
     * 查询所有机架
     */
    default List<ClusterRack> selectAll() {
        QueryWrapper query = QueryWrapper.create();
        return this.selectListByQuery(query);
    }
}
