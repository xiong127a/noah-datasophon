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

package com.datasophon.dao.mapper;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateChain;
import com.datasophon.dao.entity.ConfigVersionInfoEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

import static com.datasophon.dao.entity.table.ConfigVersionInfoEntityTableDef.CONFIG_VERSION_INFO_ENTITY;

/**
 * 配置版本详情Mapper
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-01
 */
@Mapper
public interface ConfigVersionInfoMapper extends BaseMapper<ConfigVersionInfoEntity> {

    /**
     * 根据引用类型和引用ID获取配置版本详情列表（按版本号降序）
     */
    default List<ConfigVersionInfoEntity> selectVersionInfoList(String refType, Integer refId) {
        return selectListByQuery(QueryWrapper.create()
                .where(CONFIG_VERSION_INFO_ENTITY.REF_TYPE.eq(refType))
                .and(CONFIG_VERSION_INFO_ENTITY.REF_ID.eq(refId))
                .orderBy(CONFIG_VERSION_INFO_ENTITY.VERSION.desc()));
    }

    /**
     * 根据版本号、引用类型和引用ID获取配置版本详情
     */
    default ConfigVersionInfoEntity selectVersionInfo(Integer version, String refType, Integer refId) {
        return selectOneByQuery(QueryWrapper.create()
                .where(CONFIG_VERSION_INFO_ENTITY.VERSION.eq(version))
                .and(CONFIG_VERSION_INFO_ENTITY.REF_TYPE.eq(refType))
                .and(CONFIG_VERSION_INFO_ENTITY.REF_ID.eq(refId)));
    }

    /**
     * 获取指定引用类型的最大版本号对应的版本信息
     */
    default ConfigVersionInfoEntity selectLatestVersion(String refType, Integer refId) {
        return selectOneByQuery(QueryWrapper.create()
                .where(CONFIG_VERSION_INFO_ENTITY.REF_TYPE.eq(refType))
                .and(CONFIG_VERSION_INFO_ENTITY.REF_ID.eq(refId))
                .orderBy(CONFIG_VERSION_INFO_ENTITY.VERSION.desc())
                .limit(1));
    }

    /**
     * 将指定引用类型和引用ID的所有版本设置为非当前版本
     */
    default boolean updateAllToNonCurrent(String refType, Integer refId) {
        return UpdateChain.of(ConfigVersionInfoEntity.class)
                .set(ConfigVersionInfoEntity::getIsCurrent, false)
                .where(ConfigVersionInfoEntity::getRefType).eq(refType)
                .and(ConfigVersionInfoEntity::getRefId).eq(refId)
                .update();
    }

    /**
     * 将指定版本设置为当前版本
     */
    default boolean updateToCurrent(Integer version, String refType, Integer refId) {
        return UpdateChain.of(ConfigVersionInfoEntity.class)
                .set(ConfigVersionInfoEntity::getIsCurrent, true)
                .where(ConfigVersionInfoEntity::getVersion).eq(version)
                .and(ConfigVersionInfoEntity::getRefType).eq(refType)
                .and(ConfigVersionInfoEntity::getRefId).eq(refId)
                .update();
    }
}