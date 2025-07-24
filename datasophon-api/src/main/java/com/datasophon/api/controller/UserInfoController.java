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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("api/user")
public class UserInfoController {

    private final UserInfoService userInfoService;

    public UserInfoController(UserInfoService userInfoService) {
        this.userInfoService = userInfoService;
    }

    /**
     * 列表带分页
     */
    @RequestMapping("/list")
    public Result list(@RequestParam("username") String username, @RequestParam("page") Integer page,
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

        return Result.success().put(Constants.DATA, userInfo);
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
