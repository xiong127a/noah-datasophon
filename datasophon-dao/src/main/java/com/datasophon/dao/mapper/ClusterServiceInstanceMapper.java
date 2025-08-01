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

import com.datasophon.dao.entity.ClusterServiceInstanceEntity;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;

/**
 * 集群服务表
 *
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
        default String getServiceConfigByClusterIdAndServiceName(@Param("clusterId") Integer clusterId,
                        @Param("serviceName") String serviceName) {
                // 对于复杂的JOIN查询，使用原生查询方式保持数据库兼容性
                QueryWrapper query = QueryWrapper.create()
                                .select("c.config_file_json")
                                .from("t_ddh_cluster_service_instance s")
                                .leftJoin("t_ddh_cluster_service_instance_config c").on("c.service_id = s.id")
                                .where("s.cluster_id = ?", clusterId)
                                .and("s.service_name = ?", serviceName)
                                .orderBy("c.config_version DESC")
                                .limit(1);

                Object result = this.selectObjectByQuery(query);
                return result != null ? (String) result : null;
        }
}
