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
import com.datasophon.api.service.ClusterUserTenantService;
import com.datasophon.api.dto.Result;
import com.datasophon.dao.entity.ClusterUserTenantEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 集群用户租户控制器
 * 提供集群用户租户关系的REST API接口
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-01
 */
@ApiVersion(path = "cluster/user/tenant")
public class ClusterUserTenantController {

    @Autowired
    private ClusterUserTenantService clusterUserTenantService;

    /**
     * 为用户添加租户授权
     */
    @PostMapping("/add")
    public Result<Void> addUserToTenant(
            @RequestParam Long clusterId,
            @RequestParam Integer userId,
            @RequestParam String tenantIds) {
        try {
            clusterUserTenantService.addUserToTenant(clusterId, userId, tenantIds);
            return Result.success();
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            return Result.error("添加用户租户授权失败: " + e.getMessage());
        }
    }

    /**
     * 删除用户租户授权
     */
    @DeleteMapping("/delete")
    public Result<Void> deleteUser(
            @RequestParam Long clusterId,
            @RequestParam Integer userId,
            @RequestParam String tenantIds) {
        try {
            clusterUserTenantService.deleteUser(clusterId, userId, tenantIds);
            return Result.success();
        } catch (Exception e) {
            return Result.error("删除用户租户授权失败: " + e.getMessage());
        }
    }

    /**
     * 根据用户ID获取授权租户列表
     */
    @GetMapping("/list")
    public Result<List<ClusterUserTenantEntity>> getListByUserId(
            @RequestParam Long clusterId,
            @RequestParam Integer userId) {
        try {
            List<ClusterUserTenantEntity> userTenantList = clusterUserTenantService.getListByUserId(clusterId, userId);
            return Result.success(userTenantList);
        } catch (Exception e) {
            return Result.error("获取用户租户授权列表失败: " + e.getMessage());
        }
    }

    /**
     * 根据ID获取用户租户关系详情
     */
    @GetMapping("/{id}")
    public Result<ClusterUserTenantEntity> getUserTenantById(@PathVariable Integer id) {
        try {
            ClusterUserTenantEntity userTenant = clusterUserTenantService.getById(id);
            if (userTenant == null) {
                return Result.error("用户租户关系不存在");
            }
            return Result.success(userTenant);
        } catch (Exception e) {
            return Result.error("获取用户租户关系详情失败: " + e.getMessage());
        }
    }

    /**
     * 创建用户租户关系
     */
    @PostMapping
    public Result<ClusterUserTenantEntity> createUserTenant(@RequestBody ClusterUserTenantEntity userTenant) {
        try {
            boolean saved = clusterUserTenantService.save(userTenant);
            if (saved) {
                return Result.success(userTenant);
            } else {
                return Result.error("创建用户租户关系失败");
            }
        } catch (Exception e) {
            return Result.error("创建用户租户关系失败: " + e.getMessage());
        }
    }

    /**
     * 更新用户租户关系
     */
    @PutMapping("/{id}")
    public Result<ClusterUserTenantEntity> updateUserTenant(
            @PathVariable Integer id, @RequestBody ClusterUserTenantEntity userTenant) {
        try {
            userTenant.setId(id);
            boolean updated = clusterUserTenantService.updateById(userTenant);
            if (updated) {
                return Result.success(userTenant);
            } else {
                return Result.error("更新用户租户关系失败");
            }
        } catch (Exception e) {
            return Result.error("更新用户租户关系失败: " + e.getMessage());
        }
    }

    /**
     * 删除用户租户关系
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteUserTenant(@PathVariable Integer id) {
        try {
            boolean deleted = clusterUserTenantService.removeById(id);
            if (deleted) {
                return Result.success();
            } else {
                return Result.error("删除用户租户关系失败");
            }
        } catch (Exception e) {
            return Result.error("删除用户租户关系失败: " + e.getMessage());
        }
    }

    /**
     * 批量删除用户租户关系
     */
    @DeleteMapping("/batch")
    public Result<Void> deleteUserTenantBatch(@RequestBody List<Integer> ids) {
        try {
            boolean deleted = clusterUserTenantService.removeByIds(ids);
            if (deleted) {
                return Result.success();
            } else {
                return Result.error("批量删除用户租户关系失败");
            }
        } catch (Exception e) {
            return Result.error("批量删除用户租户关系失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有用户租户关系
     */
    @GetMapping("/all")
    public Result<List<ClusterUserTenantEntity>> getAllUserTenants() {
        try {
            List<ClusterUserTenantEntity> userTenantList = clusterUserTenantService.list();
            return Result.success(userTenantList);
        } catch (Exception e) {
            return Result.error("获取所有用户租户关系失败: " + e.getMessage());
        }
    }

    /**
     * 根据集群ID获取用户租户关系
     */
    @GetMapping("/cluster/{clusterId}")
    public Result<List<ClusterUserTenantEntity>> getUserTenantsByClusterId(@PathVariable Long clusterId) {
        try {
            // 暂时简化实现
            List<ClusterUserTenantEntity> userTenantList = clusterUserTenantService.list().stream()
                    .filter(ut -> ut.getClusterId().equals(clusterId))
                    .toList();
            return Result.success(userTenantList);
        } catch (Exception e) {
            return Result.error("获取集群用户租户关系失败: " + e.getMessage());
        }
    }

    /**
     * 根据租户ID获取用户租户关系
     */
    @GetMapping("/tenant/{tenantId}")
    public Result<List<ClusterUserTenantEntity>> getUserTenantsByTenantId(@PathVariable Integer tenantId) {
        try {
            // 暂时简化实现
            List<ClusterUserTenantEntity> userTenantList = clusterUserTenantService.list().stream()
                    .filter(ut -> ut.getTenantId().equals(tenantId))
                    .toList();
            return Result.success(userTenantList);
        } catch (Exception e) {
            return Result.error("获取租户用户关系失败: " + e.getMessage());
        }
    }
}