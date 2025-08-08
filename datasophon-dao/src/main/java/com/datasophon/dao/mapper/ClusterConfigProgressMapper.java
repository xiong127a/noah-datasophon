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

import com.datasophon.dao.entity.ClusterConfigProgressEntity;
import com.datasophon.common.enums.ConfigStatus;
import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

import static com.datasophon.dao.entity.table.ClusterConfigProgressEntityTableDef.CLUSTER_CONFIG_PROGRESS_ENTITY;

/**
 * 集群配置进度数据访问层
 * 使用MyBatis-Flex官方推荐的QueryWrapper lambda写法
 *
 * @author DataSophon Team
 */
@Mapper
public interface ClusterConfigProgressMapper extends BaseMapper<ClusterConfigProgressEntity> {
    
    /**
     * 根据集群ID查询配置进度
     */
    default ClusterConfigProgressEntity findByClusterId(Integer clusterId) {
        return selectOneByQuery(QueryWrapper.create()
                .from(CLUSTER_CONFIG_PROGRESS_ENTITY)
                .where(CLUSTER_CONFIG_PROGRESS_ENTITY.CLUSTER_ID.eq(clusterId)));
    }
    
    /**
     * 根据配置状态查询配置进度列表
     */
    default List<ClusterConfigProgressEntity> findByConfigStatus(ConfigStatus configStatus) {
        return selectListByQuery(QueryWrapper.create()
                .from(CLUSTER_CONFIG_PROGRESS_ENTITY)
                .where(CLUSTER_CONFIG_PROGRESS_ENTITY.CONFIG_STATUS.eq(configStatus)));
    }
    
    /**
     * 查询需要继续配置的集群（UNCONFIGURED 或 CONFIGURING 状态）
     */
    default List<ClusterConfigProgressEntity> findNeedsContinueConfig() {
        return selectListByQuery(QueryWrapper.create()
                .from(CLUSTER_CONFIG_PROGRESS_ENTITY)
                .where(CLUSTER_CONFIG_PROGRESS_ENTITY.CONFIG_STATUS.in(
                        List.of(ConfigStatus.UNCONFIGURED, ConfigStatus.CONFIGURING)
                )));
    }
    
    /**
     * 查询配置已完成的集群（COMPLETED 状态）
     */
    default List<ClusterConfigProgressEntity> findConfigCompleted() {
        return selectListByQuery(QueryWrapper.create()
                .from(CLUSTER_CONFIG_PROGRESS_ENTITY)
                .where(CLUSTER_CONFIG_PROGRESS_ENTITY.CONFIG_STATUS.eq(ConfigStatus.COMPLETED)));
    }
    
    /**
     * 批量查询集群配置进度
     */
    default List<ClusterConfigProgressEntity> findByClusterIds(List<Integer> clusterIds) {
        return selectListByQuery(QueryWrapper.create()
                .from(CLUSTER_CONFIG_PROGRESS_ENTITY)
                .where(CLUSTER_CONFIG_PROGRESS_ENTITY.CLUSTER_ID.in(clusterIds)));
    }
    
    /**
     * 根据集群ID删除配置进度
     */
    default int deleteByClusterId(Integer clusterId) {
        return deleteByQuery(QueryWrapper.create()
                .where(CLUSTER_CONFIG_PROGRESS_ENTITY.CLUSTER_ID.eq(clusterId)));
    }
    
    /**
     * 检查集群配置进度是否存在
     */
    default boolean existsByClusterId(Integer clusterId) {
        return selectCountByQuery(QueryWrapper.create()
                .from(CLUSTER_CONFIG_PROGRESS_ENTITY)
                .where(CLUSTER_CONFIG_PROGRESS_ENTITY.CLUSTER_ID.eq(clusterId))) > 0;
    }
    
    /**
     * 获取集群的下一步骤
     */
    default Integer getNextStep(Integer clusterId) {
        Object result = selectObjectByQuery(QueryWrapper.create()
                .select(CLUSTER_CONFIG_PROGRESS_ENTITY.COMPLETED_STEP)
                .from(CLUSTER_CONFIG_PROGRESS_ENTITY)
                .where(CLUSTER_CONFIG_PROGRESS_ENTITY.CLUSTER_ID.eq(clusterId)));
        
        Integer completedStep = result != null ? (Integer) result : null;
        if (completedStep == null) {
            return 1; // 如果不存在记录，返回第一步
        }
        return completedStep >= 8 ? -1 : completedStep + 1;
    }
    
    /**
     * 获取集群配置进度百分比
     */
    default Integer getProgressPercentage(Integer clusterId) {
        Object result = selectObjectByQuery(QueryWrapper.create()
                .select(CLUSTER_CONFIG_PROGRESS_ENTITY.COMPLETED_STEP)
                .from(CLUSTER_CONFIG_PROGRESS_ENTITY)
                .where(CLUSTER_CONFIG_PROGRESS_ENTITY.CLUSTER_ID.eq(clusterId)));
        
        Integer completedStep = result != null ? (Integer) result : 0;
        return (completedStep * 100) / 8; // 总共8个步骤
    }
}