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

package com.datasophon.worker.handler;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.StreamProgress;
import cn.hutool.core.lang.Console;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSONObject;
import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.command.InstallServiceRoleCommand;
import com.datasophon.common.model.RunAs;
import com.datasophon.common.utils.*;
import com.datasophon.worker.utils.TaskConstants;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.datasophon.common.utils.HostUtils.GetMasterHost;

@Data
public class InstallServiceHandler {


    private static final String HADOOP = "hadoop";

    private String serviceName;

    private String serviceRoleName;

    private Logger logger;

    public InstallServiceHandler(String serviceName, String serviceRoleName) {
        this.serviceName = serviceName;
        this.serviceRoleName = serviceRoleName;
        var loggerName = "%s-%s-%s".formatted(TaskConstants.TASK_LOG_LOGGER_NAME, serviceName, serviceRoleName);
        logger = LoggerFactory.getLogger(loggerName);
    }

    /**
     * 安装服务角色
     *
     * @param command 安装服务角色的命令
     * @return 执行结果
     */
    public ExecResult install(InstallServiceRoleCommand command) {
        var execResult = new ExecResult();
        try {
            // 使用集群ID创建日志路径
            var loggerName = "%s-%s-%s-%s".formatted(
                TaskConstants.TASK_LOG_LOGGER_NAME, command.getClusterId(), serviceName, serviceRoleName);
            logger = LoggerFactory.getLogger(loggerName);
            String destDir = Constants.INSTALL_PATH + Constants.SLASH + "DDP/packages" + Constants.SLASH;
            String packageName = command.getPackageName();
            String packagePath = destDir + packageName;

            // 判断是否需要下载包文件
            boolean needDownLoad = !GetMasterHost().contains(CacheUtils.get(Constants.HOSTNAME))
                    && isNeedDownloadPkg(packagePath, command.getPackageMd5());

            if (needDownLoad) {
                // 下载包文件
                downloadPkg(packageName, packagePath);
            }

            // 解压缩包文件
            boolean result = decompressPkg(packageName, command.getDecompressPackageName(), command.getRunAs(), packagePath);
            execResult.setExecResult(result);
        } catch (Exception e) {
            execResult.setExecOut(e.getMessage());
            e.printStackTrace();
        }
        return execResult;
    }


    private Boolean isNeedDownloadPkg(String packagePath, String packageMd5) {
        boolean needDownLoad = true;

        // 输出远程包的md5
        logger.info("Remote package md5 is {}", packageMd5);

        // 如果本地包路径存在
        if (FileUtil.exist(packagePath)) {
            // 检查md5
            String md5 = FileUtils.md5(new File(packagePath));

            // 输出本地包的md5
            logger.info("Local md5 is {}", md5);

            // 如果本地md5不为空且与远程md5相等
            if (StringUtils.isNotBlank(md5) && packageMd5.trim().equals(md5.trim())) {
                needDownLoad = false;
            }
        }

        // 返回是否需要下载
        return needDownLoad;
    }


    private void downloadPkg(String packageName, String packagePath) {
        // 获取集群的存储库配置
        RepositoryConfig repoConfig = getClusterRepositoryFromMaster();
        
        logger.info("Repository config: type={}, url={}", repoConfig.getRepoType(), repoConfig.getRepoUrl());
        
        // 根据存储库类型选择下载方式
        if (Constants.REPO_TYPE_HTTP.equals(repoConfig.getRepoType())) {
            // HTTP远程存储库：直接从HTTP URL下载
            downloadFromHttp(repoConfig.getRepoUrl(), packageName, packagePath);
        } else {
            // 本地存储库：从Master HTTP接口下载（保持现有逻辑）
            downloadFromMaster(packageName, packagePath);
        }
    }
    
    /**
     * 从Master获取集群存储库配置
     */
    private RepositoryConfig getClusterRepositoryFromMaster() {
        String masterPort = PropertyUtils.getString(Constants.MASTER_WEB_PORT);
        List<String> masterHosts = GetMasterHost();
        
        for (String masterHost : masterHosts) {
            try {
                String apiUrl = "http://" + masterHost + ":" + masterPort
                        + "/ddh/api/v1/cluster/parcel/cluster/" + command.getClusterId() + "/repository";
                
                logger.info("Getting repository config from: {}", apiUrl);
                
                String response = HttpUtil.get(apiUrl);
                JSONObject json = JSONObject.parseObject(response);
                
                if (json.getInteger("code") == 200) {
                    JSONObject data = json.getJSONObject("data");
                    RepositoryConfig config = new RepositoryConfig();
                    config.setRepoType(data.getString("repoType"));
                    config.setRepoUrl(data.getString("repoUrl"));
                    logger.info("Successfully got repository config: type={}, url={}", 
                            config.getRepoType(), config.getRepoUrl());
                    return config;
                }
            } catch (Exception e) {
                logger.warn("Failed to get repository config from {}: {}", masterHost, e.getMessage());
            }
        }
        
        // 如果获取失败，返回默认本地配置
        logger.warn("Failed to get repository config from all masters, using default local config");
        RepositoryConfig defaultConfig = new RepositoryConfig();
        defaultConfig.setRepoType(Constants.REPO_TYPE_LOCAL);
        defaultConfig.setRepoUrl("/opt/datasophon/DDP/packages");
        return defaultConfig;
    }
    
    /**
     * 从HTTP远程存储库下载
     */
    private void downloadFromHttp(String repoUrl, String packageName, String packagePath) {
        String downloadUrl = repoUrl.endsWith("/") 
                ? repoUrl + packageName 
                : repoUrl + "/" + packageName;
        
        logger.info("Downloading from HTTP repository: {}", downloadUrl);
        
        try {
            HttpUtil.downloadFile(downloadUrl, FileUtil.file(packagePath), new StreamProgress() {
                @Override
                public void start() {
                    logger.info("Start downloading package from HTTP repository");
                    Console.log("start to download from HTTP repository...");
                }

                @Override
                public void progress(long progressSize, long total) {
                    logger.info("Download progress: {}/{}", 
                            FileUtil.readableFileSize(progressSize), 
                            FileUtil.readableFileSize(total));
                    Console.log("downloaded: {}", FileUtil.readableFileSize(progressSize));
                }

                @Override
                public void finish() {
                    logger.info("Download from HTTP repository finished");
                    Console.log("download success!");
                }
            });
            
            logger.info("Successfully downloaded package from HTTP repository");
        } catch (Exception e) {
            logger.error("Failed to download from HTTP repository: {}", downloadUrl, e);
            throw new RuntimeException("Failed to download package from HTTP repository: " + e.getMessage());
        }
    }
    
    /**
     * 从Master下载（本地存储库）
     */
    private void downloadFromMaster(String packageName, String packagePath) {

        String masterPort = PropertyUtils.getString(Constants.MASTER_WEB_PORT);

        List<String> masterHosts = GetMasterHost();
        String downloadUrl = "http://" + masterHosts.getFirst() + ":" + masterPort
                + "/ddh/service/install/downloadPackage?packageName=" + packageName;

        logger.info("default download url is {}", downloadUrl);
        boolean downloadSuccess = false;
        for (int i = 0; i < masterHosts.size(); i++) {
            try {
                String masterHost = masterHosts.get(i);
                downloadUrl = "http://" + masterHost + ":" + masterPort
                        + "/ddh/service/install/downloadPackage?packageName=" + packageName;

                logger.info("Trying to download from Master {}", downloadUrl);

                // 下载文件
                HttpUtil.downloadFile(downloadUrl, FileUtil.file(packagePath), new StreamProgress() {

                    @Override
                    public void start() {
                        Console.log("start to install。。。。");
                    }

                    @Override
                    public void progress(long progressSize, long l1) {
                        Console.log("installed：{}", FileUtil.readableFileSize(progressSize));
                    }

                    @Override
                    public void finish() {
                        Console.log("install success！");
                    }
                });
                downloadSuccess = true;
                logger.info("Download package {} success from {}", packageName, masterHost);
                break;  // 跳出循环，表示下载成功
            } catch (Exception e) {
                // 捕获异常并记录日志
                logger.error("Download failed from {}. Error: {}", masterHosts.get(i), e.getMessage());

                // 如果是最后一个主机，抛出异常
                if (i == masterHosts.size() - 1) {
                    throw new RuntimeException("Download failed from all hosts");
                }
            }
        }
        if (!downloadSuccess) {
            logger.error("Download package {} failed from all hosts", packageName);
        }
        logger.info("download package {} success", packageName);
    }

    /**
     * 解压包
     *
     * @param packageName 包名
     * @param decompressPackageName 解压后的包名
     * @param runAs 运行用户
     * @param packagePath 包路径
     * @return 解压是否成功
     */
    private boolean decompressPkg(String packageName, String decompressPackageName, RunAs runAs, String packagePath) {
        // 判断解压后的包是否存在
        if (!FileUtil.exist(Constants.INSTALL_PATH + Constants.SLASH + decompressPackageName)) {
            // 解压包
            Boolean decompressResult = decompressTarGz(packagePath, Constants.INSTALL_PATH);
            if (Boolean.TRUE.equals(decompressResult)) {
                // 设置解压后的包权限
                if (Objects.nonNull(runAs)) {
                    ShellUtils.exceShell(" chown -R " + runAs.getUser() + ":" + runAs.getGroup() + " "
                            + Constants.INSTALL_PATH + Constants.SLASH + decompressPackageName);
                }
                ShellUtils
                        .exceShell(" chmod -R 775 " + Constants.INSTALL_PATH + Constants.SLASH + decompressPackageName);
                // 修改包含Prometheus的包中的文件
                if (decompressPackageName.contains(Constants.PROMETHEUS)) {
                    String alertPath = Constants.INSTALL_PATH + Constants.SLASH + decompressPackageName
                            + Constants.SLASH + "alert_rules";
                    ShellUtils.exceShell("sed -i \"s/clusterIdValue/" + PropertyUtils.getString("clusterId")
                            + "/g\" `grep clusterIdValue -rl " + alertPath + "`");
                }
                // 修改包含Hadoop的包中的文件
                if (decompressPackageName.contains(HADOOP)) {
                    changeHadoopInstallPathPerm(decompressPackageName);
                }
                return true;
            } else {
                logger.warn("install package {} failed", packageName);
                return false;
            }
        } else {
            return true;
        }
    }


    public Boolean decompressTarGz(String sourceTarGzFile, String targetDir) {
        logger.info("Start to use tar -zxvf to decompress {}", sourceTarGzFile);
        ArrayList<String> command = new ArrayList<>();
        command.add("tar");
        command.add("-zxvf");
        command.add(sourceTarGzFile);
        command.add("-C");
        command.add(targetDir);
        ExecResult execResult = ShellUtils.execWithStatus(targetDir, command, 120, logger);
        return execResult.getExecResult();
    }


    private void changeHadoopInstallPathPerm(String decompressPackageName) {
        ShellUtils.exceShell(
                " chown -R  root:hadoop " + Constants.INSTALL_PATH + Constants.SLASH + decompressPackageName);
        ShellUtils.exceShell(" chmod 755 " + Constants.INSTALL_PATH + Constants.SLASH + decompressPackageName);
        ShellUtils.exceShell(
                " chmod -R 755 " + Constants.INSTALL_PATH + Constants.SLASH + decompressPackageName + "/etc");
        ShellUtils.exceShell(" chmod 6050 " + Constants.INSTALL_PATH + Constants.SLASH + decompressPackageName
                + "/bin/container-executor");
        ShellUtils.exceShell(" chmod 400 " + Constants.INSTALL_PATH + Constants.SLASH + decompressPackageName
                + "/etc/hadoop/container-executor.cfg");
        ShellUtils.exceShell(" chown -R yarn:hadoop " + Constants.INSTALL_PATH + Constants.SLASH + decompressPackageName
                + "/logs/userlogs");
        ShellUtils.exceShell(
                " chmod 775 " + Constants.INSTALL_PATH + Constants.SLASH + decompressPackageName + "/logs/userlogs");
    }
}
