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

package com.datasophon.api.load.model;

/**
 * JDK 21 Record: 服务DDL解析结果
 * 使用密封类型提供类型安全的解析结果封装
 */
public sealed interface ParseResult 
    permits ParseResult.Success, ParseResult.Failure {
    
    /**
     * 解析成功的结果
     */
    record Success(ServiceMetaConfig config) implements ParseResult {
        
        /**
         * 获取服务名称
         */
        public String serviceName() {
            return config.serviceName();
        }
        
        /**
         * 获取框架代码
         */
        public String frameCode() {
            return config.frameCode();
        }
    }
    
    /**
     * 解析失败的结果
     */
    record Failure(String serviceName, String errorMessage, Throwable cause) implements ParseResult {
        
        /**
         * 创建失败结果的便捷方法
         */
        public static Failure of(String serviceName, String errorMessage) {
            return new Failure(serviceName, errorMessage, null);
        }
        
        /**
         * 创建带异常的失败结果
         */
        public static Failure of(String serviceName, String errorMessage, Throwable cause) {
            return new Failure(serviceName, errorMessage, cause);
        }
        
        /**
         * 获取完整的错误描述
         */
        public String fullErrorMessage() {
            return cause != null 
                ? String.format("%s: %s", errorMessage, cause.getMessage())
                : errorMessage;
        }
    }
    
    /**
     * 判断是否为成功结果
     */
    default boolean isSuccess() {
        return this instanceof Success;
    }
    
    /**
     * 判断是否为失败结果
     */
    default boolean isFailure() {
        return this instanceof Failure;
    }
    
    /**
     * 如果是成功结果，执行给定的动作
     */
    default ParseResult ifSuccess(java.util.function.Consumer<Success> action) {
        if (this instanceof Success success) {
            action.accept(success);
        }
        return this;
    }
    
    /**
     * 如果是失败结果，执行给定的动作
     */
    default ParseResult ifFailure(java.util.function.Consumer<Failure> action) {
        if (this instanceof Failure failure) {
            action.accept(failure);
        }
        return this;
    }
}