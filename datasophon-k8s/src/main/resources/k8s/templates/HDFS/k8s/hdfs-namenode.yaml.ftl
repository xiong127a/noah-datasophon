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
        - name: set-permissions
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
              echo "Setting permissions for NameNode PVC mount path..."
              chmod -R 777 ${mount_path}
              echo "Permissions set successfully"
          securityContext:
            runAsUser: 0  # 以root用户运行
            privileged: true
          volumeMounts:
            - name: namenode-data
              mountPath: ${mount_path}
              subPathExpr: $(POD_NAMESPACE)/$(POD_NAME)
        - name: prepare-dirs
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
              echo "========== 开始准备NameNode数据目录 =========="
              echo "当前工作目录: $(pwd)"
              echo "系统信息: $(uname -a)"
              echo "容器环境变量: $(env | sort)"
              echo "目标数据目录: ${namenodeDir}"
              
              # 检查父目录结构
              echo "父目录结构:"
              mkdir -p $(dirname ${namenodeDir})
              ls -la $(dirname ${namenodeDir})
              
              # 确保数据目录存在
              echo "正在创建数据目录: ${namenodeDir}"
              mkdir -p ${namenodeDir}
              MKDIR_STATUS=$?
              echo "数据目录创建状态: $MKDIR_STATUS"
              
              # 检查目录是否成功创建
              if [ -d "${namenodeDir}" ]; then
                echo "✓ 数据目录已成功创建"
              else
                echo "✗ 数据目录创建失败，错误码: $MKDIR_STATUS"
                echo "尝试诊断问题:"
                mount | grep -i "${mount_path}"
                df -h | grep -i "${mount_path}"
              fi
              
              # 设置宽松的权限，确保所有人都能读写
              echo "正在设置目录权限为777: ${namenodeDir}"
              chmod -R 777 ${namenodeDir}
              CHMOD_STATUS=$?
              echo "权限设置状态: $CHMOD_STATUS"
              
              # 检查权限设置结果
              ls -la ${namenodeDir}
              
              # 使用数字UID/GID设置所有权，根据Dockerfile中的定义
              echo "正在设置目录所有者为UID 2001, GID 2001 (hdfs:hadoop)"
              chown -R 2001:2001 ${namenodeDir}
              CHOWN_STATUS=$?
              echo "所有者更改状态: $CHOWN_STATUS"
              
              # 验证所有权更改
              echo "验证目录所有权:"
              ls -la ${namenodeDir}
              stat ${namenodeDir}
              
              # 检查数据目录情况
              echo "数据目录详细信息:"
              ls -la ${namenodeDir}
              echo "目录空间使用情况:"
              df -h ${namenodeDir}
              echo "目录inode使用情况:"
              df -i ${namenodeDir}
              echo "目录权限和属性详情:"
              stat ${namenodeDir}
              
              # 检查目录内容
              echo "目录内容计数:"
              find ${namenodeDir} -type f | wc -l
              
              # 检查系统资源状态
              echo "系统内存使用情况:"
              free -h || echo "free命令不可用"
              echo "系统磁盘使用情况:"
              df -h
              
              echo "========== 完成数据目录准备 =========="
          securityContext:
            runAsUser: 0  # 以root用户运行
            privileged: true
          volumeMounts:
            - name: namenode-data
              mountPath: ${mount_path}
              subPathExpr: $(POD_NAMESPACE)/$(POD_NAME)
        - name: wait-for-journalnodes
          image: "${dockerBusyboxImage}"
          command:
            - "/bin/sh"
            - "-c"
            - |
              echo "等待JournalNode服务就绪..."
              
              # 获取JournalNode服务端点
              <#if dfs_namenode_shared_edits_dir??>
              JOURNAL_ENDPOINTS=$(echo "${dfs_namenode_shared_edits_dir}" | sed -r 's|qjournal://([^/]+)/.*|\1|g')
              <#else>
              echo "警告: dfs.namenode.shared.edits.dir 未定义，使用默认值"
              JOURNAL_ENDPOINTS="journalnode-0.journalnode.default.svc.cluster.local:8485;journalnode-1.journalnode.default.svc.cluster.local:8485;journalnode-2.journalnode.default.svc.cluster.local:8485"
              </#if>
              echo "JournalNode端点: $JOURNAL_ENDPOINTS"
              
              # 使用ash兼容的方式分割字符串
              OLD_IFS="$IFS"
              IFS=";"
              for NODE in $JOURNAL_ENDPOINTS; do
                IFS="$OLD_IFS"
                HOST=$(echo $NODE | cut -d':' -f1)
                PORT=$(echo $NODE | cut -d':' -f2)
                echo "正在检查JournalNode: $HOST:$PORT"
                
                # 重试计数器
                RETRIES=0
                MAX_RETRIES=90
                
                # 循环尝试连接JournalNode
                while [ $RETRIES -lt $MAX_RETRIES ]; do
                  if nc -z $HOST $PORT; then
                    echo "JournalNode $HOST:$PORT 已就绪"
                    break
                  else
                    echo "JournalNode $HOST:$PORT 未就绪，等待重试... ($((RETRIES+1))/$MAX_RETRIES)"
                    RETRIES=$((RETRIES+1))
                    sleep 2
                  fi
                done
                
                # 检查是否达到最大重试次数
                if [ $RETRIES -eq $MAX_RETRIES ]; then
                  echo "错误: JournalNode $HOST:$PORT 在$MAX_RETRIES次尝试后仍未就绪"
                  exit 1
                fi
                IFS=";"
              done
              IFS="$OLD_IFS"
              
              echo "所有JournalNode服务已就绪，可以继续初始化NameNode"
        - name: namenode-format
          image: "${dockerImage}"
          env:
            - name: USER
              value: ${runAsUser}
            - name: POD_NAME
              valueFrom:
                fieldRef:
                  fieldPath: metadata.name
            - name: POD_NAMESPACE
              valueFrom:
                fieldRef:
                  fieldPath: metadata.namespace
          args:
            - "/bin/bash"
            - "-c"
            - |
              # 从Pod名称确定NameNode ID和角色
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
                  HOSTNAME=$(hostname)
                  cd /opt/datasophon/script && sh keystore.sh $HOSTNAME
                fi
                if [ ! -f ${appHome}/etc/hadoop/ssl-client.xml ]; then
                  echo "ssl-client.xml not found. Copying from template...";
                  cp ${appHome}/etc/hadoop/ssl-client.xml.template ${appHome}/etc/hadoop/ssl-client.xml
                fi
                if [ ! -f ${appHome}/etc/hadoop/ssl-server.xml ]; then
                  echo "ssl-server.xml not found. Copying from template...";
                  cp ${appHome}/etc/hadoop/ssl-server.xml.template ${appHome}/etc/hadoop/ssl-server.xml
                fi
              else
                echo "Kerberos is not enabled. Skipping Kerberos setup.";
              fi
              if ${enableRangerPlugin}; then
                echo "Ranger plugin is enabled. Performing Ranger setup...";
                cd ${appHome}/ranger-hdfs-plugin && \
                sh ${appHome}/ranger-hdfs-plugin/enable-hdfs-plugin.sh
              else
                echo "Ranger plugin is not enabled. Skipping Ranger setup.";
              fi
              
              # 在格式化之前检查数据目录权限
              echo "========== 格式化前检查 =========="
              echo "当前用户: $(id)"
              echo "数据目录权限: $(ls -ld ${namenodeDir})"
              echo "可创建文件测试:"
              su - ${runAsUser} -c "touch ${namenodeDir}/test_write_permission && echo \"✓ 写入测试成功\" || echo \"✗ 写入测试失败\""
              su - ${runAsUser} -c "rm -f ${namenodeDir}/test_write_permission"
              
              # 确保以hdfs用户运行NameNode格式化命令
              
              # 根据角色和目录状态执行不同操作
              if [ "$NAMENODE_ROLE" == "active" ]; then
                # 第一个NameNode (active)
                if [ ! -d ${namenodeDir}/current ]; then
                  echo "格式化主NameNode (nn1)..."
                  set -x  # 启用命令跟踪，便于调试
                  # 使用${runAsUser}用户执行格式化命令
                  su - ${runAsUser} -c "echo Y | ${appHome}/bin/hdfs namenode -format smhadoop"
                  FORMAT_RESULT=$?
                  echo "format结果: $FORMAT_RESULT"
                  set +x  # 关闭命令跟踪
                else
                  echo "主NameNode (nn1) 已格式化，跳过格式化步骤"
                fi
              else
                # 其他NameNode (standby)
                if [ ! -d ${namenodeDir}/current ]; then
                  echo "同步备用NameNode (nn2) 元数据..."
                  set -x  # 启用命令跟踪，便于调试
                  # 使用${runAsUser}用户执行bootstrapStandby命令
                  su - ${runAsUser} -c "echo Y | ${appHome}/bin/hdfs namenode -bootstrapStandby"
                  BOOTSTRAP_RESULT=$?
                  echo "bootstrapStandby结果: $BOOTSTRAP_RESULT"
                  set +x  # 关闭命令跟踪
                else
                  echo "备用NameNode (nn2) 已同步，跳过同步步骤"
                fi
              fi
              
              # 格式化后检查
              echo "========== 格式化/同步后检查 =========="
              echo "数据目录内容:"
              su - ${runAsUser} -c "ls -la ${namenodeDir}/"
              if [ -d ${namenodeDir}/current ]; then
                echo "current目录创建成功，内容:"
                su - ${runAsUser} -c "ls -la ${namenodeDir}/current/"
              else
                echo "警告: current目录未创建"
              fi
          volumeMounts:
            - name: namenode-data
              mountPath: ${mount_path}
              subPathExpr: $(POD_NAMESPACE)/$(POD_NAME)
            <#list volumeConfigMapSet as item>
            - name: "${item.name}"
              mountPath: "${item.value}"
              subPath: "${item.fileName}"
            </#list>
            - name: "timezone"
              mountPath: "/etc/localtime"
        <#if isFirstInstall?? && isFirstInstall>
        # ZKFC格式化初始化容器 - 只在首次安装时执行
        - name: zkfc-format
          image: "${dockerImage}"
          env:
            - name: USER
              value: ${runAsUser}
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
              echo "开始检查是否需要格式化ZKFC..."
              
              # 从Pod名称确定NameNode ID和角色
              POD_INDEX=$(echo $POD_NAME | awk -F'-' '{print $NF}')
              
              # 只在第一个NameNode（index=0）上执行格式化
              if [ "$POD_INDEX" != "0" ]; then
                echo "当前Pod不是第一个NameNode (index=$POD_INDEX)，跳过ZKFC格式化"
                exit 0
              fi
              
              # 设置环境变量
              export NAMENODE_ID="nn1"
              export HADOOP_OPTS="-Ddfs.ha.namenode.id=nn1"
              export HDFS_NAMENODE_OPTS="-Ddfs.ha.namenode.id=nn1"
              
              # 检查ZooKeeper中是否已经存在zkfc相关的znode
               # 实际使用中，这里应该添加适当的ZooKeeper检查逻辑
               # 简化版本：检查ZKFC是否已经格式化的标记文件
               ZKFC_FORMATTED_FLAG="${namenodeDir}/zkfc_formatted"
               
               if [ -f "$ZKFC_FORMATTED_FLAG" ]; then
                 echo "检测到ZKFC已经格式化（标记文件 $ZKFC_FORMATTED_FLAG 存在），跳过格式化"
                 exit 0
               fi
               
               # 尝试通过ZooKeeper CLI检查znode是否存在
               echo "尝试检查ZooKeeper中是否已存在HDFS HA的znode..."
               
               # 提取ZooKeeper地址
               if [ -f "${appHome}/etc/hadoop/core-site.xml" ]; then
                 ZK_QUORUM=$(grep -A1 "ha.zookeeper.quorum" ${appHome}/etc/hadoop/core-site.xml | grep "<value>" | sed -e 's/.*<value>\(.*\)<\/value>.*/\1/')
                 CLUSTER_NAME=$(grep -A1 "fs.defaultFS" ${appHome}/etc/hadoop/core-site.xml | grep "<value>" | sed -e 's/.*<value>hdfs:\/\/\(.*\)<\/value>.*/\1/' | cut -d '/' -f1)
                 
                 if [ -n "$ZK_QUORUM" ] && [ -n "$CLUSTER_NAME" ]; then
                   echo "检测到ZooKeeper地址: $ZK_QUORUM"
                   echo "检测到集群名称: $CLUSTER_NAME"
                   
                   # 尝试使用ZooKeeper命令检查znode
                   ZK_PATH="/hadoop-ha/$CLUSTER_NAME"
                   echo "检查ZooKeeper路径: $ZK_PATH"
                   
                   # 尝试使用zkCli.sh检查znode是否存在
                   # 注意：这里简化处理，实际情况可能需要更复杂的逻辑
                   ZK_CHECK_CMD="${appHome}/bin/hdfs zkfc -getServiceState"
                   if su - ${runAsUser} -c "$ZK_CHECK_CMD" 2>&1 | grep -q "active\|standby"; then
                     echo "检测到ZKFC已经格式化（ZooKeeper znode已存在），跳过格式化"
                     # 创建标记文件，避免重复检查
                     touch "$ZKFC_FORMATTED_FLAG"
                     chown ${runAsUser}:${runAsUser} "$ZKFC_FORMATTED_FLAG"
                     exit 0
                   else
                     echo "ZooKeeper中未找到HDFS HA相关的znode，需要执行格式化"
                   fi
                 else
                   echo "无法从core-site.xml提取ZooKeeper地址或集群名称，继续格式化"
                 fi
               else
                 echo "未找到core-site.xml文件，跳过ZooKeeper检查，继续格式化"
               fi
               
               echo "开始执行ZKFC格式化..."
              
              # 添加Kerberos相关配置
              if ${enableKerberos}; then
                echo "Kerberos is enabled. Running keystore setup...";
                HOSTNAME=$(hostname)
                if [ ! -f /etc/security/keytab/keystore ]; then
                  cd /opt/datasophon/script && sh keystore.sh $HOSTNAME
                fi
                if [ ! -f ${appHome}/etc/hadoop/ssl-client.xml ]; then
                  echo "ssl-client.xml not found. Copying from template...";
                  cp ${appHome}/etc/hadoop/ssl-client.xml.template ${appHome}/etc/hadoop/ssl-client.xml
                fi
                if [ ! -f ${appHome}/etc/hadoop/ssl-server.xml ]; then
                  echo "ssl-server.xml not found. Copying from template...";
                  cp ${appHome}/etc/hadoop/ssl-server.xml.template ${appHome}/etc/hadoop/ssl-server.xml
                fi
                # 执行Kerberos身份验证
                su - ${runAsUser} -c "kinit -kt /etc/security/keytab/nn.service.keytab nn/$HOSTNAME@HADOOP.COM"
              fi
              
              # 执行ZKFC格式化
              set -x  # 启用命令跟踪，便于调试
              FORMAT_CMD="${appHome}/bin/hdfs zkfc -formatZK"
              echo "执行命令: $FORMAT_CMD"
              su - ${runAsUser} -c "cd ${appHome} && echo Y | $FORMAT_CMD"
              FORMAT_RESULT=$?
              set +x  # 关闭命令跟踪
              
              # 检查格式化结果
              if [ $FORMAT_RESULT -eq 0 ]; then
                echo "✅ ZKFC格式化成功"
                # 创建标记文件，避免重复格式化
                touch "$ZKFC_FORMATTED_FLAG"
                chown ${runAsUser}:${runAsUser} "$ZKFC_FORMATTED_FLAG"
              else
                echo "❌ ZKFC格式化失败，错误码: $FORMAT_RESULT"
                # 这里可以添加重试逻辑或其他错误处理
                # 但为了保证初始化容器不阻塞Pod启动，我们依然返回成功
                echo "警告：ZKFC格式化失败，但允许Pod继续启动"
              fi
              
              echo "ZKFC格式化步骤完成"
          volumeMounts:
            - name: namenode-data
              mountPath: ${mount_path}
              subPathExpr: $(POD_NAMESPACE)/$(POD_NAME)
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
                  cd /opt/datasophon/script && sh keystore.sh $HOSTNAME
                fi
                if [ ! -f ${appHome}/etc/hadoop/ssl-client.xml ]; then
                  echo "ssl-client.xml not found. Copying from template...";
                  cp ${appHome}/etc/hadoop/ssl-client.xml.template ${appHome}/etc/hadoop/ssl-client.xml
                fi
                if [ ! -f ${appHome}/etc/hadoop/ssl-server.xml ]; then
                  echo "ssl-server.xml not found. Copying from template...";
                  cp ${appHome}/etc/hadoop/ssl-server.xml.template ${appHome}/etc/hadoop/ssl-server.xml
                fi
                su - ${runAsUser} -c "kinit -kt /etc/security/keytab/nn.service.keytab nn/$HOSTNAME@HADOOP.COM"
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
              subPathExpr: $(POD_NAMESPACE)/$(POD_NAME)
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
              value: ${namenodeDir}
          image: "${dockerImage}"
          imagePullPolicy: "Always"
          ports:
            - containerPort: 8019
              name: zkfc
          command:
            - "/bin/bash"
            - "-c"
            - |
              echo "ZKFC 启动中..."
              
              # 等待NameNode进程启动
              echo "等待NameNode进程启动..."
              RETRIES=0
              MAX_RETRIES=60
              while [ $RETRIES -lt $MAX_RETRIES ]; do
                if su - ${runAsUser} -c "jps" | grep -q "NameNode"; then
                  echo "✅ NameNode进程已启动"
                  break
                else
                  echo "⏳ 等待NameNode进程启动... ($((RETRIES+1))/$MAX_RETRIES)"
                  RETRIES=$((RETRIES+1))
                  sleep 5
                fi
              done
              
              if [ $RETRIES -eq $MAX_RETRIES ]; then
                echo "❌ 等待NameNode进程启动超时"
                # 继续执行，因为可能是首次启动时NameNode还未就绪
              fi
              
              # 获取Pod索引以确定NameNode角色
              POD_INDEX=$(echo $POD_NAME | awk -F'-' '{print $NF}')
              
              # 根据Pod索引确定NameNode ID
              if [ "$POD_INDEX" == "0" ]; then
                NAMENODE_ID="nn1"
              else
                NAMENODE_ID="nn2"
              fi
              echo "根据索引设置NAMENODE_ID=$NAMENODE_ID"
              
              # 输出ZKFC和NameNode的ID关系，用于调试
              echo "当前ZKFC在Pod: $POD_NAME 中, 索引: $POD_INDEX, 使用NameNode ID: $NAMENODE_ID"
              
              # 启动ZKFC服务
              echo "启动ZKFC服务..."
              su - ${runAsUser} -c "${appHome}/bin/hdfs --daemon start zkfc"
              
              # 保持容器运行
              while true; do
                # 检查ZKFC进程是否在运行
                if ! su - ${runAsUser} -c "jps" | grep -q "DFSZKFailoverController"; then
                  echo "警告: ZKFC进程不在运行，尝试重新启动..."
                  su - ${runAsUser} -c "${appHome}/bin/hdfs --daemon start zkfc"
                fi
                sleep 30
              done
          readinessProbe:
            exec:
              command:
                - "/bin/bash"
                - "-c"
                - "su - ${runAsUser} -c \"jps | grep -q DFSZKFailoverController\" || exit 1"
            failureThreshold: 3
            initialDelaySeconds: 30
            periodSeconds: 10
            successThreshold: 1
            timeoutSeconds: 5
          name: "zkfc"
          resources:
            requests:
              memory: ${zkfc_requests_memory}
              cpu: ${zkfc_requests_cpu}
            limits:
              memory: ${zkfc_limits_memory}
              cpu: ${zkfc_limits_cpu}
          securityContext:
            privileged: true
          volumeMounts:
            - name: namenode-data
              mountPath: ${mount_path}
              subPathExpr: $(POD_NAMESPACE)/$(POD_NAME)
            <#list volumeConfigMapSet as item>
            - name: "${item.name}"
              mountPath: "${item.value}"
              subPath: "${item.fileName}"
            </#list>
            - name: "timezone"
              mountPath: "/etc/localtime"
      nodeSelector:
        ${serviceRoleFullName}: "true"
      terminationGracePeriodSeconds: 30
      volumes:
        - name: namenode-data
          persistentVolumeClaim:
            claimName: "${serviceRoleFullName}-pvc"
        <#list volumeConfigMapSet as item>
        - name: "${item.name}"
          configMap:
            name: "${item.name}"
        </#list>
        - name: "timezone"
          hostPath:
            path: "/etc/localtime"