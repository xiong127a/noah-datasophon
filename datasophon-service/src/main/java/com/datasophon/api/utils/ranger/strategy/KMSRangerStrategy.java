package com.datasophon.api.utils.ranger.strategy;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import com.datasophon.api.utils.ranger.client.RangerUtil;
import com.datasophon.api.utils.ranger.client.model.Policy;
import com.datasophon.api.utils.ranger.client.model.PolicyItem;
import com.datasophon.api.utils.ranger.client.model.PolicyItemAccess;
import com.datasophon.api.utils.ranger.client.model.PolicyResource;
import com.datasophon.api.utils.ranger.client.model.Service;
import com.datasophon.api.utils.ranger.client.model.User;
import com.datasophon.api.utils.ranger.client.utils.RangerClientException;
import com.datasophon.common.model.tenant.resource.TenantKmsResource;
import com.datasophon.common.model.tenant.resource.TenantResource;
import com.datasophon.common.utils.ExecResult;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class KMSRangerStrategy extends AbstractRangerStrategy implements RangerStrategy {

    public KMSRangerStrategy(Integer clusterId) throws Exception {
        super(clusterId);
        logger = LoggerFactory.getLogger("KmsRangerOperateLogger");
    }

    @Override
    public ExecResult createService() {
        try {
            createRangerAdminUserIfNotExists();
            if (isKmsServiceExists()) {
                logger.info("kmsdev service already exists");
                execResult.setExecResult(true);
                RangerUtil.updateDefaultPolicy(rangerKmsClient, "kmsdev");
                logger.info("Config KMS Ranger plugin successfully");
                return execResult;
            }

            String rangerKmsHost = globalVariables.get("${rangerKmsHost}");
            String kmsUrl = "kms://http@" + rangerKmsHost + ":9292/kms";
            Service kmsService = createKmsService("kmsdev", kmsUrl);

            rangerKmsClient.getServices().createService(kmsService);
            RangerUtil.updateDefaultPolicy(rangerKmsClient, "kmsdev");
            logger.info("Config KMS Ranger plugin successfully");
            execResult.setExecResult(true);
        } catch (RangerClientException e) {
            handleRangerClientException(e);
        }
        return execResult;
    }

    private void createRangerAdminUserIfNotExists() throws RangerClientException {
        User rangeradmin = rangerClient.getUsers().getUserByName("rangeradmin");
        if (Objects.isNull(rangeradmin)) {
            logger.info("rangeradmin user does not exist, creating rangeradmin user");
            User user = User.builder()
                    .firstName("rangeradmin")
                    .name("rangeradmin")
                    .password("rangeradmin123")
                    .status(1)
                    .userRoleList(Collections.singletonList("ROLE_SYS_ADMIN"))
                    .build();
            rangerClient.getUsers().createUser(user);
        }
    }

    private boolean isKmsServiceExists() throws RangerClientException {
        Service kmsService = rangerKmsClient.getServices().getServiceByName("kmsdev");
        return Objects.nonNull(kmsService);
    }


    private void handleRangerClientException(RangerClientException e) {
        logger.error("Failed to configure KMS Ranger plugin", e);
        execResult.setExecErrOut(e.getMessage());
    }

    @Override
    public ExecResult operatePolicy(TenantResource resource) {
        execResult.setExecResult(true);
        if (CollUtil.isNotEmpty(resource.getKmsResourceList())) {
            Policy policy = getKmsPolicy(resource);
            try {
//                if (Objects.isNull(resource.getId())) {
                rangerClient.getPolicies().createPolicy(policy);
//                } else {
//                    Policy returnPolicy = rangerClient.getPolicies().getPolicyByName("hadoopdev", resource.getTenantName());
//                    rangerClient.getPolicies().updatePolicy(returnPolicy.getId(), policy);
//                }
                logger.info("operate kms policy success");
            } catch (Exception e) {
                logger.error("operate kms policy failed");
                execResult.setExecResult(false);
                execResult.setExecErrOut(e.getMessage());
            }
        }
        return execResult;
    }

    @Override
    public ExecResult deletePolicy(String policyName) {
        try {
            Policy returnPolicy = rangerClient.getPolicies().getPolicyByName("hadoopdev", policyName);
            rangerClient.getPolicies().deletePolicy(returnPolicy.getId());
            logger.info("delete kms policy {} success", policyName);
            execResult.setExecResult(true);
        } catch (Exception e) {
            logger.error("delete kms policy {} failed", policyName);
            execResult.setExecErrOut(e.getMessage());
        }
        return execResult;
    }

    private Policy getKmsPolicy(TenantResource resource) {
        List<String> keynames = resource.getKmsResourceList()
                .map(TenantKmsResource::getKeyname)
                .collect(Collectors.toList());
        return simpleKmsPolicy(
                "kmsdev",
                resource.getTenantName(),
                keynames,
                Collections.singletonList(resource.getTenantName()));
    }

    public Service createKmsService(String serviceName, String kmsUrl) {
        return Service.builder()
                .name(serviceName)
                .isEnabled(true)
                .type("kms")
                .configs(
                        MapUtil.<String, String>builder()
                                .put("username", "keyadmin")
                                .put("password", "keyadmin")
                                .put("provider", kmsUrl)
                                .build()
                )
                .build();
    }

    public Policy simpleKmsPolicy(String serviceName, String policyName, List<String> keyNameList, List<String> roleList) {
        Map<String, PolicyResource> resources = new HashMap<>();
        PolicyResource policyResource = new PolicyResource();
        policyResource.setIsRecursive(true);
        policyResource.setValues(keyNameList);
        resources.put("keyname", policyResource);

        PolicyItem policyItem = new PolicyItem();
        PolicyItemAccess createAccess = new PolicyItemAccess();
        createAccess.setType("create");
        createAccess.setIsAllowed(true);

        PolicyItemAccess deleteAccess = new PolicyItemAccess();
        deleteAccess.setType("delete");
        deleteAccess.setIsAllowed(true);

        PolicyItemAccess rolloverAccess = new PolicyItemAccess();
        rolloverAccess.setType("rollover");
        rolloverAccess.setIsAllowed(true);

        PolicyItemAccess setKeyMaterialAccess = new PolicyItemAccess();
        setKeyMaterialAccess.setType("setkeymaterial");
        setKeyMaterialAccess.setIsAllowed(true);

        PolicyItemAccess getAccess = new PolicyItemAccess();
        getAccess.setType("get");
        getAccess.setIsAllowed(true);

        PolicyItemAccess getKeysAccess = new PolicyItemAccess();
        getKeysAccess.setType("getkeys");
        getKeysAccess.setIsAllowed(true);

        PolicyItemAccess getMetadataAccess = new PolicyItemAccess();
        getMetadataAccess.setType("getmetadata");
        getMetadataAccess.setIsAllowed(true);

        PolicyItemAccess generateEEKAccess = new PolicyItemAccess();
        generateEEKAccess.setType("generateeek");
        generateEEKAccess.setIsAllowed(true);

        PolicyItemAccess decryptEEKAccess = new PolicyItemAccess();
        decryptEEKAccess.setType("decrypteek");
        decryptEEKAccess.setIsAllowed(true);

        policyItem.getAccesses().add(createAccess);
        policyItem.getAccesses().add(deleteAccess);
        policyItem.getAccesses().add(rolloverAccess);
        policyItem.getAccesses().add(setKeyMaterialAccess);
        policyItem.getAccesses().add(getAccess);
        policyItem.getAccesses().add(getKeysAccess);
        policyItem.getAccesses().add(getMetadataAccess);
        policyItem.getAccesses().add(generateEEKAccess);
        policyItem.getAccesses().add(decryptEEKAccess);
        policyItem.setRoles(roleList);

        Policy policy = new Policy();
        policy.setResources(resources);
        policy.setPolicyItems(Collections.singletonList(policyItem));
        policy.setPolicyType(0);
        policy.setName(policyName);
        policy.setIsEnabled(true);
        policy.setPolicyPriority(1);
        policy.setIsAuditEnabled(true);
        policy.setIsDenyAllElse(true);
        policy.setService(serviceName);
        policy.setDescription("");
        policy.setAllowExceptions(Collections.emptyList());
        policy.setDenyPolicyItems(Collections.emptyList());
        policy.setDenyExceptions(Collections.emptyList());

        return policy;
    }

}
