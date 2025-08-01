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

import com.datasophon.dao.entity.FrameServiceEntity;

import java.util.List;

/**
 * 集群框架版本服务表
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2024-12-19
 */
public interface FrameServiceService {

    List<FrameServiceEntity> getAllFrameService(Integer clusterId);

    List<FrameServiceEntity> getAllFrameServiceWithRequired(Integer clusterId, String type);

    List<FrameServiceEntity> getServiceListByServiceIds(List<Integer> serviceIds);

    FrameServiceEntity getServiceByFrameIdAndServiceName(Integer id, String serviceName);

    FrameServiceEntity getServiceByFrameCodeAndServiceName(String clusterFrame, String serviceName);

    List<FrameServiceEntity> getAllFrameServiceByFrameCode(String clusterFrame);

    List<FrameServiceEntity> listServices(String serviceIds);

    // 标准CRUD方法
    FrameServiceEntity getById(Integer id);

    FrameServiceEntity save(FrameServiceEntity entity);

    FrameServiceEntity updateById(FrameServiceEntity entity);

    boolean removeByIds(List<Integer> ids);
}
