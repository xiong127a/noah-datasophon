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

package com.datasophon.api.configuration;

import com.datasophon.common.utils.PropertyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

/**
 * Spring环境配置提供者
 * 将Spring环境变量注册为PropertyUtils的配置源
 */
@Configuration
public class SpringEnvironmentConfigProvider implements PropertyUtils.ConfigProvider {

    private static final Logger logger = LoggerFactory.getLogger(SpringEnvironmentConfigProvider.class);

    private final Environment environment;

    public SpringEnvironmentConfigProvider(Environment environment) {
        this.environment = environment;
        logger.info("SpringEnvironmentConfigProvider created with environment: {}", environment);
    }

    @PostConstruct
    public void init() {
        // 将自身注册为PropertyUtils的配置提供者
        PropertyUtils.registerConfigProvider(this);
        logger.info("SpringEnvironmentConfigProvider registered to PropertyUtils");
    }

    @Override
    public String getProperty(String key) {
        if (key == null) {
            return null;
        }
        try {
            return environment.getProperty(key);
        } catch (Exception e) {
            logger.warn("Failed to get property from Spring Environment: {}", key, e);
            return null;
        }
    }

    @Override
    public Map<String, String> getPrefixedProperties(String prefix) {
        Map<String, String> matchedProperties = new HashMap<>();

        try {
            if (environment instanceof ConfigurableEnvironment configurableEnvironment) {

                configurableEnvironment.getPropertySources().forEach(propertySource -> {
                    if (propertySource.getSource() instanceof Map) {
                        Map<String, Object> source = (Map<String, Object>) propertySource.getSource();
                        source.forEach((key, value) -> {
                            if (key.startsWith(prefix)) {
                                matchedProperties.put(key, String.valueOf(value));
                            }
                        });
                    }
                });
            }
        } catch (Exception e) {
            logger.warn("Failed to get prefixed properties from Spring Environment: {}", prefix, e);
        }

        return matchedProperties;
    }
}