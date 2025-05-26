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

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datasophon.api.service.ConfigVersionInfoService;
import com.datasophon.dao.entity.ConfigVersionInfoEntity;
import com.datasophon.dao.mapper.ConfigVersionInfoMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 配置版本详情服务实现类
 *
 * @author datasophon
 */
@Service("configVersionInfoService")
public class ConfigVersionInfoServiceImpl extends ServiceImpl<ConfigVersionInfoMapper, ConfigVersionInfoEntity>
                implements ConfigVersionInfoService {

        @Override
        public List<ConfigVersionInfoEntity> getVersionInfoList(String refType, Integer refId) {
                LambdaQueryWrapper<ConfigVersionInfoEntity> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(ConfigVersionInfoEntity::getRefType, refType)
                                .eq(ConfigVersionInfoEntity::getRefId, refId)
                                .orderByDesc(ConfigVersionInfoEntity::getVersion);
                return this.list(queryWrapper);
        }

        @Override
        public ConfigVersionInfoEntity getVersionInfo(Integer version, String refType, Integer refId) {
                LambdaQueryWrapper<ConfigVersionInfoEntity> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(ConfigVersionInfoEntity::getVersion, version)
                                .eq(ConfigVersionInfoEntity::getRefType, refType)
                                .eq(ConfigVersionInfoEntity::getRefId, refId);
                return this.getOne(queryWrapper);
        }

        @Override
        public Integer getMaxVersion(String refType, Integer refId) {
                LambdaQueryWrapper<ConfigVersionInfoEntity> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(ConfigVersionInfoEntity::getRefType, refType)
                                .eq(ConfigVersionInfoEntity::getRefId, refId)
                                .orderByDesc(ConfigVersionInfoEntity::getVersion)
                                .last("LIMIT 1");

                ConfigVersionInfoEntity latestVersion = this.getOne(queryWrapper);
                return latestVersion != null ? latestVersion.getVersion() : 0;
        }

        @Override
        @Transactional(rollbackFor = Exception.class)
        public boolean updateCurrentVersion(Integer version, String refType, Integer refId) {
                // 先将所有版本设置为非当前版本
                LambdaUpdateWrapper<ConfigVersionInfoEntity> updateWrapper = new LambdaUpdateWrapper<>();
                updateWrapper.eq(ConfigVersionInfoEntity::getRefType, refType)
                                .eq(ConfigVersionInfoEntity::getRefId, refId)
                                .set(ConfigVersionInfoEntity::getIsCurrent, false);
                this.update(updateWrapper);

                // 再将指定版本设置为当前版本
                updateWrapper = new LambdaUpdateWrapper<>();
                updateWrapper.eq(ConfigVersionInfoEntity::getVersion, version)
                                .eq(ConfigVersionInfoEntity::getRefType, refType)
                                .eq(ConfigVersionInfoEntity::getRefId, refId)
                                .set(ConfigVersionInfoEntity::getIsCurrent, true);
                return this.update(updateWrapper);
        }
}