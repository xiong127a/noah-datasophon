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

package com.datasophon.api.service;

import com.datasophon.dao.entity.ClusterAlertHistory;
import com.datasophon.common.model.PageResult;

import java.util.List;

/**
 * 集群告警历史表
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2024-12-19
 */
public interface ClusterAlertHistoryService {

    void saveAlertHistory(String alertMessage);

    List<ClusterAlertHistory> getAlertList(Integer serviceInstanceId);

    PageResult<ClusterAlertHistory> getAllAlertList(Integer clusterId, Integer page, Integer pageSize);

    void removeAlertByRoleInstanceIds(List<Integer> ids);

    // 新增方法：根据服务实例ID统计启用的告警数量
    long countEnabledByServiceInstanceId(Integer serviceInstanceId);

    // 标准CRUD方法
    ClusterAlertHistory getById(Integer id);

    ClusterAlertHistory save(ClusterAlertHistory entity);

    ClusterAlertHistory updateById(ClusterAlertHistory entity);

    boolean removeByIds(List<Integer> ids);

    List<ClusterAlertHistory> getAllAlertHistories();
}
