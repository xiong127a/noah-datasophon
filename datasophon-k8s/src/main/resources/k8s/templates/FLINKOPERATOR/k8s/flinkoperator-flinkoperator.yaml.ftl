apiVersion: v1
kind: ServiceAccount
metadata:
  name: "${serviceRoleFullName}"
  namespace: ${namespace}
---
apiVersion: v1
kind: ServiceAccount
metadata:
  name: flink
  namespace: ${namespace}

# 2. 配置映射
apiVersion: v1
kind: ConfigMap
metadata:
  name: flink-operator-config
  namespace: ${namespace}
data:
  config.yaml: |+
    taskmanager.numberOfTaskSlots: 1
    parallelism.default: 1
    kubernetes.operator.health.probe.enabled: true
    kubernetes.operator.health.probe.port: 8085
  
  flink-conf.yaml: |+
    taskmanager.numberOfTaskSlots: 1
    parallelism.default: 1
    kubernetes.operator.metrics.reporter.slf4j.factory.class: org.apache.flink.metrics.slf4j.Slf4jReporterFactory
    kubernetes.operator.metrics.reporter.slf4j.interval: 5 MINUTE
    kubernetes.operator.reconcile.interval: 15 s
    kubernetes.operator.observer.progress-check.interval: 5 s
    kubernetes.operator.health.probe.enabled: true
    kubernetes.operator.health.probe.port: 8085
  
  log4j-operator.properties: |+
    rootLogger.level = INFO
    rootLogger.appenderRef.console.ref = ConsoleAppender
    appender.console.name = ConsoleAppender
    appender.console.type = CONSOLE
    appender.console.layout.type = PatternLayout
    appender.console.layout.pattern = %d{yyyy-MM-dd HH:mm:ss} %-5p [%-30c{1}] %X{resource.namespace}/%X{resource.name} - %m%n
  
  log4j-console.properties: |+
    rootLogger.level = INFO
    rootLogger.appenderRef.console.ref = ConsoleAppender
    appender.console.name = ConsoleAppender
    appender.console.type = CONSOLE
    appender.console.layout.type = PatternLayout
    appender.console.layout.pattern = %d{yyyy-MM-dd HH:mm:ss} %-5p [%-30c{1}] - %m%n

---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: "${serviceRoleFullName}"
rules:
  - apiGroups: [""]
    resources: ["pods", "services", "events", "configmaps", "secrets"]
    verbs: ["get", "list", "watch", "create", "update", "patch", "delete"]
  - apiGroups: ["apps"]
    resources: ["deployments"]
    verbs: ["get", "list", "watch", "create", "update", "patch", "delete"]
  - apiGroups: ["flink.apache.org"]
    resources: ["flinkdeployments", "flinksessionjobs"]
    verbs: ["get", "list", "watch", "create", "update", "patch", "delete"]
  - apiGroups: ["networking.k8s.io"]
    resources: ["ingresses"]
    verbs: ["get", "list", "watch", "create", "update", "patch", "delete"]
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: flink-operator-role-binding
roleRef:
  kind: ClusterRole
  name: "${serviceRoleFullName}"
  apiGroup: rbac.authorization.k8s.io
subjects:
  - kind: ServiceAccount
    name: "${serviceRoleFullName}"
    namespace: ${namespace}
---
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  name: flink
  namespace: ${namespace}
rules:
  - apiGroups: [""]
    resources: ["pods", "configmaps"]
    verbs: ["get", "list", "watch", "create", "update", "patch", "delete"]
---
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: flink-role-binding
  namespace: ${namespace}
roleRef:
  kind: Role
  name: flink
  apiGroup: rbac.authorization.k8s.io
subjects:
  - kind: ServiceAccount
    name: flink
    namespace: ${namespace}

---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: "${serviceRoleFullName}"
  namespace: ${namespace}
spec:
  replicas: 1
  strategy:
    type: Recreate
  selector:
    matchLabels:
      app: "${serviceRoleFullName}"
  template:
    metadata:
      labels:
        app: "${serviceRoleFullName}"
        name: "${serviceRoleFullName}"
        podConflictName: "${serviceRoleFullName}"
    spec:
      serviceAccountName: "${serviceRoleFullName}"
      affinity:
        podAntiAffinity:
          requiredDuringSchedulingIgnoredDuringExecution:
            - labelSelector:
                matchLabels:
                  name: "${serviceRoleFullName}"
                  podConflictName: "${serviceRoleFullName}"
              namespaces:
                - "${namespace}"
              topologyKey: kubernetes.io/hostname
      containers:
        - name: "${serviceRoleFullName}"
          image: "${dockerImage}"  
          imagePullPolicy: "Always"
          command: ["/docker-entrypoint.sh", "operator"]
          ports:
            - containerPort: 8085
              name: health-port
          env:
            - name: OPERATOR_NAMESPACE
              valueFrom:
                fieldRef:
                  fieldPath: metadata.namespace
            - name: FLINK_CONF_DIR
              value: /opt/flink/conf
            - name: LOG_CONFIG
              value: -Dlog4j.configurationFile=/opt/flink/conf/log4j-operator.properties
          volumeMounts:
            - name: config-volume
              mountPath: /opt/flink/conf
          livenessProbe:
            httpGet:
              path: /
              port: health-port
            initialDelaySeconds: 30
            periodSeconds: 10
          startupProbe:
            httpGet:
              path: /
              port: health-port
            failureThreshold: 30
            periodSeconds: 10
      volumes:
        - name: config-volume
          configMap:
            name: flink-operator-config