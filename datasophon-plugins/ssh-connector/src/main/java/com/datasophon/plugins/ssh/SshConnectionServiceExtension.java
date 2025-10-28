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

package com.datasophon.plugins.ssh;

import com.datasophon.plugins.api.model.HostCheckContext;
import com.datasophon.plugins.api.service.SshConnectionService;
import com.datasophon.plugins.impl.ssh.SshConnectionServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.pf4j.Extension;

import java.io.InputStream;

/**
 * SSH连接服务扩展点实现
 * 通过 PF4J 扩展点机制提供 SSH 连接服务
 * 
 * 设计原则（符合 PF4J 插件化架构）：
 * 1. 使用 @Extension 注解，让 PF4J 自动发现和加载
 * 2. 继承实现类 SshConnectionServiceImpl，复用所有功能
 * 3. 通过 PluginManager.getExtensions() 获取实例
 * 4. 无需 Spring Bean 注册，完全由 PF4J 管理
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-10-22
 */
@Slf4j
@Extension
public class SshConnectionServiceExtension extends SshConnectionServiceImpl implements SshConnectionService {
    
    public SshConnectionServiceExtension() {
        super();
        log.info("【SSH插件】SSH连接服务扩展点已加载 - 通过PF4J扩展点机制");
    }
    
    /**
     * 显式覆盖父类方法，确保字节码正确生成
     * 解决 AbstractMethodError 问题
     */
    @Override
    public boolean uploadFileFromStream(HostCheckContext context, InputStream inputStream, 
                                       String remoteFilePath, long totalBytes, UploadProgressCallback progressCallback) {
        return super.uploadFileFromStream(context, inputStream, remoteFilePath, totalBytes, progressCallback);
    }
}

