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
            // 安装服务
            ShellUtils.exceShell("yum -y install openldap compat-openldap openldap-clients openldap-servers openldap-servers-sql openldap-devel migrationtools");
            logger.info("yum install openldap success");

            // 配置OpenLDAP数据库
            logger.info("start config database");
            ShellUtils.exceShell("cp /usr/share/openldap-servers/DB_CONFIG.example /var/lib/ldap/DB_CONFIG");
            ShellUtils.exceShell("chown ldap:ldap -R /var/lib/ldap");
            ShellUtils.exceShell("chmod 700 -R /var/lib/ldap");
            logger.info("config database success");

            // 启动
            ShellUtils.exceShell("systemctl enable slapd");
            ShellUtils.exceShell("systemctl start slapd");

            // 配置域名等
            ShellUtils.exceShell("slappasswd -s 123456 |sed -e 's#{SSHA}#olcRootPW: {SSHA}#g' >> " + workPath + "/changeDomain.ldif");
            ShellUtils.exceShell("ldapmodify -Y EXTERNAL -H ldapi:/// -f " + workPath + "/changeDomain.ldif");

            // 导入基本Schema
            logger.info("import database schema");
            ShellUtils.exceShell("ldapadd -Y EXTERNAL -H ldapi:/// -f /etc/openldap/schema/cosine.ldif");
            ShellUtils.exceShell("ldapadd -Y EXTERNAL -H ldapi:/// -f /etc/openldap/schema/core.ldif");
            ShellUtils.exceShell("ldapadd -Y EXTERNAL -H ldapi:/// -f /etc/openldap/schema/collective.ldif");
            ShellUtils.exceShell("ldapadd -Y EXTERNAL -H ldapi:/// -f /etc/openldap/schema/corba.ldif");
            ShellUtils.exceShell("ldapadd -Y EXTERNAL -H ldapi:/// -f /etc/openldap/schema/duaconf.ldif");
            ShellUtils.exceShell("ldapadd -Y EXTERNAL -H ldapi:/// -f /etc/openldap/schema/dyngroup.ldif");
            ShellUtils.exceShell("ldapadd -Y EXTERNAL -H ldapi:/// -f /etc/openldap/schema/inetorgperson.ldif");
            ShellUtils.exceShell("ldapadd -Y EXTERNAL -H ldapi:/// -f /etc/openldap/schema/java.ldif");
            ShellUtils.exceShell("ldapadd -Y EXTERNAL -H ldapi:/// -f /etc/openldap/schema/misc.ldif");
            ShellUtils.exceShell("ldapadd -Y EXTERNAL -H ldapi:/// -f /etc/openldap/schema/nis.ldif");
            ShellUtils.exceShell("ldapadd -Y EXTERNAL -H ldapi:/// -f /etc/openldap/schema/openldap.ldif");
            ShellUtils.exceShell("ldapadd -Y EXTERNAL -H ldapi:/// -f /etc/openldap/schema/pmi.ldif");
            ShellUtils.exceShell("ldapadd -Y EXTERNAL -H ldapi:/// -f /etc/openldap/schema/ppolicy.ldif");
            logger.info("import success");

            // 开启memberof支持
            ShellUtils.exceShell(" ldapadd -Q -Y EXTERNAL -H ldapi:/// -f " + workPath + "/add-memberof.ldif");
            ShellUtils.exceShell(" ldapadd -Q -Y EXTERNAL -H ldapi:/// -f " + workPath + "/refint1.ldif");
            ShellUtils.exceShell(" ldapadd -Q -Y EXTERNAL -H ldapi:/// -f " + workPath + "/refint2.ldif");

            // 开启日志
            ShellUtils.exceShell("echo \"local4.* /var/log/slapd/slapd.log\" >> /etc/rsyslog.conf");
            ShellUtils.exceShell("systemctl restart rsyslog");

            // 添加基础用户
            ShellUtils.exceShell("ldapadd -x -D cn=root,dc=ldap,dc=com -w 123456 -f " + workPath + "/base.ldif");
        }

        ExecResult startResult = serviceHandler.start(command.getStartRunner(), command.getStatusRunner(),
                command.getDecompressPackageName(), command.getRunAs());
        return startResult;
    }
}
