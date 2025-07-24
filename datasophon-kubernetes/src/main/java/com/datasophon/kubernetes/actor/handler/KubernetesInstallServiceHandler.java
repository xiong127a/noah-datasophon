package com.datasophon.kubernetes.actor.handler;

import com.datasophon.common.Constants;
import com.datasophon.common.command.InstallServiceRoleCommand;
import com.datasophon.common.model.RunAs;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.utils.PropertyUtils;
import com.datasophon.kubernetes.util.KubernetesMinaUtils;
import lombok.Data;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;

@Data
public class KubernetesInstallServiceHandler {

    private static final String HADOOP = "hadoop";

    private String serviceName;

    private String serviceRoleName;

    private Logger logger;

    public KubernetesInstallServiceHandler(String serviceName, String serviceRoleName) {
        this.serviceName = serviceName;
        this.serviceRoleName = serviceRoleName;
        String loggerName = String.format("%s-%s-%s", Constants.TASK_LOG_LOGGER_NAME, serviceName, serviceRoleName);
        logger = LoggerFactory.getLogger(loggerName);
    }

    /**
     * 安装服务角色
     *
     * @param command 安装服务角色的命令
     * @return 执行结果
     */
    public ExecResult install(InstallServiceRoleCommand command) throws IOException {
        ExecResult execResult = new ExecResult();
        try {
//            execResult.setExecResult(createConfDir(command.getDecompressPackageName(), command.getRunAs(), command.getHostName()));
            execResult.setExecResult(true);
        } catch (Exception e) {
            execResult.setExecOut(e.getMessage());
            e.printStackTrace();
        }
        return execResult;
    }

    private boolean createConfDir(String decompressPackageName, RunAs runAs, String hostname) throws IOException {
        if (Objects.nonNull(runAs) && !Constants.ROOT.equals(runAs.getUser())) {
            KubernetesMinaUtils.createUserAndGroup(hostname, runAs.getUser(), runAs.getGroup());
        }

        String appHome = Constants.INSTALL_PATH + Constants.SLASH + decompressPackageName;
        String appLinkHome = Constants.INSTALL_PATH + Constants.SLASH + StringUtils.lowerCase(serviceName);
        if (!KubernetesMinaUtils.checkPathExists(hostname, appHome)) {
            if (Objects.nonNull(runAs)) {
                KubernetesMinaUtils.execCmdWithResult(hostname,
                        "mkdir -p " + appHome + " && " +
                                " chown -R " + runAs.getUser() + ":" + runAs.getGroup() + " " + appHome);
            }
            KubernetesMinaUtils.execCmdWithResult(hostname, " chmod -R 775 " + appHome);
            // 修改包含Prometheus的包中的文件
            if (decompressPackageName.contains(Constants.PROMETHEUS)) {
                String alertPath = Constants.INSTALL_PATH + Constants.SLASH + decompressPackageName
                        + Constants.SLASH + "alert_rules";
                KubernetesMinaUtils.execCmdWithResult(hostname,
                        "sed -i \"s/clusterIdValue/" + PropertyUtils.getString("clusterId")
                                + "/g\" `grep clusterIdValue -rl " + alertPath + "`");
            }
            // 修改包含Hadoop的包中的文件
            if (decompressPackageName.contains(HADOOP)) {
                changeHadoopInstallPathPerm(decompressPackageName, hostname);
            }
        }
        if (!KubernetesMinaUtils.checkPathExists(hostname, appLinkHome)) {
            KubernetesMinaUtils.execCmdWithResult(hostname, "ln -s " + appHome + " " + appLinkHome);
        }
        return true;
    }

    private void changeHadoopInstallPathPerm(String decompressPackageName, String hostname) {
        KubernetesMinaUtils.execCmdWithResult(hostname,
                " chown -R  root:hadoop " + Constants.INSTALL_PATH + Constants.SLASH + decompressPackageName);
        KubernetesMinaUtils.execCmdWithResult(hostname,
                " chmod 755 " + Constants.INSTALL_PATH + Constants.SLASH + decompressPackageName);
        KubernetesMinaUtils.execCmdWithResult(hostname,
                " chmod -R 755 " + Constants.INSTALL_PATH + Constants.SLASH + decompressPackageName + "/etc");
        KubernetesMinaUtils.execCmdWithResult(hostname,
                " chmod 6050 " + Constants.INSTALL_PATH + Constants.SLASH + decompressPackageName
                        + "/bin/container-executor");
        KubernetesMinaUtils.execCmdWithResult(hostname,
                " chmod 400 " + Constants.INSTALL_PATH + Constants.SLASH + decompressPackageName
                        + "/etc/hadoop/container-executor.cfg");
        KubernetesMinaUtils.execCmdWithResult(hostname,
                " chown -R yarn:hadoop " + Constants.INSTALL_PATH + Constants.SLASH + decompressPackageName
                        + "/logs/userlogs");
        KubernetesMinaUtils.execCmdWithResult(hostname,
                " chmod 775 " + Constants.INSTALL_PATH + Constants.SLASH + decompressPackageName + "/logs/userlogs");
    }
}
