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

package com.datasophon.api.master.handler.service;

import com.datasophon.api.utils.ClusterInfoUtils;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.command.GenerateServiceConfigCommand;
import com.datasophon.common.model.ServiceRoleInfo;
import com.datasophon.common.utils.ExecResult;

import java.util.Objects;

public class ServiceConfigureAsyncHandler extends ServiceHandler {


    private OnComplete<Object> function;


  @Override
  public ExecResult handlerRequest(ServiceRoleInfo serviceRoleInfo)  {
    ExecResult execResult = new ExecResult();
    execResult.setExecResult(true);
    // config
    GenerateServiceConfigCommand generateServiceConfigCommand = new GenerateServiceConfigCommand();
    generateServiceConfigCommand.setServiceName(serviceRoleInfo.getParentName());
    generateServiceConfigCommand.setClusterId(serviceRoleInfo.getClusterId()); // 设置集群ID
    String namespace = ClusterInfoUtils.getKubernetesNamespace(serviceRoleInfo.getClusterId());
    generateServiceConfigCommand.setNamespace(namespace);
    generateServiceConfigCommand.setCofigFileMap(serviceRoleInfo.getConfigFileMap());
    generateServiceConfigCommand.setDecompressPackageName(serviceRoleInfo.getDecompressPackageName());
    generateServiceConfigCommand.setRunAs(serviceRoleInfo.getRunAs());
    if ("zkserver".equalsIgnoreCase(serviceRoleInfo.getName())) {
      generateServiceConfigCommand.setMyid((Integer) CacheUtils.get("zkserver_" + serviceRoleInfo.getHostname()));
    }
    generateServiceConfigCommand.setServiceRoleName(serviceRoleInfo.getName());
    // 使用HTTP方式提交任务到Worker（改为同步调用，简化异步逻辑）
    ExecResult configResult = WorkerTaskHelper.submitAndWait(
            serviceRoleInfo.getHostname(), generateServiceConfigCommand, 180);
    
    if (Objects.nonNull(configResult) && configResult.getExecResult() && Objects.nonNull(getNext())) {
        return getNext().handlerRequest(serviceRoleInfo);
    }
    
    // 如果有回调函数，执行它
    if (function != null) {
        try {
            function.onComplete(null, configResult);
        } catch (Throwable e) {
            // 忽略回调异常
        }
    }
    
    return configResult;
  }
}
