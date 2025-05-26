# Apache Ranger 用户指南

## 基础操作指南

本文档将详细介绍Apache Ranger的安装、配置和使用方法，帮助您快速上手Ranger安全管理系统。

## 环境准备

在部署Ranger之前，请确保您的环境满足以下要求：

- JDK 1.8或更高版本
- Hadoop生态组件(如HDFS、Hive、HBase等)
- MySQL/PostgreSQL/Oracle用于Ranger管理数据库
- Solr(可选，用于审计)
- Unix/Linux操作系统

## 安装与部署

### Ranger Admin安装

Ranger Admin是Ranger的中央管理组件，负责策略管理和审计。安装步骤如下：

1. **下载Ranger安装包**

   ```bash
   wget https://downloads.apache.org/ranger/2.0.0/apache-ranger-2.0.0.tar.gz
   tar -xvf apache-ranger-2.0.0.tar.gz
   cd apache-ranger-2.0.0
   ```

2. **配置Ranger Admin**

   修改`install.properties`文件配置数据库连接信息：
   
   ```properties
   DB_FLAVOR=MYSQL
   SQL_CONNECTOR_JAR=/path/to/mysql-connector-java.jar
   db_root_user=root
   db_root_password=root_password
   db_host=localhost
   db_name=ranger
   db_user=ranger
   db_password=ranger_password
   ```

3. **执行安装脚本**

   ```bash
   ./setup.sh
   ```

4. **启动Ranger Admin服务**

   ```bash
   ranger-admin start
   ```

   安装成功后，可通过`http://<ranger-admin-host>:6080`访问Web界面，默认账号为`admin/admin`。

### Ranger UserSync配置

UserSync组件用于从LDAP/AD同步用户和组信息：

1. **修改配置文件**

   编辑`install.properties`：
   
   ```properties
   POLICY_MGR_URL=http://<ranger-admin-host>:6080
   SYNC_SOURCE=ldap
   SYNC_LDAP_URL=ldap://ldap.example.com:389
   SYNC_LDAP_BIND_DN=cn=admin,dc=example,dc=com
   SYNC_LDAP_BIND_PASSWORD=ldap_password
   SYNC_LDAP_SEARCH_BASE=dc=example,dc=com
   SYNC_LDAP_USER_SEARCH_BASE=ou=users,dc=example,dc=com
   SYNC_LDAP_USER_OBJECT_CLASS=posixAccount
   SYNC_LDAP_USER_NAME_ATTRIBUTE=uid
   ```

2. **执行安装脚本**

   ```bash
   ./setup.sh
   ```

3. **启动UserSync服务**

   ```bash
   ranger-usersync start
   ```

### 安装Ranger插件

以HDFS插件为例：

1. **配置插件属性**

   修改`install.properties`：
   
   ```properties
   POLICY_MGR_URL=http://<ranger-admin-host>:6080
   REPOSITORY_NAME=hadoopdev
   COMPONENT_INSTALL_DIR_NAME=/path/to/hadoop
   ```

2. **执行安装脚本**

   ```bash
   ./enable-hdfs-plugin.sh
   ```

3. **重启HDFS服务**

   ```bash
   hadoop-daemon.sh stop namenode
   hadoop-daemon.sh start namenode
   ```

## 基本使用

### 登录Ranger管理控制台

1. 打开浏览器访问`http://<ranger-admin-host>:6080`
2. 使用默认凭据`admin/admin`登录

### 创建服务

以创建HDFS服务为例：

1. 在左侧菜单选择"Service Manager" → "HDFS"
2. 点击"+"按钮添加新服务
3. 填写服务信息：
   - 服务名称：`hdfs_service`
   - 用户名：`hdfs`
   - 密码：(如已配置)
   - NameNode URL：`hdfs://namenode:8020`
4. 点击"测试连接"验证配置
5. 点击"添加"保存服务

### 创建策略

1. 点击已创建的服务名称进入策略页面
2. 点击"添加新策略"
3. 填写策略信息：
   - 策略名称：`hdfs_sensitive_data`
   - 资源路径：`/sensitive/data`
   - 指定用户/组：选择需要授权的用户或组
   - 权限：选择"读取"、"写入"、"执行"等权限
4. 点击"添加"保存策略

### 验证策略

可通过以下命令验证策略是否生效：

```bash
# 以授权用户身份
kinit authorized_user
hadoop fs -ls /sensitive/data  # 应成功

# 以未授权用户身份
kinit unauthorized_user
hadoop fs -ls /sensitive/data  # 应失败并提示权限错误
```

## 高级配置

### 基于标签的策略

Ranger支持基于标签的访问控制，配合Apache Atlas使用：

1. **启用标签服务**

   在Ranger Admin控制台中，创建Tag服务：
   - 服务名称：`tag_service`
   - 标签源：选择Atlas

2. **将标签服务关联到组件服务**

   编辑HDFS服务，在"配置"选项卡中设置：
   - 标签服务：选择刚创建的`tag_service`

3. **创建标签策略**

   在Tag服务中创建策略：
   - 标签：`PII`(敏感数据标签)
   - 用户/组：选择授权用户
   - 权限：配置适当权限

### 行过滤和掩码策略

为Hive服务配置行过滤和掩码策略：

1. **创建行过滤策略**

   - 选择"行级过滤器"选项
   - 填写过滤条件：`department='{USER_DEPARTMENT}'`

2. **创建数据掩码策略**

   - 选择"掩码"选项
   - 选择掩码类型：如"显示最后4位"
   - 应用于包含PII数据的列

### 策略导出与导入

1. **导出策略**
   
   ```bash
   curl -u admin:admin -X GET "http://<ranger-admin>:6080/service/plugins/policies/exportJson?serviceName=hdfs_service" > hdfs_policies.json
   ```

2. **导入策略**

   ```bash
   curl -u admin:admin -X POST -d @hdfs_policies.json -H "Content-Type: application/json" "http://<ranger-admin>:6080/service/plugins/policies/importPoliciesFromFile?isOverride=true&serviceType=hdfs"
   ```

## 审计管理

### 启用审计日志

1. **配置Solr审计**

   修改`install.properties`：
   
   ```properties
   XAAUDIT.SOLR.ENABLE=true
   XAAUDIT.SOLR.URL=http://<solr-host>:8983/solr/ranger_audits
   XAAUDIT.SOLR.USER=solr
   XAAUDIT.SOLR.PASSWORD=solr
   XAAUDIT.SOLR.ZOOKEEPER=<zk-host>:2181
   ```

2. **重新安装插件**使更改生效

### 查看审计日志

1. 在Ranger Admin界面，选择"审计"→"访问"查看所有访问审计记录
2. 使用过滤器按服务、用户、时间等维度过滤审计日志
3. 点击详情查看完整审计信息，包括访问结果和策略详情

### 审计报告

1. 在"审计"→"报告"中查看预定义报告
2. 支持创建自定义报告：
   - 点击"创建新报告"
   - 配置报告条件和指标
   - 设置报告计划和通知选项

## 最佳实践

### 策略管理

1. **采用最小权限原则**：仅授予用户完成工作所需的最小权限
2. **使用组策略**：通过组而非个人用户分配权限，便于管理
3. **分层管理权限**：根据数据敏感度定义不同级别的权限
4. **定期审查**：定期检查和清理过时的策略

### 性能优化

1. **缓存策略**：增加插件缓存大小提高授权性能
   ```properties
   ranger.plugin.hdfs.policy.cache.size=10000
   ```

2. **优化审计配置**：对高频访问路径减少审计日志
   ```properties
   ranger.plugin.hdfs.exclude.paths=/tmp,/staging
   ```

3. **使用Solr集群**：为大规模环境使用Solr集群存储审计日志

### 安全加固

1. **启用SSL**：配置Admin和插件间的SSL通信
2. **定期更改密码**：特别是admin账户密码
3. **集成Kerberos**：确保底层平台已启用Kerberos身份验证
4. **限制Ranger Admin访问**：使用防火墙限制Ranger Admin仅允许授权IP访问

## 故障排除

### 常见问题

1. **策略未生效**
   - 检查插件日志
   - 验证策略定义是否正确
   - 确认服务配置连接参数正确

2. **审计日志不显示**
   - 检查Solr连接配置
   - 确认审计已启用
   - 查看插件审计日志配置

3. **用户同步问题**
   - 检查LDAP连接设置
   - 验证搜索筛选器和基本DN配置
   - 查看UserSync日志

### 日志分析

关键日志文件位置：

- Ranger Admin: `<ranger-home>/ews/logs/ranger-admin-*`
- UserSync: `<ranger-home>/usersync/logs/usersync-*`
- 插件: `<component-home>/logs/ranger-*-plugin-*`

### 诊断命令

```bash
# 检查Ranger Admin状态
ranger-admin status

# 查看插件策略刷新
curl -u admin:admin http://<ranger-admin>:6080/service/plugins/policies/download/<service-name>

# 测试LDAP连接
ldapsearch -x -h <ldap-host> -p 389 -D "cn=admin,dc=example,dc=com" -w password -b "ou=users,dc=example,dc=com"
```

## 参考资源

- [Apache Ranger官方文档](https://ranger.apache.org/documentation.html)
- [Apache Ranger Wiki](https://cwiki.apache.org/confluence/display/RANGER/Index)
- [Ranger GitHub仓库](https://github.com/apache/ranger)
- [Ranger用户邮件列表](https://lists.apache.org/list.html?users@ranger.apache.org) 