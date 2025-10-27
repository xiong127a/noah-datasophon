#!/bin/bash

# example: sh init-jdk.sh
# instal and config jdk env
if [ $UID -ne 0 ]; then
  echo Non root user. Please run as root.
  exit 1
fi
if [ -L $0 ]; then
  BASE_DIR=$(dirname $(readlink $0))
else
  BASE_DIR=$(dirname $0)
fi
BASE_PATH=$(
  cd ${BASE_DIR}
  pwd
)
INIT_PATH=$(dirname "${BASE_PATH}")
echo "INIT_PATH: ${INIT_PATH}"
INIT_BIN_PATH=${INIT_PATH}/bin
echo "INIT_BIN_PATH: ${INIT_BIN_PATH}"
INIT_SBIN_PATH=${INIT_PATH}/sbin
echo "INIT_SBIN_PATH: ${INIT_SBIN_PATH}"
PACKAGES_PATH=${INIT_PATH}/packages
echo "PACKAGES_PATH: ${PACKAGES_PATH}"
JDK_FOLDER_PATH=/usr/java
source /etc/profile
mkdir -p /usr/java
JDK_PATH_NAME="jdk1.8.0_333"
JDK_VERSION="1.8"
BASH_PROFILE_PATH="/root/.bash_profile"
BASHRC_PATH="/root/.bashrc"
ETC_PROFILE_PATH="/etc/profile"
JDK_TAR_NAME="jdk-8u333-linux-x64.tar.gz"

jdkAvailable=$(java -version 2>&1 | awk 'NR==1{gsub(/"/,"");print $3}')
result=$(echo $jdkAvailable | grep $JDK_VERSION)
if [[ "$result" != "" ]]; then
  echo "JDK installed.............................."
else
  echo "JDK not installed.............................."
  echo "JDK environment already sets"
  pid="sed -i '/export JAVA_HOME/d' /etc/profile"
  eval $pid
  pid="sed -i '/export CLASSPATH/d' /etc/profile"
  eval $pid
  pid="sed -i '/source \/etc\/profile/d' /root/.bash_profile"
  eval $pid
  pid="sed -i '/source \/etc\/profile/d' /root/.bashrc"
  eval $pid
  pid="sed -i '/source \/etc\/profile/d' /home/hadoop/.bash_profile"
  eval $pid
  pid="sed -i '/source \/etc\/profile/d' /home/hadoop/.bashrc"
  eval $pid
  echo "Prepare to Install JDK..."
  sleep 2s
  mkdir -p ${JDK_FOLDER_PATH}
  tar -zxvf ${PACKAGES_PATH}/${JDK_TAR_NAME} -C ${JDK_FOLDER_PATH}
  JAVA_HOME="${JDK_FOLDER_PATH}/${JDK_PATH_NAME}"
  JAVA_SOURCE_ENV="source /etc/profile"
  echo "export JAVA_HOME=$JAVA_HOME" >>/etc/profile
  echo "export PATH=\$PATH:\$JAVA_HOME/bin" >>/etc/profile
  echo ${JAVA_SOURCE_ENV} >>~/.bash_profile
  echo ${JAVA_SOURCE_ENV} >>~/.bashrc
  # JDK 21+ 不再需要配置 BCPROV，已内置现代加密算法支持
  echo "JDK 21+ includes modern crypto support, skipping BCPROV configuration."

  echo "If you need to effect the environment variable in the current session, do it manually: "
  source ${BASH_PROFILE_PATH}
  source ${BASHRC_PATH}
  source ${ETC_PROFILE_PATH}
  jdk2=$(grep -n "export JAVA_HOME=.*" /home/hadoop/.bash_profile | cut -f1 -d':')
  if [ -n "$jdk2" ]; then
    echo "JDK HADOOP environment exists"
  else
    echo ${JAVA_SOURCE_ENV} >>/home/hadoop/.bash_profile
    echo ${JAVA_SOURCE_ENV} >>/home/hadoop/.bashrc
    echo "JDK HADOOP environment sets successfully"
  fi
  echo "JDK install successfully"
  source /etc/profile
fi
echo "INIT-init-jdk.sh finished."
echo "Done."
