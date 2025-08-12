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

import com.datasophon.api.load.model.ServiceMetaConfig;
import com.datasophon.dao.entity.FrameServiceEntity;
import com.datasophon.common.model.ServiceConfig;
import java.util.List;

/**
 * 服务元数据事务处理服务
 * 专注于数据库操作，避免循环调用
 * 
 * 作者：任相鹏
 * 邮箱：635887935@qq.com  
 * 日期：2025-01-20
 */
public interface ServiceMetadataTransactionService {
    
    /**
     * 在事务中保存框架服务
     */
    FrameServiceEntity saveFrameServiceInTransaction(ServiceMetaConfig config);
    
    /**
     * 在事务中保存框架服务角色
     */
    void saveFrameServiceRoleInTransaction(ServiceMetaConfig config, FrameServiceEntity serviceEntity);
    
    /**
     * 在事务中更新服务实例配置
     */
    void updateServiceInstanceConfigInTransaction(String frameCode, String serviceName, List<ServiceConfig> parameters);
}
