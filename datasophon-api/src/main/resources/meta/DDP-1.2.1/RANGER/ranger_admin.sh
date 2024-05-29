#!/bin/bash

usage="Usage: start.sh (start|stop|restart) <command> "

# if no args specified, show usage
startStop=$1

start(){
	echo "ranger admin start"
	ranger-admin start
	if [ $? -eq 0 ]
    then
		echo "ranger admin start success"
	else
		echo "ranger admin start failed"
		exit 1
	fi
}
stop(){
	echo "ranger admin stop"
	ranger-admin stop
	if [ $? -eq 0 ]
    then
		echo "ranger admin stop success"
	else
		echo "ranger admin stop failed"
		exit 1
	fi
}
status(){
  echo "ranger admin status"
  pid=$(ps -ef | grep java | grep -- '-Dproc_rangeradmin' | grep -v grep | awk '{ print $2 }')
  echo "pid is: $pid"
  if [ -n "$pid" ]; then
    kill -0 $pid
    if [ $? -eq 0 ]; then
      echo "ranger admin is running"
    else
      echo "ranger admin is not running"
      exit 1
    fi
  else
    echo "ranger admin is not running"
    exit 1
  fi
}
restart(){
	echo "ranger admin restart"
	ranger-admin restart
	if [ $? -eq 0 ]
    then
		echo "ranger admin restart success"
	else
		echo "ranger admin restart failed"
		exit 1
	fi
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
    echo $usage
    exit 1
    ;;
esac


echo "End $startStop ranger"