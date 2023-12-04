package com.datasophon.worker.actor;

import akka.actor.UntypedActor;
import com.datasophon.common.command.LdapCommand;
import com.datasophon.common.model.LdapUser;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.worker.utils.OpenldapUtils;

import javax.naming.ldap.LdapContext;

public class OpenldapActor extends UntypedActor {
    @Override
    public void onReceive(Object message) throws Throwable {
        if (message instanceof LdapCommand) {
            ExecResult execResult = new ExecResult();
            LdapCommand ldapCommand = (LdapCommand) message;
            LdapContext ldapContext = OpenldapUtils.ldapConnect(ldapCommand.getLdapUrl(), ldapCommand.getRootDn(), ldapCommand.getLdapPwd());

            if ("add".equals(ldapCommand.getOperation())) {
                LdapUser ldapUser = OpenldapUtils.buildBaseLdapUser(
                        ldapCommand.getUsername(),
                        ldapCommand.getMail(),
                        ldapCommand.getDescription(),
                        ldapCommand.getUidNumber(),
                        ldapCommand.getGidNumber(),
                        ldapCommand.getUserPwd()
                );
                boolean addResult = OpenldapUtils.addUser(ldapUser, ldapCommand.getUserRootDn(), ldapContext);
                execResult.setExecResult(addResult);
            } else {
                boolean deleteResult = OpenldapUtils.delete(
                        ldapCommand.getUsername(),
                        ldapCommand.getUserRootDn(),
                        ldapContext
                );
                execResult.setExecResult(deleteResult);
            }

            getSender().tell(execResult, getSelf());
        } else {
            unhandled(message);
        }
    }
}
