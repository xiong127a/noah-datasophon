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
        - name: init-sysctl
          image: "${dockerImage}"
          command: ["/bin/bash", "-c"]
          args:
            - |
              sysctl -w fs.file-max=1000000
              sysctl -w fs.inotify.max_user_watches=524288
              sysctl -w fs.inotify.max_user_instances=524288
              echo "* soft nofile 1000000" >> /etc/security/limits.conf
              echo "* hard nofile 1000000" >> /etc/security/limits.conf
              echo "* soft nproc 65535" >> /etc/security/limits.conf
              echo "* hard nproc 65535" >> /etc/security/limits.conf
          securityContext:
            privileged: true
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
              ulimit -n 1000000
              ulimit -u 65535
              sysctl -w fs.file-max=1000000
              sysctl -w fs.inotify.max_user_watches=524288
              sysctl -w fs.inotify.max_user_instances=524288
              ${startCommand}
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
            capabilities:
              add: ["SYS_RESOURCE"]
            runAsUser: 0
            runAsGroup: 0
            fsGroup: 0
            runAsNonRoot: false
            allowPrivilegeEscalation: true
            readOnlyRootFilesystem: false
            seLinuxOptions:
              level: "s0:c123,c456"
            windowsOptions:
              runAsUserName: "ContainerAdministrator"
            sysctls:
              - name: fs.file-max
                value: "1000000"
              - name: fs.inotify.max_user_watches
                value: "524288"
              - name: fs.inotify.max_user_instances
                value: "524288"
          volumeMounts:
            <#list volumePathSet as item>
            - name: "${item.name}"
              mountPath: "${item.value}"
            </#list>
            <#list volumeConfigMapSet as item>
            - name: "${item.name}"
              mountPath: "${item.value}"
              <#if item.fileName?? && item.fileName != "">
              subPath: "${item.fileName}"
              </#if>
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