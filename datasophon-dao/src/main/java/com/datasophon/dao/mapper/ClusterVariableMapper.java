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

import com.datasophon.dao.entity.ClusterVariable;

import org.apache.ibatis.annotations.Mapper;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;

import java.util.List;

/**
 * 集群变量映射器
 * 迁移SQL逻辑到DAO层
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-01
 */
@Mapper
public interface ClusterVariableMapper extends BaseMapper<ClusterVariable> {

    /**
     * 根据变量名和集群ID查询集群变量
     *
     * @param variableName 变量名
     * @param clusterId    集群ID
     * @return 集群变量列表
     */
    default List<ClusterVariable> selectByVariableNameAndClusterId(String variableName, Integer clusterId) {
        QueryWrapper query = QueryWrapper.create()
                .where(ClusterVariable::getVariableName).eq(variableName)
                .and(ClusterVariable::getClusterId).eq(clusterId);
        return this.selectListByQuery(query);
    }

    /**
     * 根据集群ID查询所有集群变量
     *
     * @param clusterId 集群ID
     * @return 集群变量列表
     */
    default List<ClusterVariable> selectByClusterId(Integer clusterId) {
        QueryWrapper query = QueryWrapper.create()
                .where(ClusterVariable::getClusterId).eq(clusterId);
        return this.selectListByQuery(query);
    }

    /**
     * 根据变量名和集群ID获取单个变量
     *
     * @param variableName 变量名
     * @param clusterId 集群ID
     * @return 集群变量，如果不存在返回null
     */
    default ClusterVariable getVariableByVariableName(String variableName, Integer clusterId) {
        QueryWrapper query = QueryWrapper.create()
                .where(ClusterVariable::getVariableName).eq(variableName)
                .and(ClusterVariable::getClusterId).eq(clusterId);
        return this.selectOneByQuery(query);
    }
}
