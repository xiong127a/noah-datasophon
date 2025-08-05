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
import com.datasophon.api.service.SimpleClusterVariableService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 简单集群变量生成服务实现
 * 从ProcessUtils迁移而来的变量生成功能
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-01
 */
@Slf4j
@Service
public class SimpleClusterVariableServiceImpl implements SimpleClusterVariableService {

    @Override
    public void generateClusterVariable(Map<String, String> globalVariables, Integer clusterId, String key, String value) {
        if (globalVariables == null) {
            log.warn("全局变量映射为空，无法设置变量 {} = {}", key, value);
            return;
        }
        
        if (key == null || key.trim().isEmpty()) {
            log.warn("变量key为空，无法设置变量");
            return;
        }
        
        globalVariables.put(key, value);
        GlobalVariables.put(clusterId, globalVariables);
        
        log.debug("成功设置集群 {} 的变量 {} = {}", clusterId, key, value);
    }
}