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

import com.datasophon.common.dto.ClusterVariableDTO;
import com.datasophon.dao.entity.ClusterVariableEntity;
import com.mybatisflex.core.service.IService;

/**
 * 集群变量服务接口
 * 继承IService提供基础CRUD操作，使用DTO进行数据传输
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-01
 */
public interface ClusterVariableService extends IService<ClusterVariableEntity> {

    /**
     * 根据变量名和集群ID获取变量
     * 
     * @param variableName 变量名
     * @param clusterId 集群ID
     * @return 变量DTO，如果不存在返回null
     */
    ClusterVariableDTO getVariableByVariableName(String variableName, Long clusterId);
}