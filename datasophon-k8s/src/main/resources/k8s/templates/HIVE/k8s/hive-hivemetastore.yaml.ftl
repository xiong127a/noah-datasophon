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
              
              # 从Secret挂载的文件中读取数据库连接信息
              DB_HOST=$(cat /etc/hive-db-secret/db-host)
              DB_PORT=$(cat /etc/hive-db-secret/db-port)
              
              if [ -z "$DB_HOST" ] || [ -z "$DB_PORT" ]; then
                  echo -e "$RED$ERROR 错误: 无法获取数据库主机或端口信息$NC"
                  exit 1
              fi

              echo -e "$INFO 目标数据库地址: $DB_HOST:$DB_PORT"
              
              # 循环等待，最多等待300秒
              for i in $(seq 1 60); do
                nc -z -w 3 $DB_HOST $DB_PORT >/dev/null 2>&1
                if [ $? -eq 0 ]; then
                  echo -e "$GREEN$CHECK_MARK $DB_HOST:$DB_PORT 连接成功!$NC"
                  break
                else
                  echo -e "$YELLOW ... 第 $i 次尝试, $DB_HOST:$DB_PORT 仍在等待中...$NC"
                  sleep 5
                fi
                if [ $i -eq 60 ]; then
                  echo -e "$RED$ERROR 错误: 等待 $DB_HOST:$DB_PORT 超时 (300秒)，请检查数据库服务状态!$NC"
                  exit 1
                fi
              done
              
              echo -e "$BLUE$INFO 数据库已准备就绪!$NC"
          volumeMounts:
            - name: db-creds
              mountPath: /etc/hive-db-secret
              readOnly: true
        <#if isInstall?? && isInstall>
        # InitContainer 2: Use a database lock to elect a leader for schema initialization.
        - name: initialize-schema-with-db-lock
          image: "${dockerImage}" # Use the main hive image
          imagePullPolicy: Always
          env:
            - name: USER
              value: ${runAsUser}
          command:
            - "/bin/sh"
            - "-c"
            - |
              # 将FreeMarker变量赋值给Shell变量
              _APP_HOME="${appHome}"
              _RUN_AS_USER="${runAsUser}"
              <#noparse>
              # 定义颜色和图标
              NC='\033[0m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; BLUE='\033[0;34m'; INFO="ℹ️"; ERROR="❌"; CHECK_MARK="✅"

              echo -e "$BLUE$INFO 开始使用数据库锁进行Schema初始化...$NC"
              
              # 从Secret挂载的文件中读取数据库连接信息
              DB_HOST=$(cat /etc/hive-db-secret/db-host)
              DB_PORT=$(cat /etc/hive-db-secret/db-port)
              DB_NAME=$(cat /etc/hive-db-secret/db-name)
              DB_USER=$(cat /etc/hive-db-secret/db-user)
              DB_PASS=$(cat /etc/hive-db-secret/db-password)
              DB_TYPE=$(cat /etc/hive-db-secret/db-type)

              if [ -z "$DB_HOST" ] || [ -z "$DB_PORT" ] || [ -z "$DB_NAME" ] || [ -z "$DB_USER" ] || [ -z "$DB_PASS" ] || [ -z "$DB_TYPE" ]; then
                echo -e "$ERROR 致命错误: 数据库Secret中缺少必要信息，无法继续$NC"
                exit 1
              fi
              
              echo -e "$INFO 已获取数据库连接: $DB_TYPE://$DB_HOST:$DB_PORT/$DB_NAME$NC"

              LOCK_TABLE="ddp_hive_init_lock"
              LOCK_KEY="hive_metastore_schema_init"

              echo -e "$BLUE$INFO 尝试创建锁表 '$LOCK_TABLE' (如果不存在)...$NC"
              mysql -h $DB_HOST -P $DB_PORT -u "$DB_USER" -p"$DB_PASS" "$DB_NAME" -e "CREATE TABLE IF NOT EXISTS ${LOCK_TABLE} (lock_key VARCHAR(255) PRIMARY KEY, status VARCHAR(50), pod_name VARCHAR(255), updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP);"
              
              echo -e "$BLUE$INFO Pod ($HOSTNAME) 正在尝试获取初始化锁...$NC"
              # 尝试插入记录以获取锁
              mysql -h $DB_HOST -P $DB_PORT -u "$DB_USER" -p"$DB_PASS" "$DB_NAME" -e "INSERT INTO ${LOCK_TABLE} (lock_key, status, pod_name) VALUES ('${LOCK_KEY}', 'initializing', '$HOSTNAME');"
              
              # 检查插入是否成功
              if [ $? -eq 0 ]; then
                # 成功获取锁，执行初始化
                echo -e "$GREEN$CHECK_MARK 锁已被 $HOSTNAME 获取。开始执行Schema初始化...$NC"
                
                su - $_RUN_AS_USER -c "$_APP_HOME/bin/schematool -dbType $DB_TYPE -initSchema"
                if [ $? -eq 0 ]; then
                    echo -e "$GREEN$CHECK_MARK Schema初始化成功完成。$NC"
                else
                    # 即便命令失败，也可能是因为已经初始化过，这通常是可接受的
                    echo -e "$YELLOW 警告: schematool 命令执行失败。这可能是因为Schema已经初始化。此状态可以接受。$NC"
                fi
                
                # 更新状态，通知其他等待的Pod
                echo -e "$BLUE$INFO 正在更新锁状态以通知所有Pod初始化已完成...$NC"
                mysql -h $DB_HOST -P $DB_PORT -u "$DB_USER" -p"$DB_PASS" "$DB_NAME" -e "UPDATE ${LOCK_TABLE} SET status = 'complete' WHERE lock_key = '${LOCK_KEY}';"
                echo -e "$GREEN$CHECK_MARK 初始化过程已结束。$NC"
                exit 0
              else
                # 未能获取锁，进入等待状态
                echo -e "$YELLOW$INFO 未能获取锁。可能有其他Pod正在进行初始化。将进入等待状态...$NC"
                
                # 循环检查锁状态
                for i in $(seq 1 120); do
                  STATUS=$(mysql -h $DB_HOST -P $DB_PORT -u "$DB_USER" -p"$DB_PASS" "$DB_NAME" -N -B -e "SELECT status FROM ${LOCK_TABLE} WHERE lock_key = '${LOCK_KEY}';")
                  if [ "$STATUS" == "complete" ]; then
                    echo -e "$GREEN$CHECK_MARK 检测到 'complete' 状态。Schema已就绪。$NC"
                    exit 0
                  fi
                  echo -e "$YELLOW ... 正在等待 'complete' 状态 (尝试次数 $i/120)...$NC"
                  sleep 5
                done

                echo -e "$ERROR 致命错误: 等待Schema初始化完成超时。$NC"
                exit 1
              fi
              </#noparse>
          volumeMounts:
            <#list volumeConfigMapSet as item>
            - name: "${item.name}"
              mountPath: "${item.value}"
              subPath: "${item.fileName}"
            </#list>
            - name: "timezone"
              mountPath: "/etc/localtime"
            - name: db-creds
              mountPath: /etc/hive-db-secret
              readOnly: true
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
          command: ["/bin/sh", "-c", "
            # 定义颜色和图标
            RED='\\033[0;31m'
            GREEN='\\033[0;32m'
            BLUE='\\033[0;34m'
            YELLOW='\\033[1;33m'
            NC='\\033[0m' # No Color
            CHECK_MARK=\"✅\"
            INFO=\"ℹ️\"
            ERROR=\"❌\"
            WARNING=\"⚠️\"
            
            # 处理hosts文件，将主机hosts文件内容与Pod DNS配置合并
            echo -e \"$BLUE$INFO ========== 开始处理hosts文件 ===========$NC\"
            
            # 备份原始hosts文件
            cp /etc/hosts /tmp/original_hosts
            echo -e \"$BLUE$INFO 已备份原始hosts文件到/tmp/original_hosts$NC\"
            
            # 从主机hosts文件中提取有用条目 (通常是自定义的主机映射)
            if [ -f /tmp/host_etc_hosts ]; then
              echo -e \"$BLUE$INFO 从主机hosts文件提取有用条目...$NC\"
              grep -v '^127.0.0.1' /tmp/host_etc_hosts | grep -v '^::1' | grep -v '^#' >> /tmp/original_hosts
            else
              echo -e \"$YELLOW$WARNING 警告: 主机hosts文件未找到$NC\"
            fi
            
            # 应用合并后的hosts文件
            cat /tmp/original_hosts > /etc/hosts
            
            echo -e \"$BLUE$INFO 最终hosts文件内容:$NC\"
            cat /etc/hosts
            
            echo -e \"$BLUE$INFO 测试主机名解析:$NC\"
            echo -e \"$BLUE$INFO hostname: $(hostname)$NC\"
            echo -e \"$BLUE$INFO hostname -f: $(hostname -f 2>/dev/null || echo '无法获取FQDN')$NC\"
            
            echo -e \"$GREEN$CHECK_MARK ========== hosts文件处理完成 ===========$NC\"
            
            # 执行原始启动命令
            ${startCommand}
          "]
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
            - name: "hosts-file"
              mountPath: "/tmp/host_etc_hosts"
      volumes:
        <#list volumeConfigMapSet as item>
        - name: "${item.name}"
          configMap:
            name: "${item.name}"
        </#list>
        - name: "timezone"
          hostPath:
            path: "/etc/localtime"
        - name: db-creds
          secret:
            secretName: ${serviceRoleFullName}-db-secret
            defaultMode: 0400
        - name: "hosts-file"
          hostPath:
            path: "/etc/hosts"