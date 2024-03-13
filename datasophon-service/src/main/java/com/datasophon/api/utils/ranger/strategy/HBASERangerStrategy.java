package com.datasophon.api.utils.ranger.strategy;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import com.datasophon.api.utils.ranger.client.RangerUtil;
import com.datasophon.api.utils.ranger.client.model.*;
import com.datasophon.api.utils.ranger.client.utils.RangerClientException;
import com.datasophon.common.model.TenantResource.TenantHbaseResource;
import com.datasophon.common.model.TenantResource.TenantResource;
import com.datasophon.common.utils.ExecResult;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class HBASERangerStrategy extends AbstractRangerStrategy implements RangerStrategy {

    public HBASERangerStrategy(Integer clusterId) throws Exception {
        super(clusterId);
        logger = LoggerFactory.getLogger("HbaseRangerOperateLogger");
    }

    @Override
    public ExecResult createService() throws Exception {
        String zkUrl = globalVariables.get("${zkUrls}");
        String zkPort = globalVariables.get("${clientPort}");
        String hbaseRootDir = globalVariables.get("${hbase.rootdir}");

        try {
            rangerClient.getServices()
                    .createService(simpleHbaseService("hbasedev", zkUrl, zkPort, hbaseRootDir));
            RangerUtil.updateDefaultPolicy(rangerClient, "hbasedev");
            logger.info("config hbase ranger plugin success");
            execResult.setExecResult(true);
        } catch (RangerClientException e) {
            logger.error("config hbase ranger plugin failed");
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
            Policy policy = getHbasePolicy(resource);
            try {
                if (Objects.isNull(resource.getId())) {
                    rangerClient.getPolicies().createPolicy(policy);
                } else {
                    Policy returnPolicy = rangerClient.getPolicies().getPolicyByName("hbasedev", resource.getTenantName());
                    rangerClient.getPolicies().updatePolicy(returnPolicy.getId(), policy);
                }
                logger.info("operate hbase policy success");
            } catch (Exception e) {
                logger.error("operate hbase policy failed");
                execResult.setExecResult(false);
                execResult.setExecErrOut(e.getMessage());
            }
        }
        rangerClient.stop();
        return execResult;
    }

    private Policy getHbasePolicy(TenantResource resource) {
        List<String> hbaseNamespaces = resource.getHbaseResourceList()
                .stream()
                .map(t -> (TenantHbaseResource) t)
                .map(t -> t.getHbaseNamespace() + ":*")
                .collect(Collectors.toList());
        return simpleHbasePolicy(
                "hbasedev",
                resource.getTenantName(),
                hbaseNamespaces,
                Collections.singletonList(resource.getTenantName())
        );
    }

    public Service simpleHbaseService(String serviceName, String zkUrl, String zkPort, String hbaseZNode) {
        return Service.builder()
                .name(serviceName)
                .isEnabled(true)
                .type("hbase")
                .configs(
                        MapUtil.<String, String>builder()
                                .put("username", "hbase")
                                .put("password", "hbase")
                                .put("hadoop.security.authentication", "simple")
                                .put("hbase.master.kerberos.principal", "")
                                .put("hbase.security.authentication", "simple")
                                .put("hbase.zookeeper.property.clientPort", zkPort)
                                .put("hbase.zookeeper.quorum", zkUrl)
                                .put("zookeeper.znode.parent", hbaseZNode)
                                .put("commonNameForCertificate", "")
                                .build()
                )
                .build();
    }

    public Policy simpleHbasePolicy(String serviceName, String policyName, List<String> tableList, List<String> roleList) {
        Map<String, PolicyResource> resources = new HashMap<>();
        PolicyResource tablePolicy = new PolicyResource();
        tablePolicy.setValues(tableList);
        tablePolicy.setIsRecursive(false);
        tablePolicy.setIsExcludes(false);
        PolicyResource columnFamilyPolicy = new PolicyResource();
        columnFamilyPolicy.setValues(Collections.singletonList("*"));
        columnFamilyPolicy.setIsRecursive(false);
        columnFamilyPolicy.setIsExcludes(false);
        PolicyResource columnPolicy = new PolicyResource();
        columnPolicy.setValues(Collections.singletonList("*"));
        columnPolicy.setIsRecursive(false);
        columnPolicy.setIsExcludes(false);
        resources.put("table", tablePolicy);
        resources.put("column-family", columnFamilyPolicy);
        resources.put("column", columnPolicy);

        PolicyItem policyItem = new PolicyItem();
        List<String> accesses = Arrays.asList("read", "write", "create", "admin", "execute");
        for (String access : accesses) {
            PolicyItemAccess policyItemAccess = new PolicyItemAccess();
            policyItemAccess.setType(access);
            policyItemAccess.setIsAllowed(true);
            policyItem.getAccesses().add(policyItemAccess);
        }
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
