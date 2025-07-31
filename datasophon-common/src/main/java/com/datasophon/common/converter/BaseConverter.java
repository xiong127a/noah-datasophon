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

package com.datasophon.common.converter;

import org.mapstruct.MappingTarget;

import java.util.List;

/**
 * 基础转换器接口
 * 定义通用的对象转换方法
 * 
 * @param <E> Entity 数据库实体类型
 * @param <D> DTO 数据传输对象类型
 * @param <V> VO 视图对象类型
 * @author DataSophon
 */
public interface BaseConverter<E, D, V> {

    /**
     * Entity 转换为 DTO
     */
    D entityToDto(E entity);

    /**
     * DTO 转换为 Entity
     */
    E dtoToEntity(D dto);

    /**
     * Entity 转换为 VO
     */
    V entityToVo(E entity);

    /**
     * DTO 转换为 VO
     */
    V dtoToVo(D dto);

    /**
     * Entity 列表转换为 DTO 列表
     */
    List<D> entityListToDtoList(List<E> entityList);

    /**
     * Entity 列表转换为 VO 列表
     */
    List<V> entityListToVoList(List<E> entityList);

    /**
     * DTO 列表转换为 VO 列表
     */
    List<V> dtoListToVoList(List<D> dtoList);

    /**
     * 更新Entity对象（用于部分字段更新）
     */
    void updateEntityFromDto(D dto, @MappingTarget E entity);
}