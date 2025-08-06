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

import com.datasophon.api.service.ClusterServiceRoleInstanceConfigService;
import com.datasophon.api.dto.Result;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceConfigEntity;
import org.springframework.beans.factory.annotation.Autowired;
import com.datasophon.api.annotation.ApiVersion;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Arrays;
import java.util.Map;

@ApiVersion(path = "cluster/service/role/instance/config")
public class ClusterServiceRoleInstanceConfigController {

    @Autowired
    private ClusterServiceRoleInstanceConfigService clusterServiceRoleInstanceConfigService;

    /**
     * 列表
     */
    @GetMapping("/list")
    public Result<Object> list(@RequestParam Map<String, Object> params) {
        // 实现具体的列表查询逻辑
        return Result.success();
    }

    /**
     * 信息
     */
    @GetMapping("/info/{id}")
    public Result<Object> info(@PathVariable("id") Integer id) {
        ClusterServiceRoleInstanceConfigEntity clusterServiceRoleInstanceConfig =
                clusterServiceRoleInstanceConfigService.getById(id);

        return Result.success().put("clusterServiceRoleInstanceConfig", clusterServiceRoleInstanceConfig);
    }

    /**
     * 保存
     */
    @PostMapping("/save")
    public Result<Object> save(@RequestBody ClusterServiceRoleInstanceConfigEntity clusterServiceRoleInstanceConfig) {
        clusterServiceRoleInstanceConfigService.save(clusterServiceRoleInstanceConfig);

        return Result.success();
    }

    /**
     * 修改
     */
    @PutMapping("/update")
    public Result<Object> update(@RequestBody ClusterServiceRoleInstanceConfigEntity clusterServiceRoleInstanceConfig) {
        clusterServiceRoleInstanceConfigService.updateById(clusterServiceRoleInstanceConfig);

        return Result.success();
    }

    /**
     * 删除
     */
    @DeleteMapping("/delete")
    public Result<Object> delete(@RequestBody Integer[] ids) {
        clusterServiceRoleInstanceConfigService.removeByIds(Arrays.asList(ids));

        return Result.success();
    }

}
