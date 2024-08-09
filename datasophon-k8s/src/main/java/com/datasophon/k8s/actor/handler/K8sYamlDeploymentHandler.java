package com.datasophon.k8s.actor.handler;

import com.datasophon.common.Constants;
import com.datasophon.common.model.Generators;
import com.datasophon.common.model.RunAs;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.model.ServiceRoleRunner;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.k8s.constants.Constant;
import com.datasophon.k8s.util.CommonUtil;
import com.datasophon.k8s.util.DockerImageUtils;
import com.datasophon.k8s.util.K8sFreemakerUtils;
import com.google.common.collect.Lists;
import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@Data
public class K8sYamlDeploymentHandler {

    private String serviceName;

    private String serviceRoleName;

    private String serviceRoleFullName;

    private Logger logger;

    public K8sYamlDeploymentHandler(String serviceName, String serviceRoleName) {
        this.serviceName = serviceName;
        this.serviceRoleName = serviceRoleName;
        this.serviceRoleFullName = CommonUtil.generateServiceRoleFullName(serviceName, serviceRoleName);
        String loggerName = String.format("%s-%s-%s", Constant.TASK_LOG_LOGGER_NAME, serviceName, serviceRoleName);
        logger = LoggerFactory.getLogger(loggerName);
    }

    public ExecResult configure(Map<Generators, List<ServiceConfig>> configFileMap,
                                RunAs runAs,
                                ServiceRoleRunner startRunner,
                                ServiceRoleRunner statusRunner,
                                Integer roleNodeCnt) {
        ExecResult execResult = new ExecResult();
        execResult.setExecResult(true);
        try {
            Map<String, Object> data = new HashMap<>();
            Set<ServiceConfig> volumePath = new HashSet<>();

            for (Generators generators : configFileMap.keySet()) {
                List<ServiceConfig> configList = configFileMap.get(generators);
                for (ServiceConfig serviceConfig : configList) {
                    if (Constants.PATH.equals(serviceConfig.getConfigType())) {
                        volumePath.add(serviceConfig);
                    }
                }
            }
            data.put("itemList", Lists.newArrayList(volumePath));
            data.put("serviceRoleFullName", serviceRoleFullName);
            data.put("serviceName", serviceName);
            data.put("namespace", Constant.K8S_NAMESPACE);
            data.put("dockerImage", DockerImageUtils.getString(serviceName));
            data.put("runAs", runAs.getUser());
            data.put("startCommand", startRunner.getProgram() + " " + String.join(" ", startRunner.getArgs()));
            data.put("statusCommand", statusRunner.getProgram() + " " + String.join(" ", statusRunner.getArgs()));
            data.put("roleNodeCnt", roleNodeCnt);

            Configuration config = new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
            List<TemplateLoader> loaderList = new ArrayList<>();
            loaderList.add(new ClassTemplateLoader(K8sFreemakerUtils.class,
                    "/k8s" + Constants.SLASH + "templates" + Constants.SLASH + serviceName + Constants.SLASH + "k8s"));
            config.setTemplateLoader(new MultiTemplateLoader(loaderList.toArray(new TemplateLoader[0])));
            Template template = config.getTemplate(serviceRoleFullName + ".yaml.ftl");

            String yamlFilePath = CommonUtil.k8sYamlFilePath(serviceRoleFullName);
            K8sFreemakerUtils.writeToTemplateLocal(template, data, yamlFilePath);

        } catch (Exception e) {
            execResult.setExecErrOut(e.getMessage());
            logger.error("{} load k8s yaml template error!", serviceRoleName, e);
        }
        return execResult;
    }
}