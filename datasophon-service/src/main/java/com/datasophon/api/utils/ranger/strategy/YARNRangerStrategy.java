package com.datasophon.api.utils.ranger.strategy;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.datasophon.api.utils.ranger.client.RangerUtil;
import com.datasophon.api.utils.ranger.client.model.*;
import com.datasophon.api.utils.ranger.client.utils.RangerClientException;
import com.datasophon.common.model.TenantResource.TenantResource;
import com.datasophon.common.model.TenantResource.TenantYarnResource;
import com.datasophon.common.utils.ExecResult;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class YARNRangerStrategy extends AbstractRangerStrategy implements RangerStrategy {

    public YARNRangerStrategy(Integer clusterId) throws Exception {
        super(clusterId);
        logger = LoggerFactory.getLogger("YarnRangerOperateLogger");
    }

    @Override
    public ExecResult createService() {
        Service yarnService;
        String rm1Addr = "http://" + globalVariables.get("${yarn.resourcemanager.webapp.address.rm1}");
        String rm2Addr = "http://" + globalVariables.get("${yarn.resourcemanager.webapp.address.rm2}");
        try {
            String enableKerberos = globalVariables.get("${enableYARNKerberos}");
            if (StrUtil.isNotEmpty(enableKerberos) && "true".equals(enableKerberos)) {
                yarnService = kerberosYarnService("yarndev", String.join(",", rm1Addr, rm2Addr));
            } else {
                yarnService = simpleYarnService("yarndev", String.join(",", rm1Addr, rm2Addr));
            }
            rangerClient.getServices().createService(yarnService);
            RangerUtil.updateDefaultPolicy(rangerClient, "yarndev");
            logger.info("config yarn ranger plugin success");
            execResult.setExecResult(true);
        } catch (RangerClientException e) {
            logger.error("config yarn ranger plugin failed");
            logger.error(e.getMessage());
            execResult.setExecErrOut(e.getMessage());
        }
        return execResult;
    }

    @Override
    public ExecResult operatePolicy(TenantResource resource) {
        execResult.setExecResult(true);
        if (CollUtil.isNotEmpty(resource.getYarnResourceList())) {
            Policy policy = getYarnPolicy(resource);
            try {
//                if (Objects.isNull(resource.getId())) {
                rangerClient.getPolicies().createPolicy(policy);
//                } else {
//                    Policy returnPolicy = rangerClient.getPolicies().getPolicyByName("yarndev", resource.getTenantName());
//                    rangerClient.getPolicies().updatePolicy(returnPolicy.getId(), policy);
//                }
                logger.info("operate yarn policy success");
            } catch (Exception e) {
                logger.error("operate yarn policy failed");
                execResult.setExecResult(false);
                execResult.setExecErrOut(e.getMessage());
            }
        }
        return execResult;
    }

    @Override
    public ExecResult deletePolicy(String policyName) {
        try {
            Policy returnPolicy = rangerClient.getPolicies().getPolicyByName("yarndev", policyName);
            rangerClient.getPolicies().deletePolicy(returnPolicy.getId());
            logger.info("delete yarn policy {} success", policyName);
            execResult.setExecResult(true);
        } catch (Exception e) {
            logger.error("delete yarn policy {} failed", policyName);
            execResult.setExecErrOut(e.getMessage());
        }
        return execResult;
    }

    private Policy getYarnPolicy(TenantResource resource) {
        List<String> queues = resource.getYarnResourceList()
                .stream()
                .map(t -> t)
                .map(t -> t.getParentQueueName() + "." + t.getQueueName())
                .collect(Collectors.toList());
        return simpleYarnPolicy(
                "yarndev",
                resource.getTenantName(),
                queues,
                Collections.singletonList(resource.getTenantName())
        );
    }

    /**
     * 当前队列列表仅对给定用户有操作权限，拒绝其它所有用户
     */
    public Policy simpleYarnPolicy(String serviceName, String policyName, List<String> queueList, List<String> roleList) {
        Map<String, PolicyResource> resources = new HashMap<>();
        PolicyResource policyResource = new PolicyResource();
        policyResource.setIsExcludes(false);
        policyResource.setIsRecursive(true);
        policyResource.setValues(queueList);
        resources.put("queue", policyResource);

        PolicyItem policyItem = new PolicyItem();
        PolicyItemAccess submitAccess = new PolicyItemAccess();
        submitAccess.setType("submit-app");
        submitAccess.setIsAllowed(true);
        PolicyItemAccess adminAccess = new PolicyItemAccess();
        adminAccess.setType("admin-queue");
        adminAccess.setIsAllowed(true);
        policyItem.getAccesses().add(submitAccess);
        policyItem.getAccesses().add(adminAccess);
        policyItem.setRoles(roleList);

        Policy policy = new Policy();
        policy.setPolicyType(0);
        policy.setName(policyName);
        policy.setIsEnabled(true);
        policy.setIsAuditEnabled(true);
        policy.setResources(resources);
        policy.setIsDenyAllElse(true);
        policy.setPolicyItems(Collections.singletonList(policyItem));
        policy.setService(serviceName);
        policy.setPolicyPriority(1);

        return policy;
    }

    public Service simpleYarnService(String serviceName, String yarnUrl) {
        return Service.builder()
                .name(serviceName)
                .isEnabled(true)
                .type("yarn")
                .configs(
                        MapUtil.<String, String>builder()
                                .put("hadoop.security.authentication", "simple")
                                .put("yarn.url", yarnUrl)
                                .put("username", "yarn")
                                .put("password", "yarn")
                                .put("commonNameForCertificate", "")
                                .build()
                )
                .build();
    }

    public Service kerberosYarnService(String serviceName, String yarnUrl) {
        return Service.builder()
                .name(serviceName)
                .isEnabled(true)
                .type("yarn")
                .configs(
                        MapUtil.<String, String>builder()
                                .put("username", "yarn")
                                .put("password", "yarn")
                                .put("yarn.url", yarnUrl)
                                .put("hadoop.security.authentication", "kerberos")
                                .put("commonNameForCertificate", "")
                                .put("yarn.nodemanager.principal", globalVariables.get("${yarn.nodemanager.principal}"))
                                .put("policy.download.auth.users", "yarn")
                                .put("yarn.resourcemanager.principal", globalVariables.get("${yarn.resourcemanager.principal}"))
                                .build()
                )
                .build();
    }

}
