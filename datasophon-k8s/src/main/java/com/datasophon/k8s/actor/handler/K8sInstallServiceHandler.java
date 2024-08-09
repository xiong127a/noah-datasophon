package com.datasophon.k8s.actor.handler;

import com.datasophon.common.Constants;
import com.datasophon.common.command.InstallServiceRoleCommand;
import com.datasophon.common.model.RunAs;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.utils.PropertyUtils;
import com.datasophon.k8s.constants.Constant;
import com.datasophon.k8s.util.K8sMinaUtils;
import lombok.Data;
import org.apache.commons.lang.StringUtils;
import org.apache.sshd.client.session.ClientSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
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
        try (ClientSession clientSession = K8sMinaUtils.openConnection(command.getHostName(), 22, Constants.ROOT)) {
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
        if (!K8sMinaUtils.checkDirExists(session, appHome)) {
            if (Objects.nonNull(runAs)) {
                K8sMinaUtils.execCmdWithResult(session,
                        " chown -R " + runAs.getUser() + ":" + runAs.getGroup() + " " + appHome);
            }
            K8sMinaUtils.execCmdWithResult(session, " chmod -R 775 " + appHome);
            // 修改包含Prometheus的包中的文件
            if (decompressPackageName.contains(Constants.PROMETHEUS)) {
                String alertPath = Constants.INSTALL_PATH + Constants.SLASH + decompressPackageName
                        + Constants.SLASH + "alert_rules";
                K8sMinaUtils.execCmdWithResult(session,
                        "sed -i \"s/clusterIdValue/" + PropertyUtils.getString("clusterId")
                                + "/g\" `grep clusterIdValue -rl " + alertPath + "`");
            }
            // 修改包含Hadoop的包中的文件
            if (decompressPackageName.contains(HADOOP)) {
                changeHadoopInstallPathPerm(decompressPackageName, session);
            }
        }
        if (!K8sMinaUtils.checkDirExists(session, appLinkHome)) {
            K8sMinaUtils.execCmdWithResult(session, "ln -s " + appHome + " " + appLinkHome);
        }
        return true;
    }

    private void changeHadoopInstallPathPerm(String decompressPackageName, ClientSession session) {
        K8sMinaUtils.execCmdWithResult(session,
                " chown -R  root:hadoop " + Constants.INSTALL_PATH + Constants.SLASH + decompressPackageName);
        K8sMinaUtils.execCmdWithResult(session,
                " chmod 755 " + Constants.INSTALL_PATH + Constants.SLASH + decompressPackageName);
        K8sMinaUtils.execCmdWithResult(session,
                " chmod -R 755 " + Constants.INSTALL_PATH + Constants.SLASH + decompressPackageName + "/etc");
        K8sMinaUtils.execCmdWithResult(session,
                " chmod 6050 " + Constants.INSTALL_PATH + Constants.SLASH + decompressPackageName
                        + "/bin/container-executor");
        K8sMinaUtils.execCmdWithResult(session,
                " chmod 400 " + Constants.INSTALL_PATH + Constants.SLASH + decompressPackageName
                        + "/etc/hadoop/container-executor.cfg");
        K8sMinaUtils.execCmdWithResult(session,
                " chown -R yarn:hadoop " + Constants.INSTALL_PATH + Constants.SLASH + decompressPackageName
                        + "/logs/userlogs");
        K8sMinaUtils.execCmdWithResult(session,
                " chmod 775 " + Constants.INSTALL_PATH + Constants.SLASH + decompressPackageName + "/logs/userlogs");
    }
}
