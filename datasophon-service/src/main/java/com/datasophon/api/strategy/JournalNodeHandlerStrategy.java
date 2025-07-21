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

package com.datasophon.api.strategy;

import com.datasophon.api.load.GlobalVariables;
import com.datasophon.api.utils.ProcessUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class JournalNodeHandlerStrategy implements ServiceRoleStrategy {
    private static final Logger logger = LoggerFactory.getLogger(JournalNodeHandlerStrategy.class);

    @Override
    public void handler(Integer clusterId, List<String> hosts) {
        Map<String, String> globalVariables = GlobalVariables.get(clusterId);
        if (hosts.size() >= 3) {
            // 为保持向后兼容，仍单独设置前三个节点变量
            ProcessUtils.generateClusterVariable(globalVariables, clusterId, "${journalNode1}", hosts.get(0));
            ProcessUtils.generateClusterVariable(globalVariables, clusterId, "${journalNode2}", hosts.get(1));
            ProcessUtils.generateClusterVariable(globalVariables, clusterId, "${journalNode3}", hosts.get(2));

            // 生成包含所有JournalNode的URL
            StringBuilder journalNodesUrl = new StringBuilder("qjournal://");
            for (int i = 0; i < hosts.size(); i++) {
                if (i > 0)
                    journalNodesUrl.append(";");
                journalNodesUrl.append(hosts.get(i)).append(":8485");
            }
            journalNodesUrl.append("/meta");

            // 设置完整的JournalNode URL变量
            String journalUrl = journalNodesUrl.toString();
            ProcessUtils.generateClusterVariable(globalVariables, clusterId, "${journalNodesUrl}", journalUrl);
            logger.info("Generated dynamic JournalNode URL: {}", journalUrl);
        }
    }
}
