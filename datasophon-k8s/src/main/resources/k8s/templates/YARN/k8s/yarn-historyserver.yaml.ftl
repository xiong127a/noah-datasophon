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
              echo "Setting permissions for HistoryServer PVC mount path..."
              chmod -R 777 ${mount_path}
              echo "Permissions set successfully"
          securityContext:
            runAsUser: 0  # 以root用户运行
            privileged: true
          volumeMounts:
            - name: yarn-data
              mountPath: ${mount_path}
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
          imagePullPolicy: "Always"
          command:
            - "/bin/bash"
            - "-c"
            - |
              if ${enableKerberos}; then
                echo "Kerberos is enabled. Performing Kerberos setup...";
                if [ ! -f /etc/security/keytab/keystore ]; then
                  FQDN=$(hostname)
                  cd /opt/datasophon/script && sh keystore.sh $FQDN
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
              ${startCommand}
          readinessProbe:
            tcpSocket:
              port: 19888
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
        - name: yarn-data
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