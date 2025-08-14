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

import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.datasophon.api.service.ConfigVersionInfoService;
import com.datasophon.dao.entity.ConfigVersionInfoEntity;
import com.datasophon.dao.mapper.ConfigVersionInfoMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 配置版本详情服务实现类
 * 按照架构重构规范，迁移QueryChain/UpdateChain到DAO层
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-01
 */
@Service("configVersionInfoService")
public class ConfigVersionInfoServiceImpl extends ServiceImpl<ConfigVersionInfoMapper, ConfigVersionInfoEntity>
                implements ConfigVersionInfoService {

        private static final Logger logger = LoggerFactory.getLogger(ConfigVersionInfoServiceImpl.class);

        @Override
        public List<ConfigVersionInfoEntity> getVersionInfoList(String refType, Long refId) {
                return getMapper().selectVersionInfoList(refType, refId);
        }

        @Override
        public ConfigVersionInfoEntity getVersionInfo(Integer version, String refType, Long refId) {
                return getMapper().selectVersionInfo(version, refType, refId);
        }

        @Override
        public Integer getMaxVersion(String refType, Long refId) {
                ConfigVersionInfoEntity latestVersion = getMapper().selectLatestVersion(refType, refId);
                return latestVersion != null ? latestVersion.getVersion() : 0;
        }

        @Override
        @Transactional(rollbackFor = Exception.class)
        public void updateCurrentVersion(Integer version, String refType, Long refId) {
                // 先将所有版本设置为非当前版本
                getMapper().updateAllToNonCurrent(refType, refId);

                // 再将指定版本设置为当前版本
                getMapper().updateToCurrent(version, refType, refId);
        }
}