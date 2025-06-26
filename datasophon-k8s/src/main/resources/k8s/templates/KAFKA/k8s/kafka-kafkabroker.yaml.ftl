apiVersion: "apps/v1"
kind: "StatefulSet"
metadata:
  labels:
    name: "${serviceRoleFullName}"
  name: "${serviceRoleFullName}"
  namespace: ${namespace}
spec:
  serviceName: "${serviceRoleFullName}"
  replicas: ${roleNodeCnt}
  selector:
    matchLabels:
      app: "${serviceRoleFullName}"
  updateStrategy:
    type: "RollingUpdate"
    rollingUpdate:
      partition: 0
  podManagementPolicy: Parallel
  minReadySeconds: 5
  revisionHistoryLimit: 10
  template:
    metadata:
      labels:
        name: "${serviceRoleFullName}"
        app: "${serviceRoleFullName}"
        podConflictName: "${serviceRoleFullName}"
      annotations:
        serviceInstanceName: "${serviceName}"
    spec:
      nodeSelector:
        ${serviceRoleFullName}: "true"
      affinity:
        podAntiAffinity:
          requiredDuringSchedulingIgnoredDuringExecution:
            - labelSelector:
                matchLabels:
                  name: "${serviceRoleFullName}"
                  podConflictName: "${serviceRoleFullName}"
              namespaces:
                - "${namespace}"
              topologyKey: "kubernetes.io/hostname"
      hostPID: false
      initContainers:
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
              echo "========== 开始准备Kafka数据目录和权限 =========="
              
              # 1. 设置PVC挂载路径权限
              echo "设置PVC挂载路径权限..."
              chmod -R 777 ${mount_path}
              
              # 2. 准备Kafka数据目录
              <#if kafka_log_dirs??>
              KAFKA_LOG_DIRS="${kafka_log_dirs}"
              <#else>
              KAFKA_LOG_DIRS="${mount_path}/kafka-logs"
              </#if>

              echo "目标数据目录: $KAFKA_LOG_DIRS"

              # 处理多个目录（逗号分隔）
              OLD_IFS="$IFS"
              IFS=","
              for LOG_DIR in $KAFKA_LOG_DIRS; do
                IFS="$OLD_IFS"
                echo "创建目录: $LOG_DIR"
                mkdir -p $LOG_DIR
                chmod -R 777 $LOG_DIR
                chown -R ${runAsUser}:${runAsGroup} $LOG_DIR
                IFS=","
              done
              IFS="$OLD_IFS"
              echo "========== 完成数据目录和权限设置 =========="
          securityContext:
            runAsUser: 0  # 以root用户运行
            privileged: true
          volumeMounts:
            - name: kafka-data
              mountPath: ${mount_path}
              subPathExpr: $(POD_NAMESPACE)/$(POD_NAME)
            - name: "timezone"
              mountPath: "/etc/localtime"

        - name: wait-for-zookeeper
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

              echo -e "$BLUE$INFO 开始检查ZooKeeper集群状态...$NC"

              # 使用从配置中获取的ZooKeeper地址
              <#if zookeeper_connect??>
              ZK_CONNECT="${zookeeper_connect}"
              <#else>
              echo -e "$RED$ERROR 错误: 配置中未提供ZooKeeper地址(zookeeper.connect)，无法继续$NC"
              exit 1
              </#if>

              # 提取ZooKeeper地址（去掉/kafka路径）
              ZK_QUORUM=$(echo "$ZK_CONNECT" | sed 's|/kafka||g')
              echo -e "$BLUE$INFO ZooKeeper地址: $ZK_QUORUM$NC"

              # 分割ZooKeeper地址并检查每个实例
              OLD_IFS="$IFS"
              IFS=","
              ZK_AVAILABLE=0
              ZK_TOTAL=0

              for ZK_SERVER in $ZK_QUORUM; do
                IFS="$OLD_IFS"
                ZK_TOTAL=$((ZK_TOTAL+1))
                IFS=","
              done
              IFS="$OLD_IFS"

              # 计算所需的最小存活数量（过半）
              MIN_AVAILABLE=$(( (ZK_TOTAL + 1) / 2 ))
              echo -e "$BLUE$INFO 需要至少 $MIN_AVAILABLE 个ZooKeeper实例可用（总实例数: $ZK_TOTAL）$NC"

              # 检查每个ZooKeeper实例
              IFS=","
              for ZK_SERVER in $ZK_QUORUM; do
                IFS="$OLD_IFS"
                HOST=$(echo $ZK_SERVER | cut -d':' -f1)
                PORT=$(echo $ZK_SERVER | cut -d':' -f2)
                if [ -z "$PORT" ]; then
                  PORT=2181  # 默认ZooKeeper端口
                fi

                echo -e "$BLUE$INFO 正在检查ZooKeeper服务: $HOST:$PORT$NC"

                # 重试计数器
                RETRIES=0
                MAX_RETRIES=60

                # 循环尝试连接ZooKeeper
                while [ $RETRIES -lt $MAX_RETRIES ]; do
                  if nc -z -w 2 $HOST $PORT; then
                    echo -e "$GREEN$CHECK_MARK ZooKeeper服务 $HOST:$PORT 已就绪$NC"
                    ZK_AVAILABLE=$((ZK_AVAILABLE+1))
                    break
                  else
                    echo -e "$YELLOW$PROGRESS ZooKeeper服务 $HOST:$PORT 未就绪，等待重试... ($((RETRIES+1))/$MAX_RETRIES)$NC"
                    RETRIES=$((RETRIES+1))
                    sleep 2
                  fi
                done

                # 检查是否达到最大重试次数
                if [ $RETRIES -eq $MAX_RETRIES ]; then
                  echo -e "$RED$WARNING ZooKeeper服务 $HOST:$PORT 在$MAX_RETRIES次尝试后仍未就绪$NC"
                fi
              done

              # 检查是否有足够的ZooKeeper实例可用
              echo -e "$BLUE$INFO ZooKeeper可用性: $ZK_AVAILABLE/$ZK_TOTAL$NC"
              if [ $ZK_AVAILABLE -lt $MIN_AVAILABLE ]; then
                echo -e "$RED$ERROR 错误: 可用的ZooKeeper实例数量($ZK_AVAILABLE)小于所需的最小数量($MIN_AVAILABLE)，无法继续初始化Kafka$NC"
                exit 1
              else
                echo -e "$GREEN$CHECK_MARK ZooKeeper集群状态正常，继续初始化Kafka$NC"
              fi
          volumeMounts:
            - name: "timezone"
              mountPath: "/etc/localtime"
      containers:
        - name: "${serviceRoleFullName}"
          image: "${dockerImage}"
          imagePullPolicy: "Always"
          ports:
          <#if node_port_mappings??>
          <#assign mappings = node_port_mappings>
          <#list mappings as item>
            - containerPort: ${(item?keys[0])}
              name: nodeport-${item?index + 1}
          </#list>
          </#if>
          <#if cluster_port_mappings??>
          <#assign mappings = cluster_port_mappings>
          <#list mappings as item>
            - containerPort: ${(item?keys[0])}
              name: clusterport-${item?index + 1}
          </#list>
          </#if>
            - containerPort: ${JMX_PORT}
              name: jmx
          command:
            - "/bin/bash"
            - "-c"
            - |
              # 定义颜色和图标
              RED='\033[0;31m'
              GREEN='\033[0;32m'
              BLUE='\033[1;34m'
              NC='\033[0m' # No Color
              CHECK_MARK="✅"
              INFO="ℹ️"

              echo -e "$BLUE$INFO 开始初始化Kafka配置文件...$NC"
              
              HOSTNAME=$(hostname -f)
              
              # 配置文件路径
              SOURCE_CONF_DIR="/opt/datasophon/datasophon-config/KAFKA/config"
              TARGET_CONF_DIR="${appHome}/config"
              SOURCE_CONF_FILE="${kafkaConfigTempDir}"
              TARGET_CONF_FILE="${kafkaConfigTargetDir}/server.properties"
              
              echo -e "$BLUE$INFO 当前主机名: $HOSTNAME$NC"
              
              # 创建并准备配置
              if [ -f "$SOURCE_CONF_FILE" ]; then
                echo -e "$BLUE$INFO 找到源配置文件，开始准备最终配置..."
                
                # 确保目标目录存在
                mkdir -p "$TARGET_CONF_DIR"
                
                # 复制并替换占位符
                cat "$SOURCE_CONF_FILE" > "$TARGET_CONF_FILE"
                
                # 替换hostname占位符为Pod的FQDN
                sed -i "s/\$(hostname)/$HOSTNAME/g" "$TARGET_CONF_FILE"
                
                # 验证文件创建成功
                if [ -f "$TARGET_CONF_FILE" ]; then
                  echo -e "$GREEN$CHECK_MARK 成功创建配置文件: $TARGET_CONF_FILE$NC"
                  
                  echo -e "$BLUE$INFO 显示关键配置:$NC"
                  grep -E "advertised.listeners|listeners|zookeeper.connect" "$TARGET_CONF_FILE"
                else
                  echo -e "$RED 错误: 无法创建配置文件 $TARGET_CONF_FILE$NC"
                  exit 1
                fi
              else
                echo -e "$YELLOW 警告: 源配置文件 $SOURCE_CONF_FILE 不存在，Kafka将使用默认配置$NC"
              fi

              # 设置JMX RMI的主机名，以便远程访问
              # 注意：JMX_PORT 和其他JMX参数已通过环境变量注入
              export KAFKA_OPTS="$KAFKA_OPTS -Djava.rmi.server.hostname=$HOSTNAME"
              
              # 启动Kafka服务
              echo -e "$BLUE$INFO 启动Kafka服务...$NC"
              ${startCommand}
          env:
            - name: JMX_PORT
              value: ${JMX_PORT}
            - name: USER
              value: ${runAsUser}
            - name: MEM_LIMIT
              valueFrom:
                resourceFieldRef:
                  resource: limits.memory
            - name: POD_NAME
              valueFrom:
                fieldRef:
                  fieldPath: metadata.name
            - name: POD_NAMESPACE
              valueFrom:
                fieldRef:
                  fieldPath: metadata.namespace
            - name: HOSTNAME
              valueFrom:
                fieldRef:
                  fieldPath: spec.nodeName
            - name: NODE_IP_ADDRESS
              valueFrom:
                fieldRef:
                  fieldPath: status.hostIP
          readinessProbe:
            exec:
              command:
                - "/bin/bash"
                - "-c"
                - "${statusCommand}"
            failureThreshold: 3
            initialDelaySeconds: 3
            periodSeconds: 30
            successThreshold: 1
            timeoutSeconds: 15
          resources:
            requests:
              memory: ${requests_memory}
              cpu: ${requests_cpu}
            limits:
              memory: ${limits_memory}
              cpu: ${limits_cpu}
          securityContext:
            privileged: true
          volumeMounts:
            <#list volumeConfigMapSet as item>
            - name: "${item.name}"
              mountPath: "${item.value}"
              subPath: "${item.fileName}"
            </#list>
            - name: kafka-data
              mountPath: ${mount_path}
              subPathExpr: $(POD_NAMESPACE)/$(POD_NAME)
            - name: "timezone"
              mountPath: "/etc/localtime"
      terminationGracePeriodSeconds: 30
      volumes:
        <#list volumeConfigMapSet as item>
        - name: "${item.name}"
          configMap:
            name: "${item.name}"
        </#list>
        - name: kafka-data
          persistentVolumeClaim:
            claimName: "${serviceRoleFullName}-pvc"
        - name: "timezone"
          hostPath:
            path: "/etc/localtime"
