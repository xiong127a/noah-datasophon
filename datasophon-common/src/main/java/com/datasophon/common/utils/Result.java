
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

package com.datasophon.common.utils;

import cn.hutool.core.collection.ListUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 统一API响应结果封装
 * 
 * @param <T> 响应数据类型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 状态码
     */
    private Integer code;

    /**
     * 返回消息
     */
    private String msg;

    private long total;

    /**
     * 返回数据
     */
    private T data;

    /**
     * 元数据
     */
    private Map<String, Object> meta;

    /**
     * 错误详情
     */
    private List<Map<String, Object>> errors;

    /**
     * 构造函数
     * 
     * @param code 状态码
     * @param msg  消息
     */
    public Result(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
        this.meta = new HashMap<>();
    }

    /**
     * 构造函数
     * 
     * @param code 状态码
     * @param msg  消息
     * @param data 数据
     */
    public Result(Integer code, String msg, T data) {
        this(code, msg);
        this.data = data;
    }

    /**
     * 构造函数
     * 
     * @param code    状态码
     * @param success 消息
     * @param data    数据
     * @param count   总数
     */
    public Result(Integer code, String success, T data, long count) {
        this(code, success);
        this.data = data;
        this.total = count;
    }

    /**
     * 返回服务器错误
     * 
     * @return 错误结果
     */
    public static <T> Result<T> error() {
        return error(500, "Internal server error");
    }

    /**
     * 返回指定消息的错误
     * 
     * @param msg 错误消息
     * @return 错误结果
     */
    public static <T> Result<T> error(String msg) {
        return error(500, msg);
    }

    /**
     * 返回指定状态码和消息的错误
     * 
     * @param code 状态码
     * @param msg  错误消息
     * @return 错误结果
     */
    public static <T> Result<T> error(int code, String msg) {
        return new Result<>(code, msg);
    }

    /**
     * 返回带有详细错误信息的错误
     * 
     * @param code   状态码
     * @param msg    错误消息
     * @param errors 错误详情列表
     * @return 错误结果
     */
    public static <T> Result<T> error(int code, String msg, List<Map<String, Object>> errors) {
        Result<T> result = error(code, msg);
        result.setErrors(errors);
        return result;
    }

    /**
     * 返回带有数据的成功结果
     * 
     * @param data 数据
     * @return 成功结果
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "success", data);
    }

    /**
     * 返回不带数据的成功结果
     * 
     * @return 成功结果
     */
    public static <T> Result<T> success() {
        return new Result<>(200, "success");
    }

    /**
     * 返回空集合和总数为0的结果
     * 
     * @return 成功结果，包含空列表和总数0
     */
    public static <T> Result<List<T>> successEmptyCount() {
        return new Result<>(200, "success", ListUtil.empty(), 0);
    }

    /**
     * 返回带有数据和总数的成功结果
     * 
     * @param data  数据
     * @param count 总数
     * @return 成功结果
     */
    public static <T> Result<T> success(T data, long count) {
        return new Result<>(200, "success", data, count);
    }

    /**
     * 判断是否成功
     * 
     * @return 是否成功
     */
    public boolean isSuccess() {
        return this.code != null && this.code >= 200 && this.code < 300;
    }

    /**
     * 在结果中添加自定义属性
     * 
     * @param key   键
     * @param value 值
     * @return 当前对象，支持链式调用
     */
    public Result<T> put(String key, Object value) {
        if (this.meta == null) {
            this.meta = new HashMap<>();
        }
        this.meta.put(key, value);
        return this;
    }
}
