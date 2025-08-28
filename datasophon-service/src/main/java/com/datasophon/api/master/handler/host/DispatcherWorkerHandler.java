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

package com.datasophon.api.master.handler.host;

import com.datasophon.common.model.HostInfo;



import java.net.UnknownHostException;

public interface DispatcherWorkerHandler {

    /**
     * 处理主机任务
     * 注意：SSH连接通过HostInfo中的连接信息和SSH插件适配器来管理
     * 
     * @param hostInfo 主机信息（包含SSH连接配置）
     * @return 处理是否成功，true表示继续执行下一个Handler，false表示中断处理链
     * @throws UnknownHostException 主机解析异常
     */
    boolean handle(HostInfo hostInfo) throws UnknownHostException;
}
