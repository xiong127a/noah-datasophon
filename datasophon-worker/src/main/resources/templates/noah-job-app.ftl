#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

spring:
  application:
    name: standalone-server
  jackson:
    time-zone: GMT+8
    date-format: "yyyy-MM-dd HH:mm:ss"
  banner:
    charset: UTF-8
  cache:
    # default enable cache, you can disable by `type: none`
    type: none
    cache-names:
      - tenant
      - user
      - processDefinition
      - processTaskRelation
      - taskDefinition
    caffeine:
      spec: maximumSize=100,expireAfterWrite=300s,recordStats
  # sql:
  #   init:
  #     schema-locations: classpath:sql/dolphinscheduler_h2.sql
  datasource:
    # driver-class-name: org.h2.Driver
    # url: jdbc:h2:mem:dolphinscheduler;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=true
    # username: sa
    # password: ""
    #driver-class-name: com.mysql.cj.jdbc.Driver
    #url: jdbc:mysql://192.168.6.45:3306/noah_scheduler?useUnicode=true&characterEncoding=UTF-8
    #username: ke_dev
    #password: ke_dev
    #driver-class-name: com.gbase.jdbc.Driver
    #url: jdbc:gbase://192.168.1.134:5258/noah
    #username: noah
    #password: Noah@123
    #driver-class-name: com.mysql.cj.jdbc.Driver
    #url: jdbc:mysql://192.168.5.163:3306/scheduler?useUnicode=true&characterEncoding=UTF-8&useSSL=true&allowMultiQueries=true&&serverTimezone=GMT%2b8
    #username: fuyong
    #password: 1234qwer
    # driver-class-name: dm.jdbc.driver.DmDriver
    # url: jdbc:dm://192.168.6.227:5236/DOLPHINSCHEDULER?useUnicode=true&characterEncoding=UTF-8
    # username: DOLPHINSCHEDULER
    # password: Jd2019@123
    url: jdbc:oscar://192.168.5.62:2005/noah?useUnicode=true&characterEncoding=UTF-8&useSSL=false&allowMultiQueries=true&&serverTimezone=GMT%2b88
    driverClassName: com.oscar.Driver
    username: sysdba
    password: szoscar55
    # 达梦环境
    # url: jdbc:dm://192.168.1.155:5236?schema=noah
    # driver-class-name: dm.jdbc.driver.DmDriver
    # 高斯环境
    # url: jdbc:gaussdb://192.168.1.197:5432/noah?currentSchema=noah
    # driver-class-name: com.huawei.gauss200.jdbc.Driver

  quartz:
    job-store-type: jdbc
    jdbc:
      initialize-schema: never
    properties:
      org.quartz.threadPool:threadPriority: 5
      org.quartz.jobStore.isClustered: true
      org.quartz.jobStore.class: org.quartz.impl.jdbcjobstore.JobStoreTX
      org.quartz.scheduler.instanceId: AUTO
      org.quartz.jobStore.tablePrefix: QRTZ_
      org.quartz.jobStore.acquireTriggersWithinLock: true
      org.quartz.scheduler.instanceName: DolphinScheduler
      org.quartz.threadPool.class: org.quartz.simpl.SimpleThreadPool
      org.quartz.jobStore.useProperties: false
      org.quartz.threadPool.makeThreadsDaemons: true
      org.quartz.threadPool.threadCount: 25
      org.quartz.jobStore.misfireThreshold: 60000
      org.quartz.scheduler.makeSchedulerThreadDaemon: true
      org.quartz.jobStore.driverDelegateClass: org.quartz.impl.jdbcjobstore.StdJDBCDelegate
      org.quartz.jobStore.clusterCheckinInterval: 5000
  servlet:
    multipart:
      max-file-size: 1024MB
      max-request-size: 1024MB
  messages:
    basename: i18n/messages
  jpa:
    hibernate:
      ddl-auto: none


registry:
  type: zookeeper
  zookeeper:
    namespace: dolphinscheduler
    connect-string: ${zkUrls}
    retry-policy:
      base-sleep-time: 60ms
      max-sleep: 300ms
      max-retries: 5
    session-timeout: 30s
    connection-timeout: 9s
    block-until-connected: 600ms
    digest: ~

security:
  authentication:
    # Authentication types (supported types: PASSWORD,LDAP)
    type: PASSWORD
    # IF you set type `LDAP`, below config will be effective
    ldap:
      # admin userId
      user.admin: read-only-admin
      # ldap server config
      urls: ldap://ldap.forumsys.com:389/
      base.dn: dc=example,dc=com
      username: cn=read-only-admin,dc=example,dc=com
      password: password
      user.identity.attribute: uid
      user.email.attribute: mail

# Traffic control, if you turn on this config, the maximum number of request/s will be limited.
# global max request number per second
# default tenant-level max request number
traffic:
  control:
    global-switch: false
    max-global-qps-rate: 300
    tenant-switch: false
    default-tenant-qps-rate: 10
      #customize-tenant-qps-rate:
      # eg.
    #tenant1: 11
    #tenant2: 20

master:
  listen-port: 5678
  # master fetch command num
  fetch-command-num: 10
  # master prepare execute thread number to limit handle commands in parallel
  pre-exec-threads: 10
  # master execute thread number to limit process instances in parallel
  exec-threads: 10
  # master dispatch task number per batch
  dispatch-task-number: 3
  # master host selector to select a suitable worker, default value: LowerWeight. Optional values include random, round_robin, lower_weight
  host-selector: lower_weight
  # master heartbeat interval
  heartbeat-interval: 10s
  # Master heart beat task error threshold, if the continuous error count exceed this count, the master will close.
  heartbeat-error-threshold: 5
  # master commit task retry times
  task-commit-retry-times: 5
  # master commit task interval
  task-commit-interval: 1s
  state-wheel-interval: 5s
  # master max cpuload avg, only higher than the system cpu load average, master server can schedule. default value -1: the number of cpu cores * 2
  max-cpu-load-avg: -1
  # master reserved memory, only lower than system available memory, master server can schedule. default value 0.3, the unit is G
  reserved-memory: 0.3
  # failover interval
  failover-interval: 10m
  # kill yarn jon when failover taskInstance, default true
  kill-yarn-job-when-task-failover: true

worker:
  # worker listener port
  listen-port: 1234
  # worker execute thread number to limit task instances in parallel
  exec-threads: 10
  # worker heartbeat interval
  heartbeat-interval: 10s
  # Worker heart beat task error threshold, if the continuous error count exceed this count, the worker will close.
  heartbeat-error-threshold: 5
  # worker host weight to dispatch tasks, default value 100
  host-weight: 100
  # worker tenant auto create
  tenant-auto-create: true
  # worker max cpuload avg, only higher than the system cpu load average, worker server can be dispatched tasks. default value -1: the number of cpu cores * 2
  max-cpu-load-avg: -1
  # worker reserved memory, only lower than system available memory, worker server can be dispatched tasks. default value 0.3, the unit is G
  reserved-memory: 0.3
  # default worker groups separated by comma, like 'worker.groups=default,test'
  groups:
    - default
  # alert server listen host
  alert-listen-host: localhost
  alert-listen-port: 50052

alert:
  port: 50052
  # Mark each alert of alert server if late after x milliseconds as failed.
  # Define value is (0 = infinite), and alert server would be waiting alert result.
  wait-timeout: 0

python-gateway:
  # Weather enable python gateway server or not. The default value is true.
  enabled: false
  # The address of Python gateway server start. Set its value to `0.0.0.0` if your Python API run in different
  # between Python gateway server. It could be be specific to other address like `127.0.0.1` or `localhost`
  gateway-server-address: 0.0.0.0
  # The port of Python gateway server start. Define which port you could connect to Python gateway server from
  # Python API side.
  gateway-server-port: 25333
  # The address of Python callback client.
  python-address: 127.0.0.1
  # The port of Python callback client.
  python-port: 25334
  # Close connection of socket server if no other request accept after x milliseconds. Define value is (0 = infinite),
  # and socket server would never close even though no requests accept
  connect-timeout: 0
  # Close each active connection of socket server if python program not active after x milliseconds. Define value is
  # (0 = infinite), and socket server would never close even though no requests accept
  read-timeout: 0

server:
  port: 12345
  servlet:
    session:
      timeout: 120m
    context-path: /scheduler/
  compression:
    enabled: true
    mime-types: text/html,text/xml,text/plain,text/css,text/javascript,application/javascript,application/json,application/xml
  jetty:
    max-http-form-post-size: 5000000

management:
  endpoints:
    web:
      exposure:
        include: '*'
  endpoint:
    health:
      enabled: true
      show-details: always
  health:
    db:
      enabled: true
    defaults:
      enabled: false
  metrics:
    tags:
      application: ${r"${spring.application.name}"}

audit:
  enabled: true

metrics:
  enabled: true


sa-token:
  # SSO-相关配置
  sso:
    auth-url: ${authUrl} # 单点登录登录页面地址url
    ## 是否打开单点注销接口
    #is-slo: true
    server-url: ${serverUrl}
    # 是否打开单点注销接口
    is-slo: true
    servicecode: NOAH # 系统编码
    public-key: MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCerqMJBTLxl/5ozlNeysw/SwuOPiqTTKd/j1MCm/2Zj/79VJ0FFvhKtOzOBYhoIu/QtlJPG2Lxrw3vNMksyqMiZ8Wca6a1/DgTicD/t8uTzx6buvfIR7GwccK1a/7u1mL2ZqddTIlmkvx/0IiAxXV2pDnQn1QS400O/xKr6RdXywIDAQAB
    exclude-urls:
      - /sso/getSsoAuthUrl
      - /sso/doLoginByTicket
      - /sso/isLogin
      - /task-group/queryTaskDefineListPaging
      - /datasource/queryDataSourceDetail
      - /workspace/initTenant
      - /cal-engine/simple-list
      - /cal-engine/list
      - /task-instances-statistics/change-statistics-info
      - /task-instances-statistics/schedulerDelProcessInstance
    active: true
    isHttp: true
  #是否允许同一账号并发登录 (为true时允许一起登录, 为false时新登录挤掉旧登录) =-1
  isConcurrent: true
  #在多人登录同一账号时，是否共用一个token (为true时所有登录共用一个token, 为false时每次登录新建一个token)
  isShare: false
  # 是否使用redis存储信息，默认false使用内存来存储信息，true使用spring redis存储信息（请配置spring redis），若配置下述alone-redis，则使用下述alone-redis存储信息（不必再配置spring redis）。
  redis-enabled: true
  # 配置Sa-Token单独使用的Redis连接 （此处需要和SSO-Server端连接同一个Redis）
  alone-redis:
    #是否启用独立的redis来存储信息，此处为true时，redis-enabled也要设置为true
    enabled: ${redisAloneEnabled}
    cluster:
      nodes: ${"redis.nodes"}
      # Redis服务器连接密码（默认为空）
    password: ${"redis.password"}
    host: ${redisAloneHost}
    port: ${redisAlonePort}
    #password:
    # 连接超时时间
    timeout: 10s
    lettuce:
      pool:
        # 连接池最大连接数
        max-active: 200
        # 连接池最大阻塞等待时间（使用负值表示没有限制）
        max-wait: -1ms
        # 连接池中的最大空闲连接
        max-idle: 10
        # 连接池中的最小空闲连接
        min-idle: 0




# Override by profile
logging:
  level:
    org.apache.dolphinscheduler.dao.mapper: debug


---
spring:
  config:
    activate:
      on-profile: ${onProfile}
  quartz:
    properties:
      org.quartz.jobStore.driverDelegateClass: ${orgQuartzJobStoreDriverDelegateClass}
  datasource:
    driver-class-name: ${driverClassName}
    url: ${databaseUrl}
    username: ${username}
    password: ${password}
