package com.datasophon.kubernetes.actor.handler;

import com.datasophon.common.Constants;
import com.datasophon.common.utils.ExecResult;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

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
     * @return 执行结果
     */
    public ExecResult install() {
        ExecResult execResult = new ExecResult();
        try {
            execResult.setExecResult(true);
        } catch (Exception e) {
            execResult.setExecOut(e.getMessage());
            logger.error("Install service role error", e);
        }
        return execResult;
    }



}
