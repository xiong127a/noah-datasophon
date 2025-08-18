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
import com.datasophon.dao.entity.ClusterHostEntity;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import com.datasophon.api.annotation.ApiVersion;
import com.datasophon.api.annotation.ClusterId;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import io.micrometer.core.annotation.Timed;

import java.util.List;

@Slf4j
@ApiVersion(path = "cluster/host")
public class ClusterHostController {

    @Autowired
    private ClusterHostService clusterHostService;

    /**
     * 查询集群所有主机
     * 使用JDK 21虚拟线程和观测性功能
     */
    @GetMapping("/all")
    @Timed(value = "cluster.host.all", description = "获取集群所有主机的时间")
    public Result<List<ClusterHostEntity>> all(@ClusterId Long clusterId) {
        List<ClusterHostEntity> list = clusterHostService.getAllManagedHostsByClusterId(clusterId);
        
        return Result.success(list);
    }

    /**
     * 获取当前线程信息 - 兼容JDK 21特性
     */
    private String getCurrentThreadInfo() {
        Thread thread = Thread.currentThread();
        if (thread.isVirtual()) {
            return String.format("虚拟线程[%s]", thread.getName());
        } else {
            return String.format("平台线程[%s]", thread.getName());
        }
    }



    @GetMapping("/getRoleListByHostname")
    public Result<Object> getRoleListByHostname(
            @ClusterId Long clusterId,
            @RequestParam("hostname") String hostname) {
        var roleList = clusterHostService.getRoleListByHostname(clusterId, hostname);
        return Result.success(roleList);
    }

    @GetMapping("/getRack")
    public Result<Object> getRack(@ClusterId Long clusterId) {
        var rackList = clusterHostService.getRack(clusterId);
        return Result.success(rackList);
    }

    @PostMapping("/assignRack")
    public Result<Void> assignRack(@ClusterId Long clusterId,
            @RequestParam("rack") String rack,
            @RequestParam("hostIds") String hostIds) {
        clusterHostService.assignRack(clusterId, rack, hostIds);
        return Result.success();
    }

    /**
     * 信息
     */
    @GetMapping("/info/{id}")
    public Result<ClusterHostEntity> info(@PathVariable("id") Long id) {
        ClusterHostEntity clusterHost = clusterHostService.getById(id);

        return Result.success(clusterHost);
    }

    /**
     * 保存
     */
    @PostMapping("/save")
    public Result<Void> save(@RequestBody ClusterHostEntity clusterHost) {
        clusterHostService.save(clusterHost);

        return Result.success();
    }

    /**
     * 修改
     */
    @PutMapping("/update")
    public Result<Void> update(@RequestBody ClusterHostEntity clusterHost) {
        clusterHostService.updateById(clusterHost);

        return Result.success();
    }

    /**
     * 删除
     */
    @DeleteMapping("/delete")
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



}
