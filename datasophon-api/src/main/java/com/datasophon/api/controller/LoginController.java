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

package com.datasophon.api.controller;

import com.datasophon.api.enums.Status;
import com.datasophon.api.security.JwtTokenProvider;
import com.datasophon.api.service.UserInfoService;
import com.datasophon.common.Constants;
import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.UserInfoEntity;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class LoginController {

    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private UserInfoService userInfoService;

    /**
     * 用户登录API
     */
    @PostMapping("/login")
    public Result login(@RequestBody LoginRequest loginRequest) {
        try {
            // 验证用户名和密码
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()));

            // 设置认证信息到上下文
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // 获取用户信息
            UserInfoEntity user = userInfoService.getUserByUsername(loginRequest.getUsername());

            if (user == null) {
                return Result.error(Status.USER_NAME_PASSWD_ERROR.getCode(),
                        Status.USER_NAME_PASSWD_ERROR.getMsg());
            }

            // 生成JWT令牌
            String jwt = tokenProvider.createToken(authentication);

            // 返回结果
            Map<String, Object> responseData = new HashMap<>();
            responseData.put(Constants.SESSION_ID, jwt);
            responseData.put(Constants.USER_INFO, user);

            return Result.success(responseData)
                    .put(Constants.CODE, Status.SUCCESS.getCode())
                    .put(Constants.MSG, Status.LOGIN_SUCCESS.getMsg());

        } catch (BadCredentialsException e) {
            logger.error("登录失败: 用户名或密码错误", e);
            return Result.error(Status.USER_NAME_PASSWD_ERROR.getCode(),
                    Status.USER_NAME_PASSWD_ERROR.getMsg());
        } catch (Exception e) {
            logger.error("登录失败: {}", e.getMessage(), e);
            return Result.error(Status.USER_NAME_PASSWD_ERROR.getCode(), "登录失败: " + e.getMessage());
        }
    }

    /**
     * 登出
     */
    @PostMapping(value = { "/logout", "/signOut" })
    public Result logout() {
        // 清除Spring Security上下文
        SecurityContextHolder.clearContext();
        return Result.success().put(Constants.MSG, "登出成功");
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/user-info")
    public Result getUserInfo(HttpServletRequest request) {
        String token = tokenProvider.resolveToken(request);
        if (token == null) {
            return Result.error(Status.LOGIN_SESSION_FAILED.getCode(), "未提供有效令牌");
        }

        try {
            Authentication authentication = tokenProvider.getAuthentication(token);
            User principal = (User) authentication.getPrincipal();

            UserInfoEntity user = userInfoService.getUserByUsername(principal.getUsername());

            if (user == null) {
                return Result.error(Status.USER_NOT_EXIST.getCode(), Status.USER_NOT_EXIST.getMsg());
            }

            return Result.success(user);
        } catch (Exception e) {
            logger.error("获取用户信息失败: {}", e.getMessage(), e);
            return Result.error(Status.LOGIN_SESSION_FAILED.getCode(), "令牌无效或已过期");
        }
    }

    public static class LoginRequest {
        private String username;
        private String password;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}
