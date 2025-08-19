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
package com.datasophon.worker.log;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.sift.AbstractDiscriminator;
import com.datasophon.worker.utils.TaskConstants;
import lombok.Getter;
import lombok.Setter;
/**
 * Task Log Discriminator
 */
@Setter
public class TaskLogDiscriminator extends AbstractDiscriminator<ILoggingEvent> {

    /**
     * key
     */
    private String key;

    /**
     * log base
     */
    @Getter
    private String logBase;

    /**
     * Logger名称格式: TaskLogLogger-{clusterId}-{serviceName}-{serviceRoleName}
     */
    @Override
    public String getDiscriminatingValue(ILoggingEvent event) {
        var loggerName = event.getLoggerName();
        var prefix = TaskConstants.TASK_LOG_LOGGER_NAME + "-";
        
        return loggerName.startsWith(prefix) 
            ? loggerName.substring(prefix.length()).replace("-", "/")
            : "unknown_task";
    }

    @Override
    public void start() {
        started = true;
    }

    @Override
    public String getKey() {
        return key;
    }

}
