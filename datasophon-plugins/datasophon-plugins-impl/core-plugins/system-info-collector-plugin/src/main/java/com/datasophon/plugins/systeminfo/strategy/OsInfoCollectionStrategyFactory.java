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

package com.datasophon.plugins.systeminfo.strategy;

import com.datasophon.common.enums.OsType;
import com.datasophon.plugins.systeminfo.strategy.impl.CentOsInfoCollectionStrategy;
import com.datasophon.plugins.systeminfo.strategy.impl.KylinInfoCollectionStrategy;
import com.datasophon.plugins.systeminfo.strategy.impl.UbuntuInfoCollectionStrategy;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 操作系统信息收集策略工厂
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-28
 */
@Component
public class OsInfoCollectionStrategyFactory {
    
    private final Map<OsType, OsInfoCollectionStrategy> strategies = new ConcurrentHashMap<>();
    
    public OsInfoCollectionStrategyFactory() {
        // 初始化策略
        strategies.put(OsType.CENTOS, new CentOsInfoCollectionStrategy());
        strategies.put(OsType.UBUNTU, new UbuntuInfoCollectionStrategy());
        strategies.put(OsType.KYLIN, new KylinInfoCollectionStrategy());
    }
    
    /**
     * 获取对应操作系统的信息收集策略
     */
    public OsInfoCollectionStrategy getStrategy(OsType osType) {
        return strategies.get(osType);
    }
    
    /**
     * 检查是否支持指定操作系统
     */
    public boolean isSupported(OsType osType) {
        return strategies.containsKey(osType);
    }
}
