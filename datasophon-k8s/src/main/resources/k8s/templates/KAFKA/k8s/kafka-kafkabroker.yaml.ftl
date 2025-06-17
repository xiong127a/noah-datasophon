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

              echo -e "$BLUE$INFO 开始初始化Kafka配置文件...$NC"
              
              <#if kafkaConfigTempDir?? && kafkaConfigTargetDir??>
              # 挂载配置的临时目录和目标目录
              TMP_CONF_FILE="${kafkaConfigTempDir}"
              TARGET_CONF_DIR="${kafkaConfigTargetDir}"
              <#else>
              # 使用默认路径
              TMP_CONF_FILE="/tmp/kafka-config"
              TARGET_CONF_DIR="/opt/datasophon/kafka-2.4.1/config"
              </#if>
              
              # 创建目标配置目录
              mkdir -p $TARGET_CONF_DIR
              
              # 获取主机名和节点IP
              HOSTNAME=$<#noparse>(hostname -f)</#noparse>
              NODE_IP=$<#noparse>NODE_IP_ADDRESS</#noparse>
              
              # 获取Pod索引从POD_NAME中提取
              POD_INDEX=$<#noparse>(echo $POD_NAME | awk -F'-' '{print $NF}')</#noparse>
              echo -e "$<#noparse>BLUE</#noparse>$<#noparse>INFO</#noparse> 当前Pod索引: $<#noparse>POD_INDEX</#noparse>$<#noparse>NC</#noparse>"
              
              <#if node_port_mappings??>
              <#-- 定义变量来存储提取的端口映射 -->
              <#assign kafka_port_mappings = "">
              <#-- 遍历node_port_mappings数组查找key为9092的项 -->
              <#list node_port_mappings as item>
                <#if item?keys?seq_contains("9092")>
                  <#assign kafka_port_mappings = item["9092"]>
                </#if>
              </#list>
              

              
              # 解析端口映射
              NODE_PORT_MAPPINGS="${kafka_port_mappings}"
              echo -e "$<#noparse>BLUE</#noparse>$<#noparse>INFO</#noparse> 可用端口映射: $<#noparse>NODE_PORT_MAPPINGS</#noparse>$<#noparse>NC</#noparse>"
              
              # 将端口映射分割为数组
              IFS=',' read -ra PORT_ARRAY <<< "$<#noparse>NODE_PORT_MAPPINGS</#noparse>"
              
              # 根据Pod索引选择对应的端口
              PORT_ARRAY_LENGTH=$<#noparse>(echo ${PORT_ARRAY[@]} | wc -w)</#noparse>
              if [ $<#noparse>POD_INDEX</#noparse> -lt $<#noparse>PORT_ARRAY_LENGTH</#noparse> ]; then
                SELECTED_PORT=$<#noparse>{PORT_ARRAY[$POD_INDEX]}</#noparse>
                echo -e "$<#noparse>BLUE</#noparse>$<#noparse>INFO</#noparse> 根据Pod索引选择端口: $<#noparse>SELECTED_PORT</#noparse>$<#noparse>NC</#noparse>"
              else
                SELECTED_PORT=$<#noparse>{PORT_ARRAY[0]}</#noparse>
                echo -e "$<#noparse>YELLOW</#noparse> 警告: Pod索引超出端口映射范围，使用第一个端口: $<#noparse>SELECTED_PORT</#noparse>$<#noparse>NC</#noparse>"
              fi
              <#else>
              # 使用默认端口
              SELECTED_PORT="9092"
              echo -e "$<#noparse>BLUE</#noparse>$<#noparse>INFO</#noparse> 使用默认端口: $<#noparse>SELECTED_PORT</#noparse>$<#noparse>NC</#noparse>"
              </#if>
              
              echo -e "$<#noparse>BLUE</#noparse>$<#noparse>INFO</#noparse> 当前主机名: $<#noparse>HOSTNAME</#noparse>$<#noparse>NC</#noparse>"
              echo -e "$<#noparse>BLUE</#noparse>$<#noparse>INFO</#noparse> K8S节点IP: $<#noparse>NODE_IP</#noparse>$<#noparse>NC</#noparse>"
              
              # 检查临时配置文件
              if [ -f "$<#noparse>TMP_CONF_FILE</#noparse>" ]; then
                echo -e "$<#noparse>BLUE</#noparse>$<#noparse>INFO</#noparse> 找到配置文件: $<#noparse>TMP_CONF_FILE</#noparse>$<#noparse>NC</#noparse>"
                
                # 创建目标配置文件
                TARGET_CONF_FILE="$<#noparse>TARGET_CONF_DIR</#noparse>/server.properties"
                
                # 替换占位符并复制到目标位置
                echo -e "$<#noparse>BLUE</#noparse>$<#noparse>INFO</#noparse> 替换配置文件中的占位符...$<#noparse>NC</#noparse>"
                cat "$<#noparse>TMP_CONF_FILE</#noparse>" | sed "s/\$<#noparse>(hostname)</#noparse>/$<#noparse>NODE_IP</#noparse>/g" | sed "s/:9092/:$<#noparse>SELECTED_PORT</#noparse>/g" > "$<#noparse>TARGET_CONF_FILE</#noparse>"
                
                # 检查advertised.listeners是否存在，如果不存在则添加
                if ! grep -q "advertised.listeners" "$<#noparse>TARGET_CONF_FILE</#noparse>"; then
                  echo -e "$<#noparse>BLUE</#noparse>$<#noparse>INFO</#noparse> advertised.listeners配置不存在，添加默认配置...$<#noparse>NC</#noparse>"
                  echo "advertised.listeners=PLAINTEXT://$<#noparse>NODE_IP</#noparse>:$<#noparse>SELECTED_PORT</#noparse>" >> "$<#noparse>TARGET_CONF_FILE</#noparse>"
                fi
                
                # 验证文件创建成功
                if [ -f "$<#noparse>TARGET_CONF_FILE</#noparse>" ]; then
                  echo -e "$<#noparse>GREEN</#noparse>$<#noparse>CHECK_MARK</#noparse> 成功创建配置文件: $<#noparse>TARGET_CONF_FILE</#noparse>$<#noparse>NC</#noparse>"
                  
                  echo -e "$<#noparse>BLUE</#noparse>$<#noparse>INFO</#noparse> 显示关键配置:$<#noparse>NC</#noparse>"
                  grep -E "advertised.listeners|listeners|zookeeper.connect" "$<#noparse>TARGET_CONF_FILE</#noparse>"
                else
                  echo -e "$<#noparse>RED</#noparse> 错误: 无法创建配置文件: $<#noparse>TARGET_CONF_FILE</#noparse>$<#noparse>NC</#noparse>"
                fi
                
                # 启动Kafka服务
                echo -e "$<#noparse>BLUE</#noparse>$<#noparse>INFO</#noparse> 启动Kafka服务...$<#noparse>NC</#noparse>"
                ${startCommand}
              else
                echo -e "$<#noparse>RED</#noparse> 错误: 找不到配置文件: $<#noparse>TMP_CONF_FILE</#noparse>$<#noparse>NC</#noparse>"
                
                # 保持容器运行以便调试
                echo -e "$<#noparse>BLUE</#noparse>$<#noparse>INFO</#noparse> 保持容器运行，可以使用kubectl exec进入容器调试$<#noparse>NC</#noparse>"
                while true; do
                  sleep 3600
                done
              fi
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
      nodeSelector:
        ${serviceRoleFullName}: "true"
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
