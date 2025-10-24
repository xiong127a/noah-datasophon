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

import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSON;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.api.service.ParcelRepositoryService;
import com.datasophon.common.Constants;
import com.datasophon.common.dto.ParcelRepositoryDTO;
import com.datasophon.dao.entity.ClusterInfoEntity;
import com.datasophon.dao.entity.ParcelRepositoryEntity;
import com.datasophon.dao.mapper.ParcelRepositoryMapper;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static com.datasophon.dao.entity.table.ParcelRepositoryEntityTableDef.PARCEL_REPOSITORY_ENTITY;
import static com.datasophon.dao.entity.table.ClusterInfoEntityTableDef.CLUSTER_INFO_ENTITY;

/**
 * Parcel存储库服务实现类
 * 
 * @author datasophon
 * @date 2025-10-24
 */
@Slf4j
@Service
public class ParcelRepositoryServiceImpl implements ParcelRepositoryService {

    @Autowired
    private ParcelRepositoryMapper parcelRepositoryMapper;

    @Autowired
    private ClusterInfoService clusterInfoService;

    @Override
    public List<ParcelRepositoryDTO> list() {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .select()
                .from(PARCEL_REPOSITORY_ENTITY)
                .orderBy(PARCEL_REPOSITORY_ENTITY.IS_DEFAULT.desc(), PARCEL_REPOSITORY_ENTITY.CREATE_TIME.desc());

        List<ParcelRepositoryEntity> entities = parcelRepositoryMapper.selectListByQuery(queryWrapper);
        return entities.stream()
                .map(this::entityToDto)
                .collect(Collectors.toList());
    }

    @Override
    public ParcelRepositoryDTO getById(Long id) {
        ParcelRepositoryEntity entity = parcelRepositoryMapper.selectOneById(id);
        if (entity == null) {
            throw new IllegalArgumentException("存储库不存在: " + id);
        }
        return entityToDto(entity);
    }

    @Override
    public ParcelRepositoryDTO getDefaultRepository() {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .select()
                .from(PARCEL_REPOSITORY_ENTITY)
                .where(PARCEL_REPOSITORY_ENTITY.IS_DEFAULT.eq(1))
                .and(PARCEL_REPOSITORY_ENTITY.STATUS.eq(1))
                .limit(1);

        ParcelRepositoryEntity entity = parcelRepositoryMapper.selectOneByQuery(queryWrapper);
        if (entity == null) {
            // 如果没有找到默认存储库，创建一个
            log.warn("未找到默认存储库，将创建默认本地存储库");
            return createDefaultLocalRepository();
        }
        return entityToDto(entity);
    }

    @Override
    public ParcelRepositoryDTO getClusterRepository(Long clusterId) {
        // 获取集群信息
        ClusterInfoEntity cluster = clusterInfoService.getById(clusterId);
        if (cluster == null) {
            throw new IllegalArgumentException("集群不存在: " + clusterId);
        }

        // 如果集群没有关联存储库，使用默认存储库
        if (cluster.getRepositoryId() == null) {
            log.info("集群 {} 未关联存储库，使用默认存储库", clusterId);
            return getDefaultRepository();
        }

        return getById(cluster.getRepositoryId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ParcelRepositoryDTO create(ParcelRepositoryDTO dto) {
        // 验证存储库名称唯一性
        QueryWrapper queryWrapper = QueryWrapper.create()
                .select()
                .from(PARCEL_REPOSITORY_ENTITY)
                .where(PARCEL_REPOSITORY_ENTITY.REPO_NAME.eq(dto.getRepoName()));

        if (parcelRepositoryMapper.selectOneByQuery(queryWrapper) != null) {
            throw new IllegalArgumentException("存储库名称已存在: " + dto.getRepoName());
        }

        // 如果设置为默认存储库，需要将其他存储库的默认标记取消
        if (dto.getIsDefault() != null && dto.getIsDefault() == 1) {
            clearDefaultFlag();
        }

        ParcelRepositoryEntity entity = new ParcelRepositoryEntity();
        BeanUtils.copyProperties(dto, entity);
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());

        // 默认值设置
        if (entity.getStatus() == null) {
            entity.setStatus(1); // 默认启用
        }
        if (entity.getIsDefault() == null) {
            entity.setIsDefault(0); // 默认非默认存储库
        }

        parcelRepositoryMapper.insert(entity);
        log.info("创建存储库成功: {}", entity.getRepoName());
        return entityToDto(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ParcelRepositoryDTO update(ParcelRepositoryDTO dto) {
        if (dto.getId() == null) {
            throw new IllegalArgumentException("存储库ID不能为空");
        }

        ParcelRepositoryEntity existingEntity = parcelRepositoryMapper.selectOneById(dto.getId());
        if (existingEntity == null) {
            throw new IllegalArgumentException("存储库不存在: " + dto.getId());
        }

        // 如果设置为默认存储库，需要将其他存储库的默认标记取消
        if (dto.getIsDefault() != null && dto.getIsDefault() == 1 
                && (existingEntity.getIsDefault() == null || existingEntity.getIsDefault() != 1)) {
            clearDefaultFlag();
        }

        // 更新字段
        if (dto.getRepoName() != null) {
            // 验证名称唯一性（排除自己）
            QueryWrapper queryWrapper = QueryWrapper.create()
                    .select()
                    .from(PARCEL_REPOSITORY_ENTITY)
                    .where(PARCEL_REPOSITORY_ENTITY.REPO_NAME.eq(dto.getRepoName()))
                    .and(PARCEL_REPOSITORY_ENTITY.ID.ne(dto.getId()));

            if (parcelRepositoryMapper.selectOneByQuery(queryWrapper) != null) {
                throw new IllegalArgumentException("存储库名称已存在: " + dto.getRepoName());
            }
            existingEntity.setRepoName(dto.getRepoName());
        }
        if (dto.getRepoUrl() != null) {
            existingEntity.setRepoUrl(dto.getRepoUrl());
        }
        if (dto.getFrameCode() != null) {
            existingEntity.setFrameCode(dto.getFrameCode());
        }
        if (dto.getDescription() != null) {
            existingEntity.setDescription(dto.getDescription());
        }
        if (dto.getIsDefault() != null) {
            existingEntity.setIsDefault(dto.getIsDefault());
        }
        if (dto.getStatus() != null) {
            existingEntity.setStatus(dto.getStatus());
        }
        existingEntity.setUpdateTime(LocalDateTime.now());

        parcelRepositoryMapper.update(existingEntity);
        log.info("更新存储库成功: {}", existingEntity.getRepoName());
        return entityToDto(existingEntity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean delete(Long id) {
        ParcelRepositoryEntity entity = parcelRepositoryMapper.selectOneById(id);
        if (entity == null) {
            throw new IllegalArgumentException("存储库不存在: " + id);
        }

        // 检查是否为默认本地存储库（不允许删除）
        if ("local".equalsIgnoreCase(entity.getRepoType()) 
                && entity.getIsDefault() != null && entity.getIsDefault() == 1) {
            throw new IllegalStateException("不允许删除默认本地存储库");
        }

        // 检查是否有集群在使用
        QueryWrapper queryWrapper = QueryWrapper.create()
                .select()
                .from(CLUSTER_INFO_ENTITY)
                .where(CLUSTER_INFO_ENTITY.REPOSITORY_ID.eq(id));

        long count = clusterInfoService.count(queryWrapper);
        if (count > 0) {
            throw new IllegalStateException("存储库正在被 " + count + " 个集群使用，无法删除");
        }

        parcelRepositoryMapper.deleteById(id);
        log.info("删除存储库成功: {}", entity.getRepoName());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean setDefault(Long id) {
        ParcelRepositoryEntity entity = parcelRepositoryMapper.selectOneById(id);
        if (entity == null) {
            throw new IllegalArgumentException("存储库不存在: " + id);
        }

        // 清除其他存储库的默认标记
        clearDefaultFlag();

        // 设置当前存储库为默认
        entity.setIsDefault(1);
        entity.setUpdateTime(LocalDateTime.now());
        parcelRepositoryMapper.update(entity);

        log.info("设置默认存储库成功: {}", entity.getRepoName());
        return true;
    }

    @Override
    public String testConnection(String url) {
        try {
            log.info("测试存储库连接: {}", url);
            
            // 发送HEAD请求测试连接（比GET更轻量）
            int statusCode = cn.hutool.http.HttpRequest.head(url)
                    .timeout(5000)
                    .execute()
                    .getStatus();
            
            if (statusCode == 200) {
                log.info("存储库连接测试成功: {} (状态码: {})", url, statusCode);
                return "连接成功";
            } else {
                log.warn("存储库连接返回非200状态码: {} (状态码: {})", url, statusCode);
                return "连接失败: HTTP状态码 " + statusCode;
            }
        } catch (Exception e) {
            log.error("存储库连接测试失败: {}", url, e);
            return "连接失败: " + e.getMessage();
        }
    }

    @Override
    public String getPackagePath(Long repositoryId, String packageName) {
        ParcelRepositoryDTO repo = getById(repositoryId);
        
        if (repo.isLocal()) {
            // 本地存储库：返回文件系统路径
            return repo.getRepoUrl() + "/" + packageName;
        } else if (repo.isHttp()) {
            // HTTP存储库：返回HTTP URL
            String url = repo.getRepoUrl();
            return url.endsWith("/") ? url + packageName : url + "/" + packageName;
        }
        
        throw new IllegalStateException("不支持的存储库类型: " + repo.getRepoType());
    }

    /**
     * Entity转DTO
     */
    private ParcelRepositoryDTO entityToDto(ParcelRepositoryEntity entity) {
        ParcelRepositoryDTO dto = new ParcelRepositoryDTO();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    /**
     * 清除所有存储库的默认标记
     */
    private void clearDefaultFlag() {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .select()
                .from(PARCEL_REPOSITORY_ENTITY)
                .where(PARCEL_REPOSITORY_ENTITY.IS_DEFAULT.eq(1));

        List<ParcelRepositoryEntity> defaultRepos = parcelRepositoryMapper.selectListByQuery(queryWrapper);
        for (ParcelRepositoryEntity repo : defaultRepos) {
            repo.setIsDefault(0);
            repo.setUpdateTime(LocalDateTime.now());
            parcelRepositoryMapper.update(repo);
        }
    }

    /**
     * 创建默认本地存储库
     */
    private ParcelRepositoryDTO createDefaultLocalRepository() {
        ParcelRepositoryDTO dto = ParcelRepositoryDTO.builder()
                .repoName("本地存储库")
                .repoType(Constants.REPO_TYPE_LOCAL)
                .repoUrl("/opt/datasophon/DDP/packages")
                .description("Master节点本地存储库")
                .isDefault(1)
                .status(1)
                .build();
        
        return create(dto);
    }
}

