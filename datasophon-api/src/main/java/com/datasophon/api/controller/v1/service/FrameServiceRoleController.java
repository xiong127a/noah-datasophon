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

package com.datasophon.api.controller.v1.service;

import com.datasophon.api.service.FrameServiceRoleService;
import com.datasophon.api.vo.Result;
import com.datasophon.dao.entity.FrameServiceRoleEntity;
import org.springframework.beans.factory.annotation.Autowired;
import com.datasophon.api.annotation.ApiVersion;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Arrays;
import java.util.List;

@ApiVersion(path = "frame/service/role")
public class FrameServiceRoleController {

    @Autowired
    private FrameServiceRoleService frameServiceRoleService;

    /**
     * 查询服务对应的角色列表
     */
    @RequestMapping("/getServiceRoleList")
    public Result<List<FrameServiceRoleEntity>> getServiceRoleOfMaster(@RequestParam("clusterId") Integer clusterId,
            @RequestParam("serviceIds") String serviceIds, @RequestParam("serviceRoleType") Integer serviceRoleType) {
        List<FrameServiceRoleEntity> roleList = frameServiceRoleService.getServiceRoleList(clusterId, serviceIds,
                serviceRoleType);
        return Result.success(roleList);
    }

    @RequestMapping("/getNonMasterRoleList")
    public Result<List<FrameServiceRoleEntity>> getNonMasterRoleList(@RequestParam("clusterId") Integer clusterId,
            @RequestParam("serviceIds") String serviceIds) {
        List<FrameServiceRoleEntity> roleList = frameServiceRoleService.getNonMasterRoleList(clusterId, serviceIds);
        return Result.success(roleList);
    }

    @RequestMapping("/getServiceRoleByServiceName")
    public Result<List<FrameServiceRoleEntity>> getServiceRoleByServiceName(
            @RequestParam("clusterId") Integer clusterId,
            @RequestParam("serviceName") String serviceName) {
        List<FrameServiceRoleEntity> roleList = frameServiceRoleService.getServiceRoleByServiceName(clusterId,
                serviceName);
        return Result.success(roleList);
    }

    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public Result<FrameServiceRoleEntity> info(@PathVariable("id") Integer id) {
        FrameServiceRoleEntity frameServiceRole = frameServiceRoleService.getById(id);
        return Result.success(frameServiceRole);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public Result<Void> save(@RequestBody FrameServiceRoleEntity frameServiceRole) {
        boolean saved = frameServiceRoleService.save(frameServiceRole);
        if (saved) {
            return Result.success();
        } else {
            return Result.error("保存失败");
        }
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public Result<Void> update(@RequestBody FrameServiceRoleEntity frameServiceRole) {
        boolean updated = frameServiceRoleService.updateById(frameServiceRole);
        if (updated) {
            return Result.success();
        } else {
            return Result.error("更新失败");
        }
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public Result<Void> delete(@RequestBody Integer[] ids) {
        boolean deleted = frameServiceRoleService.removeByIds(Arrays.asList(ids));
        if (deleted) {
            return Result.success();
        } else {
            return Result.error("删除失败");
        }
    }

}
