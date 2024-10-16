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
      initContainers: 
        - name: check-and-copy-backup
          image: "${dockerImage}" 
          args: 
            - "/bin/bash"
            - "-c"
            - |
              if [ -z "$(ls -A /var/lib/openldap)" ]; then 
                echo '/var/lib/openldap file not already exist'
                cp -r /mnt/backup/ldap/* /var/lib/openldap/; 
              fi 
              if [ -z "$(ls -A /etc/openldap/slapd.d)" ]; then 
                echo '/etc/openldap/slapd.d file not already exist'
                cp -r /mnt/backup/slapd.d/* /etc/openldap/slapd.d/; 
              fi
              chown ldap:ldap -R /var/lib/openldap /etc/openldap/slapd.d/
              chmod 700 -R /var/lib/openldap /etc/openldap/slapd.d/
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
          workingDir: ${appHome}
          image: "${dockerImage}"
          imagePullPolicy: "Always"
          command:
            - "/bin/bash"
            - "-c"
            - |
              systemctl restart systemd-journald && systemctl restart rsyslog && ${startCommand}
          readinessProbe:
            exec:
              command:
                - "/bin/bash"
                - "-c"
                - "${statusCommand}"
            failureThreshold: 3
            initialDelaySeconds: 3
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
            <#list itemList as item>
            - mountPath: "${item.value}"
              name: "${item.name}"
            </#list>
            - mountPath: "/etc/localtime"
              name: "timezone"
      nodeSelector:
        ${serviceRoleFullName}: "true"
      terminationGracePeriodSeconds: 30
      volumes:
        <#list itemList as item>
        - hostPath:
            path: "${item.value}"
          name: "${item.name}"
        </#list>
        - hostPath:
            path: "/etc/localtime"
          name: "timezone"