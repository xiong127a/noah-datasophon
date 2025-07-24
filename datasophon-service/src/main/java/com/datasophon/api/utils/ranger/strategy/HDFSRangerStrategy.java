package com.datasophon.api.utils.ranger.strategy;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.datasophon.api.load.GlobalVariables;
import com.datasophon.api.utils.ranger.client.RangerUtil;
import com.datasophon.api.utils.ranger.client.model.*;
import com.datasophon.api.utils.ranger.client.utils.RangerClientException;
import com.datasophon.common.model.TenantResource.TenantHdfsResource;
import com.datasophon.common.model.TenantResource.TenantResource;
import com.datasophon.common.utils.ExecResult;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class HDFSRangerStrategy extends AbstractRangerStrategy implements RangerStrategy {

    public HDFSRangerStrategy(Integer clusterId) throws Exception {
        super(clusterId);
        logger = LoggerFactory.getLogger("HdfsRangerOperateLogger");
    }

    @Override
    public ExecResult createService() {
        Service hadoopService;
        String nn1Add = "hdfs://" + globalVariables.get("${dfs.namenode.rpc-address.nameservice1.nn1}");
        String nn2Add = "hdfs://" + globalVariables.get("${dfs.namenode.rpc-address.nameservice1.nn2}");
        try {
            String enableKerberos = globalVariables.get("${enableHDFSKerberos}");
            if (StrUtil.isNotEmpty(enableKerberos) && "true".equals(enableKerberos)) {
                hadoopService = kerberosHdfsService("hadoopdev", String.join(",", nn1Add, nn2Add));
            } else {
                hadoopService = simpleHdfsService("hadoopdev", String.join(",", nn1Add, nn2Add));
            }
            rangerClient.getServices().createService(hadoopService);
            RangerUtil.updateDefaultPolicy(rangerClient, "hadoopdev");
            logger.info("config hdfs ranger plugin success");
            execResult.setExecResult(true);
        } catch (RangerClientException e) {
            logger.error("config hdfs ranger plugin failed");
            logger.error(e.getMessage());
            execResult.setExecErrOut(e.getMessage());
        }
        return execResult;
    }

    @Override
    public ExecResult operatePolicy(TenantResource resource) {
        execResult.setExecResult(true);
        if (CollUtil.isNotEmpty(resource.getHdfsResourceList())) {
            Policy policy = getHdfsPolicy(resource);
            try {
//                if (Objects.isNull(resource.getId())) {
                rangerClient.getPolicies().createPolicy(policy);
//                } else {
//                    Policy returnPolicy = rangerClient.getPolicies().getPolicyByName("hadoopdev", resource.getTenantName());
//                    rangerClient.getPolicies().updatePolicy(returnPolicy.getId(), policy);
//                }
                logger.info("operate hdfs policy success");
            } catch (Exception e) {
                logger.error("operate hdfs policy failed");
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
            logger.info("delete hdfs policy {} success", policyName);
            execResult.setExecResult(true);
        } catch (Exception e) {
            logger.error("delete hdfs policy {} failed", policyName);
            execResult.setExecErrOut(e.getMessage());
        }
        return execResult;
    }

    private Policy getHdfsPolicy(TenantResource resource) {
        List<String> hdfsPaths = resource.getHdfsResourceList()
                .stream()
                .map(TenantHdfsResource::getHdfsPath)
                .collect(Collectors.toList());
        return simpleHdfsPolicy(
                "hadoopdev",
                resource.getTenantName(),
                hdfsPaths,
                Collections.singletonList(resource.getTenantName()));
    }

    public Service simpleHdfsService(String serviceName, String hdfsUrl) {
        return Service.builder()
                .name(serviceName)
                .isEnabled(true)
                .type("hdfs")
                .configs(
                        MapUtil.<String, String>builder()
                                .put("username", "hdfs")
                                .put("password", "hdfs")
                                .put("fs.default.name", hdfsUrl)
                                .put("hadoop.security.authorization", "false")
                                .put("hadoop.security.authentication", "simple")
                                .put("hadoop.security.auth_to_local", "")
                                .put("dfs.datanode.kerberos.principal", "")
                                .put("dfs.namenode.kerberos.principal", "")
                                .put("dfs.secondary.namenode.kerberos.principal", "")
                                .put("hadoop.rpc.protection", "authentication")
                                .put("commonNameForCertificate", "")
                                .build()
                )
                .build();
    }

    public Service kerberosHdfsService(String serviceName, String hdfsUrl) {
        return Service.builder()
                .name(serviceName)
                .isEnabled(true)
                .type("hdfs")
                .configs(
                        MapUtil.<String, String>builder()
                                .put("username", "hdfs")
                                .put("password", "hdfs")
                                .put("fs.default.name", hdfsUrl)
                                .put("hadoop.security.authorization", "true")
                                .put("hadoop.security.authentication", "kerberos")
                                .put("hadoop.security.auth_to_local", "")
                                .put("dfs.datanode.kerberos.principal", globalVariables.get("${dfs.datanode.kerberos.principal}"))
                                .put("dfs.namenode.kerberos.principal", globalVariables.get("${dfs.namenode.kerberos.principal}"))
                                .put("dfs.secondary.namenode.kerberos.principal", "")
                                .put("hadoop.rpc.protection", "authentication")
                                .put("commonNameForCertificate", "")
                                .put("policy.download.auth.users", "hdfs")
                                .put("dfs.journalnode.kerberos.principal", globalVariables.get("${dfs.journalnode.kerberos.principal}"))
                                .build()
                )
                .build();
    }

    public Policy simpleHdfsPolicy(String serviceName, String policyName, List<String> pathList, List<String> roleList) {
        Map<String, PolicyResource> resources = new HashMap<>();
        PolicyResource policyResource = new PolicyResource();
        policyResource.setIsRecursive(true);
        policyResource.setValues(pathList);
        resources.put("path", policyResource);

        PolicyItem policyItem = new PolicyItem();
        PolicyItemAccess readAccess = new PolicyItemAccess();
        readAccess.setType("read");
        readAccess.setIsAllowed(true);
        PolicyItemAccess writeAccess = new PolicyItemAccess();
        writeAccess.setType("write");
        writeAccess.setIsAllowed(true);
        PolicyItemAccess executeAccess = new PolicyItemAccess();
        executeAccess.setType("execute");
        executeAccess.setIsAllowed(true);
        policyItem.getAccesses().add(readAccess);
        policyItem.getAccesses().add(writeAccess);
        policyItem.getAccesses().add(executeAccess);
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
