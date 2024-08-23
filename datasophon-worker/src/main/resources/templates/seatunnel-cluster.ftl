#!/bin/bash
#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
JAVA_OPTS="-Xms3G -Xmx3G"
set -eu
# resolve links - $0 may be a softlink
PRG="$0"

while [ -h "$PRG" ] ; do
# shellcheck disable=SC2006
ls=`ls -ld "$PRG"`
# shellcheck disable=SC2006
link=`expr "$ls" : '.*-> \(.*\)$'`
if expr "$link" : '/.*' > /dev/null; then
PRG="$link"
else
# shellcheck disable=SC2006
PRG=`dirname "$PRG"`/"$link"
fi
done

PRG_DIR=`dirname "$PRG"`
APP_DIR=`cd "$PRG_DIR/.." >/dev/null; pwd`
CONF_DIR=${r"${APP_DIR}"}/config
APP_JAR=${r"${APP_DIR}"}/starter/seatunnel-starter.jar
APP_MAIN="org.apache.seatunnel.core.starter.seatunnel.SeaTunnelServer"
OUT="${r"${APP_DIR}"}/logs/seatunnel-server.out"
HELP=false

if [ -f "${r"${CONF_DIR}"}/seatunnel-env.sh" ]; then
. "${r"${CONF_DIR}"}/seatunnel-env.sh"
fi

if [ $# == 0 ]
then
args=""
else
args=$@
fi

set +u
# SeaTunnel Engine Config
if [ -z $HAZELCAST_CONFIG ]; then
HAZELCAST_CONFIG=${r"${CONF_DIR}"}/hazelcast.yaml
fi

if [ -z $SEATUNNEL_CONFIG ]; then
SEATUNNEL_CONFIG=${r"${CONF_DIR}"}/seatunnel.yaml
fi

if test ${r"${JvmOption}"} ;then
JAVA_OPTS="${r"${JAVA_OPTS}"} ${r"${JvmOption}"}"
fi

for i in "$@"
do
if [[ "${r"${i}"}" == *"JvmOption"* ]]; then
JVM_OPTION="${r"${i}"}"
JAVA_OPTS="${r"${JAVA_OPTS}"} ${r"${JVM_OPTION#*=}"}"
elif [[ "${r"${i}"}" == "-d" || "${r"${i}"}" == "--daemon" ]]; then
DAEMON=true
elif [[ "${r"${i}"}" == "-h" || "${r"${i}"}" == "--help" ]]; then
HELP=true
fi
done

JAVA_OPTS="${r"${JAVA_OPTS}"} -Dseatunnel.config=${r"${SEATUNNEL_CONFIG}"}"
JAVA_OPTS="${r"${JAVA_OPTS}"} -Dhazelcast.config=${r"${HAZELCAST_CONFIG}"}"

JAVA_OPTS="${r"${JAVA_OPTS}"} -Dscheduler_url=${schedulerUrl}"
JAVA_OPTS="${r"${JAVA_OPTS}"} -Dkafka_bootstrapserver=${kafkaBootstrapserver}"
JAVA_OPTS="${r"${JAVA_OPTS}"} -Ddi_url=http://${diUrl}"

# Log4j2 Config
JAVA_OPTS="${r"${JAVA_OPTS}"} -Dlog4j2.contextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector"
if [ -e "${r"${CONF_DIR}"}/log4j2.properties" ]; then
JAVA_OPTS="${r"${JAVA_OPTS}"} -Dlog4j2.configurationFile=${r"${CONF_DIR}"}/log4j2.properties"
JAVA_OPTS="${r"${JAVA_OPTS}"} -Dseatunnel.logs.path=${r"${APP_DIR}"}/logs"
JAVA_OPTS="${r"${JAVA_OPTS}"} -Dseatunnel.logs.file_name=seatunnel-engine-server"
fi

# Server Debug Config
# Usage instructions:
# If you need to debug your code in cluster mode, please enable this configuration option and listen to the specified
# port in your IDE. After that, you can happily debug your code.
# JAVA_OPTS="${r"${JAVA_OPTS}"} -Xdebug -Xrunjdwp:server=y,transport=dt_socket,address=5001,suspend=y"

CLASS_PATH=${r"${APP_DIR}"}/lib/*:${r"${APP_JAR}"}

while read line
do
if [[ ! $line == \#* ]] && [ -n "$line" ]; then
JAVA_OPTS="$JAVA_OPTS $line"
fi
done < ${r"${APP_DIR}"}/config/jvm_options

if [[ $DAEMON == true && $HELP == false ]]; then
if [[ ! -d ${r"${APP_DIR}"}/logs ]]; then
mkdir -p ${r"${APP_DIR}"}/logs
fi
touch $OUT
echo ${r"${JAVA_OPTS}"}
nohup java ${r"${JAVA_OPTS}"} -cp ${r"${CLASS_PATH}"} ${r"${APP_MAIN}"} ${r"${args}"} > "$OUT" 200<&- 2>&1 < /dev/null &
else
java ${r"${JAVA_OPTS}"} -cp ${r"${CLASS_PATH}"} ${r"${APP_MAIN}"} ${r"${args}"}
fi

