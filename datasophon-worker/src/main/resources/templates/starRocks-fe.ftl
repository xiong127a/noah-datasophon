# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

#####################################################################
## The uppercase properties are read and exported by bin/start_fe.sh.
## To see all Frontend configurations,
## see fe/src/com/starrocks/common/Config.java

# the output dir of stderr/stdout/gc
LOG_DIR = ${r"${STARROCKS_HOME}/log"}

DATE = "$(date +%Y%m%d-%H%M%S)"
JAVA_OPTS=${r"-Dlog4j2.formatMsgNoLookups=true -Xmx8192m -XX:+UseMembar -XX:SurvivorRatio=8 -XX:MaxTenuringThreshold=7 -XX:+PrintGCDateStamps -XX:+PrintGCDetails -XX:+UseConcMarkSweepGC -XX:+UseParNewGC -XX:+CMSClassUnloadingEnabled -XX:-CMSParallelRemarkEnabled -XX:CMSInitiatingOccupancyFraction=80 -XX:SoftRefLRUPolicyMSPerMB=0 -Xloggc:${LOG_DIR}/fe.gc.log.$DATE -XX:+PrintConcurrentLocks -Djava.security.policy=${STARROCKS_HOME}/conf/udf_security.policy"}

# For jdk 11+, this JAVA_OPTS will be used as default JVM options
JAVA_OPTS_FOR_JDK_11=${r"-Dlog4j2.formatMsgNoLookups=true -Xmx8192m -XX:+UseG1GC -Xlog:gc*:${LOG_DIR}/fe.gc.log.$DATE:time -Djava.security.policy=${STARROCKS_HOME}/conf/udf_security.policy"}

##
## the lowercase properties are read by main program.
##

# DEBUG, INFO, WARN, ERROR, FATAL
sys_log_level = INFO

# store metadata, create it if it is not exist.


# Enable jaeger tracing by setting jaeger_grpc_endpoint
# jaeger_grpc_endpoint = http://localhost:14250

# Choose one if there are more than one ip except loopback address.
# Note that there should at most one ip match this list.
# If no ip match this rule, will choose one randomly.
# use CIDR format, e.g. 10.10.10.0/24
# Default value is empty.
# priority_networks = 10.10.10.0/24;192.168.0.0/16
# 以下部分内容是自动读取服务配置
<#list itemList as item>
${item.name} = ${item.value}
</#list>