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

import com.mybatisflex.core.service.IService;
import com.datasophon.dao.entity.ConfigVersionInfoEntity;

import java.util.List;

/**
 * 配置版本详情服务接口
 *
 * @author datasophon
 */
public interface ConfigVersionInfoService extends IService<ConfigVersionInfoEntity> {

    /**
     * 根据引用类型和引用ID获取配置版本详情列表
     *
     * @param refType 引用类型(SERVICE/ROLE_GROUP)
     * @param refId   关联对象ID
     * @return 配置版本详情列表
     */
    List<ConfigVersionInfoEntity> getVersionInfoList(String refType, Integer refId);

    /**
     * 根据版本号、引用类型和引用ID获取配置版本详情
     *
     * @param version 版本号
     * @param refType 引用类型(SERVICE/ROLE_GROUP)
     * @param refId   关联对象ID
     * @return 配置版本详情
     */
    ConfigVersionInfoEntity getVersionInfo(Integer version, String refType, Integer refId);

    /**
     * 更新当前使用版本状态
     *
     * @param version 版本号
     * @param refType 引用类型(SERVICE/ROLE_GROUP)
     * @param refId   关联对象ID
     * @return 是否更新成功
     */
    boolean updateCurrentVersion(Integer version, String refType, Integer refId);

    /**
     * 获取指定引用类型的最大版本号
     *
     * @param refType 引用类型
     * @param refId   引用ID
     * @return 最大版本号，如果没有记录则返回0
     */
    Integer getMaxVersion(String refType, Integer refId);
}