package com.datasophon.dao.alert;

import com.datasophon.dao.entity.ClusterAlertHistory;
import com.datasophon.dao.enums.AlertLevel;
import com.datasophon.dao.mapper.ClusterAlertHistoryMapper;
import com.datasophon.domain.alert.gateway.AlertHistoryGateway;
import com.datasophon.domain.alert.model.AlertHistory;
import com.mybatisflex.core.query.QueryChain;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;

@Component
public class AlertHistoryGatewayImpl implements AlertHistoryGateway {

    private static final int ENABLED = 1;
    private static final int DISABLED = 2;

    private final ClusterAlertHistoryMapper alertHistoryMapper;

    public AlertHistoryGatewayImpl(ClusterAlertHistoryMapper alertHistoryMapper) {
        this.alertHistoryMapper = alertHistoryMapper;
    }

    @Override
    public boolean hasEnabledAlertHistory(String alertname, int clusterId, String hostname) {
        ClusterAlertHistory alertHistory = QueryChain.of(ClusterAlertHistory.class)
                .where(ClusterAlertHistory::getAlertTargetName).eq(alertname)
                .and(ClusterAlertHistory::getClusterId).eq(clusterId)
                .and(ClusterAlertHistory::getHostname).eq(hostname)
                .and(ClusterAlertHistory::getIsEnabled).eq(ENABLED)
                .one();

        return Objects.nonNull(alertHistory);
    }

    @Override
    public AlertHistory getEnabledAlertHistory(String alertname, int clusterId, String hostname) {
        ClusterAlertHistory clusterAlertHistory = QueryChain.of(ClusterAlertHistory.class)
                .where(ClusterAlertHistory::getAlertTargetName).eq(alertname)
                .and(ClusterAlertHistory::getClusterId).eq(clusterId)
                .and(ClusterAlertHistory::getHostname).eq(hostname)
                .and(ClusterAlertHistory::getIsEnabled).eq(ENABLED)
                .one();

        if (Objects.nonNull(clusterAlertHistory)) {
            AlertHistory alertHistory = new AlertHistory();
            BeanUtils.copyProperties(clusterAlertHistory, alertHistory);
            alertHistory.setAlertLevel(clusterAlertHistory.getAlertLevel().getValue());
            return alertHistory;
        }

        return null;
    }

    @Override
    public void updateAlertHistoryToDisabled(Integer id) {
        ClusterAlertHistory alertHistory = QueryChain.of(ClusterAlertHistory.class)
                .where(ClusterAlertHistory::getId).eq(id)
                .one();

        if (Objects.nonNull(alertHistory)) {
            alertHistory.setIsEnabled(DISABLED);
            alertHistoryMapper.update(alertHistory);
        }
    }

    @Override
    public boolean nodeHasWarnAlertList(String hostname, String serviceRoleName, Integer id) {
        List<ClusterAlertHistory> alertHistories = QueryChain.of(ClusterAlertHistory.class)
                .where(ClusterAlertHistory::getHostname).eq(hostname)
                .and(ClusterAlertHistory::getAlertGroupName).eq(serviceRoleName.toLowerCase())
                .and(ClusterAlertHistory::getIsEnabled).eq(ENABLED)
                .and(ClusterAlertHistory::getAlertLevel).eq(AlertLevel.WARN)
                .and(ClusterAlertHistory::getId).ne(id)
                .list();

        return !CollectionUtils.isEmpty(alertHistories);
    }
}
