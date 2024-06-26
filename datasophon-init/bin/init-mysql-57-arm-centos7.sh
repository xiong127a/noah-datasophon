#!/bin/bash

num1="$1"
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

mariadb_rpm=$(rpm -qa | grep mariadb)
if [[ "$?" == "0" ]]; then
  echo "exist mariadb"
  rpm -qa | grep mariadb | xargs rpm -e --nodeps
fi
mysql_rpm=$(rpm -qa | grep mysql)
if [[ "$?" == "0" ]]; then
  echo "exist mysql"
  echo "开始卸载已存在的 mysql..............."
  systemctl stop mysql
  systemctl stop mysqld
  rpm -qa | grep -i -E mysql\|mariadb | xargs -n1 sudo rpm -e --nodeps
  rm -rf /var/lib/mysql
  rm -rf /usr/sbin/mysqld
  rm -rf /usr/local/mysql
  rm -rf /etc/my.cnf
  rm -rf /data/mysql/data
fi

# 检测指定端口是否有进程占用
if lsof -Pi :3306 -sTCP:LISTEN -t &> /dev/null; then
    echo "Port 3306 is in use. Killing the process..."
    # 获取占用指定端口的进程 PID，并杀死该进程
    sudo kill $(lsof -t -Pi :3306 -sTCP:LISTEN)
    echo "Process killed."
else
    echo "Port 3306 is not in use."
fi

rpm -qa | grep zlib-devel
if [ "$?" == "0" ]; then
  echo "zlib-devel exists"
else
  yum -y install zlib-devel
  rpm -qa | grep zlib-devel
  if [ "$?" == "0" ]; then
    echo "zlib-devel install successfully"
  fi
fi

rpm -qa | grep bzip2-devel
if [ "$?" == "0" ]; then
  echo "bzip2-devel exists"
else
  yum -y install bzip2-devel
  rpm -qa | grep bzip2-devel
  if [ "$?" == "0" ]; then
    echo "bzip2-devel install successfully"
  fi
fi

rpm -qa | grep openssl-devel
if [ "$?" == "0" ]; then
  echo "openssl-devel exists"
else
  yum -y install openssl-devel
  rpm -qa | grep openssl-devel
  if [ "$?" == "0" ]; then
    echo "openssl-devel install successfully"
  fi
fi

rpm -qa | grep ncurses-devel
if [ "$?" == "0" ]; then
  echo "ncurses-devel exists"
else
  yum -y install ncurses-devel
  rpm -qa | grep ncurses-devel
  if [ "$?" == "0" ]; then
    echo "ncurses-devel install successfully"
  fi
fi

MYSQL_TAR_NAME=mysql-5.7.27-1.el7.aarch64.rpm

echo "mysql start install mysql-5.7.27-1.el7.aarch64........."
rpm -ivh ${PACKAGES_PATH}/mysql-5.7.27-1.el7.aarch64.rpm

mkdir -pv /data/mysql/data
chmod 777 /data/mysql/data
/usr/local/mysql/bin/mysqld --initialize --user=mysql --log-error=/data/mysql/data/mysqld.log

# 检查并删除现有的符号链接
if [ -L /usr/bin/mysql ]; then
    echo "Found existing symlink at /usr/bin/mysql. Deleting it."
    sudo rm -f /usr/bin/mysql
fi
# 创建新的符号链接
echo "Creating new symlink from /usr/local/mysql/bin/mysql to /usr/bin/mysql."
sudo ln -s /usr/local/mysql/bin/mysql /usr/bin/mysql

systemctl start mysql
systemctl enable mysql
sleep 2
echo "${num1}"

if [ $(systemctl status mysql | grep running | wc -l) -eq 1 ]; then
  echo "mysql在运行"
  tmp_passwd=$(grep 'temporary password' /data/mysql/data/mysqld.log | awk '{print $NF}')
  echo "临时密码：${tmp_passwd}"

  /usr/local/mysql/bin/mysqladmin -uroot -p''$tmp_passwd'' password ''$num1''
  mysql -uroot -p''$num1'' -e "update mysql.user set host='%' where user ='root';"
  mysql -uroot -p''$num1'' -e "FLUSH PRIVILEGES;"
  mysql -uroot -p''$num1'' -e "ALTER USER 'root'@'%' IDENTIFIED BY '$num1' PASSWORD EXPIRE NEVER;"
  echo "num1：'$num1'"
  mysql -uroot -p''$num1'' -e "ALTER USER 'root'@'%' IDENTIFIED WITH mysql_native_password BY '$num1';"
  mysql -uroot -p''$num1'' -e "FLUSH PRIVILEGES;"

hostname=$(hostname)
  cat >/etc/my.cnf <<EOF
[mysqld]

datadir=/data/mysql/data
socket=/tmp/mysql.sock
symbolic-links=0
log-error=/data/mysql/data/mysqld.log
pid-file=/data/mysql/data/${hostname}.pid
skip_ssl
server-id = 1
log-bin=mysql-bin
binlog_format=row
binlog-do-db=financial_lease
binlog-do-db=financial_lease_config

EOF

  systemctl restart mysql
  echo "install mysql-5.7.27-1.el7.aarch64  finished........."
else
  echo "####################################################################"
  echo "mysql install finished & but service startup failed & checkup /var/log/mysqld.log"
fi
