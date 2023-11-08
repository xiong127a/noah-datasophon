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

package com.datasophon.api.load;

import com.datasophon.common.model.ServiceConfig;

import java.util.HashMap;
import java.util.List;

/**
 * 配置文件和对应的配置参数缓存
 */
public class ServiceConfigMap {

    // DDP-1.2.0_HDFS_config -> List<serviceConfig>
    /*
     * {
     *      "configWithHA": false,
     *      "configWithKerberos": false,
     *      "configWithRack": false,
     *      "configurableInWizard": true,
     *      "defaultValue": "hdfs://nameservice1/alluxio",
     *      "description": "挂载到Alluxio根目录的底层存储URI",
     *      "hidden": false,
     *      "label": "挂载到Alluxio根目录的底层存储URI",
     *      "name": "alluxio.master.mount.table.root.ufs",
     *      "required": true,
     *      "type": "input",
     *      "value": "hdfs://nameservice1/alluxio"
     * }
     */
    private static HashMap<String, List<ServiceConfig>> map = new HashMap<String, List<ServiceConfig>>();

    public static void put(String key, List<ServiceConfig> configs) {
        map.put(key, configs);
    }

    public static List<ServiceConfig> get(String key) {
        return map.get(key);
    }

    public static boolean exists(String key) {
        return map.containsKey(key);
    }
}
