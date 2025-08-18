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

package com.datasophon.common.command;

import java.io.Serializable;

/**
 * 流式日志命令
 * 支持启动/停止远程主机的实时日志推送
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-18
 */
public class LogStreamCommand implements Serializable {

    /**
     * 操作类型
     */
    public enum Action {
        START,  // 启动日志流
        STOP,   // 停止日志流
        GET     // 获取当前日志（一次性）
    }

    private Action action;
    private String logFile;
    private String decompressPackageName;
    private String sessionKey; // 用于标识会话
    private boolean fromEnd = true; // 是否从文件末尾开始

    public LogStreamCommand() {}

    public LogStreamCommand(Action action, String logFile, String sessionKey) {
        this.action = action;
        this.logFile = logFile;
        this.sessionKey = sessionKey;
    }

    // Getters and Setters
    public Action getAction() { return action; }
    public void setAction(Action action) { this.action = action; }
    
    public String getLogFile() { return logFile; }
    public void setLogFile(String logFile) { this.logFile = logFile; }
    
    public String getDecompressPackageName() { return decompressPackageName; }
    public void setDecompressPackageName(String decompressPackageName) { this.decompressPackageName = decompressPackageName; }
    
    public String getSessionKey() { return sessionKey; }
    public void setSessionKey(String sessionKey) { this.sessionKey = sessionKey; }
    
    public boolean isFromEnd() { return fromEnd; }
    public void setFromEnd(boolean fromEnd) { this.fromEnd = fromEnd; }

    @Override
    public String toString() {
        return "LogStreamCommand{" +
                "action=" + action +
                ", logFile='" + logFile + '\'' +
                ", sessionKey='" + sessionKey + '\'' +
                ", fromEnd=" + fromEnd +
                '}';
    }
}
