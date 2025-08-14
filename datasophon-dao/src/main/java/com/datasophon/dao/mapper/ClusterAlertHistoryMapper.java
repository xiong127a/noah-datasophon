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

import com.datasophon.dao.entity.ClusterAlertHistoryEntity;
import com.datasophon.common.model.PageResult;
import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.paginate.Page;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 集群告警历史数据访问对象
 * 提供集群告警历史的数据库操作
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
@Mapper
public interface ClusterAlertHistoryMapper extends BaseMapper<ClusterAlertHistoryEntity> {

    /**
     * 根据服务实例ID查询启用的告警历史
     */
    default List<ClusterAlertHistoryEntity> selectEnabledByServiceInstanceId(Long serviceInstanceId) {
        QueryWrapper query = QueryWrapper.create()
                .where(ClusterAlertHistoryEntity::getIsEnabled).eq(1);

        if (serviceInstanceId != null) {
            query.and(ClusterAlertHistoryEntity::getServiceInstanceId).eq(serviceInstanceId);
        }

        return this.selectListByQuery(query);
    }

    /**
     * 根据集群ID分页查询启用的告警历史
     */
    default PageResult<ClusterAlertHistoryEntity> selectEnabledByClusterIdWithPage(Long clusterId, Integer page,
                                                                                   Integer pageSize) {
        // 构建查询条件
        QueryWrapper query = QueryWrapper.create()
                .where(ClusterAlertHistoryEntity::getClusterId).eq(clusterId)
                .and(ClusterAlertHistoryEntity::getIsEnabled).eq(1)
                .orderBy(ClusterAlertHistoryEntity::getCreateTime, false);

        // 获取总数
        long count = this.selectCountByQuery(query);

        if (count == 0) {
            return PageResult.empty(page, pageSize);
        }

        // 分页查询
        Page<ClusterAlertHistoryEntity> flexPage = new Page<>(page, pageSize);
        Page<ClusterAlertHistoryEntity> resultPage = this.paginate(flexPage, query);

        return PageResult.of(resultPage.getRecords(), count, page, pageSize);
    }

    /**
     * 根据角色实例ID列表删除启用的告警历史
     */
    default int removeEnabledByRoleInstanceIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        QueryWrapper query = QueryWrapper.create()
                .where(ClusterAlertHistoryEntity::getIsEnabled).eq(1)
                .and(ClusterAlertHistoryEntity::getServiceRoleInstanceId).in(ids);
        return this.deleteByQuery(query);
    }

    /**
     * 根据服务实例ID统计启用的告警数量
     */
    default long countEnabledByServiceInstanceId(Long serviceInstanceId) {
        QueryWrapper query = QueryWrapper.create()
                .where(ClusterAlertHistoryEntity::getServiceInstanceId).eq(serviceInstanceId)
                .and(ClusterAlertHistoryEntity::getIsEnabled).eq(1);
        return this.selectCountByQuery(query);
    }

    /**
     * 根据服务实例ID查询停止状态的角色实例
     * 
     * @param serviceInstanceId 服务实例ID
     * @return 停止状态的告警历史列表
     */
    default List<ClusterAlertHistoryEntity> selectStoppedRolesByServiceId(Long serviceInstanceId) {
        QueryWrapper query = QueryWrapper.create()
                .where(ClusterAlertHistoryEntity::getServiceInstanceId).eq(serviceInstanceId)
                .and(ClusterAlertHistoryEntity::getIsEnabled).eq(1)
                .and(ClusterAlertHistoryEntity::getAlertInfo).like("%停止%");
        return this.selectListByQuery(query);
    }

    /**
     * 根据服务实例ID查询告警状态的角色实例
     * 
     * @param serviceInstanceId 服务实例ID
     * @return 告警状态的告警历史列表
     */
    default List<ClusterAlertHistoryEntity> selectAlarmRolesByServiceId(Long serviceInstanceId) {
        QueryWrapper query = QueryWrapper.create()
                .where(ClusterAlertHistoryEntity::getServiceInstanceId).eq(serviceInstanceId)
                .and(ClusterAlertHistoryEntity::getIsEnabled).eq(1)
                .and(ClusterAlertHistoryEntity::getAlertLevel).in("WARNING", "ERROR", "CRITICAL");
        return this.selectListByQuery(query);
    }

    /**
     * 根据告警目标名称、集群ID、主机名和启用状态查询告警历史
     * 用于ServiceStateManagementService中的告警管理
     * 
     * @param alertTargetName 告警目标名称
     * @param clusterId       集群ID
     * @param hostname        主机名
     * @param isEnabled       是否启用（1-启用，2-禁用）
     * @return 告警历史
     */
    default ClusterAlertHistoryEntity selectByAlertTargetNameAndClusterIdAndHostnameAndEnabled(
            String alertTargetName, Long clusterId, String hostname, Integer isEnabled) {
        QueryWrapper query = QueryWrapper.create()
                .where(ClusterAlertHistoryEntity::getAlertTargetName).eq(alertTargetName)
                .and(ClusterAlertHistoryEntity::getClusterId).eq(clusterId)
                .and(ClusterAlertHistoryEntity::getHostname).eq(hostname)
                .and(ClusterAlertHistoryEntity::getIsEnabled).eq(isEnabled);
        return this.selectOneByQuery(query);
    }

}
