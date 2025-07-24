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

package com.datasophon.api.utils;

import com.datasophon.common.model.ServiceRoleInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

/**
 * 滚动重启工具类
 * 1、分批
 * a、 master 权限主节点分批
 * b、其他实例分批
 * <p>
 * 2、调用
 * a、master role 节点调用
 * b、其他 权限 节点调用
 * c、延时执行
 * d、同步执行
 * <p>
 * 3、处理结果
 * a、状态回收
 * b、异常信息保存
 */
public class RollingRestartUtils {

    private static final Logger logger = LoggerFactory.getLogger(RollingRestartUtils.class);

    /**
     * hostName+服务id:实例id，
     */
    public static Map<String, CountDownLatch> bachIdMap = new ConcurrentHashMap<>();

    /**
     * hostName+服务id  :启动状态
     */
    public static Map<String, Boolean> serverInstanceExecuteResultMap = new ConcurrentHashMap<>();


    private RollingRestartUtils() {
    }

    /**
     * 更新实例执行结果
     *
     */
    public static void updateStatus(String key, boolean execResult) {
        serverInstanceExecuteResultMap.put(key, execResult);
        logger.info("Rolling restart updateStatus key:{}, execResult :{}, ", key, execResult);
        if (Objects.nonNull(bachIdMap.get(key))) {
            bachIdMap.get(key).countDown();
            logger.info("Rolling restart updateStatus key:{}, countDown :{}, ", key, bachIdMap.get(key).getCount());
        }
    }

    public static int getErrorCount(String key) {
        return Optional.of(RollingRestartUtils.serverInstanceExecuteResultMap.get(key)).orElse(true) ? 0 : 1;
    }


    /**
     * 对master 权限的实例进行排序
     * 从节点在前，主节点在后
     *
     */
    public static List<ServiceRoleInfo> sortMasterRole(List<ServiceRoleInfo> serviceRoleInfoList) {
        return serviceRoleInfoList.stream().sorted(Comparator.comparingInt(v -> (v.isSlave() ? 1 : 0))).collect(Collectors.toList());
    }

    /**
     * 获取 计数器，进行同步控制等待
     *
     */
    public static CountDownLatch getCountDownLatchByServiceKey(String key) {
        bachIdMap.put(key, new CountDownLatch(1));
        logger.info("Rolling restart getCountDownLatchByServiceKey bachIdMap ,{}:{}", key, bachIdMap.get(key).getCount());
        return bachIdMap.get(key);
    }


    /**
     * 清楚缓存
     */
    public static void clean() {
        bachIdMap.clear();
        serverInstanceExecuteResultMap.clear();
    }



}
