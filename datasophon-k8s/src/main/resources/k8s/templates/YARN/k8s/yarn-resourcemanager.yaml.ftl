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
              echo "Setting permissions for ResourceManager PVC mount path..."
              chmod -R 777 ${mount_path}
              echo "Permissions set successfully"
          securityContext:
            runAsUser: 0  # 以root用户运行
            privileged: true
          volumeMounts:
            - name: yarn-data
              mountPath: ${mount_path}
              subPathExpr: $(POD_NAMESPACE)/$(POD_NAME)
        - name: wait-for-zookeeper
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
              NC='\033[0m' # No Color
              CHECK_MARK="✅"
              WARNING="⚠️"
              ERROR="❌"
              INFO="ℹ️"
              PROGRESS="🔄"
              
              echo -e "$BLUE$INFO 开始检查ZooKeeper集群状态...$NC"
              
              # 使用从配置中获取的ZooKeeper地址
              <#if yarn_resourcemanager_zk_address??>
              ZK_QUORUM="${yarn_resourcemanager_zk_address}"
              <#else>
              echo -e "$RED$ERROR 错误: 配置中未提供ZooKeeper地址(yarn.resourcemanager.zk-address)，无法继续$NC"
              exit 1
              </#if>
              
              echo -e "$BLUE$INFO ZooKeeper地址: $ZK_QUORUM$NC"
              
              # 分割ZooKeeper地址并检查每个实例
              OLD_IFS="$IFS"
              IFS=","
              ZK_AVAILABLE=0
              ZK_TOTAL=0
              
              for ZK_SERVER in $ZK_QUORUM; do
                IFS="$OLD_IFS"
                ZK_TOTAL=$((ZK_TOTAL+1))
                IFS=","
              done
              IFS="$OLD_IFS"
              
              # 计算所需的最小存活数量（过半）
              MIN_AVAILABLE=$(( (ZK_TOTAL + 1) / 2 ))
              echo -e "$BLUE$INFO 需要至少 $MIN_AVAILABLE 个ZooKeeper实例可用（总实例数: $ZK_TOTAL）$NC"
              
              # 检查每个ZooKeeper实例
              IFS=","
              for ZK_SERVER in $ZK_QUORUM; do
                IFS="$OLD_IFS"
                HOST=$(echo $ZK_SERVER | cut -d':' -f1)
                PORT=$(echo $ZK_SERVER | cut -d':' -f2)
                if [ -z "$PORT" ]; then
                  PORT=2181  # 默认ZooKeeper端口
                fi
                
                echo -e "$BLUE$INFO 正在检查ZooKeeper服务: $HOST:$PORT$NC"
                
                # 重试计数器
                RETRIES=0
                MAX_RETRIES=60
                
                # 循环尝试连接ZooKeeper
                while [ $RETRIES -lt $MAX_RETRIES ]; do
                  if nc -z -w 2 $HOST $PORT; then
                    echo -e "$GREEN$CHECK_MARK ZooKeeper服务 $HOST:$PORT 已就绪$NC"
                    ZK_AVAILABLE=$((ZK_AVAILABLE+1))
                    break
                  else
                    echo -e "$YELLOW$PROGRESS ZooKeeper服务 $HOST:$PORT 未就绪，等待重试... ($((RETRIES+1))/$MAX_RETRIES)$NC"
                    RETRIES=$((RETRIES+1))
                    sleep 2
                  fi
                done
                
                # 检查是否达到最大重试次数
                if [ $RETRIES -eq $MAX_RETRIES ]; then
                  echo -e "$RED$WARNING ZooKeeper服务 $HOST:$PORT 在$MAX_RETRIES次尝试后仍未就绪$NC"
                fi
              done
              
              # 检查是否有足够的ZooKeeper实例可用
              echo -e "$BLUE$INFO ZooKeeper可用性: $ZK_AVAILABLE/$ZK_TOTAL$NC"
              if [ $ZK_AVAILABLE -lt $MIN_AVAILABLE ]; then
                echo -e "$RED$ERROR 错误: 可用的ZooKeeper实例数量($ZK_AVAILABLE)小于所需的最小数量($MIN_AVAILABLE)，无法继续初始化ResourceManager$NC"
                exit 1
              else
                echo -e "$GREEN$CHECK_MARK ZooKeeper集群状态正常，继续初始化ResourceManager$NC"
              fi
          volumeMounts:
            <#list volumeConfigMapSet as item>
            - name: "${item.name}"
              mountPath: "${item.value}"
              subPath: "${item.fileName}"
            </#list>
            - name: "timezone"
              mountPath: "/etc/localtime"
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
              HOSTNAME=$(hostname)
              if ${enableKerberos}; then
                echo "Kerberos is enabled. Performing Kerberos setup...";
                if [ ! -f /etc/security/keytab/keystore ]; then
                  cd /opt/datasophon/script && sh keystore.sh $HOSTNAME
                fi
                if [ ! -f ${appHome}/etc/hadoop/ssl-client.xml ]; then
                  echo "ssl-client.xml not found. Copying from template...";
                  cp ${appHome}/etc/hadoop/ssl-client.xml.template ${appHome}/etc/hadoop/ssl-client.xml
                fi
                if [ ! -f ${appHome}/etc/hadoop/ssl-server.xml ]; then
                  echo "ssl-server.xml not found. Copying from template...";
                  cp ${appHome}/etc/hadoop/ssl-server.xml.template ${appHome}/etc/hadoop/ssl-server.xml
                fi
                 su - hdfs -c "kinit -kt /etc/security/keytab/spnego.service.keytab HTTP/$HOSTNAME@HADOOP.COM"
                 su - hdfs -c "kinit -kt /etc/security/keytab/hdfs.user.keytab hdfs/user@HADOOP.COM"
              else
                echo "Kerberos is not enabled. Skipping Kerberos setup.";
              fi
              su - hdfs -c "${appHome}/bin/hdfs dfs -test -e /user/yarn" \
                || (su - hdfs -c "${appHome}/bin/hdfs dfs -mkdir -p /user/yarn" \
                && su - hdfs -c "${appHome}/bin/hdfs dfs -chown yarn:hadoop /user/yarn")
              if ${enableRangerPlugin}; then
                echo "Ranger plugin is enabled. Performing Ranger setup...";
                cd ${appHome}/ranger-yarn-plugin && \
                sh ${appHome}/ranger-yarn-plugin/enable-yarn-plugin.sh
              else
                echo "Ranger plugin is not enabled. Skipping Ranger setup.";
              fi
              ${startCommand}
          readinessProbe:
            tcpSocket:
              port: 8088
            failureThreshold: 3
            initialDelaySeconds: 10
            periodSeconds: 10
            successThreshold: 1
            timeoutSeconds: 1
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
            - name: yarn-data
              mountPath: ${mount_path}
              subPathExpr: $(POD_NAMESPACE)/$(POD_NAME)
            <#list volumeConfigMapSet as item>
            - name: "${item.name}"
              mountPath: "${item.value}"
              subPath: "${item.fileName}"
            </#list>
            - name: "timezone"
              mountPath: "/etc/localtime"
      terminationGracePeriodSeconds: 30
      volumes:
        - name: yarn-data
          persistentVolumeClaim:
            claimName: "${serviceRoleFullName}-pvc"
        - name: "timezone"
          hostPath:
            path: "/etc/localtime"