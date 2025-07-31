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

import com.datasophon.api.service.ClusterYarnQueueService;
import com.datasophon.api.vo.Result;
import com.datasophon.common.model.PageResult;
import com.datasophon.dao.entity.ClusterYarnQueue;
import org.springframework.beans.factory.annotation.Autowired;
import com.datasophon.api.annotation.ApiVersion;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Arrays;

@ApiVersion(path = "cluster/yarn/queue")
public class ClusterYarnQueueController {

    @Autowired
    private ClusterYarnQueueService clusterYarnQueueService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    public Result<Object> list(@RequestParam("clusterId") Integer clusterId, @RequestParam("page") Integer page,
            @RequestParam("pageSize") Integer pageSize) {
        PageResult<ClusterYarnQueue> pageResult = clusterYarnQueueService.listByPage(clusterId, page, pageSize);
        return Result.success(pageResult.getRecords(), pageResult.getTotal());
    }

    /**
     * 刷新队列
     */
    @RequestMapping("/refreshQueues")
    public Result<Void> refreshQueues(@RequestParam("clusterId") Integer clusterId) {
        clusterYarnQueueService.refreshQueues(clusterId);
        return Result.success();
    }

    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public Result<Object> info(@PathVariable("id") Integer id) {
        ClusterYarnQueue clusterYarnQueue = clusterYarnQueueService.getById(id);

        return Result.success().put("clusterYarnQueue", clusterYarnQueue);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public Result<Void> save(@RequestBody ClusterYarnQueue clusterYarnQueue) {
        clusterYarnQueueService.saveQueue(clusterYarnQueue);
        return Result.success();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public Result<Void> update(@RequestBody ClusterYarnQueue clusterYarnQueue) {

        clusterYarnQueueService.updateById(clusterYarnQueue);

        return Result.success();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public Result<Void> delete(@RequestBody Integer[] ids) {
        clusterYarnQueueService.removeByIds(Arrays.asList(ids));

        return Result.success();
    }

}
