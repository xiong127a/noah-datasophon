package com.datasophon.k8s.actor.handler;

import com.datasophon.api.service.ClusterServiceRoleInstanceService;
import com.datasophon.api.utils.MinaUtils;
import com.datasophon.api.utils.SpringTool;
import com.datasophon.common.Constants;
import com.datasophon.common.model.Generators;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;
import com.datasophon.k8s.constants.Constant;
import com.datasophon.k8s.util.CommonUtil;
import com.datasophon.k8s.util.DockerImageUtils;
import com.datasophon.k8s.util.FreemakerUtils;
import com.google.common.collect.Lists;
import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import lombok.Data;
import org.apache.sshd.client.session.ClientSession;
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
                                String decompressPackageName,
                                String hostName,
                                Integer clusterId) {
        ExecResult execResult = new ExecResult();
        try (ClientSession session = MinaUtils.openConnection(hostName, 22, Constants.ROOT)) {
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

            ClusterServiceRoleInstanceService clusterServiceRoleInstanceService =
                    SpringTool.getApplicationContext().getBean(ClusterServiceRoleInstanceService.class);
            List<ClusterServiceRoleInstanceEntity> roleHostList =
                    clusterServiceRoleInstanceService.getServiceRoleInstanceListByClusterIdAndRoleName(clusterId, serviceRoleName);
            data.put("roleNodeCnt", roleHostList.size());

            Configuration config = new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
            List<TemplateLoader> loaderList = new ArrayList<>();
            loaderList.add(new ClassTemplateLoader(FreemakerUtils.class,
                    "/k8s-templates" + Constants.SLASH + serviceName + Constants.SLASH + "k8s"));
            config.setTemplateLoader(new MultiTemplateLoader(loaderList.toArray(new TemplateLoader[0])));
            Template template = config.getTemplate(serviceRoleFullName + ".yaml.ftl");

            String packagePath =
                    Constants.INSTALL_PATH + Constants.SLASH + decompressPackageName + Constants.SLASH + serviceRoleFullName + ".yaml";
            FreemakerUtils.writeToTemplate(template, data, packagePath, session);

        } catch (Exception e) {
            execResult.setExecErrOut(e.getMessage());
            logger.error("load k8s yaml template error!", e);
        }
        return execResult;
    }
}