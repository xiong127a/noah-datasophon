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

package com.datasophon.api.controller;

import com.datasophon.api.service.ClusterGroupService;
import com.datasophon.api.utils.ProcessUtils;
import com.datasophon.common.Constants;
import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.ClusterGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/cluster/group")
public class ClusterGroupController {

    @Autowired
    private ClusterGroupService clusterGroupService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    public Result list(@RequestParam("groupName") String groupName, @RequestParam("clusterId") Integer clusterId, @RequestParam("page") Integer page, @RequestParam("pageSize") Integer pageSize) {

        return clusterGroupService.listPage(groupName, clusterId, page, pageSize);
    }

    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public Result info(@PathVariable("id") Integer id) {
        ClusterGroup clusterGroup = clusterGroupService.getById(id);

        return Result.success().put("clusterGroup", clusterGroup);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public Result save(@RequestParam("clusterId") Integer clusterId, @RequestParam("groupName") String groupName) {
        return Constants.PVM_MODE.equals(ProcessUtils.getDepMode(clusterId))?clusterGroupService.saveClusterGroup(clusterId, groupName):clusterGroupService.saveClusterGroupOnKubernetes(clusterId, groupName);
    }

    /**
     * 删除用户组
     */
    @RequestMapping("/delete")
    public Result delete(@RequestParam("clusterId") Integer clusterId,@RequestParam("id") Integer id) {
        return Constants.PVM_MODE.equals(ProcessUtils.getDepMode(clusterId))?clusterGroupService.deleteUserGroup(id):clusterGroupService.deleteUserGroupOnKubernetes(id);

    }

    /**
     * 刷新用户组到主机
     */
    @RequestMapping("/refreshUserGroupToHost")
    public Result refreshUserGroupToHost(@RequestParam("clusterId") Integer clusterId) {

        clusterGroupService.refreshUserGroupToHost(clusterId);

        return Result.success();
    }

}
