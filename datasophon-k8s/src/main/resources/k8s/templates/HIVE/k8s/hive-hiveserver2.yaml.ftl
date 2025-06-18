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
        - name: wait-for-metastore
          image: "${dockerBusyboxImage}"
          imagePullPolicy: Always
          command:
            - "/bin/sh"
            - "-c"
            - |
              # 定义颜色和图标
              RED='\033[0;31m'
              GREEN='\033[0;32m'
              YELLOW='\033[1;33m'
              BLUE='\033[0;34m'
              NC='\033[0m'
              CHECK_MARK="✅"
              ERROR="❌"
              INFO="ℹ️"

              echo -e "$BLUE$INFO 开始检查HiveMetaStore服务状态...$NC"

              # 从配置中提取Metastore URI
              <#if metastore_uris??>
              METASTORE_URIS="${metastore_uris}"
              # 格式: thrift://host:port,thrift://host2:port2 -> host:port host2:port2
              HOST_PORTS=$(echo $METASTORE_URIS | sed -e 's/thrift:\/\///g' -e 's/,/ /g')
              <#else>
              echo -e "$RED$ERROR 错误: 配置中未提供 hive.metastore.uris，无法继续$NC"
              exit 1
              </#if>

              echo -e "$INFO 目标Metastore地址: $HOST_PORTS"

              for host_port in $HOST_PORTS; do
                host=$(echo $host_port | cut -d: -f1)
                port=$(echo $host_port | cut -d: -f2)
                echo -e "$YELLOW 正在检查 $host:$port ...$NC"
                
                # 循环等待，最多等待300秒
                for i in $(seq 1 60); do
                  nc -z -w 3 $host $port >/dev/null 2>&1
                  if [ $? -eq 0 ]; then
                    echo -e "$GREEN$CHECK_MARK $host:$port 连接成功!$NC"
                    break
                  else
                    echo -e "$YELLOW ... 第 $i 次尝试, $host:$port 仍在等待中...$NC"
                    sleep 5
                  fi
                  if [ $i -eq 60 ]; then
                    echo -e "$RED$ERROR 错误: 等待 $host:$port 超时 (300秒)，请检查Metastore服务状态!$NC"
                    exit 1
                  fi
                done
              done

              echo -e "$BLUE$INFO 所有HiveMetaStore服务均已准备就绪!$NC"
      containers:
        - name: ${serviceRoleFullName}
          image: ${dockerImage}
          imagePullPolicy: Always
          env:
            - name: POD_NAME
              valueFrom:
                fieldRef:
                  fieldPath: metadata.name
            - name: POD_NAMESPACE
              valueFrom:
                fieldRef:
                  fieldPath: metadata.namespace
            - name: HOST_IP
              valueFrom:
                fieldRef:
                  fieldPath: status.hostIP
          command: ["/bin/sh", "-c", "${startCommand}"]
          resources:
            requests:
              memory: ${requests_memory}
              cpu: ${requests_cpu}
            limits:
              memory: ${limits_memory}
              cpu: ${limits_cpu}
          volumeMounts:
            <#list volumeConfigMapSet as item>
            - name: "${item.name}"
              mountPath: "${item.value}"
              subPath: "${item.fileName}"
            </#list>
            - name: "timezone"
              mountPath: "/etc/localtime"
      volumes:
        <#list volumeConfigMapSet as item>
        - name: "${item.name}"
          configMap:
            name: "${item.name}"
        </#list>
        - name: "timezone"
          hostPath:
            path: "/etc/localtime"