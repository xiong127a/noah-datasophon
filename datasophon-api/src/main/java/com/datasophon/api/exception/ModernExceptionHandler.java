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

package com.datasophon.api.exception;

import com.datasophon.api.dto.Result;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 现代化异常处理器
 * 使用JDK 21和Spring Boot 3.5的现代特性处理异常
 * 
 * @author datasophon
 */
@Slf4j
@RestControllerAdvice
public class ModernExceptionHandler {

    /**
     * 错误响应记录
     * 使用JDK 21记录类定义不可变的错误响应
     */
    public record ErrorResponse(
        String timestamp,
        int status,
        String error,
        String message,
        String path,
        String traceId,
        Map<String, Object> details
    ) {
        
        /**
         * 创建错误响应的静态工厂方法
         */
        public static ErrorResponse of(
            HttpStatus status, 
            String message, 
            String path, 
            String traceId,
            Map<String, Object> details
        ) {
            return new ErrorResponse(
                LocalDateTime.now().toString(),
                status.value(),
                status.getReasonPhrase(),
                message,
                path,
                traceId,
                details != null ? details : Map.of()
            );
        }
        
        /**
         * 简化的错误响应创建方法
         */
        public static ErrorResponse of(HttpStatus status, String message, String path) {
            return of(status, message, path, null, null);
        }
    }

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<ErrorResponse>> handleBusinessException(
            BusinessException ex, 
            WebRequest request
    ) {
        var path = request.getDescription(false).replace("uri=", "");
        var errorResponse = ErrorResponse.of(
            HttpStatus.BAD_REQUEST, 
            ex.getMessage(), 
            path
        );
        
        log.warn("业务异常: {} - 路径: {}", ex.getMessage(), path);
        
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(Result.error(errorResponse.message()));
    }

    /**
     * 处理参数验证异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Result<ErrorResponse>> handleIllegalArgumentException(
            IllegalArgumentException ex, 
            WebRequest request
    ) {
        var path = request.getDescription(false).replace("uri=", "");
        var errorResponse = ErrorResponse.of(
            HttpStatus.BAD_REQUEST, 
            ex.getMessage(), 
            path
        );
        
        log.warn("参数验证异常: {} - 路径: {}", ex.getMessage(), path);
        
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(Result.error("参数验证失败: " + errorResponse.message()));
    }

    /**
     * 处理系统异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<ErrorResponse>> handleGenericException(
            Exception ex, 
            WebRequest request
    ) {
        var path = request.getDescription(false).replace("uri=", "");
        Map<String, Object> errorDetails = Map.of(
            "exception.class", ex.getClass().getSimpleName(),
            "thread.name", Thread.currentThread().getName(),
            "thread.virtual", Thread.currentThread().isVirtual()
        );
        
        log.error("系统异常: {} - 路径: {} - 线程: {} (虚拟: {}) - 详情: {}", 
            ex.getMessage(), 
            path, 
            Thread.currentThread().getName(),
            Thread.currentThread().isVirtual(),
            errorDetails,
            ex
        );
        
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Result.error("系统内部错误，请联系管理员"));
    }

    /**
     * 业务异常类
     * 使用Lombok和现代化的异常设计模式
     */
    @Getter
    public static class BusinessException extends RuntimeException {
        
        private final String errorCode;
        private final Map<String, Object> context;
        
        public BusinessException(String message) {
            this(message, "BUSINESS_ERROR", Map.of());
        }
        
        public BusinessException(String message, String errorCode) {
            this(message, errorCode, Map.of());
        }
        
        public BusinessException(String message, String errorCode, Map<String, Object> context) {
            super(message);
            this.errorCode = errorCode;
            this.context = context;
        }
        
        /**
         * 创建业务异常的静态工厂方法
         */
        public static BusinessException of(String message) {
            return new BusinessException(message);
        }
        
        public static BusinessException of(String message, String errorCode) {
            return new BusinessException(message, errorCode);
        }
        
        public static BusinessException of(String message, String errorCode, Map<String, Object> context) {
            return new BusinessException(message, errorCode, context);
        }
    }
}