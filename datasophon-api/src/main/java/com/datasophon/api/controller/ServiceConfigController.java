/*
 *
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
 *
 */

package com.datasophon.api.controller;

import com.datasophon.api.service.ServiceConfigFileService;
import com.datasophon.common.model.ConfigFile;
import com.datasophon.common.utils.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * 服务配置文件控制器
 */
@RestController
@RequestMapping("/api/service/config")
@Slf4j
public class ServiceConfigController {

    @Autowired
    private ServiceConfigFileService serviceConfigFileService;

    /**
     * 获取服务配置文件列表
     */
    @PostMapping("/getConfigFiles")
    public Result getServiceConfigFiles(@RequestBody Map<String, Object> params) {
        try {
            Integer serviceInstanceId = Integer.parseInt(params.get("serviceInstanceId").toString());
            List<ConfigFile> configFiles = serviceConfigFileService.getServiceConfigFiles(serviceInstanceId);
            return Result.success(configFiles);
        } catch (Exception e) {
            log.error("获取服务配置文件列表失败", e);
            return Result.error("获取服务配置文件列表失败: " + e.getMessage());
        }
    }

    /**
     * 下载单个配置文件
     */
    @GetMapping("/downloadFile")
    public ResponseEntity<InputStreamResource> downloadServiceConfigFile(
            @RequestParam("serviceInstanceId") Integer serviceInstanceId,
            @RequestParam("fileName") String fileName) {
        try {
            byte[] fileContent = serviceConfigFileService.getServiceConfigFileContent(serviceInstanceId, fileName);

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName);

            InputStream inputStream = new ByteArrayInputStream(fileContent);
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(new InputStreamResource(inputStream));
        } catch (Exception e) {
            log.error("下载配置文件失败", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 打包下载所有配置文件
     */
    @GetMapping("/downloadAllFiles")
    public ResponseEntity<InputStreamResource> downloadAllServiceConfigFiles(
            @RequestParam("serviceInstanceId") Integer serviceInstanceId,
            HttpServletResponse response) {
        try {
            byte[] zipContent = serviceConfigFileService.getAllServiceConfigFilesAsZip(serviceInstanceId);

            String serviceName = serviceConfigFileService.getServiceName(serviceInstanceId);
            String fileName = serviceName + "_configs.zip";

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName);

            InputStream inputStream = new ByteArrayInputStream(zipContent);
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(new InputStreamResource(inputStream));
        } catch (Exception e) {
            log.error("打包下载配置文件失败", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 预览配置文件内容
     */
    @PostMapping("/previewFile")
    public Result previewServiceConfigFile(@RequestBody Map<String, Object> params) {
        try {
            Integer serviceInstanceId = Integer.parseInt(params.get("serviceInstanceId").toString());
            String fileName = params.get("fileName").toString();

            byte[] fileContent = serviceConfigFileService.getServiceConfigFileContent(serviceInstanceId, fileName);
            return Result.success(new String(fileContent, "UTF-8"));
        } catch (Exception e) {
            log.error("预览配置文件失败", e);
            return Result.error("预览配置文件失败: " + e.getMessage());
        }
    }
}