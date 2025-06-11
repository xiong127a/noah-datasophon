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
        - name: create-user
          image: "${dockerBusyboxImage}"
          command:
            - "/bin/sh"
            - "-c"
            - |
              echo "Creating HDFS user if not exists..."
              if ! id ${runAs} &>/dev/null; then
                addgroup -g 1000 ${runAs}
                adduser -u 1000 -G ${runAs} -h /home/${runAs} -D ${runAs}
                echo "User ${runAs} created."
              else
                echo "User ${runAs} already exists."
              fi
          securityContext:
            runAsUser: 0  # 以root用户运行
            privileged: true
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
          args:
            - "/bin/sh"
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
                export HADOOP_OPTS="$HADOOP_OPTS -Ddfs.ha.namenode.id=$NAMENODE_ID"
                
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
                if [ -d ${journalnodeDir}/meta ]; then
                  echo "Standby"
                  echo Y | ${appHome}/bin/hdfs namenode -bootstrapStandby
                else
                  echo "active"
                  echo Y | ${appHome}/bin/hdfs namenode -format smhadoop
                fi
              else
                echo "formatted......."
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
              export HADOOP_OPTS="$HADOOP_OPTS -Ddfs.ha.namenode.id=$NAMENODE_ID"
              
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
                  su - hdfs -c "kinit -kt /etc/security/keytab/nn.service.keytab nn/$HOSTNAME@HADOOP.COM"
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
            initialDelaySeconds: 10
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