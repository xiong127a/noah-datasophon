apiVersion: "apps/v1"
kind: "Deployment"
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
              cp /opt/datasophon/ranger-2.1.0/ranger-2.1.0-usersync/install.properties1 /opt/datasophon/ranger-2.1.0/ranger-2.1.0-usersync/install.properties \
              && chmod 755 /opt/datasophon/ranger-2.1.0/ranger-2.1.0-usersync/install.properties \
              && cd /opt/datasophon/ranger-2.1.0/ranger-2.1.0-usersync \
              && sh ./setup.sh \
              && sh ./set_globals.sh
              #literal#sed -i '/<name>ranger\\.usersync\\.enabled<\\/name>/{n; s/<value>false<\\/value>/<value>true<\\/value>/}' /opt/datasophon/ranger-2.1.0/ranger-2.1.0-usersync/conf/ranger-ugsync-site.xml#end#
              ln -s /opt/datasophon/ranger-2.1.0/ranger-2.1.0-usersync/ranger-usersync-services.sh /usr/bin/ranger-usersync
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
              memory: ${requests_memory}
              cpu: ${requests_cpu}
            limits:
              memory: ${limits_memory}
              cpu: ${limits_cpu}
          securityContext:
            privileged: true
          volumeMounts:
            <#list itemList as item>
            - mountPath: "${item.value}"
              name: "${item.name}"
            </#list>
      nodeSelector:
        ${namespace}-${serviceRoleFullName}: "true"
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