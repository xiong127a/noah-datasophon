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
            - "sleep 3 && ${startCommand}"
          readinessProbe:
            tcpSocket:
              port: 9998
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