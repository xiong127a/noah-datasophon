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
      containers:
        - env:
            - name: USER
              value: ${runAsUser}
            - name: MEM_LIMIT
              valueFrom:
                resourceFieldRef:
                  resource: limits.memory
          image: "${dockerImage}"
          imagePullPolicy: "Always"
          command:
            - "/bin/bash"
            - "-c"
            - |
              if ${enableKerberos}; then
                  echo "Kerberos is enabled. Running keystore setup...";
                if [ ! -f /etc/security/keytab/keystore ]; then
                  HOSTNAME=$(hostname)
                  cd /opt/datasophon/script && sh keystore.sh $HOSTNAME
                fi
                if [ ! -f /opt/datasophon/hadoop-3.3.3/etc/hadoop/ssl-client.xml ]; then
                  echo "ssl-client.xml not found. Copying from template...";
                  cp /opt/datasophon/hadoop-3.3.3/etc/hadoop/ssl-client.xml.template /opt/datasophon/hadoop-3.3.3/etc/hadoop/ssl-client.xml
                fi
                if [ ! -f /opt/datasophon/hadoop-3.3.3/etc/hadoop/ssl-server.xml ]; then
                  echo "ssl-server.xml not found. Copying from template...";
                  cp /opt/datasophon/hadoop-3.3.3/etc/hadoop/ssl-server.xml.template /opt/datasophon/hadoop-3.3.3/etc/hadoop/ssl-server.xml
                fi
              else
                echo "Kerberos is not enabled. Skipping Kerberos setup.";
              fi
              if ${enableRangerPlugin}; then
                echo "Ranger plugin is enabled. Performing Ranger setup...";
                cp /opt/datasophon/hadoop-3.3.3/share/hadoop/common/lib/jackson-mapper-asl-1.9.13.jar /opt/datasophon/hbase-2.2.7/lib
                cp /opt/datasophon/hadoop-3.3.3/ranger-hdfs-plugin/lib/ranger-hdfs-plugin-impl/httpcore-nio-4.4.6.jar /opt/datasophon/hbase-2.2.7/lib
                cd ${appHome}/ranger-hbase-plugin && \
                sh ${appHome}/ranger-hbase-plugin/enable-hbase-plugin.sh
              else
                echo "Ranger plugin is not enabled. Skipping Ranger setup.";
              fi
              chown -R ${runAsUser}:${runAsGroup} ${appHome}
              cp ${appHome}/conf/hbase-site.xml.example  ${appHome}/conf/hbase-site.xml
              HOSTNAME=$(hostname -f)
              sed -i "s/\$(hostname)/$HOSTNAME/g" ${appHome}/conf/hbase-site.xml
              ${startCommand}
          readinessProbe:
            exec:
              command:
                - "/bin/bash"
                - "-c"
                - "${statusCommand}"
            failureThreshold: 3
            initialDelaySeconds: 10
            periodSeconds: 30
            successThreshold: 1
            timeoutSeconds: 15
          name: "${serviceRoleFullName}"
          resources:
            requests:
              memory: "2Gi"
              cpu: "1"
            limits:
              memory: "4Gi"
              cpu: "2"
          securityContext:
            privileged: true
          volumeMounts:
            <#list volumeConfigMapSet as item>
            - name: "${item.name}"
              mountPath: "${item.value}"
              subPath: "${item.fileName}"
            </#list>
            - name: "timezone"
              mountPath: "/etc/localtime"
      terminationGracePeriodSeconds: 30
      volumes:

        - name: "timezone"
          hostPath:
            path: "/etc/localtime"