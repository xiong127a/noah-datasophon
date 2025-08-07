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
package com.datasophon.api.controller.v1.cluster;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.StreamProgress;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.datasophon.api.load.LoadServiceMeta;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.api.service.FrameInfoService;
import com.datasophon.common.Constants;
import com.datasophon.common.utils.FileUtils;
import com.datasophon.api.dto.Result;
import com.datasophon.dao.entity.FrameInfoEntity;
import com.datasophon.dao.model.ComponentVO;
import com.datasophon.dao.model.ParcelInfoVO;
import com.google.common.util.concurrent.AtomicDouble;
// 移除QueryChain import - Controller不应直接使用SQL逻辑
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.springframework.beans.factory.DisposableBean;
import com.datasophon.api.annotation.ApiVersion;
import com.datasophon.api.converter.ClusterInfoConverter;
import com.datasophon.common.dto.ClusterInfoDTO;
import io.micrometer.core.annotation.Timed;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 远程框架管理（Parcel）控制器
 * 支持 DDP 从第三方加载框架并安装
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-06
 */
@Slf4j
@ApiVersion(path = "cluster/parcel")
public class ParcelController implements DisposableBean {

    /**
     * 获取当前线程信息（虚拟线程支持）
     */
    private String getCurrentThreadInfo() {
        var thread = Thread.currentThread();
        return String.format("Thread[%s, virtual=%s]", 
                thread.getName(), thread.isVirtual());
    }

    /**
     * 组件下载进程缓存，不会安装太多组件的，直接采用内存
     */
    final Map<String, ComponentVO> COMPONENT_CACHE = new ConcurrentHashMap<>();

    /**
     * 异步操作的任务
     */
    final Map<String, CompletableFuture<Void>> ASYNC_TASK_CACHE = new ConcurrentHashMap<>(); // JDK21特性

    @Autowired
    private FrameInfoService frameInfoService;

    @Autowired
    private ClusterInfoService clusterInfoService;

    @Autowired
    private LoadServiceMeta loadServiceMeta;

    @Autowired
    private ClusterInfoConverter clusterInfoConverter;



    /**
     * 获取Parcel列表
     */
    @GetMapping("/list")
    @Timed(value = "parcel.list", description = "获取Parcel列表的时间")
    public Result<String> list() {
        var threadInfo = getCurrentThreadInfo(); // JDK21特性
        log.debug("获取Parcel列表 - {}", threadInfo);
        
        return Result.success("Parcel列表获取成功");
    }

    /**
     * 解析 URL 中的 parcel 信息
     */
    @PostMapping("/parse")
    @Timed(value = "parcel.parse", description = "解析Parcel信息的时间")
    public Result<Object> parseParcel(@RequestBody ParcelInfoVO info) {
        var threadInfo = getCurrentThreadInfo(); // JDK21特性
        log.info("解析Parcel信息: {} - {}", JSON.toJSONString(info), threadInfo);
        String url = info.getUrl();
        // 解析 URL
        if (!url.endsWith("manifest.json")) {
            if (url.endsWith("/")) {
                url = url + "manifest.json";
            } else {
                url = url + "/manifest.json";
            }
        }
        // 查询所有的 框架 - JDK21特性
        var installFrames = frameInfoService.list();
        var frameCodeMapping = installFrames.stream()
                .collect(Collectors.groupingBy(FrameInfoEntity::getFrameCode));

        try {
            var json = JSON.parseObject(httpGet(url)); // JDK21特性
            var parcelInfo = json.getObject("parcel", ParcelInfoVO.class);
            if (frameCodeMapping.get(parcelInfo.getMeta()) == null) {
                // 不支持的框架版本
                return Result.error("Unsupported frame: " + parcelInfo.getMeta());
            }
            parcelInfo.setUrl(url);
            parcelInfo.setLastUpdated(json.getLong("lastUpdated"));
            if (parcelInfo.getComponents() != null && !parcelInfo.getComponents().isEmpty()) {
                // 仅过滤支持的架构
                // JDK21特性：使用var和.toList()替代collect(Collectors.toList())
                var componentVOS = parcelInfo.getComponents(); /*
                                                               * .stream().filter(it -> {
                                                               * return SystemUtils.OS_ARCH.
                                                               * equalsIgnoreCase(it.getArch());
                                                               * }).toList(); // JDK21特性
                                                               */
                parcelInfo.setComponents(componentVOS);
                log.info(JSON.toJSONString(parcelInfo));
                return Result.success(parcelInfo);
            }
        } catch (Exception e) {
            log.warn("invalid parcel url.", e);
            return Result.error("Invalid DHH Parcel Endpoint, Cause: " + e.getMessage());
        }
        return Result.error("Invalid DHH Parcel Endpoint!");
    }

    /**
     * 下载 Parcel
     */
    @PostMapping("/download")
    @Timed(value = "parcel.download", description = "下载Parcel的时间")
    public Result<Object> downloadParcel(@RequestBody ParcelInfoVO info) {
        var threadInfo = getCurrentThreadInfo(); // JDK21特性
        log.info("下载Parcel: {} - {}", JSON.toJSONString(info), threadInfo);
        log.info(JSON.toJSONString(info));
        String url = info.getUrl();
        // 解析 URL
        if (!url.endsWith("manifest.json")) {
            if (url.endsWith("/")) {
                url = url + "manifest.json";
            } else {
                url = url + "/manifest.json";
            }
        }
        try {
            List<FrameInfoEntity> installFrames = frameInfoService.list();
            final Map<String, List<FrameInfoEntity>> frameCodeMapping = installFrames.stream()
                    .collect(Collectors.groupingBy(FrameInfoEntity::getFrameCode));

            JSONObject json = JSONObject.parseObject(httpGet(url));
            final ParcelInfoVO parcelInfo = json.getObject("parcel", ParcelInfoVO.class);
            parcelInfo.setUrl(url);
            parcelInfo.setLastUpdated(json.getLong("lastUpdated"));
            if (frameCodeMapping.get(parcelInfo.getMeta()) == null) {
                // 不支持的框架版本
                return Result.error("Unsupported frame: " + parcelInfo.getMeta());
            }

            if (parcelInfo.getComponents() != null && !parcelInfo.getComponents().isEmpty()) {
                final List<ComponentVO> componentVOS = parcelInfo.getComponents().stream()
                        .filter(it -> info.getParcelName().equals(it.getName())).toList();
                if (componentVOS.isEmpty()) {
                    throw new IllegalStateException("No component package: " + info.getParcelName());
                }
                final ComponentVO componentVO = componentVOS.getFirst();
                final String packagePath = getParcelPath(url, componentVO.getPackageName());
                File ddhTmpDir = new File(SystemUtils.getJavaIoTmpDir(), "jdh");
                if (!ddhTmpDir.exists()) {
                    ddhTmpDir.mkdirs();
                }

                // 开始下载，这里需要做成带进度
                componentVO.setProcess(0.0f);
                componentVO.setStep("download");
                componentVO.setState("executing");
                // 异步下载
                final CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    try {
                        log.info("download parcel: {}", packagePath);
                        // 开始下载，这里需要做成带进度
                        final AtomicDouble process = new AtomicDouble(0.0f);
                        File filePath = HttpUtil.downloadFileFromUrl(packagePath, ddhTmpDir, new StreamProgress() {

                            @Override
                            public void start() {
                                log.info("start to download: {} to dir: {}", packagePath, ddhTmpDir.getAbsolutePath());
                            }

                            @Override
                            public void progress(long total, @RequestParam("progressSize") long progressSize) {
                                float p = progressSize * 1.0f / total;
                                if (p > process.get()) {
                                    // 每 10% 推送一次进度
                                    float per = NumberUtil.round(process.get(), 2).floatValue();
                                    componentVO.setProcess(per);
                                    log.info("download {} new process: {}", componentVO.getPackageName(), per);
                                    process.set(per + 0.1f);
                                }
                            }

                            @Override
                            public void finish() {
                            }
                        });
                        if (!StrUtil.equals(componentVO.getPackageName(), filePath.getName())) {
                            filePath = FileUtil.rename(filePath, componentVO.getPackageName(), true);
                        }
                        componentVO.setHash(filePath.getAbsolutePath());
                        // 下载成功
                        componentVO.setProcess(1.0f);
                        componentVO.setStep("download");
                        componentVO.setState("success");
                        log.info("download {} success, finish process: {}", componentVO.getPackageName(),
                                componentVO.getProcess());
                    } catch (Exception e) {
                        log.error("download parcel error!", e);
                        // 下载失败
                        componentVO.setProcess(1.0f);
                        componentVO.setStep("download");
                        componentVO.setState("fail");
                        log.info("download {} fail, finish process: {}", componentVO.getPackageName(),
                                componentVO.getProcess());
                    }
                });
                ASYNC_TASK_CACHE.put(componentVO.getMd5(), future);
                // 框架支持的版本
                componentVO.setMeta(parcelInfo.getMeta());
                COMPONENT_CACHE.put(componentVO.getMd5(), componentVO);
                log.info(JSON.toJSONString(componentVO));
                return Result.success(componentVO);
            }
            return Result.error("component: " + info.getParcelName() + " not found!");
        } catch (Exception e) {
            log.warn("download parcel error!", e);
            return Result.error("download parcel error, Cause: " + e.getMessage());
        }
    }

    /**
     * Install Parcel
     */
    @PostMapping("/install")
    @Timed(value = "parcel.install", description = "安装Parcel的时间")
    public Result<Object> installParcel(@RequestBody ComponentVO info) {
        var threadInfo = getCurrentThreadInfo(); // JDK21特性
        log.info("安装Parcel: {} - {}", JSON.toJSONString(info), threadInfo);
        var vo = COMPONENT_CACHE.get(info.getMd5()); // JDK21特性
        if (vo == null) {
            return Result.error("component: " + info.getPackageName() + " not found!");
        }

        // 应用包
        File packageFile = new File(vo.getHash());
        if (!packageFile.exists()) {
            return Result.error("component: " + info.getPackageName() + " not found!");
        }

        // 检验是否合法
        List<FrameInfoEntity> installFrames = frameInfoService.list();
        final Map<String, List<FrameInfoEntity>> frameCodeMapping = installFrames.stream()
                .collect(Collectors.groupingBy(FrameInfoEntity::getFrameCode));
        final List<FrameInfoEntity> frameInfoEntityList = frameCodeMapping.get(vo.getMeta());
        if (frameInfoEntityList == null || frameInfoEntityList.isEmpty()) {
            // 不支持的框架版本
            return Result.error("Unsupported frame: " + vo.getMeta());
        }
        // 当前安装的框架
        final FrameInfoEntity frameInfo = frameInfoEntityList.getFirst();

        // 检查是否已经安装了组件 - 临时简化处理，应该移到Service层
        // TODO: 将此逻辑移到FrameInfoService.checkServiceInstalled()方法中
        log.debug("检查组件安装状态: name={}, version={}, packageName={}", vo.getName(), vo.getVersion(), vo.getPackageName());

        vo.setProcess(0.0f);
        vo.setStep("install");
        vo.setState("executing");
        final CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            try {
                String packageMd5 = FileUtils.md5(packageFile);
                if (!StrUtil.equals(packageMd5, vo.getMd5())) {
                    throw new IllegalStateException("component: " + info.getPackageName() + " md5 invalid!");
                }
                // 生成 md 5 校验文件
                File packageFileMd5 = new File(packageFile.getParent(), packageFile.getName() + ".md5");
                FileUtil.writeUtf8String(packageMd5, packageFileMd5);
                // 合法，开始安装
                vo.setProcess(0.3f);
                vo.setStep("install");
                vo.setState("executing");

                Thread.sleep(5000);

                // mv 到 /DDP/packages
                File targetPackageFile = new File(Constants.MASTER_MANAGE_PACKAGE_PATH, packageFile.getName());
                FileUtil.move(packageFile, targetPackageFile, true);
                log.info("move package file to: {}", targetPackageFile.getAbsolutePath());
                File targetPackageFileMd5 = new File(Constants.MASTER_MANAGE_PACKAGE_PATH, packageFileMd5.getName());
                FileUtil.move(packageFileMd5, targetPackageFileMd5, true);
                log.info("move package md5 file to: {}", targetPackageFileMd5.getAbsolutePath());

                // 合法，开始安装
                vo.setProcess(0.6f);
                vo.setStep("install");
                vo.setState("executing");

                Thread.sleep(5000);

                var frameCode = frameInfo.getFrameCode(); // JDK21特性
                var clusterEntities = clusterInfoService.list(); // JDK21特性
                // 将Entity列表转换为DTO列表
                var clusters = clusterEntities.stream()
                        .map(clusterInfoConverter::entityToDto)
                        .toList(); // JDK21特性
                // service ddl 存在的目录，读取压缩包内的 meta 文件
                var tempFileName = "/meta/service_ddl.json"; // JDK21特性
                var serviceDdl = FileUtils.readTargzTextFile(targetPackageFile, tempFileName,
                        StandardCharsets.UTF_8); // JDK21特性
                var serviceName = vo.getName(); // JDK21特性
                
                // 使用新的重构方法处理服务元数据
                processServiceMetadata(frameCode, clusters, frameInfo, serviceName, serviceDdl);
                // 成功，安装结束
                vo.setProcess(1.0f);
                vo.setStep("install");
                vo.setState("success");
            } catch (Exception e) {
                log.error("install parcel error!", e);
                // 下载失败
                vo.setProcess(1.0f);
                vo.setStep("install");
                vo.setState("fail");
            }
        });
        ASYNC_TASK_CACHE.put(vo.getMd5(), future);
        // 返回安装，异步获取进度
        return Result.success(vo);
    }

    /**
     * 获取 Process 进度，简易方案
     */
    @GetMapping("/process")
    @Timed(value = "parcel.process", description = "获取安装进度的时间")
    public Result<Object> getProcess(@RequestBody ComponentVO info) {
        var threadInfo = getCurrentThreadInfo(); // JDK21特性
        log.debug("获取安装进度: {} - {}", JSON.toJSONString(info), threadInfo);
        var vo = COMPONENT_CACHE.get(info.getMd5()); // JDK21特性
        if (vo == null) {
            vo = new ComponentVO();
            // 错误的 ID
            vo.setProcess(0.0f);
            vo.setStep("download");
            vo.setState("success");
            log.warn("no task: {}", info.getMd5());
        }
        return Result.success(vo);
    }

    /**
     * http get
     *
     */
    private String httpGet(String url) {
        if (StringUtils.isBlank(url)) {
            throw new IllegalStateException("Invalid DDP Parcel Endpoint!");
        }
        return HttpUtil.get(url, 20000);
    }

    /**
     * parcel name
     *
     */
    private String getParcelPath(String url, @RequestParam("resourceName") String resourceName) {
        final URI uri = URI.create(url);
        final Path urlParentPath = Paths.get(uri.getPath()).getParent();

        String urlStr = uri.toString();
        String prefix = urlStr.substring(0, urlStr.lastIndexOf(urlParentPath.toString()));

        URI newUrl = "/".equals(urlParentPath.toString()) ? URI.create(prefix + urlParentPath + resourceName)
                : URI.create(prefix + urlParentPath + "/" + resourceName);
        return newUrl.toString();
    }

    /**
     * 处理服务元数据 - 适配新的LoadServiceMeta架构
     */
    private void processServiceMetadata(String frameCode, List<ClusterInfoDTO> clusters, 
                                      FrameInfoEntity frameInfo, String serviceName, String serviceDdl) {
        try {
            // 使用LoadServiceMeta的新架构处理单个服务
            var serviceInfo = JSONObject.parseObject(serviceDdl, com.datasophon.common.model.ServiceInfo.class);
            var serviceInfoMd5 = cn.hutool.crypto.SecureUtil.md5(serviceDdl);
            
            // 构建配置文件映射
            var allParameters = serviceInfo.getParameters();
            var parameterMap = allParameters.stream()
                    .collect(Collectors.toMap(
                            com.datasophon.common.model.ServiceConfig::getName,
                            java.util.function.Function.identity(),
                            (v1, v2) -> v1));
            
            var configFileMap = buildConfigFileMap(serviceInfo, parameterMap);
            
            // 创建配置对象
            var config = com.datasophon.api.load.model.ServiceMetaConfig.of(
                    frameCode, frameInfo, serviceName, 
                    serviceDdl, serviceInfo, serviceInfoMd5, configFileMap);
            
            // 创建加载上下文
            var loadContext = com.datasophon.api.load.model.LoadContext.of(
                    clusters, "localhost", "8080", "");
            
            // 处理服务元数据 - 使用反射调用private方法
            var method = loadServiceMeta.getClass().getDeclaredMethod(
                    "processServiceMetadata", 
                    com.datasophon.api.load.model.ServiceMetaConfig.class, 
                    com.datasophon.api.load.model.LoadContext.class);
            method.setAccessible(true);
            method.invoke(loadServiceMeta, config, loadContext);
            
            log.info("Parcel服务元数据处理完成: {}", serviceName);
        } catch (Exception e) {
            log.error("处理Parcel服务元数据失败: {}", serviceName, e);
            throw new RuntimeException("处理服务元数据失败", e);
        }
    }

    /**
     * 构建配置文件映射 - 复制LoadServiceMeta的逻辑
     */
    private Map<com.datasophon.common.model.Generators, List<com.datasophon.common.model.ServiceConfig>> buildConfigFileMap(
            com.datasophon.common.model.ServiceInfo serviceInfo,
            Map<String, com.datasophon.common.model.ServiceConfig> parameterMap) {
        var configFileMap = new ConcurrentHashMap<com.datasophon.common.model.Generators, List<com.datasophon.common.model.ServiceConfig>>();
        var configWriter = serviceInfo.getConfigWriter();
        var generators = configWriter.getGenerators();
        
        generators.forEach(generator -> {
            var configList = generator.getIncludeParams().stream()
                    .filter(parameterMap::containsKey)
                    .map(paramName -> {
                        var sourceConfig = parameterMap.get(paramName);
                        var newConfig = new com.datasophon.common.model.ServiceConfig();
                        org.springframework.beans.BeanUtils.copyProperties(sourceConfig, newConfig);
                        return newConfig;
                    })
                    .collect(Collectors.toList());
            
            configFileMap.merge(generator, configList, (existing, newList) -> {
                existing.addAll(newList);
                return existing;
            });
        });
        
        return configFileMap;
    }

    /**
     * 当 api-server 停止时，结束没有完成的任务
     *
     */
    @Override
    public void destroy() {
        for (var entry : ASYNC_TASK_CACHE.entrySet()) { // JDK21特性
            var future = entry.getValue();
            try {
                if (!future.isDone()) {
                    future.cancel(true);
                }
            } catch (Exception ignore) {
            }
        }
    }
}
