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
  strategy:
    type: "RollingUpdate"
    rollingUpdate:
      maxSurge: 0
      maxUnavailable: 1
  podManagementPolicy: Parallel
  minReadySeconds: 30
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
        - name: "init-dirs"
          image: "${dockerBusyboxImage}"
          imagePullPolicy: "Always"
          command:
            - "/bin/sh"
            - "-c"
            - |
              # 创建必要的目录并赋权
              mkdir -p ${mount_path}
              chown -R ${runAsUser}:${runAsGroup} ${mount_path}
              echo "目录创建和权限设置完成: ${mount_path}"
              
              # 创建FE配置文件中定义的目录
              # 元数据目录
              <#if meta_dir??>
              mkdir -p ${meta_dir}
              chown -R ${runAsUser}:${runAsGroup} ${meta_dir}
              echo "创建FE元数据目录: ${meta_dir}"
              </#if>
              
              # 日志目录
              <#if LOG_DIR??>
              mkdir -p ${LOG_DIR}
              chown -R ${runAsUser}:${runAsGroup} ${LOG_DIR}
              echo "创建FE日志目录: ${LOG_DIR}"
              <#else>
              # 默认日志目录
              mkdir -p ${appHome}/log
              chown -R ${runAsUser}:${runAsGroup} ${appHome}/log
              echo "创建FE默认日志目录: ${appHome}/log"
              </#if>
              
              # 如果有其他需要创建的目录，可以在这里添加
              <#if create_dirs??>
              <#list create_dirs as dir>
              mkdir -p ${dir}
              chown -R ${runAsUser}:${runAsGroup} ${dir}
              echo "目录创建和权限设置完成: ${dir}"
              </#list>
              </#if>
          securityContext:
            privileged: true
          volumeMounts:
            - name: srfe-data
              mountPath: ${mount_path}
              subPathExpr: $(POD_NAME)
            - name: "timezone"
              mountPath: "/etc/localtime"
          env:
            - name: POD_NAME
              valueFrom:
                fieldRef:
                  fieldPath: metadata.name
            - name: POD_NAMESPACE
              valueFrom:
                fieldRef:
                  fieldPath: metadata.namespace
        - name: "wait-for-master"
          image: "${dockerBusyboxImage}"
          imagePullPolicy: "Always"
          command:
            - "/bin/sh"
            - "-c"
            - |
              # 使用POD_NAME环境变量获取Pod索引
              POD_NAME=$(hostname)
              POD_INDEX=$(echo $POD_NAME | awk -F'-' '{print $NF}')
              
              # 如果当前节点是索引0（master），则不需要等待
              if [ "$POD_INDEX" = "0" ]; then
                echo "This is the master node (index 0), no need to wait."
                exit 0
              fi
              
              echo "This is a follower node (index $POD_INDEX), waiting for master to be ready..."
              
              # 等待master节点就绪
              for i in $(seq 1 30); do
                echo "Checking if master node $MASTER_HOST is ready (attempt $i)..."
                if nc -z -w 5 $MASTER_HOST ${fe_master_port}; then
                  echo "Master node is ready, proceeding with follower startup"
                  exit 0
                fi
                echo "Master node not ready yet, waiting 5 seconds..."
                sleep 5
              done
              
              echo "Master node not ready after 30 attempts, proceeding anyway"
              exit 0
          env:
            - name: POD_NAME
              valueFrom:
                fieldRef:
                  fieldPath: metadata.name
            - name: MASTER_HOST
              value: "${fe_master_host}"
      containers:
        - name: "${serviceRoleFullName}"
          image: "${dockerRoleImage}"
          imagePullPolicy: "Always"
          <#if node_port_mappings?? || cluster_port_mappings??>
          ports:
          <#if node_port_mappings??>
          <#assign mappings = node_port_mappings>
          <#list mappings as item>
            <#if item?size gt 0 && item?keys[0]?has_content>
            - containerPort: ${(item?keys[0])}
              name: nodeport-${item?index + 1}
            </#if>
          </#list>
          </#if>
          <#if cluster_port_mappings??>
          <#assign mappings = cluster_port_mappings>
          <#list mappings as item>
            <#if item?size gt 0 && item?keys[0]?has_content>
            - containerPort: ${(item?keys[0])}
              name: clusterport-${item?index + 1}
            </#if>
          </#list>
          </#if>
          </#if>
          command:
            - "/bin/bash"
            - "-c"
            - |
              # 使用POD_NAME环境变量获取Pod索引
              POD_INDEX=$(echo $POD_NAME | awk -F'-' '{print $NF}')
              
              # 根据Pod索引决定启动命令
              if [ "$POD_INDEX" = "0" ]; then
                echo "Starting as master FE node (index 0)..."
                # 主节点不使用--helper参数
                ${startCommand}
              else
                echo "Starting as follower FE node (index $POD_INDEX)..."
                # 从节点添加--helper参数，使用环境变量MASTER_HOST
                HELPER_CMD=$(echo "${startCommand}" | sed "s|start_fe.sh --daemon|start_fe.sh --helper $MASTER_HOST:${fe_master_port} --daemon|")
                echo "Executing: $HELPER_CMD"
                eval $HELPER_CMD
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
            - name: MASTER_HOST
              value: "${fe_master_host}"
          readinessProbe:
            exec:
              command:
                - "/bin/bash"
                - "-c"
                - "${statusCommand}"
            failureThreshold: 3
            initialDelaySeconds: 30
            periodSeconds: 30
            successThreshold: 1
            timeoutSeconds: 15
          livenessProbe:
            exec:
              command:
                - "/bin/bash"
                - "-c"
                - "${statusCommand}"
            failureThreshold: 3
            initialDelaySeconds: 60
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
            - name: srfe-data
              mountPath: ${mount_path}
              subPathExpr: $(POD_NAME)
            <#list volumeConfigMapSet as item>
            - name: "${item.name}"
              mountPath: "${item.value}"
              subPath: "${item.fileName}"
            </#list>
            - name: "timezone"
              mountPath: "/etc/localtime"
      nodeSelector:
        ${namespace}-${serviceRoleFullName}: "true"
      terminationGracePeriodSeconds: 30
      volumes:
        - name: srfe-data
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
