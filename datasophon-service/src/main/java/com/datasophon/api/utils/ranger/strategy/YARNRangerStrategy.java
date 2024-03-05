package com.datasophon.api.utils.ranger.strategy;

import cn.hutool.core.collection.CollUtil;
import com.datasophon.api.utils.ranger.client.RangerClient;
import com.datasophon.api.utils.ranger.client.RangerUtil;
import com.datasophon.api.utils.ranger.client.model.Policy;
import com.datasophon.api.utils.ranger.client.utils.RangerClientException;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.dao.entity.ClusterTenant;
import com.datasophon.dao.entity.tenantResource.TenantYarnResource;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
public class YARNRangerStrategy extends AbstractRangerStrategy implements RangerStrategy {

    public YARNRangerStrategy(Integer clusterId) throws Exception {
        super(clusterId);
    }

    @Override
    public ExecResult createService() throws Exception {
        String rm1Addr = "http://" + globalVariables.get("${yarn.resourcemanager.webapp.address.rm1}");
        String rm2Addr = "http://" + globalVariables.get("${yarn.resourcemanager.webapp.address.rm2}");

        try {
            rangerClient.getServices()
                    .createService(RangerUtil.simpleYarnService("yarndev", String.join(",", rm1Addr, rm2Addr)));
            RangerUtil.updateDefaultPolicy(rangerClient, "yarndev");
            log.info("config yarn ranger plugin success");
            execResult.setExecResult(true);
        } catch (RangerClientException e) {
            log.error("config yarn ranger plugin failed");
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
            Policy policy = getYarnPolicy(clusterTenant);
            try {
                if (Objects.isNull(clusterTenant.getId())) {
                    rangerClient.getPolicies().createPolicy(policy);
                } else {
                    Policy returnPolicy = rangerClient.getPolicies().getPolicyByName("yarndev", clusterTenant.getTenantName());
                    rangerClient.getPolicies().updatePolicy(returnPolicy.getId(), policy);
                }
                log.info("operate yarn policy success");
            } catch (Exception e) {
                log.error("operate yarn policy failed");
                execResult.setExecResult(false);
                execResult.setExecErrOut(e.getMessage());
            }
        }
        rangerClient.stop();
        return execResult;
    }

    private Policy getYarnPolicy(ClusterTenant clusterTenant) {
        List<String> queues = clusterTenant.getYarnResourceList()
                .stream()
                .map(t -> (TenantYarnResource) t)
                .map(TenantYarnResource::getYarnQueueName)
                .collect(Collectors.toList());
        return RangerUtil.simpleYarnPolicy(
                "yarndev",
                clusterTenant.getTenantName(),
                queues,
                Collections.singletonList(clusterTenant.getTenantName())
        );
    }

}
