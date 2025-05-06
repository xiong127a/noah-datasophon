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

import com.datasophon.dao.entity.UserInfoEntity;
import com.datasophon.dao.entity.AccessTokenEntity;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.yulichang.base.MPJBaseMapper;
import com.github.yulichang.wrapper.MPJLambdaWrapper;

/**
 * 用户信息表
 * 
 * @author gaodayu
 * @email gaodayu2022@163.com
 * @date 2022-03-15 17:36:08
 */
@Mapper
public interface UserInfoMapper extends MPJBaseMapper<UserInfoEntity> {

    default UserInfoEntity queryUserByToken(@Param("token") String token) {
        return this.selectJoinOne(UserInfoEntity.class,
                new MPJLambdaWrapper<UserInfoEntity>()
                        .selectAll(UserInfoEntity.class)
                        .leftJoin(AccessTokenEntity.class, AccessTokenEntity::getUserId, UserInfoEntity::getId)
                        .eq(AccessTokenEntity::getToken, token)
                        .gt(AccessTokenEntity::getExpireTime, new java.util.Date()));
    }
}
