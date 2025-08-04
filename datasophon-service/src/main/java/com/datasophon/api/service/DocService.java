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

import com.datasophon.common.dto.ServiceDocDTO;
import org.springframework.core.io.Resource;

/**
 * 文档服务接口
 * 按照架构重构规范，Service层不返回Result，抛出业务异常
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
public interface DocService {

    /**
     * 获取服务文档
     *
     * @param clusterId 集群ID
     * @param serviceId 服务ID
     * @param type      文档类型 (component: 组件介绍, guide: 用户指南, help: 帮助文档)
     * @return 文档DTO对象
     */
    ServiceDocDTO getServiceDoc(Integer clusterId, Integer serviceId, String type);

    /**
     * 获取文档中引用的图片资源
     *
     * @param imagePath 图片路径
     * @return 图片资源，不存在时抛出异常
     */
    Resource getImageResource(String imagePath);

    /**
     * 检查服务文档是否存在
     *
     * @param clusterId 集群ID
     * @param serviceId 服务ID
     * @param type      文档类型
     * @return 是否存在
     */
    boolean hasServiceDoc(Integer clusterId, Integer serviceId, String type);

    /**
     * 获取服务名称
     *
     * @param serviceId 服务ID
     * @return 服务名称
     */
    String getServiceName(Integer serviceId);
}