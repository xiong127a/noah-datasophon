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

package com.datasophon.api;

import cn.hutool.extra.spring.EnableSpringUtil;
import com.datasophon.api.master.ActorUtils;
import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import org.mybatis.spring.annotation.MapperScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;

import jakarta.annotation.PostConstruct;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;


@SpringBootApplication
@ComponentScan(basePackages = {
        "com.datasophon",                  // 原项目包
        "com.norinrd"       // 新增 JAR 包的包路径
})
@ServletComponentScan
@MapperScan("com.datasophon.dao")
@EnableSpringUtil
@EnableScheduling
public class DataSophonApplicationServer extends SpringBootServletInitializer {

    private static final Logger logger = LoggerFactory.getLogger(DataSophonApplicationServer.class);

    private static long startTime;

    public static void main(String[] args) {
        startTime = System.currentTimeMillis();
        logger.info("开始启动 DataSophon-API...");
        SpringApplication.run(DataSophonApplicationServer.class, args);
        // add shutdown hook， close and shutdown resources
        Runtime.getRuntime().addShutdownHook(new Thread(DataSophonApplicationServer::shutdown));
    }

    @PostConstruct
    public void init() throws UnknownHostException, NoSuchAlgorithmException {
        String hostName = InetAddress.getLocalHost().getHostName();
        CacheUtils.put(Constants.HOSTNAME, hostName);
        ActorUtils.init();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        double duration = (System.currentTimeMillis() - startTime) / 1000.0;
        logger.info("DataSophon-API 启动成功，耗时 {} 秒", String.format("%.3f", duration));
    }

    /**
     * Master 关闭时调用
     */
    public static void shutdown() {
        ActorUtils.shutdown();
    }
}
