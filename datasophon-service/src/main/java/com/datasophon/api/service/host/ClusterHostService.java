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

package com.datasophon.api.service.host;

import com.datasophon.common.dto.ClusterRackDTO;
import com.datasophon.common.dto.ClusterServiceRoleInstanceDTO;
import com.datasophon.common.model.PageResult;
import com.datasophon.dao.entity.ClusterHostDO;
import com.datasophon.common.exception.BusinessException;
import com.mybatisflex.core.service.IService;

import java.util.List;

public interface ClusterHostService extends IService<ClusterHostDO> {

    ClusterHostDO getClusterHostByHostname(String hostname);

    ClusterHostDO getClusterHostByIp(String ip);

    PageResult<ClusterHostDO> listByPage(Integer clusterId, String hostname, String ip, String cpuArchitecture,
            Integer hostState,
            String orderField, String orderType, Integer page, Integer pageSize);

    List<ClusterHostDO> getHostListByClusterId(Integer id);

    /**
     * 获取集群所有受管理的主机，按主机名排序
     */
    List<ClusterHostDO> getAllManagedHostsByClusterId(Integer clusterId);

    List<ClusterServiceRoleInstanceDTO> getRoleListByHostname(Integer clusterId, String hostname);

    /**
     * 批量删除主机。
     * 删除主机，首先停止主机上的服务
     * 其次删除主机 worker，同时移除 Prometheus hosts
     * 然后删除主机运行的实例
     *
     * @throws BusinessException 删除失败时抛出异常
     */
    void deleteHosts(String hostIds) throws BusinessException;

    List<ClusterRackDTO> getRack(Integer clusterId);

    void removeHostByClusterId(Integer id);

    void updateBatchNodeLabel(List<String> hostIds, String nodeLabel);

    List<ClusterHostDO> getHostListByIds(List<String> ids);

    void assignRack(Integer clusterId, String rack, String hostIds) throws BusinessException;

    List<ClusterHostDO> getClusterHostByRack(Integer clusterId, String rack);



    /**
     * 批量更新主机状态信息
     *
     * @param hosts 需要更新的主机列表
     */
    void updateBatchHostStatus(List<ClusterHostDO> hosts);

    /**
     * 根据IP列表查询指定集群的主机（用于检查IP重复）
     */
    List<ClusterHostDO> getHostsByIpList(Integer clusterId, List<String> ipList);

    /**
     * 保存主机信息
     *
     * @param clusterHostDO 主机信息
     */
    void saveHost(ClusterHostDO clusterHostDO);
}
