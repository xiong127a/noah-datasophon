# Kerberos 用户指南

## 快速入门

本指南将帮助你在 DataSophon 平台上快速部署、配置和使用 Kerberos，为大数据集群构建强大的安全认证体系。

## 前置条件

在开始之前，请确保满足以下条件：

- DataSophon 平台已成功安装并正常运行
- 集群节点之间网络连接正常
- 所有节点时间已同步（使用 NTP 服务）
- 已规划好 Kerberos 领域名称（通常使用大写形式，如 EXAMPLE.COM）
- 已确定 KDC 服务器的部署节点
- DNS 配置正确，所有节点可通过主机名互相访问

## 部署流程

### 通过 DataSophon 平台部署

1. 登录 DataSophon 管理平台
2. 进入【组件管理】页面
3. 点击【添加服务】，在组件列表中选择 Kerberos
4. 按照向导完成配置：
   - 选择 KDC 服务器节点
   - 设置 Kerberos 领域名称
   - 配置管理员账户
   - 设置数据库密码
   - 配置高级参数
5. 确认配置无误后，点击【部署】
6. 等待部署完成，可在【服务状态】查看部署进度

### 配置参数说明

#### 基本配置

| 参数名称 | 说明 | 推荐值 |
|---------|------|-------|
| Kerberos 领域 | Kerberos 管理域名称 | EXAMPLE.COM（使用大写） |
| KDC 主机 | 运行 KDC 服务的主机名 | kdc-server.example.com |
| 管理员主体 | Kerberos 管理员账户名 | admin/admin |
| 管理员密码 | 管理员账户密码 | 强密码，至少12位 |
| 最大票据生命周期 | TGT 票据最长有效期 | 24h（根据安全策略调整） |
| 最大可更新时间 | 票据最长可更新时间 | 7d（根据安全策略调整） |

#### 高级配置

| 参数名称 | 说明 | 推荐值 |
|---------|------|-------|
| kdc_ports | KDC 服务端口 | 88 |
| kadmin_port | 管理服务端口 | 749 |
| master_key_type | 主密钥类型 | aes256-cts-hmac-sha1-96 |
| default_principal_flags | 默认主体标志 | +preauth |
| supported_enctypes | 支持的加密类型 | aes256-cts-hmac-sha1-96:normal aes128-cts-hmac-sha1-96:normal |
| kdc_tcp_ports | KDC TCP 端口 | 88 |

### 部署后初始化

完成平台部署后，需要进行以下初始化操作：

1. 确认 KDC 服务状态：
   ```bash
   systemctl status krb5-kdc
   systemctl status kadmin
   ```

2. 验证管理员账户：
   ```bash
   kadmin.local -q "listprincs"
   ```

3. 创建测试主体：
   ```bash
   kadmin.local -q "addprinc -pw password test"
   ```

4. 测试票据获取：
   ```bash
   kinit test
   klist
   ```

## 基本操作指南

### 主体管理

#### 创建主体

```bash
# 创建用户主体
kadmin.local -q "addprinc -pw password username"

# 创建服务主体
kadmin.local -q "addprinc -randkey HTTP/host.example.com"

# 创建具有特定策略的主体
kadmin.local -q "addprinc -policy user_policy username"
```

#### 列出主体

```bash
# 列出所有主体
kadmin.local -q "listprincs"

# 列出特定模式的主体
kadmin.local -q "listprincs */admin"
```

#### 查看主体详情

```bash
kadmin.local -q "getprinc username"
```

#### 修改主体属性

```bash
# 修改密码
kadmin.local -q "cpw username"

# 修改主体策略
kadmin.local -q "modprinc -policy secure_policy username"

# 修改主体过期时间
kadmin.local -q "modprinc -expire 2023-12-31 username"
```

#### 删除主体

```bash
kadmin.local -q "delprinc username"
```

### 密钥表管理

密钥表（keytab）文件包含服务主体的加密密钥，用于服务认证。

#### 创建密钥表

```bash
# 为服务创建密钥表
kadmin.local -q "ktadd -k /etc/krb5.keytab HTTP/host.example.com"

# 为特定服务创建专用密钥表
kadmin.local -q "ktadd -k /path/to/service.keytab service/host.example.com"
```

#### 查看密钥表内容

```bash
klist -kt /path/to/keytab
```

#### 测试密钥表

```bash
kinit -kt /path/to/keytab service/host.example.com
klist
```

### 策略管理

密码策略用于强制执行密码安全规则。

#### 创建策略

```bash
# 创建基本策略
kadmin.local -q "addpol -minlength 8 -minclasses 3 -history 5 basic_policy"

# 创建高安全性策略
kadmin.local -q "addpol -minlength 12 -minclasses 4 -history 10 -maxlife 90d secure_policy"
```

#### 列出策略

```bash
kadmin.local -q "listpols"
```

#### 查看策略详情

```bash
kadmin.local -q "getpol policy_name"
```

#### 修改策略

```bash
kadmin.local -q "modpol -minlength 10 policy_name"
```

#### 删除策略

```bash
kadmin.local -q "delpol policy_name"
```

### 客户端配置

在集群的所有节点上配置 Kerberos 客户端：

1. 确认 krb5.conf 文件配置正确：

   ```ini
   [libdefaults]
     default_realm = EXAMPLE.COM
     dns_lookup_realm = false
     dns_lookup_kdc = false
     ticket_lifetime = 24h
     renew_lifetime = 7d
     forwardable = true
     udp_preference_limit = 1
     default_tkt_enctypes = aes256-cts-hmac-sha1-96
     default_tgs_enctypes = aes256-cts-hmac-sha1-96
     permitted_enctypes = aes256-cts-hmac-sha1-96
   
   [realms]
     EXAMPLE.COM = {
       kdc = kdc-server.example.com:88
       admin_server = kdc-server.example.com:749
       default_domain = example.com
     }
   
   [domain_realm]
     .example.com = EXAMPLE.COM
     example.com = EXAMPLE.COM
   ```

2. 测试客户端配置：

   ```bash
   kinit admin/admin
   klist
   ```

## 与 Hadoop 生态系统集成

### HDFS 安全配置

1. 创建 HDFS 服务主体：

   ```bash
   kadmin.local -q "addprinc -randkey nn/namenode.example.com@EXAMPLE.COM"
   kadmin.local -q "addprinc -randkey dn/datanode1.example.com@EXAMPLE.COM"
   # 为所有 DataNode 创建主体
   ```

2. 生成 HDFS 密钥表：

   ```bash
   kadmin.local -q "ktadd -k /etc/hadoop/conf/hdfs.keytab nn/namenode.example.com@EXAMPLE.COM"
   kadmin.local -q "ktadd -k /etc/hadoop/conf/hdfs.keytab dn/datanode1.example.com@EXAMPLE.COM"
   ```

3. 配置 core-site.xml：

   ```xml
   <property>
     <name>hadoop.security.authentication</name>
     <value>kerberos</value>
   </property>
   <property>
     <name>hadoop.security.authorization</name>
     <value>true</value>
   </property>
   ```

4. 配置 hdfs-site.xml：

   ```xml
   <property>
     <name>dfs.namenode.kerberos.principal</name>
     <value>nn/_HOST@EXAMPLE.COM</value>
   </property>
   <property>
     <name>dfs.namenode.keytab.file</name>
     <value>/etc/hadoop/conf/hdfs.keytab</value>
   </property>
   <property>
     <name>dfs.datanode.kerberos.principal</name>
     <value>dn/_HOST@EXAMPLE.COM</value>
   </property>
   <property>
     <name>dfs.datanode.keytab.file</name>
     <value>/etc/hadoop/conf/hdfs.keytab</value>
   </property>
   <property>
     <name>dfs.web.authentication.kerberos.principal</name>
     <value>HTTP/_HOST@EXAMPLE.COM</value>
   </property>
   ```

### YARN 安全配置

1. 创建 YARN 服务主体：

   ```bash
   kadmin.local -q "addprinc -randkey rm/resourcemanager.example.com@EXAMPLE.COM"
   kadmin.local -q "addprinc -randkey nm/nodemanager1.example.com@EXAMPLE.COM"
   # 为所有 NodeManager 创建主体
   ```

2. 生成 YARN 密钥表：

   ```bash
   kadmin.local -q "ktadd -k /etc/hadoop/conf/yarn.keytab rm/resourcemanager.example.com@EXAMPLE.COM"
   kadmin.local -q "ktadd -k /etc/hadoop/conf/yarn.keytab nm/nodemanager1.example.com@EXAMPLE.COM"
   ```

3. 配置 yarn-site.xml：

   ```xml
   <property>
     <name>yarn.resourcemanager.principal</name>
     <value>rm/_HOST@EXAMPLE.COM</value>
   </property>
   <property>
     <name>yarn.resourcemanager.keytab</name>
     <value>/etc/hadoop/conf/yarn.keytab</value>
   </property>
   <property>
     <name>yarn.nodemanager.principal</name>
     <value>nm/_HOST@EXAMPLE.COM</value>
   </property>
   <property>
     <name>yarn.nodemanager.keytab</name>
     <value>/etc/hadoop/conf/yarn.keytab</value>
   </property>
   <property>
     <name>yarn.resourcemanager.webapp.delegation-token-auth-filter.enabled</name>
     <value>true</value>
   </property>
   ```

### HBase 安全配置

1. 创建 HBase 服务主体：

   ```bash
   kadmin.local -q "addprinc -randkey hbase/master.example.com@EXAMPLE.COM"
   kadmin.local -q "addprinc -randkey hbase/regionserver1.example.com@EXAMPLE.COM"
   # 为所有 RegionServer 创建主体
   ```

2. 生成 HBase 密钥表：

   ```bash
   kadmin.local -q "ktadd -k /etc/hbase/conf/hbase.keytab hbase/master.example.com@EXAMPLE.COM"
   kadmin.local -q "ktadd -k /etc/hbase/conf/hbase.keytab hbase/regionserver1.example.com@EXAMPLE.COM"
   ```

3. 配置 hbase-site.xml：

   ```xml
   <property>
     <name>hbase.security.authentication</name>
     <value>kerberos</value>
   </property>
   <property>
     <name>hbase.security.authorization</name>
     <value>true</value>
   </property>
   <property>
     <name>hbase.master.kerberos.principal</name>
     <value>hbase/_HOST@EXAMPLE.COM</value>
   </property>
   <property>
     <name>hbase.master.keytab.file</name>
     <value>/etc/hbase/conf/hbase.keytab</value>
   </property>
   <property>
     <name>hbase.regionserver.kerberos.principal</name>
     <value>hbase/_HOST@EXAMPLE.COM</value>
   </property>
   <property>
     <name>hbase.regionserver.keytab.file</name>
     <value>/etc/hbase/conf/hbase.keytab</value>
   </property>
   ```

### Hive 安全配置

1. 创建 Hive 服务主体：

   ```bash
   kadmin.local -q "addprinc -randkey hive/hiveserver.example.com@EXAMPLE.COM"
   ```

2. 生成 Hive 密钥表：

   ```bash
   kadmin.local -q "ktadd -k /etc/hive/conf/hive.keytab hive/hiveserver.example.com@EXAMPLE.COM"
   ```

3. 配置 hive-site.xml：

   ```xml
   <property>
     <name>hive.server2.authentication</name>
     <value>KERBEROS</value>
   </property>
   <property>
     <name>hive.server2.authentication.kerberos.principal</name>
     <value>hive/_HOST@EXAMPLE.COM</value>
   </property>
   <property>
     <name>hive.server2.authentication.kerberos.keytab</name>
     <value>/etc/hive/conf/hive.keytab</value>
   </property>
   <property>
     <name>hive.metastore.sasl.enabled</name>
     <value>true</value>
   </property>
   <property>
     <name>hive.metastore.kerberos.principal</name>
     <value>hive/_HOST@EXAMPLE.COM</value>
   </property>
   <property>
     <name>hive.metastore.kerberos.keytab.file</name>
     <value>/etc/hive/conf/hive.keytab</value>
   </property>
   ```

## 用户管理与授权

### 创建用户账户

1. 创建用户主体：

   ```bash
   kadmin.local -q "addprinc -pw password user1"
   ```

2. 为用户设置适当的权限策略：

   ```bash
   kadmin.local -q "modprinc -policy user_policy user1"
   ```

### 用户授权

在启用 Kerberos 的集群中，还需要配置授权系统（如 Ranger 或 HDFS ACL）：

1. HDFS 权限设置：

   ```bash
   # 为用户创建主目录
   hdfs dfs -mkdir /user/user1
   hdfs dfs -chown user1:user1 /user/user1
   hdfs dfs -chmod 750 /user/user1
   
   # 设置 ACL
   hdfs dfs -setfacl -m user:user1:rwx /data/shared
   ```

2. Ranger 权限设置（如已集成）：
   - 登录 Ranger 管理界面
   - 创建针对服务（HDFS、HBase、Hive 等）的访问策略
   - 为用户分配适当的权限

## 日常运维

### 密码管理

#### 更改用户密码

```bash
# 管理员更改密码
kadmin.local -q "cpw username"

# 用户自行更改密码
kpasswd
```

#### 密钥轮换

定期轮换服务密钥是安全最佳实践：

```bash
# 更新服务密钥
kadmin.local -q "cpw -randkey service/host.example.com"
kadmin.local -q "ktadd -k /path/to/service.keytab service/host.example.com"

# 重启相关服务
systemctl restart service-name
```

### 票据管理

#### 查看当前票据

```bash
klist
```

#### 销毁票据

```bash
kdestroy
```

#### 更新票据

```bash
kinit -R
```

### 数据库维护

#### 备份 KDC 数据库

```bash
kdb5_util dump /path/to/backup/kerberos-db-backup
```

#### 恢复 KDC 数据库

```bash
kdb5_util load /path/to/backup/kerberos-db-backup
```

#### 查看数据库状态

```bash
kdb5_util status
```

## 故障排除

### 常见问题及解决方案

#### 认证失败

问题症状：
- 出现 "Kerberos authentication failed" 错误
- kinit 命令失败

排查步骤：
1. 检查用户主体是否存在：`kadmin.local -q "getprinc username"`
2. 确认密码是否正确
3. 检查时间同步：`ntpq -p`
4. 查看 KDC 日志：`/var/log/krb5kdc.log`

解决方案：
- 重置用户密码：`kadmin.local -q "cpw username"`
- 同步服务器时间：`ntpdate time-server`
- 确保 DNS 和主机名解析正确

#### 服务无法启动

问题症状：
- 启用 Kerberos 后服务无法启动
- 服务日志中出现认证错误

排查步骤：
1. 检查服务主体是否存在：`kadmin.local -q "getprinc service/host.example.com"`
2. 验证密钥表是否正确：`klist -kt /path/to/service.keytab`
3. 检查服务配置中的主体名称是否匹配

解决方案：
- 重新创建服务主体和密钥表
- 确保密钥表权限正确：`chmod 400 /path/to/service.keytab`
- 检查配置文件中的 _HOST 占位符是否正确解析

#### 票据过期

问题症状：
- 操作过程中出现 "Ticket expired" 错误
- klist 显示票据已过期

解决方案：
- 重新获取票据：`kinit username`
- 考虑延长票据生命周期：修改 krb5.conf 中的 ticket_lifetime 参数
- 使用票据自动更新机制：`k5start -f /path/to/keytab -K 60 -k /tmp/krb5cc_service service/host.example.com`

#### 跨领域认证问题

问题症状：
- 无法访问其他领域的服务
- 出现 "Cannot find KDC for realm" 错误

排查步骤：
1. 检查领域信任配置：`kadmin.local -q "getprinc krbtgt/REALM2@REALM1"`
2. 确认 krb5.conf 中包含所有相关领域配置

解决方案：
- 建立领域信任关系：
  ```bash
  kadmin.local -q "addprinc -randkey krbtgt/REALM2@REALM1"
  kadmin.local -q "addprinc -randkey krbtgt/REALM1@REALM2"
  ```
- 更新 krb5.conf 文件，添加所有相关领域信息

### 日志分析

Kerberos 相关日志位置：

- KDC 日志：`/var/log/krb5kdc.log`
- 管理服务日志：`/var/log/kadmind.log`
- 客户端日志：可通过设置环境变量启用
  ```bash
  export KRB5_TRACE=/tmp/krb5_trace.log
  kinit username
  ```

常见日志错误及含义：

- `Client not found in Kerberos database`：用户主体不存在
- `Preauthentication failed`：密码错误或加密类型不匹配
- `Cannot contact any KDC for realm`：网络问题或 KDC 配置错误
- `Clock skew too great`：客户端与 KDC 服务器时间不同步
- `Key version number for principal mismatch`：密钥表过期或不匹配

## 安全最佳实践

### 密码策略

- 实施强密码策略（长度、复杂度、定期更改）
- 为不同类型的用户设置不同的密码策略
- 定期审计密码强度

### 密钥表保护

- 限制密钥表文件访问权限（chmod 400）
- 不同服务使用不同的密钥表文件
- 定期轮换服务密钥

### 监控与审计

- 监控认证失败事件
- 定期审查 KDC 日志
- 设置异常行为告警
- 实施入侵检测系统

### 高可用配置

- 部署多个 KDC 服务器（主从架构）
- 定期备份 KDC 数据库
- 制定灾难恢复计划
- 测试故障转移机制

## 总结

本指南介绍了在 DataSophon 平台上部署、配置和管理 Kerberos 的关键步骤。通过正确实施 Kerberos 认证，可以为大数据集群建立强大的安全基础，保护敏感数据和计算资源。

Kerberos 作为一种成熟的身份验证协议，与 Hadoop 生态系统的深度集成使其成为大数据安全的核心组件。通过遵循本指南中的最佳实践和建议，可以有效提升集群的安全性，满足企业级安全要求和合规标准。

在实际部署过程中，建议根据具体环境和安全需求调整配置参数，并结合其他安全组件（如 Ranger、Knox、Sentry 等）构建全面的大数据安全解决方案。 