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
              subPathExpr: $(POD_NAME)
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
          <#if load_balancer_port_mappings??>
          <#assign mappings = load_balancer_port_mappings>
          <#list mappings as item>
            - containerPort: ${(item?keys[0])}
              name: loadbalancer-${item?index + 1}
          </#list>
          </#if>
            - containerPort: ${JMX_PORT}
              name: jmx
          command:
            - "/bin/bash"
            - "-c"
            - |
              RED='\033[0;31m'
              GREEN='\033[0;32m'
              BLUE='\033[1;34m'
              NC='\033[0m' # No Color
              CHECK_MARK="✅"
              INFO="ℹ️"

              echo -e "$BLUE$INFO 开始启动Kafka服务...$NC"
              
              FQDN=$(hostname -f)
              echo -e "$BLUE$INFO 当前主机名: $FQDN$NC"
              
              # 获取外部IP地址
              echo -e "$BLUE$INFO 获取外部IP地址...$NC"
              if [ -f "/etc/kafka-external-ip/$POD_NAME" ]; then
                EXTERNAL_IP=$(cat /etc/kafka-external-ip/$POD_NAME)
                echo -e "$GREEN$CHECK_MARK 获取到外部IP: $EXTERNAL_IP$NC"
              else
                echo -e "$RED$ERROR 未找到外部IP配置$NC"
              fi
              
              # 处理包含占位符的配置文件
              <#if example_config_files?? && (example_config_files?size > 0)>
              echo -e "$BLUE$INFO 开始处理配置文件模板...$NC"
              echo -e "$BLUE$INFO 当前Pod的FQDN: $FQDN$NC"
              echo -e "$BLUE$INFO 当前Pod的IP: $NODE_IP_ADDRESS$NC"
              echo -e "$BLUE$INFO 当前external的IP: $EXTERNAL_IP$NC"
              
              <#list example_config_files as exampleFilePath>
              # 确定源文件和目标文件路径
              SOURCE_PATH="${exampleFilePath}"
              TARGET_PATH="${exampleFilePath?remove_ending('.example')}"
              FILE_NAME="$(basename "$TARGET_PATH")"
              
              echo -e "$BLUE$INFO 处理配置文件: $FILE_NAME$NC"
              echo -e "$BLUE$INFO 源文件: $SOURCE_PATH$NC"
              echo -e "$BLUE$INFO 目标文件: $TARGET_PATH$NC"
              
              # 确保目标目录存在
              mkdir -p $(dirname "$TARGET_PATH")
              
              # 替换占位符并写入目标文件
              if [ -f "$SOURCE_PATH" ]; then
                echo -e "$BLUE$INFO 正在替换 $SOURCE_PATH 中的占位符...$NC"
                # 替换所有$(hostname)为Pod的FQDN
                sed "s/\\\$(hostname)/$FQDN/g" "$SOURCE_PATH" > "$TARGET_PATH"
                # 替换所有{{HOST}}为Pod的FQDN
                sed -i "s/{{HOST}}/$FQDN/g" "$TARGET_PATH"
                # 替换所有{{IP}}为Pod的IP
                sed -i "s/{{IP}}/$NODE_IP_ADDRESS/g" "$TARGET_PATH"
                # 替换所有{{EXTERNAL_IP}}为Pod的EXTERNAL_IP
                sed -i "s/{{EXTERNAL_IP}}/$EXTERNAL_IP/g" "$TARGET_PATH"
                # 替换所有${r"${hostname}"}为Pod的FQDN
                sed -i "s/${r"${hostname}"}/$FQDN/g" "$TARGET_PATH"
                
                # 检查文件是否创建成功
                if [ -f "$TARGET_PATH" ]; then
                  echo -e "$GREEN$CHECK_MARK 配置文件 $FILE_NAME 处理成功!$NC"
                  echo -e "$BLUE$INFO 文件权限设置中...$NC"
                  chmod 644 "$TARGET_PATH"
                  chown ${runAsUser}:${runAsGroup} "$TARGET_PATH"
                  echo -e "$GREEN$CHECK_MARK 文件权限设置完成!$NC"
                else
                  echo -e "$RED$ERROR 配置文件 $FILE_NAME 处理失败: 目标文件未创建$NC"
                  exit 1
                fi
              else
                echo -e "$RED$ERROR 源文件 $SOURCE_PATH 不存在!$NC"
                exit 1
              fi
              </#list>
              
              echo -e "$GREEN$CHECK_MARK 所有配置文件处理完成!$NC"
              </#if>
              
              # 设置JMX RMI的主机名，以便远程访问
              # 注意：JMX_PORT 和其他JMX参数已通过环境变量注入
              export KAFKA_OPTS="$KAFKA_OPTS -Djava.rmi.server.hostname=$FQDN"
              
              
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
            - name: FQDN
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
            - name: kafka-external-ip
              mountPath: "/etc/kafka-external-ip"
            - name: kafka-data
              mountPath: ${mount_path}
              subPathExpr: $(POD_NAME)
            - name: "timezone"
              mountPath: "/etc/localtime"
      terminationGracePeriodSeconds: 30
      volumes:
        <#list volumeConfigMapSet as item>
        - name: "${item.name}"
          configMap:
            name: "${item.name}"
        </#list>
        - name: kafka-external-ip
          configMap:
            name: "${serviceRoleFullName}-external"
        - name: kafka-data
          persistentVolumeClaim:
            claimName: "${serviceRoleFullName}"
        - name: "timezone"
          hostPath:
            path: "/etc/localtime"
