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

package com.datasophon.api.security;

import com.datasophon.api.service.UserInfoService;
import com.datasophon.dao.entity.UserInfoEntity;
import com.mybatisflex.core.query.QueryChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Spring Security用户详情服务实现
 * 负责加载用户信息以进行认证
 */
@Service
public class PasswordAuthenticator implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(PasswordAuthenticator.class);

    @Autowired
    private UserInfoService userService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 直接使用QueryChain查询用户
        UserInfoEntity user = QueryChain.of(UserInfoEntity.class)
                .where(UserInfoEntity::getUsername).eq(username)
                .one();

        if (user == null) {
            logger.error("User not found with username: {}", username);
            throw new UsernameNotFoundException("用户不存在: " + username);
        }

        // 创建权限列表
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

        // 如果是管理员，添加管理员角色
        if (user.getUserType() != null && user.getUserType() == 1) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }

        // 返回Spring Security用户对象
        return new User(
                user.getUsername(),
                user.getPassword(),
                true, // 所有用户默认为启用状态
                true, // accountNonExpired
                true, // credentialsNonExpired
                true, // accountNonLocked
                authorities);
    }
}
