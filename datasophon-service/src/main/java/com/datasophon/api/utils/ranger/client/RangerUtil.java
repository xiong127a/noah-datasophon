package com.datasophon.api.utils.ranger.client;

import cn.hutool.core.map.MapUtil;
import com.datasophon.api.utils.ranger.client.model.*;
import com.datasophon.api.utils.ranger.client.utils.RangerClientException;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class RangerUtil {

    private final static String SUPER_USER = "admin";

    private final static String SUPER_ROLE_NAME = "admin";

    public static Service simpleYarnService(String serviceName, String yarnUrl) {
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

    public static Service simpleHdfsService(String serviceName, String hdfsUrl) {
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

    public static Service simpleHiveService(String serviceName, String hiveUrl) {
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

    public static Service simpleHbaseService(String serviceName, String zkUrl, String zkPort, String hbaseZNode) {
        return Service.builder()
                .name("hbasedev")
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

    /**
     * 当前队列列表仅对给定用户有操作权限，拒绝其它所有用户
     */
    public static Policy simpleYarnPolicy(String serviceName, String policyName, List<String> queueList, List<String> userList) {
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
        policyItem.setUsers(new HashSet<>(userList));

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

    public static Policy simpleHdfsPolicy(String serviceName, String policyName, List<String> pathList, List<String> roleList) {
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

    public static Policy simpleHivePolicyForDatabase(String serviceName, String policyName, List<String> databaseList, List<String> roleList) {
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

    public static Policy simpleHbasePolicy(String serviceName, String policyName, List<String> tableList, List<String> roleList) {
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

    public static void updateDefaultPolicy(RangerClient rangerClient, String serviceName) {
        List<String> accessTypeList = new ArrayList<>();
        String policyName = "";
        switch (serviceName) {
            case "hadoopdev":
                accessTypeList = Arrays.asList("read", "write", "execute");
                policyName = "all - path";
                break;
            case "hivedev":
                accessTypeList = Arrays.asList("select", "update", "create", "drop", "alter", "index", "lock", "all", "read", "write", "repladmin", "serviceadmin", "tempudfadmin", "refresh");
                policyName = "all - database";
                break;
            case "yarndev":
                accessTypeList = Arrays.asList("submit-app", "admin-queue");
                policyName = "all - queue";
                break;
            case "hbasedev":
                accessTypeList = Arrays.asList("read", "write", "create", "admin", "execute");
                policyName = "all - table, column-family, column";
                break;
            default:
                return;
        }

        Policy defaultPolicy = rangerClient.getPolicies().getPolicyByName(serviceName, policyName);
        PolicyItem policyItem = new PolicyItem();
        policyItem.setRoles(Collections.singletonList(SUPER_ROLE_NAME));
        policyItem.setDelegateAdmin(true);
        List<PolicyItemAccess> policyItemAccesses = new ArrayList<>();
        for (String access : accessTypeList) {
            PolicyItemAccess policyItemAccess = new PolicyItemAccess();
            policyItemAccess.setIsAllowed(true);
            policyItemAccess.setType(access);
            policyItemAccesses.add(policyItemAccess);
        }
        policyItem.setAccesses(policyItemAccesses);
        defaultPolicy.setPolicyItems(Collections.singletonList(policyItem));
        defaultPolicy.setIsDenyAllElse(true);
        rangerClient.getPolicies().updatePolicy(defaultPolicy.getId(), defaultPolicy);
    }

    /**
     * 创建ranger默认超级用户组并添加系统内置用户
     */
    public static void createSuperRole(RangerClient rangerClient) {
        Role role = new Role();
        role.setName(SUPER_ROLE_NAME);
        List<RoleMember> defaultRoleMembers = new ArrayList<>();
        List<String> defaultUsers = Arrays.asList("hdfs", "yarn", "hive", "hbase", "mapred");
        for (String defaultUser : defaultUsers) {
            RoleMember roleMember = new RoleMember();
            roleMember.setName(defaultUser);
            roleMember.setIsAdmin(false);
            defaultRoleMembers.add(roleMember);
        }
        role.setUsers(defaultRoleMembers);
        try {
            rangerClient.getRoles().createRole(role);
            log.info("create ranger super role success");
        } catch (Exception e) {
            log.error("create ranger super role failed");
            log.error(e.getMessage());
        }
    }

    public static void setRoleUser(RangerClient rangerClient, String roleName, List<String> userList) throws RangerClientException {
        Role role = rangerClient.getRoles().getRoleByName(roleName);
        List<RoleMember> defaultRoleMembers = new ArrayList<>();
        for (String user : userList) {
            RoleMember roleMember = new RoleMember();
            roleMember.setName(user);
            roleMember.setIsAdmin(false);
            defaultRoleMembers.add(roleMember);
        }
        role.setUsers(defaultRoleMembers);
        rangerClient.getRoles().addUserAndGroups(role.getId(), role);
    }

}
