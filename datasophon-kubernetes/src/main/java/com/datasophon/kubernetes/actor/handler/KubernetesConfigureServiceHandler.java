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

package com.datasophon.kubernetes.actor.handler;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.datasophon.common.Constants;
import com.datasophon.common.model.Generators;
import com.datasophon.common.model.RunAs;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.utils.PlaceholderUtils;
import com.datasophon.kubernetes.util.CommonUtil;
import com.datasophon.kubernetes.util.KubernetesFreeMakerUtils;
import com.datasophon.kubernetes.util.KubernetesMinaUtils;
import lombok.Data;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.datasophon.kubernetes.util.KubernetesFreeMakerUtils.generateConfigMapName;

@Data
public class KubernetesConfigureServiceHandler {

    private static final String RANGER_ADMIN = "RangerAdmin";

    private static final String SH = "sh";

    private String serviceName;

    private String serviceRoleName;
    private String serviceRoleFullName;

    private Logger logger;

    public KubernetesConfigureServiceHandler(String serviceName, String serviceRoleName) {
        this.serviceName = serviceName;
        this.serviceRoleName = serviceRoleName;
        this.serviceRoleFullName = CommonUtil.generateServiceRoleFullName(serviceName, serviceRoleName);
        String loggerName = String.format("%s-%s-%s", Constants.TASK_LOG_LOGGER_NAME, serviceName, serviceRoleName);
        logger = LoggerFactory.getLogger(loggerName);
    }

    public ExecResult configure(String namespace, Map<Generators, List<ServiceConfig>> configFileMap,
            String decompressPackageName,
            Integer myid,
            String serviceRoleName,
            RunAs runAs,
            String hostName) throws Exception {
        ExecResult execResult = new ExecResult();

        try {
            HashMap<String, String> paramMap = new HashMap<>();
            paramMap.put("${hostname}", "$(hostname)");
            paramMap.put("${host}", "{{HOST}}");
            paramMap.put("${user}", "root");
            paramMap.put("${myid}", myid + "");
            logger.info("Start to configure service role {}", serviceRoleName);
            for (Generators generators : configFileMap.keySet()) {
                List<ServiceConfig> configs = configFileMap.get(generators);
                if (StrUtil.startWith(generators.getFilename(), Constants.KUBERNETES_CONFIG_PREFIX)) {
                    continue;
                }

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
                    if (Constants.MV_PATH.equals(config.getConfigType())) {
                        movePath(config, runAs, hostName);
                    }
                    if (Constants.CUSTOM.equals(config.getConfigType())) {
                        addToCustomList(iterator, customConfList, config);
                    }
                    if (!config.isRequired() && !Constants.CUSTOM.equals(config.getConfigType())) {
                        if (StrUtil.equals("map2", config.getConfigType())) {
                            config.setConfigType("map");
                        } else {
                            iterator.remove();
                        }
                    }
                    if (config.getValue() instanceof Boolean || config.getValue() instanceof Integer) {
                        logger.info("Convert boolean and integer to string");
                        config.setValue(config.getValue().toString());
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

                    if ("KyuubiServer".equals(serviceRoleName) && "sparkHome".equals(config.getName())) {
                        // add hive-site.xml link in kerberos module
                        final String targetPath = Constants.INSTALL_PATH + File.separator + decompressPackageName
                                + "/conf/hive-site.xml";
                        if (!KubernetesMinaUtils.checkPathExists(hostName, targetPath)) {
                            logger.info("Add hive-site.xml link");
                            KubernetesMinaUtils.execCmdWithResult(hostName,
                                    "ln -s " + config.getValue() + "/conf/hive-site.xml " + targetPath);
                        }
                    }
                }

                if ("AlluxioMaster".equals(serviceRoleName)
                        && "alluxio-site.properties".equals(generators.getFilename())) {
                    ServiceConfig serviceConfig = new ServiceConfig();
                    serviceConfig.setName("alluxio.master.hostname");
                    serviceConfig.setValue(hostName);
                    customConfList.add(serviceConfig);
                }
                if ("AlluxioWorker".equals(serviceRoleName)
                        && "alluxio-site.properties".equals(generators.getFilename())) {
                    if (KubernetesMinaUtils.checkPathExists(hostName, Constants.INSTALL_PATH + File.separator
                            + decompressPackageName + "/conf/alluxio-site.properties")) {
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
                    KubernetesFreeMakerUtils.generateConfigFile(
                            namespace,
                            generators,
                            configs,
                            serviceRoleName, serviceRoleFullName);
                } else if (!generators.getFilename().endsWith(SH)) {
                    String configMapName = generateConfigMapName(serviceRoleFullName, generators);
                    KubernetesFreeMakerUtils.cacheConfigMap(namespace, configMapName, "", generators.getFilename(),
                            serviceRoleFullName);
                }

                execResult.setExecOut("configure success");
                logger.info("configure success");

            }
            execResult.setExecResult(true);
        } catch (Exception e) {
            execResult.setExecErrOut(e.getMessage());
            logger.error("load app config template error!", e);
        }
        return execResult;
    }

    private void movePath(ServiceConfig config, RunAs runAs, String hostname) {
        String oldPath = (String) config.getDefaultValue();
        String newPath = (String) config.getValue();
        if (KubernetesMinaUtils.checkPathExists(hostname, oldPath)
                && !KubernetesMinaUtils.checkPathExists(hostname, newPath)) {
            if (StringUtils.isNotBlank(config.getSeparator()) && newPath.contains(config.getSeparator())) {
                for (String dir : newPath.split(config.getSeparator())) {
                    mkdir(dir, runAs, hostname);
                }
            } else {
                mkdir(newPath, runAs, hostname);
            }
            KubernetesMinaUtils.deleteFile(hostname, oldPath);
            KubernetesMinaUtils.createFile(hostname, newPath);
            logger.info("move path {} to {}", oldPath, newPath);
        }
    }

    private void addToCustomList(Iterator<ServiceConfig> iterator, ArrayList<ServiceConfig> customConfList,
            ServiceConfig config) {
        List<JSONObject> list = Convert.toList(JSONObject.class, config.getValue());
        iterator.remove();
        for (JSONObject json : list) {
            if (Objects.nonNull(json)) {
                Set<String> set = json.keySet();
                for (String key : set) {
                    if (StringUtils.isNotBlank(key)) {
                        ServiceConfig serviceConfig = new ServiceConfig();
                        serviceConfig.setName(key);
                        serviceConfig.setValue(json.get(key));
                        serviceConfig.setConfigType(config.getConfigType());
                        customConfList.add(serviceConfig);
                    }
                }
            }
        }
    }

    private void conventToStr(ServiceConfig config) {
        JSONArray value = (JSONArray) config.getValue();
        List<String> strs = value.toJavaList(String.class);
        logger.info("size is :{}", strs.size());
        String joinValue = String.join(config.getSeparator(), strs);
        config.setValue(joinValue);
        logger.info("config set value to {}", config.getValue());
    }

    private void mkdir(String path, RunAs runAs, String hostname) {
        logger.info("create file path {}", path);
        if (!KubernetesMinaUtils.checkPathExists(hostname, path)) {
            String command = "mkdir -p " + path
                    + " && "
                    + "chmod 775 " + path;
            if (Objects.nonNull(runAs)) {
                command = command + " && chown -R " + runAs.getUser() + ":" + runAs.getGroup() + " " + path;
            }
            KubernetesMinaUtils.execCmdWithResult(hostname, command);
        }
    }

}