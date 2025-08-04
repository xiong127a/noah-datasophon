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
import com.datasophon.dao.entity.ClusterVariable;
import com.mybatisflex.core.service.IService;

import java.util.List;

/**
 * 集群变量服务接口
 * 提供集群变量的业务逻辑处理
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
public interface ClusterVariableService extends IService<ClusterVariable> {

    /**
     * 根据变量名和集群ID获取变量
     */
    ClusterVariableDTO getVariableByVariableName(String variableName, Integer clusterId);

    /**
     * 根据集群ID获取所有变量
     */
    List<ClusterVariableDTO> getVariablesByClusterId(Integer clusterId);

    /**
     * 保存或更新集群变量
     */
    ClusterVariableDTO saveOrUpdateVariable(ClusterVariableDTO dto);

    /**
     * 根据ID获取变量DTO
     */
    ClusterVariableDTO getByIdAsDto(Integer id);

    /**
     * 删除变量
     */
    boolean deleteVariable(Integer id);
}
