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

import com.mybatisflex.core.BaseMapper;
import com.datasophon.dao.entity.ClusterHostDO;
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
public interface ClusterHostMapper extends BaseMapper<ClusterHostDO> {

    /**
     * 根据主机名查询主机
     */
    default ClusterHostDO selectByHostname(@Param("hostname") String hostname) {
        QueryWrapper query = QueryWrapper.create()
                .where(ClusterHostDO::getHostname).eq(hostname);
        return this.selectOneByQuery(query);
    }

    /**
     * 根据IP查询主机
     */
    default ClusterHostDO selectByIp(@Param("ip") String ip) {
        QueryWrapper query = QueryWrapper.create()
                .where(ClusterHostDO::getIp).eq(ip);
        return this.selectOneByQuery(query);
    }

    /**
     * 根据IP列表查询指定集群的主机（用于检查IP重复）
     */
    default List<ClusterHostDO> selectByClusterIdAndIpList(@Param("clusterId") Integer clusterId, 
                                                           @Param("ipList") List<String> ipList) {
        if (ipList == null || ipList.isEmpty()) {
            return new ArrayList<>();
        }
        
        QueryWrapper query = QueryWrapper.create()
                .where(ClusterHostDO::getClusterId).eq(clusterId)
                .and(ClusterHostDO::getIp).in(ipList);
        return this.selectListByQuery(query);
    }

    /**
     * 根据集群ID查询所有受管理的主机
     * 注意：配置中状态的主机不计入受管统计
     */
    default List<ClusterHostDO> selectByClusterId(@Param("clusterId") Integer clusterId) {
        QueryWrapper query = QueryWrapper.create()
                .where(ClusterHostDO::getClusterId).eq(clusterId)
                .and(ClusterHostDO::getManagementStatus).eq(ManagementStatus.MANAGED);
        return this.selectListByQuery(query);
    }

    /**
     * 根据集群ID查询所有主机（包括未受管和配置中状态）
     */
    default List<ClusterHostDO> selectAllByClusterId(@Param("clusterId") Integer clusterId) {
        QueryWrapper query = QueryWrapper.create()
                .where(ClusterHostDO::getClusterId).eq(clusterId);
        return this.selectListByQuery(query);
    }

    /**
     * 根据集群ID查询所有受管理的主机，按主机名排序
     * 注意：配置中状态的主机不计入受管统计
     */
    default List<ClusterHostDO> selectManagedHostsByClusterIdOrderByHostname(@Param("clusterId") Integer clusterId) {
        QueryWrapper query = QueryWrapper.create()
                .where(ClusterHostDO::getClusterId).eq(clusterId)
                .and(ClusterHostDO::getManagementStatus).eq(ManagementStatus.MANAGED)
                .orderBy(ClusterHostDO::getHostname, true);
        return this.selectListByQuery(query);
    }

    // 向后兼容方法已删除，统一使用managementStatus字段

    /**
     * 分页查询主机，支持主机名筛选
     */
    default Page<ClusterHostDO> selectPageByClusterIdAndFilters(Page<ClusterHostDO> page,
            @Param("clusterId") Integer clusterId,
            @Param("hostname") String hostname,
            @Param("ip") String ip,
            @Param("cpuArchitecture") String cpuArchitecture,
            @Param("hostState") Integer hostState,
            @Param("orderType") String orderType) {
        QueryWrapper query = QueryWrapper.create()
                .where(ClusterHostDO::getClusterId).eq(clusterId)
                .and(ClusterHostDO::getManagementStatus).eq(ManagementStatus.MANAGED);

        if (StringUtils.isNotBlank(cpuArchitecture)) {
            query.and(ClusterHostDO::getCpuArchitecture).eq(cpuArchitecture);
        }

        if (hostState != null) {
            query.and(ClusterHostDO::getHostState).eq(hostState);
        }

        if (StringUtils.isNotBlank(ip)) {
            query.and(ClusterHostDO::getIp).like("%" + ip + "%");
        }

        if (StringUtils.isNotBlank(hostname)) {
            query.and(ClusterHostDO::getHostname).like("%" + hostname + "%");
        }

        query.orderBy(ClusterHostDO::getHostname, "asc".equals(orderType));

        return this.paginate(page, query);
    }

    /**
     * 根据ID列表查询主机
     */
    default List<ClusterHostDO> selectByIds(@Param("ids") List<String> ids) {
        QueryWrapper query = QueryWrapper.create()
                .where(ClusterHostDO::getId).in(ids);
        return this.selectListByQuery(query);
    }

    /**
     * 根据主机名列表查询主机
     */
    default List<ClusterHostDO> selectByHostnames(@Param("hostnames") List<String> hostnames) {
        QueryWrapper query = QueryWrapper.create()
                .where(ClusterHostDO::getHostname).in(hostnames);
        return this.selectListByQuery(query);
    }

    /**
     * 根据集群ID和机架查询主机
     */
    default List<ClusterHostDO> selectByClusterIdAndRack(@Param("clusterId") Integer clusterId,
            @Param("rack") String rack) {
        QueryWrapper query = QueryWrapper.create()
                .where(ClusterHostDO::getClusterId).eq(clusterId)
                .and(ClusterHostDO::getRack).eq(rack);
        return this.selectListByQuery(query);
    }

    /**
     * 根据集群ID删除主机
     */
    default void deleteByClusterId(@Param("clusterId") Integer clusterId) {
        QueryWrapper query = QueryWrapper.create()
                .where(ClusterHostDO::getClusterId).eq(clusterId);
        this.deleteByQuery(query);
    }
}
