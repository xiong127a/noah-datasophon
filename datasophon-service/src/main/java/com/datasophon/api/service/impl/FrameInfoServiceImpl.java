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

import com.datasophon.api.service.FrameInfoService;
import com.datasophon.common.utils.CollectionUtils;

import com.datasophon.dao.entity.FrameInfoEntity;
import com.datasophon.dao.entity.FrameServiceEntity;
import com.datasophon.dao.mapper.FrameInfoMapper;
import com.datasophon.dao.mapper.FrameServiceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 集群框架表实现
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2024-12-19
 */
@Service("frameInfoService")
public class FrameInfoServiceImpl implements FrameInfoService {

    @Autowired
    private FrameInfoMapper frameInfoMapper;

    @Autowired
    private FrameServiceMapper frameServiceMapper;

    @Override
    public List<FrameInfoEntity> getAllClusterFrame() {
        List<FrameInfoEntity> frameInfoEntities = frameInfoMapper.selectAll();
        if (CollectionUtils.isEmpty(frameInfoEntities)) {
            return java.util.Collections.emptyList();
        }

        java.util.Set<Integer> frameInfoIds = frameInfoEntities.stream()
                .map(FrameInfoEntity::getId)
                .collect(java.util.stream.Collectors.toSet());

        Map<Integer, List<FrameServiceEntity>> frameServiceGroupBys = frameServiceMapper.selectByFrameIds(frameInfoIds)
                .stream()
                .collect(java.util.stream.Collectors.groupingBy(FrameServiceEntity::getFrameId));

        frameInfoEntities.forEach(f -> f.setFrameServiceList(frameServiceGroupBys.get(f.getId())));

        return frameInfoEntities;
    }

    // 标准CRUD方法实现
    @Override
    public FrameInfoEntity getById(Integer id) {
        return frameInfoMapper.selectById(id);
    }

    @Override
    public FrameInfoEntity save(FrameInfoEntity entity) {
        frameInfoMapper.insert(entity);
        return entity;
    }

    @Override
    public FrameInfoEntity updateById(FrameInfoEntity entity) {
        frameInfoMapper.updateById(entity);
        return entity;
    }

    @Override
    public boolean removeByIds(List<Integer> ids) {
        return frameInfoMapper.deleteByIds(ids) > 0;
    }

    @Override
    public List<FrameInfoEntity> getAllFrameInfos() {
        return frameInfoMapper.selectAll();
    }
}
