package com.datasophon.k8s.actor.handler;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.model.Generators;
import com.datasophon.common.model.RunAs;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.model.ServiceRoleRunner;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.utils.PlaceholderUtils;
import com.datasophon.k8s.constants.Constant;
import com.datasophon.k8s.util.*;
import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.sftp.client.fs.SftpFileSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Data
@Slf4j
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
                                Integer roleNodeCnt,
                                String decompressPackageName,
                                String logFile,
                                String hostname) {

        ExecResult execResult = new ExecResult();
        execResult.setExecResult(true);

        String appHome = Constants.INSTALL_PATH + Constants.SLASH + decompressPackageName;
        try {
            Set<ServiceConfig> volumePathSet = new HashSet<>();

            volumeConfig(configFileMap, appHome, volumePathSet);

            volumeLog(configFileMap, logFile, hostname, appHome, volumePathSet);

            volumeHadoopConfig(volumePathSet);

            Map<String, Object> data = prepareTemplateMap(runAs, startRunner, statusRunner, roleNodeCnt, appHome, volumePathSet, configFileMap);

            Template template = generateTemplate();

            String yamlFilePath = CommonUtil.k8sYamlFilePath(serviceRoleFullName);

            K8sFreemakerUtils.writeToTemplateLocal(template, data, yamlFilePath);

        } catch (Exception e) {
            execResult.setExecErrOut(e.getMessage());
            logger.error("{} load k8s yaml template error!", serviceRoleName, e);
        }

        return execResult;
    }

    private Template generateTemplate() throws IOException {
        Configuration config = new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
        config.setTemplateLoader(new MultiTemplateLoader(
                new TemplateLoader[]{
                        new ClassTemplateLoader(K8sFreemakerUtils.class,
                                "/k8s" + Constants.SLASH + "templates" + Constants.SLASH + serviceName + Constants.SLASH + "k8s")
                }
        ));
        return config.getTemplate(serviceRoleFullName + ".yaml.ftl");
    }

    private Map<String, Object> prepareTemplateMap(RunAs runAs,
                                                   ServiceRoleRunner startRunner,
                                                   ServiceRoleRunner statusRunner,
                                                   Integer roleNodeCnt,
                                                   String appHome,
                                                   Set<ServiceConfig> volumePathSet,
                                                   Map<Generators, List<ServiceConfig>> configFileMap) {
        Map<String, Object> data = new HashMap<>();
        data.put("itemList", new ArrayList<>(volumePathSet));
        data.put("serviceRoleFullName", serviceRoleFullName);
        data.put("serviceName", serviceName);
        data.put("namespace", Constant.K8S_NAMESPACE);
        data.put("dockerImage", DockerImageUtils.getString(serviceName));
        data.put("runAs", runAs.getUser());
        data.put("startCommand", String.format("su - %s -c 'cd %s && sh %s %s && tail -f /dev/null'",
                runAs.getUser(), appHome, startRunner.getProgram(), String.join(" ", startRunner.getArgs())));
        data.put("statusCommand", String.format("su - %s -c 'cd %s && sh %s %s'",
                runAs.getUser(), appHome, statusRunner.getProgram(), String.join(" ", statusRunner.getArgs())));
        data.put(Constant.ROLE_NODE_CNT, roleNodeCnt);
        String journalnodeDir = configFileMap.values()
                .stream()
                .flatMap(List::stream)
                .filter(t -> "dfs.journalnode.edits.dir".equals(t.getName()))
                .map(t -> Convert.toStr(t.getValue()))
                .findFirst()
                .orElse(null);
        if (Objects.nonNull(journalnodeDir)) {
            data.put("journalnodeDir", journalnodeDir);
        }

        CacheUtils.put(serviceRoleFullName + "_" + Constant.ROLE_NODE_CNT, roleNodeCnt);
        return data;
    }

    private static void volumeConfig(Map<Generators, List<ServiceConfig>> configFileMap, String appHome, Set<ServiceConfig> volumePathSet) {
        int fileCount = 1;
        int pathCount = 1;
        for (Map.Entry<Generators, List<ServiceConfig>> entry : configFileMap.entrySet()) {
            Generators generators = entry.getKey();
            String configFilePath;
            if (StrUtil.isNotBlank(generators.getOutputDirectory())) {
                String output = generators.getOutputDirectory().replaceAll("^/+", "").replaceAll("/+$", "");
                configFilePath = String.join(Constants.SLASH, appHome, output, generators.getFilename());
            } else {
                configFilePath = String.join(Constants.SLASH, appHome, generators.getFilename());
            }

            Generators key = entry.getKey();
            String filename = key.getFilename();
            // 配置文件挂载
            if (!"java.env".equals(filename)) {
                ServiceConfig fileConfig = new ServiceConfig();
                fileConfig.setName("config" + fileCount++);
                fileConfig.setValue(configFilePath);
                volumePathSet.add(fileConfig);
            }

            // path配置目录挂载
            for (ServiceConfig serviceConfig : entry.getValue()) {
                if (Constants.PATH.equals(serviceConfig.getConfigType())) {
                    ServiceConfig pathConfig = new ServiceConfig();
                    pathConfig.setName("path" + pathCount++);
                    pathConfig.setValue(serviceConfig.getValue());
                    volumePathSet.add(pathConfig);
                }
            }
        }
    }

    private static void volumeLog(
            Map<Generators, List<ServiceConfig>> configFileMap, String logFile, String hostname, String appHome, Set<ServiceConfig> volumePathSet) {
        String logStr;
        Map<String, String> paramMap = configFileMap.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toMap(
                        t -> "${" + t.getName() + "}",
                        t -> Convert.toStr(t.getValue()),
                        (existing, replacement) -> replacement
                ));
        paramMap.put("${user}", "root");
        paramMap.put("${host}", hostname);
        String logFileName = PlaceholderUtils.replacePlaceholders(logFile, paramMap, Constants.REGEX_VARIABLE);

        if (logFileName.startsWith(StrUtil.SLASH)) {
            logStr = logFileName;
        } else {
            logStr = appHome + Constants.SLASH + logFileName;
        }

        try {
            K8sMinaUtils.checkParentPath(hostname, logStr);
//            if (!K8sMinaUtils.checkPathExists(hostname, logStr)) {
//                K8sMinaUtils.createFile(hostname, logStr);
//            }
        } catch (Exception e) {
            log.error("An error occurred while checking or creating the file: {}", e.getMessage(), e);
        }

        ServiceConfig logConfig = new ServiceConfig();
        logConfig.setName("logs");
        Path logPath = Paths.get(logStr);
        Path parentPath = logPath.getParent();
        logConfig.setValue(parentPath.toString().replace("\\", "/"));
        volumePathSet.add(logConfig);
    }

    private void volumeHadoopConfig(Set<ServiceConfig> volumePathSet) {
        if ("HIVE".equals(serviceName)) {
            List<String> hadoopConf = Arrays.asList(
                    "/opt/datasophon/hadoop-3.3.3/etc/hadoop/core-site.xml",
                    "/opt/datasophon/hadoop-3.3.3/etc/hadoop/hdfs-site.xml",
                    "/opt/datasophon/hadoop-3.3.3/etc/hadoop/hadoop-env.sh",
                    "/opt/datasophon/hadoop-3.3.3/etc/hadoop/mapred-site.xml",
                    "/opt/datasophon/hadoop-3.3.3/etc/hadoop/yarn-site.xml"
            );
            int config = 1;
            for (String conf : hadoopConf) {
                ServiceConfig hadoopConfig = new ServiceConfig();
                hadoopConfig.setName("hadoopconfig" + config++);
                hadoopConfig.setValue(conf);
                volumePathSet.add(hadoopConfig);
            }
        }
    }

}