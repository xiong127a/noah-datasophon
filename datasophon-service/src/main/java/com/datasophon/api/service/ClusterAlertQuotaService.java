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
import com.mybatisflex.core.service.IService;

import java.util.List;
import java.util.Set;

/**
 * 集群告警指标表
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-01
 */
public interface ClusterAlertQuotaService extends IService<ClusterAlertQuota> {

    /**
     * 获取告警指标分页列表
     */
    PageResult<ClusterAlertQuota> getAlertQuotaList(Integer clusterId, Integer alertGroupId, Integer noticeGroupId,
            String quotaName, Integer page, Integer pageSize);

    /**
     * 启动告警指标
     */
    void start(Integer clusterId, String alertQuotaIds);

    /**
     * 停止告警指标
     */
    void stop(Integer clusterId, String alertQuotaIds);

    /**
     * 保存告警指标
     */
    ClusterAlertQuota saveAlertQuota(ClusterAlertQuota clusterAlertQuota);

    /**
     * 根据服务名称查询告警指标列表
     */
    List<ClusterAlertQuota> listAlertQuotaByServiceName(String serviceName);

    /**
     * 根据通知组ID列表查询告警指标
     */
    List<ClusterAlertQuota> getByNoticeGroupIds(List<Integer> groupIds);

    /**
     * 根据告警组ID集合查询告警指标
     */
    List<ClusterAlertQuota> selectByAlertGroupIds(Set<Integer> alertGroupIds);
}
