package com.datasophon.api.strategy;

import com.datasophon.api.load.GlobalVariables;
import com.datasophon.api.utils.ProcessUtils;

import java.util.List;
import java.util.Map;

public class OpenldapHandlerStrategy implements ServiceRoleStrategy {
    @Override
    public void handler(Integer clusterId, List<String> hosts) {
        Map<String, String> globalVariables = GlobalVariables.get(clusterId);
        if (!hosts.isEmpty()) {
            ProcessUtils.generateClusterVariable(globalVariables, clusterId, "${openldapAddr}", "ldap://" + hosts.getFirst() + ":389");
            ProcessUtils.generateClusterVariable(globalVariables, clusterId, "${syncLdapUrl}", "ldap://" + hosts.getFirst() + ":389");
            ProcessUtils.generateClusterVariable(globalVariables, clusterId, "${openldapIp}", hosts.getFirst());
            ProcessUtils.generateClusterVariable(globalVariables, clusterId, "${syncLdapBindDn}", "cn=root,dc=ldap,dc=com");
            ProcessUtils.generateClusterVariable(globalVariables, clusterId, "${syncLdapBindPassword}", "123456");
            ProcessUtils.generateClusterVariable(globalVariables, clusterId, "${syncLdapSearchBase}", "dc=ldap,dc=com");
            ProcessUtils.generateClusterVariable(globalVariables, clusterId, "${syncLdapUserSearchBase}", "ou=People,dc=ldap,dc=com");
        }
    }

}
