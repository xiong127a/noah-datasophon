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

package com.datasophon.api.service;

import com.datasophon.common.command.CacheCommand;
import com.datasophon.common.command.ConfigMapCacheCommand;
import com.datasophon.common.command.VariableCacheCommand;
import com.datasophon.common.utils.ExecResult;

/**
 * 服务缓存同步服务
 * 替代serviceCacheSyncActor，用于Master节点间的缓存同步
 */
public interface ServiceCacheSyncService {
    
    /**
     * 处理配置缓存命令
     */
    ExecResult handleConfigMapCache(ConfigMapCacheCommand command);
    
    /**
     * 处理变量缓存命令
     */
    ExecResult handleVariableCache(VariableCacheCommand command);
    
    /**
     * 处理通用缓存命令
     */
    ExecResult handleCache(CacheCommand command);
}

