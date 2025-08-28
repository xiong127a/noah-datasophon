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

package com.datasophon.plugins.impl.cpu;

import lombok.Builder;
import lombok.Data;

/**
 * CPU指标数据模型
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-08-28
 */
@Data
@Builder
public class CpuMetrics {
    
    /**
     * CPU使用率 (百分比)
     */
    private double cpuUsage;
    
    /**
     * 负载平均值 [1分钟, 5分钟, 15分钟]
     */
    private double[] loadAverage;
    
    /**
     * CPU核心数
     */
    private int cpuCores;
    
    /**
     * CPU型号
     */
    private String cpuModel;
    
    /**
     * 获取1分钟负载平均值
     */
    public double getLoad1Min() {
        return loadAverage != null && loadAverage.length > 0 ? loadAverage[0] : 0.0;
    }
    
    /**
     * 获取5分钟负载平均值
     */
    public double getLoad5Min() {
        return loadAverage != null && loadAverage.length > 1 ? loadAverage[1] : 0.0;
    }
    
    /**
     * 获取15分钟负载平均值
     */
    public double getLoad15Min() {
        return loadAverage != null && loadAverage.length > 2 ? loadAverage[2] : 0.0;
    }
}