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

import com.datasophon.common.dto.ParcelRepositoryDTO;

import java.util.List;

/**
 * Parcel存储库服务接口
 * 
 * @author datasophon
 * @date 2025-10-24
 */
public interface ParcelRepositoryService {

    /**
     * 获取所有存储库列表
     * 
     * @return 存储库列表
     */
    List<ParcelRepositoryDTO> list();

    /**
     * 根据ID获取存储库
     * 
     * @param id 存储库ID
     * @return 存储库信息
     */
    ParcelRepositoryDTO getById(Long id);

    /**
     * 获取默认存储库
     * 
     * @return 默认存储库信息
     */
    ParcelRepositoryDTO getDefaultRepository();

    /**
     * 获取集群关联的存储库
     * 
     * @param clusterId 集群ID
     * @return 存储库信息
     */
    ParcelRepositoryDTO getClusterRepository(Long clusterId);

    /**
     * 创建存储库
     * 
     * @param dto 存储库信息
     * @return 创建的存储库信息
     */
    ParcelRepositoryDTO create(ParcelRepositoryDTO dto);

    /**
     * 更新存储库
     * 
     * @param dto 存储库信息
     * @return 更新后的存储库信息
     */
    ParcelRepositoryDTO update(ParcelRepositoryDTO dto);

    /**
     * 删除存储库
     * 
     * @param id 存储库ID
     * @return 是否删除成功
     */
    boolean delete(Long id);

    /**
     * 设置默认存储库
     * 
     * @param id 存储库ID
     * @return 是否设置成功
     */
    boolean setDefault(Long id);

    /**
     * 测试存储库连接
     * 
     * @param url 存储库URL
     * @return 测试结果信息
     */
    String testConnection(String url);

    /**
     * 根据存储库ID和包名构建完整下载路径
     * 
     * @param repositoryId 存储库ID
     * @param packageName 包名
     * @return 完整下载路径
     */
    String getPackagePath(Long repositoryId, String packageName);
    
    /**
     * 列出存储库中的JDK文件
     * 
     * @param repositoryId 存储库ID
     * @return JDK文件列表
     */
    java.util.List<String> listJdkFiles(Long repositoryId);
}

