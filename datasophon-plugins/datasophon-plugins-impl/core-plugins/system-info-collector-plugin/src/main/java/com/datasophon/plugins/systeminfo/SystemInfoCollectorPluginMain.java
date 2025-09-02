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

package com.datasophon.plugins.systeminfo;

import lombok.extern.slf4j.Slf4j;
import org.pf4j.PluginWrapper;
import org.pf4j.spring.SpringPlugin;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * 系统信息收集插件主类
 * PF4J插件的主入口类，集成Spring支持
 * 
 * @author DataSophon Team
 */
@Slf4j
public class SystemInfoCollectorPluginMain extends SpringPlugin {
    
    public SystemInfoCollectorPluginMain(PluginWrapper wrapper) {
        super(wrapper);
    }
    
    @Override
    protected ApplicationContext createApplicationContext() {
        log.info("创建系统信息收集插件Spring上下文");
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
        // 扫描插件包路径
        applicationContext.scan("com.datasophon.plugins.systeminfo");
        applicationContext.refresh();
        log.info("系统信息收集插件Spring上下文创建完成");
        return applicationContext;
    }
    
    @Override
    public void start() {
        log.info("系统信息收集插件启动: {}", wrapper.getPluginId());
        super.start(); // 调用SpringPlugin的start方法
    }
    
    @Override
    public void stop() {
        log.info("系统信息收集插件停止: {}", wrapper.getPluginId());
        super.stop(); // 调用SpringPlugin的stop方法
    }
    
    @Override
    public void delete() {
        log.info("系统信息收集插件删除: {}", wrapper.getPluginId());
        super.delete(); // 调用SpringPlugin的delete方法
    }
}
