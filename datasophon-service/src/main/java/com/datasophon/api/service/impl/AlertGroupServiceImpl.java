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
import com.datasophon.dao.entity.ClusterAlertGroupMapEntity;
import com.datasophon.dao.entity.ClusterAlertQuotaEntity;
import com.datasophon.dao.mapper.AlertGroupMapper;
import com.datasophon.dao.mapper.ClusterAlertGroupMapMapper;
import com.datasophon.dao.mapper.ClusterAlertQuotaMapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 告警组表实现
 * 按照架构重构规范，迁移QueryChain到DAO层
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-01
 */
@Service("alertGroupService")
public class AlertGroupServiceImpl
        extends ServiceImpl<AlertGroupMapper, AlertGroupEntity>
        implements AlertGroupService {

    private static final Logger logger = LoggerFactory.getLogger(AlertGroupServiceImpl.class);

    @Autowired
    private ClusterAlertGroupMapMapper clusterAlertGroupMapMapper;

    @Autowired
    private ClusterAlertQuotaMapper clusterAlertQuotaMapper;

    @Autowired
    private AlertGroupConverter alertGroupConverter;

    @Override
    public PageResult<AlertGroupDTO> getAlertGroupList(Long clusterId, String alertGroupName, Integer page,
            Integer pageSize) {
        // 查询告警组映射关系
        List<ClusterAlertGroupMapEntity> alertGroupMapList = clusterAlertGroupMapMapper.selectByClusterId(clusterId);

        if (CollectionUtils.isEmpty(alertGroupMapList)) {
            return PageResult.empty(page, pageSize);
        }

        // 提取告警组ID集合
        List<Long> groupIds = alertGroupMapList.stream()
                .map(ClusterAlertGroupMapEntity::getAlertGroupId)
                .toList();

        // 查询告警组列表和总数
        PageResult<AlertGroupEntity> entityPageResult = this.getMapper().selectAlertGroupsByIdsWithName(
                groupIds, alertGroupName, page, pageSize);

        List<AlertGroupEntity> alertGroupList = entityPageResult.getRecords();

        if (CollectionUtils.isEmpty(alertGroupList)) {
            return PageResult.empty(page, pageSize);
        }

        // 获取告警组ID集合用于后续查询
        Set<Long> alertGroupIdSet = alertGroupList.stream()
                .map(AlertGroupEntity::getId)
                .collect(java.util.stream.Collectors.toSet());

        // 查询告警组下告警指标个数
        List<ClusterAlertQuotaEntity> quotaList = clusterAlertQuotaMapper.selectByAlertGroupIds(alertGroupIdSet);

        if (CollectionUtils.isNotEmpty(quotaList)) {
            // 按告警组ID分组统计指标数量
            Map<Long, List<ClusterAlertQuotaEntity>> quotaGroupMap = quotaList.stream()
                    .collect(java.util.stream.Collectors.groupingBy(ClusterAlertQuotaEntity::getAlertGroupId));

            // 设置告警指标数量
            alertGroupList.forEach(entity -> {
                List<ClusterAlertQuotaEntity> tmpQuotaList = quotaGroupMap.get(entity.getId());
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
        List<Long> existGroupIds = clusterAlertGroupMapMapper.selectByClusterId(alertGroupDTO.clusterId())
                .stream()
                .map(ClusterAlertGroupMapEntity::getAlertGroupId)
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
        ClusterAlertGroupMapEntity clusterAlertGroupMapEntity = new ClusterAlertGroupMapEntity();
        clusterAlertGroupMapEntity.setAlertGroupId(alertGroupEntity.getId());
        clusterAlertGroupMapEntity.setClusterId(alertGroupEntity.getClusterId());
        clusterAlertGroupMapMapper.insertSelective(clusterAlertGroupMapEntity);

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
    public boolean deleteAlertGroups(List<Long> ids) {
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
    public void validateAlertGroupBeforeDelete(List<Long> ids) {
        // 校验是否绑定告警指标
        List<ClusterAlertQuotaEntity> quotaList = clusterAlertQuotaMapper.selectByAlertGroupIds(ids);

        if (CollectionUtils.isNotEmpty(quotaList)) {
            logger.warn("告警组删除验证失败，存在 {} 个绑定的告警指标", quotaList.size());
            throw new RuntimeException("告警组已绑定告警指标，无法删除");
        }
        logger.debug("告警组删除验证通过，无绑定的告警指标");
    }

}
