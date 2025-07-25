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

import com.mybatisflex.core.FlexGlobalConfig;
import com.mybatisflex.core.keygen.IKeyGenerator;
import com.mybatisflex.core.keygen.KeyGeneratorFactory;
import com.mybatisflex.core.keygen.impl.SnowFlakeIDKeyGenerator;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;




/**
 * MyBatisFlex配置类
 */
@Configuration
public class MybatisFlexConfig {

    @Bean
    public FlexGlobalConfig flexGlobalConfig() {
        FlexGlobalConfig config = new FlexGlobalConfig();
        // 设置打印banner为false
        config.setPrintBanner(false);
        return config;
    }

    @PostConstruct
    public void init() {
        // 注册雪花算法ID生成器
        KeyGeneratorFactory.register("snowflakeId", new SnowFlakeIDKeyGenerator());
    }
}