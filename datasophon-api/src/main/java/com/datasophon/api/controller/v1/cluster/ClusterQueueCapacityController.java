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

import com.datasophon.api.converter.ClusterQueueCapacityConverter;
import com.datasophon.api.service.ClusterQueueCapacityService;
import com.datasophon.common.dto.ClusterQueueCapacityDTO;
import com.datasophon.common.vo.ClusterQueueCapacityVO;
import com.datasophon.common.vo.ClusterQueueCapacityListVO;
import com.datasophon.common.vo.Result;
import com.datasophon.dao.model.ClusterQueueCapacityList;
import org.springframework.beans.factory.annotation.Autowired;
import com.datasophon.api.annotation.ApiVersion;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 集群队列容量控制器
 * 提供集群队列容量的REST API接口
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
@ApiVersion(path = "cluster/queue/capacity")
public class ClusterQueueCapacityController {

    @Autowired
    private ClusterQueueCapacityService clusterQueueCapacityService;

    @Autowired
    private ClusterQueueCapacityConverter clusterQueueCapacityConverter;

    /**
     * 列表（树形结构）
     */
    @RequestMapping("/list")
    public Result<ClusterQueueCapacityListVO> list(@RequestParam("clusterId") Integer clusterId) {
        // 调用Service层方法，获取ClusterQueueCapacityList
        ClusterQueueCapacityList capacityList = clusterQueueCapacityService.listCapacityQueue(clusterId);
        // Controller层：Model → VO转换
        ClusterQueueCapacityListVO vo = clusterQueueCapacityConverter.capacityListToListVO(capacityList);
        return Result.success(vo);
    }

    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public Result<ClusterQueueCapacityVO> info(@PathVariable("id") Integer id) {
        // 调用Service层方法，获取DTO
        ClusterQueueCapacityDTO dto = clusterQueueCapacityService.getByIdAsDto(id);
        // Controller层：DTO → VO转换
        ClusterQueueCapacityVO vo = clusterQueueCapacityConverter.dtoToVo(dto);
        return Result.success(vo);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public Result<ClusterQueueCapacityVO> save(@RequestBody ClusterQueueCapacityDTO dto) {
        // Controller层直接传递DTO给Service层
        ClusterQueueCapacityDTO savedDto = clusterQueueCapacityService.saveQueueCapacity(dto);
        // Controller层：DTO → VO转换
        ClusterQueueCapacityVO vo = clusterQueueCapacityConverter.dtoToVo(savedDto);
        return Result.success(vo);
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public Result<String> update(@RequestBody ClusterQueueCapacityDTO dto) {
        // Controller层直接传递DTO给Service层
        clusterQueueCapacityService.updateQueueCapacity(dto);
        return Result.success("更新成功");
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public Result<String> delete(@RequestParam("id") Integer id) {
        clusterQueueCapacityService.removeById(id);
        return Result.success("删除成功");
    }

    /**
     * 批量删除
     */
    @RequestMapping("/deleteBatch")
    public Result<String> deleteBatch(@RequestBody Integer[] ids) {
        clusterQueueCapacityService.removeByIds(List.of(ids));
        return Result.success("删除成功");
    }

    /**
     * 刷新队列配置到YARN
     */
    @RequestMapping("/refreshToYarn")
    public Result<String> refreshToYarn(@RequestParam("clusterId") Integer clusterId) throws Exception {
        boolean result = clusterQueueCapacityService.refreshToYarn(clusterId);
        return result ? Result.success("刷新成功") : Result.error("刷新失败");
    }
}
