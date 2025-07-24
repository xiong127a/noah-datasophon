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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Spring Security用户详情服务实现
 * 负责加载用户信息以进行认证和授权
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(CustomUserDetailsService.class);

    private final UserInfoService userService;

    public CustomUserDetailsService(UserInfoService userService) {
        this.userService = userService;
    }

    /**
     * 根据用户名加载用户详情
     * 
     * @param username 用户名
     * @return UserDetails 用户详情
     * @throws UsernameNotFoundException 如果用户未找到则抛出异常
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        logger.debug("认证用户: {}", username);

        // 调用服务方法获取用户
        UserInfoEntity user = userService.getUserByUsername(username);

        if (user == null) {
            logger.error("用户未找到: {}", username);
            throw new UsernameNotFoundException("用户不存在: " + username);
        }

        // 创建权限列表
        List<SimpleGrantedAuthority> authorities = buildUserAuthority(user);

        // 创建并返回UserDetails对象
        return buildUserForAuthentication(user, authorities);
    }

    /**
     * 构建用户的权限列表
     * 
     * @param user 用户实体
     * @return 权限列表
     */
    private List<SimpleGrantedAuthority> buildUserAuthority(UserInfoEntity user) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();

        // 添加基本用户角色
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

        // 管理员角色
        if (user.getUserType() != null && user.getUserType() == 1) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }

        // 这里可以添加更多的角色和权限，例如从数据库加载用户的角色和权限

        return authorities;
    }

    /**
     * 构建认证用户
     * 
     * @param user        用户实体
     * @param authorities 权限列表
     * @return UserDetails对象
     */
    private UserDetails buildUserForAuthentication(UserInfoEntity user, List<SimpleGrantedAuthority> authorities) {
        return new User(
                user.getUsername(),
                user.getPassword(),
                true, // enabled
                true, // accountNonExpired
                true, // credentialsNonExpired
                true, // accountNonLocked
                authorities);
    }
}
