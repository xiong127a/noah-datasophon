package com.datasophon.api.utils.ranger.client;

import cn.hutool.json.JSONUtil;
import com.datasophon.api.utils.ranger.client.api.RoleApis;
import com.datasophon.api.utils.ranger.client.config.RangerAuthConfig;
import com.datasophon.api.utils.ranger.client.config.RangerClientConfig;
import com.datasophon.api.utils.ranger.client.model.Policy;
import com.datasophon.api.utils.ranger.client.model.Role;
import com.datasophon.api.utils.ranger.client.model.RoleMember;
import com.datasophon.api.utils.ranger.client.model.Service;
import feign.Logger;

import java.util.Collections;

public class Test {
    public static void main(String[] args) throws Exception {
        RangerClientConfig clientConfig = RangerClientConfig.builder()
                .connectTimeoutMillis(1000)
                .readTimeoutMillis(1000)
                .logLevel(Logger.Level.BASIC)
                .authConfig(RangerAuthConfig.builder()
                        .username("admin")
                        .password("admin123")
                        .build())
                .url("http://192.168.1.54:6080")
                .build();
        RangerClient rangerClient = new RangerClient(clientConfig);
        rangerClient.start();
//        Service yarndev = rangerClient.getServices().getServiceByName("yarndev");
//        System.out.println(yarndev);
//        rangerClient.stop();

//        Service yarnService = Service.builder()
//                .name("yarndev")
//                .isEnabled(true)
//                .type("yarn")
//                .configs(
//                        MapUtil.<String, String>builder()
//                                .put("hadoop.security.authentication", "simple")
//                                .put("yarn.url", "http://192.168.1.54:8088,http://192.168.1.55:8088")
//                                .put("username", "yarn")
//                                .put("password", "yarn")
//                                .put("commonNameForCertificate", "")
//                                .build()
//                )
//                .build();
//        Service service = rangerClient.getServices()
//                .createService(yarnService);
//        System.out.println(service);

//        Policy policy = RangerUtil.simpleYarnPolicy("yarndev", "test", Collections.singletonList("root"), Collections.singletonList("gzf1"));
//        System.out.println(policy);
//        rangerClient.getPolicies().createPolicy(policy);

//        Service hadoopdev = RangerUtil.simpleHdfsService("hadoopdev", "hdfs://hadoop1:8020");
//        Service service = rangerClient.getServices().createService(hadoopdev);
//        System.out.println(service);

//        Policy policy = RangerUtil.simpleHdfsPolicy("hadoopdev", "test", Collections.singletonList("/output"), Collections.singletonList("aaa"));
//        System.out.println(JSONUtil.toJsonStr(policy));
//        rangerClient.getPolicies().createPolicy(policy);

//        Service hivedev = RangerUtil.simpleHiveService("hivedev", "jdbc:hive2://192.168.1.54:10000");
//        Service service = rangerClient.getServices().createService(hivedev);
//        System.out.println(service);

//        Policy policy = RangerUtil.simpleHivePolicyForDatabase("hivedev", "test", Collections.singletonList("*"), Collections.singletonList("gzf1"));
//        Policy policy1 = rangerClient.getPolicies().createPolicy(policy);
//        System.out.println(policy1);

//        Service hbasedev = RangerUtil.simpleHbaseService("hbasedev", "hadoop2:2181,hadoop3:2181,hadoop1:2181", "2181", "/hbase");
//        Service service = rangerClient.getServices().createService(hbasedev);
//        System.out.println(service);

//        Policy policy = RangerUtil.simpleHbasePolicy("hbasedev", "test", Collections.singletonList("bigdata:*"), Collections.singletonList("gzf1"));
//        Policy policy1 = rangerClient.getPolicies().createPolicy(policy);
//        System.out.println(policy1);

//        Policy hadoopdev = rangerClient.getPolicies().getPolicyByName("hadoopdev", "all - path");
//        Policy hadoopdev = rangerClient.getPolicies().getPolicyByName("hadoopdev", "all%20-%20path");
//        System.out.println("old json --> " + JSONUtil.toJsonStr(hadoopdev));
//        hadoopdev.setDenyAllElse(true);
//        Policy policy = rangerClient.getPolicies().updatePolicy(hadoopdev.getId(), hadoopdev);
//        System.out.println("new json --> " + JSONUtil.toJsonStr(policy));

//        RoleApis roles = rangerClient.getRoles();
//        Role role = new Role();
//        role.setName("bbb");
//        Role role1 = rangerClient.getRoles().createRole(role);
//        System.out.println(role1);

//        Role bbb = roles.getRoleByName("bbb");
//        RoleMember roleMember = new RoleMember();
//        roleMember.setIsAdmin(true);
//        roleMember.setName("hdfs");
//        bbb.setUsers(Collections.singletonList(roleMember));
//
//        Role role = roles.addUserAndGroups(3, bbb);
//        System.out.println(role);

//        roles.deleteRoleByName("aaa");

//        RangerUtil.updateHdfsDefaultPolicy(rangerClient, "hadoopdev", "all - path");

//        RangerUtil.updateDefaultPolicy(rangerClient, "yarndev");
//        RangerUtil.createSuperRole(rangerClient);

        rangerClient.stop();
    }
}
