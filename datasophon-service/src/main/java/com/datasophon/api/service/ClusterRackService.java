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

import com.datasophon.common.dto.ClusterRackDTO;
import com.datasophon.dao.entity.ClusterRack;
import com.mybatisflex.core.service.IService;

import java.util.List;

/**
 * 集群机架服务接口
 * 提供集群机架的业务逻辑处理
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
public interface ClusterRackService extends IService<ClusterRack> {

    /**
     * 查询集群机架
     */
    List<ClusterRackDTO> queryClusterRack(Integer clusterId);

    /**
     * 保存机架
     */
    ClusterRackDTO saveRack(Integer clusterId, String rack);

    /**
     * 删除机架
     */
    boolean deleteRack(Integer rackId);

    /**
     * 创建默认机架
     */
    void createDefaultRack(Integer clusterId);

    /**
     * 根据ID获取机架DTO
     */
    ClusterRackDTO getByIdAsDto(Integer id);

    /**
     * 保存机架DTO
     */
    ClusterRackDTO saveRackDto(ClusterRackDTO dto);

    /**
     * 更新机架
     */
    void updateRack(ClusterRackDTO dto);
}
