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

import com.datasophon.common.dto.ClusterNodeLabelDTO;
import com.datasophon.dao.entity.ClusterNodeLabelEntity;
import com.mybatisflex.core.service.IService;

import java.util.List;

/**
 * 集群节点标签服务接口
 * 提供集群节点标签的业务逻辑处理
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
public interface ClusterNodeLabelService extends IService<ClusterNodeLabelEntity> {

    /**
     * 保存节点标签
     */
    ClusterNodeLabelDTO saveNodeLabel(Integer clusterId, String nodeLabel);

    /**
     * 删除节点标签
     */
    boolean deleteNodeLabel(Integer nodeLabelId);

    /**
     * 分配节点标签
     */
    boolean assignNodeLabel(Integer nodeLabelId, String hostIds);

    /**
     * 查询集群节点标签
     */
    List<ClusterNodeLabelDTO> queryClusterNodeLabel(Integer clusterId);

    /**
     * 创建默认节点标签
     */
    void createDefaultNodeLabel(Integer clusterId);

    /**
     * 根据ID获取节点标签DTO
     */
    ClusterNodeLabelDTO getByIdAsDto(Integer id);

    /**
     * 保存节点标签DTO
     */
    ClusterNodeLabelDTO saveNodeLabelDto(ClusterNodeLabelDTO dto);

    /**
     * 更新节点标签
     */
    void updateNodeLabel(ClusterNodeLabelDTO dto);
}
