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
  volumeClaimTemplates:
    - metadata:
        name: namenode-data
      spec:
        accessModes: [ "ReadWriteOnce" ]
        storageClassName: ${storage_classes}
        resources:
          requests:
            storage: ${storage}
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
              value: ${runAs}
            - name: POD_NAME
              valueFrom:
                fieldRef:
                  fieldPath: metadata.name
            - name: POD_NAMESPACE
              valueFrom:
                fieldRef:
                  fieldPath: metadata.namespace
            - name: HADOOP_OPTS
              valueFrom:
                fieldRef:
                  fieldPath: metadata.name
                  # 这个值会在容器启动命令中被覆盖，这里只是为了创建环境变量
            - name: HDFS_NAMENODE_OPTS
              valueFrom:
                fieldRef:
                  fieldPath: metadata.name
                  # 这个值会在容器启动命令中被覆盖，这里只是为了创建环境变量
          args:
            - "/bin/bash"
            - "-c"
            - |
              if [ ! -d ${namenodeDir}/current ]; then
                echo "format namenode";
                
                # 从Pod名称确定NameNode ID
                POD_INDEX=$(echo $POD_NAME | awk -F'-' '{print $NF}')
                
                # 根据Pod索引确定NameNode ID (0对应nn1，1对应nn2)
                if [ "$POD_INDEX" == "0" ]; then
                  NAMENODE_ID="nn1"
                else
                  NAMENODE_ID="nn2"
                fi
                
                echo "初始化NameNode ID设置为: $NAMENODE_ID"
                
                # 通过环境变量设置NameNode ID，这将覆盖配置文件中的值
                export HDFS_NAMENODE_OPTS="-Ddfs.ha.namenode.id=$NAMENODE_ID"
                export HADOOP_OPTS="-Ddfs.ha.namenode.id=$NAMENODE_ID"
                
                # 将环境变量写入到系统环境变量中
                echo "export HDFS_NAMENODE_OPTS=\"-Ddfs.ha.namenode.id=$NAMENODE_ID\"" > /etc/profile.d/hadoop_env.sh
                echo "export HADOOP_OPTS=\"-Ddfs.ha.namenode.id=$NAMENODE_ID\"" >> /etc/profile.d/hadoop_env.sh
                chmod +x /etc/profile.d/hadoop_env.sh
                
                # 将环境变量写入到用户的.bash_profile中
                echo "export HDFS_NAMENODE_OPTS=\"-Ddfs.ha.namenode.id=$NAMENODE_ID\"" > /home/${runAs}/.bash_profile
                echo "export HADOOP_OPTS=\"-Ddfs.ha.namenode.id=$NAMENODE_ID\"" >> /home/${runAs}/.bash_profile
                chown ${runAs}:${runAs} /home/${runAs}/.bash_profile
                chmod 644 /home/${runAs}/.bash_profile
                
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
                sleep $((RANDOM % 10))
                
                # 在格式化之前检查数据目录权限
                echo "========== 格式化前检查 =========="
                echo "当前用户: $(id)"
                echo "数据目录权限: $(ls -ld ${namenodeDir})"
                echo "可创建文件测试:"
                touch ${namenodeDir}/test_write_permission && echo "✓ 写入测试成功" || echo "✗ 写入测试失败"
                rm -f ${namenodeDir}/test_write_permission
                
                # 确保以hdfs用户运行NameNode格式化命令
                chown -R ${runAs}:${runAs} ${namenodeDir}
                chmod -R 755 ${namenodeDir}
                
                if [ -d ${journalnodeDir}/meta ]; then
                  echo "Standby"
                  set -x  # 启用命令跟踪，便于调试
                  # 直接执行命令
                  echo "执行bootstrapStandby命令..."
                  echo Y | ${appHome}/bin/hdfs namenode -bootstrapStandby
                  BOOTSTRAP_RESULT=$?
                  echo "bootstrapStandby结果: $BOOTSTRAP_RESULT"
                  set +x  # 关闭命令跟踪
                else
                  echo "active"
                  set -x  # 启用命令跟踪，便于调试
                  # 直接执行命令
                  echo "执行format命令..."
                  echo Y | ${appHome}/bin/hdfs namenode -format smhadoop
                  FORMAT_RESULT=$?
                  echo "format结果: $FORMAT_RESULT"
                  set +x  # 关闭命令跟踪
                fi
                
                # 格式化后检查
                echo "========== 格式化后检查 =========="
                echo "数据目录内容:"
                ls -la ${namenodeDir}/
                if [ -d ${namenodeDir}/current ]; then
                  echo "current目录创建成功，内容:"
                  ls -la ${namenodeDir}/current/
                else
                  echo "警告: current目录未创建"
                fi
              else
                echo "formatted......."
                # 显示现有数据目录结构
                echo "已格式化的数据目录结构:"
                ls -la ${namenodeDir}/
                echo "current目录内容:"
                ls -la ${namenodeDir}/current/
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
      containers:
        - env:
            - name: USER
              value: ${runAs}
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
            - name: HADOOP_OPTS
              valueFrom:
                fieldRef:
                  fieldPath: metadata.name
                  # 这个值会在容器启动命令中被覆盖，这里只是为了创建环境变量
            - name: HDFS_NAMENODE_OPTS
              valueFrom:
                fieldRef:
                  fieldPath: metadata.name
                  # 这个值会在容器启动命令中被覆盖，这里只是为了创建环境变量
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
              # 从Pod名称确定NameNode ID
              POD_INDEX=$(echo $POD_NAME | awk -F'-' '{print $NF}')
              
              # 根据Pod索引确定NameNode ID (0对应nn1，1对应nn2)
              if [ "$POD_INDEX" == "0" ]; then
                NAMENODE_ID="nn1"
              else
                NAMENODE_ID="nn2"
              fi
              
              echo "NameNode ID设置为: $NAMENODE_ID"
              
              # 通过环境变量设置NameNode ID，这将覆盖配置文件中的值
              export HDFS_NAMENODE_OPTS="-Ddfs.ha.namenode.id=$NAMENODE_ID"
              export HADOOP_OPTS="-Ddfs.ha.namenode.id=$NAMENODE_ID"
              
              # 将环境变量写入到系统环境变量中
              echo "export HDFS_NAMENODE_OPTS=\"-Ddfs.ha.namenode.id=$NAMENODE_ID\"" > /etc/profile.d/hadoop_env.sh
              echo "export HADOOP_OPTS=\"-Ddfs.ha.namenode.id=$NAMENODE_ID\"" >> /etc/profile.d/hadoop_env.sh
              chmod +x /etc/profile.d/hadoop_env.sh
              
              # 将环境变量写入到用户的.bash_profile中
              echo "export HDFS_NAMENODE_OPTS=\"-Ddfs.ha.namenode.id=$NAMENODE_ID\"" > /home/${runAs}/.bash_profile
              echo "export HADOOP_OPTS=\"-Ddfs.ha.namenode.id=$NAMENODE_ID\"" >> /home/${runAs}/.bash_profile
              chown ${runAs}:${runAs} /home/${runAs}/.bash_profile
              chmod 644 /home/${runAs}/.bash_profile
              
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
                su - ${runAs} -c "kinit -kt /etc/security/keytab/nn.service.keytab nn/$HOSTNAME@HADOOP.COM"
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
      nodeSelector:
        ${serviceRoleFullName}: "true"
      terminationGracePeriodSeconds: 30
      volumes:
        <#list volumeConfigMapSet as item>
        - name: "${item.name}"
          configMap:
            name: "${item.name}"
        </#list>
        - name: "timezone"
          hostPath:
            path: "/etc/localtime"