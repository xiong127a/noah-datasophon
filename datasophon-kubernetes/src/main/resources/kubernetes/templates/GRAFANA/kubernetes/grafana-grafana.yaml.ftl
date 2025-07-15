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
        - name: prepare-data-dirs
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
              
              NFS_MOUNT="/tmp/grafana-nfs"  # PVC挂载点
              
              echo -e "$BLUE$INFO 开始准备Grafana NFS数据目录 $NC"
              
              mkdir -p $NFS_MOUNT/log
              mkdir -p $NFS_MOUNT/plugins
              mkdir -p $NFS_MOUNT/dashboards
              
              # 设置适当的权限
              chmod -R 755 $NFS_MOUNT
              
              echo -e "$GREEN$CHECK_MARK Grafana NFS数据目录准备完成 $NC"
          securityContext:
            runAsUser: 0  # 以root用户运行
            privileged: true
          volumeMounts:
            - name: grafana-data
              mountPath: /tmp/grafana-nfs
              subPathExpr: $(POD_NAME)
        - name: copy-initial-data
          image: "${dockerImage}"
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
          command:
            - "/bin/bash"
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
              
              SOURCE_DATA="${mount_path}"  # 容器内的源数据目录
              NFS_MOUNT="/tmp/grafana-nfs"  # PVC挂载点
              
              echo -e "$BLUE$INFO 检查NFS目录是否需要复制初始数据... $NC"
              
              # 检查NFS挂载点是否有数据
              if [ ! -f "$NFS_MOUNT/grafana.db" ]; then
                echo -e "$YELLOW$PROGRESS NFS中没有找到grafana.db，从$SOURCE_DATA复制初始数据 $NC"
                
                # 从容器内的默认路径复制数据
                if [ -d "$SOURCE_DATA" ]; then
                  # 创建必要的目录结构（如果不存在）
                  mkdir -p $NFS_MOUNT/log
                  mkdir -p $NFS_MOUNT/plugins
                  mkdir -p $NFS_MOUNT/dashboards
                  
                  # 复制数据到NFS挂载点
                  cp -rf $SOURCE_DATA/* $NFS_MOUNT/
                  echo -e "$GREEN$CHECK_MARK 初始数据复制完成 $NC"
                else
                  echo -e "$RED$ERROR 源数据目录不存在: $SOURCE_DATA $NC"
                  exit 1
                fi
              else
                echo -e "$GREEN$CHECK_MARK NFS目录已有数据，跳过初始数据复制 $NC"
              fi
              
              # 设置适当的权限
              chmod -R 755 $NFS_MOUNT
              
              echo -e "$GREEN$CHECK_MARK Grafana数据准备完成 $NC"
          securityContext:
            runAsUser: 0  # 以root用户运行
            privileged: true
          volumeMounts:
            - name: grafana-data
              mountPath: /tmp/grafana-nfs
              subPathExpr: $(POD_NAME)
        - name: update-datasources
          image: "${dockerImage}"
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
          command:
            - "/bin/bash"
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
              
              NFS_MOUNT="/tmp/grafana-nfs"  # PVC挂载点
              GRAFANA_DB="$NFS_MOUNT/grafana.db"
              PROMETHEUS_FLAG_FILE="$NFS_MOUNT/.prometheus_datasource_updated"
              INFINITY_FLAG_FILE="$NFS_MOUNT/.infinity_datasource_updated"
              
              # 检查数据库文件是否存在
              if [ ! -f "$GRAFANA_DB" ]; then
                echo -e "$RED$ERROR 找不到Grafana数据库: $GRAFANA_DB $NC"
                exit 1
              fi
              
              # 更新Prometheus数据源
              if [ ! -f "$PROMETHEUS_FLAG_FILE" ]; then
                echo -e "$BLUE$INFO 更新Prometheus数据源URL... $NC"
                
                # 使用sqlite3更新数据源URL
                sqlite3 "$GRAFANA_DB" "UPDATE data_source SET url = 'http://prometheus-prometheus-0.prometheus-prometheus.datasophon.svc.cluster.local:9090' WHERE name = 'Prometheus';"
                
                # 检查是否成功
                if [ $? -eq 0 ]; then
                  # 创建标记文件
                  touch "$PROMETHEUS_FLAG_FILE"
                  echo "$(date) - Prometheus数据源已更新" > "$PROMETHEUS_FLAG_FILE"
                  echo -e "$GREEN$CHECK_MARK Prometheus数据源URL已更新为集群内地址 $NC"
                else
                  echo -e "$RED$ERROR 更新Prometheus数据源失败 $NC"
                fi
              else
                echo -e "$GREEN$CHECK_MARK Prometheus数据源已经更新过，跳过 $NC"
              fi
              
              # 更新Infinity数据源
              if [ ! -f "$INFINITY_FLAG_FILE" ]; then
                echo -e "$BLUE$INFO 更新Infinity数据源URL... $NC"
                
                # 使用sqlite3更新Infinity数据源URL
                sqlite3 "$GRAFANA_DB" "UPDATE data_source SET url = '${apiUrl}' WHERE name = 'Infinity';"
                
                # 检查是否成功
                if [ $? -eq 0 ]; then
                  # 创建标记文件
                  touch "$INFINITY_FLAG_FILE"
                  echo "$(date) - Infinity数据源已更新" > "$INFINITY_FLAG_FILE"
                  echo -e "$GREEN$CHECK_MARK Infinity数据源URL更新为${apiUrl} $NC"
                else
                  echo -e "$RED$ERROR 更新Infinity数据源失败 $NC"
                fi
              else
                echo -e "$GREEN$CHECK_MARK Infinity数据源已经更新过，跳过 $NC"
              fi
          securityContext:
            runAsUser: 0  # 以root用户运行
            privileged: true
          volumeMounts:
            - name: grafana-data
              mountPath: /tmp/grafana-nfs
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
          image: "${dockerImage}"
          imagePullPolicy: Always
          <#assign hasPorts = false>
          <#if node_port_mappings?? && node_port_mappings?has_content>
            <#list node_port_mappings as item>
              <#if item?keys?has_content && item?keys[0]?? && item[item?keys[0]]?? && item[item?keys[0]]?string != "">
                <#assign hasPorts = true>
                <#break>
              </#if>
            </#list>
          </#if>
          <#if !hasPorts && cluster_port_mappings?? && cluster_port_mappings?has_content>
            <#list cluster_port_mappings as item>
              <#if item?keys?has_content && item?keys[0]?? && item[item?keys[0]]?? && item[item?keys[0]]?string != "">
                <#assign hasPorts = true>
                <#break>
              </#if>
            </#list>
          </#if>
          
          <#if hasPorts>
          ports:
            <#if node_port_mappings?? && node_port_mappings?has_content>
              <#list node_port_mappings as item>
                <#if item?keys?has_content && item?keys[0]?? && item[item?keys[0]]?? && item[item?keys[0]]?string != "">
            - containerPort: ${item[item?keys[0]]}
              name: nodeport-${item?index + 1}
                </#if>
              </#list>
            </#if>
            <#if cluster_port_mappings?? && cluster_port_mappings?has_content>
              <#list cluster_port_mappings as item>
                <#if item?keys?has_content && item?keys[0]?? && item[item?keys[0]]?? && item[item?keys[0]]?string != "">
            - containerPort: ${item[item?keys[0]]}
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
          readinessProbe:
            exec:
              command:
                - "/bin/bash"
                - "-c"
                - |
                  ${statusCommand}
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
            - name: grafana-data
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
        - name: grafana-data
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