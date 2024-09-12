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
                                String hostname,
                                String serviceRoleName) {

        ExecResult execResult = new ExecResult();
        execResult.setExecResult(true);

        String appHome = Constants.INSTALL_PATH + Constants.SLASH + decompressPackageName;
        try {
            Set<ServiceConfig> volumePathSet = new HashSet<>();

            volumeConfig(configFileMap, appHome, volumePathSet,serviceRoleName,hostname,enableKerberos);

            volumeLog(configFileMap, logFile, hostname, appHome, volumePathSet, serviceRoleName);

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
        if (StrUtil.isBlank(runAs.getUser()) || Constants.ROOT.equals(runAs.getUser())) {
            data.put("startCommand", String.format("cd %s && sh %s %s && tail -f /dev/null",
                    appHome, startRunner.getProgram(), String.join(" ", startRunner.getArgs())));
            data.put("statusCommand", String.format("cd %s && sh %s %s",
                    appHome, statusRunner.getProgram(), String.join(" ", statusRunner.getArgs())));
        } else {
            data.put("startCommand", String.format("su - %s -c 'cd %s && sh %s %s && tail -f /dev/null'",
                    runAs.getUser(), appHome, startRunner.getProgram(), String.join(" ", startRunner.getArgs())));
            data.put("statusCommand", String.format("su - %s -c 'cd %s && sh %s %s'",
                    runAs.getUser(), appHome, statusRunner.getProgram(), String.join(" ", statusRunner.getArgs())));
        }
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

    private void volumeConfig(Map<Generators, List<ServiceConfig>> configFileMap, String appHome, Set<ServiceConfig> volumePathSet,String serviceRoleName,String hostname,Boolean enableKerberos) {
        int fileCount = 1;
        int pathCount = 1;
        for (Map.Entry<Generators, List<ServiceConfig>> entry : configFileMap.entrySet()) {
            Generators generators = entry.getKey();
            String configFilePath;
            String outputDirectory = generators.getOutputDirectory();
            if (StrUtil.isNotBlank(outputDirectory) ){
                // 如果输出目录以斜杠开头，则直接使用输出目录作为输出文件的路径
                if (outputDirectory.startsWith(Constants.SLASH)) {
                    configFilePath = String.join(Constants.SLASH, outputDirectory, generators.getFilename());
                }else {
                    String output = generators.getOutputDirectory().replaceAll("^/+", "").replaceAll("/+$", "");
                    configFilePath = String.join(Constants.SLASH, appHome, output, generators.getFilename());
                }
            } else {
                configFilePath = String.join(Constants.SLASH, appHome, generators.getFilename());
            }

            Generators key = entry.getKey();
            String filename = key.getFilename();
            if (key.getOutputDirectory().startsWith("/var/kerberos/krb5kdc")){
                continue;
            }
            // 配置文件挂载
           // if (!"java.env".equals(filename)) {
                ServiceConfig fileConfig = new ServiceConfig();
                fileConfig.setName("config" + fileCount++);
                fileConfig.setValue(configFilePath);
                volumePathSet.add(fileConfig);
            //}

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


        //if (enableKerberos){
        if(enableKerberos){
            String keytabDir = "/etc/security/keytab/";
            ServiceConfig keytabConfig = new ServiceConfig();
            keytabConfig.setName("keytab");
            keytabConfig.setValue(keytabDir);
            volumePathSet.add(keytabConfig);

            String krb5Conf = "/etc/krb5.conf";
            ServiceConfig krb5ConfConfig = new ServiceConfig();
            krb5ConfConfig.setName("krd5conf");
            krb5ConfConfig.setValue(krb5Conf);
            volumePathSet.add(krb5ConfConfig);
        }
        if ("TrinoCoordinator".equals(serviceRoleName)||"TrinoWorker".equals(serviceRoleName)){
            ServiceConfig fileConfig = new ServiceConfig();
            fileConfig.setName("config" + fileCount++);
            fileConfig.setValue("/opt/datasophon/hadoop-3.3.3/etc/hadoop");
            volumePathSet.add(fileConfig);
        }
        if ("Krb5Kdc".equals(serviceRoleName)||"KAdmin".equals(serviceRoleName)){
            String krb5kdcDir="/var/kerberos/krb5kdc";
            ServiceConfig fileConfig = new ServiceConfig();
            fileConfig.setName("kerberos-data");
            fileConfig.setValue(krb5kdcDir);
            volumePathSet.add(fileConfig);
            String keytabDir = "/etc/security/keytab/";
            ServiceConfig keytabConfig = new ServiceConfig();
            keytabConfig.setName("keytab");
            keytabConfig.setValue(keytabDir);
            volumePathSet.add(keytabConfig);
        }
        if ("KafkaBroker".equals(serviceRoleName)){
            K8sMinaUtils.execCmdWithResult(hostname, " chmod -R 775 " + appHome);
        }
    }

    private static void volumeLog(
            Map<Generators, List<ServiceConfig>> configFileMap, String logFile, String hostname, String appHome, Set<ServiceConfig> volumePathSet,String serviceRoleName) {
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

        if ("TrinoCoordinator".equals(serviceRoleName) || "TrinoWorker".equals(serviceRoleName)) {
            log.info("start config trino logfile");
            int lastSlashIndex = logStr.lastIndexOf('/');
            logStr = (lastSlashIndex != -1) ? logStr.substring(0, lastSlashIndex) : logStr;
        }

        ServiceConfig logConfig = new ServiceConfig();
        logConfig.setName("logs");
        Path logPath = Paths.get(logStr);
        Path parentPath = logPath.getParent();
        logConfig.setValue(parentPath.toString().replace("\\", "/"));
        volumePathSet.add(logConfig);
    }

    private void volumeHadoopConfig(Set<ServiceConfig> volumePathSet) {
        List<String> needHadoopService = Arrays.asList("HIVE", "HBASE");
        if (needHadoopService.contains(serviceName)) {
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