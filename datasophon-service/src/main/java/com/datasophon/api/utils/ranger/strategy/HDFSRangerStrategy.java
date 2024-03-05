package com.datasophon.api.utils.ranger.strategy;

import cn.hutool.core.collection.CollUtil;
import com.datasophon.api.utils.ranger.client.RangerClient;
import com.datasophon.api.utils.ranger.client.RangerUtil;
import com.datasophon.api.utils.ranger.client.model.Policy;
import com.datasophon.api.utils.ranger.client.utils.RangerClientException;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.dao.entity.ClusterTenant;
import com.datasophon.dao.entity.tenantResource.TenantHdfsResource;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
public class HDFSRangerStrategy extends AbstractRangerStrategy implements RangerStrategy {

    public HDFSRangerStrategy(Integer clusterId) throws Exception {
        super(clusterId);
    }

    @Override
    public ExecResult createService() throws Exception {
        String nn1Add = "hdfs://" + globalVariables.get("${dfs.namenode.rpc-address.nameservice1.nn1}");
        String nn2Add = "hdfs://" + globalVariables.get("${dfs.namenode.rpc-address.nameservice1.nn2}");
        try {
            rangerClient.getServices()
                    .createService(RangerUtil.simpleHdfsService("hadoopdev", String.join(",", nn1Add, nn2Add)));
            RangerUtil.updateDefaultPolicy(rangerClient, "hadoopdev");
            log.info("config hdfs ranger plugin success");
            execResult.setExecResult(true);
        } catch (RangerClientException e) {
            log.error("config hdfs ranger plugin failed");
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
            Policy policy = getHdfsPolicy(clusterTenant);
            try {
                if (Objects.isNull(clusterTenant.getId())) {
                    rangerClient.getPolicies().createPolicy(policy
                    );
                } else {
                    Policy returnPolicy = rangerClient.getPolicies().getPolicyByName("hadoopdev", clusterTenant.getTenantName());
                    rangerClient.getPolicies().updatePolicy(returnPolicy.getId(), policy);
                }
                log.info("operate hdfs policy success");
            } catch (Exception e) {
                log.error("operate hdfs policy failed");
                execResult.setExecResult(false);
                execResult.setExecErrOut(e.getMessage());
            }
        }
        rangerClient.stop();
        return execResult;
    }

    private Policy getHdfsPolicy(ClusterTenant clusterTenant) {
        List<String> hdfsPaths = clusterTenant.getHdfsResourceList()
                .stream()
                .map(t -> (TenantHdfsResource) t)
                .map(TenantHdfsResource::getHdfsPath)
                .collect(Collectors.toList());
        return RangerUtil.simpleHdfsPolicy(
                "hadoopdev",
                clusterTenant.getTenantName(),
                hdfsPaths,
                Collections.singletonList(clusterTenant.getTenantName()));
    }

}
