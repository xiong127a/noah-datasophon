package com.datasophon.api.alert.gateway;

import com.datasophon.api.alert.model.AlertHistory;

public interface AlertHistoryGateway {
    boolean hasEnabledAlertHistory(String alertname, Long clusterId, String hostname);

    AlertHistory getEnabledAlertHistory(String alertname, Long clusterId, String hostname);

    void updateAlertHistoryToDisabled(Long id);

    boolean nodeHasWarnAlertList(String hostname, String serviceRoleName, Long id);
}
