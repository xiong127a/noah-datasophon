# HDFS 命令行示例
# 主机: ${data.getBasicInfoValue('host', 'localhost')}
<#if data.getSecurityInfoValue('kerberos.enabled', 'false') == 'true'>
# 已启用Kerberos认证
<#else>
# 未启用Kerberos认证
</#if>

TIP> 以下是常用的HDFS命令示例，您可以根据实际情况修改路径和参数

<#if data.getSecurityInfoValue('kerberos.enabled', 'false') == 'true'>
PRT> [root@${data.hostName} ~]# 
CMD> klist
RES> Ticket cache: FILE:/tmp/krb5cc_0
     Default principal: hdfs/${data.getBasicInfoValue('host', 'localhost')}@${data.getSecurityInfoValue('realm', 'HADOOP.COM')}
     
     Valid starting       Expires              Service principal
     05/15/2023 08:00:00  05/16/2023 08:00:00  krbtgt/${data.getSecurityInfoValue('realm', 'HADOOP.COM')}@${data.getSecurityInfoValue('realm', 'HADOOP.COM')}

PRT> [root@${data.hostName} ~]# 
CMD> kinit -kt ${data.getSecurityInfoValue('keytab.path', '/etc/security/keytabs/hdfs.keytab')} ${data.getSecurityInfoValue('principal', 'hdfs@HADOOP.COM')}
RES> # 无输出表示成功

</#if>

PRT> [root@${data.hostName} ~]# 
CMD> ssh root@${data.getBasicInfoValue('host', 'localhost')}
RES> Last login: Wed May 15 10:00:00 2023 from 192.168.1.100
     [root@${data.getBasicInfoValue('host', 'localhost')} ~]#

<#if data.getConnectInfoValue('hdfsUri', '') != ''>
PRT> [root@${data.hostName} ~]# 
CMD> export HDFS_URI="${data.getConnectInfoValue('hdfsUri', '')}"
RES> [root@${data.getBasicInfoValue('host', 'localhost')} ~]#
<#else>
PRT> [root@${data.hostName} ~]# 
CMD> export HDFS_URI="hdfs://${data.getBasicInfoValue('host', 'localhost')}:${data.getBasicInfoValue('port', '8020')}"
RES> [root@${data.getBasicInfoValue('host', 'localhost')} ~]#
</#if>

PRT> [root@${data.hostName} ~]# 
CMD> ${data.serviceHome}/bin/hdfs dfsadmin -report
RES> Configured Capacity: 200 GB
     Present Capacity: 180 GB
     DFS Remaining: 160 GB
     DFS Used: 20 GB
     DFS Used%: 10%
     Under replicated blocks: 0
     Blocks with corrupt replicas: 0
     Missing blocks: 0
     Missing blocks (with replication factor 1): 0
     ...
     Live datanodes (3):
     ...

PRT> [root@${data.hostName} ~]# 
CMD> ${data.serviceHome}/bin/hdfs dfs -ls /
RES> Found 4 items
     drwxr-xr-x   - hdfs supergroup          0 2023-05-15 10:20 /apps
     drwxr-xr-x   - hdfs supergroup          0 2023-05-15 10:21 /hbase
     drwxrwxrwx   - hdfs supergroup          0 2023-05-15 10:22 /tmp
     drwxr-xr-x   - hdfs supergroup          0 2023-05-15 10:23 /user

PRT> [root@${data.hostName} ~]# 
CMD> ${data.serviceHome}/bin/hdfs dfs -mkdir -p /user/example
RES> [root@${data.getBasicInfoValue('host', 'localhost')} ~]#

PRT> [root@${data.hostName} ~]# 
CMD> echo "Hello, HDFS!" > /tmp/test.txt
RES> [root@${data.getBasicInfoValue('host', 'localhost')} ~]#

PRT> [root@${data.hostName} ~]# 
CMD> ${data.serviceHome}/bin/hdfs dfs -put /tmp/test.txt /user/example/
RES> [root@${data.getBasicInfoValue('host', 'localhost')} ~]#

PRT> [root@${data.hostName} ~]# 
CMD> ${data.serviceHome}/bin/hdfs dfs -cat /user/example/test.txt
RES> Hello, HDFS!

PRT> [root@${data.hostName} ~]# 
CMD> ${data.serviceHome}/bin/hdfs dfs -ls /user/example
RES> Found 1 items
     -rw-r--r--   3 root supergroup         13 2023-05-15 10:33 /user/example/test.txt

PRT> [root@${data.hostName} ~]# 
CMD> ${data.serviceHome}/bin/hdfs dfs -chmod 755 /user/example/test.txt
RES> [root@${data.getBasicInfoValue('host', 'localhost')} ~]#

PRT> [root@${data.hostName} ~]# 
CMD> ${data.serviceHome}/bin/hdfs dfs -chown hdfs:hadoop /user/example/test.txt
RES> [root@${data.getBasicInfoValue('host', 'localhost')} ~]#

PRT> [root@${data.hostName} ~]# 
CMD> ${data.serviceHome}/bin/hdfs dfs -ls /user/example
RES> Found 1 items
     -rwxr-xr-x   3 hdfs hadoop         13 2023-05-15 10:33 /user/example/test.txt

PRT> [root@${data.hostName} ~]# 
CMD> ${data.serviceHome}/bin/hdfs dfs -get /user/example/test.txt /tmp/test_from_hdfs.txt
RES> [root@${data.getBasicInfoValue('host', 'localhost')} ~]#

PRT> [root@${data.hostName} ~]# 
CMD> ${data.serviceHome}/bin/hdfs dfs -stat /user/example/test.txt
RES> 2023-05-15 10:33:45
     13
     3

PRT> [root@${data.hostName} ~]# 
CMD> ${data.serviceHome}/bin/hdfs dfs -checksum /user/example/test.txt
RES> /user/example/test.txt MD5-of-0MD5-of-512CRC32C 000002000000000000000000b9ade8ad22cc1f47b11bc9a5e89672f64

<#if data.getBasicInfoValue('highAvailability', 'false') == 'true'>
PRT> [root@${data.hostName} ~]# 
CMD> ${data.serviceHome}/bin/hdfs haadmin -getServiceState nn1
RES> active

PRT> [root@${data.hostName} ~]# 
CMD> ${data.serviceHome}/bin/hdfs haadmin -getServiceState nn2
RES> standby

</#if>

PRT> [root@${data.hostName} ~]# 
CMD> ${data.serviceHome}/bin/hdfs dfs -df -h /
RES> Filesystem               Size     Used  Available  Use%
     hdfs://${data.getBasicInfoValue('host', 'localhost')}:${data.getBasicInfoValue('port', '8020')}  200 GB  20 GB    180 GB   10%

<#if data.getConnectInfoValue('webhdfsUri', '') != ''>
PRT> [root@${data.hostName} ~]# 
CMD> curl -i "${data.getConnectInfoValue('webhdfsUri', '')}/user/example/test.txt?op=OPEN"
RES> HTTP/1.1 307 TEMPORARY_REDIRECT
     Location: http://datanode1:9864/webhdfs/v1/user/example/test.txt?op=OPEN&namenoderpcaddress=${data.getBasicInfoValue('host', 'localhost')}:${data.getBasicInfoValue('port', '8020')}&offset=0
     ...
</#if>

PRT> [root@${data.hostName} ~]# 
CMD> ${data.serviceHome}/bin/hdfs fsck /
RES> Connecting to namenode via http://${data.getBasicInfoValue('host', 'localhost')}:9870/fsck?ugi=hdfs&path=%2F
     FSCK started by hdfs from /172.18.0.2 for path / at Wed May 15 10:35:31 UTC 2023
     Status: HEALTHY
      Total size:	12345678 B
      Total dirs:	123
      Total files:	456
      Total symlinks:	0
      Total blocks (validated):	789 (avg. block size 15646 B)
      Minimally replicated blocks:	789 (100.0 %)
      Over-replicated blocks:	0 (0.0 %)
      Under-replicated blocks:	0 (0.0 %)
      Mis-replicated blocks:	0 (0.0 %)
      Default replication factor:	3
      Average block replication:	3.0
      Corrupt blocks:	0
      Missing replicas:	0 (0.0 %)
      Number of data-nodes:	3
      Number of racks:	1
     FSCK ended at Wed May 15 10:35:35 UTC 2023 in 4 milliseconds
     
     The filesystem under path '/' is HEALTHY

PRT> [root@${data.hostName} ~]# 
CMD> ${data.serviceHome}/bin/hdfs dfs -rm /user/example/test.txt
RES> Deleted /user/example/test.txt

PRT> [root@${data.hostName} ~]# 
CMD> ${data.serviceHome}/bin/hdfs dfs -rmdir /user/example
RES> Deleted /user/example

<---> 