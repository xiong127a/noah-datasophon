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

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Flyway配置类，自动检测数据库类型并加载对应的SQL脚本
 */
@Slf4j
@Configuration
public class FlywayConfiguration {

    @Bean
    public FlywayConfigurationCustomizer flywayConfigurationCustomizer(DataSource dataSource) {
        return configuration -> {
            String databaseType = detectDatabaseType(dataSource);
            log.info("检测到数据库类型: {}", databaseType);

            // 构建多个位置
            List<String> locations = buildLocations(databaseType);
            log.info("Flyway将从以下位置加载迁移脚本: {}", locations);

            // 设置locations
            configuration.locations(locations.toArray(new String[0]));
        };
    }

    /**
     * 检测数据库类型
     */
    private String detectDatabaseType(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            String productName = connection.getMetaData().getDatabaseProductName().toLowerCase();

            if (productName.contains("mysql") || productName.contains("mariadb")) {
                return "mysql";
            } else if (productName.contains("postgresql")) {
                return "postgresql";
            } else if (productName.contains("oracle")) {
                return "oracle";
            } else if (productName.contains("db2")) {
                return "db2";
            } else if (productName.contains("sqlserver") || productName.contains("microsoft")) {
                return "sqlserver";
            } else if (productName.contains("dm")) {
                return "dm"; // 达梦数据库
            } else if (productName.contains("h2")) {
                return "h2";
            } else {
                log.warn("未识别的数据库类型: {}，将使用默认MySQL脚本", productName);
                return "mysql"; // 默认使用MySQL脚本
            }
        } catch (SQLException e) {
            log.error("检测数据库类型时出错: {}", e.getMessage(), e);
            return "mysql"; // 发生错误时默认使用MySQL脚本
        }
    }

    /**
     * 构建Flyway脚本位置列表
     */
    private List<String> buildLocations(String databaseType) {
        List<String> locations = new ArrayList<>();

        // 添加特定数据库类型的脚本位置
        // 例如: classpath:db/migration/mysql
        String specificLocation = "classpath:db/migration/" + databaseType;
        locations.add(specificLocation);

        // 如果有通用脚本也可以添加
        // locations.add("classpath:db/migration/common");

        return locations;
    }
}