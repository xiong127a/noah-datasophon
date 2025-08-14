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

import cn.hutool.core.bean.BeanUtil;
import com.datasophon.api.service.UserInfoService;
import com.datasophon.common.dto.UserInfoDTO;
import com.datasophon.common.model.PageResult;
import com.datasophon.common.exception.UserBusinessException;
import com.datasophon.dao.entity.UserInfoEntity;
import com.datasophon.dao.mapper.UserInfoMapper;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

/**
 * 用户信息服务实现
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-01
 */
@Service("userInfoService")
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfoEntity> implements UserInfoService {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserInfoMapper userInfoMapper;

    @Override
    public UserInfoDTO createUser(UserInfoDTO userInfoDTO) {
        if (StringUtils.isBlank(userInfoDTO.getUsername())) {
            throw UserBusinessException.usernameIsNull();
        }

        // 用户名判重
        if (userInfoMapper.existsByUsername(userInfoDTO.getUsername())) {
            throw UserBusinessException.usernameExists(userInfoDTO.getUsername());
        }

        // DTO转Entity
        UserInfoEntity userInfo = dtoToEntity(userInfoDTO);

        // 设置基本信息
        userInfo.setCreateTime(LocalDateTime.now());
        userInfo.setPassword(passwordEncoder.encode(userInfo.getPassword()));

        // 设置新字段的默认值
        if (userInfo.getUserType() == null) {
            userInfo.setUserType(2); // 默认为普通用户
        }

        // 保存用户
        try {
            this.save(userInfo);
            return entityToDto(userInfo);
        } catch (Exception e) {
            throw UserBusinessException.createUserFailed(e.getMessage());
        }
    }

    @Override
    public UserInfoDTO updateUser(UserInfoDTO userInfoDTO) {
        if (userInfoDTO.getId() == null) {
            throw UserBusinessException.updateUserFailed("用户ID不能为空");
        }

        if (StringUtils.isBlank(userInfoDTO.getUsername())) {
            throw UserBusinessException.usernameIsNull();
        }

        // 用户名判重（排除自己）
        if (userInfoMapper.existsByUsernameExcludeId(userInfoDTO.getUsername(), userInfoDTO.getId())) {
            throw UserBusinessException.usernameExists(userInfoDTO.getUsername());
        }

        // DTO转Entity
        UserInfoEntity userInfo = dtoToEntity(userInfoDTO);

        // 只有当密码不为空时才更新密码
        String password = userInfo.getPassword();
        if (StringUtils.isNotBlank(password)) {
            userInfo.setPassword(passwordEncoder.encode(password));
        } else {
            // 如果密码为空，保持原密码不变
            UserInfoEntity existingUser = this.getById(userInfo.getId());
            if (existingUser != null) {
                userInfo.setPassword(existingUser.getPassword());
            }
        }

        try {
            this.updateById(userInfo);
            return entityToDto(userInfo);
        } catch (Exception e) {
            throw UserBusinessException.updateUserFailed(e.getMessage());
        }
    }

    @Override
    public PageResult<UserInfoDTO> getUserListByPage(String username, Integer page, Integer pageSize) {
        // 创建分页参数
        Page<UserInfoEntity> pageParam = Page.of(page, pageSize);

        // 调用DAO层分页查询
        Page<UserInfoEntity> pageResult = userInfoMapper.selectPageByUsername(pageParam, username);

        // Entity列表转DTO列表
        List<UserInfoDTO> dtoList = pageResult.getRecords().stream()
                .map(this::entityToDto)
                .toList();

        // 返回分页结果
        return PageResult.of(dtoList, pageResult.getTotalRow(), page, pageSize);
    }

    @Override
    public UserInfoDTO getUserByUsername(String username) {
        UserInfoEntity entity = userInfoMapper.selectByUsername(username);
        return entity != null ? entityToDto(entity) : null;
    }

    @Override
    public UserInfoEntity getUserEntityByUsername(String username) {
        return userInfoMapper.selectByUsername(username);
    }

    @Override
    public boolean checkUsernameExists(String username, Long excludeId) {
        if (excludeId != null) {
            return userInfoMapper.existsByUsernameExcludeId(username, excludeId);
        } else {
            return userInfoMapper.existsByUsername(username);
        }
    }

    @Override
    public UserInfoEntity getById(Long id) {
        return super.getById(id);
    }

    // ============ 私有转换方法 ============

    /**
     * Entity转DTO
     */
    private UserInfoDTO entityToDto(UserInfoEntity entity) {
        if (entity == null) {
            return null;
        }
        return BeanUtil.copyProperties(entity, UserInfoDTO.class);
    }

    /**
     * DTO转Entity
     */
    private UserInfoEntity dtoToEntity(UserInfoDTO dto) {
        if (dto == null) {
            return null;
        }
        return BeanUtil.copyProperties(dto, UserInfoEntity.class);
    }
}
