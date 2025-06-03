apiVersion: "apps/v1"
kind: "Deployment"
metadata:
  labels:
    name: "${serviceRoleFullName}"
  name: "${serviceRoleFullName}"
  namespace: ${namespace}
spec:
  replicas: ${roleNodeCnt}
  selector:
    matchLabels:
      app: "${serviceRoleFullName}"
  strategy:
    type: "RollingUpdate"
    rollingUpdate:
      maxSurge: 0
      maxUnavailable: 1
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
        - name: namenode-format
          image: "${dockerImage}"
          args:
            - "/bin/bash"
            - "-c"
            - |
              if [ ! -d ${namenodeDir}/current ]; then
                echo "format namenode";
                if ${enableKerberos}; then
                  echo "Kerberos is enabled. Running keystore setup...";
                  if [ ! -f /etc/security/keytab/keystore ]; then
                    HOSTNAME=$(hostname)
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
                else
                  echo "Kerberos is not enabled. Skipping Kerberos setup.";
                fi
                if ${enableRangerPlugin}; then
                  echo "Ranger plugin is enabled. Performing Ranger setup...";
                  cd ${appHome}/ranger-hdfs-plugin && \
                  sh ${appHome}/ranger-hdfs-plugin/enable-hdfs-plugin.sh
                else
                  echo "Ranger plugin is not enabled. Skipping Ranger setup.";
                fi
                sleep $((RANDOM % 10))
                if [ -d ${journalnodeDir}/meta ]; then
                  echo "Standby"
                  echo Y | ${appHome}/bin/hdfs namenode -bootstrapStandby
                else
                  echo "active"
                  echo Y | ${appHome}/bin/hdfs namenode -format smhadoop
                fi
              else
                echo "formatted......."
              fi
          volumeMounts:
            <#list itemList as item>
            - mountPath: "${item.value}"
              name: "${item.name}"
            </#list>
            - mountPath: "/etc/localtime"
              name: "timezone"
      containers:
        - env:
            - name: USER
              value: ${runAs}
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
              HOSTNAME=$(hostname)
              if ${enableKerberos}; then
                echo "Kerberos is enabled. Running keystore setup...";
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
                  su - hdfs -c "kinit -kt /etc/security/keytab/nn.service.keytab nn/$HOSTNAME@HADOOP.COM"
              else
                echo "Kerberos is not enabled.";
              fi
              if ${enableRangerPlugin}; then
                echo "Ranger plugin is enabled. Performing Ranger setup...";
                cd ${appHome}/ranger-hdfs-plugin && \
                sh ${appHome}/ranger-hdfs-plugin/enable-hdfs-plugin.sh
              else
                echo "Ranger plugin is not enabled. Skipping Ranger setup.";
              fi
              ${startCommand}
          readinessProbe:
            exec:
              command:
                - "/bin/bash"
                - "-c"
                - "${statusCommand}"
            failureThreshold: 3
            initialDelaySeconds: 10
            periodSeconds: 10
            successThreshold: 1
            timeoutSeconds: 1
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
            <#list volumePathSet as item>
            - name: "${item.name}"
              mountPath: "${item.value}"
            </#list>
            <#list volumeConfigMapSet as item>
            - name: "${item.name}"
              mountPath: "${item.value}"
              subPath: "${item.fileName}"
            </#list>
            - name: "timezone"
              mountPath: "/etc/localtime"
      nodeSelector:
        ${serviceRoleFullName}: "true"
      terminationGracePeriodSeconds: 30
      volumes:
        <#list volumeConfigMapSet as item>
        - name: "${item.name}"
          configMap:
            name: "${item.name}"
        </#list>
        <#list volumePathSet as item>
        - name: "${item.name}"
          hostPath:
            path: "${item.value}"
        </#list>
        - name: "timezone"
          hostPath:
            path: "/etc/localtime"