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

import com.datasophon.dao.entity.ClusterInfoEntity;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;
import com.datasophon.dao.enums.ClusterState;
import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 集群信息表
 *
 */
@Mapper
public interface ClusterInfoMapper extends BaseMapper<ClusterInfoEntity> {

    /**
     * 根据集群代码获取集群信息
     *
     * @param clusterCode 集群代码
     * @return 集群信息实体
     */
    default ClusterInfoEntity getClusterByClusterCode(@Param("clusterCode") String clusterCode) {
        QueryWrapper query = QueryWrapper.create()
                .where(ClusterInfoEntity::getClusterCode).eq(clusterCode);
        return this.selectOneByQuery(query);
    }

    /**
     * 根据集群代码查询集群信息
     *
     * @param clusterCode 集群代码
     * @return 集群信息实体
     */
    default ClusterInfoEntity selectByClusterCode(@Param("clusterCode") String clusterCode) {
        QueryWrapper query = QueryWrapper.create()
                .where(ClusterInfoEntity::getClusterCode).eq(clusterCode);
        return this.selectOneByQuery(query);
    }

    /**
     * 查询所有集群信息
     *
     * @return 集群信息列表
     */
    default List<ClusterInfoEntity> selectAll() {
        QueryWrapper query = QueryWrapper.create();
        return this.selectListByQuery(query);
    }

    /**
     * 根据集群状态查询集群列表
     *
     * @param clusterState 集群状态
     * @return 集群信息列表
     */
    default List<ClusterInfoEntity> selectByClusterState(@Param("clusterState") ClusterState clusterState) {
        QueryWrapper query = QueryWrapper.create()
                .where(ClusterInfoEntity::getClusterState).eq(clusterState);
        return this.selectListByQuery(query);
    }

    /**
     * 根据框架代码查询集群列表
     *
     * @param frameCode 框架代码
     * @return 集群信息列表
     */
    default List<ClusterInfoEntity> selectByFrameCode(@Param("frameCode") String frameCode) {
        QueryWrapper query = QueryWrapper.create()
                .where(ClusterInfoEntity::getClusterFrame).eq(frameCode);
        return this.selectListByQuery(query);
    }

    /**
     * 查询所有运行中的服务角色实例
     * 
     * @return 运行中的服务角色实例列表
     */
    default List<ClusterServiceRoleInstanceEntity> selectRunningRoleInstances() {
        // 这个方法应该在ClusterServiceRoleInstanceMapper中，这里暂时返回空列表
        // 避免编译错误，实际应该使用相应的Mapper
        return java.util.Collections.emptyList();
    }

}
