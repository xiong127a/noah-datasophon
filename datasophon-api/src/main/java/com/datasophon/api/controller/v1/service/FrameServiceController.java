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

import cn.hutool.core.io.FileUtil;
import com.datasophon.api.converter.FrameServiceConverter;
import com.datasophon.api.service.FrameServiceRoleService;
import com.datasophon.api.service.FrameServiceService;
import com.datasophon.common.Constants;
import com.datasophon.common.dto.FrameServiceDTO;
import com.datasophon.common.vo.FrameServiceVO;
import com.datasophon.api.dto.Result;
import com.datasophon.dao.entity.ClusterServiceInstanceEntity;
import com.datasophon.dao.entity.FrameServiceRoleEntity;
import com.mybatisflex.core.query.QueryChain;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.datasophon.api.annotation.ApiVersion;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import java.io.File;
import java.util.List;

/**
 * 集群框架版本服务控制器
 * 按照三层架构规范，使用DTO接收请求，VO返回响应
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
@Slf4j
@Api(tags = "集群框架服务管理")
@ApiVersion(path = "frame/service")
@RequiredArgsConstructor
public class FrameServiceController {

    private final FrameServiceService frameServiceService;
    private final FrameServiceConverter frameServiceConverter;
    private final FrameServiceRoleService frameServiceRoleService;

    /**
     * 获取指定集群的框架服务列表
     */
    @ApiOperation("获取指定集群的框架服务列表")
    @GetMapping("/list")
    public Result<List<FrameServiceVO>> list(
            @ApiParam(value = "集群ID", required = true) @RequestParam("clusterId") Integer clusterId) {
        List<FrameServiceDTO> frameServiceDTOs = frameServiceService.getAllFrameService(clusterId);
        List<FrameServiceVO> frameServiceVOs = frameServiceConverter.dtoListToVoList(frameServiceDTOs);
        return Result.success(frameServiceVOs);
    }

    /**
     * 获取包含必选组件标识的框架服务列表
     */
    @ApiOperation("获取包含必选组件标识的框架服务列表")
    @GetMapping("/listWithRequired")
    public Result<List<FrameServiceVO>> listWithRequired(
            @ApiParam(value = "集群ID", required = true) @RequestParam("clusterId") Integer clusterId,
            @ApiParam(value = "集群类型", required = true) @RequestParam("type") String type) {
        List<FrameServiceDTO> frameServiceDTOs = frameServiceService.getAllFrameServiceWithRequired(clusterId, type);
        List<FrameServiceVO> frameServiceVOs = frameServiceConverter.dtoListToVoList(frameServiceDTOs);
        return Result.success(frameServiceVOs);
    }

    /**
     * 根据服务ID列表查询服务
     */
    @ApiOperation("根据服务ID列表查询服务")
    @GetMapping("/getServiceListByServiceIds")
    public Result<List<FrameServiceVO>> getServiceListByServiceIds(
            @ApiParam(value = "服务ID列表", required = true) @RequestParam("serviceIds") List<Integer> serviceIds) {
        List<FrameServiceDTO> frameServiceDTOs = frameServiceService.getServiceListByServiceIds(serviceIds);
        List<FrameServiceVO> frameServiceVOs = frameServiceConverter.dtoListToVoList(frameServiceDTOs);
        return Result.success(frameServiceVOs);
    }

    /**
     * 根据ID获取服务信息
     */
    @ApiOperation("根据ID获取服务信息")
    @GetMapping("/info/{id}")
    public Result<FrameServiceVO> info(@ApiParam(value = "服务ID", required = true) @PathVariable("id") Integer id) {
        FrameServiceDTO frameServiceDTO = frameServiceService.getFrameServiceById(id);
        FrameServiceVO frameServiceVO = frameServiceConverter.dtoToVo(frameServiceDTO);
        return Result.success(frameServiceVO);
    }

    /**
     * 保存服务信息
     */
    @ApiOperation("保存服务信息")
    @PostMapping("/save")
    public Result<FrameServiceVO> save(
            @ApiParam(value = "服务信息", required = true) @RequestBody FrameServiceDTO frameServiceDTO) {
        FrameServiceDTO savedDTO = frameServiceService.saveFrameService(frameServiceDTO);
        FrameServiceVO frameServiceVO = frameServiceConverter.dtoToVo(savedDTO);
        return Result.success(frameServiceVO);
    }

    /**
     * 更新服务信息
     */
    @ApiOperation("更新服务信息")
    @PutMapping("/update")
    public Result<FrameServiceVO> update(
            @ApiParam(value = "服务信息", required = true) @RequestBody FrameServiceDTO frameServiceDTO) {
        FrameServiceDTO updatedDTO = frameServiceService.updateFrameService(frameServiceDTO);
        FrameServiceVO frameServiceVO = frameServiceConverter.dtoToVo(updatedDTO);
        return Result.success(frameServiceVO);
    }

    /**
     * 删除服务组件（包含文件清理和依赖检查）
     */
    @ApiOperation("删除服务组件")
    @DeleteMapping("/delete/{id}")
    public Result<Boolean> delete(@ApiParam(value = "服务ID", required = true) @PathVariable("id") Integer id) {
        try {
            // 获取服务信息
            FrameServiceDTO serviceDTO = frameServiceService.getFrameServiceById(id);
            if (serviceDTO == null) {
                return Result.error("Service 组件不存在。");
            }

            // 检查是否有集群正在使用此服务
            List<ClusterServiceInstanceEntity> serviceInstances = QueryChain.of(ClusterServiceInstanceEntity.class)
                    .where(ClusterServiceInstanceEntity::getFrameServiceId).eq(id)
                    .list();

            if (serviceInstances != null && !serviceInstances.isEmpty()) {
                return Result.error("Service 组件正在使用中，无法删除。");
            }

            // 删除软件包文件
            if (serviceDTO.packageName() != null) {
                File targetPackageFile = new File(Constants.MASTER_MANAGE_PACKAGE_PATH, serviceDTO.packageName());
                if (targetPackageFile.exists()) {
                    FileUtil.del(targetPackageFile);
                    log.info("已删除软件包文件: {}", targetPackageFile.getAbsolutePath());
                }

                // 删除MD5文件
                File targetPackageFileMd5 = new File(Constants.MASTER_MANAGE_PACKAGE_PATH,
                        serviceDTO.packageName() + ".md5");
                if (targetPackageFileMd5.exists()) {
                    FileUtil.del(targetPackageFileMd5);
                    log.info("已删除软件包MD5文件: {}", targetPackageFileMd5.getAbsolutePath());
                }
            }

            // 删除相关配置
            boolean configDeleted = frameServiceRoleService.remove(QueryChain.of(FrameServiceRoleEntity.class)
                    .where(FrameServiceRoleEntity::getServiceId).eq(id));
            log.info("删除服务配置结果: {}", configDeleted);

            // 删除主服务
            boolean serviceDeleted = frameServiceService.removeFrameServiceById(id);
            if (!serviceDeleted) {
                return Result.error("删除服务失败。");
            }

            log.info("成功删除服务组件，ID: {}", id);
            return Result.success(true);

        } catch (Exception e) {
            log.error("删除服务组件失败，ID: {}", id, e);
            return Result.error("删除服务组件失败: " + e.getMessage());
        }
    }

}
