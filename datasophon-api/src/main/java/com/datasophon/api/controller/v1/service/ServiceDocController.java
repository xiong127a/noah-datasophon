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

package com.datasophon.api.controller.v1.service;

import com.datasophon.api.service.DocService;
import com.datasophon.api.vo.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.datasophon.api.annotation.ApiVersion;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * 文档管理控制器
 */
@ApiVersion(path = "service/doc")
@Slf4j
public class ServiceDocController {

        @Autowired
        private DocService docService;



        /**
         * 获取服务文档
         *
         * @param params 参数包含：
         *               - clusterId 集群ID
         *               - serviceId 服务ID
         *               - type 文档类型 (component: 组件介绍, guide: 用户指南, help: 帮助文档)
         * @return 文档内容
         */
        @PostMapping("/getServiceDoc")
        public Result getServiceDoc(@RequestBody Map<String, Object> params) {
                log.info("获取服务文档, 参数: {}", params);

                Integer clusterId = params.get("clusterId") != null
                                ? Integer.parseInt(params.get("clusterId").toString())
                                : null;
                Integer serviceId = params.get("serviceId") != null
                                ? Integer.parseInt(params.get("serviceId").toString())
                                : null;
                String type = (String) params.get("type");

                return docService.getServiceDoc(clusterId, serviceId, type);
        }

        /**
         * 获取文档中引用的图片资源(查询参数方式)
         *
         * @param imagePath 图片路径
         * @return 图片资源
         */
        @GetMapping(value = "/image")
        public ResponseEntity<Resource> getImageByPath(@RequestParam(value = "imagePath") String imagePath) {

                log.info("通过查询参数获取图片, 原始路径: {}", imagePath);

                Resource resource = docService.getImageResource(imagePath);

                return ResponseEntity
                                .ok()
                                .contentType(MediaTypeFactory.getMediaType(resource)
                                                .orElse(MediaType.APPLICATION_OCTET_STREAM))
                                .body(resource);
        }
}
