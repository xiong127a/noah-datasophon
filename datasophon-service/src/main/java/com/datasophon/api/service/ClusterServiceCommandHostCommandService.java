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

import com.datasophon.dao.entity.ClusterServiceCommandHostCommandEntity;
import com.datasophon.common.model.PageResult;

import java.util.List;

/**
 * 集群服务操作指令主机指令表
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2024-12-19
 */
public interface ClusterServiceCommandHostCommandService {

    PageResult<ClusterServiceCommandHostCommandEntity> getHostCommandList(String hostname, String commandHostId,
            Integer page, Integer pageSize);

    List<ClusterServiceCommandHostCommandEntity> getHostCommandListByCommandId(String id);

    ClusterServiceCommandHostCommandEntity getByHostCommandId(String hostCommandId);

    void updateByHostCommandId(ClusterServiceCommandHostCommandEntity hostCommand);

    Long getHostCommandSizeByHostnameAndCommandHostId(String hostname, String commandHostId);

    Integer getHostCommandTotalProgressByHostnameAndCommandHostId(String hostname, String commandHostId);

    String getHostCommandLog(Integer clusterId, String hostCommandId) throws Exception;

    List<ClusterServiceCommandHostCommandEntity> findFailedHostCommand(String hostname, String commandHostId);

    List<ClusterServiceCommandHostCommandEntity> findCanceledHostCommand(String hostname, String commandHostId);

    // 标准CRUD方法
    ClusterServiceCommandHostCommandEntity getById(String id);

    ClusterServiceCommandHostCommandEntity save(ClusterServiceCommandHostCommandEntity entity);

    ClusterServiceCommandHostCommandEntity updateById(ClusterServiceCommandHostCommandEntity entity);

    boolean removeByIds(List<String> ids);

    List<ClusterServiceCommandHostCommandEntity> getAllHostCommands();
}
