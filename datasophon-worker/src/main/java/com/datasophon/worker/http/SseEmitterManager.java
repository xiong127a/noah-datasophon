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

package com.datasophon.worker.http;

import com.datasophon.worker.http.event.WorkerEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * SSE连接管理器
 * 管理所有的SSE连接，支持按taskId或clusterId推送事件
 * 
 * 单向通讯设计：
 * - Master主动建立SSE连接（GET请求）
 * - Worker通过已建立的连接推送事件
 * - Worker绝不主动连接Master
 */
@Component
public class SseEmitterManager {

    private static final Logger logger = LoggerFactory.getLogger(SseEmitterManager.class);

    /**
     * 存储所有按taskId订阅的emitters
     * Key: taskId, Value: emitters集合
     */
    private final Map<String, CopyOnWriteArraySet<SseEmitter>> taskEmitters = new ConcurrentHashMap<>();

    /**
     * 存储所有按clusterId订阅的emitters
     * Key: clusterId, Value: emitters集合
     */
    private final Map<Long, CopyOnWriteArraySet<SseEmitter>> clusterEmitters = new ConcurrentHashMap<>();

    /**
     * 默认超时时间（0表示无超时）
     */
    private static final long DEFAULT_TIMEOUT = 0L;

    /**
     * 创建SSE Emitter
     */
    public SseEmitter createEmitter() {
        return new SseEmitter(DEFAULT_TIMEOUT);
    }

    /**
     * 添加任务订阅
     * Master通过GET /api/tasks/{taskId}/events建立连接时调用
     */
    public void addTaskSubscription(String taskId, SseEmitter emitter) {
        taskEmitters.computeIfAbsent(taskId, k -> new CopyOnWriteArraySet<>()).add(emitter);
        logger.info("Added SSE subscription for task: {}", taskId);

        // 设置完成/超时/错误回调
        emitter.onCompletion(() -> removeTaskSubscription(taskId, emitter));
        emitter.onTimeout(() -> removeTaskSubscription(taskId, emitter));
        emitter.onError(e -> {
            logger.error("SSE error for task: {}", taskId, e);
            removeTaskSubscription(taskId, emitter);
        });
    }

    /**
     * 添加集群订阅
     */
    public void addClusterSubscription(Long clusterId, SseEmitter emitter) {
        clusterEmitters.computeIfAbsent(clusterId, k -> new CopyOnWriteArraySet<>()).add(emitter);
        logger.info("Added SSE subscription for cluster: {}", clusterId);

        emitter.onCompletion(() -> removeClusterSubscription(clusterId, emitter));
        emitter.onTimeout(() -> removeClusterSubscription(clusterId, emitter));
        emitter.onError(e -> {
            logger.error("SSE error for cluster: {}", clusterId, e);
            removeClusterSubscription(clusterId, emitter);
        });
    }

    /**
     * 移除任务订阅
     */
    private void removeTaskSubscription(String taskId, SseEmitter emitter) {
        CopyOnWriteArraySet<SseEmitter> emitters = taskEmitters.get(taskId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                taskEmitters.remove(taskId);
            }
            logger.info("Removed SSE subscription for task: {}", taskId);
        }
    }

    /**
     * 移除集群订阅
     */
    private void removeClusterSubscription(Long clusterId, SseEmitter emitter) {
        CopyOnWriteArraySet<SseEmitter> emitters = clusterEmitters.get(clusterId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                clusterEmitters.remove(clusterId);
            }
            logger.info("Removed SSE subscription for cluster: {}", clusterId);
        }
    }

    /**
     * 向指定任务的所有订阅者推送事件
     * Worker在任务执行过程中调用此方法推送事件
     */
    public void sendEventToTask(String taskId, WorkerEvent event) {
        CopyOnWriteArraySet<SseEmitter> emitters = taskEmitters.get(taskId);
        if (emitters == null || emitters.isEmpty()) {
            logger.debug("No subscribers for task: {}", taskId);
            return;
        }

        logger.debug("Sending event to {} subscriber(s) for task: {}", emitters.size(), taskId);
        
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name(event.getEventType())
                        .data(event));
                logger.debug("Event sent successfully for task: {}", taskId);
            } catch (IOException e) {
                logger.error("Failed to send event for task: {}", taskId, e);
                removeTaskSubscription(taskId, emitter);
            }
        }
    }

    /**
     * 向指定集群的所有订阅者推送事件
     */
    public void sendEventToCluster(Long clusterId, WorkerEvent event) {
        CopyOnWriteArraySet<SseEmitter> emitters = clusterEmitters.get(clusterId);
        if (emitters == null || emitters.isEmpty()) {
            logger.debug("No subscribers for cluster: {}", clusterId);
            return;
        }

        logger.debug("Sending event to {} subscriber(s) for cluster: {}", emitters.size(), clusterId);
        
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name(event.getEventType())
                        .data(event));
            } catch (IOException e) {
                logger.error("Failed to send event for cluster: {}", clusterId, e);
                removeClusterSubscription(clusterId, emitter);
            }
        }
    }

    /**
     * 完成任务的所有SSE连接
     */
    public void completeTask(String taskId) {
        CopyOnWriteArraySet<SseEmitter> emitters = taskEmitters.remove(taskId);
        if (emitters != null) {
            emitters.forEach(emitter -> {
                try {
                    emitter.complete();
                } catch (Exception e) {
                    logger.error("Failed to complete emitter for task: {}", taskId, e);
                }
            });
            logger.info("Completed all SSE connections for task: {}", taskId);
        }
    }

    /**
     * 获取任务订阅数量
     */
    public int getTaskSubscriberCount(String taskId) {
        CopyOnWriteArraySet<SseEmitter> emitters = taskEmitters.get(taskId);
        return emitters == null ? 0 : emitters.size();
    }

    /**
     * 获取集群订阅数量
     */
    public int getClusterSubscriberCount(Long clusterId) {
        CopyOnWriteArraySet<SseEmitter> emitters = clusterEmitters.get(clusterId);
        return emitters == null ? 0 : emitters.size();
    }
}

