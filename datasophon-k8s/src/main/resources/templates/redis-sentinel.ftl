bind 0.0.0.0
daemonize yes
protected-mode no
port ${sentinelServerPort}
logfile "/opt/datasophon/redissentinel/logs/redis-sentinel.log"
pidfile /opt/datasophon/redissentinel/var/redis-sentinel.pid
sentinel resolve-hostnames yes
sentinel monitor mymaster ${redisSentinelMasterHost} ${redisSentinelMasterPort} 2
sentinel down-after-milliseconds mymaster 3000
sentinel parallel-syncs mymaster 1
sentinel failover-timeout mymaster 120000


<#list itemList as item>
${item.name} ${item.value}
</#list>