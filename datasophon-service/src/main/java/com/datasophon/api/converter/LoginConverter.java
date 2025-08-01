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

package com.datasophon.api.converter;

import com.datasophon.common.vo.LoginResponseVO;
import com.datasophon.common.vo.TokenResponseVO;
import com.datasophon.common.vo.UserInfoVO;
import com.datasophon.dao.entity.UserInfoEntity;
import com.datasophon.common.utils.FormatterUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

/**
 * 登录相关对象转换器
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-01
 */
@Mapper(componentModel = "spring", uses = FormatterUtils.class)
public interface LoginConverter {

    /**
     * 创建登录响应VO
     * 
     * @param user         用户实体
     * @param token        访问令牌
     * @param refreshToken 刷新令牌
     * @param roles        用户角色
     * @return 登录响应VO
     */
    @Mapping(target = "token", source = "token")
    @Mapping(target = "refreshToken", source = "refreshToken")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "username", source = "user.username")
    @Mapping(target = "nickName", source = "user.nickName")
    @Mapping(target = "email", source = "user.email")
    @Mapping(target = "phone", source = "user.phone")
    @Mapping(target = "lastLoginTime", source = "user.lastLoginTime")
    @Mapping(target = "previousLoginTime", source = "user.previousLoginTime")
    @Mapping(target = "roles", source = "roles")
    @Mapping(target = "userType", source = "user.userType", qualifiedByName = "formatUserType")
    LoginResponseVO toLoginResponseVO(UserInfoEntity user, String token, String refreshToken, String roles);

    /**
     * 创建令牌响应VO
     * 
     * @param token        访问令牌
     * @param refreshToken 刷新令牌
     * @param message      消息
     * @return 令牌响应VO
     */
    default TokenResponseVO toTokenResponseVO(String token, String refreshToken, String message) {
        return new TokenResponseVO(token, refreshToken, message);
    }

    /**
     * 用户详情转换为UserInfoVO（用于获取用户信息接口）
     * 
     * @param username 用户名
     * @return UserInfoVO
     */
    default UserInfoVO createUserInfoVO(String username) {
        // TODO: 这里应该从UserInfoDTO转换，当前临时实现
        return UserInfoVO.builder()
                .id(1) // 临时ID
                .username(username)
                .email(username + "@example.com") // 临时邮箱
                .phone("1234567890") // 临时电话
                .userTypeDesc("普通用户") // 临时用户类型描述
                .online(true) // 在线状态
                .build();
    }

    /**
     * 格式化用户类型
     * 
     * @param userType 用户类型
     * @return 格式化后的用户类型
     */
    @Named("formatUserType")
    default String formatUserType(Integer userType) {
        if (userType == null) {
            return "普通用户";
        }

        if (userType == 1) {
            return "管理员";
        } else if (userType == 2) {
            return "运维人员";
        } else if (userType == 3) {
            return "开发人员";
        } else {
            return "普通用户";
        }
    }
}