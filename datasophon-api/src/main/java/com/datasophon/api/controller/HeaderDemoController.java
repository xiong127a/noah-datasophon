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

import com.datasophon.common.web.HeaderContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 请求头示例控制器
 * 用于演示如何使用HeaderContextHolder获取请求头信息
 */
@RestController
@RequestMapping("/api/header-demo")
public class HeaderDemoController {

    /**
     * 获取所有请求头信息
     *
     * @return 所有请求头信息的Map
     */
    @GetMapping("/headers")
    public ResponseEntity<Map<String, Object>> getHeaders() {
        Map<String, Object> result = new HashMap<>();
        result.put("headers", HeaderContextHolder.getAllHeaders());

        // 获取特定请求头
        String authorization = HeaderContextHolder.getHeader("Authorization");
        String grafanaHost = HeaderContextHolder.getHeader("grafanaHost");

        result.put("grafanaHost", grafanaHost);
        result.put("hasAuthorization", authorization != null);

        return ResponseEntity.ok(result);
    }

    /**
     * 测试在服务层使用请求头
     *
     * @return 服务层处理结果
     */
    @GetMapping("/service-demo")
    public ResponseEntity<String> serviceDemo() {
        // 在服务层中，可以直接通过HeaderContextHolder获取请求头，不需要通过参数传递
        String grafanaHost = HeaderContextHolder.getHeader("grafanaHost");

        // 这里可以调用实际的服务层方法，处理业务逻辑

        return ResponseEntity.ok("从HeaderContextHolder获取到grafanaHost: " +
                (grafanaHost != null ? grafanaHost : "未找到"));
    }
}