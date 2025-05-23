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

import akka.actor.ActorRef;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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
import com.datasophon.api.utils.StringValidator.LengthValidator;
import com.datasophon.api.utils.StringValidator.NotEmptyValidator;
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

    @Autowired
    private NoticeGroupUserService noticeGroupUserService;

    @Autowired
    private UserInfoService userInfoService;

    @Autowired
    private ClusterAlertQuotaService clusterAlertQuotaService;

    @Autowired
    private ServiceInstallService serviceInstallService;

    @Autowired
    private ClusterServiceRoleGroupConfigService clusterServiceRoleGroupConfigService;
    @Autowired
    private ClusterServiceRoleInstanceService clusterServiceRoleInstanceService;

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

        LambdaQueryWrapper<NoticeGroupEntity> query = new LambdaQueryWrapper<>();
        query.eq(NoticeGroupEntity::getNoticeGroupName, noticeGroup.getNoticeGroupName());
        if (Objects.nonNull(noticeGroup.getId())) {
            query.ne(NoticeGroupEntity::getId, noticeGroup.getId());
        }
        List<NoticeGroupEntity> list = list(query);
        if (CollectionUtil.isNotEmpty(list)) {
            // return Result.error("通知组名称重复");
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
                    roleGroupConfig.getRoleGroupId(), "(AUTO) 生成alertManager 配置信息",-1,"system");
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
            // throw new ServiceException(Status.NOTICE_GROUP_USE);
        }
        removeByIds(list);
        noticeGroupUserService.removeByGroupIds(list);

        genAlertManagerConfig();
    }

    @Override
    public IPage<NoticeGroupEntity> pageNoticeGroup(MPage<NoticeGroupEntity> mPage) {
        // 获取查询参数
        NoticeGroupEntity param = Optional.ofNullable(mPage.getParam()).orElse(NoticeGroupEntity.builder().build());
        // 设置查询条件
        LambdaQueryWrapper<NoticeGroupEntity> query = new LambdaQueryWrapper<>();

        if (StrUtil.isNotBlank(param.getNoticeGroupName())) {
            query.like(NoticeGroupEntity::getNoticeGroupName, param.getNoticeGroupName());
        }

        // 查询
        IPage<NoticeGroupEntity> page = page(mPage, query);

        // 查询用户
        List<Integer> groupIds = page.getRecords().stream().map(NoticeGroupEntity::getId).collect(Collectors.toList());
        Map<Integer, UserInfoEntity> userinfo = userInfoService.list().stream()
                .collect(Collectors.toMap(UserInfoEntity::getId, v -> v));

        // groupid 和用户的的对应map
        Map<Integer, List<UserInfoEntity>> users = noticeGroupUserService.listByGroupIds(groupIds).stream()
                .filter(v -> Objects.nonNull(userinfo.get(v.getUserId())))
                .map(v -> {
                    UserInfoEntity userInfoEntity = userinfo.get(v.getUserId());
                    userInfoEntity.setUserType(v.getNoticeGroupId());
                    return userInfoEntity;
                }).collect(Collectors.groupingBy(UserInfoEntity::getUserType));

        page.getRecords().forEach(record -> record.setUserIds(users.get(record.getId())));
        return page;
    }

}
