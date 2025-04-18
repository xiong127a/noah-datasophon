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

package com.datasophon.common.model.k8s;

import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Pod信息实体类
 */
@Data
public class PodInfo {
    /**
     * Pod名称
     */
    private String name;

    /**
     * 命名空间
     */
    private String namespace;

    /**
     * Pod状态
     */
    private String status;

    /**
     * Pod IP
     */
    private String ip;

    /**
     * Pod所在节点
     */
    private String nodeName;

    /**
     * Pod标签
     */
    private Map<String, String> labels;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 容器列表
     */
    private List<ContainerInfo> containers;

    /**
     * 容器信息类
     */
    @Data
    public static class ContainerInfo {
        /**
         * 容器名称
         */
        private String name;

        /**
         * 容器镜像
         */
        private String image;

        /**
         * 容器状态
         */
        private String status;

        /**
         * 容器就绪状态
         */
        private boolean ready;

        /**
         * 容器端口映射
         */
        private List<PortMapping> ports;

        /**
         * 重启次数
         */
        private int restartCount;
    }

    /**
     * 端口映射信息
     */
    @Data
    public static class PortMapping {
        /**
         * 容器端口
         */
        private int containerPort;

        /**
         * 主机端口
         */
        private Integer hostPort;

        /**
         * 协议
         */
        private String protocol;
    }
}