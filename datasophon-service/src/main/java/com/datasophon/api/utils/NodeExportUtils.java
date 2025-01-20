package com.datasophon.api.utils;

import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.common.Constants;
import com.datasophon.common.utils.PropertyUtils;
import com.datasophon.common.utils.ShellUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;

import static com.datasophon.api.utils.ProcessUtils.getDepMode;
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class NodeExportUtils {
    @Autowired
    private  ClusterInfoService clusterInfoService;
    private static final Logger logger = LoggerFactory.getLogger(NodeExportUtils.class);
    private static final String USER_DIR = "user.dir";
    @PostConstruct
    public void startNodeExporter() {
        Integer clusterId = PropertyUtils.getInt("clusterId");
        String depMode =  clusterInfoService.getById(clusterId).getDepType();
        if (Constants.K8S_MODE.equals(depMode)) {
            String workDir = System.getProperty(USER_DIR);
            operateNodeExporter(workDir, "apply");
        }
    }

    public static void stopNodeExporter() {
        Integer clusterId = PropertyUtils.getInt("clusterId");
        String depMode = getDepMode(clusterId);
        if (Constants.K8S_MODE.equals(depMode)) {
            String workDir = System.getProperty(USER_DIR);
            operateNodeExporter(workDir, "delete");
        }
    }

    private static void operateNodeExporter(
            String workDir, String operate) {
        String yamlFile = workDir + "/conf/k8s/templates/PROMETHEUS/test/node-exporter.yaml";
        ArrayList<String> commands = new ArrayList<>();
        commands.add("kubectl");
        commands.add(operate);
        commands.add("-f");
        commands.add(yamlFile);
        ShellUtils.execWithStatus(Constants.INSTALL_PATH, commands, 60L, logger);
    }
}
