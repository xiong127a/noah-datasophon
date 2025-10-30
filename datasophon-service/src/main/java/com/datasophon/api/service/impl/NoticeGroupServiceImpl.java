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

import com.datasophon.api.exceptions.ServiceException;
import com.datasophon.api.service.AlertManagersConfigService;
import com.datasophon.api.service.ClusterAlertQuotaService;
import com.datasophon.api.service.ClusterServiceRoleGroupConfigService;
import com.datasophon.api.service.ClusterServiceRoleInstanceService;
import com.datasophon.api.service.NoticeGroupService;
import com.datasophon.api.service.NoticeGroupUserService;
import com.datasophon.api.service.ServiceInstallService;
import com.datasophon.api.converter.NoticeGroupConverter;
import com.datasophon.api.utils.ProcessUtils;
import com.datasophon.api.utils.string.validator.LengthValidator;
import com.datasophon.api.utils.string.validator.NotEmptyValidator;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.dto.NoticeGroupDTO;
import com.datasophon.common.model.PageResult;
import com.datasophon.common.utils.CollectionUtils;
import com.datasophon.dao.entity.ClusterAlertQuotaEntity;
import com.datasophon.dao.entity.NoticeGroupEntity;
import com.datasophon.dao.entity.NoticeGroupUserEntity;
import com.datasophon.dao.mapper.NoticeGroupMapper;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.apache.pekko.actor.ActorRef;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 通知组表实现
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-01
 */
@Service("noticeGroupService")
public class NoticeGroupServiceImpl extends ServiceImpl<NoticeGroupMapper, NoticeGroupEntity>
        implements NoticeGroupService {

    @Autowired
    private NoticeGroupUserService noticeGroupUserService;

    @Autowired
    private NoticeGroupConverter noticeGroupConverter;

    @Autowired
    private ClusterAlertQuotaService clusterAlertQuotaService;

    @Autowired
    private ServiceInstallService serviceInstallService;

    @Autowired
    private ClusterServiceRoleGroupConfigService clusterServiceRoleGroupConfigService;
    @Autowired
    private ClusterServiceRoleInstanceService clusterServiceRoleInstanceService;

    @Autowired
    private AlertManagersConfigService alertManagersConfigService;

    @Override
    public PageResult<NoticeGroupDTO> getNoticeGroupList(String noticeGroupName, Integer page, Integer pageSize) {
        // 执行分页查询
        List<NoticeGroupEntity> noticeGroupList = getMapper().selectByNameWithPagination(
                noticeGroupName, (page - 1) * pageSize, pageSize);

        // 执行总数查询
        long total = getMapper().countByName(noticeGroupName);

        if (CollectionUtils.isEmpty(noticeGroupList)) {
            return PageResult.empty(page, pageSize);
        }

        // 查询通知组关联的用户信息
        List<Long> groupIds = noticeGroupList.stream()
                .map(NoticeGroupEntity::getId)
                .toList();

        // 获取用户关联信息
        Map<Long, List<Long>> groupUserMap = noticeGroupUserService.listByGroupIds(groupIds).stream()
                .collect(Collectors.groupingBy(
                        NoticeGroupUserEntity::getNoticeGroupId,
                        Collectors.mapping(NoticeGroupUserEntity::getUserId, Collectors.toList())));

        // 转换为DTO
        List<NoticeGroupDTO> dtoList = noticeGroupList.stream()
                .map(entity -> {
                    List<Long> userIds = groupUserMap.getOrDefault(entity.getId(), List.of());
                    return entityToDto(entity).withUserIds(userIds);
                })
                .toList();

        return PageResult.of(dtoList, total, page, pageSize);
    }

    @Override
    public NoticeGroupDTO saveNoticeGroup(NoticeGroupDTO noticeGroupDTO) {
        // 名称校验
        NotEmptyValidator notEmptyValidator = new NotEmptyValidator();
        LengthValidator lengthValidator = new LengthValidator();
        notEmptyValidator.setNext(lengthValidator);
        try {
            notEmptyValidator.validate(noticeGroupDTO.noticeGroupName());
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }

        // 重复校验
        List<NoticeGroupEntity> existGroups = getMapper().selectByNameExcludingId(noticeGroupDTO.noticeGroupName(),
                null);

        if (CollectionUtils.isNotEmpty(existGroups)) {
            throw new RuntimeException("通知组名称重复");
        }

        // 转换并保存
        NoticeGroupEntity entity = dtoToEntity(noticeGroupDTO);
        entity.setCreateTime(LocalDateTime.now());
        this.save(entity);

        // 处理用户关联
        if (CollectionUtils.isNotEmpty(noticeGroupDTO.userIds())) {
            List<NoticeGroupUserEntity> userRelations = noticeGroupDTO.userIds().stream()
                    .map(userId -> NoticeGroupUserEntity.builder()
                            .noticeGroupId(entity.getId())
                            .userId(userId)
                            .build())
                    .collect(Collectors.toList());
            noticeGroupUserService.saveBatch(userRelations);
        }

        genAlertManagerConfig();

        return entityToDto(entity).withUserIds(noticeGroupDTO.userIds());
    }

    @Override
    public NoticeGroupDTO getNoticeGroupById(Long id) {
        NoticeGroupEntity entity = this.getById(id);
        if (entity == null) {
            return null;
        }

        // 查询关联的用户ID
        List<Long> userIds = noticeGroupUserService.listByGroupIds(List.of(id)).stream()
                .map(NoticeGroupUserEntity::getUserId)
                .toList();

        return entityToDto(entity).withUserIds(userIds);
    }

    @Override
    public NoticeGroupDTO updateNoticeGroup(NoticeGroupDTO noticeGroupDTO) {
        // 名称校验
        NotEmptyValidator notEmptyValidator = new NotEmptyValidator();
        LengthValidator lengthValidator = new LengthValidator();
        notEmptyValidator.setNext(lengthValidator);
        try {
            notEmptyValidator.validate(noticeGroupDTO.noticeGroupName());
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }

        // 重复校验（排除自身）
        List<NoticeGroupEntity> existGroups = getMapper().selectByNameExcludingId(noticeGroupDTO.noticeGroupName(),
                noticeGroupDTO.id());

        if (CollectionUtils.isNotEmpty(existGroups)) {
            throw new RuntimeException("通知组名称重复");
        }

        // 更新通知组
        NoticeGroupEntity entity = dtoToEntity(noticeGroupDTO);
        this.updateById(entity);

        // 更新用户关联（先删除再添加）
        noticeGroupUserService.removeByGroupIds(List.of(noticeGroupDTO.id()));

        if (CollectionUtils.isNotEmpty(noticeGroupDTO.userIds())) {
            List<NoticeGroupUserEntity> userRelations = noticeGroupDTO.userIds().stream()
                    .map(userId -> NoticeGroupUserEntity.builder()
                            .noticeGroupId(noticeGroupDTO.id())
                            .userId(userId)
                            .build())
                    .collect(Collectors.toList());
            noticeGroupUserService.saveBatch(userRelations);
        }

        genAlertManagerConfig();

        return entityToDto(entity).withUserIds(noticeGroupDTO.userIds());
    }

    @Override
    public boolean deleteNoticeGroups(List<Long> ids) {
        validateNoticeGroupBeforeDelete(ids);

        // 删除通知组
        boolean result = this.removeByIds(ids);

        // 删除用户关联
        noticeGroupUserService.removeByGroupIds(ids);

        genAlertManagerConfig();

        return result;
    }

    @Override
    public List<NoticeGroupDTO> getAllNoticeGroups() {
        List<NoticeGroupEntity> entities = this.list();
        return entities.stream()
                .map(this::entityToDto)
                .toList();
    }

    @Override
    public void validateNoticeGroupBeforeDelete(List<Long> ids) {
        List<ClusterAlertQuotaEntity> quotaList = clusterAlertQuotaService.getByNoticeGroupIds(ids);
        if (CollectionUtils.isNotEmpty(quotaList)) {
            throw new ServiceException("该通知组被告警指标使用，无法删除");
        }
    }

    @Override
    public List<NoticeGroupDTO> getByIds(List<Long> ids) {
        List<NoticeGroupEntity> entities = this.listByIds(ids);
        return entities.stream()
                .map(this::entityToDto)
                .toList();
    }

    /**
     * 生成alertManager 配置信息
     */
    private void genAlertManagerConfig() {
        /*
         * 更新配置信息，修改了通知组之后，配置要同步变更，
         */
        List<com.datasophon.common.dto.ClusterServiceRoleInstanceDTO> alertManager = clusterServiceRoleInstanceService
                .listServiceRoleByName("AlertManager");
        for (com.datasophon.common.dto.ClusterServiceRoleInstanceDTO roleInstanceDto : alertManager) {
            com.datasophon.common.dto.ClusterServiceRoleGroupConfigDTO roleGroupConfig = clusterServiceRoleGroupConfigService
                    .getConfigByRoleGroupId(roleInstanceDto.roleGroupId());
            List<ServiceConfig> serviceConfig = ProcessUtils.getServiceConfig(roleGroupConfig);
            serviceInstallService.saveServiceConfig(roleInstanceDto.clusterId(), roleInstanceDto.serviceName(), serviceConfig,
                    roleGroupConfig.roleGroupId(), "(AUTO) 生成alertManager 配置信息", -1L, "system");
        }

        // 调用配置生成 - 使用Service代替Actor
        alertManagersConfigService.generateAlertManagerConfig();
    }

    // Manual conversion methods
    private NoticeGroupDTO entityToDto(NoticeGroupEntity entity) {
        if (entity == null) {
            return null;
        }
        // 使用MapStruct转换器进行Entity到DTO转换
        return noticeGroupConverter.entityToDto(entity);
    }

    private NoticeGroupEntity dtoToEntity(NoticeGroupDTO dto) {
        if (dto == null) {
            return null;
        }
        return NoticeGroupEntity.builder()
                .id(dto.id())
                .clusterId(dto.clusterId())
                .noticeGroupName(dto.noticeGroupName())
                .createTime(dto.createTime())
                .build();
    }
}
