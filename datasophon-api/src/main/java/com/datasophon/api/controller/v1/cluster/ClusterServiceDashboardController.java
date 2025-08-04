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

import com.datasophon.api.converter.ClusterServiceDashboardConverter;
import com.datasophon.api.service.ClusterServiceDashboardService;
import com.datasophon.common.dto.ClusterServiceDashboardDTO;
import com.datasophon.common.vo.ClusterServiceDashboardVO;
import com.datasophon.api.dto.Result;
import com.datasophon.dao.entity.ClusterServiceDashboard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import com.datasophon.api.annotation.ApiVersion;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 集群服务仪表盘控制器
 * 提供集群服务仪表盘的REST API接口
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-01
 */
@ApiVersion(path = "cluster/service/dashboard")
public class ClusterServiceDashboardController {

    private static final Logger logger = LoggerFactory.getLogger(ClusterServiceDashboardController.class);

    @Autowired
    private ClusterServiceDashboardService clusterServiceDashboardService;

    @Autowired
    private ClusterServiceDashboardConverter converter;

    /**
     * 获取仪表盘URL
     * 
     * @param clusterId 集群ID
     * @return Result<String>
     */
    @RequestMapping("/getDashboardUrl")
    public Result<String> getDashboardUrl(@RequestParam("clusterId") Integer clusterId) {
        String dashboardUrl = clusterServiceDashboardService.getDashboardUrl(clusterId);
        return Result.success(dashboardUrl);
    }

    /**
     * 获取Datasophon仪表盘URL
     * 
     * @param clusterId 集群ID
     * @return Result<String>
     */
    @RequestMapping("/getDatasophonDashboard")
    public Result<String> getDatasophonDashboard(@RequestParam("clusterId") Integer clusterId) {
        String dashboardUrl = clusterServiceDashboardService.getDatasophonDashboard(clusterId);
        return Result.success(dashboardUrl);
    }

    /**
     * 根据ID获取仪表盘信息
     */
    @RequestMapping("/info/{id}")
    public Result<ClusterServiceDashboardVO> info(@PathVariable("id") Integer id) {
        ClusterServiceDashboardDTO dto = clusterServiceDashboardService.getByIdAsDto(id);
        if (dto == null) {
            return Result.error("仪表盘不存在");
        }
        ClusterServiceDashboardVO vo = converter.dtoToVo(dto);
        return Result.success(vo);
    }

    /**
     * 保存仪表盘
     */
    @RequestMapping("/save")
    public Result<ClusterServiceDashboardVO> save(@RequestBody ClusterServiceDashboardDTO dto) {
        ClusterServiceDashboardDTO savedDto = clusterServiceDashboardService.saveDashboard(dto);
        ClusterServiceDashboardVO vo = converter.dtoToVo(savedDto);
        return Result.success(vo);
    }

    /**
     * 更新仪表盘
     */
    @RequestMapping("/update")
    public Result<String> update(@RequestBody ClusterServiceDashboardDTO dto) {
        clusterServiceDashboardService.updateDashboard(dto);
        return Result.success("更新成功");
    }

    /**
     * 删除仪表盘
     */
    @RequestMapping("/delete")
    public Result<String> delete(@RequestBody Integer[] ids) {
        clusterServiceDashboardService.removeByIds(List.of(ids));
        return Result.success("删除成功");
    }

    /**
     * 获取所有仪表盘
     */
    @RequestMapping("/list")
    public Result<List<ClusterServiceDashboardVO>> getAllDashboards() {
        try {
            List<ClusterServiceDashboard> entities = clusterServiceDashboardService.list();
            List<ClusterServiceDashboardVO> vos = converter.entityListToVoList(entities);
            return Result.success(vos);
        } catch (Exception e) {
            logger.error("获取仪表盘列表失败", e);
            return Result.error("获取仪表盘列表失败");
        }
    }
}
