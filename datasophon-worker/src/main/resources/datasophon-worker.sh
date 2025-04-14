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

# 查找Java环境
# 1. 首先尝试使用 ../java 目录的JDK
RELATIVE_JAVA_HOME="$DDH_HOME/java"
if [ -d "$RELATIVE_JAVA_HOME" ]; then
  export JAVA_HOME=$RELATIVE_JAVA_HOME
  export PATH=$JAVA_HOME/bin:$PATH
  JAVA=$JAVA_HOME/bin/java
  echo "使用相对路径Java: $JAVA_HOME"
else
  # 2. 尝试使用 /usr/local/jdk1.8.0_333
  SYSTEM_JAVA="/usr/local/jdk1.8.0_333"
  if [ -d "$SYSTEM_JAVA" ]; then
    # 创建软链接到 ../java
    echo "创建软链接: $SYSTEM_JAVA -> $RELATIVE_JAVA_HOME"
    mkdir -p `dirname $RELATIVE_JAVA_HOME` 2>/dev/null
    ln -sf $SYSTEM_JAVA $RELATIVE_JAVA_HOME 2>/dev/null

    export JAVA_HOME=$SYSTEM_JAVA
    export PATH=$JAVA_HOME/bin:$PATH
    JAVA=$JAVA_HOME/bin/java
    echo "使用系统Java并创建软链接: $JAVA_HOME"
  else
    # 3. 尝试使用 JAVA_HOME 环境变量
    if [ -n "$JAVA_HOME" ] && [ -d "$JAVA_HOME" ]; then
      export PATH=$JAVA_HOME/bin:$PATH
      JAVA=$JAVA_HOME/bin/java
      echo "使用JAVA_HOME环境变量: $JAVA_HOME"
    else
      # 4. 尝试直接使用java命令
      JAVA=`which java 2>/dev/null`
      if [ -n "$JAVA" ]; then
        echo "使用系统PATH中的Java: $JAVA"
      else
        # 5. 如果都失败，报错退出
        echo "错误: 未找到可用的Java环境! 请安装JDK或设置JAVA_HOME环境变量。"
        exit 1
      fi
    fi
  fi
fi

# 测试Java是否可用
if ! "$JAVA" -version >/dev/null 2>&1; then
  echo "错误: Java命令无法执行! 请检查Java安装或权限。"
  exit 1
fi

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
  export DDH_OPTS="$HEAP_OPTS $DDH_OPTS $JAVA_OPTS"
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
