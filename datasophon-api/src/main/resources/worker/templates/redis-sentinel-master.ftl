bind 0.0.0.0
daemonize yes
protected-mode no
port ${redisSentinelMasterPort} 
logfile "/opt/datasophon/redissentinel/logs/redis-master.log"
pidfile /opt/datasophon/redissentinel/var/redis-master.pid
dir /opt/datasophon/redissentinel/var/data
dbfilename dump-master.rdb
appendonly yes
appendfilename "appendonly-master.aof"

<#list itemList as item>
${item.name} ${item.value}
</#list>