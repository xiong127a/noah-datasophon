package com.datasophon.kubernetes.strategy;

import com.datasophon.common.command.KubernetesServiceRoleOperateCommand;
import com.datasophon.common.enums.CommandType;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.kubernetes.actor.handler.KubernetesServiceHandler;
import com.datasophon.kubernetes.util.KubeUtil;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.utils.Serialization;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class KubernetesFlinkOperatorHandlerStrategy extends KubernetesAbstractHandlerStrategy
        implements KubernetesServiceRoleStrategy {

    private static final String CRD_DIR = "kubernetes/templates/FLINKOPERATOR/crd";

    public KubernetesFlinkOperatorHandlerStrategy(String serviceName, String serviceRoleName) {
        super(serviceName, serviceRoleName);
    }

    @Override
    public ExecResult handler(KubernetesServiceRoleOperateCommand command) {

        KubernetesServiceHandler serviceHandler = new KubernetesServiceHandler(command.getServiceName(),
                command.getServiceRoleName());
        if (command.getCommandType() == CommandType.INSTALL_SERVICE) {
            try (KubernetesClient client = KubeUtil.getKubeClientByConfig(command.getKubeConfig())) {
                submitCRDs(client);

            }
        }
        return serviceHandler.start(command);
    }

    private void submitCRDs(KubernetesClient client) {
        // 获取类加载器
        ClassLoader classLoader = getClass().getClassLoader();
        // 获取CRD目录资源
        URL resource = classLoader.getResource(CRD_DIR);

        if (resource == null) {
            logger.warn("CRD directory not found: {}", CRD_DIR);
            return;
        }

        try {
            // 处理文件系统路径（开发环境）
            if ("file".equals(resource.getProtocol())) {
                File crdDir = new File(URLDecoder.decode(resource.getFile(), StandardCharsets.UTF_8));
                if (crdDir.isDirectory()) {
                    // 扫描目录下所有YAML文件
                    File[] crdFiles = crdDir.listFiles((dir, name) -> name.endsWith(".yaml") || name.endsWith(".yml"));

                    if (crdFiles == null || crdFiles.length == 0) {
                        logger.warn("No CRD files found in directory: {}", CRD_DIR);
                        return;
                    }

                    // 提交所有CRD文件
                    for (File crdFile : crdFiles) {
                        submitCrdFile(client, crdFile.getName(), new FileInputStream(crdFile));
                    }
                }
            }
            // 处理JAR包内路径（生产环境）
            else if ("jar".equals(resource.getProtocol())) {
                String jarPath = resource.getPath().substring(5, resource.getPath().indexOf("!"));
                try (JarFile jar = new JarFile(URLDecoder.decode(jarPath, StandardCharsets.UTF_8))) {
                    Enumeration<JarEntry> entries = jar.entries();
                    while (entries.hasMoreElements()) {
                        JarEntry entry = entries.nextElement();
                        String entryName = entry.getName();
                        // 过滤目录下的YAML文件
                        if (entryName.startsWith(CRD_DIR + "/") &&
                                (entryName.endsWith(".yaml") || entryName.endsWith(".yml"))) {
                            try (InputStream is = classLoader.getResourceAsStream(entryName)) {
                                submitCrdFile(client, entryName.substring(entryName.lastIndexOf('/') + 1), is);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Failed to process CRD directory: {}", e.getMessage(), e);
        }
    }

    private void submitCrdFile(KubernetesClient client, String fileName, InputStream is) {
        if (is == null) {
            logger.error("CRD file not found: {}", fileName);
            return;
        }
        try {
            CustomResourceDefinition crd = Serialization.unmarshal(is, CustomResourceDefinition.class);
            // 检查CRD是否已存在
            CustomResourceDefinition existingCrd = client.apiextensions().v1().customResourceDefinitions()
                    .withName(crd.getMetadata().getName())
                    .get();

            if (existingCrd != null) {
                // 设置现有CRD的UID和资源版本到新对象
                crd.getMetadata().setUid(existingCrd.getMetadata().getUid());
                crd.getMetadata().setResourceVersion(existingCrd.getMetadata().getResourceVersion());
            }

            // 创建或更新CRD
            client.apiextensions().v1().customResourceDefinitions().resource(crd).serverSideApply();
            logger.info("Successfully submitted CRD: {}", fileName);
        } catch (Exception e) {
            logger.error("Failed to submit CRD from {}: {}", fileName, e.getMessage(), e);
        }
    }
}
