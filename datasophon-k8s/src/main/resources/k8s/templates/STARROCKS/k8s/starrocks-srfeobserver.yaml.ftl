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
              
              echo "This is an observer node (index $POD_INDEX), waiting for master to be ready..."
              
              # 等待master节点就绪
              for i in $(seq 1 30); do
                echo "Checking if master node $MASTER_HOST is ready (attempt $i)..."
                if nc -z -w 5 $MASTER_HOST ${fe_master_port}; then
                  echo "Master node is ready, proceeding with observer startup"
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
              # 使用POD_NAME环境变量获取Pod索引
              POD_INDEX=$(echo $POD_NAME | awk -F'-' '{print $NF}')
              
              echo "Starting as observer node (index $POD_INDEX), connecting to master at $MASTER_HOST"
              
              # 添加--helper参数
              HELPER_CMD=$(echo "${startCommand}" | sed "s|start_fe.sh --daemon|start_fe.sh --helper $MASTER_HOST:${fe_master_port} --daemon|")
              echo "Executing: $HELPER_CMD"
              eval $HELPER_CMD
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
