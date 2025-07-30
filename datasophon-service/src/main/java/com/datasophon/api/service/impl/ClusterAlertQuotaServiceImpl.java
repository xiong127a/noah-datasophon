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

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.datasophon.api.master.ActorUtils;
import com.datasophon.api.master.PrometheusActor;
import com.datasophon.api.service.AlertGroupService;
import com.datasophon.api.service.ClusterAlertQuotaService;
import com.datasophon.api.service.NoticeGroupService;
import com.datasophon.common.Constants;
import com.datasophon.common.command.GenerateAlertConfigCommand;
import com.datasophon.common.model.AlertItem;
import com.datasophon.common.model.Generators;
import com.datasophon.common.utils.CollectionUtils;
import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.AlertGroupEntity;
import com.datasophon.dao.entity.ClusterAlertQuota;
import com.datasophon.dao.entity.NoticeGroupEntity;
import com.datasophon.dao.enums.QuotaState;
import com.datasophon.dao.mapper.ClusterAlertQuotaMapper;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.apache.pekko.actor.ActorRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service("clusterAlertQuotaService")
public class ClusterAlertQuotaServiceImpl extends ServiceImpl<ClusterAlertQuotaMapper, ClusterAlertQuota>
        implements
        ClusterAlertQuotaService {

    private static final Logger logger = LoggerFactory.getLogger(ClusterAlertQuotaServiceImpl.class);

    private static final String ALERT_RULE_FILE_SUFFIX = ".yml";
    private static final String ALERT_CONFIG_FORMAT = "prometheus";
    private static final String ALERT_OUTPUT_DIRECTORY = "alert_rules";

    @Autowired
    private AlertGroupService alertGroupService;

    @Autowired
    private NoticeGroupService noticeGroupService;


    @Override
    public Result getAlertQuotaList(Integer clusterId, Integer alertGroupId, Integer noticeGroupId, String quotaName,
            Integer page,
            Integer pageSize) {
        // 使用分页对象代替手动计算偏移量
        Page<ClusterAlertQuota> flexPage = new Page<>(page, pageSize);

        // 构建查询条件
        QueryChain<ClusterAlertQuota> query = QueryChain.of(ClusterAlertQuota.class);

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

        // 执行分页查询
        Page<ClusterAlertQuota> resultPage = query.page(flexPage);
        List<ClusterAlertQuota> alertQuotaList = resultPage.getRecords();

        if (CollectionUtils.isEmpty(alertQuotaList)) {
            return Result.successEmptyCount();
        }

        // 查询告警组
        Set<Integer> alertGroupIdList = alertQuotaList.stream()
                .map(ClusterAlertQuota::getAlertGroupId)
                .collect(Collectors.toSet());

        // 查询通知组
        List<Integer> noticeGroupIdList = alertQuotaList.stream()
                .map(ClusterAlertQuota::getNoticeGroupId)
                .collect(Collectors.toList());

        Collection<AlertGroupEntity> alertGroupEntityList = alertGroupService.listByIds(alertGroupIdList);
        Collection<NoticeGroupEntity> noticeGroupEntityList = noticeGroupService.listByIds(noticeGroupIdList);

        if (CollectionUtils.isNotEmpty(alertGroupEntityList)) {
            // 使用更具描述性的映射变量名
            Map<Integer, AlertGroupEntity> alertGroupById = alertGroupEntityList.stream()
                    .collect(Collectors.toMap(AlertGroupEntity::getId, entity -> entity, (a1, a2) -> a1));

            Map<Integer, NoticeGroupEntity> noticeGroupById = noticeGroupEntityList.stream()
                    .collect(Collectors.toMap(NoticeGroupEntity::getId, entity -> entity, (a1, a2) -> a1));

            // 填充告警指标的相关属性
            alertQuotaList.forEach(quota -> {
                AlertGroupEntity alertGroup = alertGroupById.get(quota.getAlertGroupId());
                NoticeGroupEntity noticeGroup = noticeGroupById.get(quota.getNoticeGroupId());

                if (Objects.nonNull(alertGroup)) {
                    quota.setAlertGroupName(alertGroup.getAlertGroupName());
                }

                if (Objects.nonNull(noticeGroup)) {
                    quota.setNoticeGroupName(noticeGroup.getNoticeGroupName());
                }

                quota.setQuotaStateCode(quota.getQuotaState().getValue());
            });
        }

        return Result.success(alertQuotaList).setTotal(resultPage.getTotalRow());
    }

    /**
     * 根据告警指标生成告警规则文件
     */
    private void alertRuleFile(Integer clusterId, Collection<ClusterAlertQuota> alertQuotaList) {
        Map<String, List<ClusterAlertQuota>> alertsByCategoryMap = new HashMap<>();

        for (ClusterAlertQuota alertQuota : alertQuotaList) {
            String category = alertQuota.getServiceCategory();

            if (!alertsByCategoryMap.containsKey(category)) {
                // 查询该类别下所有已启动的告警指标
                List<ClusterAlertQuota> activeQuotas = QueryChain.of(ClusterAlertQuota.class)
                        .where(ClusterAlertQuota::getServiceCategory).eq(category)
                        .and(ClusterAlertQuota::getQuotaState).eq(QuotaState.RUNNING)
                        .list();

                activeQuotas.add(alertQuota);
                alertsByCategoryMap.put(category, activeQuotas);
            } else {
                alertsByCategoryMap.get(category).add(alertQuota);
            }

            alertQuota.setQuotaState(QuotaState.RUNNING);
        }

        if (!alertQuotaList.isEmpty()) {
            logger.info("启动告警指标数量: {}", alertQuotaList.size());
            this.updateBatch(alertQuotaList);
        }

        // 构建告警配置文件映射
        Map<Generators, List<AlertItem>> configFileMap = new HashMap<>();

        for (Map.Entry<String, List<ClusterAlertQuota>> entry : alertsByCategoryMap.entrySet()) {
            String category = entry.getKey();
            List<ClusterAlertQuota> alerts = entry.getValue();

            // 去重处理
            List<ClusterAlertQuota> uniqueAlerts = alerts.stream()
                    .collect(Collectors.collectingAndThen(
                            Collectors.toCollection(
                                    () -> new TreeSet<>(Comparator.comparing(ClusterAlertQuota::getAlertQuotaName))),
                            ArrayList::new));

            // 配置生成器
            Generators generators = new Generators();
            generators.setFilename(category.toLowerCase() + ALERT_RULE_FILE_SUFFIX);
            generators.setConfigFormat(ALERT_CONFIG_FORMAT);
            generators.setOutputDirectory(ALERT_OUTPUT_DIRECTORY);

            // 转换告警指标为AlertItem
            List<AlertItem> alertItems = uniqueAlerts.stream().map(this::convertToAlertItem)
                    .peek(item -> item.setClusterId(clusterId))
                    .collect(Collectors.toList());

            configFileMap.put(generators, alertItems);
        }

        // 发送命令生成告警配置文件
        ActorRef prometheusActor = ActorUtils.getLocalActor(
                PrometheusActor.class,
                ActorUtils.getActorRefName(PrometheusActor.class));

        GenerateAlertConfigCommand alertConfigCommand = new GenerateAlertConfigCommand();
        alertConfigCommand.setClusterId(clusterId);
        alertConfigCommand.setConfigFileMap(configFileMap);
        prometheusActor.tell(alertConfigCommand, ActorRef.noSender());
    }

    /**
     * 将告警指标转换为AlertItem
     */
    private AlertItem convertToAlertItem(ClusterAlertQuota quota) {
        AlertItem alertItem = new AlertItem();
        alertItem.setAlertName(quota.getAlertQuotaName());

        // 构建告警表达式
        String exprBuilder = quota.getAlertExpr() +
                " " +
                quota.getCompareMethod() +
                " " +
                quota.getAlertThreshold();
        alertItem.setAlertExpr(exprBuilder);

        alertItem.setServiceRoleName(quota.getServiceRoleName());
        alertItem.setAlertLevel(quota.getAlertLevel().getDesc());
        alertItem.setAlertAdvice(quota.getAlertAdvice());
        alertItem.setTriggerDuration(quota.getTriggerDuration());
        alertItem.setNoticeGroupId(quota.getNoticeGroupId());

        return alertItem;
    }

    @Override
    public void start(Integer clusterId, String alertQuotaIds) {
        if (StringUtils.isBlank(alertQuotaIds)) {
            return;
        }

        List<String> ids = Arrays.asList(alertQuotaIds.split(StrUtil.COMMA));
        if (CollUtil.isEmpty(ids)) {
            return;
        }

        Collection<ClusterAlertQuota> alertQuotaList = this.listByIds(ids);
        alertRuleFile(clusterId, alertQuotaList);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void stop(Integer clusterId, String alertQuotaIds) {
        if (StringUtils.isBlank(alertQuotaIds)) {
            return;
        }

        List<String> ids = Arrays.asList(alertQuotaIds.split(StrUtil.COMMA));
        if (CollUtil.isEmpty(ids)) {
            return;
        }

        Set<String> categories = new HashSet<>(ids.size());

        // 1、修改禁用状态并更新
        Collection<ClusterAlertQuota> alertQuotas = this.listByIds(ids);
        alertQuotas.forEach(quota -> {
            quota.setQuotaState(QuotaState.STOPPED);
            categories.add(quota.getServiceCategory());
        });
        this.updateBatch(alertQuotas);

        // 2、查询需要重新生成告警规则文件的告警指标
        if (CollUtil.isEmpty(categories)) {
            return;
        }

        Collection<ClusterAlertQuota> activeQuotas = QueryChain.of(ClusterAlertQuota.class)
                .where(ClusterAlertQuota::getQuotaState).eq(QuotaState.RUNNING)
                .and(ClusterAlertQuota::getServiceCategory).in(categories)
                .list();

        if (CollUtil.isEmpty(activeQuotas)) {
            return;
        }

        alertRuleFile(clusterId, activeQuotas);
    }

    @Override
    public void saveAlertQuota(ClusterAlertQuota clusterAlertQuota) {
        clusterAlertQuota.setQuotaState(QuotaState.STOPPED);
        clusterAlertQuota.setCreateTime(new Date());

        // 获取告警组信息并设置服务类别
        AlertGroupEntity alertGroupEntity = alertGroupService.getById(clusterAlertQuota.getAlertGroupId());
        clusterAlertQuota.setServiceCategory(alertGroupEntity.getAlertGroupCategory());

        this.save(clusterAlertQuota);
    }

    @Override
    public List<ClusterAlertQuota> listAlertQuotaByServiceName(String serviceName) {
        return QueryChain.of(ClusterAlertQuota.class)
                .where(ClusterAlertQuota::getServiceCategory).eq(serviceName)
                .list();
    }

    @Override
    public List<ClusterAlertQuota> getByNoticeGroupIds(List<Integer> groupIds) {
        if (CollUtil.isEmpty(groupIds)) {
            return new ArrayList<>();
        }

        return QueryChain.of(ClusterAlertQuota.class)
                .where(ClusterAlertQuota::getNoticeGroupId).in(groupIds)
                .list();
    }
}
