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
      hostNetwork: true
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
              port: 8485
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