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

package com.datasophon.plugins.api.factory;

import com.datasophon.plugins.api.service.SshConnectionService;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * SSH连接服务工厂 - 提供SshConnectionService实例
 * 
 * 设计原则：
 * 1. 插件API模块直接提供SSH服务工厂，无需依赖server模块
 * 2. 通过反射创建具体的SSH服务实现实例
 * 3. 提供单例缓存机制，提高性能
 * 4. 支持多种SSH服务实现的动态切换
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2024-12-19
 */
@Slf4j
public class SshConnectionServiceFactory {
    
    private static final String DEFAULT_SSH_SERVICE_IMPL = "com.datasophon.plugins.impl.ssh.SshConnectionServiceImpl";
    private static SshConnectionServiceFactory instance;
    private static final Object lock = new Object();
    
    // 服务实例缓存
    private final Map<String, SshConnectionService> serviceCache = new ConcurrentHashMap<>();
    
    /**
     * 获取工厂单例
     */
    public static SshConnectionServiceFactory getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new SshConnectionServiceFactory();
                }
            }
        }
        return instance;
    }
    
    /**
     * 获取默认的SSH连接服务实例
     */
    public SshConnectionService getDefaultSshConnectionService() {
        return getSshConnectionService(DEFAULT_SSH_SERVICE_IMPL);
    }
    
    /**
     * 获取指定实现类的SSH连接服务实例
     * 
     * @param implClassName 实现类全限定名
     * @return SSH连接服务实例
     */
    public SshConnectionService getSshConnectionService(String implClassName) {
        return serviceCache.computeIfAbsent(implClassName, className -> {
            try {
                Class<?> serviceClass = Class.forName(className);
                Object serviceInstance = serviceClass.getDeclaredConstructor().newInstance();
                
                if (serviceInstance instanceof SshConnectionService) {
                    log.info("【SSH服务工厂】成功创建SSH连接服务实例: {}", className);
                    return (SshConnectionService) serviceInstance;
                } else {
                    log.error("【SSH服务工厂】类不是SshConnectionService的实现: {}", className);
                    return null;
                }
                
            } catch (Exception e) {
                log.error("【SSH服务工厂】无法创建SSH连接服务实例: {}, 错误: {}", className, e.getMessage(), e);
                return null;
            }
        });
    }
    
    /**
     * 检查SSH连接服务是否可用
     * 
     * @return true如果可用，false如果不可用
     */
    public boolean isSshConnectionServiceAvailable() {
        SshConnectionService service = getDefaultSshConnectionService();
        return service != null;
    }
    
    /**
     * 获取所有已缓存的服务实例信息
     * 
     * @return 服务信息映射
     */
    public Map<String, Object> getCachedServiceInfo() {
        Map<String, Object> info = new ConcurrentHashMap<>();
        info.put("cacheSize", serviceCache.size());
        info.put("defaultImplClass", DEFAULT_SSH_SERVICE_IMPL);
        info.put("isDefaultServiceAvailable", isSshConnectionServiceAvailable());
        
        // 添加每个缓存服务的状态
        serviceCache.forEach((className, service) -> {
            info.put(className + "_status", service != null ? "available" : "unavailable");
        });
        
        return info;
    }
    
    /**
     * 清理服务缓存
     */
    public void clearCache() {
        log.info("【SSH服务工厂】清理服务缓存");
        serviceCache.clear();
    }
    
    /**
     * 重新加载指定的SSH连接服务
     * 
     * @param implClassName 实现类全限定名
     * @return 重新加载的服务实例
     */
    public SshConnectionService reloadSshConnectionService(String implClassName) {
        log.info("【SSH服务工厂】重新加载SSH连接服务: {}", implClassName);
        serviceCache.remove(implClassName);
        return getSshConnectionService(implClassName);
    }
}
