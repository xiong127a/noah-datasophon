package test;

import com.datasophon.api.utils.MinaUtils;
import com.datasophon.common.Constants;
import com.datasophon.common.model.Generators;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.k8s.actor.handler.K8sYamlDeploymentHandler;
import org.apache.sshd.client.session.ClientSession;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class YamlTest {

    @Test
    public void test() {
        Generators generators = new Generators();
        generators.setConfigFormat("properties");
        generators.setFilename("zoo.cfg");
        generators.setTemplateName("properties.ftl");
        generators.setOutputDirectory("/opt/datasophon/zookeeper-3.5.10");

        // config data
        ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.setType("input");
        serviceConfig.setConfigType("path");
        serviceConfig.setName("dataDir");
        serviceConfig.setValue("/data/zookeeper");

        ServiceConfig serviceConfig2 = new ServiceConfig();
        serviceConfig2.setType("input");
        serviceConfig2.setConfigType("path");
        serviceConfig2.setName("dataLogDir");
        serviceConfig2.setValue("/data/log");

        ArrayList<ServiceConfig> serviceConfigs = new ArrayList<>();
        serviceConfigs.add(serviceConfig);
        serviceConfigs.add(serviceConfig2);

        Map<Generators, List<ServiceConfig>> configFileMap = new HashMap<>();
        configFileMap.put(generators, serviceConfigs);

        K8sYamlDeploymentHandler k8sYamlDeploymentHandler = new K8sYamlDeploymentHandler("ZOOKEEPER", "ZkServer");
        k8sYamlDeploymentHandler.configure(
                configFileMap,
                "zookeeper-3.5.10",
                "k8s-01");
    }

    @Test
    public void testConn() {
        ClientSession clientSession = MinaUtils.openConnection("k8s-01", 22, Constants.ROOT);
        MinaUtils.writeUtf8String(clientSession, "aa", "/opt/datasophon/zookeeper-3.5.10/aaa.txt");
    }

}
