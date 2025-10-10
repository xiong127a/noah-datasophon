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
  podManagementPolicy: Parallel
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
    spec:
      nodeSelector:
        ${namespace}-${serviceRoleFullName}: "true"
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
              
              # 创建CN节点所需目录
              # 存储根目录
              <#if storage_root_path??>
              mkdir -p ${storage_root_path}
              chown -R ${runAsUser}:${runAsGroup} ${storage_root_path}
              echo "创建CN存储根目录: ${storage_root_path}"
              </#if>
              
              # 溢出存储目录
              <#if spill_local_storage_dir??>
              mkdir -p ${spill_local_storage_dir}
              chown -R ${runAsUser}:${runAsGroup} ${spill_local_storage_dir}
              echo "创建CN溢出存储目录: ${spill_local_storage_dir}"
              </#if>
              
              # 块缓存目录
              <#if block_cache_disk_path??>
              mkdir -p ${block_cache_disk_path}
              chown -R ${runAsUser}:${runAsGroup} ${block_cache_disk_path}
              echo "创建CN块缓存目录: ${block_cache_disk_path}"
              </#if>
              
              # 日志目录
              mkdir -p ${appHome}/log
              chown -R ${runAsUser}:${runAsGroup} ${appHome}/log
              echo "创建CN日志目录: ${appHome}/log"
              
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
            - name: srcn-data
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
              ${startCommand}
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
          readinessProbe:
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
            - name: srcn-data
              mountPath: ${mount_path}
              subPathExpr: $(POD_NAME)
            <#list volumeConfigMapSet as item>
            - name: "${item.name}"
              mountPath: "${item.value}"
              subPath: "${item.fileName}"
            </#list>
            - name: "timezone"
              mountPath: "/etc/localtime"
      terminationGracePeriodSeconds: 30
      volumes:
        - name: srcn-data
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
