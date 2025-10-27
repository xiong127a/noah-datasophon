#!/bin/bash
#
#  Licensed to the Apache Software Foundation (ASF) under one or more
#  contributor license agreements.  See the NOTICE file distributed with
#  this work for additional information regarding copyright ownership.
#  The ASF licenses this file to You under the Apache License, Version 2.0
#  (the "License"); you may not use this file except in compliance with
#  the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#

# 设置HotSeconds路径，默认为空
HOT_SECONDS_PATH=""
HOT_SECONDS_CONF_PATH=""
# 设置JRebel路径，默认为空
JREBEL_HOME=""

usage="Usage: datasophon-worker.sh (start|stop|restart|log) <command> "

# if no args specified, show usage
if [ $# -le 1 ]; then
  echo $usage
  exit 1
fi

# 系统资源限制设置
# 设置文件描述符上限
ulimit -n 65536 > /dev/null 2>&1
# 设置用户进程数上限
ulimit -u 65536 > /dev/null 2>&1
# 关闭core文件生成
ulimit -c 0 > /dev/null 2>&1

startStop=$1
if [ $# -gt 0 ]; then shift; fi

command=$1
if [ $# -gt 0 ]; then shift; fi

JAVA_DEBUG_OPTS=""
if [ "$1" = "debug" ]; then
    JAVA_DEBUG_OPTS=" -Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,address=8001,server=y,suspend=n "
    if [ $# -gt 0 ]; then shift; fi
fi

echo "Begin $startStop $command......"
source /etc/profile

SCRIPT="$0"
# SCRIPT may be an arbitrarily deep series of symlinks. Loop until we have the concrete path.
while [ -h "$SCRIPT" ] ; do
  ls=`ls -ld "$SCRIPT"`
  # Drop everything prior to ->
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    SCRIPT="$link"
  else
    SCRIPT=`dirname "$SCRIPT"`/"$link"
  fi
done

# 获取脚本的绝对路径
BIN_DIR=`dirname "$SCRIPT"`
BIN_DIR=`cd "$BIN_DIR"; pwd`
export DDH_HOME=$BIN_DIR/..
echo "脚本所在目录: $BIN_DIR"
echo "DDH_HOME: $DDH_HOME"

# 查找Java环境 - 只使用JAVA_HOME环境变量
if [ -z "$JAVA_HOME" ]; then
  echo "错误: JAVA_HOME环境变量未设置! 请先配置JAVA_HOME。"
  echo "提示: 执行 'source /etc/profile' 加载环境变量"
  exit 1
fi

if [ ! -d "$JAVA_HOME" ]; then
  echo "错误: JAVA_HOME目录不存在: $JAVA_HOME"
  exit 1
fi

JAVA="$JAVA_HOME/bin/java"
if [ ! -x "$JAVA" ]; then
  echo "错误: Java可执行文件不存在或无执行权限: $JAVA"
  exit 1
fi

echo "使用JAVA_HOME: $JAVA_HOME"
export PATH=$JAVA_HOME/bin:$PATH

export HOSTNAME=`hostname`

export DDH_PID_DIR=$DDH_HOME/pid
export DDH_LOG_DIR=$DDH_HOME/logs
export DDH_CONF_DIR=$DDH_HOME/conf
export DDH_LIB_JARS=$DDH_HOME/lib/*

export DDH_OPTS="-server -Xms512m -Xmx512m -Dddh.home=$DDH_HOME"
export STOP_TIMEOUT=5

if [ ! -d "$DDH_LOG_DIR" ]; then
  mkdir -p $DDH_LOG_DIR
fi

log=$DDH_LOG_DIR/$command-$HOSTNAME.out
pid=$DDH_PID_DIR/$command.pid

cd $DDH_HOME

if [ "$command" = "worker" ]; then
  LOG_FILE="-Dlogging.config=classpath:logback.xml -Dspring.profiles.active=worker"
  JMX="-javaagent:$DDH_HOME/jmx/jmx_prometheus_javaagent-0.16.1.jar=8585:$DDH_HOME/jmx/jmx_exporter_config.yaml"
  CLASS=com.datasophon.worker.WorkerApplicationServer
  
  # 添加HotSeconds相关参数（如果路径已设置）
  HOT_SECONDS_OPTS=""
  if [ -n "$HOT_SECONDS_PATH" ] && [ -n "$HOT_SECONDS_CONF_PATH" ]; then
    HOT_SECONDS_OPTS="-XXaltjvm=dcevm -javaagent:$HOT_SECONDS_PATH/HotSecondsServer.jar=hotconf=$HOT_SECONDS_CONF_PATH/hot-seconds-remote.xml"
  fi
  
  # 添加JRebel相关参数（如果路径已设置）
  JREBEL_OPTS=""
  if [ -n "$JREBEL_HOME" ]; then
    JREBEL_OPTS="-agentpath:$JREBEL_HOME/lib/libjrebel64.so -Drebel.remoting_plugin=true  -Drebel.remoting_port=1099 "
  fi
  
  export DDH_OPTS="$HEAP_OPTS $DDH_OPTS $JAVA_OPTS $HOT_SECONDS_OPTS $JREBEL_OPTS"
else
  echo "Error: No command named \`$command' was found."
  exit 1
fi

case $startStop in
  (start)
    [ -w "$DDH_PID_DIR" ] ||  mkdir -p "$DDH_PID_DIR"

    if [ -f $pid ]; then
      if kill -0 `cat $pid` > /dev/null 2>&1; then
        echo $command running as process `cat $pid`.  Stop it first.
        exit 1
      fi
    fi

    echo starting $command, logging to $log

    exec_command="$DDH_OPTS $LOG_FILE $JMX $JAVA_DEBUG_OPTS -classpath $DDH_CONF_DIR:$DDH_LIB_JARS $CLASS"

    echo "nohup $JAVA $exec_command > $log 2>&1 &"
    nohup $JAVA $exec_command > $log 2>&1 &
    echo $! > $pid
    ;;

  (stop)
      if [ -f $pid ]; then
        TARGET_PID=`cat $pid`
        if kill -0 $TARGET_PID > /dev/null 2>&1; then
          echo stopping $command
          kill $TARGET_PID
          sleep $STOP_TIMEOUT
          if kill -0 $TARGET_PID > /dev/null 2>&1; then
            echo "$command did not stop gracefully after $STOP_TIMEOUT seconds: killing with kill -9"
            kill -9 $TARGET_PID
          fi
        else
          echo no $command to stop
        fi
        rm -f $pid
      else
        echo no $command to stop
      fi
      ;;
  (status)
      if [ -f $pid ]; then
        TARGET_PID=`cat $pid`
        if kill -0 $TARGET_PID > /dev/null 2>&1; then
          echo $command is running
        else
          echo $command is stop
        fi
      else
        echo $command not found
      fi
      ;;
  (restart)
      if [ -f $pid ]; then
        TARGET_PID=`cat $pid`
        if kill -0 $TARGET_PID > /dev/null 2>&1; then
          echo stopping $command
          kill $TARGET_PID
          sleep $STOP_TIMEOUT
          if kill -0 $TARGET_PID > /dev/null 2>&1; then
            echo "$command did not stop gracefully after $STOP_TIMEOUT seconds: killing with kill -9"
            kill -9 $TARGET_PID
          fi
        else
          echo no $command to stop
        fi
        rm -f $pid
      else
        echo no $command to stop
      fi

      # 等待2秒
      sleep 2s

      # 再启动
      [ -w "$DDH_PID_DIR" ] ||  mkdir -p "$DDH_PID_DIR"
      if [ -f $pid ]; then
          if kill -0 `cat $pid` > /dev/null 2>&1; then
            echo $command running as process `cat $pid`.  Stop it first.
            exit 1
          fi
      fi
      echo starting $command, logging to $log

      exec_command="$DDH_OPTS $LOG_FILE $JMX $JAVA_DEBUG_OPTS -classpath $DDH_CONF_DIR:$DDH_LIB_JARS $CLASS"

      echo "nohup $JAVA $exec_command > $log 2>&1 &"
      nohup $JAVA $exec_command > $log 2>&1 &
      echo $! > $pid
      ;;
  (log)
      if [ -f $log ]; then
        # 实时查看最后100行日志
        tail -n 100 -f $log
      else
        echo "日志文件不存在: $log"
      fi
      ;;
  (*)
    echo $usage
    exit 1
    ;;

esac

echo "End $startStop $command."
