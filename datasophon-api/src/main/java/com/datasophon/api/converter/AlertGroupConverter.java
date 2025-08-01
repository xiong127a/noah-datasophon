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

package com.datasophon.api.converter;

import com.datasophon.common.dto.AlertGroupDTO;
import com.datasophon.api.vo.AlertGroupVO;
import com.datasophon.dao.entity.AlertGroupEntity;
import com.datasophon.common.converter.BaseConverter;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * AlertGroup对象转换器
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-14
 */
@Mapper(componentModel = "spring")
public interface AlertGroupConverter extends BaseConverter<AlertGroupEntity, AlertGroupDTO, AlertGroupVO> {

    /**
     * Entity转DTO
     */
    @Override
    AlertGroupDTO entityToDto(AlertGroupEntity entity);

    /**
     * DTO转Entity
     */
    @Override
    AlertGroupEntity dtoToEntity(AlertGroupDTO dto);

    /**
     * DTO转VO
     */
    @Override
    @Mapping(target = "clusterName", ignore = true)
    @Mapping(target = "alertQuotaNumFormatted", source = "alertQuotaNum", qualifiedByName = "formatAlertQuotaNum")
    @Mapping(target = "createTimeFormatted", source = "createTime", qualifiedByName = "formatDateTime")
    AlertGroupVO dtoToVo(AlertGroupDTO dto);

    /**
     * Entity列表转DTO列表
     */
    @Override
    List<AlertGroupDTO> entityListToDtoList(List<AlertGroupEntity> entityList);

    /**
     * DTO列表转VO列表
     */
    @Override
    List<AlertGroupVO> dtoListToVoList(List<AlertGroupDTO> dtoList);

    /**
     * Entity转VO
     */
    @Override
    @Mapping(target = "clusterName", ignore = true)
    @Mapping(target = "alertQuotaNumFormatted", source = "alertQuotaNum", qualifiedByName = "formatAlertQuotaNum")
    @Mapping(target = "createTimeFormatted", source = "createTime", qualifiedByName = "formatDateTime")
    AlertGroupVO entityToVo(AlertGroupEntity entity);

    /**
     * Entity列表转VO列表
     */
    @Override
    List<AlertGroupVO> entityListToVoList(List<AlertGroupEntity> entityList);

    /**
     * 更新Entity对象
     */
    @Override
    void updateEntityFromDto(AlertGroupDTO dto, AlertGroupEntity entity);

    /**
     * 格式化告警指标数量
     */
    @Named("formatAlertQuotaNum")
    default String formatAlertQuotaNum(Integer alertQuotaNum) {
        if (alertQuotaNum == null) {
            return "0个指标";
        }
        return switch (alertQuotaNum) {
            case 0 -> "暂无指标";
            case 1 -> "1个指标";
            default -> alertQuotaNum + "个指标";
        };
    }

    /**
     * 格式化日期时间
     */
    @Named("formatDateTime")
    default String formatDateTime(Date date) {
        if (date == null) {
            return null;
        }
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return formatter.format(date);
    }
}