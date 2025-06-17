apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: ${serviceRoleFullName}
  namespace: ${namespace}
spec:
  replicas: ${roleNodeCnt}
  selector:
    matchLabels:
      app: ${serviceRoleFullName}
  serviceName: ${serviceRoleFullName}
  template:
    metadata:
      labels:
        app: ${serviceRoleFullName}
    spec:
      initContainers:
        - name: wait-for-db
          image: "${dockerBusyboxImage}"
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

              echo -e "$BLUE$INFO 开始检查后端数据库连接...$NC"

              # 从配置中提取数据库连接URL
              <#if db_connection_url??>
              DB_URL="${db_connection_url}"
              # 格式: jdbc:mysql://host:port/dbname... -> host port
              HOST=$(echo $DB_URL | sed -n 's/.*:\/\/\(.*\):.*/\1/p')
              PORT=$(echo $DB_URL | sed -n 's/.*:\([0-9]*\)\/.*/\1/p')
              <#else>
              echo -e "$RED$ERROR 错误: 配置中未提供 javax.jdo.option.ConnectionURL，无法继续$NC"
              exit 1
              </#if>
              
              if [ -z "$HOST" ] || [ -z "$PORT" ]; then
                  echo -e "$RED$ERROR 错误: 无法从 '$DB_URL' 解析数据库主机或端口$NC"
                  exit 1
              fi

              echo -e "$INFO 目标数据库地址: $HOST:$PORT"
              
              # 循环等待，最多等待300秒
              for i in $(seq 1 60); do
                (echo > /dev/tcp/$HOST/$PORT) >/dev/null 2>&1
                if [ $? -eq 0 ]; then
                  echo -e "$GREEN$CHECK_MARK $HOST:$PORT 连接成功!$NC"
                  break
                else
                  echo -e "$YELLOW ... 第 $i 次尝试, $HOST:$PORT 仍在等待中...$NC"
                  sleep 5
                fi
                if [ $i -eq 60 ]; then
                  echo -e "$RED$ERROR 错误: 等待 $HOST:$PORT 超时 (300秒)，请检查数据库服务状态!$NC"
                  exit 1
                fi
              done
              
              echo -e "$BLUE$INFO 数据库已准备就绪!$NC"
      containers:
        - name: ${serviceRoleFullName}
          image: ${dockerImage}
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