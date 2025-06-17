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
              echo "========== 开始准备EFAK数据目录和权限 =========="
              
              # 1. 设置PVC挂载路径权限
              echo "设置PVC挂载路径权限..."
              chmod -R 777 ${mount_path}
              
              # 2. 准备EFAK数据目录
              EFAK_DATA_DIR="${mount_path}/efak-data"
              echo "目标数据目录: $EFAK_DATA_DIR"
              
              mkdir -p $EFAK_DATA_DIR
              chmod -R 777 $EFAK_DATA_DIR
              chown -R ${runAsUser}:${runAsGroup} $EFAK_DATA_DIR
              
              # 3. 准备ConfigMap挂载目录
              echo "开始检查和创建ConfigMap挂载路径..."
              <#list volumeConfigMapSet as item>
              MOUNT_PATH="${item.value}"
              # 提取目录部分
              DIR_PATH=$(dirname "$MOUNT_PATH")
              echo "检查挂载目录: $DIR_PATH"
              if [ ! -d "$DIR_PATH" ]; then
                echo "创建目录: $DIR_PATH"
                mkdir -p "$DIR_PATH"
                chmod -R 755 "$DIR_PATH"
                chown -R ${runAsUser}:${runAsGroup} "$DIR_PATH"
              fi
              </#list>
              
              echo "========== 完成数据目录和权限设置 =========="
          securityContext:
            runAsUser: 0  # 以root用户运行
            privileged: true
          volumeMounts:
            - name: efak-data
              mountPath: ${mount_path}
              subPathExpr: $(POD_NAMESPACE)/$(POD_NAME)
            - name: "timezone"
              mountPath: "/etc/localtime"

        - name: wait-for-zookeeper-and-kafka
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

              echo -e "$BLUE$INFO 开始检查ZooKeeper集群状态（EFAK依赖）...$NC"

              # 使用从配置中获取的ZooKeeper地址
              <#if cluster1ZkList??>
              ZK_CONNECT="${cluster1ZkList}"
              <#else>
              echo -e "$RED$ERROR 错误: 配置中未提供ZooKeeper地址(cluster1.zk.list)，无法继续$NC"
              exit 1
              </#if>

              echo -e "$BLUE$INFO ZooKeeper地址: $ZK_CONNECT$NC"

              # 分割ZooKeeper地址并检查每个实例
              OLD_IFS="$IFS"
              IFS=","
              ZK_AVAILABLE=0
              ZK_TOTAL=0

              for ZK_SERVER in $ZK_CONNECT; do
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
              for ZK_SERVER in $ZK_CONNECT; do
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
                echo -e "$RED$ERROR 错误: 可用的ZooKeeper实例数量($ZK_AVAILABLE)小于所需的最小数量($MIN_AVAILABLE)，无法继续初始化EFAK$NC"
                exit 1
              else
                echo -e "$GREEN$CHECK_MARK ZooKeeper集群状态正常$NC"
              fi
              
              # 检查Kafka集群状态
              echo -e "$BLUE$INFO 开始检查Kafka集群状态（EFAK依赖）...$NC"
              
              # 检查Kafka服务
              KAFKA_SERVICE="kafka-kafkabroker"
              KAFKA_PORT=9092
              KAFKA_NAMESPACE="datasophon"
              
              echo -e "$BLUE$INFO 正在检查Kafka服务: $KAFKA_SERVICE.$KAFKA_NAMESPACE.svc.cluster.local:$KAFKA_PORT$NC"
              
              # 重试计数器
              RETRIES=0
              MAX_RETRIES=60
              
              # 循环尝试连接Kafka
              while [ $RETRIES -lt $MAX_RETRIES ]; do
                if nc -z -w 2 $KAFKA_SERVICE.$KAFKA_NAMESPACE.svc.cluster.local $KAFKA_PORT; then
                  echo -e "$GREEN$CHECK_MARK Kafka服务已就绪$NC"
                  break
                else
                  echo -e "$YELLOW$PROGRESS Kafka服务未就绪，等待重试... ($((RETRIES+1))/$MAX_RETRIES)$NC"
                  RETRIES=$((RETRIES+1))
                  sleep 2
                fi
              done
              
              # 检查是否达到最大重试次数
              if [ $RETRIES -eq $MAX_RETRIES ]; then
                echo -e "$RED$ERROR 错误: Kafka服务在$MAX_RETRIES次尝试后仍未就绪，无法继续初始化EFAK$NC"
                exit 1
              fi
              
              echo -e "$GREEN$CHECK_MARK 所有依赖服务已就绪，可以继续初始化EFAK$NC"
          volumeMounts:
            <#list volumeConfigMapSet as item>
            - name: "${item.name}"
              mountPath: "${item.value}"
              <#if item.fileName?? && item.fileName != "">
              subPath: "${item.fileName}"
              </#if>
            </#list>
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
          command:
            - "/bin/bash"
            - "-c"
            - |
              # 定义颜色和图标
              RED='\033[0;31m'
              GREEN='\033[0;32m'
              BLUE='\033[0;34m'
              NC='\033[0m' # No Color
              CHECK_MARK="✅"
              INFO="ℹ️"

              echo -e "$BLUE$INFO 开始初始化EFAK配置文件...$NC"
              
              <#if kafkaConfigTempDir?? && kafkaConfigTargetDir??>
              # 挂载配置的临时目录和目标目录
              TMP_CONF_DIR="${kafkaConfigTempDir}"
              TARGET_CONF_DIR="${kafkaConfigTargetDir}"
              <#else>
              # 使用默认路径
              TMP_CONF_DIR="/tmp/efak-config"
              TARGET_CONF_DIR="/opt/datasophon/kafka-2.4.1/efak/conf"
              </#if>
              
              # 创建目标配置目录
              mkdir -p $TARGET_CONF_DIR
              
              # 获取主机名
              HOSTNAME=$(hostname -f)
              echo -e "$BLUE$INFO 当前主机名: $HOSTNAME$NC"
              
              # 处理所有配置文件
              for CONF_FILE in $TMP_CONF_DIR/*; do
                if [ -f "$CONF_FILE" ]; then
                  FILENAME=$(basename "$CONF_FILE")
                  # 替换##HOSTNAME##占位符为实际的主机名
                  sed -i "s/##HOSTNAME##/$HOSTNAME/g" "$CONF_FILE"
                  # 复制到目标目录
                  cp -f "$CONF_FILE" "$TARGET_CONF_DIR/"
                fi
              done
              
              echo -e "$GREEN$CHECK_MARK EFAK配置文件初始化完成$NC"
              
              # 启动EFAK服务
              echo -e "$BLUE$INFO 启动EFAK服务...$NC"
              ${startCommand}
          env:
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
              <#if item.fileName?? && item.fileName != "">
              subPath: "${item.fileName}"
              </#if>
            </#list>
            - name: efak-data
              mountPath: ${mount_path}
              subPathExpr: $(POD_NAMESPACE)/$(POD_NAME)
            - name: "timezone"
              mountPath: "/etc/localtime"
      nodeSelector:
        ${serviceRoleFullName}: "true"
      terminationGracePeriodSeconds: 30
      volumes:
        <#list volumeConfigMapSet as item>
        - name: "${item.name}"
          configMap:
            name: "${item.name}"
        </#list>
        - name: efak-data
          persistentVolumeClaim:
            claimName: "${serviceRoleFullName}-pvc"
        - name: "timezone"
          hostPath:
            path: "/etc/localtime"
