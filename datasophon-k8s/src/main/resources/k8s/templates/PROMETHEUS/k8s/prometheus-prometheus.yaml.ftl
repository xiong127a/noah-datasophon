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
              subPathExpr: $(POD_NAMESPACE)/$(POD_NAME)
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
              subPathExpr: $(POD_NAMESPACE)/$(POD_NAME)
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
            claimName: "${serviceRoleFullName}-pvc"
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