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

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.datasophon.common.converter.BaseConverter;
import com.datasophon.common.dto.ClusterTenantDTO;
import com.datasophon.common.utils.FormatterUtils;
import com.datasophon.common.vo.ClusterTenantVO;
import com.datasophon.dao.entity.ClusterTenant;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 集群租户转换器
 * 负责Entity、DTO和VO之间的转换，处理复杂资源列表和统计信息
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
@Mapper(componentModel = "spring", uses = FormatterUtils.class)
public interface ClusterTenantConverter extends BaseConverter<ClusterTenant, ClusterTenantDTO, ClusterTenantVO> {

    @Override
    @Named("entityToDto")
    @Mapping(target = "hdfsResourceList", source = "hdfsResourceList", qualifiedByName = "convertResourceToMap")
    @Mapping(target = "yarnResourceList", source = "yarnResourceList", qualifiedByName = "convertResourceToMap")
    @Mapping(target = "hiveResourceList", source = "hiveResourceList", qualifiedByName = "convertResourceToMap")
    @Mapping(target = "hbaseResourceList", source = "hbaseResourceList", qualifiedByName = "convertResourceToMap")
    @Mapping(target = "kafkaResourceList", source = "kafkaResourceList", qualifiedByName = "convertResourceToMap")
    ClusterTenantDTO entityToDto(ClusterTenant entity);

    @Override
    @Named("dtoToEntity")
    @Mapping(target = "hdfsResourceList", source = "hdfsResourceList", qualifiedByName = "convertMapToResource")
    @Mapping(target = "yarnResourceList", source = "yarnResourceList", qualifiedByName = "convertMapToResource")
    @Mapping(target = "hiveResourceList", source = "hiveResourceList", qualifiedByName = "convertMapToResource")
    @Mapping(target = "hbaseResourceList", source = "hbaseResourceList", qualifiedByName = "convertMapToResource")
    @Mapping(target = "kafkaResourceList", source = "kafkaResourceList", qualifiedByName = "convertMapToResource")
    ClusterTenant dtoToEntity(ClusterTenantDTO dto);

    @Override
    @Named("entityToVo")
    @Mapping(target = "hdfsResourceList", source = "hdfsResourceList", qualifiedByName = "convertResourceToMap")
    @Mapping(target = "yarnResourceList", source = "yarnResourceList", qualifiedByName = "convertResourceToMap")
    @Mapping(target = "hiveResourceList", source = "hiveResourceList", qualifiedByName = "convertResourceToMap")
    @Mapping(target = "hbaseResourceList", source = "hbaseResourceList", qualifiedByName = "convertResourceToMap")
    @Mapping(target = "kafkaResourceList", source = "kafkaResourceList", qualifiedByName = "convertResourceToMap")
    @Mapping(target = "totalResourceCount", source = ".", qualifiedByName = "calculateTotalResourceCount")
    @Mapping(target = "resourceSummary", source = ".", qualifiedByName = "generateResourceSummary")
    ClusterTenantVO entityToVo(ClusterTenant entity);

    @Override
    @Named("dtoToVo")
    @Mapping(target = "totalResourceCount", source = ".", qualifiedByName = "calculateTotalResourceCountFromDto")
    @Mapping(target = "resourceSummary", source = ".", qualifiedByName = "generateResourceSummaryFromDto")
    ClusterTenantVO dtoToVo(ClusterTenantDTO dto);

    /**
     * 计算总资源数量（基于Entity）
     */
    @Named("calculateTotalResourceCount")
    default Integer calculateTotalResourceCount(ClusterTenant entity) {
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
     * 计算总资源数量（基于DTO）
     */
    @Named("calculateTotalResourceCountFromDto")
    default Integer calculateTotalResourceCountFromDto(ClusterTenantDTO dto) {
        if (dto == null) {
            return 0;
        }
        int count = 0;
        if (dto.hdfsResourceList() != null)
            count += dto.hdfsResourceList().size();
        if (dto.yarnResourceList() != null)
            count += dto.yarnResourceList().size();
        if (dto.hiveResourceList() != null)
            count += dto.hiveResourceList().size();
        if (dto.hbaseResourceList() != null)
            count += dto.hbaseResourceList().size();
        if (dto.kafkaResourceList() != null)
            count += dto.kafkaResourceList().size();
        return count;
    }

    /**
     * 生成资源摘要（基于Entity）
     */
    @Named("generateResourceSummary")
    default String generateResourceSummary(ClusterTenant entity) {
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

    /**
     * 生成资源摘要（基于DTO）
     */
    @Named("generateResourceSummaryFromDto")
    default String generateResourceSummaryFromDto(ClusterTenantDTO dto) {
        if (dto == null) {
            return "无资源";
        }

        List<String> summaryParts = new ArrayList<>();
        if (dto.hdfsResourceList() != null && !dto.hdfsResourceList().isEmpty()) {
            summaryParts.add("HDFS:" + dto.hdfsResourceList().size());
        }
        if (dto.yarnResourceList() != null && !dto.yarnResourceList().isEmpty()) {
            summaryParts.add("Yarn:" + dto.yarnResourceList().size());
        }
        if (dto.hiveResourceList() != null && !dto.hiveResourceList().isEmpty()) {
            summaryParts.add("Hive:" + dto.hiveResourceList().size());
        }
        if (dto.hbaseResourceList() != null && !dto.hbaseResourceList().isEmpty()) {
            summaryParts.add("HBase:" + dto.hbaseResourceList().size());
        }
        if (dto.kafkaResourceList() != null && !dto.kafkaResourceList().isEmpty()) {
            summaryParts.add("Kafka:" + dto.kafkaResourceList().size());
        }

        return summaryParts.isEmpty() ? "无资源" : String.join(", ", summaryParts);
    }

    /**
     * 将资源列表转换为Map（简化转换）
     */
    @Named("convertResourceToMap")
    default Map<String, Object> convertResourceToMap(Object resourceList) {
        return BeanUtil.beanToMap(resourceList);
    }

    /**
     * 将Map转换为资源列表（简化转换）
     */
    @Named("convertMapToResource")
    default Object convertMapToResource(Map<String, Object> resourceMap) {
        return resourceMap;
    }
}