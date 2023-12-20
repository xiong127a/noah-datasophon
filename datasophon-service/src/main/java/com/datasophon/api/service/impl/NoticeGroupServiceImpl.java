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
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datasophon.api.enums.Status;
import com.datasophon.api.exceptions.ServiceException;
import com.datasophon.api.service.ClusterAlertQuotaService;
import com.datasophon.api.service.NoticeGroupService;
import com.datasophon.api.service.NoticeGroupUserService;
import com.datasophon.api.service.UserInfoService;
import com.datasophon.dao.entity.ClusterAlertQuota;
import com.datasophon.dao.entity.NoticeGroupEntity;
import com.datasophon.dao.entity.NoticeGroupUserEntity;
import com.datasophon.dao.entity.UserInfoEntity;
import com.datasophon.dao.mapper.NoticeGroupMapper;
import com.datasophon.dao.model.MPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service("noticeGroupService")
public class NoticeGroupServiceImpl extends ServiceImpl<NoticeGroupMapper, NoticeGroupEntity> implements NoticeGroupService {

    @Autowired
    private NoticeGroupUserService noticeGroupUserService;

    @Autowired
    private UserInfoService userInfoService;

    @Autowired
    private ClusterAlertQuotaService clusterAlertQuotaService;

    @Override
    public void saveOrUpdateNoticeGroup(NoticeGroupEntity noticeGroup) {

        LambdaQueryWrapper<NoticeGroupEntity> query = new LambdaQueryWrapper<>();
        query.eq(NoticeGroupEntity::getNoticeGroupName, noticeGroup.getNoticeGroupName());
        if (Objects.nonNull(noticeGroup.getId())) {
            query.ne(NoticeGroupEntity::getId, noticeGroup.getId());
        }
        List<NoticeGroupEntity> list = list(query);
        if (CollectionUtil.isNotEmpty(list)) {
            throw new ServiceException(Status.NOTICE_GROUP_NAME_EXIST);
        }

        if (Objects.nonNull(noticeGroup.getId())) {
            updateById(noticeGroup);
        } else {
            save(noticeGroup);
        }
        noticeGroupUserService.removeByGroupIds(Collections.singletonList(noticeGroup.getId()));

        List<NoticeGroupUserEntity> collect = noticeGroup.getUserIds().stream()
                .map(v ->
                        NoticeGroupUserEntity.builder()
                                .noticeGroupId(noticeGroup.getId())
                                .userId(v.getId())
                                .build()
                )
                .collect(Collectors.toList());
        noticeGroupUserService.saveBatch(collect);
    }


    @Override
    @Transactional
    public void removeNoticeGroup(List<Integer> list) {
        List<ClusterAlertQuota> byNoticeGroupIds = clusterAlertQuotaService.getByNoticeGroupIds(list);
        if (CollectionUtil.isNotEmpty(byNoticeGroupIds)){
            throw new ServiceException(Status.NOTICE_GROUP_USE);
        }
        removeByIds(list);
        noticeGroupUserService.removeByGroupIds(list);
    }


    public IPage<NoticeGroupEntity> pageNoticeGroup(MPage<NoticeGroupEntity> mPage) {
        //获取查询参数
        NoticeGroupEntity param = Optional.ofNullable(mPage.getParam()).orElse(NoticeGroupEntity.builder().build());
        //设置查询条件
        LambdaQueryWrapper<NoticeGroupEntity> query = new LambdaQueryWrapper<>();

        if (StrUtil.isNotBlank(param.getNoticeGroupName())) {
            query.like(NoticeGroupEntity::getNoticeGroupName, param.getNoticeGroupName());
        }

        //查询
        IPage<NoticeGroupEntity> page = page(mPage, query);

        //查询用户
        List<Integer> groupIds = page.getRecords().stream().map(NoticeGroupEntity::getId).collect(Collectors.toList());
        Map<Integer, String> userinfo = userInfoService.list().stream().collect(Collectors.toMap(UserInfoEntity::getId, UserInfoEntity::getUsername));

        Map<Integer, List<UserInfoEntity>> users = noticeGroupUserService
                .listByGroupIds(groupIds).stream()
                .map(v -> UserInfoEntity.builder().userType(v.getNoticeGroupId()).id(v.getUserId()).username(userinfo.get(v.getUserId())).build())
                .collect(Collectors.groupingBy(UserInfoEntity::getUserType));

        page.getRecords().forEach(record -> record.setUserIds(users.get(record.getId())));
        return page;
    }
}
