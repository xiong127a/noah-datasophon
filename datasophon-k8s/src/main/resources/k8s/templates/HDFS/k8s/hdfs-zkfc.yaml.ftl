apiVersion: "apps/v1"
kind: "Deployment"
metadata:
  labels:
    name: "${serviceRoleFullName}"
  name: "${serviceRoleFullName}"
  namespace: ${namespace}
spec:
  replicas: ${roleNodeCnt}
  selector:
    matchLabels:
      app: "${serviceRoleFullName}"
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
                NAMENODE_HOST="namenode-0.namenode.${namespace}.svc.cluster.local"
              else
                NAMENODE_ID="nn2"
                NAMENODE_HOST="namenode-1.namenode.${namespace}.svc.cluster.local"
              fi
              
              echo "检查NameNode $NAMENODE_ID ($NAMENODE_HOST) 服务是否就绪"
              
              # 尝试连接NameNode RPC端口
              RETRIES=0
              MAX_RETRIES=90
              
              while [ $RETRIES -lt $MAX_RETRIES ]; do
                if nc -z $NAMENODE_HOST 8020; then
                  echo "NameNode RPC端口已开放，检查Web UI端口"
                  
                  # 检查Web UI端口
                  if nc -z $NAMENODE_HOST 9870; then
                    echo "NameNode Web UI端口已开放，NameNode服务已就绪"
                    # 将环境变量写入到系统环境变量中，供后续容器使用
                    echo "export HDFS_NAMENODE_OPTS=\"-Ddfs.ha.namenode.id=$NAMENODE_ID\"" > /etc/profile.d/hadoop_env.sh
                    echo "export HADOOP_OPTS=\"-Ddfs.ha.namenode.id=$NAMENODE_ID\"" >> /etc/profile.d/hadoop_env.sh
                    chmod +x /etc/profile.d/hadoop_env.sh
                    
                    # 将环境变量写入到用户的.bash_profile中
                    echo "export HDFS_NAMENODE_OPTS=\"-Ddfs.ha.namenode.id=$NAMENODE_ID\"" > /home/${runAs}/.bash_profile
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
              # 确定关联的NameNode ID
              HOSTNAME=$(hostname)
              POD_INDEX=$(echo $POD_NAME | awk -F'-' '{print $NF}')
              
              # 根据Pod索引确定NameNode ID (0对应nn1，1对应nn2)
              if [ "$POD_INDEX" == "0" ]; then
                NAMENODE_ID="nn1"
              else
                NAMENODE_ID="nn2"
              fi
              
              echo "ZKFC 将监控 NameNode ID: $NAMENODE_ID"
              
              # 显式设置NameNode ID到环境变量
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
              
              echo "启动ZKFC服务，使用NameNode ID: $NAMENODE_ID"
              echo "HADOOP_OPTS=$HADOOP_OPTS"
              
              # 直接执行startCommand，因为它已经包含了用户切换命令
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