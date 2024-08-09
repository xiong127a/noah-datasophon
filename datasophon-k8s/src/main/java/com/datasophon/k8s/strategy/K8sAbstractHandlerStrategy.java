package com.datasophon.k8s.strategy;

import com.datasophon.k8s.constants.Constant;
import com.datasophon.k8s.util.CommonUtil;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Data
public class K8sAbstractHandlerStrategy {
    public String serviceName;

    public String serviceRoleName;

    private String serviceRoleFullName;

    public Logger logger;

    public K8sAbstractHandlerStrategy(String serviceName, String serviceRoleName) {
        this.serviceName = serviceName;
        this.serviceRoleName = serviceRoleName;
        this.serviceRoleFullName = CommonUtil.generateServiceRoleFullName(serviceName, serviceRoleName);
        String loggerName = String.format("%s-%s-%s", Constant.TASK_LOG_LOGGER_NAME, serviceName, serviceRoleName);
        logger = LoggerFactory.getLogger(loggerName);
    }

}
