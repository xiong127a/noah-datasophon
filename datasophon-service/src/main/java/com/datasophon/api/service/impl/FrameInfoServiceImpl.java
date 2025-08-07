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

package com.datasophon.api.service.impl;

import cn.hutool.core.util.StrUtil;
import com.datasophon.api.converter.FrameInfoConverter;
import com.datasophon.api.service.FrameInfoService;
import com.datasophon.common.dto.FrameInfoDTO;
import com.datasophon.common.exception.BusinessException;
import com.datasophon.dao.entity.FrameInfoEntity;
import com.datasophon.dao.entity.FrameServiceEntity;
import com.datasophon.dao.mapper.FrameInfoMapper;
import com.datasophon.dao.mapper.FrameServiceMapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.Set;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 集群框架表服务实现
 * 继承ServiceImpl提供基础CRUD操作，使用Converter进行对象转换
 * 按照架构重构规范，返回DTO对象，抛出业务异常
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
@Slf4j
@Service("frameInfoService")
@RequiredArgsConstructor
public class FrameInfoServiceImpl extends ServiceImpl<FrameInfoMapper, FrameInfoEntity> implements FrameInfoService {

    private final FrameInfoConverter frameInfoConverter;
    private final FrameServiceMapper frameServiceMapper;

    @Override
    public List<FrameInfoDTO> getAllClusterFrame() {
        // 调用DAO层方法查询所有框架信息
        List<FrameInfoEntity> frameInfoEntities = getMapper().selectAllFrameInfo();

        if (frameInfoEntities.isEmpty()) {
            return List.of();
        }

        // 获取框架ID集合，使用JDK21特性，转换为Set类型
        Set<Integer> frameInfoIds = frameInfoEntities.stream()
                .map(FrameInfoEntity::getId)
                .collect(Collectors.toSet());

        // 查询关联的服务信息
        Map<Integer, List<FrameServiceEntity>> frameServiceGroupBys = frameServiceMapper
                .selectByFrameIds(frameInfoIds)
                .stream()
                .collect(java.util.stream.Collectors.groupingBy(FrameServiceEntity::getFrameId));

        // 设置服务列表并转换为DTO
        return frameInfoEntities.stream()
                .map(frame -> {
                    frame.setFrameServiceList(frameServiceGroupBys.get(frame.getId()));
                    return frameInfoConverter.entityToDto(frame);
                })
                .sorted(Comparator.comparing(FrameInfoDTO::frameCode))
                .toList();
    }

    @Override
    public FrameInfoDTO getFrameInfoByFrameCode(String frameCode) {
        if (StrUtil.isBlank(frameCode)) {
            throw new BusinessException("框架代码不能为空");
        }

        FrameInfoEntity entity = getMapper().getFrameInfoByFrameCode(frameCode);
        if (entity == null) {
            throw new BusinessException("未找到框架代码为 " + frameCode + " 的框架信息");
        }

        return frameInfoConverter.entityToDto(entity);
    }

    @Override
    public FrameInfoDTO saveFrameInfo(FrameInfoDTO frameInfoDTO) {
        if (frameInfoDTO == null) {
            throw new BusinessException("框架信息不能为空");
        }

        FrameInfoEntity entity = frameInfoConverter.dtoToEntity(frameInfoDTO);
        boolean result = save(entity);

        if (!result) {
            throw new BusinessException("保存框架信息失败");
        }

        return frameInfoConverter.entityToDto(entity);
    }

    @Override
    public FrameInfoDTO updateFrameInfo(FrameInfoDTO frameInfoDTO) {
        if (frameInfoDTO == null || frameInfoDTO.id() == null) {
            throw new BusinessException("框架信息或ID不能为空");
        }

        // 检查记录是否存在
        FrameInfoEntity existingEntity = getById(frameInfoDTO.id());
        if (existingEntity == null) {
            throw new BusinessException("未找到ID为 " + frameInfoDTO.id() + " 的框架信息");
        }

        FrameInfoEntity entity = frameInfoConverter.dtoToEntity(frameInfoDTO);
        boolean result = updateById(entity);

        if (!result) {
            throw new BusinessException("更新框架信息失败");
        }

        return frameInfoConverter.entityToDto(entity);
    }

    @Override
    public FrameInfoDTO getFrameInfoById(Integer id) {
        if (id == null) {
            throw new BusinessException("框架ID不能为空");
        }

        FrameInfoEntity entity = getById(id);
        if (entity == null) {
            throw new BusinessException("未找到ID为 " + id + " 的框架信息");
        }

        return frameInfoConverter.entityToDto(entity);
    }

    @Override
    public boolean removeFrameInfoByIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("删除的ID列表不能为空");
        }

        return removeByIds(ids);
    }

    @Override
    public List<FrameInfoDTO> getAllFrameInfos() {
        List<FrameInfoEntity> entities = getMapper().selectAllFrameInfo();

        return frameInfoConverter.entityListToDtoList(entities);
    }
}
