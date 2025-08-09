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

import com.datasophon.api.converter.FrameServiceRoleConverter;
import com.datasophon.api.service.FrameServiceRoleService;
import com.datasophon.common.dto.FrameServiceRoleDTO;
import com.datasophon.common.vo.FrameServiceRoleVO;
import com.datasophon.api.dto.Result;
import com.datasophon.api.annotation.ClusterId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.datasophon.api.annotation.ApiVersion;

import java.util.Arrays;
import java.util.List;

/**
 * 框架服务角色控制器
 * 按照三层架构规范，使用DTO接收请求，VO返回响应
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
@Slf4j
@ApiVersion(path = "frame/service/role")
@RequiredArgsConstructor
public class FrameServiceRoleController {

    private final FrameServiceRoleService frameServiceRoleService;
    private final FrameServiceRoleConverter frameServiceRoleConverter;

    /**
     * 查询服务对应的角色列表
     */
    @GetMapping("/getServiceRoleList")
    public Result<List<FrameServiceRoleVO>> getServiceRoleOfMaster(
            @ClusterId Integer clusterId,
            @RequestParam("serviceIds") String serviceIds,
            @RequestParam("serviceRoleType") Integer serviceRoleType) {
        try {
            List<FrameServiceRoleDTO> roleList = frameServiceRoleService.getServiceRoleList(clusterId, serviceIds,
                    serviceRoleType);
            List<FrameServiceRoleVO> roleVOList = frameServiceRoleConverter.dtoListToVoList(roleList);
            return Result.success(roleVOList);
        } catch (Exception e) {
            log.error("查询服务角色列表失败: {}", e.getMessage(), e);
            return Result.error("查询服务角色列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取非Master角色列表
     */
    @GetMapping("/getNonMasterRoleList")
    public Result<List<FrameServiceRoleVO>> getNonMasterRoleList(
            @ClusterId Integer clusterId,
            @RequestParam("serviceIds") String serviceIds) {
        try {
            List<FrameServiceRoleDTO> roleList = frameServiceRoleService.getNonMasterRoleList(clusterId, serviceIds);
            List<FrameServiceRoleVO> roleVOList = frameServiceRoleConverter.dtoListToVoList(roleList);
            return Result.success(roleVOList);
        } catch (Exception e) {
            log.error("查询非Master角色列表失败: {}", e.getMessage(), e);
            return Result.error("查询非Master角色列表失败: " + e.getMessage());
        }
    }

    /**
     * 根据服务名称获取服务角色列表
     */
    @GetMapping("/getServiceRoleByServiceName")
    public Result<List<FrameServiceRoleVO>> getServiceRoleByServiceName(
            @RequestParam("clusterId") Integer clusterId,
            @RequestParam("serviceName") String serviceName) {
        try {
            List<FrameServiceRoleDTO> roleList = frameServiceRoleService.getServiceRoleByServiceName(clusterId,
                    serviceName);
            List<FrameServiceRoleVO> roleVOList = frameServiceRoleConverter.dtoListToVoList(roleList);
            return Result.success(roleVOList);
        } catch (Exception e) {
            log.error("根据服务名称查询角色列表失败: {}", e.getMessage(), e);
            return Result.error("根据服务名称查询角色列表失败: " + e.getMessage());
        }
    }

    /**
     * 根据ID获取服务角色信息
     */
    @GetMapping("/info/{id}")
    public Result<FrameServiceRoleVO> info(@PathVariable("id") Integer id) {
        try {
            FrameServiceRoleDTO frameServiceRoleDTO = frameServiceRoleService.getFrameServiceRoleById(id);
            FrameServiceRoleVO frameServiceRoleVO = frameServiceRoleConverter.dtoToVo(frameServiceRoleDTO);
            return Result.success(frameServiceRoleVO);
        } catch (Exception e) {
            log.error("获取服务角色信息失败: {}", e.getMessage(), e);
            return Result.error("获取服务角色信息失败: " + e.getMessage());
        }
    }

    /**
     * 保存服务角色
     */
    @PostMapping("/save")
    public Result<FrameServiceRoleVO> save(@RequestBody FrameServiceRoleDTO frameServiceRoleDTO) {
        try {
            FrameServiceRoleDTO savedDTO = frameServiceRoleService.saveFrameServiceRole(frameServiceRoleDTO);
            FrameServiceRoleVO frameServiceRoleVO = frameServiceRoleConverter.dtoToVo(savedDTO);
            return Result.success(frameServiceRoleVO);
        } catch (Exception e) {
            log.error("保存服务角色失败: {}", e.getMessage(), e);
            return Result.error("保存服务角色失败: " + e.getMessage());
        }
    }

    /**
     * 更新服务角色
     */
    @PutMapping("/update")
    public Result<FrameServiceRoleVO> update(@RequestBody FrameServiceRoleDTO frameServiceRoleDTO) {
        try {
            FrameServiceRoleDTO updatedDTO = frameServiceRoleService.updateFrameServiceRole(frameServiceRoleDTO);
            FrameServiceRoleVO frameServiceRoleVO = frameServiceRoleConverter.dtoToVo(updatedDTO);
            return Result.success(frameServiceRoleVO);
        } catch (Exception e) {
            log.error("更新服务角色失败: {}", e.getMessage(), e);
            return Result.error("更新服务角色失败: " + e.getMessage());
        }
    }

    /**
     * 批量删除服务角色
     */
    @DeleteMapping("/delete")
    public Result<Boolean> delete(@RequestBody Integer[] ids) {
        try {
            boolean deleted = frameServiceRoleService.removeFrameServiceRoleByIds(Arrays.asList(ids));
            return Result.success(deleted);
        } catch (Exception e) {
            log.error("删除服务角色失败: {}", e.getMessage(), e);
            return Result.error("删除服务角色失败: " + e.getMessage());
        }
    }

}
