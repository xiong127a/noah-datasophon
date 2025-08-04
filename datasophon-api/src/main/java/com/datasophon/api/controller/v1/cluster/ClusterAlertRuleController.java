/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.datasophon.api.controller.v1.cluster;

import com.datasophon.api.converter.ClusterAlertRuleConverter;
import com.datasophon.api.service.ClusterAlertRuleService;
import com.datasophon.common.dto.ClusterAlertRuleDTO;
import com.datasophon.common.vo.ClusterAlertRuleVO;
import com.datasophon.api.dto.Result;
import org.springframework.beans.factory.annotation.Autowired;
import com.datasophon.api.annotation.ApiVersion;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

/**
 * 集群告警规则控制器
 * 提供集群告警规则的REST API接口
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
@ApiVersion(path = "cluster/alert/rule")
public class ClusterAlertRuleController {

    @Autowired
    private ClusterAlertRuleService clusterAlertRuleService;

    @Autowired
    private ClusterAlertRuleConverter clusterAlertRuleConverter;

    /**
     * 列表
     */
    @RequestMapping("/list")
    public Result<List<ClusterAlertRuleVO>> list() {
        // 这里需要根据实际业务需求实现列表查询逻辑
        return Result.success();
    }

    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public Result<ClusterAlertRuleVO> info(@PathVariable("id") Long id) {
        // 调用Service层方法，获取DTO
        ClusterAlertRuleDTO dto = clusterAlertRuleService.getByIdAsDto(id);
        // Controller层：DTO → VO转换
        ClusterAlertRuleVO vo = clusterAlertRuleConverter.dtoToVo(dto);
        return Result.success(vo);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public Result<String> save(@RequestBody ClusterAlertRuleDTO dto) {
        // Controller层直接传递DTO给Service层
        clusterAlertRuleService.saveAlertRule(dto);
        return Result.success("保存成功");
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public Result<String> update(@RequestBody ClusterAlertRuleDTO dto) {
        // Controller层直接传递DTO给Service层
        clusterAlertRuleService.updateAlertRule(dto);
        return Result.success("更新成功");
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public Result<String> delete(@RequestBody Long[] ids) {
        clusterAlertRuleService.removeByIds(List.of(ids));
        return Result.success("删除成功");
    }
}