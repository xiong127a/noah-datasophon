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
import jakarta.validation.Valid;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 认证控制器 - 处理用户登录、登出和用户信息
 */
@RestController
@RequestMapping("/api")
public class LoginController {

    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    private final AuthenticationManager authenticationManager;

    private final JwtTokenProvider tokenProvider;

    private final UserInfoService userInfoService;

    /**
     * 用户登录API - JSON格式
     * 
     * @param loginRequest 包含用户名和密码的请求
     * @return 带有JWT令牌的响应
     */
    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Result loginJson(@RequestBody @Valid LoginRequest loginRequest) {
        return processLogin(loginRequest.getUsername(), loginRequest.getPassword());
    }

    /**
     * 用户登录API - 表单格式
     * 
     * @param username 用户名
     * @param password 密码
     * @return 带有JWT令牌的响应
     */
    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Result loginForm(@RequestParam("username") String username, @RequestParam("password") String password) {
        return processLogin(username, password);
    }

    /**
     * 处理登录逻辑
     * 
     * @param username 用户名
     * @param password 密码
     * @return 登录结果
     */
    private Result processLogin(String username, String password) {
        try {
            logger.debug("尝试登录用户: {}", username);

            // 验证用户名和密码
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password));

            // 设置认证信息到上下文
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // 获取用户详情
            UserInfoEntity user = userInfoService.getUserByUsername(username);
            if (user == null) {
                logger.error("登录成功但未找到用户实体: {}", username);
                return Result.error(Status.USER_NOT_EXIST.getCode(), Status.USER_NOT_EXIST.getMsg());
            }

            // 生成JWT令牌
            String accessToken = tokenProvider.createToken(authentication);
            String refreshToken = tokenProvider.createRefreshToken(user.getId().toString());

            // 清除密码等敏感信息
            user.setPassword(null);

            // 构建响应数据
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("token", accessToken);
            responseData.put("refreshToken", refreshToken);
            responseData.put("user", user);

            // 添加额外的用户信息（如权限）
            if (authentication.getAuthorities() != null && !authentication.getAuthorities().isEmpty()) {
                String authorities = authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.joining(","));
                responseData.put("roles", authorities);
            }

            return Result.success(responseData)
                    .put(Constants.CODE, Status.SUCCESS.getCode())
                    .put(Constants.MSG, Status.LOGIN_SUCCESS.getMsg());

        } catch (BadCredentialsException e) {
            logger.error("登录失败: 用户名或密码错误: {}", username);
            return Result.error(Status.USER_NAME_PASSWD_ERROR.getCode(),
                    Status.USER_NAME_PASSWD_ERROR.getMsg());
        } catch (Exception e) {
            logger.error("登录失败: {}", e.getMessage(), e);
            return Result.error(Status.USER_NAME_PASSWD_ERROR.getCode(), "登录失败: " + e.getMessage());
        }
    }

    /**
     * 刷新令牌API
     * 
     * @param refreshRequest 包含刷新令牌的请求
     * @return 带有新JWT令牌的响应
     */
    @PostMapping("/refresh-token")
    public Result refreshToken(@RequestBody RefreshTokenRequest refreshRequest) {
        try {
            // 验证刷新令牌
            String refreshToken = refreshRequest.getRefreshToken();
            if (!tokenProvider.validateToken(refreshToken)) {
                return Result.error(Status.LOGIN_SESSION_FAILED.getCode(), "刷新令牌无效或已过期");
            }

            // 从刷新令牌中获取用户ID
            String userId = tokenProvider.getUserIdFromToken(refreshToken);
            UserInfoEntity user = userInfoService.getById(Long.parseLong(userId));

            if (user == null) {
                return Result.error(Status.USER_NOT_EXIST.getCode(), Status.USER_NOT_EXIST.getMsg());
            }

            // 创建新的认证对象
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            // 生成新的访问令牌
            String newAccessToken = tokenProvider.createToken(authentication);

            // 返回新令牌
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("token", newAccessToken);
            responseData.put("refreshToken", refreshToken); // 保持相同的刷新令牌

            return Result.success(responseData)
                    .put(Constants.CODE, Status.SUCCESS.getCode())
                    .put(Constants.MSG, "令牌已刷新");

        } catch (Exception e) {
            logger.error("刷新令牌失败: {}", e.getMessage(), e);
            return Result.error(Status.LOGIN_SESSION_FAILED.getCode(), "刷新令牌失败: " + e.getMessage());
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
     * 获取当前用户信息 - 使用Spring Security注解自动注入当前用户
     */
    @GetMapping("/user-info")
    public Result getUserInfo(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return Result.error(Status.LOGIN_SESSION_FAILED.getCode(), "未提供有效令牌");
        }

        try {
            UserInfoEntity user = userInfoService.getUserByUsername(userDetails.getUsername());

            if (user == null) {
                return Result.error(Status.USER_NOT_EXIST.getCode(), Status.USER_NOT_EXIST.getMsg());
            }

            // 清除敏感信息
            user.setPassword(null);

            return Result.success(user);
        } catch (Exception e) {
            logger.error("获取用户信息失败: {}", e.getMessage(), e);
            return Result.error(Status.LOGIN_SESSION_FAILED.getCode(), "获取用户信息失败");
        }
    }

    /**
     * 登录请求DTO
     */
    @Data
    public static class LoginRequest {
        private String username;
        private String password;
    }

    /**
     * 刷新令牌请求DTO
     */
    @Data
    public static class RefreshTokenRequest {
        private String refreshToken;
    }
}
