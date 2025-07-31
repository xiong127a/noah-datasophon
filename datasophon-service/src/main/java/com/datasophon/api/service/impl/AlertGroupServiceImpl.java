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

import com.datasophon.api.service.AlertGroupService;
import com.datasophon.api.service.ClusterAlertGroupMapService;
import com.datasophon.api.utils.string.validator.LengthValidator;
import com.datasophon.api.utils.string.validator.NotEmptyValidator;
import com.datasophon.common.utils.CollectionUtils;
import com.datasophon.api.vo.Result;
import com.datasophon.dao.entity.AlertGroupEntity;
import com.datasophon.dao.entity.ClusterAlertGroupMap;
import com.datasophon.dao.entity.ClusterAlertQuota;
import com.datasophon.dao.mapper.AlertGroupMapper;
import com.mybatisflex.core.query.QueryChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service("alertGroupService")
public class AlertGroupServiceImpl extends ServiceImpl<AlertGroupMapper, AlertGroupEntity>
        implements
        AlertGroupService {


    @Autowired
    private ClusterAlertGroupMapService alertGroupMapService;



    @Override
    public Result<List<AlertGroupEntity>> getAlertGroupList(Integer clusterId, String alertGroupName, Integer page, Integer pageSize) {
        // 查询告警组映射关系
        List<ClusterAlertGroupMap> alertGroupMapList = QueryChain.of(ClusterAlertGroupMap.class)
                .where(ClusterAlertGroupMap::getClusterId).eq(clusterId)
                .list();

        if (CollectionUtils.isEmpty(alertGroupMapList)) {
            return Result.successEmptyCount();
        }

        // 提取告警组ID集合
        List<Integer> groupIds = alertGroupMapList.stream()
                .map(ClusterAlertGroupMap::getAlertGroupId)
                .collect(Collectors.toList());

        // 构建查询条件
        QueryChain<AlertGroupEntity> query = QueryChain.of(AlertGroupEntity.class)
                .where(AlertGroupEntity::getId).in(groupIds);

        // 添加名称过滤条件
        if (StringUtils.isNotBlank(alertGroupName)) {
            query.and(AlertGroupEntity::getAlertGroupName).like(alertGroupName);
        }

        // 获取总记录数
        long count = query.count();

        if (count == 0) {
            return Result.successEmptyCount();
        }

        // 使用MyBatis-Flex内置的分页API
        com.mybatisflex.core.paginate.Page<AlertGroupEntity> flexPage = new com.mybatisflex.core.paginate.Page<>(page,
                pageSize);
        com.mybatisflex.core.paginate.Page<AlertGroupEntity> resultPage = query.page(flexPage);

        List<AlertGroupEntity> alertGroupList = resultPage.getRecords();

        if (CollectionUtils.isEmpty(alertGroupList)) {
            return Result.successEmptyCount();
        }

        // 获取告警组ID集合用于后续查询
        Set<Integer> alertGroupIdList = alertGroupList.stream()
                .map(AlertGroupEntity::getId)
                .collect(Collectors.toSet());

        // 查询告警组下告警指标个数
        List<ClusterAlertQuota> clusQuotaList = QueryChain.of(ClusterAlertQuota.class)
                .where(ClusterAlertQuota::getAlertGroupId).in(alertGroupIdList)
                .list();

        if (CollectionUtils.isNotEmpty(clusQuotaList)) {
            // 按告警组ID分组统计指标数量
            Map<Integer, List<ClusterAlertQuota>> alertGroupByGroupId = clusQuotaList.stream()
                    .collect(Collectors.groupingBy(ClusterAlertQuota::getAlertGroupId));

            // 设置告警指标数量
            alertGroupList.forEach(a -> {
                List<ClusterAlertQuota> tmpQuotaList = alertGroupByGroupId.get(a.getId());
                int quotaCnt = CollectionUtils.isEmpty(tmpQuotaList) ? 0 : tmpQuotaList.size();
                a.setAlertQuotaNum(quotaCnt);
            });
        }

        return Result.success(alertGroupList,count);
    }

    @Override
    public Result<String> saveAlertGroup(AlertGroupEntity alertGroup) {

        // 名称校验
        NotEmptyValidator notEmptyValidator = new NotEmptyValidator();
        LengthValidator lengthValidator = new LengthValidator();
        notEmptyValidator.setNext(lengthValidator);
        try {
            notEmptyValidator.validate(alertGroup.getAlertGroupName());
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }

        // 重复校验
        List<Integer> existGroupId = QueryChain.of(ClusterAlertGroupMap.class)
                .where(ClusterAlertGroupMap::getClusterId).eq(alertGroup.getClusterId())
                .list()
                .stream()
                .map(ClusterAlertGroupMap::getAlertGroupId)
                .collect(Collectors.toList());

        List<String> existGroupName = this
                .listByIds(existGroupId)
                .stream()
                .map(AlertGroupEntity::getAlertGroupName)
                .toList();

        if (existGroupName.contains(alertGroup.getAlertGroupName())) {
            return Result.error("告警组名称重复");
        }

        this.save(alertGroup);
        ClusterAlertGroupMap clusterAlertGroupMap = new ClusterAlertGroupMap();
        clusterAlertGroupMap.setAlertGroupId(alertGroup.getId());
        clusterAlertGroupMap.setClusterId(alertGroup.getClusterId());
        alertGroupMapService.save(clusterAlertGroupMap);
        return Result.success();
    }
}
