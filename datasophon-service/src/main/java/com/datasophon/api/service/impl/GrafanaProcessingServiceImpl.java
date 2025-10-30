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

package com.datasophon.api.service.impl;

import cn.hutool.json.JSONUtil;
import com.datasophon.api.service.GrafanaProcessingService;
import com.datasophon.common.command.Sqlite3ExecCommand;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.utils.Sqlite3Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Grafana处理服务实现
 * 替代GrafanaProcessingActor，负责处理Grafana SQLite数据库的操作（更新datasource等）
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-10-30
 */
@Service
public class GrafanaProcessingServiceImpl implements GrafanaProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(GrafanaProcessingServiceImpl.class);

    // 默认的Grafana数据库文件路径
    private static final String DEFAULT_GRAFANA_DB_PATH = "/opt/datasophon/grafana/data/grafana.db";
    
    // 最大重试次数
    private static final int MAX_RETRY_TIMES = 3;
    
    // 重试间隔（秒）
    private static final int RETRY_INTERVAL_SECONDS = 10;

    @Override
    @Async("taskExecutor")
    public void processSqlite3Command(Sqlite3ExecCommand command) {
        try {
            logger.info("GrafanaProcessingService 接收到命令: {}", JSONUtil.toJsonStr(command));

            String grafanaIp = command.getGrafanaIp();
            String url = command.getUrl();
            // 使用默认路径，如需自定义可扩展Sqlite3ExecCommand类
            String dbFilePath = DEFAULT_GRAFANA_DB_PATH;

            // 第一次尝试更新
            ExecResult execResult = Sqlite3Utils.updateDatasource(dbFilePath, url);

            if (execResult.getExecResult()) {
                logger.info("Grafana {} datasource 更新成功", grafanaIp);
                return;
            }

            logger.warn("Grafana {} datasource 首次更新失败，准备重试", grafanaIp);

            // 失败后重试
            int tryTimes = 0;
            while (!execResult.getExecResult() && tryTimes < MAX_RETRY_TIMES) {
                try {
                    // 等待一段时间后重试
                    TimeUnit.SECONDS.sleep(RETRY_INTERVAL_SECONDS);

                    logger.info("Grafana {} datasource 第{}次重试", grafanaIp, tryTimes + 1);

                    execResult = Sqlite3Utils.updateDatasource(dbFilePath, url);

                    if (execResult.getExecResult()) {
                        logger.info("Grafana {} datasource 重试成功（第{}次）", grafanaIp, tryTimes + 1);
                        break;
                    } else {
                        logger.warn("Grafana {} datasource 第{}次重试失败", grafanaIp, tryTimes + 1);
                    }

                    tryTimes++;
                } catch (InterruptedException e) {
                    logger.warn("Grafana操作重试被中断", e);
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            if (!execResult.getExecResult()) {
                logger.error("Grafana {} datasource 更新失败，已尝试 {} 次", grafanaIp, tryTimes + 1);
            }

        } catch (Throwable e) {
            logger.error("处理Grafana SQLite命令时发生错误", e);
        }
    }
}

