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
import com.datasophon.api.security.UserPermission;
import com.datasophon.api.service.UserInfoService;
import com.datasophon.api.utils.SecurityUtils;
import com.datasophon.common.Constants;
import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.UserInfoEntity;
import com.mybatisflex.core.query.QueryChain;
import org.springframework.beans.factory.annotation.Autowired;
import com.datasophon.api.annotation.ApiVersion;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Arrays;
import java.util.List;

@ApiVersion(path = "user")
public class UserInfoController {

    @Autowired
    private UserInfoService userInfoService;

    /**
     * 获取当前登录用户信息
     */
    @PostMapping("/user-info")
    public Result getCurrentUserInfo() {
        // 从Spring Security上下文中获取当前认证用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            return Result.error("未登录或会话已过期");
        }

        // 获取用户名
        String username = authentication.getName();

        // 根据用户名获取用户信息
        UserInfoEntity userInfo = userInfoService.getUserByUsername(username);
        if (userInfo == null) {
            return Result.error("用户信息不存在");
        }

        // 出于安全考虑，清除敏感字段
        userInfo.setPassword(null);

        return Result.success(userInfo);
    }

    /**
     * 列表带分页
     */
    @RequestMapping("/list")
    public Result list(@RequestParam(name = "username",required = false) String username, @RequestParam("page") Integer page,
            @RequestParam("pageSize") Integer pageSize) {
        return userInfoService.getUserListByPage(username, page, pageSize);
    }

    /**
     * 查询所有用户
     */
    @RequestMapping("/all")
    public Result all() {
        List<UserInfoEntity> list = QueryChain.of(UserInfoEntity.class).list();
        return Result.success(list);
    }

    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public Result info(@PathVariable("id") Integer id) {
        UserInfoEntity userInfo = userInfoService.getById(id);

        return Result.success(userInfo);
    }

    /**
     * 检查用户名是否存在
     */
    @PostMapping("/checkName")
    public Result checkUsername(@RequestBody CheckUsernameRequest request) {
        boolean exists = userInfoService.checkUsernameExists(request.getUsername(), request.getExcludeId());
        return Result.success(exists);
    }

    /**
     * 检查用户名请求类
     */
    public static class CheckUsernameRequest {
        private String username;
        private Integer excludeId;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public Integer getExcludeId() {
            return excludeId;
        }

        public void setExcludeId(Integer excludeId) {
            this.excludeId = excludeId;
        }
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    @UserPermission
    public Result save(@RequestBody UserInfoEntity userInfo) {

        return userInfoService.createUser(userInfo);
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    @UserPermission
    public Result update(@RequestBody UserInfoEntity userInfo) {
        return userInfoService.updateUser(userInfo);
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    @UserPermission
    public Result delete(@RequestBody Integer[] ids) {
        if (SecurityUtils.getAuthUser().getId() != 1) {
            return Result.error(Status.USER_NO_OPERATION_PERM.getMsg());
        }
        userInfoService.removeByIds(Arrays.asList(ids));

        return Result.success();
    }

}
