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

package com.datasophon.common.spring;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Spring应用上下文工具类
 * 为PF4J插件提供获取Spring bean的能力
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-28
 */
@Slf4j
@Component
public class SpringContextUtils implements ApplicationContextAware {
    
    private static ApplicationContext applicationContext;
    
    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        SpringContextUtils.applicationContext = context;
        log.info("Spring应用上下文已设置，插件可以开始获取bean");
    }
    
    /**
     * 获取Spring应用上下文
     */
    public static ApplicationContext getApplicationContext() {
        if (applicationContext == null) {
            throw new IllegalStateException("Spring应用上下文尚未初始化");
        }
        return applicationContext;
    }
    
    /**
     * 根据bean名称获取bean
     */
    public static Object getBean(String beanName) {
        try {
            return getApplicationContext().getBean(beanName);
        } catch (Exception e) {
            log.error("获取bean失败: {}", beanName, e);
            return null;
        }
    }
    
    /**
     * 根据bean类型获取bean
     */
    public static <T> T getBean(Class<T> clazz) {
        try {
            return getApplicationContext().getBean(clazz);
        } catch (Exception e) {
            log.error("获取bean失败: {}", clazz.getName(), e);
            return null;
        }
    }
    
    /**
     * 根据bean名称和类型获取bean
     */
    public static <T> T getBean(String beanName, Class<T> clazz) {
        try {
            return getApplicationContext().getBean(beanName, clazz);
        } catch (Exception e) {
            log.error("获取bean失败: {} {}", beanName, clazz.getName(), e);
            return null;
        }
    }
    
    /**
     * 检查是否存在指定的bean
     */
    public static boolean containsBean(String beanName) {
        try {
            return getApplicationContext().containsBean(beanName);
        } catch (Exception e) {
            log.error("检查bean是否存在失败: {}", beanName, e);
            return false;
        }
    }
    
    /**
     * 检查Spring上下文是否已初始化
     */
    public static boolean isInitialized() {
        return applicationContext != null;
    }
}
