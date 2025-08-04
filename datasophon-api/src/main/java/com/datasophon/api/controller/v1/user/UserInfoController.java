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

package com.datasophon.api.controller.v1.user;

import com.datasophon.common.enums.Status;
import com.datasophon.api.security.UserPermission;
import com.datasophon.api.service.UserInfoService;
import com.datasophon.api.utils.SecurityUtils;
import com.datasophon.api.converter.UserInfoConverter;
import com.datasophon.api.dto.Result;
import com.datasophon.common.vo.UserInfoVO;
import com.datasophon.common.dto.UserInfoDTO;
import com.datasophon.dao.entity.UserInfoEntity;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import com.datasophon.api.annotation.ApiVersion;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@ApiVersion(path = "user")
public class UserInfoController {

    @Autowired
    private UserInfoService userInfoService;

    @Autowired
    private UserInfoConverter userInfoConverter;

    /**
     * 列表带分页
     */
    @RequestMapping("/list")
    public Result<List<UserInfoVO>> list(@RequestParam(name = "username", required = false) String username,
            @RequestParam("page") Integer page,
            @RequestParam("pageSize") Integer pageSize) {
        // Service层返回PageResult<DTO>
        com.datasophon.common.model.PageResult<UserInfoDTO> servicePageResult = userInfoService
                .getUserListByPage(username, page, pageSize);

        // 转换DTO列表为VO列表
        List<UserInfoVO> voList = servicePageResult.getRecords().stream()
                .map(userInfoConverter::dtoToVo)
                .collect(Collectors.toList());

        // API层包装成Result返回
        return Result.success(voList, servicePageResult.getTotal());
    }

    /**
     * 查询所有用户
     */
    @RequestMapping("/all")
    public Result<List<UserInfoVO>> all() {
        // 使用Service提供的分页方法，获取所有数据
        com.datasophon.common.model.PageResult<UserInfoDTO> servicePageResult = userInfoService.getUserListByPage(null,
                1, Integer.MAX_VALUE);

        // 转换为VO列表
        List<UserInfoVO> voList = servicePageResult.getRecords().stream()
                .map(userInfoConverter::dtoToVo)
                .collect(Collectors.toList());

        return Result.success(voList);
    }

    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public Result<UserInfoVO> info(@PathVariable("id") Integer id) {
        // 通过Entity方法获取用户（保持兼容性）
        UserInfoEntity userInfo = userInfoService.getById(id);

        if (userInfo == null) {
            return Result.success(null);
        }

        // 转换为VO返回
        UserInfoVO userVO = userInfoConverter.entityToVo(userInfo);
        return Result.success(userVO);
    }

    /**
     * 检查用户名是否存在
     */
    @PostMapping("/checkName")
    public Result<Boolean> checkUsername(@RequestBody CheckUsernameRequest request) {
        boolean exists = userInfoService.checkUsernameExists(request.getUsername(), request.getExcludeId());
        return Result.success(exists);
    }

    /**
     * 检查用户名请求类
     */
    @Data
    public static class CheckUsernameRequest {
        private String username;
        private Integer excludeId;

    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    @UserPermission
    public Result<UserInfoVO> save(@RequestBody UserInfoDTO userInfoDTO) {
        // Service层处理业务逻辑，返回DTO（可能抛出异常）
        UserInfoDTO createdUser = userInfoService.createUser(userInfoDTO);

        // API层将DTO转换为VO并包装Result
        UserInfoVO userVO = userInfoConverter.dtoToVo(createdUser);
        return Result.success(userVO);
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    @UserPermission
    public Result<UserInfoVO> update(@RequestBody UserInfoDTO userInfoDTO) {
        // Service层处理业务逻辑，返回DTO（可能抛出异常）
        UserInfoDTO updatedUser = userInfoService.updateUser(userInfoDTO);

        // API层将DTO转换为VO并包装Result
        UserInfoVO userVO = userInfoConverter.dtoToVo(updatedUser);
        return Result.success(userVO);
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    @UserPermission
    public Result<Void> delete(@RequestBody Integer[] ids) {
        if (Objects.requireNonNull(SecurityUtils.getAuthUser()).getId() != 1) {
            return Result.error(Status.USER_NO_OPERATION_PERM.getMsg());
        }
        userInfoService.removeByIds(Arrays.asList(ids));

        return Result.success();
    }

}
