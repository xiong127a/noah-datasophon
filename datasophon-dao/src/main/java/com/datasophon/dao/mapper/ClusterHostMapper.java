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
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;
import com.datasophon.dao.enums.MANAGED;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryChain;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 集群主机表
 * 
 * @author gaodayu
 * @email gaodayu2022@163.com
 * @date 2022-04-14 20:32:39
 */
@Mapper
public interface ClusterHostMapper extends BaseMapper<ClusterHostDO> {

    /**
     * 根据主机名查询主机
     */
    default ClusterHostDO selectByHostname(@Param("hostname") String hostname) {
        return QueryChain.of(ClusterHostDO.class)
                .where(ClusterHostDO::getHostname).eq(hostname)
                .one();
    }

    /**
     * 根据IP查询主机
     */
    default ClusterHostDO selectByIp(@Param("ip") String ip) {
        return QueryChain.of(ClusterHostDO.class)
                .where(ClusterHostDO::getIp).eq(ip)
                .one();
    }

    /**
     * 根据集群ID查询所有受管理的主机
     */
    default List<ClusterHostDO> selectByClusterId(@Param("clusterId") Integer clusterId) {
        return QueryChain.of(ClusterHostDO.class)
                .where(ClusterHostDO::getClusterId).eq(clusterId)
                .and(ClusterHostDO::getManaged).eq(MANAGED.YES)
                .list();
    }

    /**
     * 根据集群ID查询所有受管理的主机，按主机名排序
     */
    default List<ClusterHostDO> selectManagedHostsByClusterIdOrderByHostname(@Param("clusterId") Integer clusterId) {
        return QueryChain.of(ClusterHostDO.class)
                .where(ClusterHostDO::getClusterId).eq(clusterId)
                .and(ClusterHostDO::getManaged).eq(MANAGED.YES)
                .orderBy(ClusterHostDO::getHostname).asc()
                .list();
    }

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
        QueryChain<ClusterHostDO> queryChain = QueryChain.of(ClusterHostDO.class)
                .where(ClusterHostDO::getClusterId).eq(clusterId)
                .and(ClusterHostDO::getManaged).eq(MANAGED.YES);

        if (StringUtils.isNotBlank(cpuArchitecture)) {
            queryChain.and(ClusterHostDO::getCpuArchitecture).eq(cpuArchitecture);
        }

        if (hostState != null) {
            queryChain.and(ClusterHostDO::getHostState).eq(hostState);
        }

        if (StringUtils.isNotBlank(ip)) {
            queryChain.and(ClusterHostDO::getIp).like("%" + ip + "%");
        }

        if (StringUtils.isNotBlank(hostname)) {
            queryChain.and(ClusterHostDO::getHostname).like("%" + hostname + "%");
        }

        if ("asc".equals(orderType)) {
            queryChain.orderBy(ClusterHostDO::getHostname).asc();
        } else {
            queryChain.orderBy(ClusterHostDO::getHostname).desc();
        }

        return queryChain.page(page);
    }

    /**
     * 根据ID列表查询主机
     */
    default List<ClusterHostDO> selectByIds(@Param("ids") List<String> ids) {
        return QueryChain.of(ClusterHostDO.class)
                .where(ClusterHostDO::getId).in(ids)
                .list();
    }

    /**
     * 根据主机名列表查询主机
     */
    default List<ClusterHostDO> selectByHostnames(@Param("hostnames") List<String> hostnames) {
        return QueryChain.of(ClusterHostDO.class)
                .where(ClusterHostDO::getHostname).in(hostnames)
                .list();
    }

    /**
     * 根据集群ID和机架查询主机
     */
    default List<ClusterHostDO> selectByClusterIdAndRack(@Param("clusterId") Integer clusterId,
            @Param("rack") String rack) {
        return QueryChain.of(ClusterHostDO.class)
                .where(ClusterHostDO::getClusterId).eq(clusterId)
                .and(ClusterHostDO::getRack).eq(rack)
                .list();
    }

    /**
     * 根据集群ID删除主机
     */
    default int deleteByClusterId(@Param("clusterId") Integer clusterId) {
        return QueryChain.of(ClusterHostDO.class)
                .where(ClusterHostDO::getClusterId).eq(clusterId)
                .remove();
    }
}
