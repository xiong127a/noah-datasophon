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
import org.apache.ibatis.annotations.Select;

import java.util.Map;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;

/**
 * 集群服务表
 * 
 * @author gaodayu
 * @email gaodayu2022@163.com
 * @date 2022-04-24 16:25:17
 */
@Mapper
public interface ClusterServiceInstanceMapper extends BaseMapper<ClusterServiceInstanceEntity> {

        /**
         * 根据集群ID和服务名称获取服务配置
         *
         * @param clusterId   集群ID
         * @param serviceName 服务名称
         * @return 服务配置文件JSON字符串
         */
        @Select("SELECT c.config_file_json FROM t_ddh_cluster_service_instance s " +
                        "LEFT JOIN t_ddh_cluster_service_instance_config c ON c.service_id = s.id " +
                        "WHERE s.cluster_id = #{clusterId} AND s.service_name = #{serviceName} " +
                        "ORDER BY c.config_version DESC LIMIT 1")
        String getServiceConfigByClusterIdAndServiceName(@Param("clusterId") Integer clusterId,
                        @Param("serviceName") String serviceName);
}
