/*
 *
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
 *
 */

package com.datasophon.worker;

import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.lifecycle.ServerLifeCycleManager;
import com.datasophon.common.utils.PropertyUtils;
import com.datasophon.common.utils.ShellUtils;
import com.datasophon.worker.utils.UnixUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Worker应用主类 - Spring Boot版本
 * 替代原来的Pekko Actor模式，使用HTTP REST + SSE架构
 */
@SpringBootApplication
public class WorkerApplication {

    private static final Logger logger = LoggerFactory.getLogger(WorkerApplication.class);

    private static final String USER_DIR = "user.dir";
    private static final String SH = "sh";
    private static final String NODE = "node";
    private static final String HADOOP = "hadoop";

    private static String workDir;
    private static String cpuArchitecture;

    public static void main(String[] args) {
        try {
            // 获取主机信息
            String hostname = InetAddress.getLocalHost().getHostName();
            workDir = System.getProperty(USER_DIR);
            cpuArchitecture = ShellUtils.getCpuArchitecture();

            // 缓存主机名
            CacheUtils.put(Constants.HOSTNAME, hostname);

            logger.info("Starting DataSophon Worker...");
            logger.info("Hostname: {}, WorkDir: {}, CPU: {}", hostname, workDir, cpuArchitecture);

            // 启动Spring Boot应用
            ConfigurableApplicationContext context = SpringApplication.run(WorkerApplication.class, args);

            logger.info("Worker started successfully - Waiting for Master to connect");
            logger.info("Worker listening on port: 2552");

            // 注册shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (!ServerLifeCycleManager.isStopped()) {
                    logger.info("Shutdown hook triggered");
                    stopNodeExporter();
                    context.close();
                }
            }));

        } catch (UnknownHostException e) {
            logger.error("Failed to get hostname", e);
            System.exit(1);
        }
    }

    /**
     * 应用启动后初始化
     */
    @PostConstruct
    public void init() {
        logger.info("Initializing Worker components...");

        // 启动NodeExporter
        startNodeExporter();

        // 初始化用户映射
        Map<String, String> userMap = new HashMap<>(16);
        initUserMap(userMap);

        // 创建默认用户
        createDefaultUser(userMap);

        logger.info("Worker initialized successfully");
    }

    /**
     * 应用关闭前清理
     */
    @PreDestroy
    public void destroy() {
        logger.info("Shutting down Worker...");
        stopNodeExporter();
        logger.info("Worker stopped");
    }

    /**
     * 初始化用户映射关系
     */
    private void initUserMap(Map<String, String> userMap) {
        userMap.put("hdfs", HADOOP);
        userMap.put("yarn", HADOOP);
        userMap.put("hive", HADOOP);
        userMap.put("mapred", HADOOP);
        userMap.put("hbase", HADOOP);
        userMap.put("kyuubi", HADOOP);
        userMap.put("elastic", "elastic");
        userMap.put("hue", "hue");
        userMap.put("postgres", "postgres");
        userMap.put("admin", HADOOP);
    }

    /**
     * 创建默认用户和组
     */
    private void createDefaultUser(Map<String, String> userMap) {
        for (Map.Entry<String, String> entry : userMap.entrySet()) {
            String user = entry.getKey();
            String group = entry.getValue();
            
            // 创建组（如果不存在）
            if (!UnixUtils.isGroupExists(group)) {
                UnixUtils.createUnixGroup(group);
            }
            
            // 创建用户
            UnixUtils.createUnixUser(user, group, null);
        }
        logger.info("Default users and groups created");
    }

    /**
     * 启动NodeExporter
     */
    private void startNodeExporter() {
        operateNodeExporter("restart");
        logger.info("NodeExporter started");
    }

    /**
     * 停止NodeExporter
     */
    private static void stopNodeExporter() {
        operateNodeExporter("stop");
        logger.info("NodeExporter stopped");
    }

    /**
     * 操作NodeExporter (启动/停止/重启)
     */
    private static void operateNodeExporter(String operate) {
        ArrayList<String> commands = new ArrayList<>();
        commands.add(SH);
        
        if (Constants.x86_64.equals(cpuArchitecture)) {
            commands.add(workDir + "/node/x86/control.sh");
        } else {
            commands.add(workDir + "/node/arm/control.sh");
        }
        
        commands.add(operate);
        commands.add(NODE);
        
        ShellUtils.execWithStatus(Constants.INSTALL_PATH, commands, 60L, logger);
    }
}

