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
import com.datasophon.api.converter.ClusterAlertHistoryConverter;
import com.datasophon.api.service.ClusterAlertHistoryService;
import com.datasophon.common.dto.ClusterAlertHistoryDTO;
import com.datasophon.common.model.PageResult;
import com.datasophon.common.vo.ClusterAlertHistoryVO;
import com.datasophon.api.dto.Result;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import com.datasophon.api.annotation.ApiVersion;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 集群告警历史控制器
 * 提供集群告警历史的REST API接口
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
@ApiVersion(path = "cluster/alert/history")
public class ClusterAlertHistoryController {

    @Autowired
    private ClusterAlertHistoryService clusterAlertHistoryService;

    @Autowired
    private ClusterAlertHistoryConverter clusterAlertHistoryConverter;

    /**
     * 根据服务实例ID获取告警列表
     */
    @RequestMapping("/getAlertList")
    public Result<List<ClusterAlertHistoryVO>> getAlertList(
            @RequestParam("serviceInstanceId") Integer serviceInstanceId) {
        try {
            // 调用Service层方法，获取DTO列表
            List<ClusterAlertHistoryDTO> dtoList = clusterAlertHistoryService.getAlertList(serviceInstanceId);
            // Controller层：DTO → VO转换
            List<ClusterAlertHistoryVO> voList = dtoList.stream()
                    .map(clusterAlertHistoryConverter::dtoToVo)
                    .toList();
            return Result.success(voList);
        } catch (Exception e) {
            return Result.error("查询告警历史失败");
        }
    }

    /**
     * 分页查询所有告警历史
     */
    @RequestMapping("/getAllAlertList")
    public Result<PageResult<ClusterAlertHistoryVO>> getAllAlertList(@ClusterId Integer clusterId,
            @RequestParam("page") Integer page,
            @RequestParam("pageSize") Integer pageSize) {
        try {
            // 调用Service层方法，获取DTO分页结果
            PageResult<ClusterAlertHistoryDTO> dtoPageResult = clusterAlertHistoryService.getAllAlertList(clusterId,
                    page, pageSize);
            // Controller层：DTO → VO转换
            List<ClusterAlertHistoryVO> voList = dtoPageResult.getRecords().stream()
                    .map(clusterAlertHistoryConverter::dtoToVo)
                    .toList();
            PageResult<ClusterAlertHistoryVO> voPageResult = PageResult.of(
                    voList,
                    dtoPageResult.getTotal(),
                    page,
                    pageSize);
            return Result.success(voPageResult);
        } catch (Exception e) {
            return Result.error("查询告警历史失败");
        }
    }

    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public Result<ClusterAlertHistoryVO> info(@PathVariable("id") Integer id) {
        // 调用Service层方法，获取DTO
        ClusterAlertHistoryDTO dto = clusterAlertHistoryService.getByIdAsDto(id);
        // Controller层：DTO → VO转换
        ClusterAlertHistoryVO vo = clusterAlertHistoryConverter.dtoToVo(dto);
        return Result.success(vo);
    }

    /**
     * 保存告警历史（异步处理告警消息）
     */
    @RequestMapping("/save")
    public Result<String> save(@RequestBody String alertMessage) {
        clusterAlertHistoryService.saveAlertHistory(alertMessage);
        return Result.success("保存告警消息成功");
    }

    /**
     * 保存告警历史DTO
     */
    @RequestMapping("/saveDto")
    public Result<String> saveDto(@RequestBody ClusterAlertHistoryDTO dto) {
        // Controller层直接传递DTO给Service层
        clusterAlertHistoryService.saveAlertHistoryDto(dto);
        return Result.success("保存成功");
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public Result<String> update(@RequestBody ClusterAlertHistoryDTO dto) {
        // Controller层直接传递DTO给Service层
        clusterAlertHistoryService.updateAlertHistory(dto);
        return Result.success("更新成功");
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public Result<String> delete(@RequestBody Integer[] ids) {
        clusterAlertHistoryService.removeByIds(List.of(ids));
        return Result.success("删除成功");
    }

}
