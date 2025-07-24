package com.datasophon.api.utils.ranger.strategy;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
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
    public ExecResult createService() {
        Service hbaseService;
        String zkUrl = globalVariables.get("${zkUrls}");
        String zkPort = globalVariables.get("${clientPort}");
        String hbaseRootDir = globalVariables.get("${hbase.rootdir}");
        try {
            String enableKerberos = globalVariables.get("${enableHBASEKerberos}");
            if (StrUtil.isNotEmpty(enableKerberos) && "true".equals(enableKerberos)) {
                hbaseService = kerberosHbaseService("hbasedev", zkUrl, zkPort, hbaseRootDir);
            } else {
                hbaseService = simpleHbaseService("hbasedev", zkUrl, zkPort, hbaseRootDir);
            }
            rangerClient.getServices().createService(hbaseService);
            RangerUtil.updateDefaultPolicy(rangerClient, "hbasedev");
            logger.info("config hbase ranger plugin success");
            execResult.setExecResult(true);
        } catch (RangerClientException e) {
            logger.error("config hbase ranger plugin failed");
            logger.error(e.getMessage());
            execResult.setExecErrOut(e.getMessage());
        }
        return execResult;
    }

    @Override
    public ExecResult operatePolicy(TenantResource resource) {
        execResult.setExecResult(true);
        if (CollUtil.isNotEmpty(resource.getHbaseResourceList())) {
            Policy policy = getHbasePolicy(resource);
            try {
//                if (Objects.isNull(resource.getId())) {
                rangerClient.getPolicies().createPolicy(policy);
//                } else {
//                    Policy returnPolicy = rangerClient.getPolicies().getPolicyByName("hbasedev", resource.getTenantName());
//                    rangerClient.getPolicies().updatePolicy(returnPolicy.getId(), policy);
//                }
                logger.info("operate hbase policy success");
            } catch (Exception e) {
                logger.error("operate hbase policy failed");
                execResult.setExecResult(false);
                execResult.setExecErrOut(e.getMessage());
            }
        }
        return execResult;
    }

    @Override
    public ExecResult deletePolicy(String policyName) {
        try {
            Policy returnPolicy = rangerClient.getPolicies().getPolicyByName("hbasedev", policyName);
            rangerClient.getPolicies().deletePolicy(returnPolicy.getId());
            logger.info("delete hbase policy {} success", policyName);
            execResult.setExecResult(true);
        } catch (Exception e) {
            logger.error("delete hbase policy {} failed", policyName);
            execResult.setExecErrOut(e.getMessage());
        }
        return execResult;
    }

    private Policy getHbasePolicy(TenantResource resource) {
        List<String> hbaseNamespaces = resource.getHbaseResourceList()
                .stream()
                .map(t -> t)
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

    public Service kerberosHbaseService(String serviceName, String zkUrl, String zkPort, String hbaseZNode) {
        return Service.builder()
                .name(serviceName)
                .isEnabled(true)
                .type("hbase")
                .configs(
                        MapUtil.<String, String>builder()
                                .put("username", "hbase")
                                .put("password", "hbase")
                                .put("hadoop.security.authentication", "kerberos")
                                .put("hbase.master.kerberos.principal", globalVariables.get("${hbase.master.kerberos.principal}"))
                                .put("hbase.security.authentication", "kerberos")
                                .put("hbase.zookeeper.property.clientPort", zkPort)
                                .put("hbase.zookeeper.quorum", zkUrl)
                                .put("zookeeper.znode.parent", hbaseZNode)
                                .put("commonNameForCertificate", "")
                                .put("policy.download.auth.users", "hbase")
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
