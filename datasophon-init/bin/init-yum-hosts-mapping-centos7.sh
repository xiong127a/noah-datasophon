#!/bin/bash

ip="$1"

if [ $UID -ne 0 ]; then
  echo Non root user. Please run as root.
  exit 1
fi

if [ -L $0 ]; then
    BASE_DIR=$(dirname $(readlink $0))
else
    BASE_DIR=$(dirname $0)
fi

BASE_PATH=$(cd ${BASE_DIR}; pwd)
INIT_PATH=$(dirname "${BASE_PATH}")
echo "INIT_PATH: ${INIT_PATH}"

# 备份/etc/yum.repos.d/目录下的所有.repo文件到/etc/yum.repos.d/backup目录
rm -rf /etc/yum.repos.d/backup
mkdir -p /etc/yum.repos.d/backup
mv `find /etc/yum.repos.d/ -name "*.repo"` /etc/yum.repos.d/backup

# 在/etc/hosts文件中删除以"#modify yum mapping hosts start"开头、以"#modify yum mapping hosts end"结尾的行。
sed -i '/#modify yum mapping hosts start/,/#modify yum mapping hosts end/d' /etc/hosts

modifyYumHosts(){
  echo "#modify yum mapping hosts start" >> /etc/hosts
  echo "${ip} yum.dataSophon.cn" >> /etc/hosts
  echo "#modify yum mapping hosts end" >> /etc/hosts
}

modifyYumHosts

# 创建/etc/yum.repos.d/dataSophon.repo文件，并写入特定的yum源配置信息。
cat > /etc/yum.repos.d/dataSophon.repo << EOF
[dataSophon-base]
name=dataSophon-base
baseurl=http://yum.dataSophon.cn:8000/centos/7/x86_64/BaseOS/
gpgcheck=0
enabled=1

[dataSophon-updates]
name=dataSophon-updates
baseurl=http://yum.dataSophon.cn:8000/centos/7/x86_64/Updates/
gpgcheck=0
enabled=1

# 如果需要，可以添加其他repo源，例如epel源
#[dataSophon-epel]
#name=dataSophon-epel
#baseurl=http://yum.dataSophon.cn:8000/epel/7/x86_64/
#enabled=1
#gpgcheck=0
EOF

yum clean all
yum makecache

echo "init-yum-hosts-mapping.sh finished."
echo "Done."
echo "yum repolist:"
echo "$(yum repolist)"
