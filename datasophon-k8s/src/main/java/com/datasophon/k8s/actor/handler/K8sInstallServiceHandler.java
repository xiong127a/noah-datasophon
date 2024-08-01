package com.datasophon.k8s.actor.handler;

import com.datasophon.api.utils.MinaUtils;
import com.datasophon.common.Constants;
import com.datasophon.common.command.InstallServiceRoleCommand;
import com.datasophon.common.model.RunAs;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.utils.PropertyUtils;
import com.datasophon.k8s.constants.Constant;
import lombok.Data;
import org.apache.commons.lang.StringUtils;
import org.apache.sshd.client.session.ClientSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

@Data
public class K8sInstallServiceHandler {

    private static final String HADOOP = "hadoop";

    private String serviceName;

    private String serviceRoleName;

    private Logger logger;

    public K8sInstallServiceHandler(String serviceName, String serviceRoleName) {
        this.serviceName = serviceName;
        this.serviceRoleName = serviceRoleName;
        String loggerName = String.format("%s-%s-%s", Constant.TASK_LOG_LOGGER_NAME, serviceName, serviceRoleName);
        logger = LoggerFactory.getLogger(loggerName);
    }

    /**
     * 安装服务角色
     *
     * @param command 安装服务角色的命令
     * @return 执行结果
     */
    public ExecResult install(InstallServiceRoleCommand command) {
        ExecResult execResult = new ExecResult();
        try (ClientSession clientSession = MinaUtils.openConnection(command.getHostName(), 22, Constants.ROOT)) {
            execResult.setExecResult(createConfDir(command.getDecompressPackageName(), command.getRunAs(), clientSession));
        } catch (Exception e) {
            execResult.setExecOut(e.getMessage());
            e.printStackTrace();
        }
        return execResult;
    }

    private boolean createConfDir(String decompressPackageName, RunAs runAs, ClientSession session) {
        String appHome = Constants.INSTALL_PATH + Constants.SLASH + decompressPackageName;
        String appLinkHome = Constants.INSTALL_PATH + Constants.SLASH + StringUtils.lowerCase(serviceName);
        if (!MinaUtils.checkDirExists(session, appHome)) {
            if (Objects.nonNull(runAs)) {
                MinaUtils.execCmdWithResult(session,
                        " chown -R " + runAs.getUser() + ":" + runAs.getGroup() + " " + appHome);
            }
            MinaUtils.execCmdWithResult(session, " chmod -R 775 " + appHome);
            // 修改包含Prometheus的包中的文件
            if (decompressPackageName.contains(Constants.PROMETHEUS)) {
                String alertPath = Constants.INSTALL_PATH + Constants.SLASH + decompressPackageName
                        + Constants.SLASH + "alert_rules";
                MinaUtils.execCmdWithResult(session,
                        "sed -i \"s/clusterIdValue/" + PropertyUtils.getString("clusterId")
                                + "/g\" `grep clusterIdValue -rl " + alertPath + "`");
            }
            // 修改包含Hadoop的包中的文件
            if (decompressPackageName.contains(HADOOP)) {
                changeHadoopInstallPathPerm(decompressPackageName, session);
            }
        }
        if (!MinaUtils.checkDirExists(session, appLinkHome)) {
            MinaUtils.execCmdWithResult(session, "ln -s " + appHome + " " + appLinkHome);
        }
        return true;
    }

    private void changeHadoopInstallPathPerm(String decompressPackageName, ClientSession session) {
        MinaUtils.execCmdWithResult(session,
                " chown -R  root:hadoop " + Constants.INSTALL_PATH + Constants.SLASH + decompressPackageName);
        MinaUtils.execCmdWithResult(session,
                " chmod 755 " + Constants.INSTALL_PATH + Constants.SLASH + decompressPackageName);
        MinaUtils.execCmdWithResult(session,
                " chmod -R 755 " + Constants.INSTALL_PATH + Constants.SLASH + decompressPackageName + "/etc");
        MinaUtils.execCmdWithResult(session,
                " chmod 6050 " + Constants.INSTALL_PATH + Constants.SLASH + decompressPackageName
                        + "/bin/container-executor");
        MinaUtils.execCmdWithResult(session,
                " chmod 400 " + Constants.INSTALL_PATH + Constants.SLASH + decompressPackageName
                        + "/etc/hadoop/container-executor.cfg");
        MinaUtils.execCmdWithResult(session,
                " chown -R yarn:hadoop " + Constants.INSTALL_PATH + Constants.SLASH + decompressPackageName
                        + "/logs/userlogs");
        MinaUtils.execCmdWithResult(session,
                " chmod 775 " + Constants.INSTALL_PATH + Constants.SLASH + decompressPackageName + "/logs/userlogs");
    }
}
