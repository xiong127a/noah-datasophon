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

package com.datasophon.api.controller.v1.system;

import com.datasophon.api.dto.Result;
import com.datasophon.common.vo.AlertGroupVO;
import com.datasophon.common.dto.AlertGroupDTO;
import com.datasophon.common.model.PageResult;
import com.datasophon.api.service.AlertGroupService;
import com.datasophon.api.converter.AlertGroupConverter;
import com.datasophon.api.annotation.ApiVersion;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * 告警组控制器
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-14
 */
@ApiVersion(path = "alert/group")
public class AlertGroupController {

    private final AlertGroupService alertGroupService;
    private final AlertGroupConverter alertGroupConverter;

    public AlertGroupController(AlertGroupService alertGroupService, AlertGroupConverter alertGroupConverter) {
        this.alertGroupService = alertGroupService;
        this.alertGroupConverter = alertGroupConverter;
    }

    /**
     * 获取告警组分页列表
     */
    @GetMapping("/list")
    public Result<PageResult<AlertGroupVO>> list(
            @RequestParam("clusterId") Integer clusterId,
            @RequestParam(value = "alertGroupName", required = false) String alertGroupName,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {

        PageResult<AlertGroupDTO> dtoPageResult = alertGroupService.getAlertGroupList(
                clusterId, alertGroupName, page, pageSize);

        List<AlertGroupVO> voList = alertGroupConverter.dtoListToVoList(dtoPageResult.getRecords());
        PageResult<AlertGroupVO> voPageResult = PageResult.of(voList, dtoPageResult.getTotal(), page, pageSize);

        return Result.success(voPageResult);
    }

    /**
     * 获取告警组详情
     */
    @GetMapping("/info/{id}")
    public Result<AlertGroupVO> info(@PathVariable("id") Integer id) {
        AlertGroupDTO alertGroupDTO = alertGroupService.getAlertGroupById(id);

        if (alertGroupDTO == null) {
            return Result.error("告警组不存在");
        }

        AlertGroupVO alertGroupVO = alertGroupConverter.dtoToVo(alertGroupDTO);
        return Result.success(alertGroupVO);
    }

    /**
     * 创建告警组
     */
    @PostMapping("/save")
    public Result<AlertGroupVO> save(@RequestBody AlertGroupDTO alertGroupDTO) {
        AlertGroupDTO savedDTO = alertGroupService.saveAlertGroup(alertGroupDTO);
        AlertGroupVO alertGroupVO = alertGroupConverter.dtoToVo(savedDTO);
        return Result.success(alertGroupVO);
    }

    /**
     * 更新告警组
     */
    @PutMapping("/update")
    public Result<AlertGroupVO> update(@RequestBody AlertGroupDTO alertGroupDTO) {
        AlertGroupDTO updatedDTO = alertGroupService.updateAlertGroup(alertGroupDTO);
        AlertGroupVO alertGroupVO = alertGroupConverter.dtoToVo(updatedDTO);
        return Result.success(alertGroupVO);
    }

    /**
     * 删除告警组
     */
    @DeleteMapping("/delete")
    public Result<String> delete(@RequestBody Integer[] ids) {
        try {
            List<Integer> idList = Arrays.asList(ids);
            boolean deleted = alertGroupService.deleteAlertGroups(idList);

            return deleted ? Result.success("删除成功") : Result.error("删除失败");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取所有告警组
     */
    @GetMapping("/all")
    public Result<List<AlertGroupVO>> getAllAlertGroups() {
        List<AlertGroupDTO> dtoList = alertGroupService.getAllAlertGroups();
        List<AlertGroupVO> voList = alertGroupConverter.dtoListToVoList(dtoList);
        return Result.success(voList);
    }

}
