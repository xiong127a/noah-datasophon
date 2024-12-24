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
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;

import javax.annotation.PostConstruct;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.Month;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
@ServletComponentScan
@ComponentScan("com.datasophon")
@MapperScan("com.datasophon.dao")
@EnableSpringUtil
public class DataSophonApplicationServer extends SpringBootServletInitializer {

    private static ScheduledExecutorService scheduler;

    public static void main(String[] args) {
        SpringApplication.run(DataSophonApplicationServer.class, args);
        // Add shutdown hook to close and shutdown resources
        Runtime.getRuntime().addShutdownHook(new Thread(DataSophonApplicationServer::shutdown));
    }

    @PostConstruct
    public void run() throws UnknownHostException, NoSuchAlgorithmException {
        checkStopDate();
        String hostName = InetAddress.getLocalHost().getHostName();
        CacheUtils.put(Constants.HOSTNAME, hostName);
        ActorUtils.init(); // 静态初始化方法

        // Schedule tasks
        scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(this::checkStopDate, 0, 24 * 60 * 60,TimeUnit.SECONDS);
    }


    /**
     * 优雅关闭方法
     */
    public static void shutdown() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        ActorUtils.shutdown(); // 静态关闭方法
    }

    private void checkStopDate() {
        LocalDate today = LocalDate.now();
        LocalDate stopDate = LocalDate.of(2025, Month.JANUARY, 21);

        if (today.isAfter(stopDate) || today.equals(stopDate)) {
            shutdown();
            System.out.println("The application cannot start because the stop date has passed.");
            System.exit(0); // 退出程序
        }
    }
}
