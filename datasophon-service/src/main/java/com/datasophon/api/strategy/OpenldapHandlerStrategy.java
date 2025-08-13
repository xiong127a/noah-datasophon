package com.datasophon.api.strategy;

import com.datasophon.api.load.GlobalVariables;
import cn.hutool.extra.spring.SpringUtil;
import com.datasophon.api.service.SimpleClusterVariableService;

import java.util.List;
import java.util.Map;

public class OpenldapHandlerStrategy implements ServiceRoleStrategy {
    @Override
    public void handler(Long clusterId, List<String> hosts) {
        Map<String, String> globalVariables = GlobalVariables.get(clusterId);
        SimpleClusterVariableService simpleClusterVariableService = SpringUtil.getBean(SimpleClusterVariableService.class);
        if (!hosts.isEmpty()) {
            simpleClusterVariableService.generateClusterVariable(globalVariables, clusterId, "${openldapAddr}", "ldap://" + hosts.getFirst() + ":389");
            simpleClusterVariableService.generateClusterVariable(globalVariables, clusterId, "${syncLdapUrl}", "ldap://" + hosts.getFirst() + ":389");
            simpleClusterVariableService.generateClusterVariable(globalVariables, clusterId, "${openldapIp}", hosts.getFirst());
            simpleClusterVariableService.generateClusterVariable(globalVariables, clusterId, "${syncLdapBindDn}", "cn=root,dc=ldap,dc=com");
            simpleClusterVariableService.generateClusterVariable(globalVariables, clusterId, "${syncLdapBindPassword}", "123456");
            simpleClusterVariableService.generateClusterVariable(globalVariables, clusterId, "${syncLdapSearchBase}", "dc=ldap,dc=com");
            simpleClusterVariableService.generateClusterVariable(globalVariables, clusterId, "${syncLdapUserSearchBase}", "ou=People,dc=ldap,dc=com");
        }
    }

}
