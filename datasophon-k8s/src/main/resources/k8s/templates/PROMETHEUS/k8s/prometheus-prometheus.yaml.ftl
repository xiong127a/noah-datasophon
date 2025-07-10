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
              echo "========== 开始准备Prometheus数据目录和权限 =========="
              
              echo "Setting permissions for Prometheus PVC mount path..."
              chmod -R 777 ${mount_path}
              echo "Permissions set successfully"
              
              echo "========== 完成数据目录和权限设置 =========="
          securityContext:
            runAsUser: 0  # 以root用户运行
            privileged: true
          volumeMounts:
            - name: prometheus-data
              mountPath: ${mount_path}
              subPathExpr: $(POD_NAME)
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
            - name: API_URL
              value: "${apiUrl}"
          image: "${dockerImage}"
          imagePullPolicy: "Always"
          command:
            - "/bin/bash"
            - "-c"
            - |
              # 处理hosts文件，将主机hosts文件内容与Pod DNS配置合并
              echo "========== 开始处理hosts文件 =========="
              
              # 备份原始hosts文件
              cp /etc/hosts /tmp/original_hosts
              echo "已备份原始hosts文件到/tmp/original_hosts"
              
              # 从主机hosts文件中提取有用条目 (通常是自定义的主机映射)
              if [ -f /tmp/host_etc_hosts ]; then
                echo "从主机hosts文件提取有用条目..."
                grep -v "^127.0.0.1" /tmp/host_etc_hosts | grep -v "^::1" | grep -v "^#" >> /tmp/original_hosts
              else
                echo "警告: 主机hosts文件未找到"
              fi
              
              # 应用合并后的hosts文件
              cat /tmp/original_hosts > /etc/hosts
              
              echo "最终hosts文件内容:"
              cat /etc/hosts
              
              echo "测试主机名解析:"
              echo "hostname: $(hostname)"
              echo "hostname -f: $(hostname -f 2>/dev/null || echo '无法获取FQDN')"
              
              echo "========== hosts文件处理完成 =========="

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
                # 替换所有{{apiUrl}}，使用环境变量而不是直接替换，避免特殊字符问题
                sed -i "s|{{apiUrl}}|$API_URL|g" "$TARGET_PATH"
                
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
              
              # 继续原来的启动命令
              ulimit -n 1000000
              ulimit -u 65535
              sysctl -w fs.file-max=1000000
              sysctl -w fs.inotify.max_user_watches=524288
              sysctl -w fs.inotify.max_user_instances=524288
              ${startCommand}
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
            capabilities:
              add: ["SYS_RESOURCE"]
            runAsUser: 0
            runAsGroup: 0
            fsGroup: 0
            runAsNonRoot: false
            allowPrivilegeEscalation: true
            readOnlyRootFilesystem: false
            seLinuxOptions:
              level: "s0:c123,c456"
            windowsOptions:
              runAsUserName: "ContainerAdministrator"
            sysctls:
              - name: fs.file-max
                value: "1000000"
              - name: fs.inotify.max_user_watches
                value: "524288"
              - name: fs.inotify.max_user_instances
                value: "524288"
          volumeMounts:
            - name: prometheus-data
              mountPath: ${mount_path}
              subPathExpr: $(POD_NAME)
            <#list volumeConfigMapSet as item>
            - name: "${item.name}"
              mountPath: "${item.value}"
              subPath: "${item.fileName}"
            </#list>
            - name: "timezone"
              mountPath: "/etc/localtime"
            - name: "hosts-file"
              mountPath: "/tmp/host_etc_hosts"  # 修改为临时目录
      terminationGracePeriodSeconds: 30
      volumes:
        - name: prometheus-data
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
        - name: "hosts-file"
          hostPath:
            path: "/etc/hosts"