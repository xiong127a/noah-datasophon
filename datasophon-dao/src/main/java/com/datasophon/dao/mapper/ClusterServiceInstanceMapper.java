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
import com.datasophon.dao.enums.ServiceState;
import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 集群服务表
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2024-12-19
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

        /**
         * 根据集群ID和服务名称查询服务实例
         */
        default ClusterServiceInstanceEntity selectByClusterIdAndServiceName(Integer clusterId, String serviceName) {
                QueryWrapper query = QueryWrapper.create()
                                .where(ClusterServiceInstanceEntity::getClusterId).eq(clusterId)
                                .and(ClusterServiceInstanceEntity::getServiceName).eq(serviceName);
                return this.selectOneByQuery(query);
        }

        /**
         * 根据集群ID查询所有服务实例，按排序号升序
         */
        default List<ClusterServiceInstanceEntity> selectByClusterIdOrderBySortNum(Integer clusterId) {
                QueryWrapper query = QueryWrapper.create()
                                .where(ClusterServiceInstanceEntity::getClusterId).eq(clusterId)
                                .orderBy(ClusterServiceInstanceEntity::getSortNum).asc();
                return this.selectListByQuery(query);
        }

        /**
         * 根据集群ID查询运行中的服务实例
         */
        default List<ClusterServiceInstanceEntity> selectRunningServicesByClusterId(Integer clusterId) {
                QueryWrapper query = QueryWrapper.create()
                                .where(ClusterServiceInstanceEntity::getClusterId).eq(clusterId)
                                .and(ClusterServiceInstanceEntity::getServiceState).eq(ServiceState.RUNNING);
                return this.selectListByQuery(query);
        }

        /**
         * 根据ID查询单个实体
         */
        default ClusterServiceInstanceEntity selectById(Integer id) {
                return this.selectOneById(id);
        }

        /**
         * 插入实体
         */
        default int insert(ClusterServiceInstanceEntity entity) {
                return this.insertSelective(entity);
        }

        /**
         * 根据ID更新实体
         */
        default int updateById(ClusterServiceInstanceEntity entity) {
                return this.update(entity);
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
         * 查询所有服务实例
         */
        default List<ClusterServiceInstanceEntity> selectAll() {
                QueryWrapper query = QueryWrapper.create();
                return this.selectListByQuery(query);
        }
}
