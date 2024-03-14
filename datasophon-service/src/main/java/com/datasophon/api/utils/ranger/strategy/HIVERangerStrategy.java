package com.datasophon.api.utils.ranger.strategy;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import com.datasophon.api.utils.ranger.client.RangerUtil;
import com.datasophon.api.utils.ranger.client.model.*;
import com.datasophon.api.utils.ranger.client.utils.RangerClientException;
import com.datasophon.common.model.TenantResource.TenantHiveResource;
import com.datasophon.common.model.TenantResource.TenantResource;
import com.datasophon.common.utils.ExecResult;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class HIVERangerStrategy extends AbstractRangerStrategy implements RangerStrategy {

    public HIVERangerStrategy(Integer clusterId) throws Exception {
        super(clusterId);
        logger = LoggerFactory.getLogger("HiveRangerOperateLogger");
    }

    @Override
    public ExecResult createService() throws Exception {
        String hiveServer2Host = globalVariables.get("${masterHiveServer2}");
        String hiveServer2Port = globalVariables.get("${hive.server2.thrift.port}");
        String hiveUrl = "jdbc:hive2://" + hiveServer2Host + ":" + hiveServer2Port;

        try {
            rangerClient.getServices()
                    .createService(simpleHiveService("hivedev", hiveUrl));
            RangerUtil.updateDefaultPolicy(rangerClient, "hivedev");
            logger.info("config hive ranger plugin success");
            execResult.setExecResult(true);
        } catch (RangerClientException e) {
            logger.error("config hive ranger plugin failed");
            logger.error(e.getMessage());
            execResult.setExecErrOut(e.getMessage());
        }
        rangerClient.stop();
        return execResult;
    }

    @Override
    public ExecResult operatePolicy(TenantResource resource) throws Exception {
        execResult.setExecResult(true);
        if (CollUtil.isNotEmpty(resource.getHdfsResourceList())) {
            Policy policy = getHivePolicy(resource);
            try {
                if (Objects.isNull(resource.getId())) {
                    rangerClient.getPolicies().createPolicy(policy);
                } else {
                    Policy returnPolicy = rangerClient.getPolicies().getPolicyByName("hivedev", resource.getTenantName());
                    rangerClient.getPolicies().updatePolicy(returnPolicy.getId(), policy);
                }
                logger.info("operate hive policy success");
            } catch (Exception e) {
                logger.error("operate hive policy failed");
                execResult.setExecResult(false);
                execResult.setExecErrOut(e.getMessage());
            }
        }
        rangerClient.stop();
        return execResult;
    }

    @Override
    public ExecResult deletePolicy(String policyName) throws Exception {
        try {
            Policy returnPolicy = rangerClient.getPolicies().getPolicyByName("hivedev", policyName);
            rangerClient.getPolicies().deletePolicy(returnPolicy.getId());
            logger.info("delete hive policy {} success", policyName);
            execResult.setExecResult(true);
        } catch (Exception e) {
            logger.error("delete hive policy {} failed", policyName);
            execResult.setExecErrOut(e.getMessage());
        }
        rangerClient.stop();
        return execResult;
    }

    private Policy getHivePolicy(TenantResource resource) {
        List<String> hiveDatabases = resource.getHiveResourceList()
                .stream()
                .map(t -> (TenantHiveResource) t)
                .map(TenantHiveResource::getHiveDatabase)
                .collect(Collectors.toList());
        return simpleHivePolicyForDatabase(
                "hivedev",
                resource.getTenantName(),
                hiveDatabases,
                Collections.singletonList(resource.getTenantName())
        );
    }

    public Service simpleHiveService(String serviceName, String hiveUrl) {
        return Service.builder()
                .name(serviceName)
                .isEnabled(true)
                .type("hive")
                .configs(
                        MapUtil.<String, String>builder()
                                .put("username", "hive")
                                .put("password", "hive")
                                .put("jdbc.driverClassName", "org.apache.hive.jdbc.HiveDriver")
                                .put("jdbc.url", hiveUrl)
                                .put("commonNameForCertificate", "")
                                .build()
                )
                .build();
    }

    public Policy simpleHivePolicyForDatabase(String serviceName, String policyName, List<String> databaseList, List<String> roleList) {
        Map<String, PolicyResource> resources = new HashMap<>();
        PolicyResource policyResource = new PolicyResource();
        policyResource.setValues(databaseList);
        policyResource.setIsRecursive(false);
        policyResource.setIsExcludes(false);
        resources.put("database", policyResource);

        PolicyItem policyItem = new PolicyItem();
        PolicyItemAccess policyItemAccess = new PolicyItemAccess();
        policyItemAccess.setType("all");
        policyItemAccess.setIsAllowed(true);
        policyItem.getAccesses().add(policyItemAccess);
        policyItem.setRoles(roleList);

        Policy policy = new Policy();
        policy.setIsDenyAllElse(true);
        policy.setPolicyType(0);
        policy.setName(policyName);
        policy.setIsEnabled(true);
        policy.setIsAuditEnabled(true);
        policy.setResources(resources);
        policy.setPolicyItems(Collections.singletonList(policyItem));
        policy.setService(serviceName);
        policy.setPolicyPriority(1);

        return policy;
    }

}
