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

package com.datasophon.dao.mapper;

import com.datasophon.dao.entity.ClusterAlertQuota;
import com.datasophon.dao.enums.QuotaState;
import com.datasophon.common.model.PageResult;
import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.paginate.Page;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 集群告警指标表
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-01
 */
@Mapper
public interface ClusterAlertQuotaMapper extends BaseMapper<ClusterAlertQuota> {

    /**
     * 根据告警组ID集合查询告警指标
     */
    default List<ClusterAlertQuota> selectByAlertGroupIds(Set<Integer> alertGroupIds) {
        if (alertGroupIds == null || alertGroupIds.isEmpty()) {
            return Collections.emptyList();
        }
        QueryWrapper query = QueryWrapper.create()
                .where(ClusterAlertQuota::getAlertGroupId).in(alertGroupIds);
        return this.selectListByQuery(query);
    }

    /**
     * 根据告警组ID列表查询告警指标（重载方法，支持List参数）
     */
    default List<ClusterAlertQuota> selectByAlertGroupIds(List<Integer> alertGroupIds) {
        if (alertGroupIds == null || alertGroupIds.isEmpty()) {
            return Collections.emptyList();
        }
        QueryWrapper query = QueryWrapper.create()
                .where(ClusterAlertQuota::getAlertGroupId).in(alertGroupIds);
        return this.selectListByQuery(query);
    }

    /**
     * 分页查询告警指标列表
     */
    default PageResult<ClusterAlertQuota> selectAlertQuotaListWithPage(Integer clusterId, Integer alertGroupId,
            Integer noticeGroupId, String quotaName, Integer page, Integer pageSize) {

        QueryWrapper query = QueryWrapper.create();

        // 按条件筛选
        if (alertGroupId != null) {
            query.where(ClusterAlertQuota::getAlertGroupId).eq(alertGroupId);
        }

        if (noticeGroupId != null) {
            query.and(ClusterAlertQuota::getNoticeGroupId).eq(noticeGroupId);
        }

        if (StringUtils.isNotBlank(quotaName)) {
            query.and(ClusterAlertQuota::getAlertQuotaName).like(quotaName);
        }

        // 获取总数
        long count = this.selectCountByQuery(query);

        if (count == 0) {
            return PageResult.empty(page, pageSize);
        }

        // 分页查询
        Page<ClusterAlertQuota> flexPage = new Page<>(page, pageSize);
        Page<ClusterAlertQuota> resultPage = this.paginate(flexPage, query);

        return PageResult.of(resultPage.getRecords(), count, page, pageSize);
    }

    /**
     * 根据服务类别查询运行中的告警指标
     */
    default List<ClusterAlertQuota> selectRunningByServiceCategory(String category) {
        QueryWrapper query = QueryWrapper.create()
                .where(ClusterAlertQuota::getServiceCategory).eq(category)
                .and(ClusterAlertQuota::getQuotaState).eq(QuotaState.RUNNING);
        return this.selectListByQuery(query);
    }

    /**
     * 根据服务类别集合查询运行中的告警指标
     */
    default List<ClusterAlertQuota> selectRunningByServiceCategories(Collection<String> categories) {
        if (categories == null || categories.isEmpty()) {
            return Collections.emptyList();
        }
        QueryWrapper query = QueryWrapper.create()
                .where(ClusterAlertQuota::getQuotaState).eq(QuotaState.RUNNING)
                .and(ClusterAlertQuota::getServiceCategory).in(categories);
        return this.selectListByQuery(query);
    }

    /**
     * 根据服务类别查询告警指标
     */
    default List<ClusterAlertQuota> selectByServiceCategory(String serviceName) {
        QueryWrapper query = QueryWrapper.create()
                .where(ClusterAlertQuota::getServiceCategory).eq(serviceName);
        return this.selectListByQuery(query);
    }

    /**
     * 根据通知组ID列表查询告警指标
     */
    default List<ClusterAlertQuota> selectByNoticeGroupIds(List<Integer> groupIds) {
        if (groupIds == null || groupIds.isEmpty()) {
            return Collections.emptyList();
        }
        QueryWrapper query = QueryWrapper.create()
                .where(ClusterAlertQuota::getNoticeGroupId).in(groupIds);
        return this.selectListByQuery(query);
    }

    /**
     * 批量更新告警指标状态
     * 注意：这里保留是因为有实际的循环更新逻辑，如果Service层可以替代则建议删除
     */
    default int updateBatchQuotaState(Collection<ClusterAlertQuota> entityList, QuotaState newState) {
        if (entityList == null || entityList.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (ClusterAlertQuota entity : entityList) {
            entity.setQuotaState(newState);
            count += this.update(entity);
        }
        return count;
    }
}