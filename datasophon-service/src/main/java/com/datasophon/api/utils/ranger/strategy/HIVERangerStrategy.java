package com.datasophon.api.utils.ranger.strategy;

import cn.hutool.core.collection.CollUtil;
import com.datasophon.api.utils.ranger.client.RangerClient;
import com.datasophon.api.utils.ranger.client.RangerUtil;
import com.datasophon.api.utils.ranger.client.model.Policy;
import com.datasophon.api.utils.ranger.client.utils.RangerClientException;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.dao.entity.ClusterTenant;
import com.datasophon.dao.entity.tenantResource.TenantHiveResource;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
public class HIVERangerStrategy extends AbstractRangerStrategy implements RangerStrategy {

    public HIVERangerStrategy(Integer clusterId) throws Exception {
        super(clusterId);
    }

    @Override
    public ExecResult createService() throws Exception {
        String hiveServer2Host = globalVariables.get("${hive.server2.thrift.bind.host}");
        String hiveServer2Port = globalVariables.get("${hive.server2.thrift.port}");
        String hiveUrl = "jdbc:hive2://" + hiveServer2Host + ":" + hiveServer2Port;

        try {
            rangerClient.getServices()
                    .createService(RangerUtil.simpleHiveService("hivedev", hiveUrl));
            RangerUtil.updateDefaultPolicy(rangerClient, "hivedev");
            log.info("config hive ranger plugin success");
            execResult.setExecResult(true);
        } catch (RangerClientException e) {
            log.error("config hive ranger plugin failed");
            log.error(e.getMessage());
            execResult.setExecErrOut(e.getMessage());
        }
        rangerClient.stop();
        return execResult;
    }

    @Override
    public ExecResult operatePolicy(ClusterTenant clusterTenant) throws Exception {
        execResult.setExecResult(true);
        if (CollUtil.isNotEmpty(clusterTenant.getHdfsResourceList())) {
            Policy policy = getHivePolicy(clusterTenant);
            try {
                if (Objects.isNull(clusterTenant.getId())) {
                    rangerClient.getPolicies().createPolicy(policy);
                } else {
                    Policy returnPolicy = rangerClient.getPolicies().getPolicyByName("hivedev", clusterTenant.getTenantName());
                    rangerClient.getPolicies().updatePolicy(returnPolicy.getId(), policy);
                }
                log.info("operate hive policy success");
            } catch (Exception e) {
                log.error("operate hive policy failed");
                execResult.setExecResult(false);
                execResult.setExecErrOut(e.getMessage());
            }
        }
        rangerClient.stop();
        return execResult;
    }

    private Policy getHivePolicy(ClusterTenant clusterTenant) {
        List<String> hiveDatabases = clusterTenant.getHiveResourceList()
                .stream()
                .map(t -> (TenantHiveResource) t)
                .map(TenantHiveResource::getHiveDatabase)
                .collect(Collectors.toList());
        return RangerUtil.simpleHivePolicyForDatabase(
                "hivedev",
                clusterTenant.getTenantName(),
                hiveDatabases,
                Collections.singletonList(clusterTenant.getTenantName())
        );
    }

}
