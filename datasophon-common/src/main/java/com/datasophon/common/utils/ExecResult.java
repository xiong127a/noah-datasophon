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

package com.datasophon.common.utils;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Accessors(chain = true)
@Data
public class ExecResult implements Serializable {

    private boolean execResult = false;
    private String execOut;
    private String execErrOut;
    private Object object;

    // 新增静态工厂方法
    public static ExecResult error(String errorMessage) {
        ExecResult result = new ExecResult();
        result.setExecResult(false);
        result.setExecErrOut(errorMessage);
        return result;
    }

    // 可选：添加带错误输出和详细输出的重载方法
    public static ExecResult error(String errorMessage, String detailOutput) {
        ExecResult result = error(errorMessage);
        result.setExecOut(detailOutput);
        return result;
    }

    // 可选：添加成功状态的快捷方法
    public static ExecResult success() {
        ExecResult result = new ExecResult();
        result.setExecResult(true);
        return result;
    }

    public boolean getExecResult() {
        return execResult;
    }
}
