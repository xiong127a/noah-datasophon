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
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryChain;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * 用户信息表数据访问层
 * 负责所有与用户相关的数据库操作
 */
@Mapper
public interface UserInfoMapper extends BaseMapper<UserInfoEntity> {

    /**
     * 根据用户名和密码查询用户（登录验证）
     * 
     * @param username 用户名
     * @param password 加密后的密码
     * @return 用户实体
     */
    default UserInfoEntity selectByUsernameAndPassword(@Param("username") String username,
            @Param("password") String password) {
        return QueryChain.of(UserInfoEntity.class)
                .where(UserInfoEntity::getUsername).eq(username)
                .and(UserInfoEntity::getPassword).eq(password)
                .one();
    }

    /**
     * 根据用户名查询用户
     * 
     * @param username 用户名
     * @return 用户实体
     */
    default UserInfoEntity selectByUsername(@Param("username") String username) {
        return QueryChain.of(UserInfoEntity.class)
                .where(UserInfoEntity::getUsername).eq(username)
                .one();
    }

    /**
     * 检查用户名是否存在（排除指定ID）
     * 
     * @param username  用户名
     * @param excludeId 要排除的用户ID
     * @return 是否存在
     */
    default boolean existsByUsernameExcludeId(@Param("username") String username,
            @Param("excludeId") Integer excludeId) {
        QueryChain<UserInfoEntity> query = QueryChain.of(UserInfoEntity.class)
                .where(UserInfoEntity::getUsername).eq(username);

        if (excludeId != null) {
            query.and(UserInfoEntity::getId).ne(excludeId);
        }

        return query.exists();
    }

    /**
     * 检查用户名是否存在
     * 
     * @param username 用户名
     * @return 是否存在
     */
    default boolean existsByUsername(@Param("username") String username) {
        return QueryChain.of(UserInfoEntity.class)
                .where(UserInfoEntity::getUsername).eq(username)
                .exists();
    }

    /**
     * 分页查询用户列表（支持用户名模糊查询）
     * 
     * @param page     分页参数
     * @param username 用户名（可为空）
     * @return 用户列表
     */
    default Page<UserInfoEntity> selectPageByUsername(Page<UserInfoEntity> page,
            @Param("username") String username) {
        QueryChain<UserInfoEntity> query = QueryChain.of(UserInfoEntity.class);

        if (StringUtils.isNotBlank(username)) {
            query.where(UserInfoEntity::getUsername).like("%" + username + "%");
        }

        return query.page(page);
    }

    /**
     * 更新用户登录时间
     * 
     * @param userId    用户ID
     * @param loginTime 登录时间
     * @return 更新行数
     */
    default int updateLoginTime(@Param("userId") Integer userId,
            @Param("loginTime") java.util.Date loginTime) {
        UserInfoEntity updateEntity = new UserInfoEntity();
        updateEntity.setId(userId);
        updateEntity.setLastLoginTime(loginTime);
        updateEntity.setPreviousLoginTime(updateEntity.getLastLoginTime()); // 保存上次登录时间

        return QueryChain.of(UserInfoEntity.class)
                .where(UserInfoEntity::getId).eq(userId)
                .update(updateEntity);
    }
}
