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

import java.util.Map;

/**
 * 简单集群变量生成服务
 * 用于替代ProcessUtils中的变量生成功能
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-01
 */
public interface SimpleClusterVariableService {

    /**
     * 生成集群变量
     * 
     * @param globalVariables 全局变量映射
     * @param clusterId 集群ID
     * @param key 变量key
     * @param value 变量值
     */
    void generateClusterVariable(Map<String, String> globalVariables, Integer clusterId, String key, String value);
}