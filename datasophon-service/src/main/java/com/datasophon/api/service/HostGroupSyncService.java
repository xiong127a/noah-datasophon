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

import com.datasophon.dao.entity.ClusterHostEntity;

import java.util.List;

/**
 * 主机用户组同步服务
 * 负责将用户组信息同步到各个主机
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-01
 */
public interface HostGroupSyncService {

    /**
     * 同步用户组到主机列表
     * 
     * @param hostList 主机列表
     * @param groupName 组名
     * @param command 执行命令（如groupadd）
     */
    void syncUserGroupToHosts(List<ClusterHostEntity> hostList, String groupName, String command);
}