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

import com.datasophon.api.service.AlertGroupService;
import com.datasophon.api.utils.string.validator.LengthValidator;
import com.datasophon.api.utils.string.validator.NotEmptyValidator;
import com.datasophon.common.utils.CollectionUtils;
import com.datasophon.common.model.PageResult;
import com.datasophon.dao.entity.AlertGroupEntity;
import com.datasophon.dao.entity.ClusterAlertGroupMap;
import com.datasophon.dao.entity.ClusterAlertQuota;
import com.datasophon.dao.mapper.AlertGroupMapper;
import com.datasophon.dao.mapper.ClusterAlertGroupMapMapper;
import com.datasophon.dao.mapper.ClusterAlertQuotaMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 告警组表实现
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2024-12-19
 */
@Service("alertGroupService")
public class AlertGroupServiceImpl implements AlertGroupService {

    @Autowired
    private AlertGroupMapper alertGroupMapper;

    @Autowired
    private ClusterAlertGroupMapMapper clusterAlertGroupMapMapper;

    @Autowired
    private ClusterAlertQuotaMapper clusterAlertQuotaMapper;

    @Override
    public PageResult<AlertGroupEntity> getAlertGroupList(Integer clusterId, String alertGroupName, Integer page,
            Integer pageSize) {
        // 查询告警组映射关系
        List<ClusterAlertGroupMap> alertGroupMapList = clusterAlertGroupMapMapper.selectByClusterId(clusterId);

        if (CollectionUtils.isEmpty(alertGroupMapList)) {
            return PageResult.empty(page, pageSize);
        }

        // 提取告警组ID集合
        List<Integer> groupIds = alertGroupMapList.stream()
                .map(ClusterAlertGroupMap::getAlertGroupId)
                .collect(java.util.stream.Collectors.toList());

        // 查询告警组列表和总数
        PageResult<AlertGroupEntity> pageResult = alertGroupMapper.selectAlertGroupsByIdsWithName(
                groupIds, alertGroupName, page, pageSize);

        List<AlertGroupEntity> alertGroupList = pageResult.getRecords();

        if (CollectionUtils.isEmpty(alertGroupList)) {
            return PageResult.empty(page, pageSize);
        }

        // 获取告警组ID集合用于后续查询
        Set<Integer> alertGroupIdList = alertGroupList.stream()
                .map(AlertGroupEntity::getId)
                .collect(java.util.stream.Collectors.toSet());

        // 查询告警组下告警指标个数
        List<ClusterAlertQuota> clusQuotaList = clusterAlertQuotaMapper.selectByAlertGroupIds(alertGroupIdList);

        if (CollectionUtils.isNotEmpty(clusQuotaList)) {
            // 按告警组ID分组统计指标数量
            Map<Integer, List<ClusterAlertQuota>> alertGroupByGroupId = clusQuotaList.stream()
                    .collect(java.util.stream.Collectors.groupingBy(ClusterAlertQuota::getAlertGroupId));

            // 设置告警指标数量
            alertGroupList.forEach(a -> {
                List<ClusterAlertQuota> tmpQuotaList = alertGroupByGroupId.get(a.getId());
                int quotaCnt = CollectionUtils.isEmpty(tmpQuotaList) ? 0 : tmpQuotaList.size();
                a.setAlertQuotaNum(quotaCnt);
            });
        }

        return pageResult;
    }

    @Override
    public AlertGroupEntity saveAlertGroup(AlertGroupEntity alertGroup) {
        // 名称校验
        NotEmptyValidator notEmptyValidator = new NotEmptyValidator();
        LengthValidator lengthValidator = new LengthValidator();
        notEmptyValidator.setNext(lengthValidator);
        try {
            notEmptyValidator.validate(alertGroup.getAlertGroupName());
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }

        // 重复校验
        List<Integer> existGroupId = clusterAlertGroupMapMapper.selectByClusterId(alertGroup.getClusterId())
                .stream()
                .map(ClusterAlertGroupMap::getAlertGroupId)
                .collect(java.util.stream.Collectors.toList());

        List<String> existGroupName = alertGroupMapper.selectByIds(existGroupId)
                .stream()
                .map(AlertGroupEntity::getAlertGroupName)
                .collect(java.util.stream.Collectors.toList());

        if (existGroupName.contains(alertGroup.getAlertGroupName())) {
            throw new RuntimeException("告警组名称重复");
        }

        // 保存告警组
        alertGroupMapper.insert(alertGroup);

        // 创建映射关系
        ClusterAlertGroupMap clusterAlertGroupMap = new ClusterAlertGroupMap();
        clusterAlertGroupMap.setAlertGroupId(alertGroup.getId());
        clusterAlertGroupMap.setClusterId(alertGroup.getClusterId());
        clusterAlertGroupMapMapper.insertSelective(clusterAlertGroupMap);

        return alertGroup;
    }

    // 标准CRUD方法实现
    @Override
    public AlertGroupEntity getById(Integer id) {
        return alertGroupMapper.selectById(id);
    }

    @Override
    public AlertGroupEntity updateById(AlertGroupEntity entity) {
        alertGroupMapper.updateById(entity);
        return entity;
    }

    @Override
    public boolean removeByIds(List<Integer> ids) {
        return alertGroupMapper.deleteByIds(ids) > 0;
    }

    @Override
    public List<AlertGroupEntity> getAllAlertGroups() {
        return alertGroupMapper.selectAll();
    }
}
