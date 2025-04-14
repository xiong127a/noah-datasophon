/*
 *
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
 *
 */

package com.datasophon.api.service;

import com.datasophon.common.model.ConfigFile;
import java.util.List;

/**
 * 服务配置文件服务接口
 */
public interface ServiceConfigFileService {

    /**
     * 获取服务配置文件列表
     * 
     * @param serviceInstanceId 服务实例ID
     * @return 配置文件列表
     */
    List<ConfigFile> getServiceConfigFiles(Integer serviceInstanceId);

    /**
     * 获取配置文件内容
     * 
     * @param serviceInstanceId 服务实例ID
     * @param fileName          文件名
     * @return 文件内容
     */
    byte[] getServiceConfigFileContent(Integer serviceInstanceId, String fileName);

    /**
     * 获取所有配置文件并根据指定格式打包
     * 
     * @param serviceInstanceId 服务实例ID
     * @param format            压缩格式（zip, tar.gz, 7z）
     * @return 压缩文件内容
     */
    byte[] getAllServiceConfigFiles(Integer serviceInstanceId, String format);

    /**
     * 获取服务名称
     * 
     * @param serviceInstanceId 服务实例ID
     * @return 服务名称
     */
    String getServiceName(Integer serviceInstanceId);
}