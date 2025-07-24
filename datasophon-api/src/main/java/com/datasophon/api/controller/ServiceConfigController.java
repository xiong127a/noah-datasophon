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
import com.datasophon.api.utils.CompressUtils;
import com.datasophon.common.model.ConfigFile;
import com.datasophon.common.utils.Result;
import lombok.extern.slf4j.Slf4j;
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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 服务配置文件控制器
 */
@RestController
@RequestMapping("/api/service/config")
@Slf4j
public class ServiceConfigController {

    private final ServiceConfigFileService serviceConfigFileService;

    public ServiceConfigController(ServiceConfigFileService serviceConfigFileService) {
        this.serviceConfigFileService = serviceConfigFileService;
    }

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
     * 验证压缩格式是否支持
     */
    private boolean isValidFormat(String format) {
        return "zip".equals(format) || "tar.gz".equals(format) || "7z".equals(format) ||
                "rar".equals(format) || "bz2".equals(format) || "gzip".equals(format);
    }

    /**
     * 打包下载所有配置文件
     */
    @GetMapping("/downloadAllFiles")
    public ResponseEntity<InputStreamResource> downloadAllServiceConfigFiles(
            @RequestParam("serviceInstanceId") Integer serviceInstanceId,
            @RequestParam(value = "format", defaultValue = "zip") String format,
            @RequestParam(value = "password", required = false) String password) {
        try {
            // 验证格式参数
            if (!isValidFormat(format)) {
                log.warn("不支持的压缩格式: {}", format);
                format = "zip"; // 默认使用zip格式
            }

            // 根据格式获取对应的压缩文件内容
            byte[] compressedContent;
            // 有密码参数，使用带密码的方法
            compressedContent = serviceConfigFileService.getAllServiceConfigFiles(serviceInstanceId, format,
                    password);

            // 获取服务名称
            String serviceName = serviceConfigFileService.getServiceName(serviceInstanceId);

            // 根据不同格式设置不同的文件扩展名
            String fileExtension = "tar.gz".equals(format) ? ".tar.gz" : "." + format;
            String fileName = serviceName + "_configs" + fileExtension;

            // 设置响应头
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName);

            // 根据不同格式设置不同的媒体类型
            MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
            mediaType = switch (format) {
                case "zip" -> MediaType.parseMediaType("application/zip");
                case "tar.gz", "gzip" -> MediaType.parseMediaType("application/gzip");
                case "7z" -> MediaType.parseMediaType("application/x-7z-compressed");
                case "rar" -> MediaType.parseMediaType("application/vnd.rar");
                case "bz2" -> MediaType.parseMediaType("application/x-bzip2");
                default -> mediaType;
            };

            // 返回文件流
            InputStream inputStream = new ByteArrayInputStream(compressedContent);
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(mediaType)
                    .body(new InputStreamResource(inputStream));
        } catch (Exception e) {
            log.error("打包下载配置文件失败", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 获取打包进度
     */
    @GetMapping("/getCompressProgress")
    public Result getCompressProgress(@RequestParam("serviceInstanceId") Integer serviceInstanceId) {
        try {
            Integer progress = serviceConfigFileService.getCompressProgress(serviceInstanceId);
            log.debug("获取服务ID[{}]的压缩进度: {}%", serviceInstanceId, progress);
            return Result.success(progress);
        } catch (Exception e) {
            log.error("获取打包进度失败", e);
            return Result.error("获取打包进度失败: " + e.getMessage());
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
            return Result.success(new String(fileContent, StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("预览配置文件失败", e);
            return Result.error("预览配置文件失败: " + e.getMessage());
        }
    }

    /**
     * 获取系统支持的压缩格式列表
     */
    @GetMapping("/getSupportedCompressFormats")
    public Result getSupportedCompressFormats() {
        try {
            // 获取所有支持的压缩格式
            List<Map<String, String>> formats = Arrays.stream(CompressUtils.CompressType.values())
                    .map(type -> {
                        Map<String, String> formatInfo = new HashMap<>();
                        formatInfo.put("format", type.getExtension());
                        formatInfo.put("description", getFormatDescription(type.getExtension()));
                        formatInfo.put("supportPassword", getSupportPasswordStatus(type.getExtension()));
                        return formatInfo;
                    })
                    .collect(Collectors.toList());

            return Result.success(formats);
        } catch (Exception e) {
            log.error("获取支持的压缩格式列表失败", e);
            return Result.error("获取支持的压缩格式列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取格式描述信息
     */
    private String getFormatDescription(String format) {
        Map<String, String> descriptions = new HashMap<>();
        descriptions.put("zip", "兼容性最佳，几乎所有系统都支持");
        descriptions.put("tar.gz", "Linux/Unix系统常用格式，压缩率高");
        descriptions.put("7z", "高压缩率，标准7z格式（不支持密码保护）");
        descriptions.put("tar", "无压缩的归档格式，用于打包多个文件");
        descriptions.put("tar.xz", "Linux系统常用高压缩率格式");
        descriptions.put("gz", "GZIP格式，单文件压缩，常用于Linux系统");
        descriptions.put("bz2", "BZIP2格式，高压缩率，通常用于Linux系统");

        return descriptions.getOrDefault(format, "压缩文件格式");
    }

    /**
     * 获取格式是否支持密码保护
     */
    private String getSupportPasswordStatus(String format) {
        // 目前仅zip和7z支持密码保护
        if ("zip".equals(format)) {
            return "需安装zip4j库";
        } else if ("7z".equals(format)) {
            return "不支持";
        } else {
            return "不支持";
        }
    }
}