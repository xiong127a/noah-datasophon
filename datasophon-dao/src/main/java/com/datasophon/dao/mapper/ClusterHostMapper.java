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

import com.datasophon.dao.entity.ClusterHostEntity;
import com.mybatisflex.core.BaseMapper;
import com.datasophon.common.enums.ManagementStatus;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;

import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.ArrayList;
import java.util.List;

// 使用lambda表达式方式，不依赖静态表定义

/**
 * 集群主机表
 * 按照架构重构规范，迁移QueryChain到DAO层
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-01
 */
@Mapper
public interface ClusterHostMapper extends BaseMapper<ClusterHostEntity> {

    /**
     * 根据主机名查询主机
     */
    default ClusterHostEntity selectByHostname(@Param("hostname") String hostname) {
        QueryWrapper query = QueryWrapper.create()
                .where(ClusterHostEntity::getHostname).eq(hostname);
        return this.selectOneByQuery(query);
    }

    /**
     * 根据IP查询主机
     */
    default ClusterHostEntity selectByIp(@Param("ip") String ip) {
        QueryWrapper query = QueryWrapper.create()
                .where(ClusterHostEntity::getIp).eq(ip);
        return this.selectOneByQuery(query);
    }

    /**
     * 根据IP列表查询指定集群的主机（用于检查IP重复）
     */
    default List<ClusterHostEntity> selectByClusterIdAndIpList(@Param("clusterId") Long clusterId,
                                                               @Param("ipList") List<String> ipList) {
        if (ipList == null || ipList.isEmpty()) {
            return new ArrayList<>();
        }
        
        QueryWrapper query = QueryWrapper.create()
                .where(ClusterHostEntity::getClusterId).eq(clusterId)
                .and(ClusterHostEntity::getIp).in(ipList);
        return this.selectListByQuery(query);
    }

    /**
     * 根据集群ID查询所有受管理的主机
     * 注意：配置中状态的主机不计入受管统计
     */
    default List<ClusterHostEntity> selectByClusterId(@Param("clusterId") Long clusterId) {
        QueryWrapper query = QueryWrapper.create()
                .where(ClusterHostEntity::getClusterId).eq(clusterId)
                .and(ClusterHostEntity::getManagementStatus).eq(ManagementStatus.MANAGED);
        return this.selectListByQuery(query);
    }

    /**
     * 根据集群ID查询所有主机（包括未受管和配置中状态）
     */
    default List<ClusterHostEntity> selectAllByClusterId(@Param("clusterId") Long clusterId) {
        QueryWrapper query = QueryWrapper.create()
                .where(ClusterHostEntity::getClusterId).eq(clusterId);
        return this.selectListByQuery(query);
    }

    /**
     * 根据集群ID查询所有受管理的主机，按主机名排序
     * 注意：配置中状态的主机不计入受管统计
     */
    default List<ClusterHostEntity> selectManagedHostsByClusterIdOrderByHostname(@Param("clusterId") Long clusterId) {
        QueryWrapper query = QueryWrapper.create()
                .where(ClusterHostEntity::getClusterId).eq(clusterId)
                .and(ClusterHostEntity::getManagementStatus).eq(ManagementStatus.CONFIGURING)
                .orderBy(ClusterHostEntity::getHostname, true);
        return this.selectListByQuery(query);
    }

    // 向后兼容方法已删除，统一使用managementStatus字段

    /**
     * 分页查询主机，支持主机名筛选
     * 查询可配置状态的主机（未受管和配置中状态）
     */
    default Page<ClusterHostEntity> selectPageByClusterIdAndFilters(Page<ClusterHostEntity> page,
                                                                    @Param("clusterId") Long clusterId,
                                                                    @Param("hostname") String hostname,
                                                                    @Param("ip") String ip,
                                                                    @Param("cpuArchitecture") String cpuArchitecture,
                                                                    @Param("hostState") Integer hostState,
                                                                    @Param("orderType") String orderType) {
        QueryWrapper query = QueryWrapper.create()
                .where(ClusterHostEntity::getClusterId).eq(clusterId)
                .and(ClusterHostEntity::getManagementStatus).in(ManagementStatus.UNMANAGED, ManagementStatus.CONFIGURING);

        if (StringUtils.isNotBlank(cpuArchitecture)) {
            query.and(ClusterHostEntity::getCpuArchitecture).eq(cpuArchitecture);
        }

        if (hostState != null) {
            query.and(ClusterHostEntity::getHostState).eq(hostState);
        }

        if (StringUtils.isNotBlank(ip)) {
            query.and(ClusterHostEntity::getIp).like("%" + ip + "%");
        }

        if (StringUtils.isNotBlank(hostname)) {
            query.and(ClusterHostEntity::getHostname).like("%" + hostname + "%");
        }

        query.orderBy(ClusterHostEntity::getHostname, "asc".equals(orderType));

        return this.paginate(page, query);
    }

    /**
     * 根据ID列表查询主机
     */
    default List<ClusterHostEntity> selectByIds(@Param("ids") List<String> ids) {
        QueryWrapper query = QueryWrapper.create()
                .where(ClusterHostEntity::getId).in(ids);
        return this.selectListByQuery(query);
    }

    /**
     * 根据主机名列表查询主机
     */
    default List<ClusterHostEntity> selectByHostnames(@Param("hostnames") List<String> hostnames) {
        QueryWrapper query = QueryWrapper.create()
                .where(ClusterHostEntity::getHostname).in(hostnames);
        return this.selectListByQuery(query);
    }

    /**
     * 根据集群ID和机架查询主机
     */
    default List<ClusterHostEntity> selectByClusterIdAndRack(@Param("clusterId") Long clusterId,
                                                             @Param("rack") String rack) {
        QueryWrapper query = QueryWrapper.create()
                .where(ClusterHostEntity::getClusterId).eq(clusterId)
                .and(ClusterHostEntity::getRack).eq(rack);
        return this.selectListByQuery(query);
    }

    /**
     * 根据集群ID删除主机
     */
    default void deleteByClusterId(@Param("clusterId") Long clusterId) {
        QueryWrapper query = QueryWrapper.create()
                .where(ClusterHostEntity::getClusterId).eq(clusterId);
        this.deleteByQuery(query);
    }
}
