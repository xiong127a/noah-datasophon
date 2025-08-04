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

package com.datasophon.api.service.impl;

import com.datasophon.api.converter.ClusterAlertRuleConverter;
import com.datasophon.api.service.ClusterAlertRuleService;
import com.datasophon.common.dto.ClusterAlertRuleDTO;
import com.datasophon.dao.entity.ClusterAlertRule;
import com.datasophon.dao.mapper.ClusterAlertRuleMapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 集群告警规则服务实现类
 * 提供集群告警规则的业务逻辑处理
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
@Service("clusterAlertRuleService")
public class ClusterAlertRuleServiceImpl extends ServiceImpl<ClusterAlertRuleMapper, ClusterAlertRule>
                implements ClusterAlertRuleService {

        @Autowired
        private ClusterAlertRuleConverter clusterAlertRuleConverter;

        @Override
        public ClusterAlertRuleDTO getByIdAsDto(Long id) {
                // Service层：Entity → DTO转换
                ClusterAlertRule entity = this.getById(id);
                return clusterAlertRuleConverter.entityToDto(entity);
        }

        @Override
        public void saveAlertRule(ClusterAlertRuleDTO dto) {
                // Service层：DTO → Entity转换
                ClusterAlertRule entity = clusterAlertRuleConverter.dtoToEntity(dto);
                this.save(entity);
        }

        @Override
        public void updateAlertRule(ClusterAlertRuleDTO dto) {
                // Service层：DTO → Entity转换
                ClusterAlertRule entity = clusterAlertRuleConverter.dtoToEntity(dto);
                this.updateById(entity);
        }
}
