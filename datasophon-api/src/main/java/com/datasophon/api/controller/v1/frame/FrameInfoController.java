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

import com.datasophon.api.converter.FrameInfoConverter;
import com.datasophon.api.service.FrameInfoService;
import com.datasophon.common.dto.FrameInfoDTO;
import com.datasophon.common.vo.FrameInfoVO;
import com.datasophon.api.dto.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.datasophon.api.annotation.ApiVersion;

import java.util.Arrays;
import java.util.List;

/**
 * 集群框架信息控制器
 * 按照三层架构规范，使用DTO接收请求，VO返回响应
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
@Slf4j
@ApiVersion(path = "frame")
@RequiredArgsConstructor
public class FrameInfoController {

    private final FrameInfoService frameInfoService;
    private final FrameInfoConverter frameInfoConverter;

    /**
     * 获取集群框架列表（包含服务信息）
     */

    @GetMapping("/list")
    public Result<List<FrameInfoVO>> list() {
        List<FrameInfoDTO> frameInfoDTOs = frameInfoService.getAllClusterFrame();
        List<FrameInfoVO> frameInfoVOs = frameInfoConverter.dtoListToVoList(frameInfoDTOs);
        return Result.success(frameInfoVOs);
    }

    /**
     * 根据ID获取框架信息
     */
    @GetMapping("/info/{id}")
    public Result<FrameInfoVO> info(@PathVariable("id") Long id) {
        FrameInfoDTO frameInfoDTO = frameInfoService.getFrameInfoById(id);
        FrameInfoVO frameInfoVO = frameInfoConverter.dtoToVo(frameInfoDTO);
        return Result.success(frameInfoVO);
    }

    /**
     * 保存框架信息
     */
    @PostMapping("/save")
    public Result<FrameInfoVO> save(@RequestBody FrameInfoDTO frameInfoDTO) {
        FrameInfoDTO savedDTO = frameInfoService.saveFrameInfo(frameInfoDTO);
        FrameInfoVO frameInfoVO = frameInfoConverter.dtoToVo(savedDTO);
        return Result.success(frameInfoVO);
    }

    /**
     * 更新框架信息
     */
    @PutMapping("/update")
    public Result<FrameInfoVO> update(@RequestBody FrameInfoDTO frameInfoDTO) {
        FrameInfoDTO updatedDTO = frameInfoService.updateFrameInfo(frameInfoDTO);
        FrameInfoVO frameInfoVO = frameInfoConverter.dtoToVo(updatedDTO);
        return Result.success(frameInfoVO);
    }

    /**
     * 批量删除框架信息
     */
    @DeleteMapping("/delete")
    public Result<Boolean> delete(@RequestBody Integer[] ids) {
        boolean result = frameInfoService.removeFrameInfoByIds(Arrays.asList(ids));
        return Result.success(result);
    }

    /**
     * 获取所有框架信息（不包含服务列表）
     */

    @GetMapping("/all")
    public Result<List<FrameInfoVO>> getAllFrameInfos() {
        List<FrameInfoDTO> frameInfoDTOs = frameInfoService.getAllFrameInfos();
        List<FrameInfoVO> frameInfoVOs = frameInfoConverter.dtoListToVoList(frameInfoDTOs);
        return Result.success(frameInfoVOs);
    }

    /**
     * 根据框架代码获取框架信息
     */
    @GetMapping("/code/{frameCode}")
    public Result<FrameInfoVO> getByFrameCode(@PathVariable("frameCode") String frameCode) {
        FrameInfoDTO frameInfoDTO = frameInfoService.getFrameInfoByFrameCode(frameCode);
        FrameInfoVO frameInfoVO = frameInfoConverter.dtoToVo(frameInfoDTO);
        return Result.success(frameInfoVO);
    }
}
