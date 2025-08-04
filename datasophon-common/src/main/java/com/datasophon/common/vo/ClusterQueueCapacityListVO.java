/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.datasophon.common.vo;

import java.io.Serializable;
import java.util.List;

/**
 * 集群队列容量列表视图对象
 * 用于前端展示队列树形结构
 *
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-04
 */
public record ClusterQueueCapacityListVO(
        String rootId,
        List<ClusterQueueCapacityVO> nodes,
        List<LinksVO> links) implements Serializable {

    /**
     * 队列链接关系视图对象
     */
    public record LinksVO(
            String from,
            String to) implements Serializable {
    }
}