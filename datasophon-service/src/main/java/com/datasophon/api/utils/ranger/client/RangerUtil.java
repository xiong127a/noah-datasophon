package com.datasophon.api.utils.ranger.client;

import cn.hutool.cache.Cache;
import cn.hutool.cache.CacheUtil;
import cn.hutool.core.collection.CollUtil;
import com.datasophon.api.load.GlobalVariables;
import com.datasophon.api.utils.ranger.client.config.RangerAuthConfig;
import com.datasophon.api.utils.ranger.client.config.RangerClientConfig;
import com.datasophon.api.utils.ranger.client.model.Policy;
import com.datasophon.api.utils.ranger.client.model.PolicyItem;
import com.datasophon.api.utils.ranger.client.model.PolicyItemAccess;
import com.datasophon.api.utils.ranger.client.model.Role;
import com.datasophon.api.utils.ranger.client.model.RoleMember;
import com.datasophon.api.utils.ranger.client.utils.RangerClientException;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
public class RangerUtil {

    private final static String SUPER_USER = "admin";
    private final static String SUPER_ROLE_NAME = "admin";

    // 创建一个缓存对象
    private static final Cache<Integer, RangerClient> clientAdminCache = CacheUtil.newTimedCache(60 * 1000); // 设置缓存有效期为1分钟
    private static final Cache<Integer, RangerClient> clientKmsCache = CacheUtil.newTimedCache(60 * 1000); // 设置缓存有效期为1分钟

    public static void updateDefaultPolicy(RangerClient rangerClient, String serviceName) {
        List<String> accessTypeList;
        String policyName;
        PolicyItem policyItem = new PolicyItem();
        policyItem.setRoles(Collections.singletonList(SUPER_ROLE_NAME));
        switch (serviceName) {
            case "hadoopdev":
                accessTypeList = Arrays.asList("read", "write", "execute");
                policyName = "all - path";
                break;
            case "hivedev":
                accessTypeList = Arrays.asList("select", "update", "create", "drop", "alter", "index", "lock", "all",
                        "read", "write", "repladmin", "serviceadmin", "tempudfadmin", "refresh");
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
            case "kmsdev":
                accessTypeList = Arrays.asList("create", "delete", "rollover", "setkeymaterial", "get", "getkeys",
                        "getmetadata", "generateeek", "decrypteek");
                policyName = "all - keyname";
                policyItem.setUsers(CollUtil.newHashSet("keyadmin", "rangeradmin"));
                policyItem.setRoles(null);
                break;
            default:
                return;
        }

        Policy defaultPolicy = rangerClient.getPolicies().getPolicyByName(serviceName, policyName);
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
        Role roleByName = rangerClient.getRoles().getRoleByName(SUPER_ROLE_NAME);
        if (roleByName != null) {
            return;
        }

        Role role = getRole();
        try {
            rangerClient.getRoles().createRole(role);
            log.info("create ranger super role success");
        } catch (Exception e) {
            log.error("create ranger super role failed", e);
        }
    }

    private static Role getRole() {
        Role role = new Role();
        role.setName(SUPER_ROLE_NAME);
        List<RoleMember> defaultRoleMembers = new ArrayList<>();
        List<String> defaultUsers = Arrays.asList("root", "hdfs", "yarn", "hive", "hbase", "mapred", "admin", "elastic",
                "hue");
        for (String defaultUser : defaultUsers) {
            RoleMember roleMember = new RoleMember();
            roleMember.setName(defaultUser);
            roleMember.setIsAdmin(false);
            defaultRoleMembers.add(roleMember);
        }
        role.setUsers(defaultRoleMembers);
        return role;
    }

    public static void setRoleUser(RangerClient rangerClient, String roleName, List<String> userList)
            throws RangerClientException {
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

    public static RangerClient getRangerClient(Integer clusterTenant) throws Exception {
        return getCachedOrNewClient(clusterTenant, "admin", clientAdminCache);
    }

    public static RangerClient getRangerKmsClient(Integer clusterTenant) throws Exception {
        return getCachedOrNewClient(clusterTenant, "keyadmin", clientKmsCache);
    }

    private static RangerClient getCachedOrNewClient(Integer clusterTenant, String username,
                                                     Cache<Integer, RangerClient> clientCache) throws Exception {
        if (clusterTenant == null) {
            throw new IllegalArgumentException("Cluster tenant cannot be null");
        }

        RangerClient cachedClient = clientCache.get(clusterTenant);
        if (cachedClient != null) {
            return cachedClient;
        }

        Map<String, String> globalVariables = GlobalVariables.get(clusterTenant);
        if (globalVariables == null) {
            throw new IllegalStateException("Global variables not found for cluster tenant: " + clusterTenant);
        }

        String rangerAdminUrl = globalVariables.get("${rangerAdminUrl}");
        if (rangerAdminUrl == null || rangerAdminUrl.isEmpty()) {
            throw new IllegalStateException("Ranger admin URL not found for cluster tenant: " + clusterTenant);
        }

        RangerClientConfig clientConfig = RangerClientConfig.builder()
                .connectTimeoutMillis(1000)
                .readTimeoutMillis(1000)
                .loggingLevel("INFO")
                .authConfig(RangerAuthConfig.builder()
                        .username(username)
                        .password("admin123")
                        .build())
                .url(rangerAdminUrl)
                .build();

        RangerClient rangerClient = new RangerClient(clientConfig);
        try {
            rangerClient.start();
        } catch (Exception e) {
            // Ensure resources are closed in case of failure
            rangerClient.stop();
            throw new RuntimeException("Failed to start RangerClient for cluster tenant: " + clusterTenant, e);
        }

        clientCache.put(clusterTenant, rangerClient);

        return rangerClient;
    }
}
