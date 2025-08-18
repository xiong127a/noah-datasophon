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

package com.datasophon.worker.actor;

import org.apache.pekko.actor.AbstractActor;
import org.apache.pekko.japi.pf.ReceiveBuilder;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.datasophon.common.Constants;
import com.datasophon.common.command.GetLogCommand;
import com.datasophon.common.command.LogStreamCommand;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.utils.FileUtils;
import com.datasophon.common.utils.PlaceholderUtils;
import com.datasophon.common.utils.PropertyUtils;
import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LogActor extends AbstractActor {

    private static final Logger logger = LoggerFactory.getLogger(LogActor.class);
    
    // 流式日志跟踪管理
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final Map<String, Tailer> activeTailers = new ConcurrentHashMap<>();

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                // 原有的一次性日志获取（保持兼容）
                .match(GetLogCommand.class, this::handleGetLogCommand)
                // 新的流式日志命令
                .match(LogStreamCommand.class, this::handleLogStreamCommand)
                .matchAny(this::unhandled)
                .build();
    }
    
    /**
     * 处理原有的GetLogCommand（保持兼容性）
     */
    private void handleGetLogCommand(GetLogCommand command) {
        logger.info("处理一次性日志获取命令");
        String logContent = getLogContent(command.getLogFile(), command.getDecompressPackageName());
        
        ExecResult execResult = new ExecResult();
        execResult.setExecResult(true);
        execResult.setExecOut(logContent);
        getSender().tell(execResult, getSelf());
    }
    
    /**
     * 处理新的LogStreamCommand（流式日志）
     */
    private void handleLogStreamCommand(LogStreamCommand command) {
        logger.info("处理流式日志命令: {}", command);
        
        switch (command.getAction()) {
            case START -> startLogStream(command);
            case STOP -> stopLogStream(command);
            case GET -> {
                // 一次性获取
                String logContent = getLogContent(command.getLogFile(), command.getDecompressPackageName());
                ExecResult result = new ExecResult();
                result.setExecResult(true);
                result.setExecOut(logContent);
                getSender().tell(result, getSelf());
            }
        }
    }
    
    /**
     * 启动流式日志跟踪
     */
    private void startLogStream(LogStreamCommand command) {
        String fullPath = getFullLogPath(command.getLogFile(), command.getDecompressPackageName());
        if (fullPath == null) {
            getSender().tell(new ExecResult(false, "日志文件不存在"), getSelf());
            return;
        }
        
        // 停止已有的跟踪
        stopLogStream(command);
        
        File logFile = new File(fullPath);
        var requester = getSender(); // 保存请求者引用
        
        // 创建Tailer监听器
        var listener = new TailerListener() {
            @Override
            public void init(Tailer tailer) {
                logger.debug("开始远程日志流跟踪: {}", fullPath);
                // 发送启动确认
                requester.tell(new ExecResult(true, "LOG_STREAM_STARTED"), getSelf());
            }
            
            @Override
            public void handle(String line) {
                // 实时推送新的日志行
                ExecResult result = new ExecResult();
                result.setExecResult(true);
                result.setExecOut(line + "\n");
                result.setStreamingLog(true); // 标记为流式日志
                requester.tell(result, getSelf());
            }
            
            @Override
            public void fileNotFound() {
                logger.warn("远程日志文件未找到: {}", fullPath);
            }
            
            @Override
            public void fileRotated() {
                logger.info("远程日志文件轮转: {}", fullPath);
            }
            
            @Override
            public void handle(Exception ex) {
                logger.error("远程日志流异常", ex);
                requester.tell(new ExecResult(false, "日志流异常: " + ex.getMessage()), getSelf());
            }
        };
        
        // 创建Tailer
        var tailer = Tailer.builder()
                .setFile(logFile)
                .setTailerListener(listener)
                .setDelayDuration(Duration.ofMillis(500))
                .setStartThread(false)
                .get();
        
        activeTailers.put(command.getSessionKey(), tailer);
        
        // 使用虚拟线程启动
        executor.submit(tailer::run);
        
        logger.info("启动远程日志流: sessionKey={}, file={}", command.getSessionKey(), fullPath);
    }
    
    /**
     * 停止流式日志跟踪
     */
    private void stopLogStream(LogStreamCommand command) {
        var tailer = activeTailers.remove(command.getSessionKey());
        if (tailer != null) {
            tailer.close();
            logger.info("停止远程日志流: sessionKey={}", command.getSessionKey());
            getSender().tell(new ExecResult(true, "LOG_STREAM_STOPPED"), getSelf());
        }
    }
    
    /**
     * 获取日志内容（复用原有逻辑）
     */
    private String getLogContent(String logFile, String decompressPackageName) {
        try {
            HashMap<String, String> paramMap = new HashMap<>();
            String hostName = InetAddress.getLocalHost().getHostName();
            paramMap.put("${user}", "root");
            paramMap.put("${hostname}", hostName);
            String logFileName = PlaceholderUtils.replacePlaceholders(logFile, paramMap, Constants.REGEX_VARIABLE);

            String logStr = "can not find log file";
            if (logFileName.startsWith(StrUtil.SLASH) && FileUtil.exist(logFileName)) {
                logStr = FileUtils.readLastRows(logFileName, Charset.defaultCharset(), PropertyUtils.getInt("rows"));
            } else if (FileUtil.exist(Constants.INSTALL_PATH + Constants.SLASH + decompressPackageName + Constants.SLASH + logFileName)) {
                logStr = FileUtils.readLastRows(
                    Constants.INSTALL_PATH + Constants.SLASH + decompressPackageName + Constants.SLASH + logFileName,
                    Charset.defaultCharset(), PropertyUtils.getInt("rows"));
            }
            return logStr;
        } catch (Exception e) {
            logger.error("获取日志内容失败", e);
            return "获取日志失败: " + e.getMessage();
        }
    }
    
    /**
     * 获取完整的日志文件路径
     */
    private String getFullLogPath(String logFile, String decompressPackageName) {
        try {
            HashMap<String, String> paramMap = new HashMap<>();
            String hostName = InetAddress.getLocalHost().getHostName();
            paramMap.put("${user}", "root");
            paramMap.put("${hostname}", hostName);
            String logFileName = PlaceholderUtils.replacePlaceholders(logFile, paramMap, Constants.REGEX_VARIABLE);

            if (logFileName.startsWith(StrUtil.SLASH) && FileUtil.exist(logFileName)) {
                return logFileName;
            } else if (FileUtil.exist(Constants.INSTALL_PATH + Constants.SLASH + decompressPackageName + Constants.SLASH + logFileName)) {
                return Constants.INSTALL_PATH + Constants.SLASH + decompressPackageName + Constants.SLASH + logFileName;
            }
            return null;
        } catch (Exception e) {
            logger.error("获取日志文件路径失败", e);
            return null;
        }
    }
    
    @Override
    public void postStop() throws Exception {
        // Actor停止时清理所有Tailer
        activeTailers.values().forEach(Tailer::close);
        activeTailers.clear();
        executor.shutdown();
        super.postStop();
    }
}
