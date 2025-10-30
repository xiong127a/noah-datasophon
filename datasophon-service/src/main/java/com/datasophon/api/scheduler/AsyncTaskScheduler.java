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

package com.datasophon.api.scheduler;

import com.github.kagkarlsson.scheduler.Scheduler;
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * 异步任务调度器
 * 统一管理异步任务提交到 db-scheduler，替代 @Async 注解
 * 
 * 优势：
 * 1. 任务持久化到数据库，不会因为进程重启丢失
 * 2. 可追踪、可重试、可监控
 * 3. 避免 Spring @Async 线程池卡死问题
 * 4. 集群友好，自动负载均衡
 * 
 * @author DataSophon Team
 */
@Slf4j
@Service
public class AsyncTaskScheduler {

    @Autowired
    private Scheduler scheduler;

    /**
     * 立即异步执行任务（无参数）
     * 
     * @param taskName 任务名称
     * @param task 任务逻辑
     */
    public void executeAsync(String taskName, Runnable task) {
        executeAsync(taskName, task, 0);
    }

    /**
     * 延迟异步执行任务（无参数）
     * 
     * @param taskName 任务名称
     * @param task 任务逻辑
     * @param delaySeconds 延迟秒数
     */
    public void executeAsync(String taskName, Runnable task, int delaySeconds) {
        String taskId = generateTaskId(taskName);
        Instant executionTime = Instant.now().plusSeconds(delaySeconds);
        
        // 创建一次性任务
        OneTimeTask<Void> oneTimeTask = Tasks.oneTime(taskName)
            .execute((instance, context) -> {
                log.info("执行异步任务: taskName={}, taskId={}", taskName, instance.getId());
                try {
                    task.run();
                    log.info("异步任务执行成功: taskName={}, taskId={}", taskName, instance.getId());
                } catch (Exception e) {
                    log.error("异步任务执行失败: taskName={}, taskId={}, error={}", 
                        taskName, instance.getId(), e.getMessage(), e);
                    throw e; // 重新抛出让 db-scheduler 处理重试
                }
            });
        
        scheduler.schedule(oneTimeTask.instance(taskId), executionTime);
        log.debug("已提交异步任务: taskName={}, taskId={}, executeAt={}", taskName, taskId, executionTime);
    }

    /**
     * 立即异步执行任务（带参数）
     * 
     * @param taskName 任务名称
     * @param data 任务数据
     * @param taskLogic 任务逻辑
     * @param <T> 数据类型
     */
    public <T> void executeAsync(String taskName, T data, Consumer<T> taskLogic) {
        executeAsync(taskName, data, taskLogic, 0);
    }

    /**
     * 延迟异步执行任务（带参数）
     * 
     * @param taskName 任务名称
     * @param data 任务数据
     * @param taskLogic 任务逻辑
     * @param delaySeconds 延迟秒数
     * @param <T> 数据类型
     */
    public <T> void executeAsync(String taskName, T data, Consumer<T> taskLogic, int delaySeconds) {
        String taskId = generateTaskId(taskName);
        Instant executionTime = Instant.now().plusSeconds(delaySeconds);
        
        // 创建一次性任务（带数据）
        OneTimeTask<T> oneTimeTask = Tasks.oneTime(taskName, (Class<T>) data.getClass())
            .execute((instance, context) -> {
                T taskData = instance.getData();
                log.info("执行异步任务: taskName={}, taskId={}, dataType={}", 
                    taskName, instance.getId(), taskData.getClass().getSimpleName());
                try {
                    taskLogic.accept(taskData);
                    log.info("异步任务执行成功: taskName={}, taskId={}", taskName, instance.getId());
                } catch (Exception e) {
                    log.error("异步任务执行失败: taskName={}, taskId={}, error={}", 
                        taskName, instance.getId(), e.getMessage(), e);
                    throw e; // 重新抛出让 db-scheduler 处理重试
                }
            });
        
        scheduler.schedule(oneTimeTask.instance(taskId, data), executionTime);
        log.debug("已提交异步任务: taskName={}, taskId={}, executeAt={}, dataType={}", 
            taskName, taskId, executionTime, data.getClass().getSimpleName());
    }

    /**
     * 生成唯一任务ID
     * 
     * @param taskName 任务名称
     * @return 任务ID
     */
    private String generateTaskId(String taskName) {
        return taskName + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}

