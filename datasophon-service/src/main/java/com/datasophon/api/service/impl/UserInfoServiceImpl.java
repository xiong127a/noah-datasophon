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

import cn.hutool.core.collection.CollUtil;
import com.datasophon.api.enums.Status;
import com.datasophon.api.service.UserInfoService;
import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.UserInfoEntity;
import com.datasophon.dao.mapper.UserInfoMapper;
import com.mybatisflex.core.query.QueryChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service("userInfoService")
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfoEntity> implements UserInfoService {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public UserInfoEntity queryUser(String username, String password) {
        String md5 = passwordEncoder.encode(password);
        return QueryChain.of(UserInfoEntity.class)
                .where(UserInfoEntity::getUsername).eq(username)
                .and(UserInfoEntity::getPassword).eq(md5)
                .one();
    }

    @Override
    public Result createUser(UserInfoEntity userInfo) {
        // 用户名判重
        List<UserInfoEntity> list = QueryChain.of(UserInfoEntity.class)
                .where(UserInfoEntity::getUsername).eq(userInfo.getUsername())
                .list();

        if (CollUtil.isNotEmpty(list)) {
            return Result.error(Status.USER_NAME_EXIST.getCode(), Status.USER_NAME_EXIST.getMsg());
        }
        
        // 设置基本信息
        userInfo.setCreateTime(new Date());
        userInfo.setPassword(passwordEncoder.encode(userInfo.getPassword()));
        
        // 设置新字段的默认值
        if (userInfo.getUserType() == null) {
            userInfo.setUserType(2); // 默认为普通用户
        }
        
        // bio和avatar字段如果为空，保持为null（数据库默认值）
        
        this.save(userInfo);
        return Result.success();
    }

    @Override
    public Result updateUser(UserInfoEntity userInfo) {
        // 用户名判重
        List<UserInfoEntity> list = QueryChain.of(UserInfoEntity.class)
                .where(UserInfoEntity::getUsername).eq(userInfo.getUsername())
                .list();

        if (CollUtil.isNotEmpty(list)) {
            UserInfoEntity userInfoEntity = list.getFirst();
            if (!userInfoEntity.getId().equals(userInfo.getId())) {
                return Result.error(Status.USER_NAME_EXIST.getCode(), Status.USER_NAME_EXIST.getMsg());
            }
        }
        
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
        
        this.updateById(userInfo);

        return Result.success();
    }

    @Override
    public Result getUserListByPage(String username, Integer page, Integer pageSize) {
        int offset = (page - 1) * pageSize;

        QueryChain<UserInfoEntity> query = QueryChain.of(UserInfoEntity.class);
        if (StringUtils.isNotBlank(username)) {
            query.where(UserInfoEntity::getUsername).like("%" + username + "%");
        }

        List<UserInfoEntity> list = query.limit(offset, pageSize).list();
        long total = query.count();

        // 直接使用Result构造方法，将数据和总数设置到正确的字段中
        return Result.success(list, total);
    }

    @Override
    public UserInfoEntity getUserByUsername(String username) {
        return QueryChain.of(UserInfoEntity.class)
                .where(UserInfoEntity::getUsername).eq(username)
                .one();
    }

    @Override
    public boolean checkUsernameExists(String username, Integer excludeId) {
        QueryChain<UserInfoEntity> query = QueryChain.of(UserInfoEntity.class)
                .where(UserInfoEntity::getUsername).eq(username);
        
        // 如果提供了excludeId，则排除该用户
        if (excludeId != null) {
            query.and(UserInfoEntity::getId).ne(excludeId);
        }
        
        return query.exists();
    }
}
