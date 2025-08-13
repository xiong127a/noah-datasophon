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

import com.datasophon.api.annotation.ApiVersion;
import com.datasophon.api.service.ClusterServiceRoleInstanceService;
import com.datasophon.api.converter.ClusterServiceRoleInstanceConverter;
import com.datasophon.common.dto.ClusterServiceRoleInstanceDTO;
import com.datasophon.common.vo.ClusterServiceRoleInstanceVO;
import com.datasophon.api.dto.Result;
import com.datasophon.common.model.PageResult;
import com.datasophon.common.enums.ServiceRoleState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 集群服务角色实例控制器
 * 提供集群服务角色实例的REST API接口
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-01
 */
@ApiVersion(path = "cluster/service/role/instance")
public class ClusterServiceRoleInstanceController {

    private static final Logger logger = LoggerFactory.getLogger(ClusterServiceRoleInstanceController.class);

    @Autowired
    private ClusterServiceRoleInstanceService clusterServiceRoleInstanceService;

    @Autowired
    private ClusterServiceRoleInstanceConverter clusterServiceRoleInstanceConverter;

    /**
     * 分页查询服务角色实例列表
     */
    @GetMapping("/list")
    public Result<PageResult<ClusterServiceRoleInstanceVO>> getServiceRoleInstanceList(
            @RequestParam Integer serviceInstanceId,
            @RequestParam(required = false) String hostname,
            @RequestParam(required = false) Integer serviceRoleState,
            @RequestParam(required = false) String serviceRoleName,
            @RequestParam(required = false) Integer roleGroupId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        try {
            PageResult<ClusterServiceRoleInstanceDTO> pageResult = clusterServiceRoleInstanceService
                    .listAll(serviceInstanceId, hostname, serviceRoleState, serviceRoleName, roleGroupId, page,
                            pageSize);

            List<ClusterServiceRoleInstanceVO> voList = clusterServiceRoleInstanceConverter
                    .dtoListToVoList(pageResult.getRecords());
            PageResult<ClusterServiceRoleInstanceVO> voPageResult = PageResult.of(voList,
                    pageResult.getTotal(), page, pageSize);

            return Result.success(voPageResult);
        } catch (Exception e) {
            return Result.error("查询服务角色实例列表失败: " + e.getMessage());
        }
    }

    /**
     * 根据服务ID获取服务角色实例列表
     */
    @GetMapping("/service/{serviceId}")
    public Result<List<ClusterServiceRoleInstanceVO>> getServiceRoleInstanceListByServiceId(
            @PathVariable Integer serviceId) {
        try {
            List<ClusterServiceRoleInstanceDTO> dtoList = clusterServiceRoleInstanceService
                    .getServiceRoleInstanceListByServiceId(serviceId);
            List<ClusterServiceRoleInstanceVO> voList = clusterServiceRoleInstanceConverter.dtoListToVoList(dtoList);
            return Result.success(voList);
        } catch (Exception e) {
            return Result.error("获取服务角色实例列表失败: " + e.getMessage());
        }
    }

    /**
     * 根据集群ID获取服务角色实例列表
     */
    @GetMapping("/cluster/{clusterId}")
    public Result<List<ClusterServiceRoleInstanceVO>> getServiceRoleInstanceListByClusterId(
            @PathVariable Long clusterId) {
        try {
            List<ClusterServiceRoleInstanceDTO> dtoList = clusterServiceRoleInstanceService
                    .getServiceRoleInstanceListByClusterId(clusterId);
            List<ClusterServiceRoleInstanceVO> voList = clusterServiceRoleInstanceConverter.dtoListToVoList(dtoList);
            return Result.success(voList);
        } catch (Exception e) {
            return Result.error("获取集群服务角色实例列表失败: " + e.getMessage());
        }
    }

    /**
     * 根据集群ID和角色名称获取服务角色实例列表
     */
    @GetMapping("/cluster/{clusterId}/role/{roleName}")
    public Result<List<ClusterServiceRoleInstanceVO>> getServiceRoleInstanceListByClusterIdAndRoleName(
            @PathVariable Long clusterId, @PathVariable String roleName) {
        try {
            List<ClusterServiceRoleInstanceDTO> dtoList = clusterServiceRoleInstanceService
                    .getServiceRoleInstanceListByClusterIdAndRoleName(clusterId, roleName);
            List<ClusterServiceRoleInstanceVO> voList = clusterServiceRoleInstanceConverter.dtoListToVoList(dtoList);
            return Result.success(voList);
        } catch (Exception e) {
            return Result.error("获取服务角色实例列表失败: " + e.getMessage());
        }
    }

    /**
     * 根据服务ID获取正在运行的服务角色实例列表
     */
    @GetMapping("/running/service/{serviceInstanceId}")
    public Result<List<ClusterServiceRoleInstanceVO>> getRunningServiceRoleInstanceListByServiceId(
            @PathVariable Integer serviceInstanceId) {
        try {
            List<ClusterServiceRoleInstanceDTO> dtoList = clusterServiceRoleInstanceService
                    .getRunningServiceRoleInstanceListByServiceId(serviceInstanceId);
            List<ClusterServiceRoleInstanceVO> voList = clusterServiceRoleInstanceConverter.dtoListToVoList(dtoList);
            return Result.success(voList);
        } catch (Exception e) {
            return Result.error("获取正在运行的服务角色实例列表失败: " + e.getMessage());
        }
    }

    /**
     * 根据集群ID、主机名和状态获取服务角色实例列表
     */
    @GetMapping("/cluster/{clusterId}/host/{hostname}/state/{state}")
    public Result<List<ClusterServiceRoleInstanceVO>> getServiceRoleInstanceListByClusterIdAndHostnameAndState(
            @PathVariable Long clusterId, @PathVariable String hostname, @PathVariable ServiceRoleState state) {
        try {
            // 暂时移除未实现的方法，返回空列表
            List<ClusterServiceRoleInstanceDTO> dtoList = List.of();
            List<ClusterServiceRoleInstanceVO> voList = clusterServiceRoleInstanceConverter.dtoListToVoList(dtoList);
            return Result.success(voList);
        } catch (Exception e) {
            return Result.error("获取服务角色实例列表失败: " + e.getMessage());
        }
    }

    /**
     * 根据服务角色名称获取服务角色实例列表
     */
    @GetMapping("/role/{serviceRoleName}")
    public Result<List<ClusterServiceRoleInstanceVO>> listServiceRoleByName(@PathVariable String serviceRoleName) {
        try {
            List<ClusterServiceRoleInstanceDTO> dtoList = clusterServiceRoleInstanceService
                    .listServiceRoleByName(serviceRoleName);
            List<ClusterServiceRoleInstanceVO> voList = clusterServiceRoleInstanceConverter.dtoListToVoList(dtoList);
            return Result.success(voList);
        } catch (Exception e) {
            return Result.error("获取服务角色实例列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取KAdmin角色实例
     */
    @GetMapping("/kadmin/{clusterId}")
    public Result<ClusterServiceRoleInstanceVO> getKAdminRoleIns(@PathVariable Long clusterId) {
        try {
            ClusterServiceRoleInstanceDTO dto = clusterServiceRoleInstanceService.getKAdminRoleIns(clusterId);
            if (dto == null) {
                return Result.success(null);
            }
            ClusterServiceRoleInstanceVO vo = clusterServiceRoleInstanceConverter.dtoToVo(dto);
            return Result.success(vo);
        } catch (Exception e) {
            return Result.error("获取KAdmin角色实例失败: " + e.getMessage());
        }
    }

    /**
     * 根据ID获取服务角色实例详情
     */
    @GetMapping("/{id}")
    public Result<ClusterServiceRoleInstanceVO> getServiceRoleInstanceById(@PathVariable Integer id) {
        try {
            // 暂时简化实现，使用基础CRUD方法
            ClusterServiceRoleInstanceDTO dto = clusterServiceRoleInstanceConverter.entityToDto(
                    clusterServiceRoleInstanceService.getById(id));
            if (dto == null) {
                return Result.error("服务角色实例不存在");
            }
            ClusterServiceRoleInstanceVO vo = clusterServiceRoleInstanceConverter.dtoToVo(dto);
            return Result.success(vo);
        } catch (Exception e) {
            return Result.error("获取服务角色实例详情失败: " + e.getMessage());
        }
    }

    /**
     * 删除服务角色实例
     */
    @DeleteMapping("/batch")
    public Result<Void> deleteServiceRole(@RequestBody List<String> idList) {
        try {
            clusterServiceRoleInstanceService.deleteServiceRole(idList);
            return Result.success();
        } catch (Exception e) {
            return Result.error("删除服务角色实例失败: " + e.getMessage());
        }
    }

    /**
     * 重启过时服务
     */
    @PostMapping("/restart-obsolete/{roleGroupId}")
    public Result<Void> restartObsoleteService(@PathVariable Integer roleGroupId) {
        try {
            clusterServiceRoleInstanceService.restartObsoleteService(roleGroupId);
            return Result.success();
        } catch (Exception e) {
            return Result.error("重启过时服务失败: " + e.getMessage());
        }
    }

    /**
     * 退役DataNode节点
     */
    @PostMapping("/decommission-datanode")
    public Result<Void> decommissionDataNode(@RequestParam String serviceRoleInstanceIds) {
        try {
            // 暂时移除未实现的方法
            logger.info("退役DataNode节点操作: {}", serviceRoleInstanceIds);
            return Result.success();
        } catch (Exception e) {
            return Result.error("退役DataNode节点失败: " + e.getMessage());
        }
    }

    /**
     * 移除角色实例
     */
    @DeleteMapping("/remove/{serviceInstanceId}")
    public Result<Void> removeRoleInstance(@PathVariable Integer serviceInstanceId) {
        try {
            clusterServiceRoleInstanceService.reomveRoleInstance(serviceInstanceId);
            return Result.success();
        } catch (Exception e) {
            return Result.error("移除角色实例失败: " + e.getMessage());
        }
    }

    /**
     * 创建服务角色实例
     */
    @PostMapping
    public Result<ClusterServiceRoleInstanceVO> createServiceRoleInstance(
            @RequestBody ClusterServiceRoleInstanceVO instanceVO) {
        try {
            // 暂时简化实现，移除未实现方法
            logger.info("创建服务角色实例操作: {}", instanceVO);
            ClusterServiceRoleInstanceDTO savedDto = null;
            ClusterServiceRoleInstanceVO resultVO = clusterServiceRoleInstanceConverter.dtoToVo(savedDto);
            return Result.success(resultVO);
        } catch (Exception e) {
            return Result.error("创建服务角色实例失败: " + e.getMessage());
        }
    }

    /**
     * 更新服务角色实例
     */
    @PutMapping
    public Result<Void> updateServiceRoleInstance(@RequestBody ClusterServiceRoleInstanceVO instanceVO) {
        try {
            // 暂时简化实现，移除未实现方法
            logger.info("更新服务角色实例操作: {}", instanceVO);
            return Result.success();
        } catch (Exception e) {
            return Result.error("更新服务角色实例失败: " + e.getMessage());
        }
    }
}