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
        - name: wait-for-db
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
                nc -z -w 3 $HOST $PORT >/dev/null 2>&1
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
        <#if isInstall?? && isInstall>
        - name: hive-schema-init
          image: "${dockerImage}"
          imagePullPolicy: Always
          env:
            - name: USER
              value: ${runAsUser}
            - name: POD_NAME
              valueFrom:
                fieldRef:
                  fieldPath: metadata.name
          command:
            - "/bin/bash"
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

              echo -e "$BLUE$INFO [Hive Metastore] 启动 Schema 初始化检查...$NC"
              POD_INDEX=$(echo $POD_NAME | awk -F'-' '{print $NF}')

              # 只有 leader pod (index 0) 才执行初始化
              if [ "$POD_INDEX" != "0" ]; then
                echo -e "$INFO 当前 Pod ($POD_NAME) 不是 leader, 跳过 schema 初始化。$NC"
                exit 0
              fi

              echo -e "$INFO 当前 Pod ($POD_NAME) 是 leader, 准备执行 schema 初始化...$NC"

              # 从JDBC URL中提取数据库类型 (e.g., jdbc:mysql://... -> mysql)
              DB_TYPE=$(echo "${db_connection_url}" | awk -F':' '{print $2}')
              if [ -z "$DB_TYPE" ]; then
                  echo -e "$RED$ERROR 无法从 JDBC URL '${db_connection_url}' 中解析数据库类型。$NC"
                  exit 1
              fi
              echo -e "$INFO 检测到数据库类型为: $DB_TYPE$NC"

              echo -e "$INFO 执行初始化命令: schematool -dbType $DB_TYPE -initSchema$NC"
              set -x # for debugging
              su - ${runAsUser} -c "${appHome}/bin/schematool -dbType $DB_TYPE -initSchema"
              INIT_RESULT=$?
              set +x

              if [ $INIT_RESULT -eq 0 ]; then
                echo -e "$GREEN$CHECK_MARK [Hive Metastore] Schema 初始化成功。$NC"
              else
                echo -e "$YELLOW WARNING: schematool 命令退出，代码: $INIT_RESULT. 这可能是因为 schema 已存在，通常是安全的。$NC"
              fi

              echo -e "$BLUE$INFO [Hive Metastore] Schema 初始化步骤完成。$NC"
              exit 0
          volumeMounts:
            <#list volumeConfigMapSet as item>
            - name: "${item.name}"
              mountPath: "${item.value}"
              subPath: "${item.fileName}"
            </#list>
            - name: "timezone"
              mountPath: "/etc/localtime"
        </#if>
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