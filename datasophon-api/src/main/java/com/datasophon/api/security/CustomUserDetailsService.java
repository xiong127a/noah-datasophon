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
import com.datasophon.common.dto.UserInfoDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-06
 */
@Slf4j
@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserInfoService userService;



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
        log.debug("认证用户: {}", username);

        // 调用服务方法获取用户DTO - 遵循架构规范
        var userDTO = userService.getUserByUsername(username); // JDK21特性

        if (userDTO == null) {
            log.error("用户未找到: {}", username);
            throw new UsernameNotFoundException("用户不存在: " + username);
        }

        // 创建权限列表 - 使用JDK21特性
        var authorities = buildUserAuthority(userDTO);

        // 创建并返回UserDetails对象
        return buildUserForAuthentication(userDTO, authorities);
    }

    /**
     * 构建用户的权限列表
     *
     * @return 权限列表
     */
    private List<SimpleGrantedAuthority> buildUserAuthority(UserInfoDTO userDTO) {
        var authorities = new ArrayList<SimpleGrantedAuthority>(); // JDK21特性

        // 添加基本用户角色
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

        // 管理员角色 - 使用JDK21条件运算符
        var isAdmin = userDTO.getUserType() != null && userDTO.getUserType() == 1; // JDK21特性
        
        if (isAdmin) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }

        // 这里可以添加更多的角色和权限，例如从数据库加载用户的角色和权限

        return authorities;
    }

    /**
     * 构建认证用户
     *
     * @param authorities 权限列表
     * @return UserDetails对象
     */
    private UserDetails buildUserForAuthentication(UserInfoDTO userDTO, List<SimpleGrantedAuthority> authorities) {
        return new User(
                userDTO.getUsername(),
                userDTO.getPassword(),
                true, // enabled
                true, // accountNonExpired
                true, // credentialsNonExpired
                true, // accountNonLocked
                authorities);
    }
}
