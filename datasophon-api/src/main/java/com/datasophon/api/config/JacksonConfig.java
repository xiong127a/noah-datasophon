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

package com.datasophon.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * Jackson 序列化配置
 * 
 * 主要解决以下问题：
 * 1. Long类型大整数在前端精度丢失问题
 * 2. 雪花算法生成的ID超出JavaScript Number.MAX_SAFE_INTEGER范围
 * 3. 统一将Long类型序列化为字符串，确保前后端数据一致性
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-14
 */
@Configuration
public class JacksonConfig {

    /**
     * 配置Jackson ObjectMapper
     * 将Long和long类型统一序列化为字符串，避免前端精度丢失
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean(ObjectMapper.class)
    public ObjectMapper jacksonObjectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper objectMapper = builder.createXmlMapper(false).build();
        
        // 创建自定义模块处理Long类型序列化
        SimpleModule longModule = new SimpleModule("LongToStringModule");
        
        // 将Long类型序列化为字符串
        longModule.addSerializer(Long.class, ToStringSerializer.instance);
        longModule.addSerializer(Long.TYPE, ToStringSerializer.instance);
        
        // 注册模块
        objectMapper.registerModule(longModule);
        
        return objectMapper;
    }
}
