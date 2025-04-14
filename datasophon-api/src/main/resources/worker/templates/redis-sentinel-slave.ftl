bind 0.0.0.0
daemonize yes
protected-mode no
port ${redisSentinelMasterPort}
logfile "/opt/datasophon/redissentinel/logs/redis-slave.log"
pidfile /opt/datasophon/redissentinel/var/redis-slave.pid
dir /opt/datasophon/redissentinel/var/data
dbfilename dump-slave.rdb
appendonly yes
appendfilename "appendonly-slave.aof"
replicaof ${redisSentinelMasterHost} ${redisSentinelMasterPort}


<#list itemList as item>
${item.name} ${item.value}
</#list>