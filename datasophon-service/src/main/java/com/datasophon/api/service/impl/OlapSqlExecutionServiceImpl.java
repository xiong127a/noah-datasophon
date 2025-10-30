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
import com.datasophon.api.service.OlapSqlExecutionService;
import com.datasophon.common.command.OlapSqlExecCommand;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.utils.OlapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * OLAP SQL执行服务实现
 * 替代MasterNodeProcessingActor，处理OLAP节点的SQL操作（添加BE/FE/CN节点等）
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-10-30
 */
@Service
public class OlapSqlExecutionServiceImpl implements OlapSqlExecutionService {

    private static final Logger logger = LoggerFactory.getLogger(OlapSqlExecutionServiceImpl.class);

    // 最大重试次数
    private static final int MAX_RETRY_TIMES = 3;
    // 重试间隔（秒）
    private static final int RETRY_INTERVAL_SECONDS = 10;

    @Override
    @Async("taskExecutor")
    public void executeOlapSqlCommand(OlapSqlExecCommand command) {
        try {
            logger.info("OlapSqlExecutionService 接收到命令: {}", JSONUtil.toJsonStr(command));

            String operationDesc = command.getOpsType().getDesc();
            String hostname = command.getHostName();
            String feMaster = command.getFeMaster();

            // 第一次尝试执行
            ExecResult execResult = executeOperation(command);

            if (execResult.getExecResult()) {
                logger.info("{} {} 添加成功", hostname, operationDesc);
                return;
            }

            logger.warn("{} {} 首次添加失败，准备重试", hostname, operationDesc);

            // 失败后重试
            int tryTimes = 0;
            while (!execResult.getExecResult() && tryTimes < MAX_RETRY_TIMES) {
                try {
                    // 等待一段时间后重试
                    TimeUnit.SECONDS.sleep(RETRY_INTERVAL_SECONDS);

                    logger.info("{} {} 第{}次重试", hostname, operationDesc, tryTimes + 1);

                    // 使用SQL客户端重试
                    execResult = executeOperationBySqlClient(command);

                    if (execResult.getExecResult()) {
                        logger.info("{} {} 重试成功（第{}次）", hostname, operationDesc, tryTimes + 1);
                        break;
                    } else {
                        logger.warn("{} {} 第{}次重试失败", hostname, operationDesc, tryTimes + 1);
                    }

                    tryTimes++;
                } catch (InterruptedException e) {
                    logger.warn("OLAP操作重试被中断", e);
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            if (!execResult.getExecResult()) {
                logger.error("{} {} 添加失败，已尝试 {} 次", hostname, operationDesc, tryTimes + 1);
            }

        } catch (Throwable e) {
            logger.error("执行OLAP SQL命令时发生错误", e);
        }
    }

    /**
     * 执行OLAP操作（第一次尝试）
     */
    private ExecResult executeOperation(OlapSqlExecCommand command) {
        try {
            return switch (command.getOpsType()) {
                case ADD_BE -> OlapUtils.addBackend(command.getFeMaster(), command.getHostName());
                case ADD_FE_FOLLOWER -> OlapUtils.addFollower(command.getFeMaster(), command.getHostName());
                case ADD_FE_OBSERVER -> OlapUtils.addObserver(command.getFeMaster(), command.getHostName());
                case ADD_CN -> OlapUtils.addCn(command.getFeMaster(), command.getHostName());
            };
        } catch (Exception e) {
            logger.error("执行OLAP操作失败", e);
            ExecResult result = new ExecResult();
            result.setExecResult(false);
            result.setExecOut("执行失败: " + e.getMessage());
            return result;
        }
    }

    /**
     * 通过SQL客户端执行OLAP操作（重试时使用）
     */
    private ExecResult executeOperationBySqlClient(OlapSqlExecCommand command) {
        try {
            return switch (command.getOpsType()) {
                case ADD_BE -> OlapUtils.addBackendBySqlClient(command.getFeMaster(), command.getHostName());
                case ADD_FE_FOLLOWER -> OlapUtils.addFollowerBySqlClient(command.getFeMaster(), command.getHostName());
                case ADD_FE_OBSERVER -> OlapUtils.addObserverBySqlClient(command.getFeMaster(), command.getHostName());
                case ADD_CN -> OlapUtils.addCnBySqlClient(command.getFeMaster(), command.getHostName());
            };
        } catch (Exception e) {
            logger.error("通过SQL客户端执行OLAP操作失败", e);
            ExecResult result = new ExecResult();
            result.setExecResult(false);
            result.setExecOut("SQL客户端执行失败: " + e.getMessage());
            return result;
        }
    }
}

