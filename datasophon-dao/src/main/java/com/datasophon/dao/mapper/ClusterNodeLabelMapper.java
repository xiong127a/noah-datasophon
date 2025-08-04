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

import com.datasophon.dao.entity.ClusterNodeLabelEntity;
import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 集群节点标签数据访问对象
 * 提供集群节点标签的数据库操作
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
@Mapper
public interface ClusterNodeLabelMapper extends BaseMapper<ClusterNodeLabelEntity> {

    /**
     * 根据集群ID查询节点标签
     */
    default List<ClusterNodeLabelEntity> selectByClusterId(Integer clusterId) {
        QueryWrapper query = QueryWrapper.create()
                .where(ClusterNodeLabelEntity::getClusterId).eq(clusterId);
        return this.selectListByQuery(query);
    }

    /**
     * 根据集群ID和节点标签查询
     */
    default List<ClusterNodeLabelEntity> selectByClusterIdAndNodeLabel(Integer clusterId, String nodeLabel) {
        QueryWrapper query = QueryWrapper.create()
                .where(ClusterNodeLabelEntity::getClusterId).eq(clusterId)
                .and(ClusterNodeLabelEntity::getNodeLabel).eq(nodeLabel);
        return this.selectListByQuery(query);
    }
}
