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

package com.datasophon.api.controller.v1.cluster;

import com.datasophon.api.service.host.ClusterHostService;
import com.datasophon.api.dto.Result;
import com.datasophon.common.model.HostInfo;
import com.datasophon.dao.entity.ClusterHostDO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import com.datasophon.api.annotation.ApiVersion;
import com.datasophon.api.annotation.ClusterId;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Slf4j
@ApiVersion(path = "cluster/host")
public class ClusterHostController {

    @Autowired
    private ClusterHostService clusterHostService;

    /**
     * 查询集群所有主机
     */
    @RequestMapping("/all")
    public Result<List<ClusterHostDO>> all(@ClusterId Integer clusterId) {
        List<ClusterHostDO> list = clusterHostService.getAllManagedHostsByClusterId(clusterId);
        return Result.success(list);
    }

    /**
     * 查询集群所有主机
     */
    @RequestMapping("/list")
    public Result<Object> list(@ClusterId Integer clusterId,
            @RequestParam("hostname") String hostname,
            @RequestParam("ip") String ip,
            @RequestParam("cpuArchitecture") String cpuArchitecture,
            @RequestParam("hostState") Integer hostState,
            @RequestParam("orderField") String orderField,
            @RequestParam("orderType") String orderType,
            @RequestParam("page") Integer page,
            @RequestParam("pageSize") Integer pageSize) {
        com.datasophon.common.model.PageResult<ClusterHostDO> pageResult = clusterHostService.listByPage(clusterId,
                hostname, ip, cpuArchitecture, hostState, orderField, orderType,
                page, pageSize);
        return Result.success(pageResult.getRecords(), pageResult.getTotal());
    }

    @RequestMapping("/getRoleListByHostname")
    public Result<List<com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity>> getRoleListByHostname(
            @ClusterId Integer clusterId,
            @RequestParam("hostname") String hostname) {
        List<com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity> roleList = clusterHostService
                .getRoleListByHostname(clusterId, hostname);
        return Result.success(roleList);
    }

    @RequestMapping("/getRack")
    public Result<List<com.datasophon.dao.entity.ClusterRack>> getRack(@ClusterId Integer clusterId) {
        List<com.datasophon.dao.entity.ClusterRack> rackList = clusterHostService.getRack(clusterId);
        return Result.success(rackList);
    }

    @RequestMapping("/assignRack")
    public Result<Void> assignRack(@ClusterId Integer clusterId,
            @RequestParam("rack") String rack,
            @RequestParam("hostIds") String hostIds) {
        clusterHostService.assignRack(clusterId, rack, hostIds);
        return Result.success();
    }

    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public Result<ClusterHostDO> info(@PathVariable("id") Integer id) {
        ClusterHostDO clusterHost = clusterHostService.getById(id);

        return Result.success(clusterHost);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public Result<Void> save(@RequestBody ClusterHostDO clusterHost) {
        clusterHostService.save(clusterHost);

        return Result.success();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public Result<Void> update(@RequestBody ClusterHostDO clusterHost) {
        clusterHostService.updateById(clusterHost);

        return Result.success();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public Result<Void> delete(@RequestParam("hostIds") String hostIds) {
        if (StringUtils.isBlank(hostIds)) {
            return Result.error("请选择移除的主机!");
        }
        try {
            clusterHostService.deleteHosts(hostIds);
            return Result.success();
        } catch (Exception e) {
            log.warn("移除主机异常.", e);
            return Result.error("移除主机异常, Cause: " + e.getMessage());
        }
    }

    /**
     * Kubernetes配置集群时添加主机
     */
    @RequestMapping(value = "/saveKubernetesHost", method = RequestMethod.POST)
    public Result<Void> saveKubernetesHost(@RequestBody List<HostInfo> hostInfoList,
            @ClusterId Integer clusterId) {
        clusterHostService.saveKubernetesHost(hostInfoList, clusterId);
        return Result.success();
    }

    /**
     * 直接保存K8S主机信息（使用从K8S API获取的完整ClusterHostDO信息）
     */
    @RequestMapping(value = "/saveKubernetesHostDirect", method = RequestMethod.POST)
    public Result<Void> saveKubernetesHostDirect(@RequestBody List<ClusterHostDO> kubernetesHosts,
            @ClusterId Integer clusterId) {
        clusterHostService.saveKubernetesHostDirect(kubernetesHosts, clusterId);
        return Result.success();
    }

    /**
     * 获取K8S模式下的完整硬件信息
     */
    @RequestMapping(value = "/getK8sHostsWithHardwareInfo", method = RequestMethod.GET)
    public Result<List<ClusterHostDO>> getK8sHostsWithHardwareInfo(@ClusterId Integer clusterId) {
        List<ClusterHostDO> hostList = clusterHostService.getK8sHostsWithHardwareInfo(clusterId);
        return Result.success(hostList);
    }

}
