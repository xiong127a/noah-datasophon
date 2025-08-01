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

import com.datasophon.dao.entity.ClusterAlertQuota;
import com.datasophon.common.model.PageResult;

import java.util.List;
import java.util.Set;

/**
 * 集群告警指标表
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2024-12-19
 */
public interface ClusterAlertQuotaService {

    PageResult<ClusterAlertQuota> getAlertQuotaList(Integer clusterId, Integer alertGroupId, Integer noticeGroupId,
            String quotaName, Integer page, Integer pageSize);

    void start(Integer clusterId, String alertQuotaIds);

    void stop(Integer clusterId, String alertQuotaIds);

    ClusterAlertQuota saveAlertQuota(ClusterAlertQuota clusterAlertQuota);

    List<ClusterAlertQuota> listAlertQuotaByServiceName(String serviceName);

    List<ClusterAlertQuota> getByNoticeGroupIds(List<Integer> list);

    // 根据告警组ID集合查询告警配额
    List<ClusterAlertQuota> selectByAlertGroupIds(Set<Integer> alertGroupIds);

    // 标准CRUD方法
    ClusterAlertQuota getById(Integer id);

    ClusterAlertQuota save(ClusterAlertQuota entity);

    ClusterAlertQuota updateById(ClusterAlertQuota entity);

    boolean removeByIds(List<Integer> ids);

    List<ClusterAlertQuota> getAllAlertQuotas();
}
