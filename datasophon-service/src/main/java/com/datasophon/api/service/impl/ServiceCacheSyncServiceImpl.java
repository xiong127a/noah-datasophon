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

package com.datasophon.api.service.impl;

import com.datasophon.api.load.GlobalVariables;
import com.datasophon.api.load.ServiceConfigMap;
import com.datasophon.api.service.ServiceCacheSyncService;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.command.CacheCommand;
import com.datasophon.common.command.ConfigMapCacheCommand;
import com.datasophon.common.command.VariableCacheCommand;
import com.datasophon.common.utils.ExecResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 服务缓存同步服务实现
 * 替代serviceCacheSyncActor，处理Master节点间的缓存同步
 */
@Service
public class ServiceCacheSyncServiceImpl implements ServiceCacheSyncService {

    private static final Logger logger = LoggerFactory.getLogger(ServiceCacheSyncServiceImpl.class);

    @Override
    public ExecResult handleConfigMapCache(ConfigMapCacheCommand configMapCacheCommand) {
        logger.info("receive cache configMap ： {}", configMapCacheCommand.getKey());

        ServiceConfigMap.put(
                configMapCacheCommand.getKey(),
                configMapCacheCommand.getConfigs());
        logger.info("sync cache configMap： {}", configMapCacheCommand.getKey());

        ExecResult result = new ExecResult();
        result.setExecResult(true);
        result.setExecOut("success cache configMap： " + configMapCacheCommand.getKey());
        return result;
    }

    @Override
    public ExecResult handleVariableCache(VariableCacheCommand variableCacheCommand) {
        logger.info("receive cache variable {}", variableCacheCommand.getKey());

        Map<String, String> globalVariables = GlobalVariables.get(variableCacheCommand.getClusterId());
        globalVariables.put(variableCacheCommand.getKey(), variableCacheCommand.getValue());

        ExecResult result = new ExecResult();
        result.setExecResult(true);
        result.setExecOut("success cache variable " + variableCacheCommand.getKey());
        return result;
    }

    @Override
    public ExecResult handleCache(CacheCommand cacheCommand) {
        logger.info("get cache key {}", cacheCommand.getKey());

        String key = cacheCommand.getKey();
        ExecResult result = new ExecResult();
        result.setExecResult(true);

        if (cacheCommand.isDelete()) {
            CacheUtils.removeKey(key);
            return result;
        }

        if (CacheUtils.containsKey(key)) {
            result.setObject(CacheUtils.get(key));
            logger.info("get cache value success");
        } else {
            logger.warn("Cache key not found: {}", key);
            result.setExecResult(false);
        }

        return result;
    }
}

