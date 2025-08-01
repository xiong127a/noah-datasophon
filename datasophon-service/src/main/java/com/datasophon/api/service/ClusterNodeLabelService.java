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


import com.datasophon.dao.entity.ClusterNodeLabelEntity;

import java.util.List;

/**
 * 集群节点标签服务
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2024-12-19
 */
public interface ClusterNodeLabelService {

    ClusterNodeLabelEntity saveNodeLabel(Integer clusterId, String nodeLabel);

    boolean deleteNodeLabel(Integer nodeLabelId);

    boolean assignNodeLabel(Integer nodeLabelId, String hostIds);

    List<ClusterNodeLabelEntity> queryClusterNodeLabel(Integer clusterId);

    void createDefaultNodeLabel(Integer clusterId);

    // 标准CRUD方法
    ClusterNodeLabelEntity getById(Integer id);

    ClusterNodeLabelEntity save(ClusterNodeLabelEntity entity);

    ClusterNodeLabelEntity updateById(ClusterNodeLabelEntity entity);

    boolean removeByIds(List<Integer> ids);

    List<ClusterNodeLabelEntity> getAllNodeLabels();
}
