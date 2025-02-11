#!/bin/bash
usage="Usage: start.sh (start|stop|restart) <command> "

# if no args specified, show usage
if [ $# -le 1 ]; then
  echo "$usage"
  exit 1
fi
startStop=$1
shift
command=$1

SH_DIR=$(dirname "$0")
ident="$SH_DIR/ident.id"
export LOG_DIR="$SH_DIR/logs"
export PID_DIR="$SH_DIR/pid"
pid="$PID_DIR/hadoop-hdfs-$command.pid"

if [[ "$command" = "namenode"  ||  "$command" = "datanode" || "$command" = "secondarynamenode" ||  "$command" = "journalnode" || "$command" = "zkfc" || "$command" = "httpfs" ]]; then
   cmd="$SH_DIR/bin/hdfs"
elif [[ "$command" = "resourcemanager" || "$command" = "nodemanager" || "$command" = "timelineserver" ]]; then
   cmd="$SH_DIR/bin/yarn"
   pid="$PID_DIR/hadoop-yarn-$command.pid"
elif [[ "$command" = "historyserver" ]]; then
   cmd="$SH_DIR/bin/mapred"
   pid="$PID_DIR/hadoop-mapred-$command.pid"
else
  echo "Error: No command named '$command' was found."
  exit 1
fi

start(){
	echo "Executing $cmd --daemon start $command"
	"$cmd" --daemon start "$command"
	if [ $? -eq 0 ]; then
		echo "$command start success"
		if [ "$command" = "namenode" ]; then
		  echo "true" > "$ident"
		fi
	else
		echo "$command start failed"
		exit 1
	fi
}

stop(){
	"$cmd" --daemon stop "$command"
	if [ $? -eq 0 ]; then
		echo "$command stop success"
		# 删除对应的pid文件
		if [ -f "$pid" ]; then
			rm -f "$pid"
			echo "Removed pid file: $pid"
		fi
	else
		echo "$command stop failed"
		exit 1
	fi
}

status(){
	if [ -f "$pid" ]; then
		TARGET_PID=$(cat "$pid")
		if kill -0 "$TARGET_PID" >/dev/null 2>&1; then
			echo "$command is running"
		else
			echo "$command is not running but pid file exists"
			exit 1
		fi
	else
		echo "$command is not running"
		exit 1
	fi
}

restart(){
	stop
	sleep 10
	start
}

case $startStop in
  (start)
    start
    ;;
  (stop)
    stop
    ;;
  (status)
    status
    ;;
  (restart)
    restart
    ;;
  (*)
    echo "$usage"
    exit 1
    ;;
esac

echo "End $startStop $command."
