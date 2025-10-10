apiVersion: "apps/v1"
kind: "Deployment"
metadata:
  labels:
    name: "${serviceRoleFullName}"
  name: "${serviceRoleFullName}"
  namespace: ${namespace}
  annotations:
    datasophon.io/notes: "注意：当前方案将NodeExporter集成在Prometheus镜像中，按需部署"
spec:
  replicas: ${roleNodeCnt}
  selector:
    matchLabels:
      app: "${serviceRoleFullName}"
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  template:
    metadata:
      labels:
        name: "${serviceRoleFullName}"
        app: "${serviceRoleFullName}"
      annotations:
        serviceInstanceName: "${serviceName}"
        datasophon.io/component: "nodeexporter"
        datasophon.io/version: "1.5.0"
    spec:
      nodeSelector:
        ${namespace}-${serviceRoleFullName}: "true"
      hostNetwork: true
      hostPID: true
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
            - name: NODE_NAME
              valueFrom:
                fieldRef:
                  fieldPath: spec.nodeName
            - name: POD_IP
              valueFrom:
                fieldRef:
                  fieldPath: status.podIP
          image: "${dockerImage}"
          imagePullPolicy: "Always"
          command:
            - "/bin/bash"
            - "-c"
            - |
              ulimit -n 1000000
              ulimit -u 65535
              echo "Starting NodeExporter on host: $NODE_NAME with IP: $POD_IP"
              cd /opt/datasophon/prometheus
              mkdir -p node_exporter/pid node_exporter/logs
              ls -la node_exporter/
              
              # 直接使用control.sh启动
              ${startCommand}
              
              # 保持容器运行，便于检查状态
              tail -f /dev/null
          readinessProbe:
            exec:
              command:
                - "/bin/bash"
                - "-c"
                - "cd /opt/datasophon/prometheus && ps -ef | grep -v grep | grep node_exporter || exit 1"
            failureThreshold: 3
            initialDelaySeconds: 30
            periodSeconds: 30
            successThreshold: 1
            timeoutSeconds: 15
          livenessProbe:
            exec:
              command:
                - "/bin/bash"
                - "-c"
                - "cd /opt/datasophon/prometheus && ps -ef | grep -v grep | grep node_exporter || exit 1"
            failureThreshold: 3
            initialDelaySeconds: 60
            periodSeconds: 60
            successThreshold: 1
            timeoutSeconds: 15
          name: "${serviceRoleFullName}"
          ports:
            - containerPort: 9100
              hostPort: 9100
              name: "metrics"
              protocol: "TCP"
          resources:
            requests:
              memory: ${requests_memory}
              cpu: ${requests_cpu}
            limits:
              memory: ${limits_memory}
              cpu: ${limits_cpu}
          securityContext:
            privileged: true
            capabilities:
              add: ["SYS_RESOURCE"]
            runAsUser: 0
            runAsGroup: 0
            fsGroup: 0
            runAsNonRoot: false
            allowPrivilegeEscalation: true
          volumeMounts:
            - name: "proc"
              mountPath: "/host/proc"
              readOnly: true
            - name: "sys"
              mountPath: "/host/sys"
              readOnly: true
            - name: "root"
              mountPath: "/host/root"
              readOnly: true
              mountPropagation: "HostToContainer"
            - name: "timezone"
              mountPath: "/etc/localtime"
            - name: "hosts-file"
              mountPath: "/etc/hosts"
      terminationGracePeriodSeconds: 30
      volumes:
        - name: "proc"
          hostPath:
            path: "/proc"
        - name: "sys"
          hostPath:
            path: "/sys"
        - name: "root"
          hostPath:
            path: "/"
        - name: "timezone"
          hostPath:
            path: "/etc/localtime"
        - name: "hosts-file"
          hostPath:
            path: "/etc/hosts"