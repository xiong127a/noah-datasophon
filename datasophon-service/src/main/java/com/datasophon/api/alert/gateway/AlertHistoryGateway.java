package com.datasophon.api.alert.gateway;

import com.datasophon.api.alert.model.AlertHistory;

public interface AlertHistoryGateway {
    boolean hasEnabledAlertHistory(String alertname, int clusterId, String hostname);

    AlertHistory getEnabledAlertHistory(String alertname, int clusterId, String hostname);

    void updateAlertHistoryToDisabled(Integer id);

    boolean nodeHasWarnAlertList(String hostname, String serviceRoleName, Integer id);
}
