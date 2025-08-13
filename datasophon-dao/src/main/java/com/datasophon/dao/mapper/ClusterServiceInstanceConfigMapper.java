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

import com.datasophon.dao.entity.ClusterServiceInstanceConfigEntity;
import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryChain;
import com.mybatisflex.core.query.QueryWrapper;
import org.apache.ibatis.annotations.Mapper;

import static com.datasophon.dao.entity.table.ClusterServiceInstanceConfigEntityTableDef.CLUSTER_SERVICE_INSTANCE_CONFIG_ENTITY;

/**
 * 集群服务实例配置Mapper
 * 按照架构重构规范，复杂SQL逻辑在DAO层处理
 * 使用MyBatis-Flex QueryChain Lambda写法
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-06
 */
@Mapper
public interface ClusterServiceInstanceConfigMapper extends BaseMapper<ClusterServiceInstanceConfigEntity> {

    /**
     * 根据服务ID获取最新版本的服务配置
     */
    default ClusterServiceInstanceConfigEntity selectLatestConfigByServiceId(Integer serviceId) {
        return selectOneByQuery(QueryWrapper.create()
                .where(CLUSTER_SERVICE_INSTANCE_CONFIG_ENTITY.SERVICE_ID.eq(serviceId))
                .orderBy(CLUSTER_SERVICE_INSTANCE_CONFIG_ENTITY.CONFIG_VERSION.desc())
                .limit(1));
    }
    
    /**
     * 分页查询服务实例配置列表
     * 支持按集群ID和服务ID过滤
     */
    default Page<ClusterServiceInstanceConfigEntity> selectConfigPageByConditions(
            Long clusterId, Integer serviceId, Integer page, Integer pageSize) {
        var queryChain = QueryChain.of(ClusterServiceInstanceConfigEntity.class)
                .from(CLUSTER_SERVICE_INSTANCE_CONFIG_ENTITY);
        
        // 条件查询
        if (clusterId != null) {
            queryChain.where(CLUSTER_SERVICE_INSTANCE_CONFIG_ENTITY.CLUSTER_ID.eq(clusterId));
        }
        if (serviceId != null) {
            queryChain.where(CLUSTER_SERVICE_INSTANCE_CONFIG_ENTITY.SERVICE_ID.eq(serviceId));
        }
        
        // 分页查询，按更新时间降序
        return queryChain.orderBy(CLUSTER_SERVICE_INSTANCE_CONFIG_ENTITY.UPDATE_TIME.desc())
                .page(Page.of(page, pageSize));
    }
}
