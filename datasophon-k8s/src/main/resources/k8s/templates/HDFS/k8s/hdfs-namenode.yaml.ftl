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
        - name: namenode-format
          image: "${dockerImage}"
          args:
            - "/bin/bash"
            - "-c"
            - "sleep $((RANDOM % 60)) && if [ -d ${journalnodeDir}/meta ]; then echo Y | /opt/datasophon/hadoop-3.3.3/bin/hdfs namenode -bootstrapStandby; else echo Y | /opt/datasophon/hadoop-3.3.3/bin/hdfs namenode -format smhadoop; fi"
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
            - "${startCommand}"
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