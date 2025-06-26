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
        - name: init-hdfs-hive-dirs
          image: ${dockerImage}
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
              WARNING="⚠️"
              
              # 1. 验证配置文件
              APP_HOME="${appHome}"
              CONFIG_FILE="$APP_HOME/conf/hive-site.xml"
              
              if [ ! -f "$CONFIG_FILE" ]; then
                echo -e "$YELLOW$WARNING 未找到Hive配置文件: $CONFIG_FILE, 尝试其他位置$NC"
                FOUND=0
                for TRY_PATH in "/etc/hive/conf/hive-site.xml" "$APP_HOME/conf/hivemetastore-site.xml"; do
                  if [ -f "$TRY_PATH" ]; then
                    CONFIG_FILE="$TRY_PATH"
                    FOUND=1
                    echo -e "$INFO 找到配置文件: $CONFIG_FILE"
                    break
                  fi
                done
                if [ "$FOUND" = "0" ]; then
                  echo -e "$RED$ERROR 致命错误: 无法找到Hive配置文件$NC"
                  exit 1
                fi
              fi
              
              # 2. 提取数据库参数
              echo -e "$INFO 正在从配置文件提取数据库参数...$NC"
              DB_URL=$(xmllint --xpath "string(//property[name='javax.jdo.option.ConnectionURL']/value)" $CONFIG_FILE 2>/dev/null)
              DB_USER=$(xmllint --xpath "string(//property[name='javax.jdo.option.ConnectionUserName']/value)" $CONFIG_FILE 2>/dev/null)
              DB_PASS=$(xmllint --xpath "string(//property[name='javax.jdo.option.ConnectionPassword']/value)" $CONFIG_FILE 2>/dev/null)
              
              if [ -z "$DB_URL" ]; then
                echo -e "$RED$ERROR 致命错误: 配置文件中未找到数据库URL$NC"
                exit 1
              fi
              
              if [ -z "$DB_USER" ]; then
                echo -e "$RED$ERROR 致命错误: 配置文件中未找到数据库用户名$NC"
                exit 1
              fi
              
              # 3. 解析数据库参数
              HOST=$(echo $DB_URL | sed -n 's/.*:\/\/\([^:\/]*\).*/\1/p')
              PORT=$(echo $DB_URL | sed -n 's/.*:\([0-9]*\)\/.*/\1/p')
              DB_NAME=$(echo $DB_URL | sed -n 's/.*\/\([^?]*\).*/\1/p' | sed 's/;//g')
              
              if [ -z "$HOST" ] || [ -z "$PORT" ] || [ -z "$DB_NAME" ]; then
                echo -e "$RED$ERROR 致命错误: 无法解析数据库连接参数$NC"
                exit 1
              fi
              
              # 4. 检查mysql客户端
              if ! command -v mysql >/dev/null 2>&1; then
                echo -e "$RED$ERROR 致命错误: MySQL客户端不可用$NC"
                exit 1
              fi
              
              # 5. 分布式锁处理
              LOCK_TABLE="ddp_hive_hdfs_init_lock"
              LOCK_KEY="hive_hdfs_dirs_init"
              
              echo -e "$INFO 创建锁表(如果不存在)...$NC"
              mysql -h $HOST -P $PORT -u "$DB_USER" -p"$DB_PASS" "$DB_NAME" -e "CREATE TABLE IF NOT EXISTS $LOCK_TABLE (lock_key VARCHAR(255) PRIMARY KEY, status VARCHAR(50), pod_name VARCHAR(255), updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP);" 2>/dev/null
              
              if [ $? -ne 0 ]; then
                echo -e "$RED$ERROR 致命错误: 创建锁表失败$NC"
                exit 1
              fi
              
              # 检查是否已完成初始化
              STATUS=$(mysql -h $HOST -P $PORT -u "$DB_USER" -p"$DB_PASS" "$DB_NAME" -N -B -e "SELECT status FROM $LOCK_TABLE WHERE lock_key = '$LOCK_KEY';" 2>/dev/null)
              if [ "$STATUS" = "complete" ]; then
                echo -e "$GREEN$CHECK_MARK 检测到HDFS目录已初始化完成，跳过$NC"
                exit 0
              fi
              
              # 6. 尝试获取锁
              echo -e "$INFO 尝试获取锁...$NC"
              mysql -h $HOST -P $PORT -u "$DB_USER" -p"$DB_PASS" "$DB_NAME" -e "INSERT INTO $LOCK_TABLE (lock_key, status, pod_name) VALUES ('$LOCK_KEY', 'initializing', '$HOSTNAME');" 2>/dev/null
              
              if [ $? -eq 0 ]; then
                echo -e "$GREEN$CHECK_MARK 成功获取锁，将执行HDFS目录初始化$NC"
                LOCK_ACQUIRED=1
              else
                echo -e "$INFO 未能获取锁，等待其他Pod完成初始化...$NC"
                for i in $(seq 1 60); do
                  echo -e "$YELLOW ... 等待其他Pod完成HDFS目录初始化 (尝试 $i/60)$NC"
                  STATUS=$(mysql -h $HOST -P $PORT -u "$DB_USER" -p"$DB_PASS" "$DB_NAME" -N -B -e "SELECT status FROM $LOCK_TABLE WHERE lock_key = '$LOCK_KEY';" 2>/dev/null)
                  if [ "$STATUS" = "complete" ]; then
                    echo -e "$GREEN$CHECK_MARK 检测到HDFS目录已初始化完成，跳过$NC"
                    exit 0
                  fi
                  sleep 5
                done
                echo -e "$YELLOW$WARNING 等待超时，退出$NC"
                exit 0
              fi
              
              # 7. 寻找HDFS命令
              echo -e "$INFO 寻找HDFS命令...$NC"
              if [ -n "$HADOOP_HOME" ]; then
                HDFS_CMD="$HADOOP_HOME/bin/hdfs"
              elif command -v hdfs >/dev/null 2>&1; then
                HDFS_CMD=$(command -v hdfs)
              else
                for DIR in "/opt/hadoop" "/usr/local/hadoop" "/usr/lib/hadoop" "$APP_HOME/../hadoop"; do
                  if [ -f "$DIR/bin/hdfs" ]; then
                    HDFS_CMD="$DIR/bin/hdfs"
                    break
                  fi
                done
              fi
              
              if [ -z "$HDFS_CMD" ] || [ ! -f "$HDFS_CMD" ]; then
                echo -e "$RED$ERROR 致命错误: 无法找到HDFS命令$NC"
                mysql -h $HOST -P $PORT -u "$DB_USER" -p"$DB_PASS" "$DB_NAME" -e "DELETE FROM $LOCK_TABLE WHERE lock_key = '$LOCK_KEY';" 2>/dev/null
                exit 1
              fi
              
              # 8. 提取Hive目录路径
              echo -e "$INFO 提取Hive目录路径...$NC"
              WAREHOUSE_DIR=$(xmllint --xpath "string(//property[name='hive.metastore.warehouse.dir']/value)" $CONFIG_FILE 2>/dev/null)
              SCRATCH_DIR=$(xmllint --xpath "string(//property[name='hive.exec.scratch.dir']/value)" $CONFIG_FILE 2>/dev/null)
              TMP_DIR=$(xmllint --xpath "string(//property[name='hive.exec.temporary.table.storage']/value)" $CONFIG_FILE 2>/dev/null)
              
              if [ -z "$WAREHOUSE_DIR" ]; then
                WAREHOUSE_DIR="/user/hive/warehouse"
                echo -e "$YELLOW$WARNING 未找到warehouse目录配置，使用默认值: $WAREHOUSE_DIR$NC"
              fi
              
              if [ -z "$SCRATCH_DIR" ]; then
                SCRATCH_DIR="/tmp/hive/scratch"
                echo -e "$YELLOW$WARNING 未找到scratch目录配置，使用默认值: $SCRATCH_DIR$NC"
              fi
              
              # 9. 创建和检查目录
              echo -e "$INFO 开始检查和创建HDFS目录...$NC"
              # 检查warehouse目录
              echo -e "$INFO 检查目录: $WAREHOUSE_DIR$NC"
              if ! $HDFS_CMD dfs -test -d "$WAREHOUSE_DIR" 2>/dev/null; then
                echo -e "$INFO 创建目录: $WAREHOUSE_DIR$NC"
                $HDFS_CMD dfs -mkdir -p "$WAREHOUSE_DIR"
                if [ $? -ne 0 ]; then
                  echo -e "$RED$ERROR 创建目录失败: $WAREHOUSE_DIR$NC"
                  mysql -h $HOST -P $PORT -u "$DB_USER" -p"$DB_PASS" "$DB_NAME" -e "DELETE FROM $LOCK_TABLE WHERE lock_key = '$LOCK_KEY';" 2>/dev/null
                  exit 1
                fi
              else
                echo -e "$GREEN$CHECK_MARK 目录已存在: $WAREHOUSE_DIR$NC"
              fi
              echo -e "$INFO 设置权限: $WAREHOUSE_DIR$NC"
              echo -e "$INFO 执行命令: $HDFS_CMD dfs -chmod 777 $WAREHOUSE_DIR$NC"
              $HDFS_CMD dfs -chmod 777 "$WAREHOUSE_DIR"
              if [ $? -ne 0 ]; then
                echo -e "$RED$ERROR 设置权限失败: $WAREHOUSE_DIR$NC"
                mysql -h $HOST -P $PORT -u "$DB_USER" -p"$DB_PASS" "$DB_NAME" -e "DELETE FROM $LOCK_TABLE WHERE lock_key = '$LOCK_KEY';" 2>/dev/null
                exit 1
              fi
              echo -e "$INFO 执行命令: $HDFS_CMD dfs -chmod g+w $WAREHOUSE_DIR$NC"
              $HDFS_CMD dfs -chmod g+w "$WAREHOUSE_DIR"
              if [ $? -ne 0 ]; then
                echo -e "$RED$ERROR 设置组写权限失败: $WAREHOUSE_DIR$NC"
                mysql -h $HOST -P $PORT -u "$DB_USER" -p"$DB_PASS" "$DB_NAME" -e "DELETE FROM $LOCK_TABLE WHERE lock_key = '$LOCK_KEY';" 2>/dev/null
                exit 1
              fi
              echo -e "$INFO 当前权限: $($HDFS_CMD dfs -ls -d $WAREHOUSE_DIR | awk '{print $1,$3,$4}')$NC"
              
              # 检查scratch目录
              if [ -n "$SCRATCH_DIR" ]; then
                echo -e "$INFO 检查目录: $SCRATCH_DIR$NC"
                if ! $HDFS_CMD dfs -test -d "$SCRATCH_DIR" 2>/dev/null; then
                  echo -e "$INFO 创建目录: $SCRATCH_DIR$NC"
                  $HDFS_CMD dfs -mkdir -p "$SCRATCH_DIR"
                  if [ $? -ne 0 ]; then
                    echo -e "$RED$ERROR 创建目录失败: $SCRATCH_DIR$NC"
                    mysql -h $HOST -P $PORT -u "$DB_USER" -p"$DB_PASS" "$DB_NAME" -e "DELETE FROM $LOCK_TABLE WHERE lock_key = '$LOCK_KEY';" 2>/dev/null
                    exit 1
                  fi
                else
                  echo -e "$GREEN$CHECK_MARK 目录已存在: $SCRATCH_DIR$NC"
                fi
                echo -e "$INFO 设置权限: $SCRATCH_DIR$NC"
                echo -e "$INFO 执行命令: $HDFS_CMD dfs -chmod 777 $SCRATCH_DIR$NC"
                $HDFS_CMD dfs -chmod 777 "$SCRATCH_DIR"
                if [ $? -ne 0 ]; then
                  echo -e "$RED$ERROR 设置权限失败: $SCRATCH_DIR$NC"
                  mysql -h $HOST -P $PORT -u "$DB_USER" -p"$DB_PASS" "$DB_NAME" -e "DELETE FROM $LOCK_TABLE WHERE lock_key = '$LOCK_KEY';" 2>/dev/null
                  exit 1
                fi
                echo -e "$INFO 当前权限: $($HDFS_CMD dfs -ls -d $SCRATCH_DIR | awk '{print $1,$3,$4}')$NC"
              fi
              
              # 检查tmp目录
              if [ -n "$TMP_DIR" ]; then
                echo -e "$INFO 检查目录: $TMP_DIR$NC"
                if ! $HDFS_CMD dfs -test -d "$TMP_DIR" 2>/dev/null; then
                  echo -e "$INFO 创建目录: $TMP_DIR$NC"
                  $HDFS_CMD dfs -mkdir -p "$TMP_DIR"
                  if [ $? -ne 0 ]; then
                    echo -e "$RED$ERROR 创建目录失败: $TMP_DIR$NC"
                    mysql -h $HOST -P $PORT -u "$DB_USER" -p"$DB_PASS" "$DB_NAME" -e "DELETE FROM $LOCK_TABLE WHERE lock_key = '$LOCK_KEY';" 2>/dev/null
                    exit 1
                  fi
                else
                  echo -e "$GREEN$CHECK_MARK 目录已存在: $TMP_DIR$NC"
                fi
                echo -e "$INFO 设置权限: $TMP_DIR$NC"
                echo -e "$INFO 执行命令: $HDFS_CMD dfs -chmod 777 $TMP_DIR$NC"
                $HDFS_CMD dfs -chmod 777 "$TMP_DIR"
                if [ $? -ne 0 ]; then
                  echo -e "$RED$ERROR 设置权限失败: $TMP_DIR$NC"
                  mysql -h $HOST -P $PORT -u "$DB_USER" -p"$DB_PASS" "$DB_NAME" -e "DELETE FROM $LOCK_TABLE WHERE lock_key = '$LOCK_KEY';" 2>/dev/null
                  exit 1
                fi
                echo -e "$INFO 当前权限: $($HDFS_CMD dfs -ls -d $TMP_DIR | awk '{print $1,$3,$4}')$NC"
              fi
              
              # 检查/tmp/hive目录
              echo -e "$INFO 检查目录: /tmp/hive$NC"
              if ! $HDFS_CMD dfs -test -d "/tmp/hive" 2>/dev/null; then
                echo -e "$INFO 创建目录: /tmp/hive$NC"
                $HDFS_CMD dfs -mkdir -p "/tmp/hive"
                if [ $? -ne 0 ]; then
                  echo -e "$RED$ERROR 创建目录失败: /tmp/hive$NC"
                  mysql -h $HOST -P $PORT -u "$DB_USER" -p"$DB_PASS" "$DB_NAME" -e "DELETE FROM $LOCK_TABLE WHERE lock_key = '$LOCK_KEY';" 2>/dev/null
                  exit 1
                fi
              else
                echo -e "$GREEN$CHECK_MARK 目录已存在: /tmp/hive$NC"
              fi
              echo -e "$INFO 设置权限: /tmp/hive$NC"
              echo -e "$INFO 执行命令: $HDFS_CMD dfs -chmod 777 /tmp/hive$NC"
              $HDFS_CMD dfs -chmod 777 "/tmp/hive"
              if [ $? -ne 0 ]; then
                echo -e "$RED$ERROR 设置权限失败: /tmp/hive$NC"
                mysql -h $HOST -P $PORT -u "$DB_USER" -p"$DB_PASS" "$DB_NAME" -e "DELETE FROM $LOCK_TABLE WHERE lock_key = '$LOCK_KEY';" 2>/dev/null
                exit 1
              fi
              echo -e "$INFO 当前权限: $($HDFS_CMD dfs -ls -d /tmp/hive | awk '{print $1,$3,$4}')$NC"
              
              # 10. 更新锁状态
              echo -e "$INFO 更新锁状态为完成...$NC"
              mysql -h $HOST -P $PORT -u "$DB_USER" -p"$DB_PASS" "$DB_NAME" -e "UPDATE $LOCK_TABLE SET status = 'complete' WHERE lock_key = '$LOCK_KEY';" 2>/dev/null
              
              echo -e "$GREEN$CHECK_MARK HDFS目录初始化完成$NC"
              exit 0
          volumeMounts:
            <#list volumeConfigMapSet as item>
            - name: "${item.name}"
              mountPath: "${item.value}"
              subPath: "${item.fileName}"
            </#list>
            - name: "timezone"
              mountPath: "/etc/localtime"
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
              
              success=0
              for host_port in $HOST_PORTS; do
                host=$(echo $host_port | cut -d: -f1)
                port=$(echo $host_port | cut -d: -f2)
                echo -e "$YELLOW 正在检查 $host:$port ...$NC"
                
                # 循环等待，最多等待300秒
                for i in $(seq 1 60); do
                  nc -z -w 3 $host $port >/dev/null 2>&1
                  if [ $? -eq 0 ]; then
                    echo -e "$GREEN$CHECK_MARK $host:$port 连接成功!$NC"
                    success=1
                    break
                  else
                    echo -e "$YELLOW ... 第 $i 次尝试, $host:$port 仍在等待中...$NC"
                    sleep 5
                  fi
                  if [ $i -eq 60 ]; then
                    echo -e "$YELLOW$INFO 警告: 等待 $host:$port 超时 (300秒)，此节点可能不可用$NC"
                    break
                  fi
                done
              done
              
              if [ $success -eq 1 ]; then
                echo -e "$GREEN$CHECK_MARK 至少有一个HiveMetaStore服务已准备就绪，可以继续启动!$NC"
                exit 0
              else
                echo -e "$RED$ERROR 错误: 所有HiveMetaStore服务均无法连接，无法启动HiveServer2!$NC"
                exit 1
              fi
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