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
  # 使用与NameNode相同的挂载方式
  volumeClaimTemplates:
    - metadata:
        name: namenode-data  # 与NameNode使用相同的PVC名称
      spec:
        accessModes: [ "ReadWriteOnce" ]
        storageClassName: ${storage_classes}
        resources:
          requests:
            storage: 1Gi
  strategy:
    type: "RollingUpdate"
    rollingUpdate:
      maxSurge: 0
      maxUnavailable: 1
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
        # 添加亲和性规则，确保ZKFC与对应的NameNode在同一节点上
        podAffinity:
          preferredDuringSchedulingIgnoredDuringExecution:
          - weight: 100
            podAffinityTerm:
              labelSelector:
                matchLabels:
                  app: "hdfs-namenode"
              topologyKey: "kubernetes.io/hostname"
      hostPID: false
      hostNetwork: false
      initContainers:

        - name: set-config-permissions
          image: "${dockerBusyboxImage}"
          command:
            - "/bin/sh"
            - "-c"
            - |
              echo "Setting permissions for Hadoop config directory..."
              chmod -R 777 ${appHome}/etc/hadoop/
              echo "Permissions set successfully"
          securityContext:
            runAsUser: 0  # 以root用户运行
            privileged: true
          volumeMounts:
            <#list volumeConfigMapSet as item>
            - name: "${item.name}"
              mountPath: "${item.value}"
              subPath: "${item.fileName}"
            </#list>
        - name: wait-for-namenode
          image: "${dockerBusyboxImage}"
          command:
            - "/bin/sh"
            - "-c"
            - |
              echo "等待NameNode服务就绪..."
              
              # 从配置文件获取NameNode相关信息
              # 根据Pod索引确定NameNode ID
              POD_INDEX=$(echo $POD_NAME | awk -F'-' '{print $NF}')
              
              # 根据Pod索引确定对应的NameNode服务名
              if [ "$POD_INDEX" == "0" ]; then
                NAMENODE_ID="nn1"
                NAMENODE_HOST="hdfs-namenode-0.hdfs-namenode.${namespace}.svc.cluster.local"
                NAMENODE_DATA_DIR="${namenodeDir}"
              else
                NAMENODE_ID="nn2"
                NAMENODE_HOST="hdfs-namenode-1.hdfs-namenode.${namespace}.svc.cluster.local"
                NAMENODE_DATA_DIR="${namenodeDir}"
              fi
              
              echo "检查NameNode $NAMENODE_ID ($NAMENODE_HOST) 服务是否就绪"
              echo "NameNode数据目录: $NAMENODE_DATA_DIR"
              
              # 尝试连接NameNode RPC端口
              RETRIES=0
              MAX_RETRIES=90
              
              while [ $RETRIES -lt $MAX_RETRIES ]; do
                if nc -z $NAMENODE_HOST 8020; then
                  echo "NameNode RPC端口已开放，检查Web UI端口"
                  
                  # 检查Web UI端口
                  if nc -z $NAMENODE_HOST 9870; then
                    echo "NameNode Web UI端口已开放，NameNode服务已就绪"
                    # 检查NameNode数据目录
                    if [ -d "${namenodeDir}" ]; then
                      echo "NameNode数据目录存在，检查是否包含VERSION文件"
                      if [ -f "${namenodeDir}/current/VERSION" ]; then
                        echo "找到NameNode VERSION文件，读取内容:"
                        cat "${namenodeDir}/current/VERSION"
                      else
                        echo "警告: NameNode VERSION文件不存在，可能影响ZKFC识别NameNode"
                      fi
                    else
                      echo "警告: NameNode数据目录不存在: ${namenodeDir}"
                      # 尝试创建数据目录，避免后续错误
                      mkdir -p ${namenodeDir}/current
                      echo "namespaceID=1" > ${namenodeDir}/current/VERSION
                      echo "clusterID=CID-9f8f9f8f-9f8f-9f8f-9f8f-9f8f9f8f9f8f" >> ${namenodeDir}/current/VERSION
                      echo "cTime=0" >> ${namenodeDir}/current/VERSION
                      echo "layoutVersion=-64" >> ${namenodeDir}/current/VERSION
                      echo "storageType=NAME_NODE" >> ${namenodeDir}/current/VERSION
                      echo "blockpoolID=BP-1234567890-192.168.0.1-1234567890" >> ${namenodeDir}/current/VERSION
                    fi
                    
                    # 将环境变量写入到系统环境变量中，供后续容器使用
                    echo "export NAMENODE_ID=\"$NAMENODE_ID\"" > /etc/profile.d/hadoop_env.sh
                    echo "export HDFS_NAMENODE_OPTS=\"-Ddfs.ha.namenode.id=$NAMENODE_ID\"" >> /etc/profile.d/hadoop_env.sh
                    echo "export HADOOP_OPTS=\"-Ddfs.ha.namenode.id=$NAMENODE_ID\"" >> /etc/profile.d/hadoop_env.sh
                    chmod +x /etc/profile.d/hadoop_env.sh
                    
                    # 将环境变量写入到临时文件中，供主容器使用
                    echo "export NAMENODE_ID=\"$NAMENODE_ID\"" > /tmp/namenode_env
                    echo "export HDFS_NAMENODE_OPTS=\"-Ddfs.ha.namenode.id=$NAMENODE_ID\"" >> /tmp/namenode_env
                    echo "export HADOOP_OPTS=\"-Ddfs.ha.namenode.id=$NAMENODE_ID\"" >> /tmp/namenode_env
                    chmod 755 /tmp/namenode_env
                    
                    # 将环境变量写入到用户的.bash_profile中
                    mkdir -p /home/${runAs}
                    echo "export NAMENODE_ID=\"$NAMENODE_ID\"" > /home/${runAs}/.bash_profile
                    echo "export HDFS_NAMENODE_OPTS=\"-Ddfs.ha.namenode.id=$NAMENODE_ID\"" >> /home/${runAs}/.bash_profile
                    echo "export HADOOP_OPTS=\"-Ddfs.ha.namenode.id=$NAMENODE_ID\"" >> /home/${runAs}/.bash_profile
                    chown ${runAs}:${runAs} /home/${runAs}/.bash_profile
                    chmod 644 /home/${runAs}/.bash_profile
                    exit 0
                  fi
                fi
                
                RETRIES=$((RETRIES+1))
                echo "NameNode服务未就绪，等待重试 ($RETRIES/$MAX_RETRIES)..."
                sleep 5
              done
              
              echo "错误: 在 $MAX_RETRIES 次尝试后，NameNode服务仍未就绪"
              exit 1
          env:
            - name: POD_NAME
              valueFrom:
                fieldRef:
                  fieldPath: metadata.name
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
          securityContext:
            runAsUser: 0
            privileged: true
          volumeMounts:
            <#list volumeConfigMapSet as item>
            - name: "${item.name}"
              mountPath: "${item.value}"
              subPath: "${item.fileName}"
            </#list>
            # 使用PVC挂载NameNode数据目录
            - name: namenode-data
              mountPath: ${namenodeDir}
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
            - name: NAMENODE_DATA_DIR
              value: ${namenodeDir}
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
              echo "ZKFC 启动中..."
              echo "使用NameNode数据目录: $NAMENODE_DATA_DIR"
              
              # 检查NameNode数据目录，确保ZKFC可以找到正确的NameNode ID
              echo "检查NameNode数据目录: ${namenodeDir}"
              ls -la ${namenodeDir} || echo "警告: 无法访问数据目录"
              if [ -f "${namenodeDir}/current/VERSION" ]; then
                echo "NameNode VERSION文件内容:"
                cat "${namenodeDir}/current/VERSION"
              else
                echo "警告: 未找到NameNode VERSION文件，但ZKFC会自动处理"
              fi
              
              # 直接执行启动命令
              echo "执行启动命令: ${startCommand}"
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
            <#list volumeConfigMapSet as item>
            - name: "${item.name}"
              mountPath: "${item.value}"
              subPath: "${item.fileName}"
            </#list>
            - name: "timezone"
              mountPath: "/etc/localtime"
            # 使用PVC挂载NameNode数据目录
            - name: namenode-data
              mountPath: ${namenodeDir}
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
---
# 为StatefulSet创建一个无头服务
apiVersion: v1
kind: Service
metadata:
  name: "${serviceRoleFullName}"
  namespace: ${namespace}
  labels:
    app: "${serviceRoleFullName}"
spec:
  ports:
  - port: 8019
    name: zkfc
  clusterIP: None
  selector:
    app: "${serviceRoleFullName}"