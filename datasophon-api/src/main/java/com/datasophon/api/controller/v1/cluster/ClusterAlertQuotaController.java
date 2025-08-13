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

import com.datasophon.api.annotation.ClusterId;
import com.datasophon.api.service.ClusterAlertQuotaService;
import com.datasophon.common.model.PageResult;
import com.datasophon.api.dto.Result;
import com.datasophon.dao.entity.ClusterAlertQuotaEntity;
import com.datasophon.common.enums.QuotaState;
import org.springframework.beans.factory.annotation.Autowired;
import com.datasophon.api.annotation.ApiVersion;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Arrays;

@ApiVersion(path = "cluster/alert/quota")
public class ClusterAlertQuotaController {

    @Autowired
    private ClusterAlertQuotaService clusterAlertQuotaService;

    /**
     * list alert quota
     */
    @RequestMapping("/list")
    public Result<Object> info(@ClusterId Long clusterId,
            @RequestParam("alertGroupId") Integer alertGroupId,
            @RequestParam("noticeGroupId") Integer noticeGroupId,
            @RequestParam("quotaName") String quotaName,
            @RequestParam("page") Integer page,
            @RequestParam("pageSize") Integer pageSize) {
        PageResult<ClusterAlertQuotaEntity> pageResult = clusterAlertQuotaService.getAlertQuotaList(clusterId, alertGroupId,
                noticeGroupId, quotaName, page,
                pageSize);
        return Result.success().put("page", pageResult);
    }

    /**
     * enable alert quota
     */
    @RequestMapping("/start")
    public Result<Object> start(@ClusterId Long clusterId,
            @RequestParam("alertQuotaIds") String alertQuotaIds) {
        clusterAlertQuotaService.start(clusterId, alertQuotaIds);
        return Result.success();
    }

    /**
     * disable alert quota
     */
    @RequestMapping("/stop")
    public Result<Object> stop(@ClusterId Long clusterId,
            @RequestParam("alertQuotaIds") String alertQuotaIds) {
        clusterAlertQuotaService.stop(clusterId, alertQuotaIds);
        return Result.success();
    }

    /**
     * save alert quota
     */
    @RequestMapping("/save")
    public Result<Object> save(@RequestBody ClusterAlertQuotaEntity clusterAlertQuotaEntity) {

        clusterAlertQuotaService.saveAlertQuota(clusterAlertQuotaEntity);
        return Result.success();
    }

    /**
     * update alert quota
     */
    @RequestMapping("/update")
    public Result<Object> update(@RequestBody ClusterAlertQuotaEntity clusterAlertQuotaEntity) {
        clusterAlertQuotaEntity.setQuotaState(QuotaState.WAIT_TO_UPDATE);
        clusterAlertQuotaService.updateById(clusterAlertQuotaEntity);

        return Result.success();
    }

    /**
     * delete alert quota
     */
    @RequestMapping("/delete")
    public Result<Object> delete(@RequestBody Integer[] ids) {
        clusterAlertQuotaService.removeByIds(Arrays.asList(ids));

        return Result.success();
    }

}
