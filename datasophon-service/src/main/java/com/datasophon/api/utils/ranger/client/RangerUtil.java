package com.datasophon.api.utils.ranger.client;

import cn.hutool.cache.Cache;
import cn.hutool.cache.CacheUtil;
import cn.hutool.core.map.MapUtil;
import com.datasophon.api.load.GlobalVariables;
import com.datasophon.api.utils.ranger.client.config.RangerAuthConfig;
import com.datasophon.api.utils.ranger.client.config.RangerClientConfig;
import com.datasophon.api.utils.ranger.client.model.*;
import com.datasophon.api.utils.ranger.client.utils.RangerClientException;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class RangerUtil {

    private final static String SUPER_USER = "admin";

    private final static String SUPER_ROLE_NAME = "admin";

    // 创建一个缓存对象
    private static final Cache<Integer, RangerClient> clientCache = CacheUtil.newTimedCache(60 * 1000); // 设置缓存有效期为1分钟

    public static Service rangerKmsService(String serviceName, String rangerUrl) {
        return Service.builder()
                .name(serviceName)
                .isEnabled(true)
                .type("kms")
                .configs(MapUtil.<String, String>builder()
                        .put("username", "keyadmin")
                        .put("password", "admin123")
                        .put("provider", "kms://http@" + rangerUrl + ":9292/kms")
                        .build()
                )
                .build();
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
        List<String> defaultUsers = Arrays.asList("root", "hdfs", "yarn", "hive", "hbase", "mapred", "admin", "elastic", "hue");
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

    public static RangerClient getRangerClient(Integer clusterTenant) throws Exception {
        RangerClient cachedClient = clientCache.get(clusterTenant);
        if (cachedClient != null) {
            return cachedClient;
        }

        Map<String, String> globalVariables = GlobalVariables.get(clusterTenant);
        String rangerAdminUrl = globalVariables.get("${rangerAdminUrl}");
        RangerClientConfig clientConfig = RangerClientConfig.builder()
                .connectTimeoutMillis(1000)
                .readTimeoutMillis(1000)
                .logLevel(feign.Logger.Level.BASIC)
                .authConfig(RangerAuthConfig.builder()
                        .username("admin")
                        .password("admin123")
                        .build())
                .url(rangerAdminUrl)
                .build();
        RangerClient rangerClient = new RangerClient(clientConfig);
        rangerClient.start();

        clientCache.put(clusterTenant, rangerClient);

        return rangerClient;
    }

}
