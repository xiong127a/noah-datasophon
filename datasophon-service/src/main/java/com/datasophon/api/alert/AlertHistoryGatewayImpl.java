package com.datasophon.api.alert;

import com.datasophon.api.alert.gateway.AlertHistoryGateway;
import com.datasophon.api.alert.model.AlertHistory;
import com.datasophon.dao.entity.ClusterAlertHistoryEntity;
import com.datasophon.common.enums.AlertLevel;
import com.datasophon.dao.mapper.ClusterAlertHistoryMapper;
import com.mybatisflex.core.query.QueryChain;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;

@Component
public class AlertHistoryGatewayImpl implements AlertHistoryGateway {

    private static final int ENABLED = 1;
    private static final int DISABLED = 2;

    @Autowired
    private ClusterAlertHistoryMapper alertHistoryMapper;



    @Override
    public boolean hasEnabledAlertHistory(String alertname, Long clusterId, String hostname) {
        ClusterAlertHistoryEntity alertHistory = QueryChain.of(ClusterAlertHistoryEntity.class)
                .where(ClusterAlertHistoryEntity::getAlertTargetName).eq(alertname)
                .and(ClusterAlertHistoryEntity::getClusterId).eq(clusterId)
                .and(ClusterAlertHistoryEntity::getHostname).eq(hostname)
                .and(ClusterAlertHistoryEntity::getIsEnabled).eq(ENABLED)
                .one();

        return Objects.nonNull(alertHistory);
    }

    @Override
    public AlertHistory getEnabledAlertHistory(String alertname, Long clusterId, String hostname) {
        ClusterAlertHistoryEntity clusterAlertHistoryEntity = QueryChain.of(ClusterAlertHistoryEntity.class)
                .where(ClusterAlertHistoryEntity::getAlertTargetName).eq(alertname)
                .and(ClusterAlertHistoryEntity::getClusterId).eq(clusterId)
                .and(ClusterAlertHistoryEntity::getHostname).eq(hostname)
                .and(ClusterAlertHistoryEntity::getIsEnabled).eq(ENABLED)
                .one();

        if (Objects.nonNull(clusterAlertHistoryEntity)) {
            AlertHistory alertHistory = new AlertHistory();
            BeanUtils.copyProperties(clusterAlertHistoryEntity, alertHistory);
            alertHistory.setAlertLevel(clusterAlertHistoryEntity.getAlertLevel().getValue());
            return alertHistory;
        }

        return null;
    }

    @Override
    public void updateAlertHistoryToDisabled(Long id) {
        ClusterAlertHistoryEntity alertHistory = QueryChain.of(ClusterAlertHistoryEntity.class)
                .where(ClusterAlertHistoryEntity::getId).eq(id)
                .one();

        if (Objects.nonNull(alertHistory)) {
            alertHistory.setIsEnabled(DISABLED);
            alertHistoryMapper.update(alertHistory);
        }
    }

    @Override
    public boolean nodeHasWarnAlertList(String hostname, String serviceRoleName, Long id) {
        List<ClusterAlertHistoryEntity> alertHistories = QueryChain.of(ClusterAlertHistoryEntity.class)
                .where(ClusterAlertHistoryEntity::getHostname).eq(hostname)
                .and(ClusterAlertHistoryEntity::getAlertGroupName).eq(serviceRoleName.toLowerCase())
                .and(ClusterAlertHistoryEntity::getIsEnabled).eq(ENABLED)
                .and(ClusterAlertHistoryEntity::getAlertLevel).eq(AlertLevel.WARN)
                .and(ClusterAlertHistoryEntity::getId).ne(id)
                .list();

        return !CollectionUtils.isEmpty(alertHistories);
    }
}
