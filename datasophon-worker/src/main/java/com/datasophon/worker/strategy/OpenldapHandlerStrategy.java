package com.datasophon.worker.strategy;

import com.datasophon.common.Constants;
import com.datasophon.common.command.ServiceRoleOperateCommand;
import com.datasophon.common.enums.CommandType;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.utils.ShellUtils;
import com.datasophon.worker.handler.ServiceHandler;

import java.sql.SQLException;

public class OpenldapHandlerStrategy extends AbstractHandlerStrategy implements ServiceRoleStrategy {

    public OpenldapHandlerStrategy(String serviceName, String serviceRoleName) {
        super(serviceName, serviceRoleName);
    }

    @Override
    public ExecResult handler(ServiceRoleOperateCommand command) throws SQLException, ClassNotFoundException {
        ServiceHandler serviceHandler = new ServiceHandler(command.getServiceName(), command.getServiceRoleName());
        String workPath = Constants.INSTALL_PATH + Constants.SLASH + command.getDecompressPackageName();

        if (command.getCommandType().equals(CommandType.INSTALL_SERVICE)) {
            //String cpuArchitecture = ShellUtils.getCpuArchitecture();

            // 开启日志
            ShellUtils.execShell("echo \"local4.* /var/log/slapd/slapd.log >> /etc/rsyslog.conf");
            ShellUtils.execShell("systemctl restart rsyslog");

            // 初始化OpenLDAP
            logger.info("start OpenLDAP");
            ShellUtils.execShell("ln -s " + workPath + "/bin/* /usr/bin/");
            ShellUtils.execShell("ln -s " + workPath + "/sbin/* /usr/sbin/");
            ShellUtils.execShell("sh " + workPath + "/setup_tls.sh");
            logger.info("init success");

            // 启动
            ShellUtils.execShell("sh " + workPath + "/control_openldap.sh start");
            ShellUtils.execShell(" ldapadd -Q -Y EXTERNAL -H ldapi:/// -f " + workPath + "/refint2.ldif");


            // 添加基础用户
            ShellUtils.execShell("ldapadd -x -D cn=root,dc=ldap,dc=com -w 123456 -f " + workPath + "/base.ldif");
            ShellUtils.execShell("ldapadd -x -D cn=root,dc=ldap,dc=com -w 123456 -f " + workPath + "/default-user.ldif");
        }
        ExecResult startResult = serviceHandler.start(command.getStartRunner(), command.getStatusRunner(),
                command.getDecompressPackageName(), command.getRunAs());
        return startResult;
    }
}
