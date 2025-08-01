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


import com.datasophon.dao.entity.UserInfoEntity;
import com.datasophon.common.dto.UserInfoDTO;
import com.datasophon.common.model.PageResult;

/**
 * 用户信息服务接口
 * 对外提供DTO接口，内部保持Entity支持以兼容MyBatis-Flex
 *
 * @author gaodayu
 * @email gaodayu2022@163.com
 * @date 2022-03-15 17:36:08
 */
public interface UserInfoService {

    /**
     * 用户登录验证
     * 
     * @param username 用户名
     * @param password 密码
     * @return 用户信息DTO
     */
    UserInfoDTO queryUser(String username, String password);

    /**
     * 创建用户
     * 
     * @param userInfoDTO 用户信息DTO
     * @return 创建的用户信息
     * @throws com.datasophon.common.exception.UserBusinessException 用户名已存在等业务异常
     */
    UserInfoDTO createUser(UserInfoDTO userInfoDTO);

    /**
     * 更新用户信息
     * 
     * @param userInfoDTO 用户信息DTO
     * @return 更新后的用户信息
     * @throws com.datasophon.common.exception.UserBusinessException 用户名已存在等业务异常
     */
    UserInfoDTO updateUser(UserInfoDTO userInfoDTO);

    /**
     * 分页查询用户列表
     * 
     * @param username 用户名（模糊查询）
     * @param page     页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    PageResult<UserInfoDTO> getUserListByPage(String username, Integer page, Integer pageSize);

    /**
     * 根据用户名查询用户
     * 
     * @param username 用户名
     * @return 用户信息DTO
     */
    UserInfoDTO getUserByUsername(String username);

    /**
     * 检查用户名是否存在
     * 
     * @param username  用户名
     * @param excludeId 排除的用户ID（编辑时排除当前用户）
     * @return true表示用户名已存在，false表示可用
     */
    boolean checkUsernameExists(String username, Integer excludeId);

    // ============ 内部使用的Entity方法（兼容现有代码） ============

    /**
     * 根据用户名查询用户Entity（内部使用）
     * 
     * @param username 用户名
     * @return 用户Entity
     */
    UserInfoEntity getUserEntityByUsername(String username);
}
