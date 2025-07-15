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
        service.kubernetes.io/headless: "true"
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
      hostNetwork: false
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
              echo "========== 开始准备NameNode数据目录和权限 =========="
              
              # 1. 设置PVC挂载路径权限
              echo "设置PVC挂载路径权限..."
              chmod -R 777 ${mount_path}
              
              # 2. 准备NameNode数据目录
              echo "目标数据目录: ${nn_name_dir}"
              mkdir -p ${nn_name_dir}
              
              # 3. 设置权限
              echo "设置目录权限和所有权..."
              chmod -R 777 ${nn_name_dir}
              chown -R ${runAsUser}:${runAsGroup} ${nn_name_dir}  # 使用变量代替硬编码的用户和组
              
              # 4. 验证
              echo "验证目录和权限:"
              ls -la ${nn_name_dir}
              
              echo "========== 完成数据目录和权限设置 =========="
          securityContext:
            runAsUser: 0  # 以root用户运行
            privileged: true
          volumeMounts:
            - name: namenode-data
              mountPath: ${mount_path}
              subPathExpr: $(POD_NAME)
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
              <#if zkQuorum??>
              ZK_QUORUM="${zkQuorum}"
              <#else>
              echo -e "$RED$ERROR 错误: 配置中未提供ZooKeeper地址(ha.zookeeper.quorum)，无法继续$NC"
              exit 1
              </#if>
              
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
                echo -e "$RED$ERROR 错误: 可用的ZooKeeper实例数量($ZK_AVAILABLE)小于所需的最小数量($MIN_AVAILABLE)，无法继续初始化HDFS$NC"
                exit 1
              else
                echo -e "$GREEN$CHECK_MARK ZooKeeper集群状态正常，继续初始化HDFS$NC"
              fi
          volumeMounts:
            <#list volumeConfigMapSet as item>
            - name: "${item.name}"
              mountPath: "${item.value}"
              subPath: "${item.fileName}"
            </#list>
            - name: "timezone"
              mountPath: "/etc/localtime"
        - name: wait-for-journalnodes
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
              
              echo -e "$BLUE$INFO 开始检查JournalNode集群状态...$NC"
              
              # 获取JournalNode服务端点
              <#if nn_shared_edits_dir??>
              JOURNAL_ENDPOINTS=$(echo "${nn_shared_edits_dir}" | sed -r 's|qjournal://([^/]+)/.*|\1|g')
              <#else>
              echo -e "$YELLOW$WARNING dfs.namenode.shared.edits.dir 未定义，使用默认值$NC"
              JOURNAL_ENDPOINTS="journalnode-0.journalnode.default.svc.cluster.local:8485;journalnode-1.journalnode.default.svc.cluster.local:8485;journalnode-2.journalnode.default.svc.cluster.local:8485"
              </#if>
              echo -e "$BLUE$INFO JournalNode端点: $JOURNAL_ENDPOINTS$NC"
              
              # 使用ash兼容的方式分割字符串
              OLD_IFS="$IFS"
              IFS=";"
              JOURNAL_AVAILABLE=0
              JOURNAL_TOTAL=0
              
              for NODE in $JOURNAL_ENDPOINTS; do
                IFS="$OLD_IFS"
                JOURNAL_TOTAL=$((JOURNAL_TOTAL+1))
                IFS=";"
              done
              IFS="$OLD_IFS"
              
              # 计算所需的最小存活数量（过半）
              MIN_AVAILABLE=$(( (JOURNAL_TOTAL + 1) / 2 ))
              echo -e "$BLUE$INFO 需要至少 $MIN_AVAILABLE 个JournalNode实例可用（总实例数: $JOURNAL_TOTAL）$NC"
              
              # 检查每个JournalNode实例
              IFS=";"
              for NODE in $JOURNAL_ENDPOINTS; do
                IFS="$OLD_IFS"
                HOST=$(echo $NODE | cut -d':' -f1)
                PORT=$(echo $NODE | cut -d':' -f2)
                echo -e "$BLUE$INFO 正在检查JournalNode: $HOST:$PORT$NC"
                
                # 重试计数器
                RETRIES=0
                MAX_RETRIES=90
                
                # 循环尝试连接JournalNode
                while [ $RETRIES -lt $MAX_RETRIES ]; do
                  if nc -z $HOST $PORT; then
                    echo -e "$GREEN$CHECK_MARK JournalNode $HOST:$PORT 已就绪$NC"
                    JOURNAL_AVAILABLE=$((JOURNAL_AVAILABLE+1))
                    break
                  else
                    echo -e "$YELLOW$PROGRESS JournalNode $HOST:$PORT 未就绪，等待重试... ($((RETRIES+1))/$MAX_RETRIES)$NC"
                    RETRIES=$((RETRIES+1))
                    sleep 2
                  fi
                done
                
                # 检查是否达到最大重试次数
                if [ $RETRIES -eq $MAX_RETRIES ]; then
                  echo -e "$RED$WARNING JournalNode $HOST:$PORT 在$MAX_RETRIES次尝试后仍未就绪$NC"
                fi
              done
              
              # 检查是否有足够的JournalNode实例可用
              echo -e "$BLUE$INFO JournalNode可用性: $JOURNAL_AVAILABLE/$JOURNAL_TOTAL$NC"
              if [ $JOURNAL_AVAILABLE -lt $MIN_AVAILABLE ]; then
                echo -e "$RED$ERROR 错误: 可用的JournalNode实例数量($JOURNAL_AVAILABLE)小于所需的最小数量($MIN_AVAILABLE)，无法继续初始化NameNode$NC"
                exit 1
              else
                echo -e "$GREEN$CHECK_MARK JournalNode集群状态正常，继续初始化NameNode$NC"
              fi
        # NameNode格式化/同步初始化容器 - 每次启动时都会检查，确保幂等性
        - name: namenode-format
          image: "${dockerImage}"
          env:
            - name: USER
              value: "${runAsUser}"
            - name: NN_NAME_DIR
              value: "${nn_name_dir}"
            - name: POD_NAME
              valueFrom:
                fieldRef:
                  fieldPath: metadata.name
            - name: POD_NAMESPACE
              valueFrom:
                fieldRef:
                  fieldPath: metadata.namespace
          command:
            - "/bin/bash"
            - "-c"
            - |
              echo "========== 开始NameNode格式化/同步检查 =========="
              
              # 检查NameNode是否已经格式化
              if [ -f "${nn_name_dir}/current/VERSION" ]; then
                echo "✅ NameNode数据目录已存在，跳过格式化/同步。"
                exit 0
              fi
              
              echo "ℹ️ NameNode数据目录不存在，开始执行首次初始化..."
              
              # 从Pod名称确定NameNode ID和角色
              POD_INDEX=$(echo $POD_NAME | awk -F'-' '{print $NF}')
              
              # 根据Pod索引确定NameNode ID (0对应nn1，1对应nn2)
              if [ "$POD_INDEX" == "0" ]; then
                NAMENODE_ID="nn1"
                NAMENODE_ROLE="active"
                echo "当前Pod是第一个NameNode (index=$POD_INDEX)，角色: $NAMENODE_ROLE"
                
                # 设置Kerberos（如果启用）
                if ${enableKerberos}; then
                  echo "设置Kerberos..."
                  FQDN=$(hostname)
                  if [ ! -f /etc/security/keytab/keystore ]; then
                    cd /opt/datasophon/script && sh keystore.sh $FQDN
                  fi
                  if [ ! -f ${appHome}/etc/hadoop/ssl-client.xml ]; then
                    cp ${appHome}/etc/hadoop/ssl-client.xml.template ${appHome}/etc/hadoop/ssl-client.xml
                  fi
                  if [ ! -f ${appHome}/etc/hadoop/ssl-server.xml ]; then
                    cp ${appHome}/etc/hadoop/ssl-server.xml.template ${appHome}/etc/hadoop/ssl-server.xml
                  fi
                fi
                
                # 设置Ranger插件（如果启用）
                if ${enableRangerPlugin}; then
                  echo "设置Ranger插件..."
                  cd ${appHome}/ranger-hdfs-plugin && \
                  sh ${appHome}/ranger-hdfs-plugin/enable-hdfs-plugin.sh
                fi
                
                # 执行NameNode格式化
                echo "格式化主NameNode (nn1)..."
                set -x  # 启用命令跟踪，便于调试
                su - ${runAsUser} -c "${appHome}/bin/hdfs namenode -format -nonInteractive -force ${nameServiceId}"
                FORMAT_RESULT=$?
                set +x  # 关闭命令跟踪
                
                if [ $FORMAT_RESULT -eq 0 ]; then
                  echo "✅ NameNode格式化成功"
                else
                  echo "❌ NameNode格式化失败，错误码: $FORMAT_RESULT"
                  echo "警告：格式化失败，但允许Pod继续启动"
                fi
              else
                NAMENODE_ID="nn2"
                NAMENODE_ROLE="standby"
                echo "当前Pod是备用NameNode (index=$POD_INDEX)，角色: $NAMENODE_ROLE"
                
                # 设置Kerberos（如果启用）
                if ${enableKerberos}; then
                  echo "设置Kerberos..."
                  FQDN=$(hostname)
                  if [ ! -f /etc/security/keytab/keystore ]; then
                    cd /opt/datasophon/script && sh keystore.sh $FQDN
                  fi
                  if [ ! -f ${appHome}/etc/hadoop/ssl-client.xml ]; then
                    cp ${appHome}/etc/hadoop/ssl-client.xml.template ${appHome}/etc/hadoop/ssl-client.xml
                  fi
                  if [ ! -f ${appHome}/etc/hadoop/ssl-server.xml ]; then
                    cp ${appHome}/etc/hadoop/ssl-server.xml.template ${appHome}/etc/hadoop/ssl-server.xml
                  fi
                fi
                
                # 设置Ranger插件（如果启用）
                if ${enableRangerPlugin}; then
                  echo "设置Ranger插件..."
                  cd ${appHome}/ranger-hdfs-plugin && \
                  sh ${appHome}/ranger-hdfs-plugin/enable-hdfs-plugin.sh
                fi
                
                # 执行备用NameNode同步
                echo "同步备用NameNode (nn2) 元数据..."
                set -x  # 启用命令跟踪，便于调试
                su - ${runAsUser} -c "${appHome}/bin/hdfs namenode -bootstrapStandby -nonInteractive -force"
                BOOTSTRAP_RESULT=$?
                set +x  # 关闭命令跟踪
                
                if [ $BOOTSTRAP_RESULT -eq 0 ]; then
                  echo "✅ 备用NameNode同步成功"
                else
                  echo "❌ 备用NameNode同步失败，错误码: $BOOTSTRAP_RESULT"
                  echo "警告：同步失败，但允许Pod继续启动"
                fi
              fi
              
              echo "========== 完成NameNode格式化/同步操作 =========="
          volumeMounts:
            - name: namenode-data
              mountPath: ${mount_path}
              subPathExpr: $(POD_NAME)
            <#list volumeConfigMapSet as item>
            - name: "${item.name}"
              mountPath: "${item.value}"
              subPath: "${item.fileName}"
            </#list>
            - name: "timezone"
              mountPath: "/etc/localtime"
        # ZKFC格式化初始化容器 - 每次启动时都会检查，确保幂等性
        # 注意：Pod索引检查在容器内部进行
        - name: zkfc-format
          image: "${dockerImage}"
          env:
            - name: USER
              value: "${runAsUser}"
            - name: ZK_QUORUM
              value: "${zkQuorum}"
            - name: NAME_SERVICE_ID
              value: "${nameServiceId}"
            - name: POD_NAME
              valueFrom:
                fieldRef:
                  fieldPath: metadata.name
            - name: POD_NAMESPACE
              valueFrom:
                fieldRef:
                  fieldPath: metadata.namespace
          command:
            - "/bin/bash"
            - "-c"
            - |
              echo "========== 开始ZKFC格式化检查 =========="
              
              # 从Pod名称确定NameNode ID和角色
              POD_INDEX=$(echo $POD_NAME | awk -F'-' '{print $NF}')
              
              # 只在第一个NameNode（index=0）上执行检查和格式化
              if [ "$POD_INDEX" != "0" ]; then
                echo "ℹ️ 当前Pod不是第一个NameNode (index=$POD_INDEX)，跳过ZKFC格式化检查。"
                exit 0
              fi
              
              ZK_HA_PATH="/hadoop-ha/${nameServiceId}"
              echo "检查ZooKeeper HA路径: $ZK_HA_PATH..."
              
              # 使用Hadoop自带的ZK客户端检查znode是否存在
              hdfs org.apache.zookeeper.ZooKeeperMain -server $ZK_QUORUM stat $ZK_HA_PATH > /dev/null 2>&1
              
              if [ $? -eq 0 ]; then
                echo "✅ ZKFC HA路径已存在，跳过格式化。"
                exit 0
              fi
              
              echo "ℹ️ ZKFC HA路径不存在，开始执行ZKFC格式化..."
              
              # 添加Kerberos相关配置
              if ${enableKerberos}; then
                echo "ℹ️ Kerberos已启用，设置Kerberos配置...";
                FQDN=$(hostname)
                if [ ! -f /etc/security/keytab/keystore ]; then
                  cd /opt/datasophon/script && sh keystore.sh $FQDN
                fi
                if [ ! -f ${appHome}/etc/hadoop/ssl-client.xml ]; then
                  cp ${appHome}/etc/hadoop/ssl-client.xml.template ${appHome}/etc/hadoop/ssl-client.xml
                fi
                if [ ! -f ${appHome}/etc/hadoop/ssl-server.xml ]; then
                  cp ${appHome}/etc/hadoop/ssl-server.xml.template ${appHome}/etc/hadoop/ssl-server.xml
                fi
                # 执行Kerberos身份验证
                su - ${runAsUser} -c "kinit -kt /etc/security/keytab/nn.service.keytab nn/$FQDN@HADOOP.COM"
              fi
              
              # 执行ZKFC格式化
              set -x  # 启用命令跟踪
              FORMAT_CMD="${appHome}/bin/hdfs zkfc -formatZK -force"
              echo "执行命令: $FORMAT_CMD"
              # 使用 -force 参数强制格式化，避免交互式提示
              su - ${runAsUser} -c "cd ${appHome} && $FORMAT_CMD"
              FORMAT_RESULT=$?
              set +x  # 关闭命令跟踪
              
              # 检查格式化结果
              if [ $FORMAT_RESULT -eq 0 ]; then
                echo "✅ ZKFC格式化成功"
              else
                echo "❌ ZKFC格式化失败，错误码: $FORMAT_RESULT"
                # 允许Pod继续启动以进行调试
                echo "⚠️ ZKFC格式化失败，但允许Pod继续启动。"
              fi
              
              echo "========== ZKFC格式化步骤完成 =========="
          volumeMounts:
            - name: namenode-data
              mountPath: ${mount_path}
              subPathExpr: $(POD_NAME)
            <#list volumeConfigMapSet as item>
            - name: "${item.name}"
              mountPath: "${item.value}"
              subPath: "${item.fileName}"
            </#list>
            - name: "timezone"
              mountPath: "/etc/localtime"
        </#if>
      containers:
        - env:
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
          image: "${dockerImage}"
          imagePullPolicy: "Always"
          <#if node_port_mappings?? || cluster_port_mappings??>
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
          </#if>
          command:
            - "/bin/bash"
            - "-c"
            - |
              HOSTNAME=$(hostname)
              # 从Pod名称确定NameNode ID，仅用于日志记录
              POD_INDEX=$(echo $POD_NAME | awk -F'-' '{print $NF}')
              
              # 根据Pod索引确定NameNode ID (0对应nn1，1对应nn2)
              if [ "$POD_INDEX" == "0" ]; then
                NAMENODE_ID="nn1"
                NAMENODE_ROLE="active"
              else
                NAMENODE_ID="nn2"
                NAMENODE_ROLE="standby"
              fi
              
              echo "NameNode ID: $NAMENODE_ID, 角色: $NAMENODE_ROLE"
              
              if ${enableKerberos}; then
                echo "Kerberos is enabled. Running keystore setup...";
                if [ ! -f /etc/security/keytab/keystore ]; then
                  cd /opt/datasophon/script && sh keystore.sh $FQDN
                fi
                if [ ! -f ${appHome}/etc/hadoop/ssl-client.xml ]; then
                  echo "ssl-client.xml not found. Copying from template...";
                  cp ${appHome}/etc/hadoop/ssl-client.xml.template ${appHome}/etc/hadoop/ssl-client.xml
                fi
                if [ ! -f ${appHome}/etc/hadoop/ssl-server.xml ]; then
                  echo "ssl-server.xml not found. Copying from template...";
                  cp ${appHome}/etc/hadoop/ssl-server.xml.template ${appHome}/etc/hadoop/ssl-server.xml
                fi
                su - ${runAsUser} -c "kinit -kt /etc/security/keytab/nn.service.keytab nn/$FQDN@HADOOP.COM"
              else
                echo "Kerberos is not enabled.";
              fi
              if ${enableRangerPlugin}; then
                echo "Ranger plugin is enabled. Performing Ranger setup...";
                cd ${appHome}/ranger-hdfs-plugin && \
                sh ${appHome}/ranger-hdfs-plugin/enable-hdfs-plugin.sh
              else
                echo "Ranger plugin is not enabled. Skipping Ranger setup.";
              fi
              
              ${startCommand}
          readinessProbe:
            exec:
              command:
                - "/bin/bash"
                - "-c"
                - "${statusCommand}"
            failureThreshold: 3
            initialDelaySeconds: 30
            periodSeconds: 10
            successThreshold: 1
            timeoutSeconds: 5
          name: "${serviceRoleFullName}"
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
            - name: namenode-data
              mountPath: ${mount_path}
              subPathExpr: $(POD_NAME)
            <#list volumeConfigMapSet as item>
            - name: "${item.name}"
              mountPath: "${item.value}"
              subPath: "${item.fileName}"
            </#list>
            - name: "timezone"
              mountPath: "/etc/localtime"
        - env:
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
            - name: POD_INDEX
              valueFrom:
                fieldRef:
                  fieldPath: metadata.name
            - name: NAMENODE_DATA_DIR
              value: ${nn_name_dir}
          image: "${dockerImage}"
          imagePullPolicy: "Always"
          ports:
            - containerPort: 8019
              name: hdfs-zkfc
          command:
            - "/bin/bash"
            - "-c"
            - |
              echo "ZKFC 启动中..."
              
              # 等待NameNode服务就绪
              echo -e "$BLUE$INFO 等待NameNode服务就绪...$NC"
              RETRIES=0
              MAX_RETRIES=60
              
              # 获取Pod索引以确定NameNode角色
              POD_INDEX=$(echo $POD_NAME | awk -F'-' '{print $NF}')
              
              # 根据Pod索引确定NameNode ID
              if [ "$POD_INDEX" == "0" ]; then
                NAMENODE_ID="nn1"
              else
                NAMENODE_ID="nn2"
              fi
              echo -e "$BLUE$INFO 根据索引设置NAMENODE_ID=$NAMENODE_ID$NC"
              
              # 获取本地主机名
              HOSTNAME=$(hostname)
              
              # 注意：由于ZKFC是作为NameNode的sidecar容器部署的，它使用与NameNode相同的FQDN
              # ZKFC的FQDN格式为: hdfs-namenode-{index}.hdfs-namenode.namespace.svc.cluster.local
              # 而不是独立的ZKFC FQDN (hdfs-zkfc-{index}.hdfs-zkfc.namespace.svc.cluster.local)
              echo -e "$BLUE$INFO ZKFC使用NameNode的FQDN: $HOSTNAME$NC"
              
              # 使用Hadoop命令检测NameNode是否就绪
              while [ $RETRIES -lt $MAX_RETRIES ]; do
                # 尝试使用hdfs haadmin命令检测NameNode状态
                NN_STATE=$(su - ${runAsUser} -c "${appHome}/bin/hdfs haadmin -getServiceState $NAMENODE_ID 2>/dev/null" || echo "ERROR")
                
                # 根据返回的状态进行处理
                case "$NN_STATE" in
                  "active")
                    echo -e "$GREEN$CHECK_MARK NameNode状态: active (活跃状态)$NC"
                    break
                    ;;
                  "standby")
                    echo -e "$GREEN$CHECK_MARK NameNode状态: standby (备用状态)$NC"
                    break
                    ;;
                  "initializing")
                    echo -e "$YELLOW$PROGRESS NameNode状态: initializing (初始化中)，继续等待...$NC"
                    ;;
                  "stopping")
                    echo -e "$RED$WARNING NameNode状态: stopping (正在停止)，继续等待...$NC"
                    ;;
                  "ERROR"|*)
                    echo -e "$YELLOW$PROGRESS 无法获取NameNode状态，可能正在启动中... ($((RETRIES+1))/$MAX_RETRIES)$NC"
                    ;;
                esac
                
                # 如果状态不是active或standby，继续等待
                if [ "$NN_STATE" != "active" ] && [ "$NN_STATE" != "standby" ]; then
                  RETRIES=$((RETRIES+1))
                  sleep 5
                fi
              done
              
              if [ $RETRIES -eq $MAX_RETRIES ]; then
                echo -e "$RED$WARNING 等待NameNode服务启动超时，但仍将尝试启动ZKFC$NC"
                echo -e "$BLUE$INFO 最后检测到的NameNode状态: $NN_STATE$NC"
                # 继续执行，因为可能是首次启动时NameNode还未就绪
              fi
              
              # 输出ZKFC和NameNode的ID关系，用于调试
              echo -e "$BLUE$INFO 当前ZKFC在Pod: $POD_NAME 中, 索引: $POD_INDEX, 使用NameNode ID: $NAMENODE_ID$NC"
              
              # 启动ZKFC服务
              echo -e "$BLUE$INFO 启动ZKFC服务...$NC"
              su ${runAsUser} -c "${appHome}/control_hadoop.sh start zkfc && tail -f ${appHome}/logs/hadoop-hdfs-zkfc-$(hostname).log"
          readinessProbe:
            exec:
              command:
                - "/bin/bash"
                - "-c"
                - "su ${runAsUser} -c '${appHome}/control_hadoop.sh status zkfc'"
            failureThreshold: 3
            initialDelaySeconds: 30
            periodSeconds: 10
            successThreshold: 1
            timeoutSeconds: 5
          name: "hdfs-zkfc"
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
            - name: namenode-data
              mountPath: ${mount_path}
              subPathExpr: $(POD_NAME)
            <#list volumeConfigMapSet as item>
            - name: "${item.name}"
              mountPath: "${item.value}"
              subPath: "${item.fileName}"
            </#list>
            - name: "timezone"
              mountPath: "/etc/localtime"
      terminationGracePeriodSeconds: 30
      volumes:
        - name: namenode-data
          persistentVolumeClaim:
            claimName: "${serviceRoleFullName}"
        <#list volumeConfigMapSet as item>
        - name: "${item.name}"
          configMap:
            name: "${item.name}"
        </#list>
        - name: "timezone"
          hostPath:
            path: "/etc/localtime"