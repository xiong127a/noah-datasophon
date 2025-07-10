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
      dnsPolicy: ClusterFirst
      containers:
        - env:
            - name: "ES_JAVA_HOME"
              value: "/opt/datasophon/elasticsearch-7.16.2/jdk"
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
              echo "vm.max_map_count=655360" >> /etc/sysctl.conf && sysctl -p && ${startCommand}
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
            - name: es-data
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
        ${serviceRoleFullName}: "true"
      terminationGracePeriodSeconds: 30
      volumes:
        - name: es-data
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
