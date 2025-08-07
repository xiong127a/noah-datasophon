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
import cn.hutool.extra.spring.SpringUtil;
import cn.hutool.http.HttpUtil;
import com.mybatisflex.core.paginate.Page;
import com.datasophon.api.service.NoticeGroupService;
import com.datasophon.api.service.ServiceStateManagementService;
import com.datasophon.common.dto.ClusterServiceRoleInstanceDTO;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.dao.entity.NoticeGroupEntity;
import com.datasophon.dao.entity.UserInfoEntity;
import com.datasophon.common.enums.AlertLevel;
import com.datasophon.dao.model.MPage;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AlertManager处理策略
 * 负责AlertManager服务的配置处理和状态检查
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-05
 */
public class AlertManagerHandlerStrategy implements ServiceRoleStrategy {

    @Override
    public void handlerConfig(Integer clusterId, List<ServiceConfig> list) {

        NoticeGroupService noticeGroupService = SpringUtil.getBean(NoticeGroupService.class);

        MPage<NoticeGroupEntity> page = new MPage<>();
        page.setPageSize(1000); // 使用MPage的setPageSize方法
        Page<NoticeGroupEntity> noticeGroupEntityPage = noticeGroupService.page(page);

        // 去掉之前的
        list.removeIf(serviceConfig -> StringUtils.isEmpty(serviceConfig.getConfigType())
                || serviceConfig.getConfigType().isEmpty());

        // 准备alertNoticeConfig,邮件通知组和路由
        List<ServiceConfig> alertNoticeConfig = noticeGroupEntityPage.getRecords().stream()
                .filter(v -> CollectionUtil.isNotEmpty(v.getUserIds()))
                .map(v -> ServiceConfig.builder()
                        .hidden(true)
                        .required(true)
                        .configType("")
                        .name(v.getId().toString())
                        .value(v.getUserIds().stream()
                                .map(UserInfoEntity::getEmail)
                                .filter(StringUtils::isNotEmpty)
                                .collect(Collectors.joining(",")))
                        .build())
                .toList();

        list.addAll(alertNoticeConfig);
    }

    @Override
    public void getConfig(Integer clusterId, List<ServiceConfig> list) {
        ServiceRoleStrategy.super.getConfig(clusterId, list);
    }

    @Override
    public void handlerServiceRoleCheck(ClusterServiceRoleInstanceDTO roleInstanceDto,
            Map<String, ClusterServiceRoleInstanceDTO> map) {
        String url = "http://" + roleInstanceDto.hostname() + ":9093";
        ServiceStateManagementService serviceStateManagementService = SpringUtil.getBean(ServiceStateManagementService.class);
        try {
            HttpUtil.get(url);
            serviceStateManagementService.recoverAlert(roleInstanceDto);
        } catch (Exception e) {
            // save alert
            String alertTargetName = roleInstanceDto.serviceRoleName() + " Survive";
            serviceStateManagementService.saveAlert(roleInstanceDto, alertTargetName, AlertLevel.EXCEPTION, "restart");
        }
    }

}
