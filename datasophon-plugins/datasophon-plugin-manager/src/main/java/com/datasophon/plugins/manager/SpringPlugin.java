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

package com.datasophon.plugins.manager;

import lombok.extern.slf4j.Slf4j;
import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * Spring插件基类
 * 支持Spring Framework内部集成的插件基类
 * 
 * @author 任相鹏
 * @email 635887935@qq.com  
 * @date 2025-01-28
 */
@Slf4j
public abstract class SpringPlugin extends Plugin {
    
    private ApplicationContext applicationContext;
    
    public SpringPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }
    
    @Override
    public void start() {
        log.info("启动Spring插件: {}", getWrapper().getPluginId());
        applicationContext = createApplicationContext();
    }
    
    @Override
    public void stop() {
        log.info("停止Spring插件: {}", getWrapper().getPluginId());
        if (applicationContext != null) {
            try {
                // 关闭Spring上下文
                if (applicationContext instanceof AnnotationConfigApplicationContext configContext) {
                    configContext.close();
                }
            } catch (Exception e) {
                log.error("关闭插件Spring上下文失败: {}", getWrapper().getPluginId(), e);
            }
        }
    }
    
    /**
     * 创建插件专用的Spring应用上下文
     * 子类应该重写此方法来配置自己的Spring上下文
     */
    protected ApplicationContext createApplicationContext() {
        // 默认创建一个空的Spring上下文
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.setClassLoader(getWrapper().getPluginClassLoader());
        context.refresh();
        return context;
    }
    
    /**
     * 获取插件的Spring上下文
     */
    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }
}
