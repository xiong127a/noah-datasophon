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

package com.datasophon.api.service;

import java.util.TreeSet;

/**
 * HDFS EC (Erasure Coding) 服务接口
 * 替代HdfsECActor，处理HDFS纠删码相关操作
 * 
 * @author DataSophon Team
 */
public interface HdfsEcService {
    
    /**
     * 处理HDFS EC命令
     * 用于管理HDFS扩容和缩容时的纠删码配置
     * 
     * @param serviceInstanceId 服务实例ID
     * @param hosts 主机列表
     * @param type 操作类型（如 "whitelist"）
     * @param roleName 角色名称（如 "NameNode"）
     */
    void handleHdfsEcCommand(Long serviceInstanceId, TreeSet<String> hosts, String type, String roleName);
}
