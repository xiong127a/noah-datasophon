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
import com.datasophon.common.Constants;
import com.datasophon.common.utils.EncryptionUtils;
import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.UserInfoEntity;
import com.datasophon.dao.mapper.UserInfoMapper;
import com.mybatisflex.core.query.QueryChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service("userInfoService")
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfoEntity> implements UserInfoService {

    @Override
    public UserInfoEntity queryUser(String username, String password) {
        String md5 = EncryptionUtils.getMd5(password);
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
        userInfo.setCreateTime(new Date());
        userInfo.setPassword(EncryptionUtils.getMd5(userInfo.getPassword()));
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
        String password = userInfo.getPassword();
        userInfo.setPassword(EncryptionUtils.getMd5(password));
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

        return Result.success().put(Constants.DATA, list).put(Constants.TOTAL, total);
    }
}
