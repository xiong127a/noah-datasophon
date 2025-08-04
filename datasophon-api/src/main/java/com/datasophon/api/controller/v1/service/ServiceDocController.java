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

import com.datasophon.api.converter.ServiceDocConverter;
import com.datasophon.api.service.DocService;
import com.datasophon.common.dto.ServiceDocDTO;
import com.datasophon.api.dto.Result;
import com.datasophon.common.vo.ServiceDocVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.datasophon.api.annotation.ApiVersion;

import java.util.Map;

/**
 * 服务文档管理控制器
 * 按照三层架构规范，使用DTO接收请求，VO返回响应
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
@ApiVersion(path = "service/doc")
@Slf4j
@RequiredArgsConstructor
public class ServiceDocController {

    private final DocService docService;
    private final ServiceDocConverter serviceDocConverter;



    /**
     * 获取服务文档
     */
    @PostMapping("/getServiceDoc")
    public Result<ServiceDocVO> getServiceDoc(@RequestBody Map<String, Object> params) {
        try {
            log.debug("获取服务文档, 参数: {}", params);

            // 参数解析和验证
            Integer clusterId = parseIntegerParam(params, "clusterId", "集群ID");
            Integer serviceId = parseIntegerParam(params, "serviceId", "服务ID");
            String type = parseStringParam(params, "type", "文档类型");

            // 获取文档DTO
            ServiceDocDTO serviceDocDTO = docService.getServiceDoc(clusterId, serviceId, type);
            
            // 转换为VO
            ServiceDocVO serviceDocVO = serviceDocConverter.dtoToVo(serviceDocDTO);
            
            return Result.success(serviceDocVO);
            
        } catch (Exception e) {
            log.error("获取服务文档失败: {}", e.getMessage(), e);
            return Result.error("获取服务文档失败: " + e.getMessage());
        }
    }

    /**
     * 检查服务文档是否存在
     */
    @GetMapping("/hasServiceDoc")
    public Result<Boolean> hasServiceDoc(
            @RequestParam("clusterId") Integer clusterId,
            @RequestParam("serviceId") Integer serviceId,
            @RequestParam("type") String type) {
        try {
            boolean exists = docService.hasServiceDoc(clusterId, serviceId, type);
            return Result.success(exists);
        } catch (Exception e) {
            log.error("检查服务文档存在性失败: {}", e.getMessage(), e);
            return Result.error("检查服务文档存在性失败: " + e.getMessage());
        }
    }

    /**
     * 获取服务名称
     */
    @GetMapping("/serviceName/{serviceId}")
    public Result<String> getServiceName(@PathVariable("serviceId") Integer serviceId) {
        try {
            String serviceName = docService.getServiceName(serviceId);
            return Result.success(serviceName);
        } catch (Exception e) {
            log.error("获取服务名称失败: {}", e.getMessage(), e);
            return Result.error("获取服务名称失败: " + e.getMessage());
        }
    }

    /**
     * 获取文档中引用的图片资源
     */
    @GetMapping(value = "/image")
    public ResponseEntity<Resource> getImageByPath(@RequestParam("imagePath") String imagePath) {
        try {
            log.debug("获取图片资源, 路径: {}", imagePath);

            Resource resource = docService.getImageResource(imagePath);

            return ResponseEntity
                    .ok()
                    .contentType(MediaTypeFactory.getMediaType(resource)
                            .orElse(MediaType.APPLICATION_OCTET_STREAM))
                    .body(resource);
                    
        } catch (Exception e) {
            log.error("获取图片资源失败: {}", e.getMessage(), e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 解析整数参数
     */
    private Integer parseIntegerParam(Map<String, Object> params, String key, String paramName) {
        Object value = params.get(key);
        if (value == null) {
            throw new IllegalArgumentException(paramName + "不能为空");
        }
        
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(paramName + "格式错误");
        }
    }

    /**
     * 解析字符串参数
     */
    private String parseStringParam(Map<String, Object> params, String key, String paramName) {
        Object value = params.get(key);
        if (value == null) {
            throw new IllegalArgumentException(paramName + "不能为空");
        }
        
        String strValue = value.toString().trim();
        if (strValue.isEmpty()) {
            throw new IllegalArgumentException(paramName + "不能为空");
        }
        
        return strValue;
    }
}
