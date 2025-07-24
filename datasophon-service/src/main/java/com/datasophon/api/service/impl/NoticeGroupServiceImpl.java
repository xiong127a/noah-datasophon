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

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.datasophon.api.exceptions.ServiceException;
import com.datasophon.api.master.ActorUtils;
import com.datasophon.api.master.AlertManagersActor;
import com.datasophon.api.service.ClusterAlertQuotaService;
import com.datasophon.api.service.ClusterServiceRoleGroupConfigService;
import com.datasophon.api.service.ClusterServiceRoleInstanceService;
import com.datasophon.api.service.NoticeGroupService;
import com.datasophon.api.service.NoticeGroupUserService;
import com.datasophon.api.service.ServiceInstallService;
import com.datasophon.api.service.UserInfoService;
import com.datasophon.api.utils.ProcessUtils;
import com.datasophon.api.utils.string.validator.LengthValidator;
import com.datasophon.api.utils.string.validator.NotEmptyValidator;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.ClusterAlertQuota;
import com.datasophon.dao.entity.ClusterServiceRoleGroupConfig;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;
import com.datasophon.dao.entity.NoticeGroupEntity;
import com.datasophon.dao.entity.NoticeGroupUserEntity;
import com.datasophon.dao.entity.UserInfoEntity;
import com.datasophon.dao.mapper.NoticeGroupMapper;
import com.datasophon.dao.model.MPage;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.apache.pekko.actor.ActorRef;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service("noticeGroupService")
public class NoticeGroupServiceImpl extends ServiceImpl<NoticeGroupMapper, NoticeGroupEntity>
        implements NoticeGroupService {

    private final NoticeGroupUserService noticeGroupUserService;

    private final UserInfoService userInfoService;

    @org.springframework.context.annotation.Lazy
    private final ClusterAlertQuotaService clusterAlertQuotaService;

    private final ServiceInstallService serviceInstallService;

    private final ClusterServiceRoleGroupConfigService clusterServiceRoleGroupConfigService;
    private final ClusterServiceRoleInstanceService clusterServiceRoleInstanceService;
    @Autowired
    public NoticeGroupServiceImpl(NoticeGroupUserService noticeGroupUserService, UserInfoService userInfoService, @org.springframework.context.annotation.Lazy ClusterAlertQuotaService clusterAlertQuotaService, ServiceInstallService serviceInstallService, ClusterServiceRoleGroupConfigService clusterServiceRoleGroupConfigService, ClusterServiceRoleInstanceService clusterServiceRoleInstanceService) {
        this.noticeGroupUserService = noticeGroupUserService;
        this.userInfoService = userInfoService;
        this.clusterAlertQuotaService = clusterAlertQuotaService;
        this.serviceInstallService = serviceInstallService;
        this.clusterServiceRoleGroupConfigService = clusterServiceRoleGroupConfigService;
        this.clusterServiceRoleInstanceService = clusterServiceRoleInstanceService;
    }

    @Override
    public Result saveOrUpdateNoticeGroup(NoticeGroupEntity noticeGroup) {

        // 名称校验
        NotEmptyValidator notEmptyValidator = new NotEmptyValidator();
        LengthValidator lengthValidator = new LengthValidator();
        notEmptyValidator.setNext(lengthValidator);
        try {
            notEmptyValidator.validate(noticeGroup.getNoticeGroupName());
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }

        // 查询是否存在相同名称的通知组
        QueryChain<NoticeGroupEntity> query = QueryChain.of(NoticeGroupEntity.class)
                .where(NoticeGroupEntity::getNoticeGroupName).eq(noticeGroup.getNoticeGroupName());

        // 如果是更新操作，则排除自身
        if (Objects.nonNull(noticeGroup.getId())) {
            query.and(NoticeGroupEntity::getId).ne(noticeGroup.getId());
        }

        List<NoticeGroupEntity> list = query.list();

        if (CollectionUtil.isNotEmpty(list)) {
            return Result.error("通知组名称重复");
        }

        if (Objects.nonNull(noticeGroup.getId())) {
            updateById(noticeGroup);
        } else {
            save(noticeGroup);
        }
        noticeGroupUserService.removeByGroupIds(Collections.singletonList(noticeGroup.getId()));

        List<NoticeGroupUserEntity> collect = noticeGroup.getUserIds().stream()
                .map(v -> NoticeGroupUserEntity.builder()
                        .noticeGroupId(noticeGroup.getId())
                        .userId(v.getId())
                        .build())
                .collect(Collectors.toList());
        noticeGroupUserService.saveBatch(collect);

        genAlertManagerConfig();

        return Result.success();
    }

    /**
     * 生成alertManager 配置信息
     */
    private void genAlertManagerConfig() {
        /*
         * 更新配置信息，修改了通知组之后，配置要同步变更，
         */
        List<ClusterServiceRoleInstanceEntity> alertManager = clusterServiceRoleInstanceService
                .listServiceRoleByName("AlertManager");
        for (ClusterServiceRoleInstanceEntity roleInstanceEntity : alertManager) {
            ClusterServiceRoleGroupConfig roleGroupConfig = clusterServiceRoleGroupConfigService
                    .getConfigByRoleGroupId(roleInstanceEntity.getRoleGroupId());
            List<ServiceConfig> serviceConfig = ProcessUtils.getServiceConfig(roleGroupConfig);
            serviceInstallService.saveServiceConfig(roleInstanceEntity.getClusterId(), "ALERTMANAGER", serviceConfig,
                    roleGroupConfig.getRoleGroupId(), "(AUTO) 生成alertManager 配置信息", -1, "system");
        }

        // 调用配置生成
        ActorRef localActor = ActorUtils.getLocalActor(AlertManagersActor.class,
                ActorUtils.getActorRefName(AlertManagersActor.class));
        localActor.tell(1, ActorRef.noSender());
    }

    @Override
    @Transactional
    public void removeNoticeGroup(List<Integer> list) {
        List<ClusterAlertQuota> byNoticeGroupIds = clusterAlertQuotaService.getByNoticeGroupIds(list);
        if (CollectionUtil.isNotEmpty(byNoticeGroupIds)) {
            throw new ServiceException("该通知组被使用，无法删除");
        }
        removeByIds(list);
        noticeGroupUserService.removeByGroupIds(list);

        genAlertManagerConfig();
    }

    @Override
    public Page<NoticeGroupEntity> pageNoticeGroup(MPage<NoticeGroupEntity> mPage) {
        // 获取查询参数
        NoticeGroupEntity param = Optional.ofNullable(mPage.getParam())
                .orElse(NoticeGroupEntity.builder().build());

        // 构建查询链，基于参数动态构建条件
        QueryChain<NoticeGroupEntity> query = QueryChain.of(NoticeGroupEntity.class);

        // 根据名称模糊查询（如果参数中有名称）
        if (StrUtil.isNotBlank(param.getNoticeGroupName())) {
            query.where(NoticeGroupEntity::getNoticeGroupName).like(param.getNoticeGroupName());
        }

        // 执行分页查询
        Page<NoticeGroupEntity> resultPage = query.page(mPage);

        // 如果查询结果为空，提前返回
        if (CollectionUtil.isEmpty(resultPage.getRecords())) {
            return resultPage;
        }

        // 查询所有相关的用户信息
        List<Integer> groupIds = resultPage.getRecords().stream()
                .map(NoticeGroupEntity::getId)
                .collect(Collectors.toList());

        // 获取所有用户信息，构建ID到用户实体的映射
        Map<Integer, UserInfoEntity> userInfoMap = userInfoService.list().stream()
                .collect(Collectors.toMap(UserInfoEntity::getId, user -> user));

        // 查询通知组和用户的关系，构建组ID到用户列表的映射
        Map<Integer, List<UserInfoEntity>> groupUserMap = noticeGroupUserService.listByGroupIds(groupIds).stream()
                .filter(relation -> Objects.nonNull(userInfoMap.get(relation.getUserId())))
                .map(relation -> {
                    // 将用户类型设置为通知组ID
                    UserInfoEntity userInfo = userInfoMap.get(relation.getUserId());
                    userInfo.setUserType(relation.getNoticeGroupId());
                    return userInfo;
                })
                .collect(Collectors.groupingBy(UserInfoEntity::getUserType));

        // 将用户列表关联到各个通知组
        resultPage.getRecords()
                .forEach(group -> group.setUserIds(groupUserMap.getOrDefault(group.getId(), Collections.emptyList())));

        return resultPage;
    }
}
