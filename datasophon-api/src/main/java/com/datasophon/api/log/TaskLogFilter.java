/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.datasophon.api.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import com.datasophon.common.Constants;
import lombok.extern.slf4j.Slf4j;

/**
 * task log filter
 */
@Slf4j
public class TaskLogFilter extends Filter<ILoggingEvent> {



    /**
     * level
     */
    private Level level;

    public void setLevel(String level) {
        this.level = Level.toLevel(level);
    }

    /**
     * Accept or reject based on thread name
     *
     * @param event event
     * @return FilterReply
     */
    @Override
    public FilterReply decide(ILoggingEvent event) {
        FilterReply filterReply = FilterReply.DENY;

        // 添加空值检查，防止访问null对象的属性
        if (event == null) {
            return filterReply;
        }

        // 检查日志名称是否以指定前缀开头
        boolean isTaskLogger = event.getLoggerName() != null &&
                event.getLoggerName().startsWith(Constants.TASK_LOG_LOGGER_NAME);

        // 检查日志级别是否大于等于配置的级别
        boolean isLevelAllowed = level != null &&
                event.getLevel() != null &&
                event.getLevel().isGreaterOrEqual(level);

        if (isTaskLogger || isLevelAllowed) {
            filterReply = FilterReply.ACCEPT;
        }

        // 安全地记录调试信息
        if (log.isDebugEnabled()) {
            String threadName = event.getThreadName() != null ? event.getThreadName() : "unknown";
            String loggerName = event.getLoggerName() != null ? event.getLoggerName() : "unknown";
            String levelName = level != null ? level.toString() : "unknown";

            log.debug("task log filter, thread name:{}, loggerName:{}, filterReply:{}, level:{}",
                    threadName, loggerName, filterReply.name(), levelName);
        }

        return filterReply;
    }
}
