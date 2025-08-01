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

import com.datasophon.api.converter.AlertGroupConverter;
import com.datasophon.api.service.AlertGroupService;
import com.datasophon.api.utils.string.validator.LengthValidator;
import com.datasophon.api.utils.string.validator.NotEmptyValidator;
import com.datasophon.common.dto.AlertGroupDTO;
import com.datasophon.common.model.PageResult;
import com.datasophon.common.utils.CollectionUtils;
import com.datasophon.dao.entity.AlertGroupEntity;
import com.datasophon.dao.entity.ClusterAlertGroupMap;
import com.datasophon.dao.entity.ClusterAlertQuota;
import com.datasophon.dao.mapper.AlertGroupMapper;
import com.datasophon.dao.mapper.ClusterAlertGroupMapMapper;
import com.datasophon.dao.mapper.ClusterAlertQuotaMapper;
import com.mybatisflex.core.query.QueryChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 告警组表实现
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-14
 */
@Service("alertGroupService")
public class AlertGroupServiceImpl
        extends ServiceImpl<AlertGroupMapper, AlertGroupEntity>
        implements AlertGroupService {

    @Autowired
    private ClusterAlertGroupMapMapper clusterAlertGroupMapMapper;

    @Autowired
    private ClusterAlertQuotaMapper clusterAlertQuotaMapper;

    @Autowired
    private AlertGroupConverter alertGroupConverter;

    @Override
    public PageResult<AlertGroupDTO> getAlertGroupList(Integer clusterId, String alertGroupName, Integer page,
            Integer pageSize) {
        // 查询告警组映射关系
        List<ClusterAlertGroupMap> alertGroupMapList = clusterAlertGroupMapMapper.selectByClusterId(clusterId);

        if (CollectionUtils.isEmpty(alertGroupMapList)) {
            return PageResult.empty(page, pageSize);
        }

        // 提取告警组ID集合
        List<Integer> groupIds = alertGroupMapList.stream()
                .map(ClusterAlertGroupMap::getAlertGroupId)
                .toList();

        // 查询告警组列表和总数
        PageResult<AlertGroupEntity> entityPageResult = this.getMapper().selectAlertGroupsByIdsWithName(
                groupIds, alertGroupName, page, pageSize);

        List<AlertGroupEntity> alertGroupList = entityPageResult.getRecords();

        if (CollectionUtils.isEmpty(alertGroupList)) {
            return PageResult.empty(page, pageSize);
        }

        // 获取告警组ID集合用于后续查询
        Set<Integer> alertGroupIdSet = alertGroupList.stream()
                .map(AlertGroupEntity::getId)
                .collect(java.util.stream.Collectors.toSet());

        // 查询告警组下告警指标个数
        List<ClusterAlertQuota> quotaList = clusterAlertQuotaMapper.selectByAlertGroupIds(alertGroupIdSet);

        if (CollectionUtils.isNotEmpty(quotaList)) {
            // 按告警组ID分组统计指标数量
            Map<Integer, List<ClusterAlertQuota>> quotaGroupMap = quotaList.stream()
                    .collect(java.util.stream.Collectors.groupingBy(ClusterAlertQuota::getAlertGroupId));

            // 设置告警指标数量
            alertGroupList.forEach(entity -> {
                List<ClusterAlertQuota> tmpQuotaList = quotaGroupMap.get(entity.getId());
                int quotaCnt = CollectionUtils.isEmpty(tmpQuotaList) ? 0 : tmpQuotaList.size();
                entity.setAlertQuotaNum(quotaCnt);
            });
        }

        // 转换为DTO
        List<AlertGroupDTO> dtoList = alertGroupList.stream()
                .map(alertGroupConverter::entityToDto)
                .toList();
        return PageResult.of(dtoList, entityPageResult.getTotal(), page, pageSize);
    }

    @Override
    public AlertGroupDTO saveAlertGroup(AlertGroupDTO alertGroupDTO) {
        // 名称校验
        NotEmptyValidator notEmptyValidator = new NotEmptyValidator();
        LengthValidator lengthValidator = new LengthValidator();
        notEmptyValidator.setNext(lengthValidator);
        try {
            notEmptyValidator.validate(alertGroupDTO.alertGroupName());
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }

        // 重复校验
        List<Integer> existGroupIds = clusterAlertGroupMapMapper.selectByClusterId(alertGroupDTO.clusterId())
                .stream()
                .map(ClusterAlertGroupMap::getAlertGroupId)
                .toList();

        List<String> existGroupNames = this.getMapper().selectByIds(existGroupIds)
                .stream()
                .map(AlertGroupEntity::getAlertGroupName)
                .toList();

        if (existGroupNames.contains(alertGroupDTO.alertGroupName())) {
            throw new RuntimeException("告警组名称重复");
        }

        // 转换为Entity并设置创建时间
        AlertGroupEntity alertGroupEntity = alertGroupConverter.dtoToEntity(alertGroupDTO);
        alertGroupEntity.setCreateTime(new Date());

        // 保存告警组
        this.save(alertGroupEntity);

        // 创建映射关系
        ClusterAlertGroupMap clusterAlertGroupMap = new ClusterAlertGroupMap();
        clusterAlertGroupMap.setAlertGroupId(alertGroupEntity.getId());
        clusterAlertGroupMap.setClusterId(alertGroupEntity.getClusterId());
        clusterAlertGroupMapMapper.insertSelective(clusterAlertGroupMap);

        // 转换为DTO并返回
        return alertGroupConverter.entityToDto(alertGroupEntity);
    }

    @Override
    public AlertGroupDTO getAlertGroupById(Integer id) {
        AlertGroupEntity entity = this.getById(id);
        return entity != null ? alertGroupConverter.entityToDto(entity) : null;
    }

    @Override
    public AlertGroupDTO updateAlertGroup(AlertGroupDTO alertGroupDTO) {
        AlertGroupEntity entity = alertGroupConverter.dtoToEntity(alertGroupDTO);
        this.updateById(entity);
        return alertGroupConverter.entityToDto(entity);
    }

    @Override
    public boolean deleteAlertGroups(List<Integer> ids) {
        // 先校验是否可以删除
        validateAlertGroupBeforeDelete(ids);

        // 删除告警组
        return this.removeByIds(ids);
    }

    @Override
    public List<AlertGroupDTO> getAllAlertGroups() {
        List<AlertGroupEntity> entities = this.list();
        return entities.stream()
                .map(alertGroupConverter::entityToDto)
                .toList();
    }

    @Override
    public void validateAlertGroupBeforeDelete(List<Integer> ids) {
        // 校验是否绑定告警指标
        List<ClusterAlertQuota> quotaList = QueryChain.of(ClusterAlertQuota.class)
                .where(ClusterAlertQuota::getAlertGroupId).in(ids)
                .list();

        if (CollectionUtils.isNotEmpty(quotaList)) {
            throw new RuntimeException("告警组已绑定告警指标，无法删除");
        }
    }

}
