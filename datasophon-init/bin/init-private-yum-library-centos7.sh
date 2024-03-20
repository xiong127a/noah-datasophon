#!/bin/bash

ip="$1"

# 检查是否以root用户身份运行
if [ $UID -ne 0 ]; then
  echo Non root user. Please run as root.
  exit 1
fi

# 获取脚本所在目录
if [ -L $0 ]; then
    BASE_DIR=$(dirname $(readlink $0))
else
    BASE_DIR=$(dirname $0)
fi

BASE_PATH=$(cd ${BASE_DIR}; pwd)
INIT_PATH=$(dirname "${BASE_PATH}")
echo "INIT_PATH: ${INIT_PATH}"
INIT_BIN_PATH=${INIT_PATH}/bin
echo "INIT_BIN_PATH: ${INIT_BIN_PATH}"
INIT_SBIN_PATH=${INIT_PATH}/sbin
echo "INIT_SBIN_PATH: ${INIT_SBIN_PATH}"
PACKAGES_PATH=${INIT_PATH}/packages
echo "PACKAGES_PATH: ${PACKAGES_PATH}"

echo "Depending on the performance of your machine, it may take between 15-20 minutes to initialize the private YUM source. Please do not log out."

# 检查是否存在/data/private-yum-library目录，如果不存在则输出提示信息。
if [ ! -d "/data/private-yum-library" ]; then
  echo "没有发现private-yum-library 离线yum源，请确认是否正确配置离线yum源......."
fi

# 创建/etc/yum.repos.d/dataSophon.repo文件，并写入特定的yum源配置信息。
cat >/etc/yum.repos.d/dataSophon.repo <<EOF
[dataSophon-base]
name=dataSophon-base
baseurl=file:///data/private-yum-library/repo/centos/7/x86_64/BaseOS/
gpgcheck=0
enable=1

[dataSophon-updates]
name=dataSophon-updates
baseurl=file:///data/private-yum-library/repo/centos/7/x86_64/Updates/
gpgcheck=0
enabled=1

# 如果需要，可以添加其他repo源，例如epel源
#[dataSophon-epel]
#name=dataSophon-epel
#baseurl=file:///data/private-yum-library/epel/7/x86_64/
#enabled=1
#gpgcheck=0
EOF

yum clean all
yum makecache

echo "init-private-yum-library-centos7.sh finished."
echo "Done."

# 检查并修改httpd配置文件中的监听端口为8000，并重启httpd服务。
cat /etc/httpd/conf/httpd.conf | grep 'Listen 8000'
if [ $? -eq 0 ]; then
  echo "httpd port modified successfully" >>${initLogDir}/installSingle_$(date +%Y%m%d).log
  systemctl stop httpd
  systemctl start httpd
else
  echo "init httpd begin."
  yum install httpd
  cat /etc/httpd/conf/httpd.conf | grep 'Listen 8000'
  sed -i 's/Listen 80/Listen 8000/g' /etc/httpd/conf/httpd.conf
  sed -i '/ServerName yum.dataSophon.cn:8000/d' /etc/httpd/conf/httpd.conf
  sed -i '/#ServerName/a ServerName yum.dataSophon.cn:8000' /etc/httpd/conf/httpd.conf
  systemctl start httpd
  systemctl enable httpd
  echo "init httpd finished."
fi

# 设置软链接/var/www/html/centos指向私有yum源的目录。
unlink /var/www/html/centos
ln -s /data/private-yum-library/repo/centos/ /var/www/html/

# 在/etc/hosts文件中删除以"#modify yum mapping hosts start"开头、以"#modify yum mapping hosts end"结尾的行，并定义一个函数modifyYumHosts用于修改hosts文件。
sed -i '/#modify yum mapping hosts start/,/#modify yum mapping hosts end/d' /etc/hosts
modifyYumHosts() {
  echo "#modify yum mapping hosts start" >>/etc/hosts
  echo "${ip} yum.dataSophon.cn" >>/etc/hosts
  echo "#modify yum mapping hosts end" >>/etc/hosts
  source /etc/profile
  source /root/.bash_profile
}

modifyYumHosts

rm /etc/yum.repos.d/dataSophon.repo

# 删除旧的/etc/yum.repos.d/dataSophon.repo文件，并重新创建并写入新的yum源配置信息。
cat >/etc/yum.repos.d/dataSophon.repo <<EOF
[dataSophon-base]
name=dataSophon-base
baseurl=http://yum.dataSophon.cn:8000/centos/7/x86_64/BaseOS/
gpgcheck=0
enable=1

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

echo "init-private-yum-library-centos7.sh finished."
echo "Done."
