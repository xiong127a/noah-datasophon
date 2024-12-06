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

package com.datasophon.api.strategy;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.datasophon.api.service.NoticeGroupService;
import com.datasophon.api.service.NoticeGroupUserService;
import com.datasophon.api.utils.ProcessUtils;
import com.datasophon.api.utils.SpringTool;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.model.ServiceRoleInfo;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;
import com.datasophon.dao.entity.NoticeGroupEntity;
import com.datasophon.dao.entity.UserInfoEntity;
import com.datasophon.dao.enums.AlertLevel;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import cn.hutool.http.HttpUtil;
import com.datasophon.dao.model.MPage;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

public class AlertManagerHandlerStrategy implements ServiceRoleStrategy {

    @Override
    public void handler(Integer clusterId, List<String> hosts) {

    }

    @Override
    public void handlerConfig(Integer clusterId, List<ServiceConfig> list) {

        NoticeGroupService noticeGroupService = (NoticeGroupService) SpringTool.getBean("noticeGroupService");

        MPage<NoticeGroupEntity> page = new MPage<>();
        page.setSize(1000);
        IPage<NoticeGroupEntity> noticeGroupEntityIPage = noticeGroupService.pageNoticeGroup(page);

        //去掉之前的
        list.removeIf(serviceConfig -> StringUtils.isEmpty(serviceConfig.getConfigType()) || "".equals(serviceConfig.getConfigType()));

        //准备alertNoticeConfig,邮件通知组和路由
        List<ServiceConfig> alertNoticeConfig = noticeGroupEntityIPage.getRecords().stream()
                .filter(v -> CollectionUtil.isNotEmpty(v.getUserIds()))
                .map(v ->
                        ServiceConfig.builder()
                                .hidden(true)
                                .required(true)
                                .configType("")
                                .name(v.getId().toString())
                                .value(v.getUserIds().stream()
                                        .map(UserInfoEntity::getEmail)
                                        .filter(StringUtils::isNotEmpty)
                                        .collect(Collectors.joining(",")))
                                .build())
                .collect(Collectors.toList());

        list.addAll(alertNoticeConfig);
    }

    @Override
    public void getConfig(Integer clusterId, List<ServiceConfig> list) {

    }

    @Override
    public void handlerServiceRoleInfo(ServiceRoleInfo serviceRoleInfo, String hostname) {

    }

    @Override
    public void handlerServiceRoleCheck(ClusterServiceRoleInstanceEntity roleInstanceEntity,
                                        Map<String, ClusterServiceRoleInstanceEntity> map) {
        String url = "http://" + roleInstanceEntity.getHostname() + ":9093";
        try {
            HttpUtil.get(url);
            ProcessUtils.recoverAlert(roleInstanceEntity);
        } catch (Exception e) {
            // save alert
            String alertTargetName = roleInstanceEntity.getServiceRoleName() + " Survive";
            ProcessUtils.saveAlert(roleInstanceEntity, alertTargetName, AlertLevel.EXCEPTION, "restart");

        }
    }

    @Override
    public void handlerK8sServiceRoleCheck(ClusterServiceRoleInstanceEntity roleInstanceEntity, Map<String, ClusterServiceRoleInstanceEntity> map) {
        handlerServiceRoleCheck(roleInstanceEntity, map);
    }
}
