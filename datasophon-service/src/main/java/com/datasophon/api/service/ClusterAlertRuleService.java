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

import com.datasophon.common.dto.ClusterAlertRuleDTO;
import com.datasophon.dao.entity.ClusterAlertRule;
import com.mybatisflex.core.service.IService;

/**
 * 集群告警规则服务接口
 * 提供集群告警规则的业务逻辑处理
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
public interface ClusterAlertRuleService extends IService<ClusterAlertRule> {

    /**
     * 根据ID获取告警规则DTO
     *
     * @param id 告警规则ID
     * @return 告警规则DTO
     */
    ClusterAlertRuleDTO getByIdAsDto(Long id);

    /**
     * 保存告警规则
     *
     * @param dto 告警规则DTO
     */
    void saveAlertRule(ClusterAlertRuleDTO dto);

    /**
     * 更新告警规则
     *
     * @param dto 告警规则DTO
     */
    void updateAlertRule(ClusterAlertRuleDTO dto);
}
