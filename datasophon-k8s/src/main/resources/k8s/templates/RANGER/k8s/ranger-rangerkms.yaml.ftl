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
              ln -s /opt/datasophon/ranger-2.1.0 /opt/datasophon/ranger \
              && cp /opt/datasophon/ranger-2.1.0/ranger-2.1.0-kms/install.properties2 /opt/datasophon/ranger-2.1.0/ranger-2.1.0-kms/install.properties \
              && chmod 755 /opt/datasophon/ranger-2.1.0/ranger-2.1.0-kms/install.properties \
              && cd /opt/datasophon/ranger-2.1.0/ranger-2.1.0-kms \
              && sh ./setup.sh \
              && sh ./enable-kms-plugin.sh
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
            <#list itemList as item>
            - mountPath: "${item.value}"
              name: "${item.name}"
            </#list>
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