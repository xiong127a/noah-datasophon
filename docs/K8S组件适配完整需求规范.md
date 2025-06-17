# K8S组件适配完整需求规范

## 文档概述

本文档定义了大数据平台组件K8S适配的完整需求规范，基于HDFS组件的成功实现模式，为其他组件（如Kafka、HBase、Spark等）提供标准化的适配指南。

## 适配目标

实现组件的完整K8S参数适配功能，包括：
- 配置分组和参数管理
- 端口绑定和网络服务配置  
- 存储和资源配置
- 依赖检查和初始化容器
- 自定义配置支持

## 核心原则

### 1. 完全参照HDFS模式
- 所有配置的标签、描述、默认值必须与HDFS保持一致的格式和风格
- 只替换组件特定的部分，不能随意修改通用描述
- 确保配置的一致性和用户体验的统一性

### 2. 配置完整性
- 所有K8S相关参数必须完整配置
- 每个配置文件都必须有对应的自定义配置参数
- 端口绑定配置必须完整

### 3. 模板规范性
- K8S模板必须遵循平台标准
- 支持SVC自动创建
- 包含必要的初始化容器和依赖检查

## 详细适配需求

### 1. 策略处理类修正

#### 文件位置
```
datasophon-k8s/src/main/java/com/datasophon/k8s/strategy/K8s[组件名]HandlerStrategy.java
```


### 2. K8S参数完全对标HDFS

#### 文件位置
```
datasophon-api/src/main/resources/meta/DDP-1.2.1/[组件名]/service_ddl.json
```

#### 2.1 存储配置参数

##### storage_classes
```json
{
  "name": "storage_classes",
  "label": "存储类名称",
  "description": "Kubernetes存储类名称，用于动态创建持久卷(PV)。需预先在集群中创建对应存储类（如nfs-client），确保与底层存储系统匹配",
  "configType": "k8s",
  "required": false,
  "type": "input",
  "value": "nfs-client",
  "configurableInWizard": true,
  "hidden": false,
  "defaultValue": "nfs-client",
  "configCategory": "k8s",
  "configGroup": "kubernetes.config.persistent-volume-claims",
  "configLevel": "advanced"
}
```

##### mount_path
```json
{
  "name": "mount_path",
  "label": "挂载路径",
  "description": "持久卷在Pod内的挂载路径，需与存储类配合使用。该路径将作为[组件名]的数据存储位置的父目录",
  "configType": "k8s",
  "required": false,
  "type": "input",
  "value": "/data",
  "configurableInWizard": true,
  "hidden": false,
  "defaultValue": "/data",
  "configCategory": "k8s",
  "configGroup": "kubernetes.config.persistent-volume-claims",
  "configLevel": "advanced"
}
```

##### storage_size
```json
{
  "name": "storage_size",
  "label": "存储大小",
  "description": "持久卷的存储大小，需与存储类配合使用。例如：20Gi",
  "configType": "k8s",
  "required": false,
  "type": "input",
  "value": "20",
  "configurableInWizard": true,
  "hidden": false,
  "unit": "Gi",
  "defaultValue": "20",
  "configCategory": "k8s",
  "configGroup": "kubernetes.config.persistent-volume-claims",
  "configLevel": "advanced"
}
```

#### 2.2 资源配置参数

##### requests_memory
```json
{
  "name": "requests_memory",
  "label": "内存请求值",
  "description": "容器启动时保证分配的最小内存量（如 2Gi）。建议设置为日常内存使用峰值的 1.2 倍，单位支持 Gi/Mi。",
  "configType": "k8s",
  "required": false,
  "type": "input",
  "value": "2",
  "unit": "Gi",
  "configurableInWizard": true,
  "hidden": false,
  "defaultValue": "2",
  "configCategory": "k8s",
  "configGroup": "kubernetes.config.resources",
  "configLevel": "advanced"
}
```

##### requests_cpu
```json
{
  "name": "requests_cpu",
  "label": "CPU 请求值",
  "description": "容器启动时保证分配的最小 CPU 核心数（如 1）。建议设置为日常 CPU 使用峰值的 1.5 倍，支持小数（如 0.5 表示 500m 毫核）。",
  "configType": "k8s",
  "required": false,
  "type": "input",
  "value": "1",
  "configurableInWizard": true,
  "hidden": false,
  "defaultValue": "1",
  "configCategory": "k8s",
  "configGroup": "kubernetes.config.resources",
  "configLevel": "advanced"
}
```

##### limits_memory
```json
{
  "name": "limits_memory",
  "label": "内存限制值",
  "description": "容器允许使用的最大内存量（如 4Gi）。必须大于请求值，建议不超过请求值的 2 倍，否则可能引发 OOM Kill 风险。",
  "configType": "k8s",
  "required": false,
  "type": "input",
  "value": "4",
  "unit": "Gi",
  "configurableInWizard": true,
  "hidden": false,
  "defaultValue": "4",
  "configCategory": "k8s",
  "configGroup": "kubernetes.config.resources",
  "configLevel": "advanced"
}
```

##### limits_cpu
```json
{
  "name": "limits_cpu",
  "label": "CPU 限制值 (limits.cpu)",
  "description": "容器允许使用的最大 CPU 核心数（如 2）。建议设置为请求值的 2-3 倍以应对突发负载，但需确保节点资源充足。",
  "configType": "k8s",
  "required": false,
  "type": "input",
  "value": "2",
  "configurableInWizard": true,
  "hidden": false,
  "defaultValue": "2",
  "configCategory": "k8s",
  "configGroup": "kubernetes.config.resources",
  "configLevel": "advanced"
}
```

#### 2.3 网络配置参数

##### node_port_mappings
```json
{
  "name": "node_port_mappings",
  "label": "NodePort端口映射",
  "description": "配置Kubernetes Service的NodePort类型端口映射，实现集群外部对服务的访问。键(key)指定容器内的服务端口(containerPort)，值(value)指定对应的NodePort端口(集群节点上开放的外部访问端口，范围30000-32767)。若值为空，则仅创建ClusterIP服务不开放NodePort。例如：9870:30870表示将容器的9870端口映射到集群节点的30870端口。",
  "configType": "k8s",
  "required": false,
  "type": "multipleWithKey",
  "value": [],
  "configurableInWizard": true,
  "hidden": false,
  "defaultValue": [],
  "configCategory": "k8s",
  "configGroup": "kubernetes.config.services",
  "configLevel": "advanced"
}
```

##### cluster_port_mappings
```json
{
  "name": "cluster_port_mappings",
  "label": "集群内部端口映射",
  "description": "配置Kubernetes Service的ClusterIP类型端口映射，仅用于集群内部通信。键(key)指定容器内的服务端口(containerPort)，值(value)通常与键相同，表示不需要特别映射。这些端口只能在Kubernetes集群内部访问，无法从外部直接访问。",
  "configType": "k8s",
  "required": false,
  "type": "multipleWithKey",
  "value": [],
  "configurableInWizard": true,
  "hidden": false,
  "defaultValue": [],
  "configCategory": "k8s",
  "configGroup": "kubernetes.config.services",
  "configLevel": "advanced"
}
```

### 3. 端口绑定配置

#### 配置要求
为组件的主要端口添加绑定配置，实现端口与角色的关联。

#### 必须包含字段
- `bindRole`: 对应的角色名称（如"KafkaBroker"、"NameNode"等）
- `serviceType`: 服务类型，"ClusterIP"或"NodePort"
- `portNumber`: 端口号（字符串格式）
- `nodePort`: NodePort端口号（仅NodePort类型需要，字符串格式）

#### 配置示例
```json
{
  "name": "advertised.listeners",
  "label": "对外广播监听地址",
  "description": "Kafka对外广播的监听地址配置",
  "bindRole": "KafkaBroker",
  "serviceType": "NodePort",
  "portNumber": "9092",
  "nodePort": "30092",
  "configType": "map",
  "required": true,
  "type": "input",
  "value": "PLAINTEXT://${hostname}:9092",
  "configurableInWizard": true,
  "hidden": false,
  "defaultValue": "PLAINTEXT://${hostname}:9092"
}
```

### 4. 配置分组完善

#### 4.1 配置分类（configCategory）
- `configCategory="k8s"`: K8S专用参数
- `configCategory="role"`: 角色相关参数
- `configCategory="file"`: 文件配置参数

#### 4.2 配置分组（configGroup）
- **K8S参数分组**:
  - `kubernetes.config.persistent-volume-claims`: 存储配置
  - `kubernetes.config.services`: 网络服务配置
  - `kubernetes.config.resources`: 资源配置

- **角色参数分组**:
  - `configGroup="[角色名]"`: 如"KafkaBroker"、"NameNode"等
  - **重要规则**: 角色名必须是在`roles`数组中定义的有效角色名

- **文件参数分组**:
  - `configGroup="[配置文件名]"`: 如"server"、"core-site"等
  - **重要规则**: 配置文件名必须是在`configWriter.generators`数组中定义的有效文件名（不含扩展名）

#### 4.3 配置级别（configLevel）
- `configLevel="advanced"`: 高级配置
- `configLevel="custom"`: 自定义配置

#### 4.4 配置分组映射规则
为确保配置参数的正确分组，必须遵循以下映射规则：

1. **如果`configCategory="file"`**:
   - `configGroup`必须是`configWriter.generators`数组中定义的有效文件名
   - 例如：如果参数属于"server.properties"文件，则`configGroup`应为"server"

2. **如果`configCategory="role"`**:
   - `configGroup`必须是`roles`数组中定义的有效角色名
   - 例如：如果参数与"KafkaBroker"角色相关，则`configGroup`应为"KafkaBroker"

3. **如果`configCategory="k8s"`**:
   - `configGroup`必须是以下三种之一：
     - `kubernetes.config.persistent-volume-claims`
     - `kubernetes.config.services`
     - `kubernetes.config.resources`

#### 配置分组示例
```json
// 文件配置示例
{
  "name": "num.partitions",
  "label": "默认分区数",
  "description": "新创建topic的默认分区数",
  "configType": "map",
  "required": true,
  "type": "input",
  "value": "3",
  "configurableInWizard": true,
  "hidden": false,
  "defaultValue": "3",
  "configCategory": "file",
  "configGroup": "server",
  "configLevel": "advanced"
}

// 角色配置示例
{
  "name": "kafkaHeapSize",
  "label": "Kafka堆内存大小",
  "description": "Kafka Broker进程的JVM堆内存大小",
  "configType": "map",
  "required": true,
  "type": "input",
  "value": "6",
  "unit": "GB",
  "configurableInWizard": true,
  "hidden": false,
  "defaultValue": "6",
  "configCategory": "role",
  "configGroup": "KafkaBroker",
  "configLevel": "advanced"
}

// K8S配置示例
{
  "name": "requests_memory",
  "label": "内存请求值",
  "description": "容器启动时保证分配的最小内存量",
  "configType": "k8s",
  "required": false,
  "type": "input",
  "value": "2",
  "unit": "Gi",
  "configurableInWizard": true,
  "hidden": false,
  "defaultValue": "2",
  "configCategory": "k8s",
  "configGroup": "kubernetes.config.resources",
  "configLevel": "advanced"
}

// 安全相关配置示例（注意configCategory仍为"file"）
{
  "name": "enableKerberos",
  "label": "开启Kerberos认证",
  "description": "开启Kerberos认证",
  "required": false,
  "type": "switch",
  "value": false,
  "configurableInWizard": true,
  "hidden": false,
  "defaultValue": false,
  "configCategory": "file",
  "configGroup": "server",
  "configLevel": "advanced"
}
```

### 5. 自定义配置参数

#### 配置要求
每个file级别的配置分组都必须有对应的自定义配置参数，允许用户添加自定义的配置项。

#### 命名规则
`custom.[配置文件名].properties`

#### 配置模板
```json
{
  "name": "custom.[配置文件名].properties",
  "label": "自定义[组件名][配置文件描述]配置",
  "description": "用于添加自定义的[组件名][配置文件描述]参数，这些参数将被添加到[配置文件名].properties配置文件中",
  "configType": "custom",
  "required": false,
  "type": "multipleWithKey",
  "value": [],
  "configurableInWizard": true,
  "hidden": false,
  "defaultValue": "",
  "configCategory": "file",
  "configGroup": "[配置文件名]",
  "configLevel": "custom"
}
```

#### 具体示例
```json
{
  "name": "custom.server.properties",
  "label": "自定义Kafka服务器配置",
  "description": "用于添加自定义的Kafka服务器参数，这些参数将被添加到server.properties配置文件中",
  "configType": "custom",
  "required": false,
  "type": "multipleWithKey",
  "value": [],
  "configurableInWizard": true,
  "hidden": false,
  "defaultValue": "",
  "configCategory": "file",
  "configGroup": "server",
  "configLevel": "custom"
}
```

#### configWriter配置
必须在对应的configWriter的includeParams中包含自定义配置参数：

```json
{
  "filename": "server.properties",
  "fileDescription": "Kafka服务器主配置文件",
  "configFormat": "properties",
  "configTargetRoles": "KafkaBroker",
  "outputDirectory": "config",
  "includeParams": [
    "broker.id",
    "listeners",
    "log.dirs",
    "custom.server.properties"
  ]
}
```

### 6. K8S配置生成器

#### 配置要求
确保包含完整的K8S配置生成器，支持存储、网络、资源三个方面的配置。

#### 必须包含的生成器
```json
{
  "filename": "kubernetes.config.persistent-volume-claims",
  "fileDescription": "[组件名] Kubernetes存储配置，定义持久卷存储类和挂载路径",
  "configFormat": "properties",
  "configTargetRoles": "[角色1],[角色2]",
  "outputDirectory": "",
  "includeParams": [
    "storage_classes",
    "mount_path",
    "storage_size"
  ]
},
{
  "filename": "kubernetes.config.services",
  "fileDescription": "[组件名] Kubernetes网络服务配置，定义端口",
  "configFormat": "properties",
  "configTargetRoles": "[角色1],[角色2]",
  "outputDirectory": "",
  "includeParams": [
    "node_port_mappings",
    "cluster_port_mappings"
  ]
},
{
  "filename": "kubernetes.config.resources",
  "fileDescription": "cpu、memory系统资源",
  "configFormat": "properties",
  "configTargetRoles": "[角色1],[角色2]",
  "outputDirectory": "",
  "includeParams": [
    "requests_memory",
    "requests_cpu",
    "limits_memory",
    "limits_cpu"
  ]
}
```

### 7. K8S模板文件适配

#### 文件位置
```
datasophon-k8s/src/main/resources/k8s/templates/[组件名]/k8s/
```

#### 7.1 SVC适配要求

##### 删除volumePathSet挂载
程序会自动创建SVC，模板中不需要volumePathSet相关配置。

**需要删除的配置**:
```yaml
# 删除volumeMounts中的volumePathSet部分
volumeMounts:
  <#list volumePathSet as item>
  - name: "${item.name}"
    mountPath: "${item.value}"
  </#list>

# 删除volumes中的volumePathSet部分
volumes:
  <#list volumePathSet as item>
  - name: "${item.name}"
    hostPath:
      path: "${item.value}"
  </#list>
```

**保留的配置**:
```yaml
volumeMounts:
  <#list volumeConfigMapSet as item>
  - name: "${item.name}"
    mountPath: "${item.value}"
    subPath: "${item.fileName}"
  </#list>
  - name: "timezone"
    mountPath: "/etc/localtime"

volumes:
  <#list volumeConfigMapSet as item>
  - name: "${item.name}"
    configMap:
      name: "${item.name}"
  </#list>
  - name: "timezone"
    hostPath:
      path: "/etc/localtime"
```

#### 7.2 资源配置要求

##### 不使用默认值
使用`${requests_memory}`而不是`${requests_memory!'2Gi'}`，与HDFS保持一致。

**正确的资源配置**:
```yaml
resources:
  requests:
    memory: ${requests_memory}
    cpu: ${requests_cpu}
  limits:
    memory: ${limits_memory}
    cpu: ${limits_cpu}
```

**错误的配置（不要使用）**:
```yaml
resources:
  requests:
    memory: "${requests_memory!'2Gi'}"  # 错误：包含默认值
    cpu: "${requests_cpu!'1'}"          # 错误：包含默认值
```

#### 7.3 初始化容器要求

##### 依赖检查
根据组件依赖关系添加相应的初始化容器，参考HDFS NameNode的实现模式。

**通用依赖检查模板**:
```yaml
initContainers:
  - name: wait-for-[依赖组件]
    image: "${dockerBusyboxImage}"
    command:
      - "/bin/sh"
      - "-c"
      - |
        # 定义颜色和图标
        RED='\033[0;31m'
        GREEN='\033[0;32m'
        YELLOW='\033[1;33m'
        BLUE='\033[0;34m'
        NC='\033[0m' # No Color
        CHECK_MARK="✅"
        WARNING="⚠️"
        ERROR="❌"
        INFO="ℹ️"
        PROGRESS="🔄"

        echo -e "$BLUE$INFO 开始检查[依赖组件]集群状态...$NC"

        # 使用从配置中获取的依赖组件地址
        <#if [依赖组件配置参数]??>
        DEPENDENCY_CONNECT="${[依赖组件配置参数]}"
        <#else>
        echo -e "$RED$ERROR 错误: 配置中未提供[依赖组件]地址，无法继续$NC"
        exit 1
        </#if>

        # 检查逻辑...

    volumeMounts:
      <#list volumeConfigMapSet as item>
      - name: "${item.name}"
        mountPath: "${item.value}"
        subPath: "${item.fileName}"
      </#list>
      - name: "timezone"
        mountPath: "/etc/localtime"
```

##### 数据目录准备
如需要，添加数据目录和权限准备的初始化容器：

```yaml
- name: prepare-dirs-and-permissions
  image: "${dockerBusyboxImage}"
  env:
    - name: POD_NAME
      valueFrom:
        fieldRef:
          fieldPath: metadata.name
    - name: POD_NAMESPACE
      valueFrom:
        fieldRef:
          fieldPath: metadata.namespace
  command:
    - "/bin/sh"
    - "-c"
    - |
      echo "========== 开始准备[组件名]数据目录和权限 =========="

      # 准备数据目录逻辑
      <#if [数据目录配置参数]??>
      DATA_DIRS="${[数据目录配置参数]}"
      <#else>
      DATA_DIRS="/data/[组件名]-data"
      </#if>

      echo "目标数据目录: $DATA_DIRS"

      # 处理多个目录（逗号分隔）
      OLD_IFS="$IFS"
      IFS=","
      for DATA_DIR in $DATA_DIRS; do
        IFS="$OLD_IFS"
        echo "创建目录: $DATA_DIR"
        mkdir -p $DATA_DIR
        chmod -R 777 $DATA_DIR
        chown -R ${runAsUser}:${runAsGroup} $DATA_DIR
        IFS=","
      done
      IFS="$OLD_IFS"

      echo "========== 完成数据目录和权限设置 =========="
  securityContext:
    runAsUser: 0  # 以root用户运行
    privileged: true
  volumeMounts:
    <#list volumeConfigMapSet as item>
    - name: "${item.name}"
      mountPath: "${item.value}"
      subPath: "${item.fileName}"
    </#list>
    - name: "timezone"
      mountPath: "/etc/localtime"
```

### 8. 重要注意事项

#### 8.1 unit字段的重要性
- **作用**: 程序会将value+unit拼接作为最终值
- **UI效果**: 页面会单独显示单位，提升用户体验
- **必须添加unit字段的参数**:
  - `storage_size`: unit="Gi"
  - `requests_memory`: unit="Gi"
  - `limits_memory`: unit="Gi"

#### 8.2 配置一致性要求
- **标签文本**: 必须与HDFS保持一致，不能随意修改
- **描述文本**: 只替换组件特定部分，通用描述保持一致
- **默认值**: 按照HDFS的模式设置，确保合理性

#### 8.3 配置完整性检查
- **K8S参数**: 6个必需参数都必须配置
- **端口绑定**: 主要端口都必须有bindRole等字段
- **自定义配置**: 每个配置文件都必须有对应的custom参数
- **配置生成器**: 3个K8S配置生成器都必须包含

#### 8.4 模板适配检查
- **volumePathSet**: 必须删除，程序自动创建SVC
- **资源配置**: 不能有默认值，必须参数化
- **初始化容器**: 根据依赖关系添加相应检查

## Kafka适配经验教训

在Kafka组件的K8S适配过程中，我们总结了以下关键经验和注意事项，这些可以帮助后续组件适配更加顺利：

### 1. 参数配置一致性
- **标签文本必须精确一致**：Kafka最初使用了"K8S存储类"等标签，与HDFS的"存储类名称"不一致，这需要严格对照HDFS进行修正
- **描述文本不可简化**：Kafka最初的描述文本过于简化，缺少了HDFS中包含的详细说明和最佳实践建议
- **默认值格式统一**：资源配置的默认值必须与HDFS保持相同格式，如"2"而非"2Gi"

### 2. unit字段的重要性
- **单位分离原则**：Kafka最初将单位合并在value中（如"2Gi"），正确做法是将数值和单位分开，由程序合成
- **处理单位的规则**：K8sYamlDeploymentHandler.java中会自动将value和unit拼接，无需在参数中预先拼接
- **常见错误**：存储大小、内存请求值、内存限制值是最容易出现unit字段问题的参数

### 3. configLevel字段缺失问题
- **必需的配置级别**：Kafka最初缺少了configLevel="advanced"字段，这会影响UI展示和配置分类
- **保持完整性**：确保每个K8S参数都包含configCategory、configGroup和configLevel字段

### 4. FreeMarker模板中的变量处理
- **带点变量的特殊处理**：对于带点的配置名（如log.dirs），需要在K8sYamlDeploymentHandler.java中通过populateDataWithConfig方法进行特殊处理
- **变量命名转换**：将log.dirs转换为kafka_log_dirs，以便在FreeMarker模板中正确引用

### 5. 模板文件问题
- **StatefulSet vs Deployment**：对于有状态服务如Kafka，应使用StatefulSet而非Deployment
- **PVC配置替代hostPath**：必须使用PVC持久卷配置替代hostPath方式，以支持K8S的动态存储分配
- **初始化容器的实现**：对于需要特殊初始化的组件，必须添加适当的初始化容器

这些经验教训为后续组件的K8S适配提供了重要参考。严格遵循这些规范，将大大减少适配过程中的问题和错误。

## 验证清单

### 配置文件验证
- [ ] 所有K8S参数的标签、描述、默认值是否与HDFS一致
- [ ] unit字段是否正确添加到需要的参数上
- [ ] 配置分组是否完整和正确（configCategory、configGroup、configLevel）
- [ ] 端口绑定配置是否完整（bindRole、serviceType、portNumber、nodePort）
- [ ] 自定义配置参数是否为每个配置文件都添加了
- [ ] K8S配置生成器是否包含完整的3个生成器

### 模板文件验证
- [ ] 是否删除了volumePathSet相关配置
- [ ] 资源配置是否使用参数化，没有默认值
- [ ] 是否添加了必要的初始化容器
- [ ] 初始化容器是否包含完整的依赖检查逻辑

### 策略类验证
- [ ] Kerberos日志信息是否修正为正确的组件名称
- [ ] keytab文件路径和下载逻辑是否正确

## 总结

本规范提供了完整的K8S组件适配指南，确保所有组件都能具备与HDFS相同的配置分组和K8S参数适配能力。严格按照本规范执行，可以保证适配质量和用户体验的一致性。

在实施过程中，务必：
1. **完全参照HDFS模式**，不随意修改
2. **确保配置完整性**，不遗漏任何必需配置
3. **遵循模板规范**，支持平台标准功能
4. **进行全面验证**，确保适配质量
