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

import com.datasophon.api.service.ClusterServiceInstanceConfigService;
import com.datasophon.common.vo.Result;
import com.datasophon.dao.entity.ClusterServiceInstanceConfigEntity;
import org.springframework.beans.factory.annotation.Autowired;
import com.datasophon.api.annotation.ApiVersion;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Arrays;

@ApiVersion(path = "cluster/service/instance/config")
public class ClusterServiceInstanceConfigController {

    @Autowired
    private ClusterServiceInstanceConfigService clusterServiceInstanceConfigService;


    /**
     * 列表
     */
    @RequestMapping("/getConfigVersion")
    public Result getConfigVersion(@RequestParam("serviceInstanceId") Integer serviceInstanceId, @RequestParam("roleGroupId") Integer roleGroupId) {
        return clusterServiceInstanceConfigService.getConfigVersion(serviceInstanceId, roleGroupId);
    }

    /**
     * 信息
     */
    @RequestMapping("/info")
    public Result info(@RequestParam("serviceInstanceId") Integer serviceInstanceId, @RequestParam("version") Integer version, @RequestParam("roleGroupId") Integer roleGroupId, @RequestParam("page") Integer page,
                       @RequestParam("pageSize") Integer pageSize) {
        return clusterServiceInstanceConfigService.getServiceInstanceConfig(serviceInstanceId, version, roleGroupId,
                page, pageSize);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public Result save(@RequestBody ClusterServiceInstanceConfigEntity clusterServiceInstanceConfig) {
        clusterServiceInstanceConfigService.save(clusterServiceInstanceConfig);

        return Result.success();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public Result update(@RequestBody ClusterServiceInstanceConfigEntity clusterServiceInstanceConfig) {
        clusterServiceInstanceConfigService.updateById(clusterServiceInstanceConfig);

        return Result.success();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public Result delete(@RequestBody Integer[] ids) {
        clusterServiceInstanceConfigService.removeByIds(Arrays.asList(ids));

        return Result.success();
    }

}
