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

import com.datasophon.api.converter.ClusterServiceRoleInstanceConverter;
import com.datasophon.api.service.ClusterServiceRoleInstanceService;
import com.datasophon.common.dto.ClusterServiceRoleInstanceDTO;
import com.datasophon.common.vo.ClusterServiceRoleInstanceVO;
import com.datasophon.common.model.PageResult;
import com.datasophon.api.dto.Result;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;
import org.springframework.beans.factory.annotation.Autowired;
import com.datasophon.api.annotation.ApiVersion;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 集群服务角色实例控制器
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-01
 */
@ApiVersion(path = "cluster/service/role/instance")
public class ClusterServiceRoleInstanceController {

    @Autowired
    private ClusterServiceRoleInstanceService clusterServiceRoleInstanceService;

    @Autowired
    private ClusterServiceRoleInstanceConverter clusterServiceRoleInstanceConverter;

    /**
     * 列表
     */
    @RequestMapping("/list")
    public Result<PageResult<ClusterServiceRoleInstanceVO>> list(
            @RequestParam("serviceInstanceId") Integer serviceInstanceId,
            @RequestParam(name = "hostname", required = false) String hostname,
            @RequestParam(name = "serviceRoleState", required = false) Integer serviceRoleState,
            @RequestParam("serviceRoleName") String serviceRoleName,
            @RequestParam(name = "roleGroupId", required = false) Integer roleGroupId,
            @RequestParam("page") Integer page, @RequestParam("pageSize") Integer pageSize) {
        try {
            PageResult<ClusterServiceRoleInstanceDTO> dtoPageResult = clusterServiceRoleInstanceService.listAll(
                    serviceInstanceId, hostname, serviceRoleState, serviceRoleName,
                    roleGroupId, page, pageSize);

            // DTO → VO 转换
            List<ClusterServiceRoleInstanceVO> voList = clusterServiceRoleInstanceConverter
                    .dtoListToVoList(dtoPageResult.getRecords());
            PageResult<ClusterServiceRoleInstanceVO> voPageResult = PageResult.of(voList, dtoPageResult.getTotal(),
                    dtoPageResult.getCurrent(), dtoPageResult.getSize());

            return Result.success(voPageResult);
        } catch (Exception e) {
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 信息
     */
    @RequestMapping("/getLog")
    public Result<String> getLog(@RequestParam("serviceRoleInstanceId") Integer serviceRoleInstanceId) {
        try {
            String logContent = clusterServiceRoleInstanceService.getLog(serviceRoleInstanceId);
            return Result.success(logContent);
        } catch (Exception e) {
            return Result.error("获取日志失败: " + e.getMessage());
        }
    }

    /**
     * 退役
     */
    @RequestMapping("/decommissionNode")
    public Result<String> decommissionNode(@RequestParam("serviceRoleInstanceIds") String serviceRoleInstanceIds,
            @RequestParam("serviceName") String serviceName) {
        try {
            String result = clusterServiceRoleInstanceService.decommissionNode(serviceRoleInstanceIds, serviceName);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("节点退役失败: " + e.getMessage());
        }
    }

    /**
     * 重启过时服务
     */
    @RequestMapping("/restartObsoleteService")
    public Result<String> restartObsoleteService(@RequestParam("roleGroupId") Integer roleGroupId) {
        try {
            clusterServiceRoleInstanceService.restartObsoleteService(roleGroupId);
            return Result.success("重启服务任务已提交");
        } catch (Exception e) {
            return Result.error("重启服务失败: " + e.getMessage());
        }
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public Result<String> save(@RequestBody ClusterServiceRoleInstanceDTO clusterServiceRoleInstanceDTO) {
        ClusterServiceRoleInstanceEntity clusterServiceRoleInstance = clusterServiceRoleInstanceConverter
                .dtoToEntity(clusterServiceRoleInstanceDTO);
        clusterServiceRoleInstanceService.save(clusterServiceRoleInstance);
        return Result.success("保存成功");
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public Result<String> update(@RequestBody ClusterServiceRoleInstanceDTO clusterServiceRoleInstanceDTO) {
        ClusterServiceRoleInstanceEntity clusterServiceRoleInstance = clusterServiceRoleInstanceConverter
                .dtoToEntity(clusterServiceRoleInstanceDTO);
        clusterServiceRoleInstanceService.updateById(clusterServiceRoleInstance);
        return Result.success("更新成功");
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public Result<String> delete(@RequestParam("serviceRoleInstancesIds") String serviceRoleInstancesIds) {
        try {
            List<String> idList = List.of(serviceRoleInstancesIds.split(","));
            clusterServiceRoleInstanceService.deleteServiceRole(idList);
            return Result.success("删除成功");
        } catch (Exception e) {
            return Result.error("删除失败: " + e.getMessage());
        }
    }

}
