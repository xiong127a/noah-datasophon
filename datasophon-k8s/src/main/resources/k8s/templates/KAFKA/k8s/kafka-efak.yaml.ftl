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
              subPathExpr: $(POD_NAME)
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
              BLUE='\033[0;34m'
              YELLOW='\033[1;33m'
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

              # 提取ZooKeeper地址（去掉znode路径）
              ZK_QUORUM=$(echo "$ZK_CONNECT" | sed 's|/.*||g')
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

                # 如果已有足够 ZK 实例，则提前退出
                if [ $ZK_AVAILABLE -ge $MIN_AVAILABLE ]; then
                  echo -e "$GREEN$CHECK_MARK 已有足够数量($ZK_AVAILABLE/$ZK_TOTAL)的ZooKeeper实例可用，提前结束检查$NC"
                  break
                fi

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
              YELLOW='\033[1;33m'
              NC='\033[0m' # No Color
              CHECK_MARK="✅"
              INFO="ℹ️"
              ERROR="❌"
              WARNING="⚠️"

              echo -e "$BLUE$INFO 开始初始化EFAK配置文件...$NC"
              
              # 处理hosts文件，将主机hosts文件内容与Pod DNS配置合并
              echo -e "$BLUE$INFO ========== 开始处理hosts文件 ==========$NC"
              
              # 备份原始hosts文件
              cp /etc/hosts /tmp/original_hosts
              echo -e "$BLUE$INFO 已备份原始hosts文件到/tmp/original_hosts$NC"
              
              # 从主机hosts文件中提取有用条目 (通常是自定义的主机映射)
              if [ -f /tmp/host_etc_hosts ]; then
                echo -e "$BLUE$INFO 从主机hosts文件提取有用条目...$NC"
                grep -v "^127.0.0.1" /tmp/host_etc_hosts | grep -v "^::1" | grep -v "^#" >> /tmp/original_hosts
              else
                echo -e "$YELLOW$WARNING 警告: 主机hosts文件未找到$NC"
              fi
              
              # 应用合并后的hosts文件
              cat /tmp/original_hosts > /etc/hosts
              
              echo -e "$BLUE$INFO 最终hosts文件内容:$NC"
              cat /etc/hosts
              
              echo -e "$BLUE$INFO 测试主机名解析:$NC"
              echo -e "$BLUE$INFO hostname: $(hostname)$NC"
              echo -e "$BLUE$INFO hostname -f: $(hostname -f 2>/dev/null || echo '无法获取FQDN')$NC"
              
              echo -e "$GREEN$CHECK_MARK ========== hosts文件处理完成 ==========$NC"
              
              # 创建目标配置目录
              mkdir -p $TARGET_CONF_DIR
              
              # 获取主机名
              HOSTNAME=$(hostname -f)
              echo -e "$BLUE$INFO 当前主机名: $HOSTNAME$NC"
              
              # 处理包含占位符的配置文件
              <#if example_config_files?? && (example_config_files?size > 0)>
              echo -e "$BLUE$INFO 开始处理配置文件模板...$NC"
              echo -e "$BLUE$INFO 当前Pod的FQDN: $HOSTNAME$NC"
              echo -e "$BLUE$INFO 当前Pod的IP: $NODE_IP_ADDRESS$NC"
              
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
                sed "s/\\\$(hostname)/$HOSTNAME/g" "$SOURCE_PATH" > "$TARGET_PATH"
                # 替换所有{{HOST}}为Pod的FQDN
                sed -i "s/{{HOST}}/$HOSTNAME/g" "$TARGET_PATH"
                # 替换所有{{IP}}为Pod的IP
                sed -i "s/{{IP}}/$NODE_IP_ADDRESS/g" "$TARGET_PATH"
                # 替换所有${r"${hostname}"}为Pod的FQDN
                sed -i "s/${r"${hostname}"}/$HOSTNAME/g" "$TARGET_PATH"
                # 替换所有##HOSTNAME##为Pod的FQDN (兼容旧格式)
                sed -i "s/##HOSTNAME##/$HOSTNAME/g" "$TARGET_PATH"
                
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
              
              # 处理常规配置文件（兼容旧逻辑）
              for CONF_FILE in $TMP_CONF_DIR/*; do
                if [ -f "$CONF_FILE" ]; then
                  FILENAME=$(basename "$CONF_FILE")
                  # 跳过已经通过example_config_files处理过的文件
                  if [[ "$CONF_FILE" == *".example" ]]; then
                    echo -e "$BLUE$INFO 跳过已处理的模板文件: $FILENAME$NC"
                    continue
                  fi
                  echo -e "$BLUE$INFO 处理常规配置文件: $FILENAME$NC"
                  # 替换##HOSTNAME##占位符为实际的主机名
                  sed -i "s/##HOSTNAME##/$HOSTNAME/g" "$CONF_FILE"
                  # 替换$(hostname)占位符为实际的主机名
                  sed -i "s/\\\$(hostname)/$HOSTNAME/g" "$CONF_FILE"
                  # 替换{{HOST}}占位符为实际的主机名
                  sed -i "s/{{HOST}}/$HOSTNAME/g" "$CONF_FILE"
                  # 替换{{IP}}占位符为实际的IP
                  sed -i "s/{{IP}}/$NODE_IP_ADDRESS/g" "$CONF_FILE"
                  # 替换${r"${hostname}"}占位符为实际的主机名
                  sed -i "s/${r"${hostname}"}/$HOSTNAME/g" "$CONF_FILE"
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
              subPathExpr: $(POD_NAME)
            - name: "timezone"
              mountPath: "/etc/localtime"
            - name: "hosts-file"
              mountPath: "/tmp/host_etc_hosts"
      terminationGracePeriodSeconds: 30
      volumes:
        <#list volumeConfigMapSet as item>
        - name: "${item.name}"
          configMap:
            name: "${item.name}"
        </#list>
        - name: efak-data
          persistentVolumeClaim:
            claimName: "${serviceRoleFullName}"
        - name: "timezone"
          hostPath:
            path: "/etc/localtime"
        - name: "hosts-file"
          hostPath:
            path: "/etc/hosts"
