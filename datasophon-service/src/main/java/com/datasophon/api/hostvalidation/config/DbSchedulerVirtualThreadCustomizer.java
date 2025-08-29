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

package com.datasophon.api.hostvalidation.config;

import com.github.kagkarlsson.scheduler.Scheduler;
import com.github.kagkarlsson.scheduler.boot.config.DbSchedulerCustomizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

/**
 * db-scheduler虚拟线程定制器
 * 为db-scheduler配置虚拟线程执行器，提升并发性能
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-28
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    name = "datasophon.checker.scheduler.virtual-threads", 
    havingValue = "true", 
    matchIfMissing = true
)
public class DbSchedulerVirtualThreadCustomizer implements DbSchedulerCustomizer {

    private final Environment environment;

    @Override
    public Optional<ExecutorService> executorService() {
        log.info("配置db-scheduler虚拟线程执行器");
        
        // 任务执行线程池 - 使用虚拟线程
        ThreadFactory taskThreadFactory = Thread.ofVirtual()
            .name(Scheduler.THREAD_PREFIX + "-execute-task-", 1)
            .factory();

        int threads = environment.getProperty(
            "datasophon.checker.scheduler.threads", 
            Integer.class, 
            10
        );
        
        // 注意：虚拟线程应该使用newThreadPerTaskExecutor而不是newScheduledThreadPool
        ExecutorService executor = Executors.newThreadPerTaskExecutor(taskThreadFactory);
        
        log.info("db-scheduler虚拟线程执行器配置完成，使用虚拟线程池");
        return Optional.of(executor);
    }

    @Override
    public Optional<ExecutorService> dueExecutor() {
        log.info("配置db-scheduler到期任务虚拟线程执行器");
        
        // 到期任务检查执行器 - 使用虚拟线程
        ThreadFactory dueThreadFactory = Thread.ofVirtual()
            .name(Scheduler.THREAD_PREFIX + "-execute-due-", 1)
            .factory();

        ExecutorService executor = Executors.newThreadPerTaskExecutor(dueThreadFactory);
        
        log.info("db-scheduler到期任务虚拟线程执行器配置完成");
        return Optional.of(executor);
    }

    @Override
    public Optional<ScheduledExecutorService> housekeeperExecutor() {
        log.info("配置db-scheduler管家任务虚拟线程执行器");
        
        // 管家任务执行器 - 需要定时功能，使用ScheduledExecutorService
        ThreadFactory housekeeperThreadFactory = Thread.ofVirtual()
            .name(Scheduler.THREAD_PREFIX + "-housekeeper-", 1)
            .factory();

        // 管家任务需要定时功能，使用ScheduledThreadPool
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(3, housekeeperThreadFactory);
        
        log.info("db-scheduler管家任务虚拟线程执行器配置完成，线程数: 3");
        return Optional.of(executor);
    }
}
