/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.datasophon.common.exception;


import com.datasophon.common.enums.Status;

/**
 * 用户业务异常类
 * 封装用户相关的业务异常
 * 
 * @author DataSophon
 */
public class UserBusinessException extends BusinessException {

    private static final long serialVersionUID = 1L;

    private UserBusinessException(Integer code, String message) {
        super(code, message);
    }

    private UserBusinessException(Integer code, String message, Throwable cause) {
        super(code, message, cause);
    }

    /**
     * 用户名已存在异常
     */
    public static UserBusinessException usernameExists() {
        return new UserBusinessException(Status.USER_NAME_EXIST.getCode(), Status.USER_NAME_EXIST.getMsg());
    }

    /**
     * 用户名已存在异常（自定义消息）
     */
    public static UserBusinessException usernameExists(String username) {
        return new UserBusinessException(Status.USER_NAME_EXIST.getCode(),
                "用户名 '" + username + "' 已存在");
    }

    /**
     * 用户不存在异常
     */
    public static UserBusinessException userNotFound() {
        return new UserBusinessException(Status.USER_NOT_EXIST.getCode(), "用户不存在");
    }

    /**
     * 用户不存在异常（自定义消息）
     */
    public static UserBusinessException userNotFound(String username) {
        return new UserBusinessException(Status.USER_NOT_EXIST.getCode(),
                "用户 '" + username + "' 不存在");
    }

    /**
     * 用户名或密码错误异常
     */
    public static UserBusinessException usernameOrPasswordError() {
        return new UserBusinessException(Status.USER_NAME_PASSWD_ERROR.getCode(),
                Status.USER_NAME_PASSWD_ERROR.getMsg());
    }

    /**
     * 用户名不能为空异常
     */
    public static UserBusinessException usernameIsNull() {
        return new UserBusinessException(Status.USER_NAME_NULL.getCode(),
                Status.USER_NAME_NULL.getMsg());
    }

    /**
     * 创建用户失败异常
     */
    public static UserBusinessException createUserFailed(String reason) {
        return new UserBusinessException(Status.CREATE_USER_ERROR.getCode(),
                "创建用户失败: " + reason);
    }

    /**
     * 更新用户失败异常
     */
    public static UserBusinessException updateUserFailed(String reason) {
        return new UserBusinessException(Status.UPDATE_USER_ERROR.getCode(),
                "更新用户失败: " + reason);
    }
}