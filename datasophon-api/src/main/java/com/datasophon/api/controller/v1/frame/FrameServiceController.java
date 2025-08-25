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

package com.datasophon.api.controller.v1.frame;

import cn.hutool.core.io.FileUtil;
import com.datasophon.api.converter.FrameServiceConverter;
import com.datasophon.api.service.FrameServiceRoleService;
import com.datasophon.api.service.FrameServiceService;
import com.datasophon.common.Constants;
import com.datasophon.common.dto.FrameServiceDTO;
import com.datasophon.common.vo.FrameServiceVO;
import com.datasophon.api.dto.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.datasophon.api.annotation.ApiVersion;
import com.datasophon.common.enums.ServiceType;
import com.datasophon.api.annotation.ClusterId;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
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
@ApiVersion(path = "frame/service")
@RequiredArgsConstructor
public class FrameServiceController {

    private final FrameServiceService frameServiceService;
    private final FrameServiceConverter frameServiceConverter;
    private final FrameServiceRoleService frameServiceRoleService;

    /**
     * 获取指定集群的框架服务列表
     * 集群ID从请求头获取，保持API设计一致性
     */
    @GetMapping("/list")
    public Result<List<FrameServiceVO>> list(@ClusterId Long clusterId) {
        List<FrameServiceDTO> frameServiceDTOs = frameServiceService.getAllFrameService(clusterId);
        List<FrameServiceVO> frameServiceVOs = frameServiceConverter.dtoListToVoList(frameServiceDTOs);
        return Result.success(frameServiceVOs);
    }

    /**
     * 获取包含必选组件标识的框架服务列表
     * 集群ID从请求头获取，后端自动根据集群ID查询集群类型
     */
    @GetMapping("/listWithRequired")
    public Result<List<FrameServiceVO>> listWithRequired(
            @ClusterId Long clusterId,
            @RequestParam("type") ServiceType type) {
        List<FrameServiceDTO> frameServiceDTOs = frameServiceService.getAllFrameServiceWithRequired(clusterId, type);
        List<FrameServiceVO> frameServiceVOs = frameServiceConverter.dtoListToVoList(frameServiceDTOs);
        return Result.success(frameServiceVOs);
    }

    /**
     * 根据服务ID列表查询服务
     */

    @GetMapping("/getServiceListByServiceIds")
    public Result<List<FrameServiceVO>> getServiceListByServiceIds(
            @RequestParam("serviceIds") List<Long> serviceIds) {
        List<FrameServiceDTO> frameServiceDTOs = frameServiceService.getServiceListByServiceIds(serviceIds);
        List<FrameServiceVO> frameServiceVOs = frameServiceConverter.dtoListToVoList(frameServiceDTOs);
        return Result.success(frameServiceVOs);
    }

    /**
     * 根据ID获取服务信息
     */

    @GetMapping("/info/{id}")
    public Result<FrameServiceVO> info(@PathVariable("id") Long id) {
        FrameServiceDTO frameServiceDTO = frameServiceService.getFrameServiceById(id);
        FrameServiceVO frameServiceVO = frameServiceConverter.dtoToVo(frameServiceDTO);
        return Result.success(frameServiceVO);
    }

    /**
     * 保存服务信息
     */

    @PostMapping("/save")
    public Result<FrameServiceVO> save(@RequestBody FrameServiceDTO frameServiceDTO) {
        FrameServiceDTO savedDTO = frameServiceService.saveFrameService(frameServiceDTO);
        FrameServiceVO frameServiceVO = frameServiceConverter.dtoToVo(savedDTO);
        return Result.success(frameServiceVO);
    }

    /**
     * 更新服务信息
     */

    @PutMapping("/update")
    public Result<FrameServiceVO> update(@RequestBody FrameServiceDTO frameServiceDTO) {
        FrameServiceDTO updatedDTO = frameServiceService.updateFrameService(frameServiceDTO);
        FrameServiceVO frameServiceVO = frameServiceConverter.dtoToVo(updatedDTO);
        return Result.success(frameServiceVO);
    }

    /**
     * 删除服务组件（包含文件清理和依赖检查）
     */

    @DeleteMapping("/delete/{id}")
    public Result<Boolean> delete(@PathVariable("id") Long id) {
        try {
            // 获取服务信息
            FrameServiceDTO serviceDTO = frameServiceService.getFrameServiceById(id);
            if (serviceDTO == null) {
                return Result.error("Service 组件不存在。");
            }

            // 检查是否有集群正在使用此服务
            if (frameServiceService.isServiceInUse(id)) {
                return Result.error("Service 组件正在使用中，无法删除。");
            }

            // 删除软件包文件
            if (serviceDTO.packageName() != null) {
                String packageName = serviceDTO.packageName();
                
                // 使用Java NIO进行安全的路径处理
                try {
                    Path basePath = Paths.get(Constants.MASTER_MANAGE_PACKAGE_PATH).toRealPath();
                    Path targetPath = basePath.resolve(packageName).normalize();
                    
                    // 验证路径是否在允许的目录范围内
                    if (!targetPath.startsWith(basePath)) {
                        log.warn("检测到路径遍历攻击尝试: {}", targetPath);
                        return Result.error("文件路径不在允许的目录范围内，删除操作已取消。");
                    }
                    
                    File targetPackageFile = targetPath.toFile();
                    if (targetPackageFile.exists()) {
                        FileUtil.del(targetPackageFile);
                        log.info("已删除软件包文件: {}", targetPackageFile.getAbsolutePath());
                    }

                    // 删除MD5文件，使用相同的安全验证
                    Path md5Path = basePath.resolve(packageName + ".md5").normalize();
                    if (!md5Path.startsWith(basePath)) {
                        log.warn("检测到MD5文件路径遍历攻击尝试: {}", md5Path);
                        return Result.error("MD5文件路径不在允许的目录范围内，删除操作已取消。");
                    }
                    
                    File targetPackageFileMd5 = md5Path.toFile();
                    if (targetPackageFileMd5.exists()) {
                        FileUtil.del(targetPackageFileMd5);
                        log.info("已删除软件包MD5文件: {}", targetPackageFileMd5.getAbsolutePath());
                    }
                    
                } catch (Exception e) {
                    log.error("处理文件路径时发生错误: {}", e.getMessage());
                    return Result.error("文件路径处理失败: " + e.getMessage());
                }
            }

            // 删除相关配置
            boolean configDeleted = frameServiceRoleService.removeByServiceId(id);
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
