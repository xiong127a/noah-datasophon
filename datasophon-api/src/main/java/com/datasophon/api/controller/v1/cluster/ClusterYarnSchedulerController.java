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
import com.datasophon.api.converter.ClusterYarnSchedulerConverter;
import com.datasophon.api.service.ClusterYarnSchedulerService;
import com.datasophon.common.dto.ClusterYarnSchedulerDTO;
import com.datasophon.common.vo.ClusterYarnSchedulerVO;
import com.datasophon.api.dto.Result;
import org.springframework.beans.factory.annotation.Autowired;
import com.datasophon.api.annotation.ApiVersion;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 集群Yarn调度器控制器
 * 按照架构重构规范，使用Result<VO>返回，调用Converter转换
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-01
 */
@ApiVersion(path = "cluster/yarn/scheduler")
public class ClusterYarnSchedulerController {

    @Autowired
    private ClusterYarnSchedulerService clusterYarnSchedulerService;

    @Autowired
    private ClusterYarnSchedulerConverter clusterYarnSchedulerConverter;

    /**
     * 根据集群ID获取调度器信息
     */
    @RequestMapping("/get")
    public Result<ClusterYarnSchedulerVO> getScheduler(@ClusterId Integer clusterId) {
        try {
            ClusterYarnSchedulerDTO dto = clusterYarnSchedulerService.getScheduler(clusterId);
            if (dto != null) {
                ClusterYarnSchedulerVO vo = clusterYarnSchedulerConverter.dtoToVo(dto);
                return Result.success(vo);
            } else {
                return Result.error("调度器不存在");
            }
        } catch (Exception e) {
            return Result.error("获取Yarn调度器失败: " + e.getMessage());
        }
    }

    /**
     * 根据集群ID获取所有调度器列表
     */
    @RequestMapping("/list")
    public Result<List<ClusterYarnSchedulerVO>> list(@ClusterId Integer clusterId) {
        try {
            List<ClusterYarnSchedulerDTO> dtoList = clusterYarnSchedulerService.getSchedulersByClusterId(clusterId);
            List<ClusterYarnSchedulerVO> voList = clusterYarnSchedulerConverter.dtoListToVoList(dtoList);
            return Result.success(voList);
        } catch (Exception e) {
            return Result.error("获取Yarn调度器列表失败: " + e.getMessage());
        }
    }

    /**
     * 根据ID获取调度器信息
     */
    @RequestMapping("/info/{id}")
    public Result<ClusterYarnSchedulerVO> info(@PathVariable("id") Integer id) {
        try {
            ClusterYarnSchedulerDTO dto = clusterYarnSchedulerService.getByIdAsDto(id);
            if (dto != null) {
                ClusterYarnSchedulerVO vo = clusterYarnSchedulerConverter.dtoToVo(dto);
                return Result.success(vo);
            } else {
                return Result.error("调度器不存在");
            }
        } catch (Exception e) {
            return Result.error("获取Yarn调度器信息失败: " + e.getMessage());
        }
    }

    /**
     * 保存或更新调度器
     */
    @RequestMapping("/save")
    public Result<ClusterYarnSchedulerVO> save(@RequestBody ClusterYarnSchedulerDTO clusterYarnSchedulerDTO) {
        try {
            ClusterYarnSchedulerDTO savedDTO = clusterYarnSchedulerService
                    .saveOrUpdateScheduler(clusterYarnSchedulerDTO);
            ClusterYarnSchedulerVO savedVO = clusterYarnSchedulerConverter.dtoToVo(savedDTO);
            return Result.success(savedVO);
        } catch (Exception e) {
            return Result.error("保存Yarn调度器失败: " + e.getMessage());
        }
    }

    /**
     * 更新调度器
     */
    @RequestMapping("/update")
    public Result<ClusterYarnSchedulerVO> update(@RequestBody ClusterYarnSchedulerDTO clusterYarnSchedulerDTO) {
        try {
            ClusterYarnSchedulerDTO updatedDTO = clusterYarnSchedulerService
                    .saveOrUpdateScheduler(clusterYarnSchedulerDTO);
            ClusterYarnSchedulerVO updatedVO = clusterYarnSchedulerConverter.dtoToVo(updatedDTO);
            return Result.success(updatedVO);
        } catch (Exception e) {
            return Result.error("更新Yarn调度器失败: " + e.getMessage());
        }
    }

    /**
     * 删除调度器
     */
    @RequestMapping("/delete/{id}")
    public Result<String> delete(@PathVariable("id") Integer id) {
        try {
            boolean success = clusterYarnSchedulerService.deleteScheduler(id);
            if (success) {
                return Result.success("删除成功");
            } else {
                return Result.error("删除失败");
            }
        } catch (Exception e) {
            return Result.error("删除Yarn调度器失败: " + e.getMessage());
        }
    }

    /**
     * 创建默认调度器
     */
    @RequestMapping("/createDefault")
    public Result<ClusterYarnSchedulerVO> createDefault(@ClusterId Integer clusterId) {
        try {
            ClusterYarnSchedulerDTO createdDTO = clusterYarnSchedulerService.createDefaultYarnScheduler(clusterId);
            ClusterYarnSchedulerVO createdVO = clusterYarnSchedulerConverter.dtoToVo(createdDTO);
            return Result.success(createdVO);
        } catch (Exception e) {
            return Result.error("创建默认Yarn调度器失败: " + e.getMessage());
        }
    }
}