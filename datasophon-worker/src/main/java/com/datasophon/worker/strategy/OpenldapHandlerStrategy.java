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
            String cpuArchitecture = ShellUtils.getCpuArchitecture();

            // 安装依赖
            logger.info("start install libtool");
            ShellUtils.exceShell("yum localinstall -y " + workPath + "/*rpm");
            // 安装服务
            logger.info("start install openldap");
            ShellUtils.exceShell("echo \"" + workPath + "/db/lib/\" > /etc/ld.so.conf");
            if ("aarch64".equals(cpuArchitecture)) {
                ShellUtils.exceShell("echo \"" + workPath + "/db/lib/\" > /etc/ld.so.conf");
            }
            ShellUtils.exceShell("ldconfig -v");
            String configureCommand = "sh " + workPath + "/configure " +
                    "--prefix=" + workPath + " " +
                    "--enable-syslog " +
                    "--enable-modules " +
                    "--enable-debug " +
                    "--enable-dynamic " +
                    "--enable-rlookups " +
                    "--enable-slapd " +
                    "--enable-crypt " +
                    "--enable-spasswd " +
                    "--enable-memberof=mod " +
                    "--enable-refint=mod " +
                    "--with-tls " +
                    "CPPFLAGS=-I" + workPath + "/db/include " +
                    "LDFLAGS=-L" + workPath + "/db/lib";
            ShellUtils.exceShell(configureCommand);
            logger.info("start make && make install");
            ShellUtils.exceShell("make && make install");

            // 开启日志
            ShellUtils.exceShell("echo \"local4.* "+ workPath + "/var/slapd.log\" >> /etc/rsyslog.conf");
            ShellUtils.exceShell("systemctl restart rsyslog");

            // 初始化OpenLDAP
            logger.info("start OpenLDAP");
            ShellUtils.exceShell("ln -s " + workPath + "/bin/* /usr/bin/");
            ShellUtils.exceShell("ln -s " + workPath + "/sbin/* /usr/sbin/");
            ShellUtils.exceShell("cp -f " + workPath + "/slapd.conf " + workPath + "/etc/openldap");
            ShellUtils.exceShell("sh " + workPath + "/setup_tls.sh");
            logger.info("init success");

            // 启动
            ShellUtils.exceShell("sh " + workPath + "/control_openldap.sh start");
            ShellUtils.exceShell(" ldapadd -Q -Y EXTERNAL -H ldapi:/// -f " + workPath + "/refint2.ldif");


            // 添加基础用户
            ShellUtils.exceShell("ldapadd -x -D cn=root,dc=ldap,dc=com -w 123456 -f " + workPath + "/base.ldif");
            ShellUtils.exceShell("ldapadd -x -D cn=root,dc=ldap,dc=com -w 123456 -f " + workPath + "/default-user.ldif");
        }
        ExecResult startResult = serviceHandler.start(command.getStartRunner(), command.getStatusRunner(),
                command.getDecompressPackageName(), command.getRunAs());
        return startResult;
    }
}
