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

package com.datasophon.api.controller.common;

import com.datasophon.api.enums.Status;
import com.datasophon.api.security.PersistentTokenManager;
import com.datasophon.api.service.AuthTokenService;
import com.datasophon.api.service.UserInfoService;
import com.datasophon.common.Constants;
import com.datasophon.common.security.TokenProvider;
import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.AuthTokenEntity;
import com.datasophon.dao.entity.UserInfoEntity;
import jakarta.servlet.http.HttpServletRequest;
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
import com.datasophon.api.annotation.ApiVersion;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 认证控制器 - 处理用户登录、登出和用户信息
 */
@ApiVersion(path = "auth")
public class LoginController {

    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private PersistentTokenManager tokenProvider;

    @Autowired
    private AuthTokenService authTokenService;

    @Autowired
    private UserInfoService userInfoService;

    /**
     * 用户登录API - JSON格式
     * 
     * @param loginRequest 包含用户名和密码的请求
     * @return 带有JWT令牌的响应
     */
    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Result loginJson(@RequestBody @Valid LoginRequest loginRequest, HttpServletRequest request) {
        return processLogin(loginRequest.getUsername(), loginRequest.getPassword(), request);
    }

    /**
     * 用户登录API - 表单格式
     * 
     * @param username 用户名
     * @param password 密码
     * @return 带有JWT令牌的响应
     */
    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Result loginForm(
            @RequestParam("username") String username,
            @RequestParam("password") String password,
            HttpServletRequest request) {
        return processLogin(username, password, request);
    }

    /**
     * 处理登录逻辑
     * 
     * @param username 用户名
     * @param password 密码
     * @param request  HTTP请求
     * @return 登录结果
     */
    private Result processLogin(String username, String password, HttpServletRequest request) {
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

            // 更新登录时间记录（线程安全的数据库操作）
            try {
                Date currentTime = new java.util.Date();
                UserInfoEntity updateUser = new UserInfoEntity();
                updateUser.setId(user.getId());
                // 将当前的lastLoginTime移动到previousLoginTime
                updateUser.setPreviousLoginTime(user.getLastLoginTime());
                // 设置新的lastLoginTime为当前时间
                updateUser.setLastLoginTime(currentTime);
                
                userInfoService.updateById(updateUser);
                
                // 更新返回给前端的用户信息
                user.setPreviousLoginTime(user.getLastLoginTime()); // 上次登录时间
                user.setLastLoginTime(currentTime); // 最后登录时间
                
                logger.debug("已更新用户 {} 的登录时间记录", username);
            } catch (Exception e) {
                logger.warn("更新用户登录时间失败: {}", e.getMessage());
                // 不影响登录流程，只记录警告
            }

            // 先撤销用户之前的令牌（可选，如果需要单点登录）
            // authTokenService.revokeAllUserTokens(user.getId(), "用户重新登录");

            // 生成JWT令牌
            String accessToken = tokenProvider.createToken(authentication, request);
            String refreshToken = authTokenService.createRefreshToken(user.getId().toString());

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

            return Result.success(responseData).setMsg(Status.LOGIN_SUCCESS.getMsg()).setCode(Status.SUCCESS.getCode());

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
     * @param request        HTTP请求
     * @return 带有新JWT令牌的响应
     */
    @PostMapping("/refresh-token")
    public Result refreshToken(@RequestBody RefreshTokenRequest refreshRequest, HttpServletRequest request) {
        try {
            // 使用AuthTokenService验证并刷新令牌
            String refreshToken = refreshRequest.getRefreshToken();
            String newAccessToken = authTokenService.refreshAccessToken(refreshToken, request);

            if (newAccessToken == null) {
                return Result.error(Status.LOGIN_SESSION_FAILED.getCode(), "刷新令牌无效或已过期");
            }

            // 返回新令牌
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("token", newAccessToken);
            responseData.put("refreshToken", refreshToken); // 保持相同的刷新令牌

            return Result.success(responseData).setCode(Status.SUCCESS.getCode()).setMsg("令牌已刷新");

        } catch (Exception e) {
            logger.error("刷新令牌失败: {}", e.getMessage(), e);
            return Result.error(Status.LOGIN_SESSION_FAILED.getCode(), "刷新令牌失败: " + e.getMessage());
        }
    }

    /**
     * 登出
     */
    @PostMapping(value = { "/logout", "/signOut" })
    public Result logout(HttpServletRequest request) {
        try {
            // 获取认证令牌
            String token = tokenProvider.resolveToken(request);
            if (token != null) {
                // 通过令牌获取数据库记录
                AuthTokenEntity tokenEntity = authTokenService.getByToken(token);
                if (tokenEntity != null) {
                    // 撤销令牌
                    authTokenService.revokeToken(tokenEntity.getId(), "用户主动登出");
                    logger.debug("用户令牌已撤销: {}", tokenEntity.getId());
                }
            }
        } catch (Exception e) {
            logger.warn("登出过程中出错", e);
        }

        // 清除Spring Security上下文
        SecurityContextHolder.clearContext();
        return Result.success().setMsg("登出成功");
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
     * 登录请求DTO - SpringBoot3规范
     */
    @Data
    public static class LoginRequest {
        @jakarta.validation.constraints.NotBlank(message = "用户名不能为空")
        private String username;
        
        @jakarta.validation.constraints.NotBlank(message = "密码不能为空")
        private String password;
    }

    /**
     * 刷新令牌请求DTO - SpringBoot3规范
     */
    @Data
    public static class RefreshTokenRequest {
        @jakarta.validation.constraints.NotBlank(message = "刷新令牌不能为空")
        private String refreshToken;
    }
}
