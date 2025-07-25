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
import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryChain;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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
        return QueryChain.of(ClusterInfoEntity.class)
                .where(ClusterInfoEntity::getClusterCode).eq(clusterCode)
                .one();
    }
}
