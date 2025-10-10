package com.datasophon.kubernetes.strategy;

import com.datasophon.common.command.KubernetesServiceRoleOperateCommand;
import com.datasophon.common.enums.CommandType;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.kubernetes.actor.handler.KubernetesServiceHandler;
import com.datasophon.kubernetes.util.KubernetesMinaUtils;

import java.io.IOException;
import java.sql.SQLException;

public class kubernetesOpenldapHandlerStrategy extends KubernetesAbstractHandlerStrategy implements KubernetesServiceRoleStrategy {

    public kubernetesOpenldapHandlerStrategy(String serviceName, String serviceRoleName) {
        super(serviceName, serviceRoleName);
    }

    @Override
    public ExecResult handler(KubernetesServiceRoleOperateCommand command) throws SQLException, ClassNotFoundException, IOException {
        KubernetesServiceHandler serviceHandler = new KubernetesServiceHandler(command.getServiceName(), command.getServiceRoleName());
        String hostname = command.getHostname();
        if (command.getCommandType().equals(CommandType.INSTALL_SERVICE)) {
            KubernetesMinaUtils.createUserAndGroup(hostname, "ldap", "ldap");
            /*if (!KubernetesMinaUtils.checkPathExists(hostname, "/var/lib/openldap")) {
                KubernetesMinaUtils.createDir(hostname,"/var/lib/openldap");
            }
            if (!KubernetesMinaUtils.checkPathExists(hostname, "/etc/openldap/slapd.d/")) {
                KubernetesMinaUtils.createDir(hostname,"/etc/openldap/slapd.d/");
            }
            KubernetesMinaUtils.execCmdWithResult(hostname, "chown -R ldap:ldap /var/lib/openldap /etc/openldap/slapd.d/");
            KubernetesMinaUtils.execCmdWithResult(hostname, "chmod -R 700 /var/lib/openldap /etc/openldap/slapd.d/");*/
        }
        return serviceHandler.start(command);
    }



}
