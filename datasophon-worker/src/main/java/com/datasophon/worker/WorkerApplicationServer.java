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

import com.alibaba.fastjson2.JSONObject;
import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.lifecycle.ServerLifeCycleManager;
import com.datasophon.common.model.StartWorkerMessage;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.utils.PropertyUtils;
import com.datasophon.common.utils.ShellUtils;
import com.datasophon.worker.actor.RemoteEventActor;
import com.datasophon.worker.actor.WorkerActor;
import com.datasophon.worker.utils.ActorUtils;
import com.datasophon.worker.utils.UnixUtils;
import com.datasophon.worker.utils.WorkerFreemarkerUtils;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSelection;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.actor.Props;
import org.apache.pekko.event.EventStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class WorkerApplicationServer {

    private static final Logger logger = LoggerFactory.getLogger(WorkerApplicationServer.class);

    private static final String USER_DIR = "user.dir";

    private static final String MASTER_HOST = "masterHost";

    private static final String WORKER = "worker";

    private static final String SH = "sh";

    private static final String NODE = "node";

    private static final String HADOOP = "hadoop";

    public static void main(String[] args) throws UnknownHostException {
        String hostname = InetAddress.getLocalHost().getHostName();
        String workDir = System.getProperty(USER_DIR);
        String masterHost = PropertyUtils.getString(MASTER_HOST);
        String cpuArchitecture = ShellUtils.getCpuArchitecture();

        CacheUtils.put(Constants.HOSTNAME, hostname);
        // init actor
        ActorSystem system = initActor(hostname);
        ActorUtils.setActorSystem(system);

        subscribeRemoteEvent(system);

        startNodeExporter(workDir, cpuArchitecture);

        Map<String, String> userMap = new HashMap<>(16);
        initUserMap(userMap);

        createDefaultUser(userMap);

        logger.info("start worker - 等待Master主动连接");
        logger.info("Worker节点已启动，主机名: {}, 工作目录: {}, CPU架构: {}", hostname, workDir, cpuArchitecture);

        /*
         * registry hooks, which are called before the process exits
         */
        Runtime.getRuntime()
                .addShutdownHook(
                        new Thread(() -> {
                            if (!ServerLifeCycleManager.isStopped()) {
                                close("WorkerServer shutdown hook");
                            }
                        }));
    }

    private static void initUserMap(Map<String, String> userMap) {
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

    private static void createDefaultUser(Map<String, String> userMap) {
        for (Map.Entry<String, String> entry : userMap.entrySet()) {
            String user = entry.getKey();
            String group = entry.getValue();
            if (!UnixUtils.isGroupExists(group)) {
                UnixUtils.createUnixGroup(group);
            }
            UnixUtils.createUnixUser(user, group, null);
        }
    }

    private static ActorSystem initActor(String hostname) {
        // 使用新的配置方式，适应Akka 2.10.7-M1
        Config config = ConfigFactory.parseString("akka.remote.artery.canonical.hostname=" + hostname);
        ActorSystem system = ActorSystem.create("datasophon", config.withFallback(ConfigFactory.load()));
        system.actorOf(Props.create(WorkerActor.class), WORKER);

        // 设置ActorSystem到FreemakerUtils，用于模板获取
        WorkerFreemarkerUtils.setActorSystem(system);

        return system;
    }

    private static void subscribeRemoteEvent(ActorSystem system) {
        // 使用Props.create()方法创建RemoteEventActor
        ActorRef remoteEventActor = system.actorOf(Props.create(RemoteEventActor.class), "remoteEventActor");
        EventStream eventStream = system.eventStream();
        // 在Akka 2.10.7-M1中，经典remoting事件已被弃用
        // 如果需要监控远程连接状态，请使用Akka集群的成员事件
        eventStream.subscribe(remoteEventActor, Object.class);
    }

    /**
     * 向Master发送Worker状态信息 [已废弃]
     * 现在改为Master主动连接Worker，这个方法不再使用
     * 
     * @deprecated Master现在主动连接Worker，不再需要Worker主动发送状态
     */
    @Deprecated
    private static void tellToMaster(
            String hostname,
            String workDir,
            String masterHost,
            String cpuArchitecture,
            ActorSystem system) {
        ActorSelection workerStartActor = system.actorSelection(
                "akka://datasophon@" + masterHost + ":2551/user/workerStartActor");
        // 收集主机信息
        ExecResult result = ShellUtils.exceShell(workDir + "/script/host-info-collect.sh");
        logger.info("host info collect result:{}", result);
        StartWorkerMessage startWorkerMessage = JSONObject.parseObject(result.getExecOut(), StartWorkerMessage.class);
        logger.info("主机信息收集结果: {}", result);

        // 如果信息收集失败,重试最多3次
        int retryCount = 0;
        while (result.getExecOut() == null && retryCount < 3) {
            logger.warn("主机信息收集失败,第{}次重试...", retryCount + 1);
            try {
                // 等待2秒后重试
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            result = ShellUtils.exceShell(workDir + "/script/host-info-collect.sh");
            retryCount++;
        }

        if (result.getExecOut() == null) {
            logger.error("主机信息收集失败,无法向Master报告状态");
            return;
        }

        // 解析收集到的主机信息
        startWorkerMessage.setCpuArchitecture(cpuArchitecture);
        startWorkerMessage.setClusterId(PropertyUtils.getLong("clusterId"));
        startWorkerMessage.setHostname(hostname);

        // 从配置文件中读取IP并设置
        final String hostIp = getHostIp();
        logger.info("使用IP地址: {}", hostIp);
        startWorkerMessage.setIp(hostIp);

        // 向Master发送Worker状态信息
        logger.info("向Master({})发送Worker状态信息", masterHost);
        workerStartActor.tell(startWorkerMessage, ActorRef.noSender());

        // 在后台线程中定期发送心跳,确保Master能够收到消息
        ScheduledExecutorService heartbeatExecutor = new ScheduledThreadPoolExecutor(1);
        final AtomicInteger heartbeatCount = new AtomicInteger(0);

        heartbeatExecutor.scheduleWithFixedDelay(() -> {
            try {
                // 最多发送3次心跳
                if (heartbeatCount.incrementAndGet() > 3) {
                    heartbeatExecutor.shutdown();
                    return;
                }

                logger.info("发送第{}次心跳消息", heartbeatCount.get());
                // 复制原消息作为心跳消息
                StartWorkerMessage heartbeatMessage = new StartWorkerMessage();
                heartbeatMessage.setHostname(hostname);
                heartbeatMessage.setClusterId(PropertyUtils.getLong("clusterId"));

                // 确保心跳消息也包含IP信息
                heartbeatMessage.setIp(hostIp);

                workerStartActor.tell(heartbeatMessage, ActorRef.noSender());
            } catch (Exception e) {
                logger.error("发送心跳消息失败", e);
                heartbeatExecutor.shutdown();
            }
        }, 5, 10, TimeUnit.SECONDS);

        // 注册关闭钩子,确保线程池正确关闭
        Runtime.getRuntime().addShutdownHook(new Thread(heartbeatExecutor::shutdown));
    }

    public static void close(String cause) {
        stopNodeExporter();
        logger.info("Worker server stopped");
    }

    private static void stopNodeExporter() {
        String workDir = System.getProperty(USER_DIR);
        String cpuArchitecture = ShellUtils.getCpuArchitecture();
        operateNodeExporter(workDir, cpuArchitecture, "stop");
    }

    private static void startNodeExporter(String workDir, String cpuArchitecture) {
        operateNodeExporter(workDir, cpuArchitecture, "restart");
    }

    private static void operateNodeExporter(
            String workDir, String cpuArchitecture, String operate) {
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

    /**
     * 获取主机IP地址
     * 从配置文件读取
     *
     * @return 主机IP地址
     */
    private static String getHostIp() {
        return PropertyUtils.getString("ip");
    }
}
