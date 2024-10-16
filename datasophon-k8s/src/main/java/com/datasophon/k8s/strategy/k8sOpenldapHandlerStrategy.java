package com.datasophon.k8s.strategy;

import com.datasophon.common.command.K8sServiceRoleOperateCommand;
import com.datasophon.common.enums.CommandType;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.k8s.actor.handler.K8sServiceHandler;
import com.datasophon.k8s.util.K8sMinaUtils;

import java.io.IOException;
import java.sql.SQLException;

public class k8sOpenldapHandlerStrategy extends K8sAbstractHandlerStrategy implements K8sServiceRoleStrategy {

    public k8sOpenldapHandlerStrategy(String serviceName, String serviceRoleName) {
        super(serviceName, serviceRoleName);
    }

    @Override
    public ExecResult handler(K8sServiceRoleOperateCommand command) throws SQLException, ClassNotFoundException, IOException {
        K8sServiceHandler serviceHandler = new K8sServiceHandler(command.getServiceName(), command.getServiceRoleName());
        String hostname = command.getHostname();
        if (command.getCommandType().equals(CommandType.INSTALL_SERVICE)) {
            K8sMinaUtils.createUserAndGroup(hostname, "ldap", "ldap");
            /*if (!K8sMinaUtils.checkPathExists(hostname, "/var/lib/openldap")) {
                K8sMinaUtils.createDir(hostname,"/var/lib/openldap");
            }
            if (!K8sMinaUtils.checkPathExists(hostname, "/etc/openldap/slapd.d/")) {
                K8sMinaUtils.createDir(hostname,"/etc/openldap/slapd.d/");
            }
            K8sMinaUtils.execCmdWithResult(hostname, "chown -R ldap:ldap /var/lib/openldap /etc/openldap/slapd.d/");
            K8sMinaUtils.execCmdWithResult(hostname, "chmod -R 700 /var/lib/openldap /etc/openldap/slapd.d/");*/
        }
        return serviceHandler.start(command);
    }



}
