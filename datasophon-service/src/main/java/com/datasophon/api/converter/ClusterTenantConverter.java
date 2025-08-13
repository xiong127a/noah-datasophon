/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.datasophon.api.converter;

import cn.hutool.core.collection.CollUtil;
import com.datasophon.common.converter.BaseConverter;
import com.datasophon.common.dto.ClusterTenantDTO;
import com.datasophon.common.utils.FormatterUtils;
import com.datasophon.common.vo.ClusterTenantVO;
import com.datasophon.dao.entity.ClusterTenantEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.util.ArrayList;
import java.util.List;

/**
 * 集群租户转换器
 * 负责Entity、DTO和VO之间的转换，处理复杂资源列表和统计信息
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
@Mapper(componentModel = "spring", uses = FormatterUtils.class,
        unmappedSourcePolicy = ReportingPolicy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ClusterTenantConverter extends BaseConverter<ClusterTenantEntity, ClusterTenantDTO, ClusterTenantVO> {

    /**
     * Entity转DTO，忽略资源列表映射
     */
    @Override
    @Mapping(target = "hdfsResourceList", ignore = true)
    @Mapping(target = "yarnResourceList", ignore = true)
    @Mapping(target = "hiveResourceList", ignore = true)
    @Mapping(target = "hbaseResourceList", ignore = true)
    @Mapping(target = "kafkaResourceList", ignore = true)
    ClusterTenantDTO entityToDto(ClusterTenantEntity entity);

    /**
     * DTO转Entity，忽略资源列表映射
     */
    @Override
    @Mapping(target = "hdfsResourceList", ignore = true)
    @Mapping(target = "yarnResourceList", ignore = true)
    @Mapping(target = "hiveResourceList", ignore = true)
    @Mapping(target = "hbaseResourceList", ignore = true)
    @Mapping(target = "kafkaResourceList", ignore = true)
    ClusterTenantEntity dtoToEntity(ClusterTenantDTO dto);

    /**
     * Entity转VO，忽略资源列表映射，添加统计信息
     */
    @Override
    @Mapping(target = "hdfsResourceList", ignore = true)
    @Mapping(target = "yarnResourceList", ignore = true)
    @Mapping(target = "hiveResourceList", ignore = true)
    @Mapping(target = "hbaseResourceList", ignore = true)
    @Mapping(target = "kafkaResourceList", ignore = true)
    @Mapping(target = "totalResourceCount", source = ".", qualifiedByName = "calculateTotalResourceCount")
    @Mapping(target = "resourceSummary", source = ".", qualifiedByName = "generateResourceSummary")
    ClusterTenantVO entityToVo(ClusterTenantEntity entity);

    /**
     * DTO转VO，忽略资源列表映射，添加统计信息
     */
    @Override
    @Mapping(target = "hdfsResourceList", ignore = true)
    @Mapping(target = "yarnResourceList", ignore = true)
    @Mapping(target = "hiveResourceList", ignore = true)
    @Mapping(target = "hbaseResourceList", ignore = true)
    @Mapping(target = "kafkaResourceList", ignore = true)
    @Mapping(target = "totalResourceCount", ignore = true)
    @Mapping(target = "resourceSummary", ignore = true)
    ClusterTenantVO dtoToVo(ClusterTenantDTO dto);

    /**
     * 更新Entity从DTO，忽略资源列表映射
     */
    @Override
    @Mapping(target = "hdfsResourceList", ignore = true)
    @Mapping(target = "yarnResourceList", ignore = true)
    @Mapping(target = "hiveResourceList", ignore = true)
    @Mapping(target = "hbaseResourceList", ignore = true)
    @Mapping(target = "kafkaResourceList", ignore = true)
    void updateEntityFromDto(ClusterTenantDTO dto, @MappingTarget ClusterTenantEntity entity);

    /**
     * 计算总资源数量（基于Entity）
     */
    @Named("calculateTotalResourceCount")
    default Integer calculateTotalResourceCount(ClusterTenantEntity entity) {
        if (entity == null) {
            return 0;
        }
        int count = 0;
        count += CollUtil.size(entity.getHdfsResourceList());
        count += CollUtil.size(entity.getYarnResourceList());
        count += CollUtil.size(entity.getHiveResourceList());
        count += CollUtil.size(entity.getHbaseResourceList());
        count += CollUtil.size(entity.getKafkaResourceList());
        return count;
    }

    /**
     * 生成资源摘要（基于Entity）
     */
    @Named("generateResourceSummary")
    default String generateResourceSummary(ClusterTenantEntity entity) {
        if (entity == null) {
            return "无资源";
        }

        List<String> summaryParts = new ArrayList<>();
        if (CollUtil.isNotEmpty(entity.getHdfsResourceList())) {
            summaryParts.add("HDFS:" + entity.getHdfsResourceList().size());
        }
        if (CollUtil.isNotEmpty(entity.getYarnResourceList())) {
            summaryParts.add("Yarn:" + entity.getYarnResourceList().size());
        }
        if (CollUtil.isNotEmpty(entity.getHiveResourceList())) {
            summaryParts.add("Hive:" + entity.getHiveResourceList().size());
        }
        if (CollUtil.isNotEmpty(entity.getHbaseResourceList())) {
            summaryParts.add("HBase:" + entity.getHbaseResourceList().size());
        }
        if (CollUtil.isNotEmpty(entity.getKafkaResourceList())) {
            summaryParts.add("Kafka:" + entity.getKafkaResourceList().size());
        }

        return summaryParts.isEmpty() ? "无资源" : String.join(", ", summaryParts);
    }
}