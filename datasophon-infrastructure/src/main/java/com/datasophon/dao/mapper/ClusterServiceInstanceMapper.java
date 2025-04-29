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
import com.datasophon.dao.entity.ClusterServiceInstanceEntity;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

import com.github.yulichang.base.MPJBaseMapper;
import com.github.yulichang.wrapper.MPJLambdaWrapper;

/**
 * 集群服务表
 * 
 * @author gaodayu
 * @email gaodayu2022@163.com
 * @date 2022-04-24 16:25:17
 */
@Mapper
public interface ClusterServiceInstanceMapper extends MPJBaseMapper<ClusterServiceInstanceEntity> {

    /**
     * 根据集群ID和服务名称获取服务配置
     *
     * @param clusterId   集群ID
     * @param serviceName 服务名称
     * @return 服务配置文件JSON字符串
     */
    default String getServiceConfigByClusterIdAndServiceName(@Param("clusterId") Integer clusterId,
            @Param("serviceName") String serviceName) {
        MPJLambdaWrapper<ClusterServiceInstanceEntity> wrapper = new MPJLambdaWrapper<ClusterServiceInstanceEntity>()
                .selectAs(ClusterServiceInstanceConfigEntity::getConfigFileJson, "config_file_json")
                .leftJoin(ClusterServiceInstanceConfigEntity.class,
                        ClusterServiceInstanceConfigEntity::getServiceId,
                        ClusterServiceInstanceEntity::getId)
                .eq(ClusterServiceInstanceEntity::getClusterId, clusterId)
                .eq(ClusterServiceInstanceEntity::getServiceName, serviceName)
                .orderByDesc(ClusterServiceInstanceConfigEntity::getConfigVersion)
                .last("limit 1");

        Map<String, Object> result = selectJoinMap(wrapper);

        return result != null ? (String) result.get("config_file_json") : null;
    }
}
