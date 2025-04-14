listen_addresses = '${listenAddresses}'
max_wal_senders = 10
wal_level = replica
wal_log_hints = on
# 归档参数设置
archive_mode = ${archiveMode}
archive_command = '${archiveCommand}'
wal_sender_timeout = 60s
# 当变成备库时，是否可以接受查询请求
hot_standby = ${hotStandby}
port = 5432

max_connections = 100
shared_buffers = 128MB
dynamic_shared_memory_type = posix
max_wal_size = ${maxWalSize}
min_wal_size = ${minWalSize}
log_timezone = 'Asia/Shanghai'
datestyle = 'iso, mdy'
timezone = 'Asia/Shanghai'
lc_messages = 'en_US.UTF-8'
lc_monetary = 'en_US.UTF-8'
lc_numeric = 'en_US.UTF-8'
lc_time = 'en_US.UTF-8'
default_text_search_config = 'pg_catalog.english'

<#list itemList as item>
${item.name} = ${item.value}
</#list>