/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.datasophon.k8s.actor.handler;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.datasophon.common.Constants;
import com.datasophon.common.model.Generators;
import com.datasophon.common.model.RunAs;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.utils.PlaceholderUtils;
import com.datasophon.k8s.constants.Constant;
import com.datasophon.k8s.util.CommonUtil;
import com.datasophon.k8s.util.K8sFreeMakerUtils;
import com.datasophon.k8s.util.K8sMinaUtils;
import lombok.Data;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.InetAddress;
import java.util.*;

import static com.datasophon.common.Constants.K8S_SVC_CONF;
import static com.datasophon.k8s.util.K8sFreeMakerUtils.*;

@Data
public class K8sConfigureServiceHandler {

    private static final String RANGER_ADMIN = "RangerAdmin";

    private static final String SH = "sh";

    private String serviceName;

    private String serviceRoleName;
    private String serviceRoleFullName;

    private Logger logger;

    public K8sConfigureServiceHandler(String serviceName, String serviceRoleName) {
        this.serviceName = serviceName;
        this.serviceRoleName = serviceRoleName;
        this.serviceRoleFullName = CommonUtil.generateServiceRoleFullName(serviceName, serviceRoleName);
        String loggerName = String.format("%s-%s-%s", Constant.TASK_LOG_LOGGER_NAME, serviceName, serviceRoleName);
        logger = LoggerFactory.getLogger(loggerName);
    }

    public ExecResult configure(Map<Generators, List<ServiceConfig>> configFileMap,
                                String decompressPackageName,
                                Integer myid,
                                String serviceRoleName,
                                RunAs runAs,
                                String hostName,
                                String kubeConfig) throws Exception {
        ExecResult execResult = new ExecResult();
        try {
            HashMap<String, String> paramMap = new HashMap<>();
            paramMap.put("${host}", "{{HOST}}");
            paramMap.put("${user}", "root");
            paramMap.put("${myid}", myid + "");
            logger.info("Start to configure service role {}", serviceRoleName);
            for (Generators generators : configFileMap.keySet()) {
                List<ServiceConfig> configs = configFileMap.get(generators);
                if (generators.getFilename().equals(K8S_SVC_CONF)) {
                    continue;
                }
                String dataDir = "";
                Iterator<ServiceConfig> iterator = configs.iterator();
                ArrayList<ServiceConfig> customConfList = new ArrayList<>();
                while (iterator.hasNext()) {
                    ServiceConfig config = iterator.next();
                    if (StringUtils.isNotBlank(config.getType())) {
                        switch (config.getType()) {
                            case Constants.INPUT:
                                String value = PlaceholderUtils.replacePlaceholders((String) config.getValue(),
                                        paramMap, Constants.REGEX_VARIABLE);
                                config.setValue(value);
                                break;
                            case Constants.MULTIPLE:
                                conventToStr(config);
                                break;
                            default:
                                break;
                        }
                    }
                    if (Constants.PATH.equals(config.getConfigType())) {
                        createPath(config, runAs, hostName);
                    }
                    if (Constants.MV_PATH.equals(config.getConfigType())) {
                        movePath(config, runAs, hostName);
                    }
                    if (Constants.CUSTOM.equals(config.getConfigType())) {
                        addToCustomList(iterator, customConfList, config);
                    }
                    if (!config.isRequired() && !Constants.CUSTOM.equals(config.getConfigType())) {
                        if (StrUtil.equals("map2", config.getConfigType())) {
                            config.setConfigType("map");
                        }else {
                            iterator.remove();
                        }
                    }
                    if (config.getValue() instanceof Boolean || config.getValue() instanceof Integer) {
                        logger.info("Convert boolean and integer to string");
                        config.setValue(config.getValue().toString());
                    }

                    if ("dataDir".equals(config.getName())) {
                        logger.info("Find dataDir : {}", config.getValue());
                        dataDir = (String) config.getValue();
                    }
                    if ("TrinoCoordinator".equals(serviceRoleName) && "coordinator".equals(config.getName())) {
                        logger.info("Start config trino coordinator");
                        config.setValue("true");
                        ServiceConfig serviceConfig = new ServiceConfig();
                        serviceConfig.setName("node-scheduler.include-coordinator");
                        serviceConfig.setValue("false");
                        customConfList.add(serviceConfig);
                    }
                    if ("PrestoCoordinator".equals(serviceRoleName) && "coordinator".equals(config.getName())) {
                        logger.info("Start config presto coordinator");
                        config.setValue("true");
                        ServiceConfig serviceConfig = new ServiceConfig();
                        serviceConfig.setName("node-scheduler.include-coordinator");
                        serviceConfig.setValue("false");
                        ServiceConfig serviceConfig1 = new ServiceConfig();
                        serviceConfig1.setName("discovery-server.enabled");
                        serviceConfig1.setValue("true");
                        customConfList.add(serviceConfig);
                        customConfList.add(serviceConfig1);
                    }
                    if ("fe_priority_networks".equals(config.getName())
                            || "be_priority_networks".equals(config.getName())) {
                        config.setName("priority_networks");
                    }
                    if (("SRFE".equals(serviceRoleName)
                            || "SRBE".equals(serviceRoleName)
                            || "SRFEObserver".equals(serviceRoleName)
                            || "SRCN".equals(serviceRoleName))
                            && "priority_networks".equals(config.getName())) {
                        config.setValue(InetAddress.getLocalHost().getHostAddress());
                    }

                    if ("KyuubiServer".equals(serviceRoleName) && "sparkHome".equals(config.getName())) {
                        // add hive-site.xml link in kerberos module
                        final String targetPath = Constants.INSTALL_PATH + File.separator + decompressPackageName + "/conf/hive-site.xml";
                        if (!K8sMinaUtils.checkPathExists(hostName, targetPath)) {
                            logger.info("Add hive-site.xml link");
                            K8sMinaUtils.execCmdWithResult(hostName, "ln -s " + config.getValue() + "/conf/hive-site.xml " + targetPath);
                        }
                    }
                }

                if ("AlluxioMaster".equals(serviceRoleName) && "alluxio-site.properties".equals(generators.getFilename())) {
                    ServiceConfig serviceConfig = new ServiceConfig();
                    serviceConfig.setName("alluxio.master.hostname");
                    serviceConfig.setValue(hostName);
                    customConfList.add(serviceConfig);
                }
                if ("AlluxioWorker".equals(serviceRoleName) && "alluxio-site.properties".equals(generators.getFilename())) {
                    if (K8sMinaUtils.checkPathExists(hostName, Constants.INSTALL_PATH + File.separator + decompressPackageName + "/conf/alluxio-site.properties")) {
                        continue;
                    }
                }


                if ("node.properties".equals(generators.getFilename())) {
                    ServiceConfig serviceConfig = new ServiceConfig();
                    serviceConfig.setName("node.id");
                    serviceConfig.setValue(IdUtil.simpleUUID());
                    customConfList.add(serviceConfig);
                }
                configs.addAll(customConfList);
                if (!configs.isEmpty()) {
                    // extra app, package: META, templates
                    String path = Constants.INSTALL_PATH + File.separator + decompressPackageName + "/templates";
                    if (K8sMinaUtils.checkPathExists(hostName, path) && K8sMinaUtils.isDirectory(hostName, path)) {
                        // 3rd app, load ext templates
                        logger.info("Add ext app template path: {} to loader path.", path);
                        K8sFreeMakerUtils.generateConfigFile(
                                generators,
                                configs,
                                serviceRoleName,
                                path,
                                kubeConfig);
                    } else {
                        K8sFreeMakerUtils.generateConfigFile(
                                generators,
                                configs,
                                serviceRoleName,
                                kubeConfig,serviceRoleFullName);
                    }
                } else if (!generators.getFilename().endsWith(SH)) {
                    String configMapName = generateConfigMapName(serviceRoleName,generators);
                    createConfigMap(configMapName, "", kubeConfig, generators.getFilename(),serviceRoleFullName);
                }
                execResult.setExecOut("configure success");
                logger.info("configure success");
            }
//            if (RANGER_ADMIN.equals(serviceRoleName) && !setupRangerAdmin(hostName, decompressPackageName)) {
//                return execResult;
//            }
            execResult.setExecResult(true);
        } catch (Exception e) {
            execResult.setExecErrOut(e.getMessage());
            logger.error("load app config template error!", e);
        }
        return execResult;
    }

    private boolean setupRangerAdmin(String hostname, String decompressPackageName) {
        logger.info("start to execute ranger admin setup.sh");
        String commands = Constants.INSTALL_PATH + Constants.SLASH + decompressPackageName + Constants.SLASH + "setup.sh";
        String result = K8sMinaUtils.execCmdWithResult(hostname, commands);

        String globalCommand = Constants.INSTALL_PATH + Constants.SLASH + decompressPackageName + Constants.SLASH + "set_globals.sh";
        K8sMinaUtils.execCmdWithResult(hostname, globalCommand);

        if ("true".equals(result)) {
            logger.info("ranger admin setup success");
            return true;
        }
        logger.info("ranger admin setup failed");
        return false;
    }

    private void createPath(ServiceConfig config, RunAs runAs, String hostname) {
        String path = (String) config.getValue();
        if (StringUtils.isNotBlank(config.getSeparator()) && path.contains(config.getSeparator())) {
            for (String dir : path.split(config.getSeparator())) {
                mkdir(dir, runAs, hostname);
            }
        } else {
            mkdir(path, runAs, hostname);
        }
    }

    private void movePath(ServiceConfig config, RunAs runAs, String hostname) {
        String oldPath = (String) config.getDefaultValue();
        String newPath = (String) config.getValue();
        if (K8sMinaUtils.checkPathExists(hostname, oldPath) && !K8sMinaUtils.checkPathExists(hostname, newPath)) {
            if (StringUtils.isNotBlank(config.getSeparator()) && newPath.contains(config.getSeparator())) {
                for (String dir : newPath.split(config.getSeparator())) {
                    mkdir(dir, runAs, hostname);
                }
            } else {
                mkdir(newPath, runAs, hostname);
            }
            K8sMinaUtils.deleteFile(hostname, oldPath);
            K8sMinaUtils.createFile(hostname, newPath);
            logger.info("move path {} to {}", oldPath, newPath);
        }
    }

    private void addToCustomList(Iterator<ServiceConfig> iterator, ArrayList<ServiceConfig> customConfList,
                                 ServiceConfig config) {
        List<JSONObject> list = (List<JSONObject>) config.getValue();
        iterator.remove();
        for (JSONObject json : list) {
            if (Objects.nonNull(json)) {
                Set<String> set = json.keySet();
                for (String key : set) {
                    if (StringUtils.isNotBlank(key)) {
                        ServiceConfig serviceConfig = new ServiceConfig();
                        serviceConfig.setName(key);
                        serviceConfig.setValue(json.get(key));
                        customConfList.add(serviceConfig);
                    }
                }
            }
        }
    }

    private String conventToStr(ServiceConfig config) {
        JSONArray value = (JSONArray) config.getValue();
        List<String> strs = value.toJavaList(String.class);
        logger.info("size is :{}", strs.size());
        String joinValue = String.join(config.getSeparator(), strs);
        config.setValue(joinValue);
        logger.info("config set value to {}", config.getValue());
        return joinValue;
    }

    private void mkdir(String path, RunAs runAs, String hostname) {
        logger.info("create file path {}", path);
        if (!K8sMinaUtils.checkPathExists(hostname, path)) {
            String command =
                    "mkdir -p " + path
                            + " && "
                            + "chmod 775 " + path;
            if (Objects.nonNull(runAs)) {
                command = command + " && chown -R " + runAs.getUser() + ":" + runAs.getGroup() + " " + path;
            }
            K8sMinaUtils.execCmdWithResult(hostname, command);
        }
    }

}