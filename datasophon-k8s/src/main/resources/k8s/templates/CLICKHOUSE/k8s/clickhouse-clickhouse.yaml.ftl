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
      containers:
        - name: "${serviceRoleFullName}"
          image: "${dockerImage}"
          imagePullPolicy: "Always"
          command:
            - "/bin/bash"
            - "-c"
            - |
              sh ${appHome}/clickhouse-common-static-23.9.1.1854/install/doinst.sh
              sh ${appHome}/clickhouse-common-static-dbg-23.9.1.1854/install/doinst.sh
              sh ${appHome}/clickhouse-server-23.9.1.1854/install/doinst.sh configure
              # 清理旧配置文件并复制新配置
              rm -rf /etc/clickhouse-server/config.xml
              rm -rf /etc/clickhouse-server/users.xml
              cp  ${appHome}/etc/config.xml /etc/clickhouse-server
              cp  ${appHome}/etc/users.xml /etc/clickhouse-server
              chown clickhouse:clickhouse /etc/clickhouse-server/config.xml /etc/clickhouse-server/users.xml
              sh ${appHome}/clickhouse-client-23.9.1.1854/install/doinst.sh
              ${startCommand}
          env:
            - name: USER
              value: ${runAsUser}
            - name: MEM_LIMIT
              valueFrom:
                resourceFieldRef:
                  resource: limits.memory
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
            - mountPath: ${item.value}
              name: ${item.name}
            </#list>
            <#list volumeConfigMapSet as item>
            - mountPath: ${item.value}
              name: ${item.name}
            </#list>
            - mountPath: "/etc/localtime"
              name: "timezone"
      nodeSelector:
        ${serviceRoleFullName}: "true"
      terminationGracePeriodSeconds: 30
      volumes:
        <#list volumeConfigMapSet as item>
        - name: ${item.name}
          configMap:
            name: ${item.name}
        </#list>
        <#list volumePathSet as item>
        - name: ${item.name}
          hostPath:
            path: ${item.value}
        </#list>
        - name: "timezone"
          hostPath:
            path: "/etc/localtime"
