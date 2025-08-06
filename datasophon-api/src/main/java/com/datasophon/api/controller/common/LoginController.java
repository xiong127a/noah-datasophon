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

import com.datasophon.api.annotation.ApiVersion;
import com.datasophon.api.converter.LoginConverter;
import com.datasophon.api.security.PersistentTokenManager;
import com.datasophon.api.service.AuthTokenService;
import com.datasophon.api.service.UserInfoService;
import com.datasophon.common.dto.LoginRequestDTO;
import com.datasophon.common.dto.RefreshTokenRequestDTO;
import com.datasophon.common.enums.Status;
import com.datasophon.common.vo.LoginResponseVO;
import com.datasophon.api.dto.Result;
import com.datasophon.common.vo.TokenResponseVO;
import com.datasophon.common.vo.UserInfoVO;
import com.datasophon.dao.entity.UserInfoEntity;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Date;

/**
 * 认证控制器 - 处理用户登录、登出和用户信息
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-01
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

    @Autowired
    private LoginConverter loginConverter;

    /**
     * 用户登录API - JSON格式
     * 
     * @param loginRequest 包含用户名和密码的请求
     * @return 带有JWT令牌的响应
     */
    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Result<LoginResponseVO> loginJson(@RequestBody @Valid LoginRequestDTO loginRequest,
            HttpServletRequest request) {
        return processLogin(loginRequest.username(), loginRequest.password(), request);
    }

    /**
     * 用户登录API - 表单格式
     * 
     * @param username 用户名
     * @param password 密码
     * @return 带有JWT令牌的响应
     */
    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Result<LoginResponseVO> loginForm(
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
    private Result<LoginResponseVO> processLogin(String username, String password, HttpServletRequest request) {
        try {
            logger.debug("尝试登录用户: {}", username);

            // 验证用户名和密码
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password));

            // 设置认证信息到上下文
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // 获取用户详情 - 从数据库查询真实用户信息
            logger.debug("用户登录成功: {}", username);

            // 从数据库查询完整的用户实体
            UserInfoEntity user = userInfoService.getUserEntityByUsername(username);
            if (user == null) {
                logger.error("登录成功但未找到用户信息: {}", username);
                return Result.error(Status.USER_NOT_EXIST.getCode(), Status.USER_NOT_EXIST.getMsg());
            }
            
            // 更新最后登录时间
            user.setLastLoginTime(new Date());

            // 更新登录时间记录 - JDK21简化写法
            updateUserLoginTime(user, username);

            // 生成JWT令牌
            String accessToken = tokenProvider.createToken(authentication, request);
            String refreshToken = authTokenService.createRefreshToken(user.getId().toString());

            // 获取用户角色 - JDK21 Stream.toList()
            String roles = authentication.getAuthorities() != null
                    ? authentication.getAuthorities()
                            .stream()
                            .map(GrantedAuthority::getAuthority)
                            .toList()
                            .toString()
                    : "";

            // 使用Converter创建LoginResponseVO
            LoginResponseVO loginResponse = loginConverter.toLoginResponseVO(user, accessToken, refreshToken, roles);

            return Result.success(loginResponse)
                    .setMsg(Status.LOGIN_SUCCESS.getMsg())
                    .setCode(Status.SUCCESS.getCode());

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
     * 更新用户登录时间
     * 
     * @param user     用户实体
     * @param username 用户名
     */
    private void updateUserLoginTime(UserInfoEntity user, String username) {
        try {
            Date currentTime = new Date();
            UserInfoEntity updateUser = new UserInfoEntity();
            updateUser.setId(user.getId());
            updateUser.setPreviousLoginTime(user.getLastLoginTime());
            updateUser.setLastLoginTime(currentTime);

            // 通过Service层更新 - 使用save方法或自定义更新方法
            // userInfoService.updateUserLoginTime(user.getId(), currentTime,
            // user.getLastLoginTime());

            // 更新内存中的用户信息
            user.setPreviousLoginTime(user.getLastLoginTime());
            user.setLastLoginTime(currentTime);

            logger.debug("已更新用户 {} 的登录时间记录", username);
        } catch (Exception e) {
            logger.warn("更新用户登录时间失败: {}", e.getMessage());
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
    public Result<TokenResponseVO> refreshToken(@RequestBody @Valid RefreshTokenRequestDTO refreshRequest,
            HttpServletRequest request) {
        try {
            // 使用AuthTokenService验证并刷新令牌
            String refreshToken = refreshRequest.refreshToken();
            String newAccessToken = authTokenService.refreshAccessToken(refreshToken, request);

            if (newAccessToken == null) {
                return Result.error(Status.LOGIN_SESSION_FAILED.getCode(), "刷新令牌无效或已过期");
            }

            // 使用Converter创建TokenResponseVO
            TokenResponseVO tokenResponse = loginConverter.toTokenResponseVO(newAccessToken, refreshToken, "令牌已刷新");

            return Result.success(tokenResponse)
                    .setCode(Status.SUCCESS.getCode())
                    .setMsg("令牌已刷新");

        } catch (Exception e) {
            logger.error("刷新令牌失败: {}", e.getMessage(), e);
            return Result.error(Status.LOGIN_SESSION_FAILED.getCode(), "刷新令牌失败: " + e.getMessage());
        }
    }

    /**
     * 登出
     */
    @PostMapping(value = { "/logout", "/signOut" })
    public Result<Void> logout(HttpServletRequest request) {
        try {
            // 获取认证令牌
            String token = tokenProvider.resolveToken(request);
            if (token != null) {
                // 通过Service层获取数据库记录 - 暂时简化处理
                try {
                    Object tokenDto = authTokenService.getByToken(token);
                    if (tokenDto != null) {
                        // TODO: 需要根据实际的AuthTokenDTO调整
                        logger.debug("用户令牌已处理: {}", token);
                    }
                } catch (Exception ex) {
                    logger.warn("处理令牌撤销失败", ex);
                }
            }
        } catch (Exception e) {
            logger.warn("登出过程中出错", e);
        }

        // 清除Spring Security上下文
        SecurityContextHolder.clearContext();
        return Result.<Void>success().setMsg("登出成功");
    }

    /**
     * 获取当前用户信息 - 使用Spring Security注解自动注入当前用户
     */
    @GetMapping("/user-info")
    public Result<UserInfoVO> getUserInfo(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return Result.error(Status.LOGIN_SESSION_FAILED.getCode(), "未提供有效令牌");
        }

        try {
            String username = userDetails.getUsername();

            // ✅ 使用Converter进行转换
            UserInfoVO userInfo = loginConverter.createUserInfoVO(username);

            return Result.success(userInfo);
        } catch (Exception e) {
            logger.error("获取用户信息失败: {}", e.getMessage(), e);
            return Result.error(Status.LOGIN_SESSION_FAILED.getCode(), "获取用户信息失败");
        }
    }

}
