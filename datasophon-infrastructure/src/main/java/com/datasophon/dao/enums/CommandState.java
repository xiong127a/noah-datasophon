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

package com.datasophon.dao.enums;

import com.mybatisflex.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.Setter;

@Getter
public enum CommandState {

    // 命令状态 1：正在运行2：成功3：失败
    WAIT(0, "待运行"),
    RUNNING(1, "正在运行"),
    SUCCESS(2, "成功"),
    FAILED(3, "失败"),
    CANCEL(4, "取消");

    @Getter
    @EnumValue
    private final int value;

    private final String desc;

    CommandState(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    @JsonValue
    public String getDesc() {
        return desc;
    }

    @Override
    public String toString() {
        return this.desc;
    }
}
